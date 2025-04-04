package com.townyblueprints.models;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Data
public class ResourceTemplate {
    private String name;
    private String description;
    private List<ResourceEntry> resources;
    private boolean randomSelection;

    @Data
    public static class ResourceEntry {
        private String type; // MONEY, TOOL, or material name
        private int minAmount;
        private int maxAmount;
        private double weight;

        // Tool-specific fields
        private String toolType; // For TOOL type only
        private int durabilityDrain; // For TOOL type only

        public int getRandomAmount() {
            if (type.equals("TOOL")) {
                return durabilityDrain;
            }

            // If min and max are the same, return that amount
            if (minAmount == maxAmount) {
                return minAmount;
            }

            // Ensure max is greater than min to avoid IllegalArgumentException
            if (maxAmount <= minAmount) {
                return minAmount;
            }

            // Calculate the range for nextInt (must be positive)
            int range = maxAmount - minAmount + 1;
            return new Random().nextInt(range) + minAmount;
        }
    }

    public ResourceTemplate(String name) {
        this.name = name;
        this.resources = new ArrayList<>();
    }

    public static ResourceTemplate fromConfig(ConfigurationSection config) {
        ResourceTemplate template = new ResourceTemplate(config.getString("name"));
        template.setDescription(config.getString("description", ""));
        template.setRandomSelection(config.getBoolean("random_selection", false));

        ConfigurationSection resourcesSection = config.getConfigurationSection("resources");
        if (resourcesSection != null) {
            for (String key : resourcesSection.getKeys(false)) {
                ConfigurationSection resourceSection = resourcesSection.getConfigurationSection(key);
                if (resourceSection != null) {
                    ResourceEntry entry = new ResourceEntry();
                    String type = resourceSection.getString("type", "MONEY");

                    // Handle different resource types
                    if (type.equals("TOOL")) {
                        entry.setType("TOOL");
                        entry.setToolType(resourceSection.getString("tool_type"));
                        entry.setDurabilityDrain(resourceSection.getInt("durability_drain", 1));
                    } else if (type.equals("MONEY")) {
                        entry.setType("MONEY");
                    } else {
                        // Try to parse as Material
                        try {
                            Material.valueOf(type);
                            entry.setType("vanilla:" + type); // Prefix with vanilla: to indicate it's a material
                        } catch (IllegalArgumentException e) {
                            // If not a valid material, use as is (might be a custom type)
                            entry.setType(type);
                        }
                    }

                    entry.setWeight(resourceSection.getDouble("weight", 1.0));

                    if (!entry.getType().equals("TOOL")) {
                        // Ensure minAmount is always positive
                        int minAmount = Math.max(1, resourceSection.getInt("min_amount", 1));
                        entry.setMinAmount(minAmount);

                        // Ensure maxAmount is at least equal to minAmount
                        int maxAmount = Math.max(minAmount, resourceSection.getInt("max_amount", minAmount));
                        entry.setMaxAmount(maxAmount);
                    }

                    template.getResources().add(entry);
                }
            }
        }

        return template;
    }

    public List<ResourceEntry> getSelectedResources() {
        if (!randomSelection || resources.isEmpty()) {
            return resources;
        }

        // If random selection is enabled, pick one resource based on weights
        List<ResourceEntry> selected = new ArrayList<>();
        double totalWeight = resources.stream().mapToDouble(ResourceEntry::getWeight).sum();
        double random = new Random().nextDouble() * totalWeight;

        double currentWeight = 0;
        for (ResourceEntry entry : resources) {
            currentWeight += entry.getWeight();
            if (random <= currentWeight) {
                selected.add(entry);
                break;
            }
        }

        return selected;
    }
}