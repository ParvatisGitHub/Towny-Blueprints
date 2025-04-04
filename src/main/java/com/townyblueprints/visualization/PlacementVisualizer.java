package com.townyblueprints.visualization;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.Blueprint;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PlacementVisualizer {
    private final TownyBlueprints plugin;
    private final Map<UUID, BukkitRunnable> visualizationTasks = new HashMap<>();
    private final Map<UUID, Location> currentLocations = new HashMap<>();
    private final Map<UUID, Set<TownBlock>> selectedPlots = new HashMap<>();
    private final double spacing;
    private final int frequency;

    public PlacementVisualizer(TownyBlueprints plugin) {
        this.plugin = plugin;
        this.spacing = plugin.getConfig().getDouble("particles.spacing", 0.5);
        this.frequency = plugin.getConfig().getInt("particles.frequency", 10);
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

                if (blueprint.isPlotBased()) {
                    visualizePlots(player);
                } else {
                    visualizeArea(player, blueprint);
                }
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
            world.spawnParticle(Particle.COMPOSTER, base.clone().add(0, y, 0), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.COMPOSTER, base.clone().add(sizeX, y, 0), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.COMPOSTER, base.clone().add(0, y, sizeZ), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.COMPOSTER, base.clone().add(sizeX, y, sizeZ), 1, 0, 0, 0, 0);
        }

        // Draw horizontal lines at top and bottom
        for (double x = 0; x <= sizeX; x += spacing) {
            // Bottom edges
            world.spawnParticle(Particle.COMPOSTER, base.clone().add(x, 0, 0), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.COMPOSTER, base.clone().add(x, 0, sizeZ), 1, 0, 0, 0, 0);
            // Top edges
            world.spawnParticle(Particle.COMPOSTER, base.clone().add(x, sizeY, 0), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.COMPOSTER, base.clone().add(x, sizeY, sizeZ), 1, 0, 0, 0, 0);
        }

        for (double z = 0; z <= sizeZ; z += spacing) {
            // Bottom edges
            world.spawnParticle(Particle.COMPOSTER, base.clone().add(0, 0, z), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.COMPOSTER, base.clone().add(sizeX, 0, z), 1, 0, 0, 0, 0);
            // Top edges
            world.spawnParticle(Particle.COMPOSTER, base.clone().add(0, sizeY, z), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.COMPOSTER, base.clone().add(sizeX, sizeY, z), 1, 0, 0, 0, 0);
        }
    }

    private void visualizePlots(Player player) {
        Set<TownBlock> plots = selectedPlots.get(player.getUniqueId());
        if (plots == null || plots.isEmpty()) return;

        for (TownBlock plot : plots) {
            World world = plot.getWorld().getBukkitWorld();
            if (world == null) continue;

            int baseX = plot.getX() * 16;
            int baseZ = plot.getZ() * 16;
            int height = 3; // Height of the plot visualization

            // Draw vertical lines at corners
            for (double y = 0; y <= height; y += spacing) {
                world.spawnParticle(Particle.COMPOSTER, new Location(world, baseX, 64 + y, baseZ), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.COMPOSTER, new Location(world, baseX + 16, 64 + y, baseZ), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.COMPOSTER, new Location(world, baseX, 64 + y, baseZ + 16), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.COMPOSTER, new Location(world, baseX + 16, 64 + y, baseZ + 16), 1, 0, 0, 0, 0);
            }

            // Draw horizontal lines
            for (double x = 0; x <= 16; x += spacing) {
                world.spawnParticle(Particle.COMPOSTER, new Location(world, baseX + x, 64, baseZ), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.COMPOSTER, new Location(world, baseX + x, 64, baseZ + 16), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.COMPOSTER, new Location(world, baseX + x, 64 + height, baseZ), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.COMPOSTER, new Location(world, baseX + x, 64 + height, baseZ + 16), 1, 0, 0, 0, 0);
            }

            for (double z = 0; z <= 16; z += spacing) {
                world.spawnParticle(Particle.COMPOSTER, new Location(world, baseX, 64, baseZ + z), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.COMPOSTER, new Location(world, baseX + 16, 64, baseZ + z), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.COMPOSTER, new Location(world, baseX, 64 + height, baseZ + z), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.COMPOSTER, new Location(world, baseX + 16, 64 + height, baseZ + z), 1, 0, 0, 0, 0);
            }
        }
    }

    public void updatePlotSelection(Player player, Set<TownBlock> plots) {
        selectedPlots.put(player.getUniqueId(), new HashSet<>(plots));
    }

    public void updatePlotHighlight(Player player, TownBlock townBlock) {
        Set<TownBlock> singlePlot = new HashSet<>();
        singlePlot.add(townBlock);
        selectedPlots.put(player.getUniqueId(), singlePlot);
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
