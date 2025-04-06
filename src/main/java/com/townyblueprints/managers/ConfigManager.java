package com.townyblueprints.managers;

import com.palmergames.bukkit.towny.object.Town;
import com.townyblueprints.TownyBlueprints;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class ConfigManager {
    private final TownyBlueprints plugin;
    private FileConfiguration config;
    private File blueprintsFile;
    private FileConfiguration blueprintsConfig;
    private final Map<Integer, Integer> maxBlueprintsPerLevel = new HashMap<>();
    private boolean debugMode;

    public void loadConfig() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        this.debugMode = config.getBoolean("debug.debugMode", false);

        // Load max blueprints per level
        maxBlueprintsPerLevel.clear();
        if (config.contains("max_blueprints_per_level")) {
            for (String key : config.getConfigurationSection("max_blueprints_per_level").getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    int max = config.getInt("max_blueprints_per_level." + key);
                    maxBlueprintsPerLevel.put(level, max);
                } catch (NumberFormatException ignored) {}
            }
        }
        
        // Initialize blueprints.yml
        this.blueprintsFile = new File(plugin.getDataFolder(), "blueprints.yml");
        if (!blueprintsFile.exists()) {
            plugin.saveResource("blueprints.yml", false);
        }
        this.blueprintsConfig = YamlConfiguration.loadConfiguration(blueprintsFile);
    }
    public boolean isDebugMode() {
        return debugMode;
    }

    public void saveBlueprints() {
        try {
            blueprintsConfig.save(blueprintsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save blueprints.yml!");
            e.printStackTrace();
        }
    }

    public int getTownLevel(Town town) {
        // This is a placeholder - implement your own town level logic
        // You might want to integrate with other plugins that handle town levels
        return town.getNumResidents() / 5; // Example: 1 level per 5 residents
    }

    public int getMaxBlueprintsForLevel(int level) {
        return maxBlueprintsPerLevel.getOrDefault(level, -1);
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public FileConfiguration getBlueprintsConfig() {
        return this.blueprintsConfig;
    }

    public int getTypeLimitForLevel(String type, int level) {
        String path = "blueprints.type_limits." + type.toLowerCase() + "." + level;
        return config.getInt(path, -1); // Return -1 if no limit is set
    }

    public Map<String, Map<Integer, Integer>> getTypeLimits() {
        Map<String, Map<Integer, Integer>> limits = new HashMap<>();
        ConfigurationSection typeLimits = config.getConfigurationSection("blueprints.type_limits");
        
        if (typeLimits != null) {
            for (String type : typeLimits.getKeys(false)) {
                Map<Integer, Integer> levelLimits = new HashMap<>();
                ConfigurationSection typeSection = typeLimits.getConfigurationSection(type);
                
                if (typeSection != null) {
                    for (String levelStr : typeSection.getKeys(false)) {
                        try {
                            int level = Integer.parseInt(levelStr);
                            int limit = typeSection.getInt(levelStr);
                            levelLimits.put(level, limit);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                
                limits.put(type, levelLimits);
            }
        }
        
        return limits;
    }
}