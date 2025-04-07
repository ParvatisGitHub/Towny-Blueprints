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
        Blueprint blueprint = blueprintCreation.get(player);

        // Basic Settings Section - Now separated
        inventory.setItem(10, createMenuItem(Material.NAME_TAG, "§e§lName", blueprint, "name"));
        inventory.setItem(11, createMenuItem(Material.BOOK, "§e§lDescription", blueprint, "description"));
        inventory.setItem(12, createMenuItem(Material.STRUCTURE_BLOCK, "§e§lDimensions", blueprint, "size"));

        // Block Requirements Section
        inventory.setItem(14, createMenuItem(Material.CHEST, "§e§lRequired Blocks", blueprint, "blocks"));
        inventory.setItem(15, createMenuItem(Material.BARRIER, "§e§lRemove Blocks", blueprint, "remove_blocks"));

        // Mob Requirements Section
        inventory.setItem(19, createMenuItem(Material.SPAWNER, "§e§lRequired Mobs", blueprint, "mobs"));
        inventory.setItem(20, createMenuItem(Material.BARRIER, "§e§lRemove Mobs", blueprint, "remove_mobs"));

        // Biome Settings Section
        inventory.setItem(22, createMenuItem(Material.GRASS_BLOCK, "§e§lRequired Biomes", blueprint, "required_biomes"));
        inventory.setItem(23, createMenuItem(Material.BARRIER, "§e§lRemove Required Biomes", blueprint, "remove_required_biomes"));
        inventory.setItem(24, createMenuItem(Material.DEAD_BUSH, "§e§lForbidden Biomes", blueprint, "forbidden_biomes"));
        inventory.setItem(25, createMenuItem(Material.BARRIER, "§e§lRemove Forbidden Biomes", blueprint, "remove_forbidden_biomes"));

        // Economy Settings Section
        inventory.setItem(28, createMenuItem(Material.GOLD_INGOT, "§e§lIncome Settings", blueprint, "income"));
        inventory.setItem(29, createMenuItem(Material.IRON_INGOT, "§e§lUpkeep Settings", blueprint, "upkeep"));
        inventory.setItem(30, createMenuItem(Material.EMERALD, "§e§lPlacement Cost", blueprint, "cost"));

        // Group Settings Section
        inventory.setItem(32, createMenuItem(Material.BOOKSHELF, "§e§lGroup Settings", blueprint, "group"));
        inventory.setItem(33, createMenuItem(Material.COMPARATOR, "§e§lUpkeep Sharing", blueprint, "shared_upkeep"));

        // Tool Settings Section
        inventory.setItem(34, createMenuItem(Material.DIAMOND_AXE, "§e§lTool Settings", blueprint, "tool"));

        // Add display material option
        inventory.setItem(35, createMenuItem(Material.ITEM_FRAME, "§e§lDisplay Material", blueprint, "display_material"));

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

        // Basic Settings Section - Now separated like create menu
        inventory.setItem(10, createMenuItem(Material.NAME_TAG, "§e§lName", blueprint, "name"));
        inventory.setItem(11, createMenuItem(Material.BOOK, "§e§lDescription", blueprint, "description"));
        inventory.setItem(12, createMenuItem(Material.STRUCTURE_BLOCK, "§e§lDimensions", blueprint, "size"));

        // Block Requirements Section
        inventory.setItem(14, createMenuItem(Material.CHEST, "§e§lRequired Blocks", blueprint, "blocks"));
        inventory.setItem(15, createMenuItem(Material.BARRIER, "§e§lRemove Blocks", blueprint, "remove_blocks"));

        // Mob Requirements Section
        inventory.setItem(19, createMenuItem(Material.SPAWNER, "§e§lRequired Mobs", blueprint, "mobs"));
        inventory.setItem(20, createMenuItem(Material.BARRIER, "§e§lRemove Mobs", blueprint, "remove_mobs"));

        // Biome Settings Section
        inventory.setItem(22, createMenuItem(Material.GRASS_BLOCK, "§e§lRequired Biomes", blueprint, "required_biomes"));
        inventory.setItem(23, createMenuItem(Material.BARRIER, "§e§lRemove Required Biomes", blueprint, "remove_required_biomes"));
        inventory.setItem(24, createMenuItem(Material.DEAD_BUSH, "§e§lForbidden Biomes", blueprint, "forbidden_biomes"));
        inventory.setItem(25, createMenuItem(Material.BARRIER, "§e§lRemove Forbidden Biomes", blueprint, "remove_forbidden_biomes"));

        // Economy Settings Section
        inventory.setItem(28, createMenuItem(Material.GOLD_INGOT, "§e§lIncome Settings", blueprint, "income"));
        inventory.setItem(29, createMenuItem(Material.IRON_INGOT, "§e§lUpkeep Settings", blueprint, "upkeep"));
        inventory.setItem(30, createMenuItem(Material.EMERALD, "§e§lPlacement Cost", blueprint, "cost"));

        // Group Settings Section
        inventory.setItem(32, createMenuItem(Material.BOOKSHELF, "§e§lGroup Settings", blueprint, "group"));
        inventory.setItem(33, createMenuItem(Material.COMPARATOR, "§e§lUpkeep Sharing", blueprint, "shared_upkeep"));

        // Tool Settings Section
        inventory.setItem(34, createMenuItem(Material.DIAMOND_AXE, "§e§lTool Settings", blueprint, "tool"));

        // Display Material Option
        inventory.setItem(35, createMenuItem(Material.ITEM_FRAME, "§e§lDisplay Material", blueprint, "display_material"));

        // Save/Cancel Section
        inventory.setItem(49, createMenuItem(Material.LIME_CONCRETE, "§a§lSave Changes", "§7Click to save changes"));
        inventory.setItem(50, createMenuItem(Material.RED_CONCRETE, "§c§lCancel", "§7Click to cancel editing"));

        player.openInventory(inventory);
    }

    public void openEditBlueprintSelection(Player player) {
        Inventory inventory = Bukkit.createInventory(null, BLUEPRINT_MENU_SIZE, "Select Blueprint to Edit");

        for (Blueprint blueprint : plugin.getBlueprintManager().getAllBlueprints()) {
            inventory.addItem(createBlueprintItem(blueprint));
        }

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

    private ItemStack createMenuItem(Material material, String name, Blueprint blueprint, String field) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();

            // Add current value based on field
            switch (field) {
                case "name":
                    lore.add("§7Current Name: §f" + blueprint.getName());
                    break;
                case "description":
                    lore.add("§7Current Description: §f" + blueprint.getDescription());
                    break;
                case "size":
                    lore.add("§7Current Size: §f" + blueprint.getSizeX() + "x" + blueprint.getSizeY() + "x" + blueprint.getSizeZ());
                    break;
                case "blocks":
                    lore.add("§7Current Block Requirements:");
                    blueprint.getRequiredBlocks().forEach((block, amount) ->
                            lore.add("§7- §f" + block + ": " + amount));
                    break;
                case "remove_blocks":
                    lore.add("§7Click to remove a block requirement");
                    blueprint.getRequiredBlocks().forEach((block, amount) ->
                            lore.add("§7- §f" + block + ": " + amount));
                    break;
                case "mobs":
                    lore.add("§7Current Mob Requirements:");
                    blueprint.getRequiredMobs().forEach((mob, amount) ->
                            lore.add("§7- §f" + mob + ": " + amount));
                    break;
                case "remove_mobs":
                    lore.add("§7Click to remove a mob requirement");
                    blueprint.getRequiredMobs().forEach((mob, amount) ->
                            lore.add("§7- §f" + mob + ": " + amount));
                    break;
                case "required_biomes":
                    lore.add("§7Current Required Biomes:");
                    blueprint.getRequiredBiomes().forEach(biome ->
                            lore.add("§7- §f" + biome));
                    break;
                case "remove_required_biomes":
                    lore.add("§7Click to remove a required biome");
                    blueprint.getRequiredBiomes().forEach(biome ->
                            lore.add("§7- §f" + biome));
                    break;
                case "forbidden_biomes":
                    lore.add("§7Current Forbidden Biomes:");
                    blueprint.getForbiddenBiomes().forEach(biome ->
                            lore.add("§7- §f" + biome));
                    break;
                case "remove_forbidden_biomes":
                    lore.add("§7Click to remove a forbidden biome");
                    blueprint.getForbiddenBiomes().forEach(biome ->
                            lore.add("§7- §f" + biome));
                    break;
                case "income":
                    if (blueprint.getIncomeType().startsWith("template:")) {
                        String templateName = blueprint.getIncomeType().substring(9);
                        lore.add("§7Current Income Template: §f" + templateName);
                        lore.addAll(plugin.getResourceTemplateManager().getResourcesDisplay(templateName));
                    } else {
                        lore.add("§7Current Income: §f" + blueprint.getDailyIncome() + " " + blueprint.getIncomeType());
                    }
                    break;
                case "upkeep":
                    if (blueprint.getUpkeepType().startsWith("template:")) {
                        String templateName = blueprint.getUpkeepType().substring(9);
                        lore.add("§7Current Upkeep Template: §f" + templateName);
                        lore.addAll(plugin.getResourceTemplateManager().getResourcesDisplay(templateName));
                    } else {
                        lore.add("§7Current Upkeep: §f" + blueprint.getDailyUpkeep() + " " + blueprint.getUpkeepType());
                    }
                    break;
                case "cost":
                    lore.add("§7Current Cost: §f" + blueprint.getPlacementCost());
                    break;
                case "group":
                    lore.add("§7Current Required Count: §f" + blueprint.getRequiredCount());
                    break;
                case "shared_upkeep":
                    lore.add("§7Shared Upkeep: §f" + blueprint.isSharedUpkeep());
                    lore.add("§7Multiplier: §f" + blueprint.getUpkeepMultiplier());
                    break;
                case "tool":
                    String toolType = blueprint.getToolType() != null ? blueprint.getToolType().name() : "None";
                    lore.add("§7Current Tool Type: §f" + toolType);
                    lore.add("§7Current Durability Drain: §f" + blueprint.getDurabilityDrain());
                    break;
                case "display_material":
                    lore.add("§7Current Display Material: §f" +
                            (blueprint.getDisplayMaterial() != null ? blueprint.getDisplayMaterial().name() : "PAPER"));
                    break;
            }

            lore.add("");
            lore.add("§eClick to modify");
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