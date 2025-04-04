package com.townyblueprints.util;

import java.util.Optional;

import org.bukkit.inventory.ItemStack;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;

public class MythicMobsUtil {

    public static ItemStack getMythicItemStack(String materialName) {
        return MythicBukkit.inst().getItemManager().getItemStack(materialName);
    }
    
    public static boolean isValidItem(String materialName) {
        ItemStack mythicItem = getMythicItemStack(materialName);
        if (mythicItem != null)
            return true;
        return false;
    }

    public static String getMaterialNameForDisplay(String materialName) {
        Optional<MythicItem> maybeMythicItem = MythicBukkit.inst().getItemManager().getItem(materialName);
        if (maybeMythicItem.isPresent()) {
            MythicItem mythicItem = maybeMythicItem.get();
            String maybeDisplayName = mythicItem.getDisplayName();
            if (maybeDisplayName != null) {
                return maybeDisplayName.replaceAll("[^\\w\\s]\\w", "");
            } else {
                if (mythicItem.getConfig().isSet("ItemStack")) {
                    ItemStack is = mythicItem.getConfig().getItemStack("ItemStack", (String) null);
                    if (is != null && is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
                        return is.getItemMeta().getDisplayName();
                    }
                }
            }
        }
        return null;
    }
}