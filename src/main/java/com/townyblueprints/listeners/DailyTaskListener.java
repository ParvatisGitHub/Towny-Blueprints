package com.townyblueprints.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.PlacedBlueprint;
import com.townyblueprints.models.ResourceTemplate;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class DailyTaskListener implements Listener {
    private final TownyBlueprints plugin;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNewDay(NewDayEvent event) {
        if (!isProcessing.compareAndSet(false, true)) {
            // Debug logging
            if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                plugin.getLogger().warning("Daily tasks are already being processed!");
            }
            return;
        }

        try {
            // Refresh warehouse data first
            // Debug logging
            if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Refreshing warehouse data...");
            }
            plugin.getWarehouseManager().loadWarehouses();
            // Debug logging
            if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Warehouse data refresh complete");
}
            List<PlacedBlueprint> blueprints = new ArrayList<>(plugin.getBlueprintManager().getAllPlacedBlueprints());

            // Group blueprints by town for combined messages
            Map<UUID, List<PlacedBlueprint>> townBlueprints = new HashMap<>();
            for (PlacedBlueprint blueprint : blueprints) {
                townBlueprints.computeIfAbsent(blueprint.getTown().getUUID(), k -> new ArrayList<>()).add(blueprint);
            }

            // Process each town's blueprints
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    for (Map.Entry<UUID, List<PlacedBlueprint>> entry : townBlueprints.entrySet()) {
                        Map<String, Integer> totalUpkeep = new HashMap<>();
                        Map<String, Integer> toolUpkeep = new HashMap<>();
                        Town town = entry.getValue().get(0).getTown();

                        for (PlacedBlueprint blueprint : entry.getValue()) {
                            try {
                                processUpkeepAndCollectTotals(blueprint, totalUpkeep, toolUpkeep);
                            } catch (Exception e) {
                                plugin.getLogger().severe("Error processing blueprint " + blueprint.getId() + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        // Update town's bonus blocks
                        int currentBonusBlocks = town.getBonusBlocks();
                        plugin.getBlueprintManager().updateTownBonusBlocks(town, currentBonusBlocks);

                        // Send combined upkeep message for the town if there are any resources
                        if (!totalUpkeep.isEmpty() || !toolUpkeep.isEmpty()) {
                            sendCombinedUpkeepMessage(town, totalUpkeep, toolUpkeep);
                        }
                    }
                } finally {
                    isProcessing.set(false);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("Error during daily task processing: " + e.getMessage());
            e.printStackTrace();
            isProcessing.set(false);
        }
    }

    // Rest of the class remains unchanged
    private void processUpkeepAndCollectTotals(PlacedBlueprint blueprint, Map<String, Integer> totalUpkeep, Map<String, Integer> toolUpkeep) {
        try {
            String upkeepType = blueprint.getBlueprint().getUpkeepType();

            // Handle template-based upkeep
            if (upkeepType.startsWith("template:")) {
                String templateName = upkeepType.substring(9); // Remove "template:" prefix
                ResourceTemplate template = plugin.getResourceTemplateManager().getTemplate(templateName);
                if (template != null) {
                    for (ResourceTemplate.ResourceEntry resource : template.getResources()) {
                        if (resource.getType().equals("TOOL")) {
                            // Add tool upkeep to the separate map
                            toolUpkeep.merge(resource.getToolType(), resource.getDurabilityDrain(), Integer::sum);
                        } else {
                            // Add regular resource upkeep
                            totalUpkeep.merge(resource.getType(), resource.getMaxAmount(), Integer::sum);
                        }
                    }
                }
            } else if (upkeepType.equals("TOOL")) {
                // Handle standard tool upkeep
                double upkeepAmount = blueprint.getBlueprint().getDailyUpkeep();
                Material toolMaterial = blueprint.getBlueprint().getToolType();
                String toolType = toolMaterial != null ? toolMaterial.name() : "UNKNOWN";
                toolUpkeep.merge(toolType, (int) upkeepAmount, Integer::sum);
            } else {
                // Handle standard upkeep (money or items)
                double upkeepAmount = blueprint.getBlueprint().getDailyUpkeep();
                totalUpkeep.merge(upkeepType, (int) upkeepAmount, Integer::sum);
            }

            // Process the upkeep
            boolean upkeepSuccess = plugin.getUpkeepHandler().processUpkeep(blueprint);
            blueprint.setSuccessfulUpkeep(upkeepSuccess);

            // If upkeep failed, deactivate the blueprint
            if (!upkeepSuccess) {
                blueprint.setActive(false);
                plugin.getBlueprintManager().saveAll();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error processing upkeep for blueprint " + blueprint.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendCombinedUpkeepMessage(Town town, Map<String, Integer> totalUpkeep, Map<String, Integer> toolUpkeep) {
        if (totalUpkeep.isEmpty() && toolUpkeep.isEmpty()) return;

        StringBuilder message = new StringBuilder("§aYour town has paid an upkeep of ");
        List<String> upkeepParts = new ArrayList<>();

        // Handle money upkeep
        if (totalUpkeep.containsKey("MONEY")) {
            upkeepParts.add(String.format("§6%d " + plugin.getConfigManager().getCurrencyName() +, totalUpkeep.get("MONEY")));
            totalUpkeep.remove("MONEY");
        }

        // Handle tool upkeep
        for (Map.Entry<String, Integer> entry : toolUpkeep.entrySet()) {
            String toolType = entry.getKey().toLowerCase().replace("_", " ");
            upkeepParts.add(String.format("§6%d %s durability", entry.getValue(), toolType));
        }

        // Handle remaining resource upkeep
        for (Map.Entry<String, Integer> entry : totalUpkeep.entrySet()) {
            String resourceType = entry.getKey();
            String displayName = formatResourceType(resourceType);
            upkeepParts.add(String.format("§6%d %s", entry.getValue(), displayName));
        }

        message.append(String.join("§a, ", upkeepParts));
        message.append("§a to maintain its structures!");

        // Send message to all online town residents
        for (Resident resident : town.getResidents()) {
            Player player = TownyAPI.getInstance().getPlayer(resident);
            if (player != null && player.isOnline()) {
                player.sendMessage(message.toString());
            }
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

    private void sendMessageToTownPlayers(PlacedBlueprint blueprint, Component message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> sendMessageToTownPlayers(blueprint, message));
            return;
        }

        for (Resident resident : blueprint.getTown().getResidents()) {
            Player player = TownyAPI.getInstance().getPlayer(resident);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
}
