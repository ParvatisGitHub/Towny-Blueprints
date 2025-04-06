package com.townyblueprints.models;

import com.townyblueprints.TownyBlueprints;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.townyblueprints.inventory.InventoryManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@Data
public class WarehouseData {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final PlacedBlueprint blueprint;
    private final InventoryManager inventoryManager;
    private List<Container> containers;
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public WarehouseData(JavaPlugin plugin, PlacedBlueprint blueprint) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.blueprint = blueprint;
        this.inventoryManager = new InventoryManager(plugin);
        scanForContainers();
    }

    private void scanForContainers() {
        if (!isScanning.compareAndSet(false, true)) {
            logger.warning("[WarehouseData] Container scan already in progress!");
            return;
        }

        try {
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(plugin, this::performScan);
            } else {
                performScan();
            }
        } finally {
            isScanning.set(false);
        }
    }

    private void performScan() {
        Location base = blueprint.getLocation();
        containers = inventoryManager.findContainers(
                base,
                blueprint.getBlueprint().getSizeX(),
                blueprint.getBlueprint().getSizeY(),
                blueprint.getBlueprint().getSizeZ()
        );
        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.info("[WarehouseData] Scanned and found " + containers.size() + " containers");
        }
    }

    public boolean addItems(ItemStack items, Player player) {
        if (!ensureMainThread()) return false;
        if (!checkContainers()) return false;

        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.info("[WarehouseData] Attempting to add " + items.getAmount() + " " + items.getType().name());
        }
        boolean result = inventoryManager.addItems(containers, items, player);
        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.info("[WarehouseData] Add operation " + (result ? "successful" : "failed"));
        }
        return result;
    }

    public boolean removeItems(ItemStack required) {
        if (!ensureMainThread()) return false;
        if (!checkContainers()) return false;

        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.info("[WarehouseData] Attempting to remove " + required.getAmount() + " " + required.getType().name());

        }boolean result = inventoryManager.removeItems(containers, required);
        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.info("[WarehouseData] Remove operation " + (result ? "successful" : "failed"));
        }
        return result;
    }

    public boolean drainToolDurability(Material toolType, int durabilityDrain) {
        if (!ensureMainThread()) return false;
        if (!checkContainers()) return false;

        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.info("[WarehouseData] Attempting to drain " + durabilityDrain + " durability from " + toolType.name());
        }
        boolean result = inventoryManager.drainToolDurability(containers, toolType, durabilityDrain);
        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.info("[WarehouseData] Durability drain " + (result ? "successful" : "failed"));
        }
        return result;
    }

    public boolean hasSpace() {
        if (!ensureMainThread()) return false;
        if (!checkContainers()) return false;

        return inventoryManager.hasSpace(containers);
    }

    public int countItems(String itemType) {
        if (!ensureMainThread()) return 0;
        if (!checkContainers()) return 0;

        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.info("[WarehouseData] Counting items of type: " + itemType);
        }
        int count = inventoryManager.countItems(containers, itemType);
        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.info("[WarehouseData] Found " + count + " items");
        }
        return count;
    }

    private boolean ensureMainThread() {
        if (!Bukkit.isPrimaryThread()) {
            // Debug logging
            if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                logger.severe("[WarehouseData] Attempted to perform inventory operation from non-main thread!");
            }
            return false;
        }
        return true;
    }

    private boolean checkContainers() {
        if (containers == null || containers.isEmpty()) {
            // Debug logging
            if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                logger.warning("[WarehouseData] No containers available!");
            }
            scanForContainers();
            return false;
        }
        return true;
    }

    public void refresh() {
        scanForContainers();
    }
}