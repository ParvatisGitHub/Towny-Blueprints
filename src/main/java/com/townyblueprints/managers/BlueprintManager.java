package com.townyblueprints.managers;

import com.palmergames.bukkit.towny.object.Town;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.Blueprint;
import com.townyblueprints.models.PlacedBlueprint;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BlueprintManager {
    private final TownyBlueprints plugin;
    private final Map<String, Blueprint> blueprints = new HashMap<>();
    private final Map<String, PlacedBlueprint> placedBlueprints = new HashMap<>();
    private final Map<Town, Map<String, Integer>> pendingResources = new HashMap<>();
    @Getter
    private final Map<String, Boolean> bonusBlockContributions = new HashMap<>();

    public void loadAll() {
        loadBlueprintsFromFolder(new File(plugin.getDataFolder(), "blueprints"));
        loadBlueprintsFromFolder(plugin.getDataFolder());
    }

    private void loadBlueprintsFromFolder(File folder) {
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml") && !name.equals("config.yml") && !name.equals("data.yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String name = config.getString("name");
            if (name == null) continue;

            Blueprint blueprint = new Blueprint();
            blueprint.setName(name);
            blueprint.setDescription(config.getString("description", ""));
            blueprint.setType(config.getString("type", "default"));

            ConfigurationSection sizeSection = config.getConfigurationSection("size");
                if (sizeSection != null) {
                    blueprint.setSizeX(sizeSection.getInt("x", 1));
                    blueprint.setSizeY(sizeSection.getInt("y", 1));
                    blueprint.setSizeZ(sizeSection.getInt("z", 1));
                }

            blueprint.setDailyIncome(config.getDouble("daily_income", 0.0));
            blueprint.setIncomeType(config.getString("income_type", "MONEY"));
            blueprint.setDailyUpkeep(config.getDouble("daily_upkeep", 0.0));
            blueprint.setUpkeepType(config.getString("upkeep_type", "MONEY"));
            blueprint.setPlacementCost(config.getDouble("placement_cost", 0.0));
            blueprint.setPermissionNode(config.getString("permission_node",
                    "townyblueprints.blueprint." + name.toLowerCase().replace(" ", "_")));
            blueprint.setMaxPerTown(config.getInt("max_per_town", -1));
            blueprint.setRequiredTownLevel(config.getInt("required_town_level", 0));
            blueprint.setBonusTownBlocks(config.getInt("bonus_town_blocks", 0));
            blueprint.setUpgradesTo(config.getString("upgrades_to"));
            blueprint.setUpgradeCost(config.getDouble("upgrade_cost", 0.0));

            String toolType = config.getString("tool_type");
            if (toolType != null) {
                try {
                    blueprint.setToolType(Material.valueOf(toolType.toUpperCase()));
                    blueprint.setDurabilityDrain(config.getInt("durability_drain", 0));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid tool type in blueprint " + name + ": " + toolType);
                }
            }

            String displayMaterialStr = config.getString("display_material");
            if (displayMaterialStr != null) {
                try {
                    blueprint.setDisplayMaterial(Material.valueOf(displayMaterialStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid display material in blueprint " + name + ": " + displayMaterialStr);
                }
            }

            Map<String, Integer> requiredBlocks = new HashMap<>();
            ConfigurationSection blocksSection = config.getConfigurationSection("required_blocks");
            if (blocksSection != null) {
                for (String key : blocksSection.getKeys(false)) {
                    requiredBlocks.put(key.toUpperCase(), blocksSection.getInt(key));
                }
            }
            blueprint.setRequiredBlocks(requiredBlocks);

            blueprints.put(name.toLowerCase(), blueprint);
            plugin.getLogger().info("Loaded blueprint: " + name);
        }
    }

    public void saveBlueprint(Blueprint blueprint) {
        // Save to file system
        String fileName = blueprint.getName().toLowerCase().replace(" ", "_") + ".yml";
        File blueprintsFolder = new File(plugin.getDataFolder(), "blueprints");
        if (!blueprintsFolder.exists()) {
            blueprintsFolder.mkdirs();
        }

        File blueprintFile = new File(blueprintsFolder, fileName);
        YamlConfiguration config = new YamlConfiguration();

        config.set("name", blueprint.getName());
        config.set("description", blueprint.getDescription());
        config.set("type", blueprint.getType());
        config.set("size.x", blueprint.getSizeX());
        config.set("size.y", blueprint.getSizeY());
        config.set("size.z", blueprint.getSizeZ());

        config.set("daily_income", blueprint.getDailyIncome());
        config.set("income_type", blueprint.getIncomeType());
        config.set("daily_upkeep", blueprint.getDailyUpkeep());
        config.set("upkeep_type", blueprint.getUpkeepType());
        config.set("placement_cost", blueprint.getPlacementCost());
        config.set("permission_node", blueprint.getPermissionNode());
        config.set("max_per_town", blueprint.getMaxPerTown());
        config.set("required_town_level", blueprint.getRequiredTownLevel());
        config.set("bonus_town_blocks", blueprint.getBonusTownBlocks());
        config.set("upgrades_to", blueprint.getUpgradesTo());
        config.set("upgrade_cost", blueprint.getUpgradeCost());

        if (blueprint.getToolType() != null) {
            config.set("tool_type", blueprint.getToolType().name());
            config.set("durability_drain", blueprint.getDurabilityDrain());
        }

        for (Map.Entry<String, Integer> entry : blueprint.getRequiredBlocks().entrySet()) {
            config.set("required_blocks." + entry.getKey(), entry.getValue());
        }

        if (blueprint.getDisplayMaterial() != null && blueprint.getDisplayMaterial() != Material.PAPER) {
            config.set("display_material", blueprint.getDisplayMaterial().name());
        }

        try {
            config.save(blueprintFile);
            blueprints.put(blueprint.getName().toLowerCase(), blueprint);
            plugin.getLogger().info("Saved blueprint: " + blueprint.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save blueprint: " + blueprint.getName());
            e.printStackTrace();
        }
    }

    public void importBlueprint(File file) {
        if (!file.getName().endsWith(".yml")) return;

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String name = config.getString("name");
            if (name == null) return;

            File blueprintsFolder = new File(plugin.getDataFolder(), "blueprints");
            if (!blueprintsFolder.exists()) {
                blueprintsFolder.mkdirs();
            }

            File destination = new File(blueprintsFolder, file.getName());
            Files.copy(file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

            loadAll();
            plugin.getLogger().info("Imported blueprint: " + name);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to import blueprint: " + file.getName());
            e.printStackTrace();
        }
    }

    public void exportBlueprint(String name, File destination) {
        Blueprint blueprint = getBlueprint(name);
        if (blueprint == null) return;

        try {
            if (!destination.exists()) {
                destination.mkdirs();
            }

            String fileName = name.toLowerCase().replace(" ", "_") + ".yml";
            File exportFile = new File(destination, fileName);

            YamlConfiguration config = new YamlConfiguration();
            config.set("name", blueprint.getName());
            config.set("description", blueprint.getDescription());
            config.set("type", blueprint.getType());
            config.set("size.x", blueprint.getSizeX());
            config.set("size.y", blueprint.getSizeY());
            config.set("size.z", blueprint.getSizeZ());

            config.set("daily_income", blueprint.getDailyIncome());
            config.set("income_type", blueprint.getIncomeType());
            config.set("daily_upkeep", blueprint.getDailyUpkeep());
            config.set("upkeep_type", blueprint.getUpkeepType());
            config.set("placement_cost", blueprint.getPlacementCost());
            config.set("permission_node", blueprint.getPermissionNode());
            config.set("max_per_town", blueprint.getMaxPerTown());
            config.set("required_town_level", blueprint.getRequiredTownLevel());
            config.set("bonus_town_blocks", blueprint.getBonusTownBlocks());
            config.set("upgrades_to", blueprint.getUpgradesTo());
            config.set("upgrade_cost", blueprint.getUpgradeCost());

            if (blueprint.getToolType() != null) {
                config.set("tool_type", blueprint.getToolType().name());
                config.set("durability_drain", blueprint.getDurabilityDrain());
            }

            for (Map.Entry<String, Integer> entry : blueprint.getRequiredBlocks().entrySet()) {
                config.set("required_blocks." + entry.getKey(), entry.getValue());
            }

            config.save(exportFile);
            plugin.getLogger().info("Exported blueprint: " + name);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to export blueprint: " + name);
            e.printStackTrace();
        }
    }

    public void deleteBlueprint(String name) {
        Blueprint blueprint = getBlueprint(name);
        if (blueprint == null) return;

        blueprints.remove(name.toLowerCase());

        String fileName = name.toLowerCase().replace(" ", "_") + ".yml";
        File blueprintsFolder = new File(plugin.getDataFolder(), "blueprints");
        File blueprintFile = new File(blueprintsFolder, fileName);

        if (blueprintFile.exists() && blueprintFile.delete()) {
            plugin.getLogger().info("Deleted blueprint: " + name);
        }
    }

    public Blueprint getBlueprint(String name) {
        return blueprints.get(name.toLowerCase());
    }

    public Collection<Blueprint> getAllBlueprints() {
        return blueprints.values();
    }

    public PlacedBlueprint getPlacedBlueprint(String id) {
        return placedBlueprints.get(id);
    }

    public Collection<PlacedBlueprint> getAllPlacedBlueprints() {
        return placedBlueprints.values();
    }

    public Collection<PlacedBlueprint> getPlacedBlueprintsForTown(Town town) {
        return placedBlueprints.values().stream()
                .filter(bp -> bp.getTown().equals(town))
                .collect(Collectors.toList());
    }

    public String createPlacedBlueprint(PlacedBlueprint blueprint) {
        String id = blueprint.getId();
        placedBlueprints.put(id, blueprint);
        plugin.getDatabase().saveBlueprint(blueprint);
        return id;
    }

    public void removePlacedBlueprint(String id) {
        PlacedBlueprint blueprint = placedBlueprints.get(id);
        if (blueprint != null) {
            if (blueprint.isActive() && bonusBlockContributions.getOrDefault(id, false)) {
                Town town = blueprint.getTown();
                int currentBonus = town.getBonusBlocks();
                updateTownBonusBlocks(town, currentBonus);
            }

            bonusBlockContributions.remove(id);
            placedBlueprints.remove(id);
            if (plugin.getConfigManager().isDynmapEnabled()) {
                plugin.getDynmapListener().removeBlueprintMarker(id);
            }
            plugin.getDatabase().deleteBlueprint(id);
            plugin.getLogger().info("Blueprint with ID " + id + " has been removed.");
        } else {
            plugin.getLogger().warning("No placed blueprint found with ID " + id);
        }
    }

    public void addLoadedBlueprint(PlacedBlueprint blueprint) {
        placedBlueprints.put(blueprint.getId(), blueprint);
        bonusBlockContributions.put(blueprint.getId(), blueprint.isActive());
    }

    public void resetBonusBlockTracking() {
        bonusBlockContributions.clear();
    }

    public int calculateTownBonusBlocks(Town town) {
        return getPlacedBlueprintsForTown(town).stream()
                .filter(PlacedBlueprint::isActive)
                .mapToInt(bp -> bp.getBlueprint().getBonusTownBlocks())
                .sum();
    }

    public int calculateInactiveBonusBlocks(Town town) {
        return getPlacedBlueprintsForTown(town).stream()
                .filter(bp -> !bp.isActive() && bonusBlockContributions.getOrDefault(bp.getId(), false))
                .mapToInt(bp -> bp.getBlueprint().getBonusTownBlocks())
                .sum();
    }

    public void updateTownBonusBlocks(Town town, int currentBonusBlocks) {
        int calculatedBonusBlocks = calculateTownBonusBlocks(town);
        int inactiveBonusBlocks = calculateInactiveBonusBlocks(town);
        boolean hasChanges = false;

        for (PlacedBlueprint blueprint : getPlacedBlueprintsForTown(town)) {
            String blueprintId = blueprint.getId();
            boolean isCurrentlyActive = blueprint.isActive();
            boolean wasContributing = bonusBlockContributions.getOrDefault(blueprintId, false);

            if (isCurrentlyActive != wasContributing) {
                hasChanges = true;
                bonusBlockContributions.put(blueprintId, isCurrentlyActive);
            }
        }

        if (hasChanges) {
            town.setBonusBlocks(town.getBonusBlocks() - inactiveBonusBlocks + calculatedBonusBlocks);
            try {
                town.save();
                // Save to database
                for (PlacedBlueprint blueprint : getPlacedBlueprintsForTown(town)) {
                    plugin.getDatabase().saveBlueprint(blueprint);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save town after updating bonus blocks: " + e.getMessage());
            }
        }
    }

    public void addPendingResource(Town town, String resourceType, int amount) {
        pendingResources.computeIfAbsent(town, k -> new HashMap<>())
                .merge(resourceType, amount, Integer::sum);
    }

    public boolean consumeResource(Town town, String resourceType, int amount) {
        Map<String, Integer> townResources = pendingResources.get(town);
        if (townResources == null) return false;

        Integer available = townResources.get(resourceType);
        if (available == null || available < amount) return false;

        townResources.put(resourceType, available - amount);
        return true;
    }

    public Map<String, Integer> getPendingResources(Town town) {
        return pendingResources.getOrDefault(town, new HashMap<>());
    }

    /**
     * Saves all placed blueprints to the database
     */
    public void saveAll() {
        for (PlacedBlueprint blueprint : placedBlueprints.values()) {
            plugin.getDatabase().saveBlueprint(blueprint);
        }
        plugin.getLogger().info("Saved all blueprints to database");
    }
}