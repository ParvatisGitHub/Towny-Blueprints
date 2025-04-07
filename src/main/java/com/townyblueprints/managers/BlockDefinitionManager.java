package com.townyblueprints.managers;

import com.townyblueprints.TownyBlueprints;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import org.bukkit.Material;
import java.util.Map;
import java.util.HashMap; 

public class BlockDefinitionManager {
    private final TownyBlueprints plugin;
    @Getter
    private final Map<String, List<Material>> blockDefinitions = new HashMap<>();

    public BlockDefinitionManager(TownyBlueprints plugin) {
        this.plugin = plugin;
        loadDefinitions();
    }

    public void loadDefinitions() {
        File file = new File(plugin.getDataFolder(), "block_definitions.yml");
        if (!file.exists()) {
            createDefaultDefinitions(file);
            plugin.getLogger().info("Successfully Created Block Definitions");
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection definitions = config.getConfigurationSection("definitions");
        if (definitions == null) return;

        for (String key : definitions.getKeys(false)) {
            List<String> materialNames = definitions.getStringList(key);
            List<Material> materials = new ArrayList<>();
            
            for (String materialName : materialNames) {
                try {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    materials.add(material);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in block definitions: " + materialName);
                }
            }
            
            if (!materials.isEmpty()) {
                blockDefinitions.put(key.toLowerCase(), materials);
            }
        }
        plugin.getLogger().info("Successfully Loaded Block Definitions");
    }

    private void createDefaultDefinitions(File file) {
        YamlConfiguration config = new YamlConfiguration();
        
        Map<String, List<String>> defaultDefinitions = new HashMap<>();
        
        // Planks definition
        defaultDefinitions.put("planks", Arrays.asList(
            "OAK_PLANKS",
            "SPRUCE_PLANKS",
            "BIRCH_PLANKS",
            "JUNGLE_PLANKS",
            "ACACIA_PLANKS",
            "DARK_OAK_PLANKS",
            "CRIMSON_PLANKS",
            "WARPED_PLANKS"
        ));

        // Logs definition
        defaultDefinitions.put("logs", Arrays.asList(
            "OAK_LOG",
            "SPRUCE_LOG",
            "BIRCH_LOG",
            "JUNGLE_LOG",
            "ACACIA_LOG",
            "DARK_OAK_LOG",
            "CRIMSON_STEM",
            "WARPED_STEM"
        ));

        // Glass definition
        defaultDefinitions.put("glass", Arrays.asList(
            "GLASS",
            "WHITE_STAINED_GLASS",
            "ORANGE_STAINED_GLASS",
            "MAGENTA_STAINED_GLASS",
            "LIGHT_BLUE_STAINED_GLASS",
            "YELLOW_STAINED_GLASS",
            "LIME_STAINED_GLASS",
            "PINK_STAINED_GLASS",
            "GRAY_STAINED_GLASS",
            "LIGHT_GRAY_STAINED_GLASS",
            "CYAN_STAINED_GLASS",
            "PURPLE_STAINED_GLASS",
            "BLUE_STAINED_GLASS",
            "BROWN_STAINED_GLASS",
            "GREEN_STAINED_GLASS",
            "RED_STAINED_GLASS",
            "BLACK_STAINED_GLASS"
        ));

        // Wood Slabs definition
        defaultDefinitions.put("wooden_slabs", Arrays.asList(
            "OAK_SLAB",
            "SPRUCE_SLAB",
            "BIRCH_SLAB",
            "JUNGLE_SLAB",
            "ACACIA_SLAB",
            "DARK_OAK_SLAB",
            "CRIMSON_SLAB",
            "WARPED_SLAB"
        ));

        // Wood Stairs definition
        defaultDefinitions.put("wooden_stairs", Arrays.asList(
            "OAK_STAIRS",
            "SPRUCE_STAIRS",
            "BIRCH_STAIRS",
            "JUNGLE_STAIRS",
            "ACACIA_STAIRS",
            "DARK_OAK_STAIRS",
            "CRIMSON_STAIRS",
            "WARPED_STAIRS"
        ));

        ConfigurationSection definitions = config.createSection("definitions");
        for (Map.Entry<String, List<String>> entry : defaultDefinitions.entrySet()) {
            definitions.set(entry.getKey(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create default block definitions file!");
            e.printStackTrace();
        }
    }

    public boolean isBlockDefinition(String name) {
        return blockDefinitions.containsKey(name.toLowerCase());
    }

    public List<Material> getDefinition(String name) {
        return blockDefinitions.getOrDefault(name.toLowerCase(), new ArrayList<>());
    }

    public boolean isValidBlock(Material material, String definitionName) {
        List<Material> definition = getDefinition(definitionName);
        return definition.contains(material);
    }
}
