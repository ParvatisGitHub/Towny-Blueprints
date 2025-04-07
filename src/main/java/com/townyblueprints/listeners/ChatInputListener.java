package com.townyblueprints.listeners;

import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.Blueprint;
import com.townyblueprints.util.BiomeUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.*;

@RequiredArgsConstructor
public class ChatInputListener implements Listener {
    private final TownyBlueprints plugin;
    private final Map<UUID, BlueprintEditSession> editSessions = new HashMap<>();

    public enum InputState {
        NONE,
        NAME,
        DESCRIPTION,
        SIZE,
        BLOCKS,
        REMOVE_BLOCKS,
        MOBS,
        REMOVE_MOBS,
        REQUIRED_BIOMES,
        REMOVE_REQUIRED_BIOMES,
        FORBIDDEN_BIOMES,
        REMOVE_FORBIDDEN_BIOMES,
        INCOME,
        UPKEEP,
        COST,
        GROUP,
        SHARED_UPKEEP,
        TOOL,
        DISPLAY_MATERIAL
    }

    public boolean isInChatInput(Player player) {
        BlueprintEditSession session = editSessions.get(player.getUniqueId());
        return session != null && session.getCurrentField() != InputState.NONE;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        BlueprintEditSession session = editSessions.get(player.getUniqueId());

        if (session != null) {
            event.setCancelled(true);
            String message = event.getMessage().trim();

            if (message.equalsIgnoreCase("cancel")) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        editSessions.remove(player.getUniqueId());
                        player.sendMessage("§cInput cancelled.");

                        if (plugin.getGuiManager().getBlueprintInCreation(player) != null) {
                            plugin.getGuiManager().openCreateMenu(player);
                        } else if (plugin.getGuiManager().getBlueprintInEditing(player) != null) {
                            plugin.getGuiManager().openEditMenu(player, plugin.getGuiManager().getBlueprintInEditing(player));
                        }
                    }
                }.runTask(plugin);
                return;
            }

            if (message.equalsIgnoreCase("done")) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        finishEditing(player);
                    }
                }.runTask(plugin);
                return;
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    processInput(player, session, message);
                }
            }.runTask(plugin);
        }
    }

    private void processInput(Player player, BlueprintEditSession session, String message) {
        switch (session.getCurrentField()) {
            case NAME:
                handleNameInput(player, session, message);
                break;
            case DESCRIPTION:
                handleDescriptionInput(player, session, message);
                break;
            case SIZE:
                handleSizeInput(player, session, message);
                break;
            case BLOCKS:
                handleBlocksInput(player, session, message);
                break;
            case REMOVE_BLOCKS:
                handleRemoveBlocks(player, session, message);
                break;
            case MOBS:
                handleMobsInput(player, session, message);
                break;
            case REMOVE_MOBS:
                handleRemoveMobs(player, session, message);
                break;
            case REQUIRED_BIOMES:
                handleRequiredBiomesInput(player, session, message);
                break;
            case REMOVE_REQUIRED_BIOMES:
                handleRemoveRequiredBiomes(player, session, message);
                break;
            case FORBIDDEN_BIOMES:
                handleForbiddenBiomesInput(player, session, message);
                break;
            case REMOVE_FORBIDDEN_BIOMES:
                handleRemoveForbiddenBiomes(player, session, message);
                break;
            case INCOME:
                handleIncomeInput(player, session, message);
                break;
            case UPKEEP:
                handleUpkeepInput(player, session, message);
                break;
            case COST:
                handleCostInput(player, session, message);
                break;
            case GROUP:
                handleGroupInput(player, session, message);
                break;
            case SHARED_UPKEEP:
                handleSharedUpkeepInput(player, session, message);
                break;
            case TOOL:
                handleToolInput(player, session, message);
                break;
            case DISPLAY_MATERIAL:
                handleDisplayMaterialInput(player, session, message);
                break;
        }

        if (plugin.getGuiManager().getBlueprintInCreation(player) != null) {
            plugin.getGuiManager().openCreateMenu(player);
        } else if (plugin.getGuiManager().getBlueprintInEditing(player) != null) {
            plugin.getGuiManager().openEditMenu(player, plugin.getGuiManager().getBlueprintInEditing(player));
        }
    }

    private void handleNameInput(Player player, BlueprintEditSession session, String input) {
        if (input.length() < 3 || input.length() > 32) {
            player.sendMessage("§cBlueprint name must be between 3 and 32 characters!");
            return;
        }
        session.getBlueprint().setName(input);
        player.sendMessage("§aName set to: §f" + input);
        session.setCurrentField(InputState.DESCRIPTION);
        player.sendMessage("§eEnter the blueprint description, or type 'done' to finish:");
    }

    private void handleDescriptionInput(Player player, BlueprintEditSession session, String input) {
        session.getBlueprint().setDescription(input);
        player.sendMessage("§aDescription set to: §f" + input);
        session.setCurrentField(InputState.SIZE);
        player.sendMessage("§eEnter the size (format: x,y,z), or type 'done' to finish:");
    }

    private void handleSizeInput(Player player, BlueprintEditSession session, String input) {
        String[] parts = input.split(",");
        if (parts.length == 3) {
            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());

                if (x < 1 || y < 1 || z < 1 || x > 32 || y > 32 || z > 32) {
                    player.sendMessage("§cDimensions must be between 1 and 32!");
                    return;
                }

                session.getBlueprint().setSizeX(x);
                session.getBlueprint().setSizeY(y);
                session.getBlueprint().setSizeZ(z);

                player.sendMessage(String.format("§aSize set to: §f%d x %d x %d", x, y, z));
                session.setCurrentField(InputState.BLOCKS);
                player.sendMessage("§eEnter required blocks (format: MATERIAL:amount), or type 'done' to finish:");
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid format. Use numbers separated by commas (e.g., 5,3,5)");
            }
        } else {
            player.sendMessage("§cInvalid format. Use x,y,z (e.g., 5,3,5)");
        }
    }

    private void handleBlocksInput(Player player, BlueprintEditSession session, String input) {
        String[] parts = input.split(":");
        if (parts.length == 2) {
            try {
                String blockType = parts[0].trim().toUpperCase();
                int amount = Integer.parseInt(parts[1].trim());

                if (amount < 1) {
                    player.sendMessage("§cAmount must be greater than 0!");
                    return;
                }

                if (!plugin.getBlockDefinitionManager().isBlockDefinition(blockType)) {
                    try {
                        Material.valueOf(blockType);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid block type! Use a valid material name or block definition.");
                        return;
                    }
                }

                session.getBlueprint().addRequiredBlock(blockType, amount);
                player.sendMessage(String.format("§aAdded requirement: §f%s x%d", blockType, amount));
                player.sendMessage("§eEnter another block requirement, or type 'done' to continue:");
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount. Use format MATERIAL:amount (e.g., STONE:10)");
            }
        } else {
            player.sendMessage("§cInvalid format. Use MATERIAL:amount (e.g., STONE:10)");
        }
    }

    private void handleRemoveBlocks(Player player, BlueprintEditSession session, String input) {
        String blockType = input.trim().toUpperCase();
        if (session.getBlueprint().removeRequiredBlock(blockType)) {
            player.sendMessage("§aRemoved block requirement: §f" + blockType);
        } else {
            player.sendMessage("§cBlock type not found in requirements!");
        }
        finishEditing(player);
    }

    private void handleMobsInput(Player player, BlueprintEditSession session, String input) {
        String[] parts = input.split(":");
        if (parts.length == 2) {
            try {
                String mobType = parts[0].trim().toUpperCase();
                int amount = Integer.parseInt(parts[1].trim());

                if (amount < 1) {
                    player.sendMessage("§cAmount must be greater than 0!");
                    return;
                }

                try {
                    org.bukkit.entity.EntityType.valueOf(mobType);
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cInvalid mob type! Use a valid entity type name.");
                    return;
                }

                session.getBlueprint().addRequiredMob(mobType, amount);
                player.sendMessage(String.format("§aAdded requirement: §f%s x%d", mobType, amount));
                player.sendMessage("§eEnter another mob requirement, or type 'done' to continue:");
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount. Use format TYPE:amount (e.g., VILLAGER:2)");
            }
        } else {
            player.sendMessage("§cInvalid format. Use TYPE:amount (e.g., VILLAGER:2)");
        }
    }

    private void handleRemoveMobs(Player player, BlueprintEditSession session, String input) {
        String mobType = input.trim().toUpperCase();
        if (session.getBlueprint().removeRequiredMob(mobType)) {
            player.sendMessage("§aRemoved mob requirement: §f" + mobType);
        } else {
            player.sendMessage("§cMob type not found in requirements!");
        }
        finishEditing(player);
    }
    private boolean isValidBiomeName(String biomeName) {
        // Check if the biome name exists in any of the valid biome lists in BiomeUtil
        return BiomeUtil.isOverWorld(biomeName) || BiomeUtil.isNether(biomeName) ||
                BiomeUtil.isEnd(biomeName) || BiomeUtil.isOcean(biomeName) ||
                BiomeUtil.isCold(biomeName) || BiomeUtil.isTemperate(biomeName) ||
                BiomeUtil.isWarm(biomeName) || BiomeUtil.isSnowy(biomeName);
    }
    private void handleRequiredBiomesInput(Player player, BlueprintEditSession session, String input) {
        try {
            String biomeName = input.toUpperCase();

            // Check if the biome name is valid by comparing it with predefined biome lists in BiomeUtil
            if (isValidBiomeName(biomeName)) {
                // Check if the biome is in one of the valid categories using BiomeUtil
                if (BiomeUtil.isOverWorld(biomeName) || BiomeUtil.isNether(biomeName) ||
                        BiomeUtil.isEnd(biomeName) || BiomeUtil.isOcean(biomeName)) {

                    // Add the biome to the required list in the session
                    session.getBlueprint().addRequiredBiome(biomeName);
                    player.sendMessage(String.format("§aAdded required biome: §f%s", biomeName));
                    player.sendMessage("§eEnter another required biome, or type 'done' to continue:");
                } else {
                    // If the biome is not in any of the valid categories, notify the player
                    player.sendMessage("§cInvalid biome type! Please enter a valid biome name.");
                }
            } else {
                // If the biome name doesn't exist, notify the player
                player.sendMessage("§cInvalid biome name! Please enter a valid biome name.");
            }
        } catch (Exception e) {
            // Handle any other exceptions (this shouldn't normally happen)
            player.sendMessage("§cAn error occurred while processing the biome name.");
        }
    }

    private void handleRemoveRequiredBiomes(Player player, BlueprintEditSession session, String input) {
        String biome = input.trim().toUpperCase();
        if (session.getBlueprint().removeRequiredBiome(biome)) {
            player.sendMessage("§aRemoved required biome: §f" + biome);
        } else {
            player.sendMessage("§cBiome not found in required biomes!");
        }
        finishEditing(player);
    }

    private void handleForbiddenBiomesInput(Player player, BlueprintEditSession session, String input) {
        try {
            String biomeName = input.toUpperCase();

            // Check if the biome name is valid by comparing it with predefined biome lists in BiomeUtil
            if (isValidBiomeName(biomeName)) {
                // Check if the biome is in one of the valid categories using BiomeUtil
                if (BiomeUtil.isOverWorld(biomeName) || BiomeUtil.isNether(biomeName) ||
                        BiomeUtil.isEnd(biomeName) || BiomeUtil.isOcean(biomeName)) {

                    // Add the biome to the forbidden list in the session
                    session.getBlueprint().addForbiddenBiome(biomeName);
                    player.sendMessage(String.format("§aAdded forbidden biome: §f%s", biomeName));
                    player.sendMessage("§eEnter another forbidden biome, or type 'done' to continue:");
                } else {
                    // If the biome is not in any of the valid categories, notify the player
                    player.sendMessage("§cInvalid biome type! Please enter a valid biome name.");
                }
            } else {
                // If the biome name doesn't exist, notify the player
                player.sendMessage("§cInvalid biome name! Please enter a valid biome name.");
            }
        } catch (Exception e) {
            // Handle any other exceptions (this shouldn't normally happen)
            player.sendMessage("§cAn error occurred while processing the biome name.");
        }
    }

    private void handleRemoveForbiddenBiomes(Player player, BlueprintEditSession session, String input) {
        String biome = input.trim().toUpperCase();
        if (session.getBlueprint().removeForbiddenBiome(biome)) {
            player.sendMessage("§aRemoved forbidden biome: §f" + biome);
        } else {
            player.sendMessage("§cBiome not found in forbidden biomes!");
        }
        finishEditing(player);
    }

    private void handleIncomeInput(Player player, BlueprintEditSession session, String input) {
        String[] parts = input.split(" ");
        if (parts.length == 2) {
            try {
                double amount = Double.parseDouble(parts[0]);
                String type = parts[1].toUpperCase();

                if (amount < 0) {
                    player.sendMessage("§cAmount cannot be negative!");
                    return;
                }

                if (!type.equals("MONEY") && !type.startsWith("template:")) {
                    try {
                        Material.valueOf(type);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid resource type!");
                        return;
                    }
                }

                session.getBlueprint().setDailyIncome(amount);
                session.getBlueprint().setIncomeType(type);
                player.sendMessage(String.format("§aDaily income set to: §f%s %s", amount, type));
                session.setCurrentField(InputState.UPKEEP);
                player.sendMessage("§eEnter daily upkeep (format: amount type), or type 'done' to finish:");
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount format!");
            }
        } else {
            player.sendMessage("§cPlease use format: amount type (e.g., 100 MONEY or 10 DIAMOND)");
        }
    }

    private void handleUpkeepInput(Player player, BlueprintEditSession session, String input) {
        String[] parts = input.split(" ");
        if (parts.length == 2) {
            try {
                double amount = Double.parseDouble(parts[0]);
                String type = parts[1].toUpperCase();

                if (amount < 0) {
                    player.sendMessage("§cAmount cannot be negative!");
                    return;
                }

                if (!type.equals("MONEY") && !type.startsWith("template:")) {
                    try {
                        Material.valueOf(type);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid resource type!");
                        return;
                    }
                }

                session.getBlueprint().setDailyUpkeep(amount);
                session.getBlueprint().setUpkeepType(type);
                player.sendMessage(String.format("§aDaily upkeep set to: §f%s %s", amount, type));
                session.setCurrentField(InputState.COST);
                player.sendMessage("§eEnter placement cost (in coins), or type 'done' to finish:");
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount format!");
            }
        } else {
            player.sendMessage("§cPlease use format: amount type (e.g., 50 MONEY or 5 COAL)");
        }
    }

    private void handleCostInput(Player player, BlueprintEditSession session, String input) {
        try {
            double cost = Double.parseDouble(input);
            if (cost < 0) {
                player.sendMessage("§cPlacement cost cannot be negative!");
                return;
            }

            session.getBlueprint().setPlacementCost(cost);
            player.sendMessage(String.format("§aPlacement cost set to: §f%s", cost));
            session.setCurrentField(InputState.GROUP);
            player.sendMessage("§eEnter required count for group bonus, or type 'done' to finish:");
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid cost format! Please enter a number.");
        }
    }

    private void handleGroupInput(Player player, BlueprintEditSession session, String input) {
        try {
            int count = Integer.parseInt(input);
            if (count < 1) {
                player.sendMessage("§cRequired count must be at least 1!");
                return;
            }

            session.getBlueprint().setRequiredCount(count);
            player.sendMessage(String.format("§aRequired count set to: §f%d", count));
            session.setCurrentField(InputState.SHARED_UPKEEP);
            player.sendMessage("§eEnter shared upkeep settings (format: true/false multiplier), or type 'done' to finish:");
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number format! Please enter a whole number.");
        }
    }

    private void handleSharedUpkeepInput(Player player, BlueprintEditSession session, String input) {
        String[] parts = input.split(" ");
        if (parts.length == 2) {
            try {
                boolean shared = Boolean.parseBoolean(parts[0].toLowerCase());
                double multiplier = Double.parseDouble(parts[1]);

                if (multiplier <= 0) {
                    player.sendMessage("§cMultiplier must be greater than 0!");
                    return;
                }

                session.getBlueprint().setSharedUpkeep(shared);
                session.getBlueprint().setUpkeepMultiplier(multiplier);
                player.sendMessage(String.format("§aShared upkeep set to: §f%s with multiplier %s", shared, multiplier));
                session.setCurrentField(InputState.TOOL);
                player.sendMessage("§eEnter tool settings (format: TOOL_TYPE durability_drain), or type 'done' to finish:");
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid multiplier format!");
            }
        } else {
            player.sendMessage("§cPlease use format: true/false multiplier (e.g., true 0.8)");
        }
    }

    private void handleToolInput(Player player, BlueprintEditSession session, String input) {
        String[] parts = input.split(" ");
        if (parts.length == 2) {
            try {
                String toolType = parts[0].trim().toUpperCase();
                int durability = Integer.parseInt(parts[1].trim());

                if (durability < 1) {
                    player.sendMessage("§cDurability drain must be greater than 0!");
                    return;
                }

                if (plugin.getToolDefinitionManager().isToolDefinition(toolType)) {
                    List<Material> tools = plugin.getToolDefinitionManager().getDefinition(toolType);
                    if (!tools.isEmpty()) {
                        session.getBlueprint().setToolType(tools.get(0));
                        session.getBlueprint().setDurabilityDrain(durability);
                        player.sendMessage(String.format("§aTool settings set to: §f%s with durability drain %d", toolType, durability));
                        finishEditing(player);
                        return;
                    }
                }

                try {
                    Material tool = Material.valueOf(toolType);
                    if (!tool.name().endsWith("_AXE") && !tool.name().endsWith("_PICKAXE") &&
                            !tool.name().endsWith("_SHOVEL") && !tool.name().endsWith("_HOE") &&
                            !tool.name().endsWith("_SWORD")) {
                        player.sendMessage("§cInvalid tool type! Must be a valid tool material or definition (e.g., AXE, PICKAXE)");
                        return;
                    }
                    session.getBlueprint().setToolType(tool);
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cInvalid tool type! Must be a valid material name or tool definition.");
                    return;
                }

                session.getBlueprint().setDurabilityDrain(durability);
                player.sendMessage(String.format("§aTool settings set to: §f%s with durability drain %d", toolType, durability));
                finishEditing(player);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid durability format! Please enter a number.");
            }
        } else {
            player.sendMessage("§cPlease use format: TOOL_TYPE durability_drain (e.g., AXE 10 or DIAMOND_AXE 10)");
        }
    }

    private void handleDisplayMaterialInput(Player player, BlueprintEditSession session, String input) {
        try {
            Material material = Material.valueOf(input.toUpperCase());
            session.getBlueprint().setDisplayMaterial(material);
            player.sendMessage(String.format("§aDisplay material set to: §f%s", material.name()));
            finishEditing(player);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid material! Please enter a valid material name.");
        }
    }

    private void finishEditing(Player player) {
        BlueprintEditSession session = editSessions.get(player.getUniqueId());
        if (session != null) {
            Blueprint blueprint = session.getBlueprint();
            plugin.getBlueprintManager().saveBlueprint(blueprint);
            editSessions.remove(player.getUniqueId());
            player.sendMessage("§aBlueprint saved successfully!");

            plugin.getGuiManager().openAdminMenu(player);
        }
    }

    public void startBlueprintCreation(Player player) {
        Blueprint blueprint = new Blueprint();
        editSessions.put(player.getUniqueId(), new BlueprintEditSession(blueprint));
        player.sendMessage("§eEnter the blueprint name, or type 'cancel' to stop:");
    }

    public void setPlayerState(Player player, InputState state) {
        editSessions.put(player.getUniqueId(), new BlueprintEditSession(
                plugin.getGuiManager().getBlueprintInCreation(player) != null ?
                        plugin.getGuiManager().getBlueprintInCreation(player) :
                        plugin.getGuiManager().getBlueprintInEditing(player)
        ));
        editSessions.get(player.getUniqueId()).setCurrentField(state);
    }

    public void clearPlayerState(Player player) {
        editSessions.remove(player.getUniqueId());
    }

    private static class BlueprintEditSession {
        private final Blueprint blueprint;
        private InputState currentField;

        public BlueprintEditSession(Blueprint blueprint) {
            this.blueprint = blueprint;
            this.currentField = InputState.NAME;
        }

        public Blueprint getBlueprint() {
            return blueprint;
        }

        public InputState getCurrentField() {
            return currentField;
        }

        public void setCurrentField(InputState field) {
            this.currentField = field;
        }
    }
}