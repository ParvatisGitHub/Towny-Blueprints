package com.townyblueprints.util;

import com.townyblueprints.TownyBlueprints;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for handling items from various sources including:
 * - Vanilla Minecraft
 * - ItemsAdder
 * - MMOItems
 * - MythicMobs
 * - Oraxen
 */
public class ItemUtil {
    private static final TownyBlueprints plugin = TownyBlueprints.getInstance();
    
    /**
     * Get an ItemStack from any supported source
     * Format:
     * - vanilla:MATERIAL_NAME
     * - itemsadder:namespace:id
     * - mmoitems:type:id
     * - mythicmobs:id
     * - oraxen:id
     */
    @Nullable
    public static ItemStack getItemStack(String fullId, int amount, Player player) {
        if (fullId == null) return null;
        
        String[] parts = fullId.split(":", 3);
        if (parts.length < 2) return null;
        
        String source = parts[0].toLowerCase();
        String id = parts.length == 3 ? parts[1] + ":" + parts[2] : parts[1];
        
        switch (source) {
            case "vanilla":
                try {
                    Material material = Material.valueOf(id.toUpperCase());
                    return new ItemStack(material, amount);
                } catch (IllegalArgumentException e) {
                    return null;
                }
                
            case "itemsadder":
                if (plugin.getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
                    try {
                        Class.forName("dev.lone.itemsadder.api.CustomStack");
                        return ItemsAdderUtil.getItemStack(id, amount);
                    } catch (ClassNotFoundException e) {
                        plugin.getLogger().warning("ItemsAdder API not found!");
                        return null;
                    }
                }
                return null;
                
            case "mmoitems":
                if (plugin.getServer().getPluginManager().getPlugin("MMOItems") != null) {
                    try {
                        Class.forName("net.Indyuce.mmoitems.MMOItems");
                        return MMOItemsUtil.getMMOItemsItemStack(id, player);
                    } catch (ClassNotFoundException e) {
                        plugin.getLogger().warning("MMOItems API not found!");
                        return null;
                    }
                }
                return null;
                
            case "mythicmobs":
                if (plugin.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
                    try {
                        Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
                        ItemStack item = MythicMobsUtil.getMythicItemStack(id);
                        if (item != null) {
                            item.setAmount(amount);
                            return item;
                        }
                    } catch (ClassNotFoundException e) {
                        plugin.getLogger().warning("MythicMobs API not found!");
                    }
                }
                return null;
                
            case "oraxen":
                if (plugin.getServer().getPluginManager().getPlugin("Oraxen") != null) {
                    try {
                        Class.forName("io.th0rgal.oraxen.api.OraxenItems");
                        return OraxenUtil.getItemStack(id, amount);
                    } catch (ClassNotFoundException e) {
                        plugin.getLogger().warning("Oraxen API not found!");
                        return null;
                    }
                }
                return null;
                
            default:
                return null;
        }
    }
    
    /**
     * Check if an item ID is valid for any supported source
     */
    public static boolean isValidItem(String fullId) {
        if (fullId == null) return false;
        
        String[] parts = fullId.split(":", 3);
        if (parts.length < 2) return false;
        
        String source = parts[0].toLowerCase();
        String id = parts.length == 3 ? parts[1] + ":" + parts[2] : parts[1];
        
        switch (source) {
            case "vanilla":
                try {
                    Material.valueOf(id.toUpperCase());
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
                
            case "itemsadder":
                if (plugin.getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
                    try {
                        Class.forName("dev.lone.itemsadder.api.CustomStack");
                        return ItemsAdderUtil.isValidItem(id);
                    } catch (ClassNotFoundException e) {
                        return false;
                    }
                }
                return false;
                
            case "mmoitems":
                if (plugin.getServer().getPluginManager().getPlugin("MMOItems") != null) {
                    try {
                        Class.forName("net.Indyuce.mmoitems.MMOItems");
                        return MMOItemsUtil.isValidItem(id);
                    } catch (ClassNotFoundException e) {
                        return false;
                    }
                }
                return false;
                
            case "mythicmobs":
                if (plugin.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
                    try {
                        Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
                        return MythicMobsUtil.isValidItem(id);
                    } catch (ClassNotFoundException e) {
                        return false;
                    }
                }
                return false;
                
            case "oraxen":
                if (plugin.getServer().getPluginManager().getPlugin("Oraxen") != null) {
                    try {
                        Class.forName("io.th0rgal.oraxen.api.OraxenItems");
                        return OraxenUtil.isValidItem(id);
                    } catch (ClassNotFoundException e) {
                        return false;
                    }
                }
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Get a display name for an item from any supported source
     */
    @Nullable
    public static String getDisplayName(String fullId) {
        if (fullId == null) return null;
        
        String[] parts = fullId.split(":", 3);
        if (parts.length < 2) return null;
        
        String source = parts[0].toLowerCase();
        String id = parts.length == 3 ? parts[1] + ":" + parts[2] : parts[1];
        
        switch (source) {
            case "vanilla":
                try {
                    return Material.valueOf(id.toUpperCase()).name();
                } catch (IllegalArgumentException e) {
                    return null;
                }
                
            case "itemsadder":
                if (plugin.getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
                    try {
                        Class.forName("dev.lone.itemsadder.api.CustomStack");
                        return ItemsAdderUtil.getMaterialNameForDisplay(id);
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                }
                return null;
                
            case "mmoitems":
                if (plugin.getServer().getPluginManager().getPlugin("MMOItems") != null) {
                    try {
                        Class.forName("net.Indyuce.mmoitems.MMOItems");
                        return MMOItemsUtil.getMaterialNameForDisplay(id);
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                }
                return null;
                
            case "mythicmobs":
                if (plugin.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
                    try {
                        Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
                        return MythicMobsUtil.getMaterialNameForDisplay(id);
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                }
                return null;
                
            case "oraxen":
                if (plugin.getServer().getPluginManager().getPlugin("Oraxen") != null) {
                    try {
                        Class.forName("io.th0rgal.oraxen.api.OraxenItems");
                        return OraxenUtil.getMaterialNameForDisplay(id);
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                }
                return null;
                
            default:
                return null;
        }
    }
}