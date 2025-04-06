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

import java.util.*;

@RequiredArgsConstructor
public class GUIManager {
    private final TownyBlueprints plugin;
    private static final String BLUEPRINT_MENU_TITLE = "Available Blueprints";
    private static final String ADMIN_MENU_TITLE = "Blueprint Admin Menu";
    private static final String CREATE_MENU_TITLE = "Create Blueprint";
    private static final String EDIT_MENU_TITLE = "Edit Blueprint";
    private static final String DELETE_MENU_TITLE = "Delete Blueprint";
    private static final int BLUEPRINT_MENU_SIZE = 54;
    private static final int ADMIN_MENU_SIZE = 27;
    private static final int CREATE_MENU_SIZE = 54;
    private static final int EDIT_MENU_SIZE = 54;
    private static final int DELETE_MENU_SIZE = 54;

    private final Map<Player, Blueprint> blueprintCreation = new HashMap<>();
    private final Map<Player, Blueprint> blueprintEditing = new HashMap<>();

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
        inventory.setItem(11, createMenuItem(Material.CRAFTING_TABLE, "§a§lCreate New Blueprint",
                "§7Click to create a new blueprint"));

        // Edit Blueprint Button
        inventory.setItem(13, createMenuItem(Material.ANVIL, "§6§lEdit Blueprint",
                "§7Click to edit an existing blueprint"));

        // Delete Blueprint Button
        inventory.setItem(15, createMenuItem(Material.BARRIER, "§c§lDelete Blueprint",
                "§7Click to delete a blueprint"));

        player.openInventory(inventory);
    }

    public void openCreateMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, CREATE_MENU_SIZE, CREATE_MENU_TITLE);

        // Basic Settings Section
        inventory.setItem(10, createMenuItem(Material.NAME_TAG, "§e§lBasic Settings",
                "§7Click to set name and description"));

        inventory.setItem(11, createMenuItem(Material.STRUCTURE_BLOCK, "§e§lDimensions",
                "§7Click to set blueprint dimensions"));

        // Block Requirements Section
        inventory.setItem(13, createMenuItem(Material.CHEST, "§e§lRequired Blocks",
                "§7Click to set required blocks"));

        inventory.setItem(14, createMenuItem(Material.SPAWNER, "§e§lRequired Mobs",
                "§7Click to set required mobs"));

        // Biome Settings Section
        inventory.setItem(16, createMenuItem(Material.GRASS_BLOCK, "§e§lRequired Biomes",
                "§7Click to set required biomes"));

        inventory.setItem(17, createMenuItem(Material.DEAD_BUSH, "§e§lForbidden Biomes",
                "§7Click to set forbidden biomes"));

        // Economy Settings Section
        inventory.setItem(28, createMenuItem(Material.GOLD_INGOT, "§e§lIncome Settings",
                "§7Click to set daily income"));

        inventory.setItem(29, createMenuItem(Material.IRON_INGOT, "§e§lUpkeep Settings",
                "§7Click to set daily upkeep"));

        inventory.setItem(30, createMenuItem(Material.EMERALD, "§e§lPlacement Cost",
                "§7Click to set placement cost"));

        // Group Settings Section
        inventory.setItem(32, createMenuItem(Material.BOOKSHELF, "§e§lGroup Settings",
                "§7Click to set group requirements"));

        inventory.setItem(33, createMenuItem(Material.COMPARATOR, "§e§lUpkeep Sharing",
                "§7Click to configure shared upkeep"));

        // Tool Settings Section
        inventory.setItem(34, createMenuItem(Material.DIAMOND_AXE, "§e§lTool Settings",
                "§7Click to set tool requirements"));

        // Save/Cancel Section
        inventory.setItem(49, createMenuItem(Material.LIME_CONCRETE, "§a§lSave Blueprint",
                "§7Click to save the blueprint"));

        inventory.setItem(50, createMenuItem(Material.RED_CONCRETE, "§c§lCancel",
                "§7Click to cancel creation"));

        player.openInventory(inventory);
    }

    public void openEditMenu(Player player, Blueprint blueprint) {
        blueprintEditing.put(player, blueprint);
        Inventory inventory = Bukkit.createInventory(null, EDIT_MENU_SIZE, EDIT_MENU_TITLE);

        // Add all the same items as create menu, but pre-filled with blueprint values
        // Basic Settings Section
        inventory.setItem(10, createMenuItem(Material.NAME_TAG, "§e§lBasic Settings",
                "§7Current Name: §f" + blueprint.getName(),
                "§7Current Description: §f" + blueprint.getDescription()));

        inventory.setItem(11, createMenuItem(Material.STRUCTURE_BLOCK, "§e§lDimensions",
                "§7Current Size: §f" + blueprint.getSizeX() + "x" + blueprint.getSizeY() + "x" + blueprint.getSizeZ()));

        // Block Requirements Section
        List<String> blockLore = new ArrayList<>();
        blockLore.add("§7Current Requirements:");
        blueprint.getRequiredBlocks().forEach((block, amount) ->
                blockLore.add("§7- §f" + block + ": " + amount));
        inventory.setItem(13, createMenuItem(Material.CHEST, "§e§lRequired Blocks", blockLore));

        // Mob Requirements Section
        List<String> mobLore = new ArrayList<>();
        mobLore.add("§7Current Requirements:");
        blueprint.getRequiredMobs().forEach((mob, amount) ->
                mobLore.add("§7- §f" + mob + ": " + amount));
        inventory.setItem(14, createMenuItem(Material.SPAWNER, "§e§lRequired Mobs", mobLore));

        // Biome Settings Section
        List<String> requiredBiomeLore = new ArrayList<>();
        requiredBiomeLore.add("§7Required Biomes:");
        blueprint.getRequiredBiomes().forEach(biome ->
                requiredBiomeLore.add("§7- §f" + biome));
        inventory.setItem(16, createMenuItem(Material.GRASS_BLOCK, "§e§lRequired Biomes", requiredBiomeLore));

        List<String> forbiddenBiomeLore = new ArrayList<>();
        forbiddenBiomeLore.add("§7Forbidden Biomes:");
        blueprint.getForbiddenBiomes().forEach(biome ->
                forbiddenBiomeLore.add("§7- §f" + biome));
        inventory.setItem(17, createMenuItem(Material.DEAD_BUSH, "§e§lForbidden Biomes", forbiddenBiomeLore));

        // Economy Settings Section
        inventory.setItem(28, createMenuItem(Material.GOLD_INGOT, "§e§lIncome Settings",
                "§7Current Income: §f" + blueprint.getDailyIncome(),
                "§7Income Type: §f" + blueprint.getIncomeType()));

        inventory.setItem(29, createMenuItem(Material.IRON_INGOT, "§e§lUpkeep Settings",
                "§7Current Upkeep: §f" + blueprint.getDailyUpkeep(),
                "§7Upkeep Type: §f" + blueprint.getUpkeepType()));

        inventory.setItem(30, createMenuItem(Material.EMERALD, "§e§lPlacement Cost",
                "§7Current Cost: §f" + blueprint.getPlacementCost()));

        // Group Settings Section
        inventory.setItem(32, createMenuItem(Material.BOOKSHELF, "§e§lGroup Settings",
                "§7Required Count: §f" + blueprint.getRequiredCount()));

        inventory.setItem(33, createMenuItem(Material.COMPARATOR, "§e§lUpkeep Sharing",
                "§7Shared Upkeep: §f" + blueprint.isSharedUpkeep(),
                "§7Multiplier: §f" + blueprint.getUpkeepMultiplier()));

        // Tool Settings Section
        inventory.setItem(34, createMenuItem(Material.DIAMOND_AXE, "§e§lTool Settings",
                "§7Tool Type: §f" + (blueprint.getToolType() != null ? blueprint.getToolType().name() : "None"),
                "§7Durability Drain: §f" + blueprint.getDurabilityDrain()));

        // Save/Cancel Section
        inventory.setItem(49, createMenuItem(Material.LIME_CONCRETE, "§a§lSave Changes",
                "§7Click to save changes"));

        inventory.setItem(50, createMenuItem(Material.RED_CONCRETE, "§c§lCancel",
                "§7Click to cancel editing"));

        player.openInventory(inventory);
    }

    public void openDeleteMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, DELETE_MENU_SIZE, DELETE_MENU_TITLE);

        int slot = 0;
        for (Blueprint blueprint : plugin.getBlueprintManager().getAllBlueprints()) {
            if (slot >= DELETE_MENU_SIZE) break;

            ItemStack item = createBlueprintItem(blueprint);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add("§c§lClick to delete this blueprint");
                lore.add("§c§lThis action cannot be undone!");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(slot++, item);
        }

        player.openInventory(inventory);
    }

    private ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMenuItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
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

            // Add required blocks
            if (!blueprint.getRequiredBlocks().isEmpty()) {
                lore.add("§eRequired Blocks:");
                blueprint.getRequiredBlocks().forEach((block, amount) ->
                        lore.add("§7- §f" + amount + "x " + block));
            }

            // Add required mobs
            if (!blueprint.getRequiredMobs().isEmpty()) {
                lore.add("§eRequired Mobs:");
                blueprint.getRequiredMobs().forEach((mob, amount) ->
                        lore.add("§7- §f" + amount + "x " + mob));
            }

            // Add biome requirements
            if (!blueprint.getRequiredBiomes().isEmpty()) {
                lore.add("§eRequired Biomes:");
                blueprint.getRequiredBiomes().forEach(biome ->
                        lore.add("§7- §f" + biome));
            }

            if (!blueprint.getForbiddenBiomes().isEmpty()) {
                lore.add("§eForbidden Biomes:");
                blueprint.getForbiddenBiomes().forEach(biome ->
                        lore.add("§7- §f" + biome));
            }

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

            // Add group requirements if any
            if (blueprint.getRequiredCount() > 1) {
                lore.add("");
                lore.add("§eGroup Requirements:");
                lore.add("§7Required Count: §f" + blueprint.getRequiredCount());
                if (blueprint.isSharedUpkeep()) {
                    lore.add("§7Shared Upkeep: §aYes");
                    lore.add("§7Upkeep Multiplier: §f" + blueprint.getUpkeepMultiplier());
                }
            }

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

    public Blueprint getBlueprintInEditing(Player player) {
        return blueprintEditing.get(player);
    }

    public void removeBlueprintInCreation(Player player) {
        blueprintCreation.remove(player);
    }

    public void removeBlueprintInEditing(Player player) {
        blueprintEditing.remove(player);
    }
}