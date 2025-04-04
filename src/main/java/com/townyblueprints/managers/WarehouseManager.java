package com.townyblueprints.managers;

import com.palmergames.bukkit.towny.object.Town;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.PlacedBlueprint;
import com.townyblueprints.models.WarehouseData;
import com.townyblueprints.util.ItemUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class WarehouseManager {
    private final TownyBlueprints plugin;
    private final Map<Town, List<WarehouseData>> townWarehouses = new HashMap<>();
    private final Object inventoryLock = new Object();

    public void loadWarehouses() {
        synchronized(inventoryLock) {
            townWarehouses.clear();
            
            for (PlacedBlueprint blueprint : plugin.getBlueprintManager().getAllPlacedBlueprints()) {
                if (blueprint.getBlueprint().getName().toLowerCase().contains("warehouse") && blueprint.isActive()) {
                    WarehouseData warehouse = new WarehouseData(plugin, blueprint);
                    townWarehouses.computeIfAbsent(blueprint.getTown(), k -> new ArrayList<>()).add(warehouse);
                }
            }
        }
    }

    public boolean storeItems(Town town, ItemStack items, Player player) {
        synchronized(inventoryLock) {
            List<WarehouseData> warehouses = townWarehouses.get(town);
            if (warehouses == null || warehouses.isEmpty()) {
                return false;
            }

            for (WarehouseData warehouse : warehouses) {
                if (warehouse.getBlueprint().isActive() && warehouse.hasSpace()) {
                    return warehouse.addItems(items, player);
                }
            }
            
            return false;
        }
    }

    public boolean removeItems(Town town, String itemType, int amount) {
        synchronized(inventoryLock) {
            List<WarehouseData> warehouses = townWarehouses.get(town);
            if (warehouses == null || warehouses.isEmpty()) {
                return false;
            }

            ItemStack required;
            if (itemType.startsWith("vanilla:")) {
                String cleanItemType = itemType.replace("vanilla:", "").toUpperCase();
                try {
                    Material material = Material.valueOf(cleanItemType);
                    required = new ItemStack(material, amount);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material: " + cleanItemType);
                    return false;
                }
            } else {
                required = ItemUtil.getItemStack(itemType, amount, null);
                if (required == null) {
                    plugin.getLogger().warning("Could not create ItemStack for: " + itemType);
                    return false;
                }
            }

            for (WarehouseData warehouse : warehouses) {
                if (warehouse.getBlueprint().isActive() && warehouse.removeItems(required)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void refreshWarehouse(PlacedBlueprint blueprint) {
        synchronized(inventoryLock) {
            if (!blueprint.getBlueprint().getName().toLowerCase().contains("warehouse")) {
                return;
            }
            
            Town town = blueprint.getTown();
            List<WarehouseData> warehouses = townWarehouses.get(town);
            
            if (warehouses != null) {
                warehouses.removeIf(w -> w.getBlueprint().equals(blueprint));
                
                if (blueprint.isActive()) {
                    warehouses.add(new WarehouseData(plugin, blueprint));
                }
            } else if (blueprint.isActive()) {
                List<WarehouseData> newList = new ArrayList<>();
                newList.add(new WarehouseData(plugin, blueprint));
                townWarehouses.put(town, newList);
            }
        }
    }

    public boolean drainToolDurability(Town town, Material toolType, int durabilityDrain) {
        synchronized(inventoryLock) {
            List<WarehouseData> warehouses = townWarehouses.get(town);
            if (warehouses == null || warehouses.isEmpty()) {
                return false;
            }

            for (WarehouseData warehouse : warehouses) {
                if (warehouse.getBlueprint().isActive() && warehouse.drainToolDurability(toolType, durabilityDrain)) {
                    return true;
                }
            }
            return false;
        }
    }

    public List<WarehouseData> getTownWarehouses(Town town) {
        synchronized(inventoryLock) {
            return new ArrayList<>(townWarehouses.getOrDefault(town, new ArrayList<>()));
        }
    }
	
	public boolean verifyWarehouseContents(Town town, String itemType, int amount) {
		List<WarehouseData> warehouses = townWarehouses.get(town);
		if (warehouses == null || warehouses.isEmpty()) {
			plugin.getLogger().warning("[Warehouse] No warehouses found for town " + town.getName());
			return false;
		}

		plugin.getLogger().info("[Warehouse] Verifying contents for " + itemType + " (amount: " + amount + ")");
    
		for (WarehouseData warehouse : warehouses) {
			if (warehouse.getBlueprint().isActive()) {
				plugin.getLogger().info("[Warehouse] Checking warehouse at " + warehouse.getBlueprint().getLocation());
            
				// Count items in warehouse
				int totalFound = warehouse.countItems(itemType);
				plugin.getLogger().info("[Warehouse] Found " + totalFound + " items of type " + itemType);
            
				if (totalFound >= amount) {
					return true;
				}
			}
		}
    
		return false;
	}

}