package com.townyblueprints.inventory;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class InventoryScanner {
    private final JavaPlugin plugin;
    private final Logger logger;

    public InventoryScanner(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public List<Container> scanForContainers(Location baseLocation, int sizeX, int sizeY, int sizeZ) {
        List<Container> containers = new ArrayList<>();

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    Location loc = baseLocation.clone().add(x, y, z);
                    Block block = loc.getBlock();
                    if (block.getState() instanceof Container) {
                        containers.add((Container) block.getState());
                        logger.info("[InventoryScanner] Found container at " + loc);
                    }
                }
            }
        }

        logger.info("[InventoryScanner] Found " + containers.size() + " containers at " + baseLocation);
        return containers;
    }
}