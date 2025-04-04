package com.townyblueprints.managers;

import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.Blueprint;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class GUIManager {
    private final TownyBlueprints plugin;
    private static final String BLUEPRINT_MENU_TITLE = "Available Blueprints";
    private static final String ADMIN_MENU_TITLE = "Blueprint Admin Menu";
    private static final String CREATE_MENU_TITLE = "Create Blueprint";
    private static final int BLUEPRINT_MENU_SIZE = 54; // 6 rows
    private static final int ADMIN_MENU_SIZE = 27; // 3 rows
    private static final int CREATE_MENU_SIZE = 54; // 6 rows

    private final Map<Player, Blueprint> blueprintCreation = new HashMap<>();

    public void openBlueprintMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, BLUEPRINT_MENU_SIZE, BLUEPRINT_MENU_TITLE);
        
        for (Blueprint blueprint : plugin.getBlueprintManager().getAllBlueprints()) {
            if (player.hasPermission(blueprint.getPermissionNode())) {
                inventory.addItem(createBlueprintItem(blueprint));
            }
        }

        player.openInventory(inventory);
    }

    public void openAdminMenu(Player player) {
        if (!player.hasPermission("townyblueprints.admin")) {
            player.sendMessage("§cYou don't have permission to access the admin menu!");
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, ADMIN_MENU_SIZE, ADMIN_MENU_TITLE);

        // Create Blueprint Button
        ItemStack createItem = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta createMeta = createItem.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName("§a§lCreate New Blueprint");
            List<String> createLore = new ArrayList<>();
            createLore.add("§7Click to create a new blueprint");
            createMeta.setLore(createLore);
            createItem.setItemMeta(createMeta);
        }
        inventory.setItem(11, createItem);

        // Edit Blueprint Button
        ItemStack editItem = new ItemStack(Material.ANVIL);
        ItemMeta editMeta = editItem.getItemMeta();
        if (editMeta != null) {
            editMeta.setDisplayName("§6§lEdit Blueprint");
            List<String> editLore = new ArrayList<>();
            editLore.add("§7Click to edit an existing blueprint");
            editMeta.setLore(editLore);
            editItem.setItemMeta(editMeta);
        }
        inventory.setItem(13, editItem);

        // Delete Blueprint Button
        ItemStack deleteItem = new ItemStack(Material.BARRIER);
        ItemMeta deleteMeta = deleteItem.getItemMeta();
        if (deleteMeta != null) {
            deleteMeta.setDisplayName("§c§lDelete Blueprint");
            List<String> deleteLore = new ArrayList<>();
            deleteLore.add("§7Click to delete a blueprint");
            deleteMeta.setLore(deleteLore);
            deleteItem.setItemMeta(deleteMeta);
        }
        inventory.setItem(15, deleteItem);

        player.openInventory(inventory);
    }

    public void openCreateMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, CREATE_MENU_SIZE, CREATE_MENU_TITLE);

        // Blueprint Name Input
        ItemStack nameItem = new ItemStack(Material.NAME_TAG);
        ItemMeta nameMeta = nameItem.getItemMeta();
        if (nameMeta != null) {
            nameMeta.setDisplayName("§e§lSet Blueprint Name");
            List<String> nameLore = new ArrayList<>();
            nameLore.add("§7Click to set the blueprint name");
            nameLore.add("§7Current: §fNone");
            nameMeta.setLore(nameLore);
            nameItem.setItemMeta(nameMeta);
        }
        inventory.setItem(10, nameItem);

        // Size Settings
        ItemStack sizeItem = new ItemStack(Material.STRUCTURE_BLOCK);
        ItemMeta sizeMeta = sizeItem.getItemMeta();
        if (sizeMeta != null) {
            sizeMeta.setDisplayName("§e§lSet Dimensions");
            List<String> sizeLore = new ArrayList<>();
            sizeLore.add("§7Click to set blueprint dimensions");
            sizeLore.add("§7X: §f1");
            sizeLore.add("§7Y: §f1");
            sizeLore.add("§7Z: §f1");
            sizeMeta.setLore(sizeLore);
            sizeItem.setItemMeta(sizeMeta);
        }
        inventory.setItem(12, sizeItem);

        // Required Blocks
        ItemStack blocksItem = new ItemStack(Material.CHEST);
        ItemMeta blocksMeta = blocksItem.getItemMeta();
        if (blocksMeta != null) {
            blocksMeta.setDisplayName("§e§lRequired Blocks");
            List<String> blocksLore = new ArrayList<>();
            blocksLore.add("§7Click to set required blocks");
            blocksLore.add("§7Current: §fNone");
            blocksMeta.setLore(blocksLore);
            blocksItem.setItemMeta(blocksMeta);
        }
        inventory.setItem(14, blocksItem);

        // Income Settings
        ItemStack incomeItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta incomeMeta = incomeItem.getItemMeta();
        if (incomeMeta != null) {
            incomeMeta.setDisplayName("§e§lIncome Settings");
            List<String> incomeLore = new ArrayList<>();
            incomeLore.add("§7Click to set daily income");
            incomeLore.add("§7Amount: §f0");
            incomeLore.add("§7Type: §fMONEY");
            incomeMeta.setLore(incomeLore);
            incomeItem.setItemMeta(incomeMeta);
        }
        inventory.setItem(16, incomeItem);

        // Upkeep Settings
        ItemStack upkeepItem = new ItemStack(Material.IRON_INGOT);
        ItemMeta upkeepMeta = upkeepItem.getItemMeta();
        if (upkeepMeta != null) {
            upkeepMeta.setDisplayName("§e§lUpkeep Settings");
            List<String> upkeepLore = new ArrayList<>();
            upkeepLore.add("§7Click to set daily upkeep");
            upkeepLore.add("§7Amount: §f0");
            upkeepLore.add("§7Type: §fMONEY");
            upkeepMeta.setLore(upkeepLore);
            upkeepItem.setItemMeta(upkeepMeta);
        }
        inventory.setItem(28, upkeepItem);

        // Placement Cost
        ItemStack costItem = new ItemStack(Material.EMERALD);
        ItemMeta costMeta = costItem.getItemMeta();
        if (costMeta != null) {
            costMeta.setDisplayName("§e§lPlacement Cost");
            List<String> costLore = new ArrayList<>();
            costLore.add("§7Click to set placement cost");
            costLore.add("§7Cost: §f0");
            costMeta.setLore(costLore);
            costItem.setItemMeta(costMeta);
        }
        inventory.setItem(30, costItem);

        // Save Button
        ItemStack saveItem = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta saveMeta = saveItem.getItemMeta();
        if (saveMeta != null) {
            saveMeta.setDisplayName("§a§lSave Blueprint");
            List<String> saveLore = new ArrayList<>();
            saveLore.add("§7Click to save the blueprint");
            saveMeta.setLore(saveLore);
            saveItem.setItemMeta(saveMeta);
        }
        inventory.setItem(49, saveItem);

        player.openInventory(inventory);
    }

    private ItemStack createBlueprintItem(Blueprint blueprint) {
        ItemStack item = blueprint.getDisplayMaterial() != null ? 
            new ItemStack(blueprint.getDisplayMaterial()) : 
            new ItemStack(Material.PAPER);
            
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§6" + blueprint.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7" + blueprint.getDescription());
            lore.add("");
            lore.add("§eSize: §f" + blueprint.getSizeX() + "x" + blueprint.getSizeY() + "x" + blueprint.getSizeZ());
            
            // Handle income display
            if (blueprint.getIncomeType().startsWith("template:")) {
                String templateName = blueprint.getIncomeType().substring(9);
                lore.add("§eDaily Income:");
                lore.addAll(plugin.getResourceTemplateManager().getResourcesDisplay(templateName));
            } else {
                lore.add("§eDaily Income: §f" + blueprint.getDailyIncome() + " " + blueprint.getIncomeType());
            }
            
            // Handle upkeep display
            if (blueprint.getUpkeepType().startsWith("template:")) {
                String templateName = blueprint.getUpkeepType().substring(9);
                lore.add("§eDaily Upkeep:");
                lore.addAll(plugin.getResourceTemplateManager().getResourcesDisplay(templateName));
            } else {
                lore.add("§eDaily Upkeep: §f" + blueprint.getDailyUpkeep() + " " + blueprint.getUpkeepType());
            }
            
            lore.add("§ePlacement Cost: §f" + blueprint.getPlacementCost());
            lore.add("");
            lore.add("§aClick to place this blueprint");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    public void startBlueprintCreation(Player player) {
        Blueprint blueprint = new Blueprint();
        blueprint.setName("New Blueprint");
        blueprintCreation.put(player, blueprint);
        openCreateMenu(player);
    }

    public Blueprint getBlueprintInCreation(Player player) {
        return blueprintCreation.get(player);
    }

    public void removeBlueprintInCreation(Player player) {
        blueprintCreation.remove(player);
    }
}