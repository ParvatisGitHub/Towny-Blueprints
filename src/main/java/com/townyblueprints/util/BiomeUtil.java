package com.townyblueprints.util;

import org.bukkit.block.Biome;
import org.bukkit.NamespacedKey;
import java.util.*;

public class BiomeUtil {
    private final static List<String> overWorldBiomeNames = Arrays.asList(
            "BADLANDS", "BAMBOO_JUNGLE", "BEACH", "BIRCH_FOREST",
            "COLD_OCEAN", "DARK_FOREST", "DEEP_COLD_OCEAN", "DEEP_DARK",
            "DEEP_FROZEN_OCEAN", "DEEP_LUKEWARM_OCEAN", "DEEP_OCEAN",
            "DESERT", "DRIPSTONE_CAVES", "ERODED_BADLANDS", "FLOWER_FOREST",
            "FOREST", "FROZEN_OCEAN", "FROZEN_PEAKS", "FROZEN_RIVER",
            "GROVE", "ICE_SPIKES", "JAGGED_PEAKS", "JUNGLE", "LUKEWARM_OCEAN",
            "LUSH_CAVES", "MANGROVE_SWAMP", "MEADOW", "MUSHROOM_FIELDS",
            "OCEAN", "OLD_GROWTH_BIRCH_FOREST", "OLD_GROWTH_PINE_TAIGA",
            "OLD_GROWTH_SPRUCE_TAIGA", "PLAINS", "RIVER", "SAVANNA",
            "SAVANNA_PLATEAU", "SNOWY_BEACH", "SNOWY_PLAINS", "SNOWY_SLOPES",
            "SNOWY_TAIGA", "SPARSE_JUNGLE", "STONY_PEAKS", "STONY_SHORE",
            "SUNFLOWER_PLAINS", "SWAMP", "TAIGA", "WARM_OCEAN", "WINDSWEPT_FOREST",
            "WINDSWEPT_GRAVELLY_HILLS", "WINDSWEPT_HILLS", "WINDSWEPT_SAVANNA",
            "WOODED_BADLANDS"
    );

    private final static List<String> netherBiomeNames = Arrays.asList(
            "NETHER_WASTES", "BASALT_DELTAS", "CRIMSON_FOREST",
            "SOUL_SAND_VALLEY", "WARPED_FOREST"
    );

    private final static List<String> endBiomeNames = Arrays.asList(
            "THE_END", "END_BARRENS", "END_HIGHLANDS", "END_MIDLANDS",
            "SMALL_END_ISLANDS"
    );

    private final static List<String> coldBiomeNames = Arrays.asList(
            "OLD_GROWTH_PINE_TAIGA", "OLD_GROWTH_SPRUCE_TAIGA", "STONY_SHORE",
            "TAIGA", "WINDSWEPT_FOREST", "WINDSWEPT_GRAVELLY_HILLS",
            "WINDSWEPT_HILLS"
    );

    private final static List<String> temperateBiomeNames = Arrays.asList(
            "PLAINS", "SUNFLOWER_PLAINS", "FOREST", "FLOWER_FOREST",
            "BIRCH_FOREST", "OLD_GROWTH_BIRCH_FOREST", "DARK_FOREST",
            "SWAMP", "MANGROVE_SWAMP", "JUNGLE", "SPARSE_JUNGLE",
            "BAMBOO_JUNGLE", "BEACH", "MUSHROOM_FIELDS", "MEADOW",
            "STONY_PEAKS", "CHERRY_GROVE"
    );

    private final static List<String> warmBiomeNames = Arrays.asList(
            "DESERT", "SAVANNA", "SAVANNA_PLATEAU", "WINDSWEPT_SAVANNA",
            "BADLANDS", "WOODED_BADLANDS", "ERODED_BADLANDS"
    );

    public static boolean isOverWorld(String biomeName) {
        return overWorldBiomeNames.contains(biomeName);
    }

    public static boolean isNether(String biomeName) {
        return netherBiomeNames.contains(biomeName);
    }

    public static boolean isEnd(String biomeName) {
        return endBiomeNames.contains(biomeName);
    }

    public static boolean isOcean(String biomeName) {
        return biomeName.contains("OCEAN");
    }

    public static boolean isCold(String biomeName) {
        return coldBiomeNames.contains(biomeName);
    }

    public static boolean isTemperate(String biomeName) {
        return temperateBiomeNames.contains(biomeName);
    }

    public static boolean isWarm(String biomeName) {
        return warmBiomeNames.contains(biomeName);
    }

    public static boolean isSnowy(String biomeName) {
        return biomeName.contains("SNOW") ||
                biomeName.equals("ICE_SPIKES") ||
                biomeName.equals("GROVE") ||
                biomeName.equals("JAGGED_PEAKS") ||
                biomeName.equals("FROZEN_PEAKS");
    }

    public static boolean isCold(Biome biome) {
        return coldBiomeNames.contains(getBiomeKey(biome));
    }

    public static boolean isTemperate(Biome biome) {
        return temperateBiomeNames.contains(getBiomeKey(biome));
    }

    public static boolean isWarm(Biome biome) {
        return warmBiomeNames.contains(getBiomeKey(biome));
    }

    public static boolean isAquatic(Biome biome) {
        return getBiomeKey(biome).contains("OCEAN") || getBiomeKey(biome).contains("RIVER");
    }

    public static boolean isCatchAll(Biome biome, List<String> allowedBiomes) {
        return isAllowedOverWorld(biome, allowedBiomes)
                || isAllowedNether(biome, allowedBiomes)
                || isAllowedEnd(biome, allowedBiomes)
                || isAllowedOcean(biome, allowedBiomes)
                || isAllowedSnowy(biome, allowedBiomes)
                || isAllowedCold(biome, allowedBiomes)
                || isAllowedTemperate(biome, allowedBiomes)
                || isAllowedWarm(biome, allowedBiomes)
                || isAllowedAquatic(biome, allowedBiomes);
    }

    private static boolean isAllowedOverWorld(Biome biome, List<String> allowedBiomes) {
        return allowedBiomes.contains("overworld") && isOverWorld(getBiomeKey(biome));
    }

    private static boolean isAllowedNether(Biome biome, List<String> allowedBiomes) {
        return allowedBiomes.contains("nether") && isNether(getBiomeKey(biome));
    }

    private static boolean isAllowedEnd(Biome biome, List<String> allowedBiomes) {
        return allowedBiomes.contains("end") && isEnd(getBiomeKey(biome));
    }

    private static boolean isAllowedOcean(Biome biome, List<String> allowedBiomes) {
        return allowedBiomes.contains("ocean") && isOcean(getBiomeKey(biome));
    }

    private static boolean isAllowedSnowy(Biome biome, List<String> allowedBiomes) {
        return allowedBiomes.contains("snowy") && isSnowy(getBiomeKey(biome));
    }

    private static boolean isAllowedCold(Biome biome, List<String> allowedBiomes) {
        return allowedBiomes.contains("cold") && isCold(getBiomeKey(biome));
    }

    private static boolean isAllowedTemperate(Biome biome, List<String> allowedBiomes) {
        return allowedBiomes.contains("temperate") && isTemperate(getBiomeKey(biome));
    }

    private static boolean isAllowedWarm(Biome biome, List<String> allowedBiomes) {
        return allowedBiomes.contains("warm") && isWarm(getBiomeKey(biome));
    }

    private static boolean isAllowedAquatic(Biome biome, List<String> allowedBiomes) {
        return allowedBiomes.contains("aquatic") && isAquatic(biome);
    }

    private static String getBiomeKey(Biome biome) {
        // Use biome.getKey().getKey() to get the biome's name
        NamespacedKey key = biome.getKey();
        return key != null ? key.getKey() : biome.name(); // Fallback to biome.name() if getKey() is null
    }
}
