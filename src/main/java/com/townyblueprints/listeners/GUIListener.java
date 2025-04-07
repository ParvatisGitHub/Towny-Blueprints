package com.townyblueprints.listeners;

import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.Blueprint;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class GUIListener implements Listener {
    private final TownyBlueprints plugin;

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals("Available Blueprints")) {
            event.setCancelled(true);
            handleBlueprintMenu(event, player);
        } else if (title.equals("Blueprint Admin Menu")) {
            event.setCancelled(true);
            handleAdminMenu(event, player);
        } else if (title.equals("Create Blueprint")) {
            event.setCancelled(true);
            handleCreateMenu(event, player);
        } else if (title.equals("Edit Blueprint")) {
            event.setCancelled(true);
            handleEditMenu(event, player);
        } else if (title.equals("Delete Blueprint")) {
            event.setCancelled(true);
            handleDeleteMenu(event, player);
        } else if (title.equals("Select Blueprint to Edit")) {
            event.setCancelled(true);
            handleEditSelection(event, player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        if ((title.equals("Create Blueprint") || title.equals("Edit Blueprint"))) {
            // Only clear states if we're not in chat input mode
            if (!plugin.getChatInputListener().isInChatInput(player)) {
                plugin.getGuiManager().removeBlueprintInCreation(player);
                plugin.getGuiManager().removeBlueprintInEditing(player);
                plugin.getChatInputListener().clearPlayerState(player);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Blueprint blueprint = plugin.getPlacementHandler().getPlayerPlacements().get(player.getUniqueId());

        if (blueprint == null) return;

        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            plugin.getPlacementHandler().handleLeftClick(player, event.getClickedBlock().getLocation());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            plugin.getPlacementHandler().handleRightClick(player);
        }
    }

    private void handleBlueprintMenu(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String name = clicked.getItemMeta().getDisplayName().substring(2); // Remove color code
        Blueprint blueprint = plugin.getBlueprintManager().getBlueprint(name);

        if (blueprint != null) {
            player.closeInventory();
            plugin.getPlacementHandler().startPlacement(player, blueprint);
        }
    }

    private void handleAdminMenu(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (clicked.getType()) {
            case CRAFTING_TABLE:
                plugin.getGuiManager().startBlueprintCreation(player);
                break;
            case ANVIL:
                plugin.getGuiManager().openEditBlueprintSelection(player);
                break;
            case BARRIER:
                plugin.getGuiManager().openDeleteMenu(player);
                break;
        }
    }

    private void handleCreateMenu(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Blueprint blueprint = plugin.getGuiManager().getBlueprintInCreation(player);
        if (blueprint == null) return;

        switch (clicked.getType()) {
            case NAME_TAG:
                startChatInput(player, ChatInputListener.InputState.NAME, "Enter the blueprint name:");
                break;
            case BOOK:
                startChatInput(player, ChatInputListener.InputState.DESCRIPTION, "Enter the blueprint description:");
                break;
            case STRUCTURE_BLOCK:
                startChatInput(player, ChatInputListener.InputState.SIZE, "Enter dimensions (format: x,y,z):");
                break;
            case CHEST:
                if (clicked.getItemMeta().getDisplayName().contains("Required Blocks")) {
                    startChatInput(player, ChatInputListener.InputState.BLOCKS, "Enter required blocks (format: MATERIAL:amount):");
                }
                break;
            case BARRIER:
                handleRemovalOption(player, clicked.getItemMeta().getDisplayName(), blueprint);
                break;
            case SPAWNER:
                startChatInput(player, ChatInputListener.InputState.MOBS, "Enter required mobs (format: TYPE:amount):");
                break;
            case GRASS_BLOCK:
                startChatInput(player, ChatInputListener.InputState.REQUIRED_BIOMES, "Enter required biomes (one per line):");
                break;
            case DEAD_BUSH:
                startChatInput(player, ChatInputListener.InputState.FORBIDDEN_BIOMES, "Enter forbidden biomes (one per line):");
                break;
            case GOLD_INGOT:
                startChatInput(player, ChatInputListener.InputState.INCOME, "Enter daily income (format: amount type):");
                break;
            case IRON_INGOT:
                startChatInput(player, ChatInputListener.InputState.UPKEEP, "Enter daily upkeep (format: amount type):");
                break;
            case EMERALD:
                startChatInput(player, ChatInputListener.InputState.COST, "Enter placement cost:");
                break;
            case BOOKSHELF:
                startChatInput(player, ChatInputListener.InputState.GROUP, "Enter required count:");
                break;
            case COMPARATOR:
                startChatInput(player, ChatInputListener.InputState.SHARED_UPKEEP, "Enter shared upkeep settings (format: true/false multiplier):");
                break;
            case DIAMOND_AXE:
                startChatInput(player, ChatInputListener.InputState.TOOL, "Enter tool settings (format: TOOL_TYPE durability_drain):");
                break;
            case ITEM_FRAME:
                startChatInput(player, ChatInputListener.InputState.DISPLAY_MATERIAL,
                        "Enter the display material name (e.g., DIAMOND_SWORD):");
                break;
            case LIME_CONCRETE:
                saveBlueprint(player, blueprint);
                break;
            case RED_CONCRETE:
                cancelCreation(player);
                break;
        }
    }

    private void handleRemovalOption(Player player, String title, Blueprint blueprint) {
        if (title.contains("Remove Blocks")) {
            startChatInput(player, ChatInputListener.InputState.REMOVE_BLOCKS,
                    "Enter the block type to remove, or 'cancel' to go back:");
        } else if (title.contains("Remove Mobs")) {
            startChatInput(player, ChatInputListener.InputState.REMOVE_MOBS,
                    "Enter the mob type to remove, or 'cancel' to go back:");
        } else if (title.contains("Remove Required Biomes")) {
            startChatInput(player, ChatInputListener.InputState.REMOVE_REQUIRED_BIOMES,
                    "Enter the required biome to remove, or 'cancel' to go back:");
        } else if (title.contains("Remove Forbidden Biomes")) {
            startChatInput(player, ChatInputListener.InputState.REMOVE_FORBIDDEN_BIOMES,
                    "Enter the forbidden biome to remove, or 'cancel' to go back:");
        }
    }

    private void handleEditMenu(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Blueprint blueprint = plugin.getGuiManager().getBlueprintInEditing(player);
        if (blueprint == null) return;

        switch (clicked.getType()) {
            case NAME_TAG:
                startChatInput(player, ChatInputListener.InputState.NAME, "Enter the blueprint name:");
                break;
            case BOOK:
                startChatInput(player, ChatInputListener.InputState.DESCRIPTION, "Enter the blueprint description:");
                break;
            case STRUCTURE_BLOCK:
                startChatInput(player, ChatInputListener.InputState.SIZE, "Enter dimensions (format: x,y,z):");
                break;
            case CHEST:
                if (clicked.getItemMeta().getDisplayName().contains("Required Blocks")) {
                    startChatInput(player, ChatInputListener.InputState.BLOCKS, "Enter required blocks (format: MATERIAL:amount):");
                }
                break;
            case BARRIER:
                handleRemovalOption(player, clicked.getItemMeta().getDisplayName(), blueprint);
                break;
            case SPAWNER:
                startChatInput(player, ChatInputListener.InputState.MOBS, "Enter required mobs (format: TYPE:amount):");
                break;
            case GRASS_BLOCK:
                startChatInput(player, ChatInputListener.InputState.REQUIRED_BIOMES, "Enter required biomes (one per line):");
                break;
            case DEAD_BUSH:
                startChatInput(player, ChatInputListener.InputState.FORBIDDEN_BIOMES, "Enter forbidden biomes (one per line):");
                break;
            case GOLD_INGOT:
                startChatInput(player, ChatInputListener.InputState.INCOME, "Enter daily income (format: amount type):");
                break;
            case IRON_INGOT:
                startChatInput(player, ChatInputListener.InputState.UPKEEP, "Enter daily upkeep (format: amount type):");
                break;
            case EMERALD:
                startChatInput(player, ChatInputListener.InputState.COST, "Enter placement cost:");
                break;
            case BOOKSHELF:
                startChatInput(player, ChatInputListener.InputState.GROUP, "Enter required count:");
                break;
            case COMPARATOR:
                startChatInput(player, ChatInputListener.InputState.SHARED_UPKEEP, "Enter shared upkeep settings (format: true/false multiplier):");
                break;
            case DIAMOND_AXE:
                startChatInput(player, ChatInputListener.InputState.TOOL, "Enter tool settings (format: TOOL_TYPE durability_drain):");
                break;
            case ITEM_FRAME:
                startChatInput(player, ChatInputListener.InputState.DISPLAY_MATERIAL,
                        "Enter the display material name (e.g., DIAMOND_SWORD):");
                break;
            case LIME_CONCRETE:
                saveEditedBlueprint(player, blueprint);
                break;
            case RED_CONCRETE:
                cancelEditing(player);
                break;
        }
    }

    private void handleEditSelection(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String name = clicked.getItemMeta().getDisplayName().substring(2); // Remove color code
        Blueprint blueprint = plugin.getBlueprintManager().getBlueprint(name);

        if (blueprint != null) {
            plugin.getGuiManager().openEditMenu(player, blueprint);
        }
    }

    private void handleDeleteMenu(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String name = clicked.getItemMeta().getDisplayName().substring(2); // Remove color code
        Blueprint blueprint = plugin.getBlueprintManager().getBlueprint(name);

        if (blueprint != null) {
            plugin.getBlueprintManager().deleteBlueprint(name);
            player.sendMessage("§aBlueprint '" + name + "' has been deleted.");
            player.closeInventory();
            plugin.getGuiManager().openAdminMenu(player);
        }
    }

    private void startChatInput(Player player, ChatInputListener.InputState state, String prompt) {
        // Set chat input state first
        plugin.getChatInputListener().setPlayerState(player, state);

        // Send prompt message
        player.sendMessage("§e" + prompt);
        player.sendMessage("§7Type your response in chat, or type 'cancel' to go back.");

        // Close inventory last
        player.closeInventory();
    }

    private void saveBlueprint(Player player, Blueprint blueprint) {
        if (blueprint == null) {
            player.sendMessage("§cError: No blueprint to save!");
            return;
        }

        if (blueprint.getName().equals("New Blueprint")) {
            player.sendMessage("§cPlease set a name for the blueprint!");
            return;
        }

        if (blueprint.getRequiredBlocks().isEmpty() && blueprint.getRequiredMobs().isEmpty()) {
            player.sendMessage("§cPlease set at least one requirement (blocks or mobs)!");
            return;
        }

        plugin.getBlueprintManager().saveBlueprint(blueprint);
        player.sendMessage("§aBlueprint saved successfully!");
        player.closeInventory();
        plugin.getGuiManager().openAdminMenu(player);
    }

    private void saveEditedBlueprint(Player player, Blueprint blueprint) {
        if (blueprint == null) {
            player.sendMessage("§cError: No blueprint to save!");
            return;
        }

        plugin.getBlueprintManager().saveBlueprint(blueprint);
        player.sendMessage("§aBlueprint changes saved successfully!");
        player.closeInventory();
        plugin.getGuiManager().openAdminMenu(player);
    }

    private void cancelCreation(Player player) {
        plugin.getGuiManager().removeBlueprintInCreation(player);
        plugin.getGuiManager().removeBlueprintInEditing(player);
        plugin.getChatInputListener().clearPlayerState(player);
        player.sendMessage("§cBlueprint creation cancelled.");
        player.closeInventory();
        plugin.getGuiManager().openAdminMenu(player);
    }

    private void cancelEditing(Player player) {
        plugin.getGuiManager().removeBlueprintInEditing(player);
        plugin.getChatInputListener().clearPlayerState(player);
        player.sendMessage("§cBlueprint editing cancelled.");
        player.closeInventory();
        plugin.getGuiManager().openAdminMenu(player);
    }
}