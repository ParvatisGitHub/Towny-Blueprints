package com.townyblueprints.tasks;

import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.PlacedBlueprint;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import org.bukkit.util.BoundingBox;

import java.util.*;

public class BlueprintStatusTask extends BukkitRunnable {
    private final TownyBlueprints plugin;
    private int currentIndex = 0;
    private final boolean isPeriodic;
    private final int blueprintsPerTick;
    private final boolean onlyLoadedChunks;
    private List<PlacedBlueprint> blueprintList = new ArrayList<>();
    private long lastFullCheck = 0;

    public BlueprintStatusTask(TownyBlueprints plugin) {
        this.plugin = plugin;
        this.isPeriodic = plugin.getConfig().getString("blueprints.status_check.type", "interval").equals("periodic");
        this.blueprintsPerTick = plugin.getConfig().getInt("blueprints.status_check.blueprints_per_tick", 5);
        this.onlyLoadedChunks = plugin.getConfig().getBoolean("blueprints.status_check.only_loaded_chunks", true);
    }

    @Override
    public void run() {
        // Refresh blueprint list periodically
        long now = System.currentTimeMillis();
        if (now - lastFullCheck > 60000) { // Refresh list every minute
            blueprintList = new ArrayList<>(plugin.getBlueprintManager().getAllPlacedBlueprints());
            lastFullCheck = now;
        }

        if (blueprintList.isEmpty()) return;

        if (isPeriodic) {
            // Check a fixed number of blueprints per tick
            for (int i = 0; i < blueprintsPerTick && i + currentIndex < blueprintList.size(); i++) {
                checkBlueprint(blueprintList.get(currentIndex + i));
            }

            // Update index for next run
            currentIndex += blueprintsPerTick;
            if (currentIndex >= blueprintList.size()) {
                currentIndex = 0;
            }
        } else {
            // Check all blueprints at once
            for (PlacedBlueprint blueprint : blueprintList) {
                checkBlueprint(blueprint);
            }
        }
    }

    private void checkBlueprint(PlacedBlueprint blueprint) {
        Location loc = blueprint.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // Check if chunk is loaded when required
        if (onlyLoadedChunks && !world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            return;
        }

        Map<String, Integer> requiredBlocks = blueprint.getBlueprint().getRequiredBlocks();
        Map<String, Integer> foundBlocks = new HashMap<>();
        Map<String, Integer> requiredMobs = blueprint.getBlueprint().getRequiredMobs();
        Map<String, Integer> foundMobs = new HashMap<>();

        // Initialize counters for block definitions
        for (String key : requiredBlocks.keySet()) {
            foundBlocks.put(key, 0);
        }

        // Initialize counters for mob types
        for (String key : requiredMobs.keySet()) {
            foundMobs.put(key, 0);
        }

        // Debug logging
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[BlueprintStatusTask] Required blocks: " + requiredBlocks);
            plugin.getLogger().info("[BlueprintStatusTask] Required mobs: " + requiredMobs);
        }

        // Scan the area for required blocks
        for (int x = 0; x < blueprint.getBlueprint().getSizeX(); x++) {
            for (int y = 0; y < blueprint.getBlueprint().getSizeY(); y++) {
                for (int z = 0; z < blueprint.getBlueprint().getSizeZ(); z++) {
                    Block block = world.getBlockAt(
                            loc.getBlockX() + x,
                            loc.getBlockY() + y,
                            loc.getBlockZ() + z
                    );

                    Material blockType = block.getType();

                    // Check each required block/definition
                    for (Map.Entry<String, Integer> entry : requiredBlocks.entrySet()) {
                        String key = entry.getKey();

                        if (plugin.getBlockDefinitionManager().isBlockDefinition(key)) {
                            // If it's a block definition, check if the current block matches any valid type
                            List<Material> validMaterials = plugin.getBlockDefinitionManager().getDefinition(key);
                            if (validMaterials.contains(blockType)) {
                                foundBlocks.merge(key, 1, Integer::sum);
                                break;
                            }
                        } else {
                            // Try direct material match
                            try {
                                Material requiredMaterial = Material.valueOf(key);
                                if (blockType == requiredMaterial) {
                                    foundBlocks.merge(key, 1, Integer::sum);
                                    break;
                                }
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid material name in blueprint: " + key);
                            }
                        }
                    }
                }
            }
        }

        // Check for required mobs in the area
        if (!requiredMobs.isEmpty()) {
            BoundingBox box = new BoundingBox(
                    loc.getX(), loc.getY(), loc.getZ(),
                    loc.getX() + blueprint.getBlueprint().getSizeX(),
                    loc.getY() + blueprint.getBlueprint().getSizeY(),
                    loc.getZ() + blueprint.getBlueprint().getSizeZ()
            );

            for (Entity entity : world.getNearbyEntities(box)) {
                String entityType = entity.getType().name();
                if (requiredMobs.containsKey(entityType)) {
                    foundMobs.merge(entityType, 1, Integer::sum);
                }
            }
        }
        boolean schematicMatched = true;

        Set<String> requiredSchematics = blueprint.getBlueprint().getRequiredSchematic();
        if (requiredSchematics != null && !requiredSchematics.isEmpty()) {
            schematicMatched = requiredSchematics.stream().anyMatch(schematicName ->
                    plugin.getSchematicUtil().matchesSchematic(schematicName, blueprint.getLocation())
            );

            if (!schematicMatched && plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("[BlueprintStatusTask] No required schematic matched for: " + requiredSchematics);
                plugin.getLogger().info("[BlueprintStatusTask] Blueprint at " + blueprint.getLocation() + " failed schematic requirement.");
            }
        }
        // Debug logging
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[BlueprintStatusTask] Found blocks: " + foundBlocks);
            plugin.getLogger().info("[BlueprintStatusTask] Found mobs: " + foundMobs);
        }

        // Check if all requirements are met
        boolean hasAllRequirements = schematicMatched &&
                requiredBlocks.entrySet().stream()
                .allMatch(entry -> {
                    String key = entry.getKey();
                    Integer required = entry.getValue();
                    Integer found = foundBlocks.get(key);

                    boolean matches = found != null && found >= required;
                    // Debug logging
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("[BlueprintStatusTask] Checking block " + key + ": required=" + required + ", found=" + found + ", matches=" + matches);
                    }
                    return matches;
                }) && requiredMobs.entrySet().stream()
                .allMatch(entry -> {
                    String key = entry.getKey();
                    Integer required = entry.getValue();
                    Integer found = foundMobs.get(key);

                    boolean matches = found != null && found >= required;
                    // Debug logging
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("[BlueprintStatusTask] Checking mob " + key + ": required=" + required + ", found=" + found + ", matches=" + matches);
                    }
                    return matches;
                });

        // Only update if the status has changed
        if (blueprint.isActive() != hasAllRequirements) {
            blueprint.setActive(hasAllRequirements);
            plugin.getBlueprintManager().saveAll();

            //update dynmap visualization
            if (plugin.getConfigManager().isDynmapEnabled()) {
                if (plugin.getDynmapListener() != null) {
                    plugin.getDynmapListener().updateBlueprintMarker(blueprint);
                }
            }
            // Update visualization for all players viewing this blueprint
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                if (player.getWorld().equals(world)) {
                    plugin.getPlacementHandler().updateVisualization(player, blueprint);
                }
            });
        }
    }

    private boolean isTownBlock(Location location, Town town) {
        try {
            TownBlock townBlock = TownyAPI.getInstance().getTownBlock(location);
            return townBlock != null && townBlock.getTown().equals(town);
        } catch (NotRegisteredException e) {
            return false;
        }
    }
}
