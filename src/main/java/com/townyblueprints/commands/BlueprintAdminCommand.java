package com.townyblueprints.commands;

import com.townyblueprints.TownyBlueprints;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BlueprintAdminCommand implements CommandExecutor, TabCompleter {
    private final TownyBlueprints plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("townyblueprints.admin")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            plugin.getGuiManager().openAdminMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(player);
                break;
            case "import":
                handleImport(player, args);
                break;
            case "export":
                handleExport(player, args);
                break;
            case "delete":
                handleDelete(player, args);
                break;
            case "list":
                handleList(player);
                break;
            case "gui":
                plugin.getGuiManager().openAdminMenu(player);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleCreate(Player player) {
        plugin.getGuiManager().startBlueprintCreation(player);
        player.sendMessage("§aOpening blueprint creation menu...");
    }

    private void handleImport(Player player, String[] args) {
    if (args.length < 2) {
        player.sendMessage("§cUsage: /bpadmin import <filename>");
        return;
    }

    String filename = args[1];
    if (!filename.endsWith(".yml")) {
        filename += ".yml";
    }

    File blueprintsDir = new File(plugin.getDataFolder(), "blueprints");
    if (!blueprintsDir.exists()) {
        blueprintsDir.mkdirs();
    }

    File file = new File(blueprintsDir, filename);
    if (!file.exists()) {
        player.sendMessage("§cFile not found: " + filename);
        return;
    }

    plugin.getBlueprintManager().importBlueprint(file);
    player.sendMessage("§aBlueprint imported successfully!");
}

    private void handleExport(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /bpadmin export <blueprint_name>");
            return;
        }

        String blueprintName = args[1];
        if (plugin.getBlueprintManager().getBlueprint(blueprintName) == null) {
            player.sendMessage("§cBlueprint not found: " + blueprintName);
            return;
        }

        File exportDir = new File(plugin.getDataFolder(), "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        plugin.getBlueprintManager().exportBlueprint(blueprintName, exportDir);
        player.sendMessage("§aBlueprint exported to: exports/" + blueprintName.toLowerCase().replace(" ", "_") + ".yml");
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /bpadmin delete <blueprint_name>");
            return;
        }

        String blueprintName = args[1];
        if (plugin.getBlueprintManager().getBlueprint(blueprintName) == null) {
            player.sendMessage("§cBlueprint not found: " + blueprintName);
            return;
        }

        plugin.getBlueprintManager().deleteBlueprint(blueprintName);
        player.sendMessage("§aBlueprint deleted successfully!");
    }

    private void handleList(Player player) {
        List<String> blueprints = plugin.getBlueprintManager().getAllBlueprints()
            .stream()
            .map(bp -> bp.getName())
            .collect(Collectors.toList());

        if (blueprints.isEmpty()) {
            player.sendMessage("§cNo blueprints found!");
            return;
        }

        player.sendMessage("§6Available Blueprints:");
        for (String name : blueprints) {
            player.sendMessage("§7- §f" + name);
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6Blueprint Admin Commands:");
        player.sendMessage("§f/bpadmin §7- Open admin GUI");
        player.sendMessage("§f/bpadmin create §7- Create a new blueprint");
        player.sendMessage("§f/bpadmin import <filename> §7- Import a blueprint");
        player.sendMessage("§f/bpadmin export <blueprint_name> §7- Export a blueprint");
        player.sendMessage("§f/bpadmin delete <blueprint_name> §7- Delete a blueprint");
        player.sendMessage("§f/bpadmin list §7- List all blueprints");
        player.sendMessage("§f/bpadmin gui §7- Open admin GUI");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("create");
            completions.add("import");
            completions.add("export");
            completions.add("delete");
            completions.add("list");
            completions.add("gui");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "export":
                case "delete":
                    completions.addAll(plugin.getBlueprintManager().getAllBlueprints()
                        .stream()
                        .map(bp -> bp.getName())
                        .collect(Collectors.toList()));
                    break;
                case "import":
                    File[] files = plugin.getDataFolder().listFiles((dir, name) -> name.endsWith(".yml"));
                    if (files != null) {
                        for (File file : files) {
                            completions.add(file.getName());
                        }
                    }
                    break;
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}