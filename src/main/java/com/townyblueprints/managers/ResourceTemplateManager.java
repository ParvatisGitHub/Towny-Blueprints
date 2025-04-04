package com.townyblueprints.managers;

import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.ResourceTemplate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
public class ResourceTemplateManager {
    private final TownyBlueprints plugin;
    
    @Getter
    private final Map<String, ResourceTemplate> templates = new HashMap<>();
    
    public void loadTemplates() {
        File templateDir = new File(plugin.getDataFolder(), "resource_templates");
        if (!templateDir.exists()) {
            templateDir.mkdirs();
            // Copy default templates
            plugin.saveResource("resource_templates/iron_farm.yml", false);
            plugin.saveResource("resource_templates/random_mine.yml", false);
        }
        
        File[] files = templateDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        
        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ResourceTemplate template = ResourceTemplate.fromConfig(config);
            String templateName = file.getName().toLowerCase().replace(".yml", "");
            templates.put(templateName, template);
            plugin.getLogger().info("Loaded resource template: " + template.getName() + " (key: " + templateName + ")");
        }
    }
    
    public ResourceTemplate getTemplate(String name) {
        // Remove the "template:" prefix if it exists
        String cleanName = name.toLowerCase().replace("template:", "").trim();
        plugin.getLogger().info("[ResourceTemplateManager] Looking for template: " + cleanName);
        plugin.getLogger().info("[ResourceTemplateManager] Available templates: " + String.join(", ", templates.keySet()));

        // First try exact match
        ResourceTemplate template = templates.get(cleanName);
        if (template != null) {
            plugin.getLogger().info("[ResourceTemplateManager] Found template using exact match: " + cleanName);
            return template;
        }

        // Then try with .yml
        template = templates.get(cleanName + ".yml");
        if (template != null) {
            plugin.getLogger().info("[ResourceTemplateManager] Found template with .yml extension: " + cleanName);
            return template;
        }

        // Finally try without .yml
        template = templates.get(cleanName.replace(".yml", ""));
        if (template != null) {
            plugin.getLogger().info("[ResourceTemplateManager] Found template without .yml extension: " + cleanName);
            return template;
        }

        plugin.getLogger().warning("[ResourceTemplateManager] Template not found: " + cleanName);
        return null;
    }
    
    public Collection<ResourceTemplate> getAllTemplates() {
        return templates.values();
    }
    
    public List<String> getResourcesDisplay(String templateName) {
        ResourceTemplate template = getTemplate(templateName);
        if (template == null) {
            plugin.getLogger().warning("Template not found: " + templateName);
            plugin.getLogger().warning("Available templates: " + String.join(", ", templates.keySet()));
            return Collections.singletonList("§7No resources defined");
        }

        List<String> display = new ArrayList<>();

        if (template.isRandomSelection()) {
            for (ResourceTemplate.ResourceEntry entry : template.getResources()) {
                // Check if the resource is of type "TOOL" and adjust the display accordingly
                if (entry.getType().equalsIgnoreCase("TOOL")) {
                    display.add(String.format("§7- §f%s §7(Weight: %d, Tool Type: %s)", 
                            formatResourceName(entry.getType()), 
                            entry.getWeight(),
                            entry.getToolType()));  // Assuming you have getToolType() method
                } else {
                    display.add(String.format("§7- §f%d-%d %s §7(Weight: %d)", 
                            entry.getMinAmount(),
                            entry.getMaxAmount(),
                            formatResourceName(entry.getType()),
                            entry.getWeight()));
                }
            }
        } else {
            for (ResourceTemplate.ResourceEntry entry : template.getResources()) {
                if (entry.getType().equalsIgnoreCase("TOOL")) {
                    display.add(String.format("§7- §f%s §7(Tool Type: %s)", 
                            formatResourceName(entry.getType()),
                            entry.getToolType()));  // Display the tool type here
                } else {
                    display.add(String.format("§7- §f%d %s", 
                            entry.getMinAmount(),
                            formatResourceName(entry.getType())));
                }
            }
        }

        return display;
    }
    
    private String formatResourceName(String resource) {
        return resource.toLowerCase().replace("_", " ");
    }
    
    public void saveTemplate(ResourceTemplate template) {
        File templateDir = new File(plugin.getDataFolder(), "resource_templates");
        if (!templateDir.exists()) {
            templateDir.mkdirs();
        }
        
        File file = new File(templateDir, template.getName().toLowerCase().replace(" ", "_") + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        
        config.set("name", template.getName());
        config.set("description", template.getDescription());
        config.set("random_selection", template.isRandomSelection());
        
        for (int i = 0; i < template.getResources().size(); i++) {
            ResourceTemplate.ResourceEntry entry = template.getResources().get(i);
            String path = "resources.resource" + (i + 1);
            config.set(path + ".type", entry.getType());
            config.set(path + ".min_amount", entry.getMinAmount());
            config.set(path + ".max_amount", entry.getMaxAmount());
            config.set(path + ".weight", entry.getWeight());
        }
        
        try {
            config.save(file);
            String templateName = file.getName().toLowerCase().replace(".yml", "");
            templates.put(templateName, template);
            plugin.getLogger().info("Saved resource template: " + template.getName() + " (key: " + templateName + ")");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save resource template: " + template.getName());
            e.printStackTrace();
        }
    }
}