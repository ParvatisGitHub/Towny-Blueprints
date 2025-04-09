package com.townyblueprints.listeners;

import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translator;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.PlacedBlueprint;
import com.townyblueprints.models.ResourceTemplate;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.*;

@RequiredArgsConstructor
public class TownStatusListener implements Listener {
    private final TownyBlueprints plugin;

    @EventHandler
    public void onTownStatusScreen(TownStatusScreenEvent event) {
        Town town = event.getTown();
        Collection<PlacedBlueprint> blueprints = plugin.getBlueprintManager().getPlacedBlueprintsForTown(town);
        if (blueprints.isEmpty()) return;

        StringBuilder status = new StringBuilder();
        boolean addedHeader = false;

        // Calculate total upkeep by type
        Map<String, Double> totalUpkeep = new HashMap<>();
        Map<String, Integer> toolUpkeep = new HashMap<>();

        for (PlacedBlueprint bp : blueprints) {
            if (bp.isActive()) {
                String upkeepType = bp.getBlueprint().getUpkeepType();
                if (upkeepType.startsWith("template:")) {
                    String templateName = upkeepType.substring(9);
                    ResourceTemplate template = plugin.getResourceTemplateManager().getTemplate(templateName);
                    if (template != null) {
                        for (ResourceTemplate.ResourceEntry resource : template.getResources()) {
                            if (resource.getType().equals("TOOL")) {
                                toolUpkeep.merge(resource.getToolType(), resource.getDurabilityDrain(), Integer::sum);
                            } else {
                                // Use maxAmount for display
                                totalUpkeep.merge(resource.getType(), (double) resource.getMaxAmount(), Double::sum);
                            }
                        }
                    }
                } else {
                    totalUpkeep.merge(upkeepType, bp.getBlueprint().getDailyUpkeep(), Double::sum);
                }
            }
        }

        // Add upkeep information if any exists
        if (!totalUpkeep.isEmpty() || !toolUpkeep.isEmpty()) {
            if (!addedHeader) {
                status.append("\n§2Blueprints:");
                addedHeader = true;
            }
            status.append("\n§2Daily Upkeep:");

            // Show regular resource upkeep
            for (Map.Entry<String, Double> entry : totalUpkeep.entrySet()) {
                status.append(String.format("\n  §2%s: §c%s",
                        formatResourceType(entry.getKey()),
                        String.format("%.0f", entry.getValue())));
            }

            // Show tool upkeep
            for (Map.Entry<String, Integer> entry : toolUpkeep.entrySet()) {
                status.append(String.format("\n  §2%s durability: §c%d",
                        entry.getKey().toLowerCase().replace("_", " "),
                        entry.getValue()));
            }
        }

        // Check for available collections
        boolean hasCollectableResources = blueprints.stream()
                .filter(PlacedBlueprint::isActive)
                .anyMatch(bp -> bp.getLastCollectionTime() < plugin.getResourceCollectionHandler().getLastNewDay());

        // Add collection notification
        if (hasCollectableResources) {
            if (!addedHeader) {
                status.append("\n§2Blueprints:");
                addedHeader = true;
            }
            status.append("\n§aResources available for collection!");
        }

        // Add bonus blocks info
        int bonusBlocks = plugin.getBlueprintManager().calculateTownBonusBlocks(town);
        if (bonusBlocks > 0) {
            if (!addedHeader) {
                status.append("\n§2Blueprints:");
                addedHeader = true;
            }
            status.append("\n§2Bonus Town Blocks: §a+").append(bonusBlocks);
        }

        // Only add the component if we have something to show
        if (addedHeader) {
            event.getStatusScreen().addComponentOf("TownyBlueprints", status.toString());
        }
    }

    private String formatResourceType(String type) {
        if (type.equals("MONEY")) return plugin.getConfigManager().getCurrencyName();
        if (type.equals("TOOL")) return "tool durability";
        if (type.startsWith("vanilla:")) {
            return type.substring(8).toLowerCase().replace("_", " ");
        }
        if (type.startsWith("template:")) {
            return type.substring(9).toLowerCase().replace("_", " ");
        }
        return type.toLowerCase().replace("_", " ");
    }
}