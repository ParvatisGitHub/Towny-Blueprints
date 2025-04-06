package com.townyblueprints.visualization;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.PlacedBlueprint;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class ExistingBlueprintVisualizer {
    private final TownyBlueprints plugin;
    private final Map<UUID, List<BukkitRunnable>> visualizationTasks = new HashMap<>();
    private final Map<UUID, Boolean> activeVisualizations = new HashMap<>();
    private final double spacing;
    private final int frequency;
    private final int viewDistance;
    private final Particle activeParticle;
    private final Particle inactiveParticle;
    private final Color inactiveColor;
    private final float inactiveSize;

    public ExistingBlueprintVisualizer(TownyBlueprints plugin) {
        this.plugin = plugin;
        this.spacing = plugin.getConfig().getDouble("particles.spacing", 0.5);
        this.frequency = plugin.getConfig().getInt("particles.frequency", 10);
        this.viewDistance = plugin.getConfig().getInt("particles.view_distance", 50);

        // Initialize particles with default values first
        Particle tempActiveParticle = Particle.COMPOSTER;
        Particle tempInactiveParticle = Particle.DUST;

        // Get particle types from config
        String activeParticleType = plugin.getConfig().getString("particles.active_type", "COMPOSTER");
        String inactiveParticleType = plugin.getConfig().getString("particles.inactive_type", "DUST");

        // Get dust particle color from config
        int red = plugin.getConfig().getInt("particles.inactive_color.red", 255);
        int green = plugin.getConfig().getInt("particles.inactive_color.green", 0);
        int blue = plugin.getConfig().getInt("particles.inactive_color.blue", 0);
        this.inactiveColor = Color.fromRGB(red, green, blue);
        this.inactiveSize = (float) plugin.getConfig().getDouble("particles.inactive_size", 1.0);

        // Try to set active particle
        try {
            tempActiveParticle = Particle.valueOf(activeParticleType.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid active particle type in config: " + activeParticleType + ". Using COMPOSTER.");
        }

        // Try to set inactive particle
        try {
            tempInactiveParticle = Particle.valueOf(inactiveParticleType.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid inactive particle type in config: " + inactiveParticleType + ". Using DUST.");
        }

        // Assign the final values
        this.activeParticle = tempActiveParticle;
        this.inactiveParticle = tempInactiveParticle;
    }

    public void startVisualization(Player player, PlacedBlueprint blueprint, boolean isDisabled) {
        UUID playerId = player.getUniqueId();
        activeVisualizations.put(playerId, true);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !activeVisualizations.getOrDefault(playerId, false)) {
                    cancel();
                    return;
                }

                Location base = blueprint.getLocation();
                World world = base.getWorld();
                if (world == null || !world.equals(player.getWorld())) return;

                if (base.distance(player.getLocation()) > viewDistance) return;
                    visualizeArea(world, base, blueprint, isDisabled);
            }
        };

        task.runTaskTimer(plugin, 0L, frequency);
        visualizationTasks.computeIfAbsent(playerId, k -> new ArrayList<>()).add(task);
    }

    private void visualizeArea(World world, Location base, PlacedBlueprint blueprint, boolean isDisabled) {
        int sizeX = blueprint.getBlueprint().getSizeX();
        int sizeY = blueprint.getBlueprint().getSizeY();
        int sizeZ = blueprint.getBlueprint().getSizeZ();

        Particle particle = isDisabled ? inactiveParticle : activeParticle;
        Particle.DustOptions dustOptions = isDisabled && particle == Particle.DUST ?
                new Particle.DustOptions(inactiveColor, inactiveSize) : null;

        // Draw vertical lines at corners
        for (double y = 0; y <= sizeY; y += spacing) {
            spawnParticle(world, base.clone().add(0, y, 0), particle, dustOptions);
            spawnParticle(world, base.clone().add(sizeX, y, 0), particle, dustOptions);
            spawnParticle(world, base.clone().add(0, y, sizeZ), particle, dustOptions);
            spawnParticle(world, base.clone().add(sizeX, y, sizeZ), particle, dustOptions);
        }

        // Draw horizontal lines at top and bottom
        for (double x = 0; x <= sizeX; x += spacing) {
            spawnParticle(world, base.clone().add(x, 0, 0), particle, dustOptions);
            spawnParticle(world, base.clone().add(x, 0, sizeZ), particle, dustOptions);
            spawnParticle(world, base.clone().add(x, sizeY, 0), particle, dustOptions);
            spawnParticle(world, base.clone().add(x, sizeY, sizeZ), particle, dustOptions);
        }

        for (double z = 0; z <= sizeZ; z += spacing) {
            spawnParticle(world, base.clone().add(0, 0, z), particle, dustOptions);
            spawnParticle(world, base.clone().add(sizeX, 0, z), particle, dustOptions);
            spawnParticle(world, base.clone().add(0, sizeY, z), particle, dustOptions);
            spawnParticle(world, base.clone().add(sizeX, sizeY, z), particle, dustOptions);
        }
    }

    public void stopVisualization(Player player) {
        UUID playerId = player.getUniqueId();
        activeVisualizations.remove(playerId);

        List<BukkitRunnable> tasks = visualizationTasks.remove(playerId);
        if (tasks != null) {
            tasks.forEach(BukkitRunnable::cancel);
        }
    }

    private void spawnParticle(World world, Location location, Particle particle, Particle.DustOptions dustOptions) {
        if (particle == Particle.DUST && dustOptions != null) {
            world.spawnParticle(particle, location, 1, 0, 0, 0, 0, dustOptions);
        } else {
            world.spawnParticle(particle, location, 1, 0, 0, 0, 0);
        }
    }
}