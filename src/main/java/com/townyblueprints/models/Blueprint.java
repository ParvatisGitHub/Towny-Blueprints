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

    // New fields for grouped upkeep
    private int requiredCount = 1; // Number of blueprints required for upkeep
    private boolean sharedUpkeep = false; // Whether upkeep is shared between all blueprints of this type
    private double upkeepMultiplier = 1.0; // Multiplier for upkeep when all required blueprints are present

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
     * Calculates the actual upkeep cost based on the number of blueprints present
     * @param currentCount Current number of blueprints of this type
     * @return The actual upkeep cost
     */
    public double calculateUpkeep(int currentCount) {
        if (currentCount < requiredCount) {
            return dailyUpkeep; // Full upkeep cost when not enough blueprints
        }

        if (sharedUpkeep) {
            // Shared upkeep means the total cost is split between all blueprints
            return (dailyUpkeep * upkeepMultiplier) / currentCount;
        } else {
            // Individual upkeep with bonus for having enough blueprints
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
            return 0; // No income if not enough blueprints
        }

        if (sharedUpkeep) {
            // Shared income means the total income is split between all blueprints
            return (dailyIncome * upkeepMultiplier) / currentCount;
        } else {
            // Individual income with bonus for having enough blueprints
            return dailyIncome * upkeepMultiplier;
        }
    }
}