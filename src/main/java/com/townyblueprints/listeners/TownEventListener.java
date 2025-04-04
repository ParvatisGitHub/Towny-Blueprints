package com.townyblueprints.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.TownClaimEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.PlacedBlueprint;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public class TownEventListener implements Listener {
    private final TownyBlueprints plugin;

    @EventHandler
    public void onTownDelete(DeleteTownEvent event) {
        Town town = TownyAPI.getInstance().getTown(event.getTownName());
        if (town == null) return;

        // Remove all blueprints for the deleted town
        for (PlacedBlueprint blueprint : plugin.getBlueprintManager().getPlacedBlueprintsForTown(town)) {
            blueprint.setActive(false);
        }
    }

    @EventHandler
    public void onTownClaim(TownClaimEvent event) {
        Town town = event.getTown();
        int bonusBlocks = plugin.getBlueprintManager().calculateTownBonusBlocks(town);
        
        // If the town is trying to claim more blocks than they should be able to
        if (town.getTownBlocks().size() >= town.getMaxTownBlocks() + bonusBlocks) {
            // We can't cancel the event, but we can notify the player
            if (event.getResident().getPlayer() != null) {
                event.getResident().getPlayer().sendMessage(
                    "§cYou cannot claim more town blocks than your town's maximum (including bonuses).");
            }
            return;
        }

        // Check if any blueprints in the area need to be reactivated
        for (PlacedBlueprint blueprint : plugin.getBlueprintManager().getAllPlacedBlueprints()) {
            if (blueprint.getLocation().getWorld().equals(event.getTownBlock().getWorld()) &&
                event.getTownBlock().getX() == blueprint.getLocation().getBlockX() >> 4 &&
                event.getTownBlock().getZ() == blueprint.getLocation().getBlockZ() >> 4) {
                blueprint.setActive(true);
            }
        }
    }

    @EventHandler
    public void onTownUnclaim(TownPreUnclaimEvent event) {
        // Deactivate any blueprints in the unclaimed area
        for (PlacedBlueprint blueprint : plugin.getBlueprintManager().getAllPlacedBlueprints()) {
            if (blueprint.getLocation().getWorld().equals(event.getTownBlock().getWorld()) &&
                event.getTownBlock().getX() == blueprint.getLocation().getBlockX() >> 4 &&
                event.getTownBlock().getZ() == blueprint.getLocation().getBlockZ() >> 4) {
                blueprint.setActive(false);
            }
        }
    }
}