package com.townyblueprints.listeners;

import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.PlacedBlueprint;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.towny.events.BuildTownMarkerDescriptionEvent;
import org.dynmap.towny.events.TownSetMarkerIconEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DynmapListener implements Listener {
    private final TownyBlueprints plugin;
    private final MarkerAPI markerAPI;
    private final MarkerSet markerSet;
    private final Map<String, Marker> blueprintMarkers = new HashMap<>();

    public DynmapListener(TownyBlueprints plugin, MarkerAPI markerAPI) {
        this.plugin = plugin;
        this.markerAPI = markerAPI;

        // Create or get our marker set
        MarkerSet existingSet = markerAPI.getMarkerSet("townyblueprints.markerset");
        if (existingSet == null) {
            this.markerSet = markerAPI.createMarkerSet("townyblueprints.markerset", "TownyBlueprints", null, false);
        } else {
            this.markerSet = existingSet;
        }

        // Load existing blueprints
        updateAllBlueprints();
    }

    @EventHandler
    public void onBuildTownDescription(BuildTownMarkerDescriptionEvent event) {
        // Get blueprints for the specific town
        Collection<PlacedBlueprint> blueprints = plugin.getBlueprintManager().getPlacedBlueprintsForTown(event.getTown());

        if (!blueprints.isEmpty()) {
            StringBuilder description = new StringBuilder(event.getDescription());
            description.append("\n<br/>Active Blueprints:");

            double totalIncome = 0;
            for (PlacedBlueprint blueprint : blueprints) {
                if (blueprint.isActive()) {
                    description.append("\n<br/>- ").append(blueprint.getBlueprint().getName());
                    double dailyIncome = blueprint.getBlueprint().getDailyIncome();
                    if (dailyIncome > 0) {
                        description.append(" (").append(dailyIncome).append(" ")
                                .append(blueprint.getBlueprint().getIncomeType()).append("/day)");

                        if ("MONEY".equals(blueprint.getBlueprint().getIncomeType())) {
                            totalIncome += dailyIncome;
                        }
                    }
                }
            }

            if (totalIncome > 0) {
                description.append("\n<br/>Total Daily Income: ").append(totalIncome).append(" coins");
            }

            event.setDescription(description.toString());
        }
    }

    @EventHandler
    public void onTownSetMarkerIcon(TownSetMarkerIconEvent event) {
        // Customize the icon for the town based on the blueprints
        Collection<PlacedBlueprint> blueprints = plugin.getBlueprintManager().getPlacedBlueprintsForTown(event.getTown());

        // Example: change icon if town has a specific blueprint type
        for (PlacedBlueprint blueprint : blueprints) {
            if (blueprint.isActive() && blueprint.getBlueprint().getType().equals("government")) {
                // Set a custom icon for the town based on blueprint type
                MarkerIcon icon = markerAPI.getMarkerIcon(plugin.getConfig().getString("dynmap.icons.government", "tower"));
                event.setIcon(icon);
                break; // Only change the icon once based on blueprint type
            }
        }
    }

    public void updateAllBlueprints() {
        // Clear existing markers
        for (Marker marker : blueprintMarkers.values()) {
            marker.deleteMarker();
        }
        blueprintMarkers.clear();

        // Add markers for all active blueprints
        for (PlacedBlueprint blueprint : plugin.getBlueprintManager().getAllPlacedBlueprints()) {
            if (blueprint.isActive()) {
                addBlueprintMarker(blueprint);
            }
        }
    }

    public void addBlueprintMarker(PlacedBlueprint blueprint) {
        // Get blueprint location
        double x = blueprint.getLocation().getBlockX();
        double y = blueprint.getLocation().getBlockY();
        double z = blueprint.getLocation().getBlockZ();
        // Calculate the center of the blueprint
        double centerX = x + (blueprint.getBlueprint().getSizeX() / 2.0);
        double centerY = y + (blueprint.getBlueprint().getSizeY() / 2.0); //for vertical placement
        double centerZ = z + (blueprint.getBlueprint().getSizeZ() / 2.0);
        // Create or update marker
        String markerId = "blueprint_" + blueprint.getId();
        Marker marker = blueprintMarkers.get(markerId);

        if (marker == null) {
            MarkerIcon icon = markerAPI.getMarkerIcon(plugin.getConfig().getString("dynmap.icons." + blueprint.getBlueprint().getType(), "default"));

            marker = markerSet.createMarker(
                    markerId,
                    blueprint.getBlueprint().getName(),
                    blueprint.getLocation().getWorld().getName(),  // World name
                    centerX, centerY, centerZ,  // Coordinates (x, y, z) for the point marker
                    icon,      // MarkerIcon
                    true       // Is the marker persistent? (true means it won't be removed on reload)
            );
            blueprintMarkers.put(markerId, marker);
        } else {
            // Updating the location correctly with world name and coordinates
            marker.setLocation(blueprint.getLocation().getWorld().getName(), centerX, centerY, centerZ);
        }

        // Set marker description with blueprint information
        String description = String.format("<div class=\"blueprint-info\">" +
                        "<h3>%s</h3>" +
                        "<p>Type: %s</p>" +
                        "<p>Income: %s %s/day</p>" +
                        "<p>Upkeep: %s %s/day</p>" +
                        "</div>",
                blueprint.getBlueprint().getName(),
                blueprint.getBlueprint().getType(),
                blueprint.getBlueprint().getDailyIncome(),
                blueprint.getBlueprint().getIncomeType(),
                blueprint.getBlueprint().getDailyUpkeep(),
                blueprint.getBlueprint().getUpkeepType());

        marker.setDescription(description);
    }
    public void updateBlueprintMarker(PlacedBlueprint blueprint) {
        if (blueprint.isActive()) {
            addBlueprintMarker(blueprint);  // Add marker if blueprint is active
        } else {
            removeBlueprintMarker(blueprint.getId());  // Remove marker if blueprint is disabled
        }
    }

    public void removeBlueprintMarker(String blueprintId) {
        String markerId = "blueprint_" + blueprintId;
        Marker marker = blueprintMarkers.remove(markerId);
        if (marker != null) {
            marker.setDescription("<div class=\"blueprint-info\"><h3></h3></div>");
            marker.deleteMarker();
        }
    }
}
