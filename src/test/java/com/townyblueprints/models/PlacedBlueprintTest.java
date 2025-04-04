package com.townyblueprints.models;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PlacedBlueprintTest {

    @Mock
    private Blueprint blueprint;

    @Mock
    private Location location;

    @Test
    void testPlacedBlueprintCreation() {
        String id = "test-id";
        PlacedBlueprint placedBlueprint = new PlacedBlueprint(id, blueprint, null, location, false);

        assertEquals(id, placedBlueprint.getId());
        assertEquals(blueprint, placedBlueprint.getBlueprint());
        assertNull(placedBlueprint.getTown()); // We don't test Town functionality here
        assertEquals(location, placedBlueprint.getLocation());
        assertFalse(placedBlueprint.isActive());
    }

    @Test
    void testActivationState() {
        PlacedBlueprint placedBlueprint = new PlacedBlueprint("test-id", blueprint, null, location, false);

        assertFalse(placedBlueprint.isActive());

        placedBlueprint.setActive(true);
        assertTrue(placedBlueprint.isActive());
    }

    @Test
    void testLastCollectionTime() {
        long beforeCreation = System.currentTimeMillis();

        PlacedBlueprint placedBlueprint = new PlacedBlueprint("test-id", blueprint, null, location, false);

        long afterCreation = System.currentTimeMillis();

        // Verify that lastCollectionTime was set between creation and now
        assertTrue(placedBlueprint.getLastCollectionTime() >= beforeCreation);
        assertTrue(placedBlueprint.getLastCollectionTime() <= afterCreation);

        long newTime = System.currentTimeMillis() + 1000;
        placedBlueprint.setLastCollectionTime(newTime);
        assertEquals(newTime, placedBlueprint.getLastCollectionTime());
    }

    @Test
    void testUpkeepStatus() {
        PlacedBlueprint placedBlueprint = new PlacedBlueprint("test-id", blueprint, null, location, false);

        assertFalse(placedBlueprint.isSuccessfulUpkeep());

        placedBlueprint.setSuccessfulUpkeep(true);
        assertTrue(placedBlueprint.isSuccessfulUpkeep());
    }
}