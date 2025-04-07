package com.townyblueprints.listeners;

import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.PlacedBlueprint;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.dynmap.markers.AreaMarker;
import org.dynmap.towny.events.BuildTownMarkerDescriptionEvent;
import org.dynmap.towny.events.TownSetMarkerIconEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DynmapListener implements Listener {
    private final TownyBlueprints plugin;
    private final MarkerAPI markerAPI;
    private final MarkerSet markerSet;
    private final Map<String, AreaMarker> blueprintMarkers = new HashMap<>();

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
        Collection<PlacedBlueprint> blueprints = plugin.getBlueprintManager().getPlacedBlueprintsForTown(event.getTown());
        if (!blueprints.isEmpty()) {
            StringBuilder description = new StringBuilder(event.getDescription());
            description.append("\n<br/>Active Blueprints:");
            
            double totalIncome = 0;
            for (PlacedBlueprint blueprint : blueprints) {
                if (blueprint.isActive()) {
                    description.append("\n<br/>- ").append(blueprint.getBlueprint().getName());
                    if (blueprint.getBlueprint().getDailyIncome() > 0) {
                        description.append(" (").append(blueprint.getBlueprint().getDailyIncome())
                            .append(" ").append(blueprint.getBlueprint().getIncomeType()).append("/day)");
                        if (blueprint.getBlueprint().getIncomeType().equals("MONEY")) {
                            totalIncome += blueprint.getBlueprint().getDailyIncome();
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
        // You can customize the marker icon based on the blueprints in the town
        Collection<PlacedBlueprint> blueprints = plugin.getBlueprintManager().getPlacedBlueprintsForTown(event.getTown());
        // Example: Change icon if town has a specific type of blueprint
        for (PlacedBlueprint blueprint : blueprints) {
            if (blueprint.isActive() && blueprint.getBlueprint().getType().equals("government")) {
                event.setIcon(markerAPI.getMarkerIcon(plugin.getConfig().getString("dynmap.icons.government", "tower")));
                break;
            }
        }
    }

    public void updateAllBlueprints() {
        // Clear existing markers
        for (AreaMarker marker : blueprintMarkers.values()) {
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
        // Create corner points for the area
        double[] x = new double[4];
        double[] z = new double[4];
        
        // Get blueprint location and dimensions
        int bx = blueprint.getLocation().getBlockX();
        int bz = blueprint.getLocation().getBlockZ();
        int sizeX = blueprint.getBlueprint().getSizeX();
        int sizeZ = blueprint.getBlueprint().getSizeZ();
        
        // Set corner points
        x[0] = bx; z[0] = bz;
        x[1] = bx + sizeX; z[1] = bz;
        x[2] = bx + sizeX; z[2] = bz + sizeZ;
        x[3] = bx; z[3] = bz + sizeZ;
        
        // Create or update marker
        String markerId = "blueprint_" + blueprint.getId();
        AreaMarker marker = blueprintMarkers.get(markerId);
        
        if (marker == null) {
            marker = markerSet.createAreaMarker(markerId, 
                blueprint.getBlueprint().getName(), 
                true, 
                blueprint.getLocation().getWorld().getName(), 
                x, z, 
                false);
            blueprintMarkers.put(markerId, marker);
        } else {
            marker.setCornerLocations(x, z);
        }
        
        // Set marker style
        String markerIcon = plugin.getConfig().getString("dynmap.icons." + blueprint.getBlueprint().getType(), "default");
        String strokeColor = plugin.getConfig().getString("dynmap.colors." + blueprint.getBlueprint().getType() + ".stroke", "#FF0000");
        String fillColor = plugin.getConfig().getString("dynmap.colors." + blueprint.getBlueprint().getType() + ".fill", "#FF0000");
        double opacity = plugin.getConfig().getDouble("dynmap.colors." + blueprint.getBlueprint().getType() + ".opacity", 0.3);
        
        marker.setLineStyle(3, 1.0, Integer.parseInt(strokeColor.substring(1), 16));
        marker.setFillStyle(opacity, Integer.parseInt(fillColor.substring(1), 16));
        
        // Set description
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

    public void removeBlueprintMarker(String blueprintId) {
        String markerId = "blueprint_" + blueprintId;
        AreaMarker marker = blueprintMarkers.remove(markerId);
        if (marker != null) {
            marker.deleteMarker();
        }
    }
}