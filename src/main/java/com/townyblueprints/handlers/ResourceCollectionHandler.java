package com.townyblueprints.handlers;

import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.PlacedBlueprint;
import com.townyblueprints.models.ResourceTemplate;
import com.townyblueprints.util.ItemUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ResourceCollectionHandler implements Listener {
    private final TownyBlueprints plugin;
    private final Map<String, Map<String, Integer>> pendingCollections = new HashMap<>();
    @Getter
    private long lastNewDay = System.currentTimeMillis();

    @EventHandler
    public void onNewDay(NewDayEvent event) {
        lastNewDay = System.currentTimeMillis();

        // Process all active blueprints and store their resources
        for (PlacedBlueprint blueprint : plugin.getBlueprintManager().getAllPlacedBlueprints()) {
            if (blueprint.isActive()) {
                String blueprintId = blueprint.getId();
                Map<String, Integer> resources = new HashMap<>();

                if (blueprint.getBlueprint().getIncomeType().startsWith("template:")) {
                    String templateName = blueprint.getBlueprint().getIncomeType().substring(9);
                    ResourceTemplate template = plugin.getResourceTemplateManager().getTemplate(templateName);
                    if (template != null) {
                        for (ResourceTemplate.ResourceEntry resource : template.getSelectedResources()) {
                            resources.put(resource.getType(), resource.getRandomAmount());
                        }
                    }
                } else {
                    resources.put(blueprint.getBlueprint().getIncomeType(),
                            (int)blueprint.getBlueprint().getDailyIncome());
                }

                pendingCollections.put(blueprintId, resources);
            }
        }
    }

    public void collectResources(Player player, Town town, String type) {
        boolean anyResourcesCollected = false;

        // Get all placed blueprints for the town
        List<PlacedBlueprint> blueprints = new ArrayList<>(plugin.getBlueprintManager().getPlacedBlueprintsForTown(town));

        // If type is specified, filter blueprints by type
        if (type != null && !type.isEmpty() && !type.equalsIgnoreCase("all")) {
            blueprints = blueprints.stream()
                    .filter(bp -> bp.getBlueprint().getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        // Process each blueprint
        for (PlacedBlueprint blueprint : blueprints) {
            // Skip if upkeep wasn't successful
            if (!blueprint.isSuccessfulUpkeep()) {
                continue;
            }

            String blueprintId = blueprint.getId();
            Map<String, Integer> resources = pendingCollections.get(blueprintId);

            if (resources != null && !resources.isEmpty()) {
                // Process each resource
                for (Map.Entry<String, Integer> entry : resources.entrySet()) {
                    String resourceType = entry.getKey();
                    int amount = entry.getValue();

                    if (resourceType.equals("MONEY")) {
                        town.getAccount().deposit(amount, "Blueprint income");
                        player.sendMessage(String.format("§aCollected §6%d" + plugin.getConfigManager().getCurrencyName() + "§a from %s!",
                                amount, blueprint.getBlueprint().getName()));
                    } else {
                        ItemStack item = ItemUtil.getItemStack(resourceType, amount, player);
                        if (item != null) {
                            // Try to store in warehouse first
                            if (plugin.getWarehouseManager().storeItems(town, item, player)) {
                                player.sendMessage(String.format("§aStored §6%dx %s§a in warehouse from %s!",
                                        amount,
                                        ItemUtil.getDisplayName(resourceType) != null ?
                                                ItemUtil.getDisplayName(resourceType) :
                                                resourceType.toLowerCase().replace("_", " "),
                                        blueprint.getBlueprint().getName()));
                            } else {
                                // If warehouse storage fails, give to player
                                Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                                if (!overflow.isEmpty()) {
                                    for (ItemStack leftover : overflow.values()) {
                                        player.getWorld().dropItem(player.getLocation(), leftover);
                                        player.sendMessage("§eInventory full! Some items have been dropped at your feet.");
                                    }
                                }
                                player.sendMessage(String.format("§aCollected §6%dx %s§a from %s!",
                                        amount,
                                        ItemUtil.getDisplayName(resourceType) != null ?
                                                ItemUtil.getDisplayName(resourceType) :
                                                resourceType.toLowerCase().replace("_", " "),
                                        blueprint.getBlueprint().getName()));
                            }
                        }
                    }
                }
                anyResourcesCollected = true;
                pendingCollections.remove(blueprintId);
                blueprint.setLastCollectionTime(System.currentTimeMillis());
            }
        }

        if (!anyResourcesCollected) {
            player.sendMessage("§cNo resources available for collection! Please wait until the next Towny day.");
        }
    }

    public void collectResourcesAtLocation(Player player, Location location) {
        PlacedBlueprint blueprint = findBlueprintAtLocation(location);
        if (blueprint == null) {
            player.sendMessage("§cNo blueprint found at your location!");
            return;
        }

        if (!blueprint.isSuccessfulUpkeep()) {
            player.sendMessage("§cThis blueprint's upkeep was not met! No resources available.");
            return;
        }

        Town town = blueprint.getTown();
        boolean collected = false;

        String blueprintId = blueprint.getId();
        Map<String, Integer> resources = pendingCollections.get(blueprintId);

        if (resources != null && !resources.isEmpty()) {
            // Process resources
            for (Map.Entry<String, Integer> entry : resources.entrySet()) {
                String resourceType = entry.getKey();
                int amount = entry.getValue();

                if (resourceType.equals("MONEY")) {
                    town.getAccount().deposit(amount, "Blueprint income");
                    player.sendMessage(String.format("§aCollected §6%d" + plugin.getConfigManager().getCurrencyName() + "§a from %s!",
                            amount, blueprint.getBlueprint().getName()));
                    collected = true;
                } else {
                    ItemStack item = ItemUtil.getItemStack(resourceType, amount, player);
                    if (item != null) {
                        if (plugin.getWarehouseManager().storeItems(town, item, player)) {
                            player.sendMessage(String.format("§aStored §6%dx %s§a in warehouse from %s!",
                                    amount,
                                    ItemUtil.getDisplayName(resourceType) != null ?
                                            ItemUtil.getDisplayName(resourceType) :
                                            resourceType.toLowerCase().replace("_", " "),
                                    blueprint.getBlueprint().getName()));
                            collected = true;
                        } else {
                            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                            if (!overflow.isEmpty()) {
                                for (ItemStack leftover : overflow.values()) {
                                    player.getWorld().dropItem(player.getLocation(), leftover);
                                    player.sendMessage("§eInventory full! Some items have been dropped at your feet.");
                                }
                            }
                            player.sendMessage(String.format("§aCollected §6%dx %s§a from %s!",
                                    amount,
                                    ItemUtil.getDisplayName(resourceType) != null ?
                                            ItemUtil.getDisplayName(resourceType) :
                                            resourceType.toLowerCase().replace("_", " "),
                                    blueprint.getBlueprint().getName()));
                            collected = true;
                        }
                    }
                }
            }

            if (collected) {
                pendingCollections.remove(blueprintId);
                blueprint.setLastCollectionTime(System.currentTimeMillis());
            }
        }

        if (!collected) {
            player.sendMessage("§cNo resources available for collection from this blueprint!");
        }
    }

    private PlacedBlueprint findBlueprintAtLocation(Location location) {
        for (PlacedBlueprint blueprint : plugin.getBlueprintManager().getAllPlacedBlueprints()) {
            Location bpLoc = blueprint.getLocation();
            if (bpLoc.getWorld().equals(location.getWorld())) {
                if (location.getX() >= bpLoc.getX() &&
                        location.getX() < bpLoc.getX() + blueprint.getBlueprint().getSizeX() &&
                        location.getY() >= bpLoc.getY() &&
                        location.getY() < bpLoc.getY() + blueprint.getBlueprint().getSizeY() &&
                        location.getZ() >= bpLoc.getZ() &&
                        location.getZ() < bpLoc.getZ() + blueprint.getBlueprint().getSizeZ()) {
                    return blueprint;
                }
            }
        }
        return null;
    }

    public void processBlueprint(PlacedBlueprint blueprint) {
        Map<String, Integer> requiredBlocks = blueprint.getBlueprint().getRequiredBlocks();
        Map<String, Integer> foundBlocks = new HashMap<>();

        // Initialize counters for block definitions
        for (String key : requiredBlocks.keySet()) {
            foundBlocks.put(key, 0);
        }

        // Scan the area for required blocks
        Location loc = blueprint.getLocation();
        for (int x = 0; x < blueprint.getBlueprint().getSizeX(); x++) {
            for (int y = 0; y < blueprint.getBlueprint().getSizeY(); y++) {
                for (int z = 0; z < blueprint.getBlueprint().getSizeZ(); z++) {
                    Block block = loc.getWorld().getBlockAt(
                            loc.getBlockX() + x,
                            loc.getBlockY() + y,
                            loc.getBlockZ() + z
                    );

                    String blockType = block.getType().name();

                    // Check each required block/definition
                    for (Map.Entry<String, Integer> entry : requiredBlocks.entrySet()) {
                        String key = entry.getKey();

                        if (plugin.getBlockDefinitionManager().isBlockDefinition(key)) {
                            // If it's a block definition, check if the current block matches any valid type
                            List<Material> validMaterials = plugin.getBlockDefinitionManager().getDefinition(key);
                            if (validMaterials.contains(block.getType())) {
                                foundBlocks.merge(key, 1, Integer::sum);
                                break; // Break once we've counted this block
                            }
                        } else if (blockType.equals(key)) {
                            // Direct material match
                            foundBlocks.merge(key, 1, Integer::sum);
                            break; // Break once we've counted this block
                        }
                    }
                }
            }
        }

        // Check if all required blocks are present
        boolean hasAllBlocks = requiredBlocks.entrySet().stream()
                .allMatch(entry -> {
                    Integer required = entry.getValue();
                    Integer found = foundBlocks.get(entry.getKey());
                    return found != null && found >= required;
                });

        blueprint.setActive(hasAllBlocks);
        plugin.getBlueprintManager().saveAll();
    }

    private Map<String, Double> getNationTaxRates(Nation nation) {
        Map<String, Double> rates = new HashMap<>();

        // Check if we should use Towny's tax rate
        if (plugin.getConfig().getBoolean("economy.nation_tax.use_towny_tax", true)) {
            double townyTaxRate = nation.getTaxes();
            if (townyTaxRate > 0) {
                rates.put("MONEY", townyTaxRate);
                if (plugin.getConfig().getBoolean("economy.nation_tax.allow_per_resource_rates", true)) {
                    rates.put("ITEMS", townyTaxRate);
                    rates.put("DEFAULT", townyTaxRate);
                }
                return rates;
            }
        }

        // Fall back to config rates
        double defaultRate = plugin.getConfig().getDouble("economy.nation_tax.default_rate", 10);
        boolean allowPerResource = plugin.getConfig().getBoolean("economy.nation_tax.allow_per_resource_rates", true);

        if (allowPerResource) {
            ConfigurationSection resourceRates = plugin.getConfig().getConfigurationSection("economy.nation_tax.resource_rates");
            if (resourceRates != null) {
                for (String key : resourceRates.getKeys(false)) {
                    rates.put(key, resourceRates.getDouble(key, defaultRate));
                }
            }
        }

        if (!rates.containsKey("DEFAULT")) {
            rates.put("DEFAULT", defaultRate);
        }

        return rates;
    }

    private double getTaxRateForResource(Map<String, Double> taxRates, String resourceType) {
        if (taxRates.containsKey(resourceType)) {
            return taxRates.get(resourceType);
        }

        String category = getResourceCategory(resourceType);
        if (taxRates.containsKey(category)) {
            return taxRates.get(category);
        }

        return taxRates.getOrDefault("DEFAULT",
                plugin.getConfig().getDouble("economy.nation_tax.default_rate", 10));
    }

    private String getResourceCategory(String resourceType) {
        if (resourceType.equals("MONEY")) {
            return "MONEY";
        } else if (resourceType.startsWith("vanilla:") ||
                resourceType.startsWith("itemsadder:") ||
                resourceType.startsWith("mmoitems:") ||
                resourceType.startsWith("mythicmobs:") ||
                resourceType.startsWith("oraxen:")) {
            return "ITEMS";
        } else {
            return "OTHER";
        }
    }

    private void storeNationTax(Nation nation, String resourceType, int amount, Player player) {
        Town capital = nation.getCapital();
        if (capital != null) {
            ItemStack taxItems = ItemUtil.getItemStack(resourceType, amount, null);
            if (taxItems != null) {
                if (plugin.getWarehouseManager().storeItems(capital, taxItems, player)) {
                    player.sendMessage(String.format("§7Nation tax collected: §6%dx %s",
                            amount,
                            ItemUtil.getDisplayName(resourceType) != null ?
                                    ItemUtil.getDisplayName(resourceType) :
                                    resourceType.toLowerCase().replace("_", " ")));
                } else {
                    player.sendMessage("§cNation capital warehouse full! Tax converted to money.");
                }
            }
        }
    }
}