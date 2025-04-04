package com.townyblueprints.models;

import com.palmergames.bukkit.towny.object.Town;
import lombok.Data;
import org.bukkit.Location;

@Data
public class PlacedBlueprint {
    private final String id;
    private final Blueprint blueprint;
    private final Town town;
    private final Location location;
    private boolean active;
    private boolean successfulUpkeep;
	private long lastCollectionTime;

    public PlacedBlueprint(String id, Blueprint blueprint, Town town, Location location, boolean active) {
        this.id = id;
        this.blueprint = blueprint;
        this.town = town;
        this.location = location;
        this.active = active;
        this.successfulUpkeep = false;        
		this.lastCollectionTime = System.currentTimeMillis();
    }
	public String getId() {
        return id;
    }
}