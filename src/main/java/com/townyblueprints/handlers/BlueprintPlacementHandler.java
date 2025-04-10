package com.townyblueprints.handlers;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.Blueprint;
import com.townyblueprints.models.PlacedBlueprint;
import com.townyblueprints.visualization.PlacementVisualizer;
import com.townyblueprints.visualization.ExistingBlueprintVisualizer;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.*;

public class BlueprintPlacementHandler {
    private final TownyBlueprints plugin;
    private final PlacementVisualizer placementVisualizer;
    private final ExistingBlueprintVisualizer existingVisualizer;

    @Getter
    private final Map<UUID, Blueprint> playerPlacements = new HashMap<>();
    private final Map<UUID, Location> selectedLocations = new HashMap<>();
    private final Map<UUID, Set<TownBlock>> selectedPlots = new HashMap<>();
    private final Map<UUID, Boolean> centerMode = new HashMap<>();
    private final Map<UUID, Boolean> visualizationMode = new HashMap<>();
    private final Map<UUID, Boolean> disabledVisualizationMode = new HashMap<>();

    public BlueprintPlacementHandler(TownyBlueprints plugin, PlacementVisualizer placementVisualizer, ExistingBlueprintVisualizer existingVisualizer) {
        this.plugin = plugin;
        this.placementVisualizer = placementVisualizer;
        this.existingVisualizer = existingVisualizer;
    }

    public void startPlacement(Player player, Blueprint blueprint) {
        Town town = TownyAPI.getInstance().getTown(player);
        if (town == null) {
            player.sendMessage("§cYou must be in a town to place blueprints!");
            return;
        }
        if (plugin.getConfigManager().isBuild_loadEnabled()) {
            // Check if placing this blueprint will exceed the town's load
            int additionalLoad = blueprint.getBuildLoad();
            if (!plugin.getTownBuildLoadManager().canTownSupportBuildLoad(town, additionalLoad)) {
                player.sendMessage("§cYour town cannot support the load of this blueprint.");
                return;
            }
        }
        // Check biome validity (both required and forbidden biomes)
        Location location = player.getLocation();
        if (!isValidBiome(location, blueprint)) {
            player.sendMessage("§cYou cannot place this blueprint in the current biome.");
            return;
        }
        cancelPlacement(player);

        playerPlacements.put(player.getUniqueId(), blueprint);
        centerMode.put(player.getUniqueId(), false);
        placementVisualizer.startVisualization(player, blueprint);

        player.sendMessage("§aLeft-click to select location, right-click to confirm placement.");
        player.sendMessage("§aUse /blueprint mode to toggle between corner and center placement.");
        player.sendMessage("§aUse /blueprint cancel to cancel placement.");
    }

    private boolean isValidBiome(Location location, Blueprint blueprint) {
        // Get the biome at the location
        Biome currentBiome = location.getBlock().getBiome();

        // Check if a required biome is specified in the blueprint
        if (!blueprint.getRequiredBiomes().isEmpty()) {
            // If the blueprint has required biomes, it must match one of them
            boolean matchesRequiredBiome = blueprint.getRequiredBiomes().contains(currentBiome.toString());
            if (!matchesRequiredBiome) {
                return false; // The biome doesn't match the required one
            }
        }

        // Check if the current biome is in the forbidden biomes list
        if (blueprint.getForbiddenBiomes().contains(currentBiome.toString())) {
            return false; // The biome is forbidden
        }

        return true; // Valid biome
    }

    private boolean checkTypeLimits(Town town, Blueprint blueprint) {
        // Get the current town level and load limits
        int townLevel = plugin.getConfigManager().getTownLevel(town);
        String type = blueprint.getType();

        // Get the type limit for this town level
        int typeLimit = plugin.getConfigManager().getTypeLimitForLevel(type, townLevel);
        if (typeLimit == -1) return true; // No type limit

        // Count existing blueprints of this type
        long count = plugin.getBlueprintManager().getPlacedBlueprintsForTown(town).stream()
                .filter(bp -> bp.getBlueprint().getType().equals(type))
                .count();

        if (count >= typeLimit) {
            return false;
        }
        if (plugin.getConfigManager().isBuild_loadEnabled()) {
            // Now check the overall load limit
            int currentLoad = plugin.getTownBuildLoadManager().getTownBuildLoad(town);
            int loadLimit = plugin.getTownBuildLoadManager().getTownBuildLoadLimit(town);
            int blueprintLoad = blueprint.getBuildLoad();

            if ((currentLoad + blueprintLoad) > loadLimit) {
                return false;
            }
        }
        return true;
    }


    public void togglePlacementMode(Player player) {
        UUID playerId = player.getUniqueId();
        Blueprint blueprint = playerPlacements.get(playerId);

        if (blueprint == null) {
            player.sendMessage("§cYou are not currently placing a blueprint!");
            return;
        }

        boolean newMode = !centerMode.getOrDefault(playerId, false);
        centerMode.put(playerId, newMode);
        selectedLocations.remove(playerId); // Reset selected location when changing modes

        player.sendMessage(newMode ?
                "§aSwitched to center placement mode" :
                "§aSwitched to corner placement mode");
    }

    public boolean isCenterMode(Player player) {
        return centerMode.getOrDefault(player.getUniqueId(), false);
    }

    public void toggleVisualizationMode(Player player) {
        UUID playerId = player.getUniqueId();
        Town town = TownyAPI.getInstance().getTown(player);
        if (town == null) {
            player.sendMessage("§cYou must be in a town to use visualization mode!");
            return;
        }

        try {
            if (!TownyAPI.getInstance().getResident(player).getTown().equals(town)) {
                player.sendMessage("§cYou can only visualize blueprints in your own town!");
                return;
            }

            boolean newMode = !visualizationMode.getOrDefault(playerId, false);
            visualizationMode.put(playerId, newMode);

            if (newMode) {
                // Get all blueprints for the town
                List<PlacedBlueprint> townBlueprints = new ArrayList<>(plugin.getBlueprintManager().getPlacedBlueprintsForTown(town));

                // Start visualization for all blueprints
                for (PlacedBlueprint blueprint : townBlueprints) {
                    existingVisualizer.startVisualization(player, blueprint, !blueprint.isActive());
                }
                player.sendMessage("§aBlueprint visualization mode enabled.");
            } else {
                existingVisualizer.stopVisualization(player);
                player.sendMessage("§cBlueprint visualization mode disabled.");
            }
        } catch (NotRegisteredException e) {
            player.sendMessage("§cYou must be a town resident to use visualization mode!");
        }
    }

    public void cancelPlacement(Player player) {
        UUID playerId = player.getUniqueId();
        playerPlacements.remove(playerId);
        selectedLocations.remove(playerId);
        selectedPlots.remove(playerId);
        centerMode.remove(playerId);
        placementVisualizer.stopVisualization(player);
    }

    public void handleLeftClick(Player player, Location location) {
        UUID playerId = player.getUniqueId();
        Blueprint blueprint = playerPlacements.get(playerId);
        if (blueprint == null) return;

        handleLocationSelection(player, location);
    }

    private void handleLocationSelection(Player player, Location location) {
        UUID playerId = player.getUniqueId();
        Blueprint blueprint = playerPlacements.get(playerId);

        // Check for overlapping blueprints
        if (isOverlappingExistingBlueprint(location, blueprint)) {
            player.sendMessage("§cThis location overlaps with an existing blueprint!");
            return;
        }

        // Adjust location based on placement mode
        if (centerMode.getOrDefault(playerId, false)) {
            location = location.clone().add(
                    -blueprint.getSizeX() / 2,
                    0,
                    -blueprint.getSizeZ() / 2
            );
        }

        selectedLocations.put(playerId, location);
        placementVisualizer.stopVisualization(player);
        placementVisualizer.startVisualization(player, blueprint);
        placementVisualizer.updateLocation(player, location);
        player.sendMessage("§aLocation selected! Right-click to confirm placement.");
    }

    public void updateVisualizationLocation(Player player, Location location) {
        UUID playerId = player.getUniqueId();
        Blueprint blueprint = playerPlacements.get(playerId);
        if (blueprint == null) return;

            // Only update location if no location is selected yet
            if (!selectedLocations.containsKey(playerId)) {
                if (centerMode.getOrDefault(playerId, false)) {
                    location = location.clone().add(
                            -blueprint.getSizeX() / 2,
                            0,
                            -blueprint.getSizeZ() / 2
                    );
                }
                placementVisualizer.updateLocation(player, location);
            }
    }

    public void stopVisualization(Player player) {
        UUID playerId = player.getUniqueId();
        visualizationMode.remove(playerId);
        disabledVisualizationMode.remove(playerId);
        placementVisualizer.stopVisualization(player);
        existingVisualizer.stopVisualization(player);
    }

    private boolean isOverlappingExistingBlueprint(Location location, Blueprint newBlueprint) {

        BoundingBox newBox = new BoundingBox(
                location.getX(), location.getY(), location.getZ(),
                location.getX() + newBlueprint.getSizeX(),
                location.getY() + newBlueprint.getSizeY(),
                location.getZ() + newBlueprint.getSizeZ()
        );

        Town town = TownyAPI.getInstance().getTown(location);
        if (town == null) return false;

        for (PlacedBlueprint existing : plugin.getBlueprintManager().getPlacedBlueprintsForTown(town)) {

            Location existingLoc = existing.getLocation();
            BoundingBox existingBox = new BoundingBox(
                    existingLoc.getX(), existingLoc.getY(), existingLoc.getZ(),
                    existingLoc.getX() + existing.getBlueprint().getSizeX(),
                    existingLoc.getY() + existing.getBlueprint().getSizeY(),
                    existingLoc.getZ() + existing.getBlueprint().getSizeZ()
            );

            if (newBox.overlaps(existingBox)) {
                return true;
            }
        }

        return false;
    }

    public boolean handleRightClick(Player player) {
        UUID playerId = player.getUniqueId();
        Blueprint blueprint = playerPlacements.get(playerId);

        if (blueprint == null) return false;

            Location location = selectedLocations.get(playerId);
            if (location == null) {
                player.sendMessage("§cPlease left-click to select a location first!");
                return false;
            }
            return tryPlaceBlueprint(player, location);
    }


    private boolean tryPlaceBlueprint(Player player, Location location) {
        Blueprint blueprint = playerPlacements.get(player.getUniqueId());
        if (blueprint == null) return false;

        Town town = TownyAPI.getInstance().getTown(location);
        if (town == null) {
            player.sendMessage("§cYou must place blueprints within a town!");
            return false;
        }

        // Check the type and load limits before placing the blueprint
        if (!checkTypeLimits(town, blueprint)) {
            player.sendMessage("§cThis blueprint exceeds the load or type limit for your town.");
            return false;
        }

        try {
            if (!TownyAPI.getInstance().getResident(player).hasTown() ||
                    !TownyAPI.getInstance().getResident(player).getTown().equals(town)) {
                player.sendMessage("§cYou don't have permission to place blueprints in this town!");
                return false;
            }

            PlacedBlueprint placedBlueprint = new PlacedBlueprint(
                    UUID.randomUUID().toString(),
                    blueprint,
                    town,
                    location,
                    false
            );

            String id = plugin.getBlueprintManager().createPlacedBlueprint(placedBlueprint);
            plugin.getTownBuildLoadManager().recalculateTownBuildLoad(town);

            cancelPlacement(player);

            player.sendMessage("§aBlueprint placed successfully!");
            return true;
        } catch (NotRegisteredException e) {
            player.sendMessage("§cError: You must be a resident of a town to place blueprints!");
            return false;
        }
    }


    private boolean checkTownBoundaries(Location location, Blueprint blueprint, Town town) {

        World world = location.getWorld();
        if (world == null) return false;

        // Check points along all edges with a spacing of 16 blocks (size of a town block)
        int spacing = 16;

        // Calculate number of points to check along each axis
        int pointsX = Math.max(2, (int) Math.ceil(blueprint.getSizeX() / (double) spacing) + 1);
        int pointsZ = Math.max(2, (int) Math.ceil(blueprint.getSizeZ() / (double) spacing) + 1);

        // Check bottom edges
        for (int i = 0; i < pointsX; i++) {
            double x = location.getX() + (i * blueprint.getSizeX() / (pointsX - 1));
            // Front edge
            if (!isTownBlock(new Location(world, x, location.getY(), location.getZ()), town)) {
                return false;
            }
            // Back edge
            if (!isTownBlock(new Location(world, x, location.getY(), location.getZ() + blueprint.getSizeZ()), town)) {
                return false;
            }
        }

        for (int i = 0; i < pointsZ; i++) {
            double z = location.getZ() + (i * blueprint.getSizeZ() / (pointsZ - 1));
            // Left edge
            if (!isTownBlock(new Location(world, location.getX(), location.getY(), z), town)) {
                return false;
            }
            // Right edge
            if (!isTownBlock(new Location(world, location.getX() + blueprint.getSizeX(), location.getY(), z), town)) {
                return false;
            }
        }

        // Check center points
        int centerPointsX = Math.max(1, pointsX - 2);
        int centerPointsZ = Math.max(1, pointsZ - 2);

        for (int i = 1; i < centerPointsX + 1; i++) {
            for (int j = 1; j < centerPointsZ + 1; j++) {
                double x = location.getX() + (i * blueprint.getSizeX() / (pointsX - 1));
                double z = location.getZ() + (j * blueprint.getSizeZ() / (pointsZ - 1));
                if (!isTownBlock(new Location(world, x, location.getY(), z), town)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isTownBlock(Location location, Town town) {
        try {
            TownBlock townBlock = TownyAPI.getInstance().getTownBlock(location);
            return townBlock != null && townBlock.getTown().equals(town);
        } catch (NotRegisteredException e) {
            return false;
        }
    }

    public void updateVisualization(Player player, PlacedBlueprint blueprint) {
        UUID playerId = player.getUniqueId();

        // Only update visualization if the player has visualization mode enabled
        if (visualizationMode.getOrDefault(playerId, false)) {
            if (blueprint.isActive()) {
                existingVisualizer.startVisualization(player, blueprint, false);
            } else {
                existingVisualizer.stopVisualization(player);
            }
        } else if (disabledVisualizationMode.getOrDefault(playerId, false)) {
            if (!blueprint.isActive()) {
                existingVisualizer.startVisualization(player, blueprint, true);
            } else {
                existingVisualizer.stopVisualization(player);
            }
        }
    }
}