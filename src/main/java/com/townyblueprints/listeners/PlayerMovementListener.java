package com.townyblueprints.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.Blueprint;
import com.townyblueprints.models.PlacedBlueprint;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class PlayerMovementListener implements Listener {
    private final TownyBlueprints plugin;
    private final Map<UUID, String> playerInBlueprint = new HashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location location = event.getTo();

        // Update visualization location if player is placing a blueprint
        Blueprint placingBlueprint = plugin.getPlacementHandler().getPlayerPlacements().get(player.getUniqueId());
        if (placingBlueprint != null) {
            Location adjustedLocation = location.clone();
            if (plugin.getPlacementHandler().isCenterMode(player)) {
                adjustedLocation.add(
                    -placingBlueprint.getSizeX() / 2,
                    0,
                    -placingBlueprint.getSizeZ() / 2
                );
            }
            plugin.getPlacementHandler().updateVisualizationLocation(player, adjustedLocation);
            return;
        }

        // Only check if player has moved to a new block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Town town = TownyAPI.getInstance().getTown(location);

        // If player has left a town, stop visualization
        if (town == null) {
            plugin.getPlacementHandler().stopVisualization(player);
            playerInBlueprint.remove(player.getUniqueId());
            return;
        }

        // Check if player is in a blueprint
        PlacedBlueprint blueprint = findBlueprintAtLocation(location);
        String currentBlueprintId = playerInBlueprint.get(player.getUniqueId());

        if (blueprint != null) {
            // Only send message if player just entered this blueprint
            if (currentBlueprintId == null || !currentBlueprintId.equals(blueprint.getId())) {
                playerInBlueprint.put(player.getUniqueId(), blueprint.getId());
                player.sendActionBar(Component.text()
                    .append(Component.text("You are in a ", NamedTextColor.GRAY))
                    .append(Component.text(blueprint.getBlueprint().getName(), NamedTextColor.GOLD))
                    .append(Component.text(" blueprint", NamedTextColor.GRAY))
                    .build());
            }
        } else {
            // Player has left a blueprint
            playerInBlueprint.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPlacementHandler().stopVisualization(player);
        playerInBlueprint.remove(player.getUniqueId());
    }

    private PlacedBlueprint findBlueprintAtLocation(Location location) {
        for (PlacedBlueprint blueprint : plugin.getBlueprintManager().getAllPlacedBlueprints()) {
            Location bpLoc = blueprint.getLocation();
            if (bpLoc.getWorld().equals(location.getWorld())) {
                if (location.getX() >= bpLoc.getX() && 
                    location.getX() < bpLoc.getX() + blueprint.getBlueprint().getSizeX() &&
                    location.getY() >= bpLoc.getY() && 
                    location.getY() < bpLoc.getY() + blueprint.getBlueprint().getSizeY() &&
                    location.getZ() >= bpLoc.getZ() && 
                    location.getZ() < bpLoc.getZ() + blueprint.getBlueprint().getSizeZ()) {
                    return blueprint;
                }
            }
        }
        return null;
    }
}