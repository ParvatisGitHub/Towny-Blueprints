package com.townyblueprints.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.townyblueprints.TownyBlueprints;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.player.PlayerData;

public class MMOItemsUtil {

    static Function<String, ItemStack> itemFunction = (str) -> getMMOItemsItemStackSync(str);
    static Function<ItemStack, String> displayNameFunction = (item) -> item.getItemMeta().getDisplayName();
    private static final Executor MAIN_THREAD_EXECUTOR = runnable -> 
        TownyBlueprints.getInstance().getServer().getScheduler().runTask(TownyBlueprints.getInstance(), runnable);
    
    public static String getMaterialNameForDisplay(String materialName) {
        ItemStack item = itemFunction.apply(materialName);
        return item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
            ? item.getItemMeta().getDisplayName() 
            : materialName;
    }

    public static boolean isValidItem(String materialName) {
        return getMMOItemsMMOItem(materialName) != null;
    }

    private static MMOItem getMMOItemsMMOItem(String materialName) {
        try {
            return MMOItems.plugin.getMMOItem(getType(materialName), getID(materialName));
        } catch (Exception e) {
            return null;
        }
    }

    public static ItemStack getMMOItemsItemStack(String materialName, Player player) {
        try {
            if (!TownyBlueprints.getInstance().getConfig().getBoolean("mmoitems.level_to_player", false)) {
                return getMMOItemsItemStack(materialName);
            }
            return MMOItems.plugin.getItem(getType(materialName), getID(materialName), PlayerData.get(player));
        } catch (Exception e) {
            return null;
        }
    }

    public static ItemStack getMMOItemsItemStack(String materialName) {
        try {
            return MMOItems.plugin.getItem(getType(materialName), getID(materialName));
        } catch (Exception e) {
            return null;
        }
    }

    public static ItemStack getMMOItemsItemStackSync(String materialName) {
        MMOItem mmoItem = getMMOItemsMMOItem(materialName);
        if (mmoItem == null) {
            return null;
        }

        if (Bukkit.isPrimaryThread()) {
            try {
                return mmoItem.newBuilder().build();
            } catch (Exception e) {
                return null;
            }
        }

        CompletableFuture<ItemStack> future = CompletableFuture.supplyAsync(() -> {
            try {
                return mmoItem.newBuilder().build();
            } catch (Exception e) {
                return null;
            }
        }, MAIN_THREAD_EXECUTOR);

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    public static Type getType(String name) {
        try {
            return Type.get(name.split(":")[0]);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String getID(String name) {
        String[] parts = name.split(":");
        return parts.length > 1 ? parts[1] : null;
    }
}