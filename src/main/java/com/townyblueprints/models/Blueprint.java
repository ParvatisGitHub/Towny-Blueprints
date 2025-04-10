package com.townyblueprints.models;

import lombok.Data;
import org.bukkit.Material;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

@Data
public class Blueprint {
    private String name = "New Blueprint";
    private String description = "";
    private Map<String, Integer> requiredBlocks = new HashMap<>();
    private Map<String, Integer> requiredMobs = new HashMap<>();
    private Set<String> requiredBiomes = new HashSet<>();
    private Set<String> forbiddenBiomes = new HashSet<>();
    private Set<String> requiredSchematics = new HashSet<>();
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
    private Set<String> requiredSchematic;
    private int buildLoad = 0;
    private int requiredCount = 1;
    private boolean sharedUpkeep = false;
    private double upkeepMultiplier = 1.0;

    public void setName(String name) {
        this.name = name;
        // Automatically set permission node when name is set
        this.permissionNode = "townyblueprints.blueprint." + name.toLowerCase().replace(" ", "_");
    }

    public Map<String, Integer> getRequiredBlocks() {
        return requiredBlocks;
    }

    public void setRequiredBlocks(Map<String, Integer> blocks) {
        this.requiredBlocks = blocks != null ? new HashMap<>(blocks) : new HashMap<>();
    }

    public void addRequiredBlock(String blockType, int amount) {
        if (blockType != null && amount > 0) {
            this.requiredBlocks.put(blockType, amount);
        }
    }

    public boolean removeRequiredBlock(String blockType) {
        if (blockType != null && this.requiredBlocks.containsKey(blockType)) {
            this.requiredBlocks.remove(blockType);
            return true;
        }
        return false;
    }

    public int getRequiredBlockAmount(String blockType) {
        return blockType != null ? this.requiredBlocks.getOrDefault(blockType, 0) : 0;
    }

    public boolean requiresBlock(String blockType) {
        return blockType != null && this.requiredBlocks.containsKey(blockType);
    }

    public Map<String, Integer> getRequiredMobs() {
        return requiredMobs;
    }

    public void setRequiredMobs(Map<String, Integer> mobs) {
        this.requiredMobs = mobs != null ? new HashMap<>(mobs) : new HashMap<>();
    }

    public void addRequiredMob(String entityType, int amount) {
        if (entityType != null && amount > 0) {
            this.requiredMobs.put(entityType.toUpperCase(), amount);
        }
    }

    public boolean removeRequiredMob(String entityType) {
        if (entityType != null && this.requiredMobs.containsKey(entityType.toUpperCase())) {
            this.requiredMobs.remove(entityType.toUpperCase());
            return true;
        }
        return false;
    }

    public int getRequiredMobAmount(String entityType) {
        return entityType != null ? this.requiredMobs.getOrDefault(entityType.toUpperCase(), 0) : 0;
    }

    public boolean requiresMob(String entityType) {
        return entityType != null && this.requiredMobs.containsKey(entityType.toUpperCase());
    }

    public void addRequiredBiome(String biome) {
        if (biome != null) {
            this.requiredBiomes.add(biome.toUpperCase());
        }
    }

    public boolean removeRequiredBiome(String biome) {
        if (biome != null && this.requiredBiomes.contains(biome.toUpperCase())) {
            this.requiredBiomes.remove(biome.toUpperCase());
            return true;
        }
        return false;
    }

    public void addForbiddenBiome(String biome) {
        if (biome != null) {
            this.forbiddenBiomes.add(biome.toUpperCase());
        }
    }

    public boolean removeForbiddenBiome(String biome) {
        if (biome != null && this.forbiddenBiomes.contains(biome.toUpperCase())) {
            this.forbiddenBiomes.remove(biome.toUpperCase());
            return true;
        }
        return false;
    }

    public boolean requiresBiome(String biome) {
        return biome != null && this.requiredBiomes.contains(biome.toUpperCase());
    }

    public boolean isBiomeForbidden(String biome) {
        return biome != null && this.forbiddenBiomes.contains(biome.toUpperCase());
    }

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
    public Set<String> getRequiredSchematic() {
        return requiredSchematic;
    }

    public void setRequiredSchematic(String requiredSchematic) {
        this.requiredSchematic.add(requiredSchematic);
    }

    public int getBuildLoad() {
        return buildLoad;
    }

    public void setBuildLoad(int buildLoad) {
        this.buildLoad = buildLoad;
    }
}