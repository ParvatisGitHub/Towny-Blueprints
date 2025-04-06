package com.townyblueprints.models;

import lombok.Data;
import org.bukkit.Material;
import java.util.Map;
import java.util.HashMap;

@Data
public class Blueprint {
    private String name = "New Blueprint";
    private String description = "";
    private Map<String, Integer> requiredBlocks = new HashMap<>();
    private Map<String, Integer> requiredMobs = new HashMap<>(); // New field for mob requirements
    private Material displayMaterial = Material.PAPER;
    private int sizeX = 1;
    private int sizeY = 1;
    private int sizeZ = 1;
    private double dailyIncome = 0;
    private String incomeType = "MONEY";
    private double dailyUpkeep = 0;
    private String upkeepType = "MONEY";
    private double placementCost = 0;
    private String permissionNode;
    private int maxPerTown = -1;
    private int requiredTownLevel = 0;
    private Material toolType;
    private int durabilityDrain = 0;
    private int bonusTownBlocks = 0;
    private String type = "default";
    private String upgradesTo;
    private double upgradeCost = 0;

    // Fields for grouped upkeep
    private int requiredCount = 1;
    private boolean sharedUpkeep = false;
    private double upkeepMultiplier = 1.0;

    /**
     * Gets the required blocks map where:
     * - Keys are either Material names or block definition names (e.g., "OAK_LOG" or "logs")
     * - Values are the required quantities
     */
    public Map<String, Integer> getRequiredBlocks() {
        return requiredBlocks;
    }

    /**
     * Sets the required blocks map where:
     * - Keys are either Material names or block definition names (e.g., "OAK_LOG" or "logs")
     * - Values are the required quantities
     */
    public void setRequiredBlocks(Map<String, Integer> blocks) {
        this.requiredBlocks = blocks != null ? new HashMap<>(blocks) : new HashMap<>();
    }

    /**
     * Adds a required block to the blueprint
     * @param blockType Material name or block definition name
     * @param amount Required quantity
     */
    public void addRequiredBlock(String blockType, int amount) {
        if (blockType != null && amount > 0) {
            this.requiredBlocks.put(blockType, amount);
        }
    }

    /**
     * Removes a required block from the blueprint
     * @param blockType Material name or block definition name
     */
    public void removeRequiredBlock(String blockType) {
        if (blockType != null) {
            this.requiredBlocks.remove(blockType);
        }
    }

    /**
     * Gets the required quantity for a specific block type
     * @param blockType Material name or block definition name
     * @return Required quantity or 0 if not required
     */
    public int getRequiredBlockAmount(String blockType) {
        return blockType != null ? this.requiredBlocks.getOrDefault(blockType, 0) : 0;
    }

    /**
     * Checks if a specific block type is required
     * @param blockType Material name or block definition name
     * @return true if the block type is required
     */
    public boolean requiresBlock(String blockType) {
        return blockType != null && this.requiredBlocks.containsKey(blockType);
    }

    /**
     * Gets the required mobs map where:
     * - Keys are entity type names (e.g., "VILLAGER", "COW")
     * - Values are the required quantities
     */
    public Map<String, Integer> getRequiredMobs() {
        return requiredMobs;
    }

    /**
     * Sets the required mobs map
     * @param mobs Map of entity types and their required quantities
     */
    public void setRequiredMobs(Map<String, Integer> mobs) {
        this.requiredMobs = mobs != null ? new HashMap<>(mobs) : new HashMap<>();
    }

    /**
     * Adds a required mob to the blueprint
     * @param entityType Entity type name (e.g., "VILLAGER")
     * @param amount Required quantity
     */
    public void addRequiredMob(String entityType, int amount) {
        if (entityType != null && amount > 0) {
            this.requiredMobs.put(entityType.toUpperCase(), amount);
        }
    }

    /**
     * Removes a required mob from the blueprint
     * @param entityType Entity type name
     */
    public void removeRequiredMob(String entityType) {
        if (entityType != null) {
            this.requiredMobs.remove(entityType.toUpperCase());
        }
    }

    /**
     * Gets the required quantity for a specific mob type
     * @param entityType Entity type name
     * @return Required quantity or 0 if not required
     */
    public int getRequiredMobAmount(String entityType) {
        return entityType != null ? this.requiredMobs.getOrDefault(entityType.toUpperCase(), 0) : 0;
    }

    /**
     * Checks if a specific mob type is required
     * @param entityType Entity type name
     * @return true if the mob type is required
     */
    public boolean requiresMob(String entityType) {
        return entityType != null && this.requiredMobs.containsKey(entityType.toUpperCase());
    }

    /**
     * Calculates the actual upkeep cost based on the number of blueprints present
     * @param currentCount Current number of blueprints of this type
     * @return The actual upkeep cost
     */
    public double calculateUpkeep(int currentCount) {
        if (currentCount < requiredCount) {
            return dailyUpkeep;
        }

        if (sharedUpkeep) {
            return (dailyUpkeep * upkeepMultiplier) / currentCount;
        } else {
            return dailyUpkeep * upkeepMultiplier;
        }
    }

    /**
     * Calculates the actual income based on the number of blueprints present
     * @param currentCount Current number of blueprints of this type
     * @return The actual income amount
     */
    public double calculateIncome(int currentCount) {
        if (currentCount < requiredCount) {
            return 0;
        }

        if (sharedUpkeep) {
            return (dailyIncome * upkeepMultiplier) / currentCount;
        } else {
            return dailyIncome * upkeepMultiplier;
        }
    }
}