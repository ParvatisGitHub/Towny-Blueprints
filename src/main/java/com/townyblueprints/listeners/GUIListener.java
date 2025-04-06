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
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        if (title.equals("Create Blueprint")) {
            plugin.getGuiManager().removeBlueprintInCreation(player);
            plugin.getChatInputListener().clearPlayerState(player);
        } else if (title.equals("Edit Blueprint")) {
            plugin.getGuiManager().removeBlueprintInEditing(player);
            plugin.getChatInputListener().clearPlayerState(player);
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
                plugin.getGuiManager().openEditMenu(player, null);
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
                startChatInput(player, "NAME", "Enter the blueprint name:");
                break;
            case STRUCTURE_BLOCK:
                startChatInput(player, "SIZE", "Enter dimensions (format: x,y,z):");
                break;
            case CHEST:
                startChatInput(player, "BLOCKS", "Enter required blocks (format: MATERIAL:amount):");
                break;
            case SPAWNER:
                startChatInput(player, "MOBS", "Enter required mobs (format: TYPE:amount):");
                break;
            case GRASS_BLOCK:
                startChatInput(player, "REQUIRED_BIOMES", "Enter required biomes (one per line):");
                break;
            case DEAD_BUSH:
                startChatInput(player, "FORBIDDEN_BIOMES", "Enter forbidden biomes (one per line):");
                break;
            case GOLD_INGOT:
                startChatInput(player, "INCOME", "Enter daily income (format: amount type):");
                break;
            case IRON_INGOT:
                startChatInput(player, "UPKEEP", "Enter daily upkeep (format: amount type):");
                break;
            case EMERALD:
                startChatInput(player, "COST", "Enter placement cost:");
                break;
            case BOOKSHELF:
                startChatInput(player, "GROUP", "Enter required count:");
                break;
            case COMPARATOR:
                startChatInput(player, "SHARED_UPKEEP", "Enter shared upkeep settings (format: true/false multiplier):");
                break;
            case DIAMOND_AXE:
                startChatInput(player, "TOOL", "Enter tool settings (format: TOOL_TYPE durability_drain):");
                break;
            case LIME_CONCRETE:
                saveBlueprint(player, blueprint);
                break;
            case RED_CONCRETE:
                cancelCreation(player);
                break;
        }
    }

    private void handleEditMenu(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Blueprint blueprint = plugin.getGuiManager().getBlueprintInEditing(player);
        if (blueprint == null) return;

        // Handle the same options as create menu
        handleCreateMenu(event, player);
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

    private void startChatInput(Player player, String type, String prompt) {
        player.closeInventory();
        plugin.getChatInputListener().setPlayerState(player, ChatInputListener.InputState.valueOf(type));
        player.sendMessage("§e" + prompt);
    }

    private void saveBlueprint(Player player, Blueprint blueprint) {
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

    private void cancelCreation(Player player) {
        plugin.getGuiManager().removeBlueprintInCreation(player);
        plugin.getGuiManager().removeBlueprintInEditing(player);
        plugin.getChatInputListener().clearPlayerState(player);
        player.sendMessage("§cBlueprint creation cancelled.");
        player.closeInventory();
        plugin.getGuiManager().openAdminMenu(player);
    }
}