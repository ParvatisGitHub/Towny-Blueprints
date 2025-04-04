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

    public ExistingBlueprintVisualizer(TownyBlueprints plugin) {
        this.plugin = plugin;
        this.spacing = plugin.getConfig().getDouble("particles.spacing", 0.5);
        this.frequency = plugin.getConfig().getInt("particles.frequency", 10);
        this.viewDistance = plugin.getConfig().getInt("particles.view_distance", 50);
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

                // Only show particles if player is within view distance
                if (base.distance(player.getLocation()) > viewDistance) return;

                if (blueprint.getBlueprint().isPlotBased()) {
                    visualizePlot(world, base, isDisabled);
                } else {
                    visualizeArea(world, base, blueprint, isDisabled);
                }
            }
        };

        task.runTaskTimer(plugin, 0L, frequency);
        visualizationTasks.computeIfAbsent(playerId, k -> new ArrayList<>()).add(task);
    }

    private void visualizeArea(World world, Location base, PlacedBlueprint blueprint, boolean isDisabled) {
        int sizeX = blueprint.getBlueprint().getSizeX();
        int sizeY = blueprint.getBlueprint().getSizeY();
        int sizeZ = blueprint.getBlueprint().getSizeZ();

        Particle particle = isDisabled ? Particle.DUST : Particle.COMPOSTER;
        Particle.DustOptions dustOptions = isDisabled ? 
            new Particle.DustOptions(Color.RED, 1) : null;

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

    private void visualizePlot(World world, Location base, boolean isDisabled) {
        TownBlock plot = com.palmergames.bukkit.towny.TownyAPI.getInstance().getTownBlock(base);
        if (plot == null) return;

        int baseX = plot.getX() * 16;
        int baseZ = plot.getZ() * 16;
        int height = 3;

        Particle particle = isDisabled ? Particle.DUST : Particle.COMPOSTER;
        Particle.DustOptions dustOptions = isDisabled ? 
            new Particle.DustOptions(Color.RED, 1) : null;

        // Draw vertical lines at corners
        for (double y = 0; y <= height; y += spacing) {
            spawnParticle(world, new Location(world, baseX, 64 + y, baseZ), particle, dustOptions);
            spawnParticle(world, new Location(world, baseX + 16, 64 + y, baseZ), particle, dustOptions);
            spawnParticle(world, new Location(world, baseX, 64 + y, baseZ + 16), particle, dustOptions);
            spawnParticle(world, new Location(world, baseX + 16, 64 + y, baseZ + 16), particle, dustOptions);
        }

        // Draw horizontal lines
        for (double x = 0; x <= 16; x += spacing) {
            spawnParticle(world, new Location(world, baseX + x, 64, baseZ), particle, dustOptions);
            spawnParticle(world, new Location(world, baseX + x, 64, baseZ + 16), particle, dustOptions);
            spawnParticle(world, new Location(world, baseX + x, 64 + height, baseZ), particle, dustOptions);
            spawnParticle(world, new Location(world, baseX + x, 64 + height, baseZ + 16), particle, dustOptions);
        }

        for (double z = 0; z <= 16; z += spacing) {
            spawnParticle(world, new Location(world, baseX, 64, baseZ + z), particle, dustOptions);
            spawnParticle(world, new Location(world, baseX + 16, 64, baseZ + z), particle, dustOptions);
            spawnParticle(world, new Location(world, baseX, 64 + height, baseZ + z), particle, dustOptions);
            spawnParticle(world, new Location(world, baseX + 16, 64 + height, baseZ + z), particle, dustOptions);
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
