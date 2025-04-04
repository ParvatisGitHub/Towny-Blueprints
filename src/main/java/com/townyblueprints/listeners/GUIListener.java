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
                player.sendMessage("§eEdit functionality coming soon!");
                break;
            case BARRIER:
                player.sendMessage("§eDelete functionality coming soon!");
                break;
        }
    }

    private void handleCreateMenu(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Blueprint blueprint = plugin.getGuiManager().getBlueprintInCreation(player);
        if (blueprint == null) return;

        player.closeInventory();

        switch (clicked.getType()) {
            case NAME_TAG:
                plugin.getChatInputListener().setPlayerState(player, ChatInputListener.InputState.BLUEPRINT_NAME);
                player.sendMessage("§ePlease type the blueprint name in chat.");
                break;
            case STRUCTURE_BLOCK:
                plugin.getChatInputListener().setPlayerState(player, ChatInputListener.InputState.BLUEPRINT_DIMENSIONS);
                player.sendMessage("§ePlease type the dimensions (x,y,z) in chat.");
                break;
            case CHEST:
                plugin.getChatInputListener().setPlayerState(player, ChatInputListener.InputState.BLUEPRINT_BLOCKS);
                player.sendMessage("§ePlease type the required blocks in format: MATERIAL:amount, MATERIAL:amount, ...");
                break;
            case GOLD_INGOT:
                plugin.getChatInputListener().setPlayerState(player, ChatInputListener.InputState.BLUEPRINT_INCOME);
                player.sendMessage("§ePlease set income in format: amount type (e.g., 100 MONEY or 10 DIAMOND)");
                break;
            case IRON_INGOT:
                plugin.getChatInputListener().setPlayerState(player, ChatInputListener.InputState.BLUEPRINT_UPKEEP);
                player.sendMessage("§ePlease set upkeep in format: amount type (e.g., 50 MONEY or 5 COAL)");
                break;
            case EMERALD:
                plugin.getChatInputListener().setPlayerState(player, ChatInputListener.InputState.BLUEPRINT_COST);
                player.sendMessage("§ePlease type the placement cost.");
                break;
            case LIME_CONCRETE:
                saveBlueprint(player, blueprint);
                break;
        }
    }

    private void saveBlueprint(Player player, Blueprint blueprint) {
        if (blueprint.getName() == null || blueprint.getName().equals("New Blueprint")) {
            player.sendMessage("§cPlease set a name for the blueprint!");
            plugin.getGuiManager().openCreateMenu(player);
            return;
        }

        if (blueprint.getRequiredBlocks() == null || blueprint.getRequiredBlocks().isEmpty()) {
            player.sendMessage("§cPlease set required blocks for the blueprint!");
            plugin.getGuiManager().openCreateMenu(player);
            return;
        }

        plugin.getBlueprintManager().saveBlueprint(blueprint);
        player.sendMessage("§aBlueprint saved successfully!");
    }
}