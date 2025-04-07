package com.townyblueprints.managers;

import com.townyblueprints.TownyBlueprints;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.bukkit.Material;

public class ToolDefinitionManager {
    private final TownyBlueprints plugin;
    @Getter
    private final Map<String, List<Material>> toolDefinitions = new HashMap<>();

    public ToolDefinitionManager(TownyBlueprints plugin) {
        this.plugin = plugin;
        loadDefinitions();
    }

    public void loadDefinitions() {
        File file = new File(plugin.getDataFolder(), "tool_definitions.yml");
        if (!file.exists()) {
            createDefaultDefinitions(file);
            plugin.getLogger().info("Successfully Created Tool Definitions");
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection defsSection = config.getConfigurationSection("definitions");
        if (defsSection == null) return;
        
        for (String key : defsSection.getKeys(false)) {
            List<String> materialNames = defsSection.getStringList(key);
            List<Material> materials = new ArrayList<>();
            
            for (String materialName : materialNames) {
                try {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    materials.add(material);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in tool definitions: " + materialName);
                }
            }
            
            if (!materials.isEmpty()) {
                toolDefinitions.put(key.toLowerCase(), materials);
                // Debug logging
                if (TownyBlueprints.getInstance().getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Loaded tool definition for " + key + " with " + materials.size() + " tools");
                }
            }
        }
        plugin.getLogger().info("Successfully Loaded Tool Definitions");
    }
    
    private void createDefaultDefinitions(File file) {
        YamlConfiguration config = new YamlConfiguration();
        
        Map<String, List<String>> defaultDefinitions = new HashMap<>();
        
        // AXE definition
        defaultDefinitions.put("axe", Arrays.asList(
            "WOODEN_AXE",
            "STONE_AXE",
            "IRON_AXE",
            "GOLDEN_AXE",
            "DIAMOND_AXE",
            "NETHERITE_AXE"
        ));

        // PICKAXE definition
        defaultDefinitions.put("pickaxe", Arrays.asList(
            "WOODEN_PICKAXE",
            "STONE_PICKAXE",
            "IRON_PICKAXE",
            "GOLDEN_PICKAXE",
            "DIAMOND_PICKAXE",
            "NETHERITE_PICKAXE"
        ));

        // SHOVEL definition
        defaultDefinitions.put("shovel", Arrays.asList(
            "WOODEN_SHOVEL",
            "STONE_SHOVEL",
            "IRON_SHOVEL",
            "GOLDEN_SHOVEL",
            "DIAMOND_SHOVEL",
            "NETHERITE_SHOVEL"
        ));

        // HOE definition
        defaultDefinitions.put("hoe", Arrays.asList(
            "WOODEN_HOE",
            "STONE_HOE",
            "IRON_HOE",
            "GOLDEN_HOE",
            "DIAMOND_HOE",
            "NETHERITE_HOE"
        ));

        // SWORD definition
        defaultDefinitions.put("sword", Arrays.asList(
            "WOODEN_SWORD",
            "STONE_SWORD",
            "IRON_SWORD",
            "GOLDEN_SWORD",
            "DIAMOND_SWORD",
            "NETHERITE_SWORD"
        ));

        ConfigurationSection definitions = config.createSection("definitions");
        for (Map.Entry<String, List<String>> entry : defaultDefinitions.entrySet()) {
            definitions.set(entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create default tool definitions file!");
            e.printStackTrace();
        }
    }

    public boolean isToolDefinition(String name) {
        return toolDefinitions.containsKey(name.toLowerCase());
    }

    public List<Material> getDefinition(String name) {
        return toolDefinitions.getOrDefault(name.toLowerCase(), new ArrayList<>());
    }

    public boolean isValidTool(Material material, String definitionName) {
        List<Material> definition = getDefinition(definitionName);
        return definition.contains(material);
    }
}