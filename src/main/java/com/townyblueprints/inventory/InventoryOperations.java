package com.townyblueprints.inventory;

import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import java.util.*;
import java.util.logging.Logger;

public class InventoryOperations {
    private final JavaPlugin plugin;
    private final Logger logger;

    public InventoryOperations(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean removeItems(List<Container> containers, ItemStack required) {
    if (!Bukkit.isPrimaryThread()) {
        throw new IllegalStateException("Inventory operations must be done on the main thread!");
    }

    if (containers.isEmpty()) {
        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.warning("[InventoryOperations] No containers provided!");
        }
		return false;
    }

    int toRemove = required.getAmount();
        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.info("[InventoryOperations] Attempting to remove " + toRemove + " " + required.getType().name());
        }

    // First, verify we have enough items across all containers
    int totalAvailable = 0;
    for (Container container : containers) {
        Inventory inv = container.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.isSimilar(required)) {
                totalAvailable += item.getAmount();
            }
        }
    }

    if (totalAvailable < toRemove) {
        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.warning("[InventoryOperations] Not enough items! Found: " + totalAvailable + ", Need: " + toRemove);
        }
        return false; // Not enough items to remove
    }

    int remainingToRemove = toRemove;

    // Loop over each container
    for (Container container : containers) {
        if (remainingToRemove <= 0) break; // Exit if we have removed enough items

        Inventory inv = container.getInventory();
        boolean modified = false;

        // Iterate over the inventory slots
        for (int slot = 0; slot < inv.getSize() && remainingToRemove > 0; slot++) {
            ItemStack currentItem = inv.getItem(slot);

            if (currentItem != null && currentItem.isSimilar(required)) {
                int currentAmount = currentItem.getAmount();

                if (currentAmount <= remainingToRemove) {
                    // If the current stack can be entirely removed
                    remainingToRemove -= currentAmount;
                    inv.setItem(slot, null); // Remove the whole stack
                    modified = true;
                    // Debug logging
                    if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                    logger.info("[InventoryOperations] Removed entire stack of " + currentAmount +
                            " from container " + containers.indexOf(container) + " slot " + slot);
                    }
                } else {
                    // Remove part of the stack
                    currentItem.setAmount(currentAmount - remainingToRemove); // Reduce the stack size
                    inv.setItem(slot, currentItem); // Update the item stack in the inventory
                    remainingToRemove = 0; // All items have been removed
                    modified = true;
                    // Debug logging
                    if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                        logger.info("[InventoryOperations] Removed " + remainingToRemove +
                                " items, leaving " + (currentAmount - remainingToRemove) +
                                " in container " + containers.indexOf(container) + " slot " + slot);
                    }
                }
            }
        }

        // Update the inventory in the container if modified (only modify inventory, no block update)
        if (modified) {
            // Debug logging
            if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                logger.info("[InventoryOperations] Inventory modified for container " + containers.indexOf(container));
            }
        }
    }
    boolean success = remainingToRemove == 0;
        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.info("[InventoryOperations] Remove operation " + (success ? "successful" : "failed") +
                    " (Remaining to remove: " + remainingToRemove + ")");
        }
	 if (!success) {
        // If we haven't removed all items, calculate how many were successfully removed
        int removedAmount = toRemove - remainingToRemove;
        int itemsToAddBack = removedAmount;

         // Debug logging
         if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
             logger.info("[InventoryOperations] Partial removal detected. Adding back " + itemsToAddBack + " items.");
         }
        addItems(containers, new ItemStack(required.getType(), itemsToAddBack), null);
    }
	return success;
}




    public boolean addItems(List<Container> containers, ItemStack items, Player player) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Inventory operations must be done on the main thread!");
        }

        if (containers.isEmpty()) {
            // Debug logging
            if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                logger.warning("[InventoryOperations] No containers provided!");
            }
            return false;
        }

        int toAdd = items.getAmount();
        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.info("[InventoryOperations] Attempting to add " + toAdd + " " + items.getType().name());
        }

        int remainingToAdd = toAdd;
        boolean modified = false;

        // Loop over each container
        for (Container container : containers) {
            if (remainingToAdd <= 0) break;

            Inventory inv = container.getInventory();

            // First try to stack with existing items
            for (int slot = 0; slot < inv.getSize() && remainingToAdd > 0; slot++) {
                ItemStack current = inv.getItem(slot);
                if (current != null && current.isSimilar(items) && current.getAmount() < current.getMaxStackSize()) {
                    int space = current.getMaxStackSize() - current.getAmount();
                    int add = Math.min(space, remainingToAdd);
                    current.setAmount(current.getAmount() + add);
                    remainingToAdd -= add;
                    modified = true;
                    // Debug logging
                    if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                        logger.info("[InventoryOperations] Added " + add + " to existing stack in slot " + slot);
                    }
                }
            }

            // Then try empty slots
            for (int slot = 0; slot < inv.getSize() && remainingToAdd > 0; slot++) {
                ItemStack current = inv.getItem(slot);
                if (current == null || current.getType() == Material.AIR) {
                    int add = Math.min(items.getMaxStackSize(), remainingToAdd);
                    ItemStack newStack = items.clone();
                    newStack.setAmount(add);
                    inv.setItem(slot, newStack);
                    remainingToAdd -= add;
                    modified = true;
                    // Debug logging
                    if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                    logger.info("[InventoryOperations] Added " + add + " to empty slot " + slot);
                    }
                }
            }
        }

		boolean success = remainingToAdd == 0;
        // Debug logging
        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
            logger.info("[InventoryOperations] Add operation " + (success ? "successful" : "failed") +
                    " (Remaining to add: " + remainingToAdd + ")");
        }
	   // Handle remaining items
        if (!success) {
			if (remainingToAdd > 0) {
				// Calculate how many items couldn't be added
				int itemsNotAdded = remainingToAdd;
				ItemStack leftoverItems = items.clone();
				leftoverItems.setAmount(itemsNotAdded);

				if (player != null) {
					// If there's a player, try their inventory first
					player.sendMessage("§eThe warehouse is full, some items couldn't be added. Trying to add them to your inventory...");
					Map<Integer, ItemStack> overflow = player.getInventory().addItem(leftoverItems);

					if (!overflow.isEmpty()) {
						// If player inventory is also full, drop at their feet
						for (ItemStack drop : overflow.values()) {
							player.getWorld().dropItemNaturally(player.getLocation(), drop);
						}
						player.sendMessage("§eYour inventory is full, the remaining items were dropped at your feet.");
					}
				} else {
					// If no player is specified, drop items in front of the first container
					if (!containers.isEmpty()) {
						Container firstContainer = containers.get(0);
						Location dropLocation = firstContainer.getLocation().add(0.5, 0.5, 0.5); // Center of the block
						firstContainer.getWorld().dropItemNaturally(dropLocation, leftoverItems);
                        // Debug logging
                        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                            logger.info("[InventoryOperations] Dropped " + itemsNotAdded + " items at warehouse location: " + dropLocation);
                        }
					} else {
                        // Debug logging
                        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                            logger.warning("[InventoryOperations] No containers available to drop items!");
                        }
						return false;
					}
				}
			}
		}
        return success;
    }

// Helper method to add items to the player's inventory
private boolean addItemsToPlayerInventory(Player player, ItemStack item, int remainingItems) {
    int toAdd = remainingItems;

    // Try to add items to the player's inventory
    Inventory inv = player.getInventory();
    boolean modified = false;

    // Try to stack with existing items
    for (int slot = 0; slot < inv.getSize() && toAdd > 0; slot++) {
        ItemStack current = inv.getItem(slot);
        if (current != null && current.isSimilar(item) && current.getAmount() < current.getMaxStackSize()) {
            int space = current.getMaxStackSize() - current.getAmount();
            int add = Math.min(space, toAdd);
            current.setAmount(current.getAmount() + add);
            toAdd -= add;
            modified = true;
        }
    }

    // Then try empty slots
    for (int slot = 0; slot < inv.getSize() && toAdd > 0; slot++) {
        ItemStack current = inv.getItem(slot);
        if (current == null || current.getType() == Material.AIR) {
            int add = Math.min(item.getMaxStackSize(), toAdd);
            ItemStack newStack = item.clone();
            newStack.setAmount(add);
            inv.setItem(slot, newStack);
            toAdd -= add;
            modified = true;
        }
    }

    return toAdd == 0;  // Return true if all items were added to the player's inventory
}

// Helper method to drop items at the player's feet
private void dropItemsAtFeet(Player player, ItemStack item, int remainingItems) {
    Location location = player.getLocation();
    ItemStack toDrop = item.clone();
    toDrop.setAmount(remainingItems);
    player.getWorld().dropItemNaturally(location, toDrop);
}

    public boolean drainToolDurability(List<Container> containers, Material toolType, int durabilityDrain) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Inventory operations must be done on the main thread!");
        }

        if (containers.isEmpty()) {
            // Debug logging
            if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                logger.warning("[InventoryOperations] No containers provided!");
            }
            return false;
        }

        for (Container container : containers) {
            Inventory inv = container.getInventory();
            boolean modified = false;

            for (int slot = 0; slot < inv.getSize(); slot++) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() == toolType && item.getItemMeta() instanceof Damageable) {
                    Damageable meta = (Damageable) item.getItemMeta();
                    int maxDurability = item.getType().getMaxDurability();
                    int currentDurability = maxDurability - meta.getDamage();

                    if (currentDurability > durabilityDrain) {
                        meta.setDamage(meta.getDamage() + durabilityDrain);
                        item.setItemMeta(meta);
                        inv.setItem(slot, item);
                        modified = true;
                        // Debug logging
                        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                            logger.info("[InventoryOperations] Drained " + durabilityDrain + " durability from tool in slot " + slot);
                        }
                        return true; // Return true when durability is successfully drained
                    } else {
                        inv.clear(slot);
                        modified = true;
                        // Debug logging
                        if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                            logger.info("[InventoryOperations] Tool in slot " + slot + " broke during use");
                        }
                        return true; // Return true when tool breaks (durability fully consumed)
                    }
                }
            }

            if (modified) {
                container.update(true);
            }
        }

        return false;
    }

    public boolean hasSpace(List<Container> containers) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Inventory operations must be done on the main thread!");
        }

        for (Container container : containers) {
            if (container.getInventory().firstEmpty() != -1) {
                return true;
            }
        }
        return false;
    }

    public int countItems(List<Container> containers, String itemType) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Inventory operations must be done on the main thread!");
        }

        int count = 0;
        for (Container container : containers) {
            Inventory inv = container.getInventory();

            for (ItemStack item : inv.getContents()) {
                if (item != null) {
                    if (itemType.startsWith("vanilla:")) {
                        String materialName = itemType.replace("vanilla:", "").toUpperCase();
                        if (item.getType().name().equals(materialName)) {
                            count += item.getAmount();
                        }
                    } else {
                        ItemStack compareItem = ItemUtil.getItemStack(itemType, 1, null);
                        if (compareItem != null && item.isSimilar(compareItem)) {
                            count += item.getAmount();
                        }
                    }
                }
            }
        }

        return count;
    }
}