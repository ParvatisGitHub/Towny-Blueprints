package com.townyblueprints.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Resident;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.Blueprint;
import com.townyblueprints.models.PlacedBlueprint;
import com.townyblueprints.models.ResourceTemplate;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Collection;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BlueprintCommand implements CommandExecutor, TabCompleter {
    private final TownyBlueprints plugin;
    private final Map<UUID, String> pendingRemovals = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("townyblueprints.use")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            showBlueprintStatus(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                plugin.getGuiManager().openBlueprintMenu(player);
                break;
            case "place":
                handlePlace(player, args);
                break;
            case "collect":
                handleCollect(player, args);
                break;
            case "cancel":
                plugin.getPlacementHandler().cancelPlacement(player);
                player.sendMessage("§aBlueprint placement cancelled.");
                break;
            case "mode":
                plugin.getPlacementHandler().togglePlacementMode(player);
                break;
            case "visualize":
                handleVisualize(player, args);
                break;
            case "remove":
                handleRemove(player);
                break;
            case "upgrade":
                handleUpgrade(player);
                break;
            default:
                showBlueprintStatus(player);
                break;
        }

        return true;
    }

    private void showBlueprintStatus(Player player) {
        Town town = TownyAPI.getInstance().getTown(player.getLocation());
        if (town == null) {
            player.sendMessage("§cYou must be in a town to view blueprint status!");
            return;
        }

        // Get all blueprints for this town
        Collection<PlacedBlueprint> blueprints = plugin.getBlueprintManager().getPlacedBlueprintsForTown(town);
        if (blueprints.isEmpty()) {
            player.sendMessage("§cYour town has no blueprints!");
            return;
        }

        // Group blueprints by type
        Map<String, List<PlacedBlueprint>> blueprintsByType = blueprints.stream()
                .collect(Collectors.groupingBy(bp -> bp.getBlueprint().getType()));

        player.sendMessage("§6=== §eTown Blueprint Status §6===");

        // Show blueprint counts by type
        for (Map.Entry<String, List<PlacedBlueprint>> entry : blueprintsByType.entrySet()) {
            String type = entry.getKey();
            List<PlacedBlueprint> typeBlueprints = entry.getValue();
            long activeCount = typeBlueprints.stream().filter(PlacedBlueprint::isActive).count();

            player.sendMessage(String.format("§2%s: §7%d/%d §a(Active/Total)",
                    capitalizeType(type), activeCount, typeBlueprints.size()));

            // Show individual blueprints of this type
            for (PlacedBlueprint bp : typeBlueprints) {
                String status = bp.isActive() ? "§2✔" : "§c✘";
                player.sendMessage(String.format("  %s §a%s", status, bp.getBlueprint().getName()));

                // Show income and upkeep if active
                if (bp.isActive()) {
                    // Handle income
                    String incomeType = bp.getBlueprint().getIncomeType();
                    if (incomeType.startsWith("template:")) {
                        String templateName = incomeType.substring(9);
                        ResourceTemplate template = plugin.getResourceTemplateManager().getTemplate(templateName);
                        if (template != null) {
                            player.sendMessage("    §2Income: §a(Resource Template)");
                            for (ResourceTemplate.ResourceEntry resource : template.getResources()) {
                                player.sendMessage(String.format("      §2- §a%d %s",
                                        resource.getMaxAmount(),
                                        formatResourceType(resource.getType())));
                            }
                        }
                    } else {
                        player.sendMessage(String.format("    §2Income: §a%s %s",
                                bp.getBlueprint().getDailyIncome(),
                                formatResourceType(bp.getBlueprint().getIncomeType())));
                    }

                    // Handle upkeep
                    String upkeepType = bp.getBlueprint().getUpkeepType();
                    if (upkeepType.startsWith("template:")) {
                        String templateName = upkeepType.substring(9);
                        ResourceTemplate template = plugin.getResourceTemplateManager().getTemplate(templateName);
                        if (template != null) {
                            player.sendMessage("    §2Upkeep: §c(Resource Template)");
                            for (ResourceTemplate.ResourceEntry resource : template.getResources()) {
                                if (resource.getType().equals("TOOL")) {
                                    player.sendMessage(String.format("      §2- §c%d %s durability",
                                            resource.getDurabilityDrain(),
                                            resource.getToolType().toLowerCase().replace("_", " ")));
                                } else {
                                    player.sendMessage(String.format("      §2- §c%d %s",
                                            resource.getMaxAmount(),
                                            formatResourceType(resource.getType())));
                                }
                            }
                        }
                    } else {
                        player.sendMessage(String.format("    §2Upkeep: §c%s %s",
                                bp.getBlueprint().getDailyUpkeep(),
                                formatResourceType(bp.getBlueprint().getUpkeepType())));
                    }
                }
            }
        }

        // Calculate and show total upkeep by type
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
                                totalUpkeep.merge(resource.getType(), (double) resource.getMaxAmount(), Double::sum);
                            }
                        }
                    }
                } else {
                    totalUpkeep.merge(upkeepType, bp.getBlueprint().getDailyUpkeep(), Double::sum);
                }
            }
        }

        if (!totalUpkeep.isEmpty() || !toolUpkeep.isEmpty()) {
            player.sendMessage("\n§6=== §eTotal Daily Upkeep: §6===");

            // Show regular resource upkeep
            for (Map.Entry<String, Double> entry : totalUpkeep.entrySet()) {
                player.sendMessage(String.format("  §2%s: §c%s",
                        formatResourceType(entry.getKey()),
                        String.format("%.0f", entry.getValue())));
            }

            // Show tool upkeep
            for (Map.Entry<String, Integer> entry : toolUpkeep.entrySet()) {
                player.sendMessage(String.format("  §2%s durability: §c%d",
                        entry.getKey().toLowerCase().replace("_", " "),
                        entry.getValue()));
            }
        }

        // Show collection status after upkeep
        boolean hasCollectableResources = blueprints.stream()
                .filter(PlacedBlueprint::isActive)
                .anyMatch(bp -> bp.getLastCollectionTime() < plugin.getResourceCollectionHandler().getLastNewDay());

        if (hasCollectableResources) {
            player.sendMessage("\n§aResources are available for collection!");
        }

        // Show bonus blocks
        int bonusBlocks = plugin.getBlueprintManager().calculateTownBonusBlocks(town);
        if (bonusBlocks > 0) {
            player.sendMessage(String.format("\n§72Total Bonus Town Blocks: §a+%d", bonusBlocks));
        }
    }

    private void handleUpgrade(Player player) {
        // Get the town at the player's location
        Town town = TownyAPI.getInstance().getTown(player.getLocation());
        if (town == null) {
            player.sendMessage("§cYou must be in a town to upgrade blueprints!");
            return;
        }

        // Get the resident and check if they're in the town
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null || !resident.hasTown() || !resident.getTownOrNull().equals(town)) {
            player.sendMessage("§cYou can only upgrade blueprints in your own town!");
            return;
        }

        if (!resident.isMayor() && !player.hasPermission("townyblueprints.upgrade") && !player.hasPermission("townyblueprints.assistant")) {
            player.sendMessage("§cOnly the mayor or assistants can upgrade blueprints!");
            return;
        }

        // Find blueprint at player's location
        PlacedBlueprint blueprint = findBlueprintAtLocation(player.getLocation());
        if (blueprint == null) {
            player.sendMessage("§cYou must be standing in a blueprint to upgrade it!");
            return;
        }

        if (!blueprint.getTown().equals(town)) {
            player.sendMessage("§cYou can only upgrade blueprints in your own town!");
            return;
        }

        // Check if blueprint has an upgrade path
        String upgradePath = blueprint.getBlueprint().getUpgradesTo();
        if (upgradePath == null) {
            player.sendMessage("§cThis blueprint cannot be upgraded further!");
            return;
        }

        // Get the upgrade blueprint
        Blueprint upgradeBlueprint = plugin.getBlueprintManager().getBlueprint(upgradePath);
        if (upgradeBlueprint == null) {
            player.sendMessage("§cUpgrade blueprint not found!");
            return;
        }

        // Check town level requirement
        int townLevel = plugin.getConfigManager().getTownLevel(town);
        if (upgradeBlueprint.getRequiredTownLevel() > townLevel) {
            player.sendMessage(String.format("§cYour town must be level %d to upgrade to this blueprint!",
                    upgradeBlueprint.getRequiredTownLevel()));
            return;
        }

        // Check if player has permission for the upgrade
        if (!player.hasPermission(upgradeBlueprint.getPermissionNode())) {
            player.sendMessage("§cYou don't have permission to upgrade to this blueprint!");
            return;
        }

        // Check if town can afford the upgrade
        double upgradeCost = blueprint.getBlueprint().getUpgradeCost();
        if (upgradeCost > town.getAccount().getHoldingBalance()) {
            player.sendMessage(String.format("§cYour town cannot afford the upgrade cost of %.2f!", upgradeCost));
            return;
        }

        // Process refund if enabled
        if (plugin.getConfig().getBoolean("economy.upgrades.refund_on_upgrade", true)) {
            double refundPercentage = plugin.getConfig().getDouble("economy.upgrades.upgrade_refund_percentage", 50) / 100.0;
            double refundAmount = blueprint.getBlueprint().getPlacementCost() * refundPercentage;

            if (refundAmount > 0) {
                town.getAccount().deposit(refundAmount, "Blueprint upgrade refund");
                player.sendMessage(String.format("§aYou have been refunded §6%.2f %s§a!", refundAmount, plugin.getConfigManager().getCurrencyName()));
            }
        }

        // Charge upgrade cost
        town.getAccount().withdraw(upgradeCost, "Blueprint upgrade cost");

        // Create new blueprint at same location
        PlacedBlueprint upgradedBlueprint = new PlacedBlueprint(
                UUID.randomUUID().toString(),
                upgradeBlueprint,
                town,
                blueprint.getLocation(),
                false
        );

        // Remove old blueprint
        plugin.getBlueprintManager().removePlacedBlueprint(blueprint.getId());

        // Place new blueprint
        plugin.getBlueprintManager().createPlacedBlueprint(upgradedBlueprint);

        player.sendMessage(String.format("§aBlueprint upgraded to %s successfully!", upgradeBlueprint.getName()));
    }

    private void handleVisualize(Player player, String[] args) {
        // Get the town at the player's location
        Town town = TownyAPI.getInstance().getTown(player.getLocation());
        if (town == null) {
            player.sendMessage("§cYou must be in a town to use visualization mode!");
            return;
        }

        // Get the resident and check if they're in the town
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null || !resident.hasTown() || !resident.getTownOrNull().equals(town)) {
            player.sendMessage("§cYou can only visualize blueprints in your own town!");
            return;
        }

        // Toggle visualization mode
        plugin.getPlacementHandler().toggleVisualizationMode(player);
    }

    private void handleRemove(Player player) {
        // Get the town at the player's location
        Town town = TownyAPI.getInstance().getTown(player.getLocation());
        if (town == null) {
            player.sendMessage("§cYou must be in a town to remove blueprints!");
            return;
        }

        // Get the resident and check if they're in the town
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null || !resident.hasTown() || !resident.getTownOrNull().equals(town)) {
            player.sendMessage("§cYou can only remove blueprints in your own town!");
            return;
        }

        if (!resident.isMayor() && !player.hasPermission("townyblueprints.remove") && !player.hasPermission("townyblueprints.assistant")) {
            player.sendMessage("§cOnly the mayor or assistants can remove blueprints!");
            return;
        }

        // Find blueprint at player's location
        PlacedBlueprint blueprint = findBlueprintAtLocation(player.getLocation());
        if (blueprint == null) {
            player.sendMessage("§cYou must be standing in a blueprint to remove it!");
            return;
        }

        if (!blueprint.getTown().equals(town)) {
            player.sendMessage("§cYou can only remove blueprints in your own town!");
            return;
        }

        // Process refund if enabled
        if (plugin.getConfig().getBoolean("economy.refund.enabled", true)) {
            double refundPercentage = plugin.getConfig().getDouble("economy.refund.percentage", 75) / 100.0;
            double refundAmount = blueprint.getBlueprint().getPlacementCost() * refundPercentage;

            if (refundAmount > 0) {
                town.getAccount().deposit(refundAmount, "Blueprint removal refund");
                player.sendMessage(String.format("§aYou have been refunded §6%.2f %s§a!", refundAmount, plugin.getConfigManager().getCurrencyName()));
            }
        }

        // Stop any active visualizations for this blueprint
        plugin.getPlacementHandler().stopVisualization(player);

        // Remove the blueprint
        plugin.getBlueprintManager().removePlacedBlueprint(blueprint.getId());
        player.sendMessage("§aBlueprint removed successfully!");
    }

    private void handlePlace(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /blueprint place <name>");
            return;
        }

        String blueprintName = args[1];
        var blueprint = plugin.getBlueprintManager().getBlueprint(blueprintName);

        if (blueprint == null) {
            player.sendMessage("§cBlueprint not found: " + blueprintName);
            return;
        }

        if (!player.hasPermission(blueprint.getPermissionNode())) {
            player.sendMessage("§cYou don't have permission to use this blueprint!");
            return;
        }

        plugin.getPlacementHandler().startPlacement(player, blueprint);
    }

    private void handleCollect(Player player, String[] args) {
        // Get the town at the player's location
        Town town = TownyAPI.getInstance().getTown(player.getLocation());
        if (town == null) {
            player.sendMessage("§cYou must be in a town to collect resources!");
            return;
        }

        // Get the resident and check if they're in the town
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null || !resident.hasTown() || !resident.getTownOrNull().equals(town)) {
            player.sendMessage("§cYou can only collect resources in your own town!");
            return;
        }

        if (!resident.isMayor() && !player.hasPermission("townyblueprints.collect")) {
            player.sendMessage("§cOnly the mayor or assistants can collect resources!");
            return;
        }

        // Handle different collection modes
        if (args.length > 1) {
            String type = args[1].toLowerCase();

            if (type.equals("all")) {
                // Collect from all blueprints
                plugin.getResourceCollectionHandler().collectResources(player, town, null);
                return;
            }

            // Collect from specific type
            plugin.getResourceCollectionHandler().collectResources(player, town, type);
            return;
        }

        // If no arguments, try to collect from blueprint at location first
        PlacedBlueprint blueprint = findBlueprintAtLocation(player.getLocation());
        if (blueprint != null) {
            plugin.getResourceCollectionHandler().collectResourcesAtLocation(player, player.getLocation());
        } else {
            // If no blueprint at location, collect from all
            plugin.getResourceCollectionHandler().collectResources(player, town, null);
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

    private String capitalizeType(String type) {
        if (type == null || type.isEmpty()) return "Default";
        return type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
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
        return type;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("list");
            completions.add("place");
            completions.add("collect");
            completions.add("cancel");
            completions.add("mode");
            if (sender.hasPermission("townyblueprints.visualize")) {
                completions.add("visualize");
            }
            if (sender.hasPermission("townyblueprints.remove")) {
                completions.add("remove");
            }
            if (sender.hasPermission("townyblueprints.upgrade")) {
                completions.add("upgrade");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("place")) {
                completions.addAll(plugin.getBlueprintManager().getAllBlueprints()
                        .stream()
                        .map(bp -> bp.getName())
                        .collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("collect")) {
                completions.add("all");
                // Add all blueprint types
                completions.addAll(plugin.getBlueprintManager().getAllBlueprints()
                        .stream()
                        .map(bp -> bp.getType().toLowerCase())
                        .distinct()
                        .collect(Collectors.toList()));
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}