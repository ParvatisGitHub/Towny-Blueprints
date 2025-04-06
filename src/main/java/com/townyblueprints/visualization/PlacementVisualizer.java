package com.townyblueprints.visualization;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.Blueprint;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Color;

import java.util.*;

public class PlacementVisualizer {
    private final TownyBlueprints plugin;
    private final Map<UUID, BukkitRunnable> visualizationTasks = new HashMap<>();
    private final Map<UUID, Location> currentLocations = new HashMap<>();
    private final Map<UUID, Set<TownBlock>> selectedPlots = new HashMap<>();
    private final double spacing;
    private final int frequency;
    private final Particle placementParticle;
    private final Particle plotParticle;

    public PlacementVisualizer(TownyBlueprints plugin) {
        this.plugin = plugin;
        this.spacing = plugin.getConfig().getDouble("particles.spacing", 0.5);
        this.frequency = plugin.getConfig().getInt("particles.frequency", 10);

        // Initialize particles with default values first
        Particle tempPlacementParticle = Particle.COMPOSTER;
        Particle tempPlotParticle = Particle.END_ROD;

        // Get particle types from config
        String placementParticleType = plugin.getConfig().getString("particles.placement_type", "COMPOSTER");
        String plotParticleType = plugin.getConfig().getString("particles.plot_type", "END_ROD");

        // Try to set placement particle
        try {
            tempPlacementParticle = Particle.valueOf(placementParticleType.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid placement particle type in config: " + placementParticleType + ". Using COMPOSTER.");
        }

        // Try to set plot particle
        try {
            tempPlotParticle = Particle.valueOf(plotParticleType.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid plot particle type in config: " + plotParticleType + ". Using END_ROD.");
        }

        // Assign the final values
        this.placementParticle = tempPlacementParticle;
        this.plotParticle = tempPlotParticle;
    }

    public void startVisualization(Player player, Blueprint blueprint) {
        UUID playerId = player.getUniqueId();
        stopVisualization(player);
        currentLocations.put(playerId, player.getLocation());

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                    visualizeArea(player, blueprint);
            }
        };

        task.runTaskTimer(plugin, 0L, frequency);
        visualizationTasks.put(playerId, task);
    }

    private void visualizeArea(Player player, Blueprint blueprint) {
        Location base = currentLocations.get(player.getUniqueId());
        if (base == null) return;

        World world = base.getWorld();
        if (world == null) return;

        int sizeX = blueprint.getSizeX();
        int sizeY = blueprint.getSizeY();
        int sizeZ = blueprint.getSizeZ();

        // Draw vertical lines at corners
        for (double y = 0; y <= sizeY; y += spacing) {
            world.spawnParticle(placementParticle, base.clone().add(0, y, 0), 1, 0, 0, 0, 0);
            world.spawnParticle(placementParticle, base.clone().add(sizeX, y, 0), 1, 0, 0, 0, 0);
            world.spawnParticle(placementParticle, base.clone().add(0, y, sizeZ), 1, 0, 0, 0, 0);
            world.spawnParticle(placementParticle, base.clone().add(sizeX, y, sizeZ), 1, 0, 0, 0, 0);
        }

        // Draw horizontal lines at top and bottom
        for (double x = 0; x <= sizeX; x += spacing) {
            world.spawnParticle(placementParticle, base.clone().add(x, 0, 0), 1, 0, 0, 0, 0);
            world.spawnParticle(placementParticle, base.clone().add(x, 0, sizeZ), 1, 0, 0, 0, 0);
            world.spawnParticle(placementParticle, base.clone().add(x, sizeY, 0), 1, 0, 0, 0, 0);
            world.spawnParticle(placementParticle, base.clone().add(x, sizeY, sizeZ), 1, 0, 0, 0, 0);
        }

        for (double z = 0; z <= sizeZ; z += spacing) {
            world.spawnParticle(placementParticle, base.clone().add(0, 0, z), 1, 0, 0, 0, 0);
            world.spawnParticle(placementParticle, base.clone().add(sizeX, 0, z), 1, 0, 0, 0, 0);
            world.spawnParticle(placementParticle, base.clone().add(0, sizeY, z), 1, 0, 0, 0, 0);
            world.spawnParticle(placementParticle, base.clone().add(sizeX, sizeY, z), 1, 0, 0, 0, 0);
        }
    }

    public void updateLocation(Player player, Location location) {
        currentLocations.put(player.getUniqueId(), location);
    }

    public void stopVisualization(Player player) {
        UUID playerId = player.getUniqueId();
        currentLocations.remove(playerId);
        selectedPlots.remove(playerId);

        BukkitRunnable task = visualizationTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
}