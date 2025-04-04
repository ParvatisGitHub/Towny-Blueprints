package com.townyblueprints.tasks;

import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.PlacedBlueprint;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // Initialize counters for block definitions
        for (String key : requiredBlocks.keySet()) {
            foundBlocks.put(key, 0);
        }

        // Debug logging
        plugin.getLogger().info("[BlueprintStatusTask] Required blocks: " + requiredBlocks);

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
                                break; // Break once we've counted this block
                            }
                        } else {
                            // Try direct material match
                            try {
                                Material requiredMaterial = Material.valueOf(key);
                                if (blockType == requiredMaterial) {
                                    foundBlocks.merge(key, 1, Integer::sum);
                                    break; // Break once we've counted this block
                                }
                            } catch (IllegalArgumentException e) {
                                // Invalid material name, skip it
                                plugin.getLogger().warning("Invalid material name in blueprint: " + key);
                            }
                        }
                    }
                }
            }
        }

        // Debug logging
        plugin.getLogger().info("[BlueprintStatusTask] Found blocks: " + foundBlocks);

        // Check if all required blocks are present
        boolean hasAllBlocks = requiredBlocks.entrySet().stream()
                .allMatch(entry -> {
                    String key = entry.getKey();
                    Integer required = entry.getValue();
                    Integer found = foundBlocks.get(key);

                    boolean matches = found != null && found >= required;
                    plugin.getLogger().info("[BlueprintStatusTask] Checking " + key + ": required=" + required + ", found=" + found + ", matches=" + matches);
                    return matches;
                });

        // Only update if the status has changed
        if (blueprint.isActive() != hasAllBlocks) {
            blueprint.setActive(hasAllBlocks);
            plugin.getBlueprintManager().saveAll();

            // Update visualization for all players viewing this blueprint
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                if (player.getWorld().equals(world)) {
                    plugin.getPlacementHandler().updateVisualization(player, blueprint);
                }
            });
        }
    }

    private boolean checkTownBoundaries(PlacedBlueprint blueprint) {
        Location loc = blueprint.getLocation();
        World world = loc.getWorld();
        if (world == null) return false;

        // Check corners and center points
        int sizeX = blueprint.getBlueprint().getSizeX();
        int sizeZ = blueprint.getBlueprint().getSizeZ();

        // Check corners
        Location[] corners = {
                loc,
                loc.clone().add(sizeX, 0, 0),
                loc.clone().add(0, 0, sizeZ),
                loc.clone().add(sizeX, 0, sizeZ)
        };

        for (Location corner : corners) {
            if (!isTownBlock(corner, blueprint.getTown())) {
                return false;
            }
        }

        // Check center
        Location center = loc.clone().add(sizeX/2, 0, sizeZ/2);
        return isTownBlock(center, blueprint.getTown());
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