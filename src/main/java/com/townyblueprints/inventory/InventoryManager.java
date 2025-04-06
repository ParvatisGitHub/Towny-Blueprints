package com.townyblueprints.inventory;

import com.townyblueprints.TownyBlueprints;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.logging.Logger;

public class InventoryManager {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final InventoryOperations operations;
    private final InventoryScanner scanner;

    public InventoryManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.operations = new InventoryOperations(plugin);
        this.scanner = new InventoryScanner(plugin);
    }

    public List<Container> findContainers(Location baseLocation, int sizeX, int sizeY, int sizeZ) {
        return scanner.scanForContainers(baseLocation, sizeX, sizeY, sizeZ);
    }

    public boolean addItems(List<Container> containers, ItemStack items, Player player) {
        if (!Bukkit.isPrimaryThread()) {
            // Debug logging
            if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                logger.warning("[InventoryManager] Attempted to add items from non-main thread!");
            }
            return false;
        }

        return operations.addItems(containers, items, player);
    }

    public boolean removeItems(List<Container> containers, ItemStack required) {
        if (!Bukkit.isPrimaryThread()) {
            // Debug logging
            if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                logger.warning("[InventoryManager] Attempted to remove items from non-main thread!");
            }
            return false;
        }

        return operations.removeItems(containers, required);
    }

    public boolean drainToolDurability(List<Container> containers, Material toolType, int durabilityDrain) {
        if (!Bukkit.isPrimaryThread()) {
            // Debug logging
            if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                logger.warning("[InventoryManager] Attempted to drain tool durability from non-main thread!");
            }
            return false;
        }

        return operations.drainToolDurability(containers, toolType, durabilityDrain);
    }

    public boolean hasSpace(List<Container> containers) {
        if (!Bukkit.isPrimaryThread()) {
            // Debug logging
            if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                logger.warning("[InventoryManager] Attempted to check space from non-main thread!");
            }
            return false;
        }

        return operations.hasSpace(containers);
    }

    public int countItems(List<Container> containers, String itemType) {
        if (!Bukkit.isPrimaryThread()) {
            // Debug logging
            if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                logger.warning("[InventoryManager] Attempted to count items from non-main thread!");
            }
            return 0;
        }

        return operations.countItems(containers, itemType);
    }
}