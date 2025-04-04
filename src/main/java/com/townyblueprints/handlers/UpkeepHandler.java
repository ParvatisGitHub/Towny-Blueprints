package com.townyblueprints.handlers;

import com.palmergames.bukkit.towny.object.Town;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.PlacedBlueprint;
import com.townyblueprints.models.ResourceTemplate;
import com.townyblueprints.util.ItemUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Location;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class UpkeepHandler {
    private final TownyBlueprints plugin;
    private final Map<String, List<Material>> toolDefinitions = new HashMap<>();

    public Map<String, List<Material>> getToolDefinitions() {
        return toolDefinitions;
    }

    public void loadToolDefinitions() {
        File file = new File(plugin.getDataFolder(), "tool_definitions.yml");
        if (!file.exists()) {
            plugin.saveResource("tool_definitions.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection defsSection = config.getConfigurationSection("definitions");
        if (defsSection != null) {
            for (String toolType : defsSection.getKeys(false)) {
                List<String> materialNames = defsSection.getStringList(toolType);
                List<Material> materials = new ArrayList<>();
                
                for (String materialName : materialNames) {
                    try {
                        materials.add(Material.valueOf(materialName.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material in tool definitions: " + materialName);
                    }
                }
                
                toolDefinitions.put(toolType.toUpperCase(), materials);
            }
        }
    }

    public boolean processUpkeep(PlacedBlueprint blueprint) {
        String upkeepType = blueprint.getBlueprint().getUpkeepType();
        double upkeep = blueprint.getBlueprint().getDailyUpkeep();

        plugin.getLogger().info("[Upkeep] Processing upkeep for blueprint " + blueprint.getId());
        plugin.getLogger().info("[Upkeep] Type: " + upkeepType + ", Amount: " + upkeep);

        boolean upkeepMet = false;

        if (upkeepType.startsWith("template:")) {
            ResourceTemplate template = plugin.getResourceTemplateManager().getTemplate(upkeepType);
            if (template != null) {
                plugin.getLogger().info("[Upkeep] Processing template upkeep: " + template.getName());
                upkeepMet = processTemplateUpkeep(blueprint, template);
            } else {
                plugin.getLogger().warning("[Upkeep] Template not found: " + upkeepType);
            }
        } else if (upkeepType.equals("MONEY")) {
            plugin.getLogger().info("[Upkeep] Processing money upkeep");
            upkeepMet = blueprint.getTown().getAccount().withdraw(upkeep, "Blueprint daily upkeep");
            plugin.getLogger().info("[Upkeep] Money withdrawal " + (upkeepMet ? "successful" : "failed"));
        } else if (upkeepType.equals("TOOL")) {
            plugin.getLogger().info("[Upkeep] Processing tool upkeep");
            upkeepMet = processDurabilityUpkeep(blueprint);
            plugin.getLogger().info("[Upkeep] Tool durability drain " + (upkeepMet ? "successful" : "failed"));
        } else {
            // Handle vanilla items by prefixing with "vanilla:" if not already prefixed
            String processedUpkeepType = upkeepType.contains(":") ? upkeepType : "vanilla:" + upkeepType;
            plugin.getLogger().info("[Upkeep] Processing resource upkeep for " + processedUpkeepType);
            upkeepMet = processResourceUpkeep(blueprint, processedUpkeepType, (int)upkeep);
            plugin.getLogger().info("[Upkeep] Resource collection " + (upkeepMet ? "successful" : "failed"));
        }

        if (!upkeepMet && blueprint.isActive()) {
            plugin.getLogger().warning("[Upkeep] Blueprint " + blueprint.getId() + " deactivated due to insufficient upkeep");
            blueprint.setActive(false);
            plugin.getBlueprintManager().saveAll();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                blueprint.getTown().getResidents().forEach(resident -> {
                    if (resident.isOnline()) {
                        resident.getPlayer().sendMessage(
                            "c[Warning] Your " + blueprint.getBlueprint().getName() + 
                            " blueprint has been deactivated due to insufficient upkeep!"
                        );
                    }
                });
            });
        }

        return upkeepMet;
    }

    private boolean processResourceUpkeep(PlacedBlueprint blueprint, String upkeepType, int amount) {
        plugin.getLogger().info("[Upkeep] Attempting to remove " + amount + " of " + upkeepType + " from warehouses");
        
        // Try to remove items from warehouses first
        if (plugin.getWarehouseManager().removeItems(blueprint.getTown(), upkeepType, amount)) {
            plugin.getLogger().info("[Upkeep] Successfully removed items from warehouse");
            return true;
        }
        
        plugin.getLogger().info("[Upkeep] Warehouse removal failed, checking blueprint containers");

        // Create ItemStack for the required items
        ItemStack requiredItem;
        if (upkeepType.startsWith("vanilla:")) {
            try {
                Material material = Material.valueOf(upkeepType.replace("vanilla:", "").toUpperCase());
                requiredItem = new ItemStack(material, amount);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[Upkeep] Invalid material: " + upkeepType);
                return false;
            }
        } else {
            requiredItem = ItemUtil.getItemStack(upkeepType, amount, null);
            if (requiredItem == null) {
                plugin.getLogger().warning("[Upkeep] Failed to create ItemStack for " + upkeepType);
                return false;
            }
        }

        List<Container> containers = findContainersInBlueprint(blueprint);
        for (Container container : containers) {
            if (hasEnoughItems(container, requiredItem)) {
                removeItems(container, requiredItem);
                plugin.getLogger().info("[Upkeep] Successfully removed items from blueprint container");
                return true;
            }
        }
        
        plugin.getLogger().warning("[Upkeep] No containers found with sufficient items");
        return false;
    }

	private boolean processDurabilityUpkeep(PlacedBlueprint blueprint) {
		if (blueprint.getBlueprint().getToolType() == null || blueprint.getBlueprint().getDurabilityDrain() <= 0) {
			return false;
		}

		Material toolType = blueprint.getBlueprint().getToolType();
		int durabilityDrain = blueprint.getBlueprint().getDurabilityDrain();

		// Try warehouse first
		if (plugin.getWarehouseManager().drainToolDurability(blueprint.getTown(), toolType, durabilityDrain)) {
			return true;
		}

		// If warehouse fails, check containers
		List<Container> containers = findContainersInBlueprint(blueprint);
		for (Container container : containers) {
			if (drainToolDurability(container, toolType, durabilityDrain, blueprint)) {
				return true;
			}
		}

		return false;
	}

    private boolean processTemplateUpkeep(PlacedBlueprint blueprint, ResourceTemplate template) {
    plugin.getLogger().info("[Upkeep] Processing template upkeep for " + blueprint.getId());
    boolean allUpkeepMet = true;

    for (ResourceTemplate.ResourceEntry resource : template.getResources()) {
        plugin.getLogger().info("[Upkeep] Processing resource: " + resource.getType());
        
        if (resource.getType().equals("TOOL")) {
            String toolType = resource.getToolType();
            int durabilityDrain = resource.getDurabilityDrain();
            
            // Try to parse as a specific Material first
            try {
                Material specificTool = Material.valueOf(toolType.toUpperCase());
                // Try warehouse first
                if (plugin.getWarehouseManager().drainToolDurability(blueprint.getTown(), specificTool, durabilityDrain)) {
                    plugin.getLogger().info("[Upkeep] Successfully drained specific tool: " + specificTool.name());
                    continue;
                }

                // If warehouse fails, check containers
                List<Container> containers = findContainersInBlueprint(blueprint);
                boolean toolFound = false;
                for (Container container : containers) {
                    if (drainToolDurability(container, specificTool, durabilityDrain, blueprint)) {
                        toolFound = true;
                        break;
                    }
                }

                if (!toolFound) {
                    plugin.getLogger().warning("[Upkeep] No suitable specific tool found: " + specificTool.name());
                    allUpkeepMet = false;
                }
                continue;
            } catch (IllegalArgumentException e) {
                // Not a specific material, try tool definition
                List<Material> validTools = plugin.getToolDefinitionManager().getDefinition(toolType.toLowerCase());
                if (validTools.isEmpty()) {
                    plugin.getLogger().warning("[Upkeep] No valid tools found for type: " + toolType);
                    allUpkeepMet = false;
                    continue;
                }

                // Try warehouse first with each valid tool type
                boolean toolFound = false;
                for (Material toolMaterial : validTools) {
                    plugin.getLogger().info("[Upkeep] Trying tool: " + toolMaterial.name());
                    if (plugin.getWarehouseManager().drainToolDurability(blueprint.getTown(), toolMaterial, durabilityDrain)) {
                        toolFound = true;
                        break;
                    }
                }

                // If warehouse fails, check containers
                if (!toolFound) {
                    List<Container> containers = findContainersInBlueprint(blueprint);
                    for (Container container : containers) {
                        for (Material toolMaterial : validTools) {
                            if (drainToolDurability(container, toolMaterial, durabilityDrain, blueprint)) {
                                toolFound = true;
                                break;
                            }
                        }
                        if (toolFound) break;
                    }
                }

                if (!toolFound) {
                    plugin.getLogger().warning("[Upkeep] No suitable tool found for durability drain");
                    allUpkeepMet = false;
                }
            }
        } else if (resource.getType().equals("MONEY")) {
            int amount = resource.getRandomAmount();
            plugin.getLogger().info("[Upkeep] Processing money upkeep: " + amount);
            
            if (!blueprint.getTown().getAccount().withdraw(amount, "Blueprint daily upkeep")) {
                plugin.getLogger().warning("[Upkeep] Failed to withdraw money: " + amount);
                allUpkeepMet = false;
            } else {
                plugin.getLogger().info("[Upkeep] Successfully withdrew money: " + amount);
            }
        } else {
            int amount = resource.getRandomAmount();
            String resourceType = resource.getType();
            plugin.getLogger().info("[Upkeep] Processing resource upkeep: " + amount + " of " + resourceType);
            
            // Try warehouse first
            if (plugin.getWarehouseManager().removeItems(blueprint.getTown(), resourceType, amount)) {
                plugin.getLogger().info("[Upkeep] Successfully removed items from warehouse");
                continue;
            }
            
            // If warehouse fails, check containers
            ItemStack requiredItem = ItemUtil.getItemStack(resourceType, amount, null);
            if (requiredItem == null) {
                plugin.getLogger().warning("[Upkeep] Failed to create ItemStack for: " + resourceType);
                allUpkeepMet = false;
                continue;
            }

            boolean resourceFound = false;
            List<Container> containers = findContainersInBlueprint(blueprint);
            for (Container container : containers) {
                if (hasEnoughItems(container, requiredItem)) {
                    removeItems(container, requiredItem);
                    resourceFound = true;
                    plugin.getLogger().info("[Upkeep] Successfully removed items from container");
                    break;
                }
            }

            if (!resourceFound) {
                plugin.getLogger().warning("[Upkeep] No containers found with sufficient items");
                allUpkeepMet = false;
            }
        }
    }

    plugin.getLogger().info("[Upkeep] Template upkeep processing complete. Success: " + allUpkeepMet);
    return allUpkeepMet;
}


    private boolean drainToolDurability(Container container, Material toolType, int durabilityDrain, PlacedBlueprint blueprint) {
        ItemStack[] contents = container.getInventory().getContents();
        boolean success = plugin.getWarehouseManager().drainToolDurability(blueprint.getTown(), toolType, durabilityDrain);
        if (!success) {
			plugin.getLogger().warning("Tool upkeep failed: Insufficient durability in warehouse for " + toolType);
		}
		// Check if we're looking for a generic tool type
        String genericType = toolType.name();
        boolean isGenericTool = toolDefinitions.containsKey(genericType);
        List<Material> validTools = isGenericTool ? toolDefinitions.get(genericType) : null;
        
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getItemMeta() instanceof Damageable) {
                // Check if the item matches our tool requirements
                boolean isMatchingTool = isGenericTool ? 
                    validTools.contains(item.getType()) : 
                    item.getType() == toolType;

                if (isMatchingTool) {
                    Damageable damageable = (Damageable) item.getItemMeta();
                    int maxDurability = item.getType().getMaxDurability();
                    int currentDurability = maxDurability - damageable.getDamage();

                    if (currentDurability > durabilityDrain) {
                        // Tool has enough durability, drain it
                        damageable.setDamage(damageable.getDamage() + durabilityDrain);
                        item.setItemMeta((ItemMeta) damageable);
                        
                        // If tool is about to break (less than 10% durability), send warning
                        if (maxDurability - damageable.getDamage() < (maxDurability * 0.1)) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                blueprint.getTown().getResidents().forEach(resident -> {
                                    if (resident.isOnline()) {
                                        resident.getPlayer().sendMessage(
                                            "c[Warning] A " + item.getType().name().toLowerCase().replace("_", " ") + 
                                            " in your " + blueprint.getBlueprint().getName() + 
                                            " blueprint is about to break!"
                                        );
                                    }
                                });
                            });
                        }
                        return true;
                    } else if (currentDurability > 0) {
                        // Tool will break after this use
                        container.getInventory().setItem(i, null);
                        return false;
                    }
                }
            }
        }
        plugin.getLogger().warning("Tool upkeep failed: No matching tools found for " + toolType);
		return false;
    }

    private List<Container> findContainersInBlueprint(PlacedBlueprint blueprint) {
        List<Container> containers = new ArrayList<>();
        Location base = blueprint.getLocation();
        int sizeX = blueprint.getBlueprint().getSizeX();
        int sizeY = blueprint.getBlueprint().getSizeY();
        int sizeZ = blueprint.getBlueprint().getSizeZ();

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    Block block = base.clone().add(x, y, z).getBlock();
                    if (block.getState() instanceof Container) {
                        containers.add((Container) block.getState());
                    }
                }
            }
        }

        return containers;
    }

    private boolean hasEnoughItems(Container container, ItemStack required) {
        int found = 0;
        for (ItemStack item : container.getInventory().getContents()) {
            if (item != null && item.isSimilar(required)) {
                found += item.getAmount();
                if (found >= required.getAmount()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeItems(Container container, ItemStack required) {
        int remaining = required.getAmount();
        ItemStack[] contents = container.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.isSimilar(required)) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    container.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
        container.update();
    }
}