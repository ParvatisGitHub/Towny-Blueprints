package com.townyblueprints.models;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

class BlueprintTest {
    @Test
    void testBlueprintCreation() {
        Blueprint blueprint = new Blueprint();
        blueprint.setName("Test Blueprint");
        assertEquals("Test Blueprint", blueprint.getName());
    }

    @Test
    void testBlueprintProperties() {
        Blueprint blueprint = new Blueprint();
        blueprint.setName("Test Blueprint");
        blueprint.setDescription("A test blueprint");
        blueprint.setSizeX(10);
        blueprint.setSizeY(5);
        blueprint.setSizeZ(10);
        blueprint.setDailyIncome(100.0);
        blueprint.setIncomeType("MONEY");
        blueprint.setDailyUpkeep(50.0);
        blueprint.setUpkeepType("MONEY");
        blueprint.setPlacementCost(1000.0);
        blueprint.setMaxPerTown(3);
        blueprint.setRequiredTownLevel(2);

        assertEquals("A test blueprint", blueprint.getDescription());
        assertEquals(10, blueprint.getSizeX());
        assertEquals(5, blueprint.getSizeY());
        assertEquals(10, blueprint.getSizeZ());
        assertEquals(100.0, blueprint.getDailyIncome());
        assertEquals("MONEY", blueprint.getIncomeType());
        assertEquals(50.0, blueprint.getDailyUpkeep());
        assertEquals("MONEY", blueprint.getUpkeepType());
        assertEquals(1000.0, blueprint.getPlacementCost());
        assertEquals(3, blueprint.getMaxPerTown());
        assertEquals(2, blueprint.getRequiredTownLevel());
    }

    @Test
    void testRequiredBlocks() {
        Blueprint blueprint = new Blueprint();
        Map<String, Integer> blocks = new HashMap<>();
        blocks.put("STONE", 10);
        blocks.put("OAK_WOOD", 5);
        blocks.put("logs", 3); // Testing block definition

        blueprint.setRequiredBlocks(blocks);

        Map<String, Integer> retrievedBlocks = blueprint.getRequiredBlocks();
        assertEquals(3, retrievedBlocks.size());
        assertEquals(10, retrievedBlocks.get("STONE"));
        assertEquals(5, retrievedBlocks.get("OAK_WOOD"));
        assertEquals(3, retrievedBlocks.get("logs"));
    }

    @Test
    void testAddRequiredBlock() {
        Blueprint blueprint = new Blueprint();
        blueprint.addRequiredBlock("STONE", 10);
        blueprint.addRequiredBlock("logs", 5);

        Map<String, Integer> blocks = blueprint.getRequiredBlocks();
        assertEquals(2, blocks.size());
        assertEquals(10, blocks.get("STONE"));
        assertEquals(5, blocks.get("logs"));
    }

    @Test
    void testRemoveRequiredBlock() {
        Blueprint blueprint = new Blueprint();
        blueprint.addRequiredBlock("STONE", 10);
        blueprint.addRequiredBlock("logs", 5);
        blueprint.removeRequiredBlock("STONE");

        Map<String, Integer> blocks = blueprint.getRequiredBlocks();
        assertEquals(1, blocks.size());
        assertNull(blocks.get("STONE"));
        assertEquals(5, blocks.get("logs"));
    }

    @Test
    void testRequiresBlock() {
        Blueprint blueprint = new Blueprint();
        blueprint.addRequiredBlock("STONE", 10);

        assertTrue(blueprint.requiresBlock("STONE"));
        assertFalse(blueprint.requiresBlock("DIRT"));
    }

    @Test
    void testGetRequiredBlockAmount() {
        Blueprint blueprint = new Blueprint();
        blueprint.addRequiredBlock("STONE", 10);

        assertEquals(10, blueprint.getRequiredBlockAmount("STONE"));
        assertEquals(0, blueprint.getRequiredBlockAmount("DIRT"));
    }

    @Test
    void testPermissionNode() {
        Blueprint blueprint = new Blueprint();
        blueprint.setPermissionNode("townyblueprints.blueprint.test");
        assertEquals("townyblueprints.blueprint.test", blueprint.getPermissionNode());
    }
}