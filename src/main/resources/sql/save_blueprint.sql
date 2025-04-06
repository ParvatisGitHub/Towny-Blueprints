INSERT INTO TOWNY_BLUEPRINTS (
    id, 
    town_id,
    blueprint_id,
    location,
    active,
    last_collection_time,
    successful_upkeep,
    contributing_bonus_blocks
) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
ON DUPLICATE KEY UPDATE
    town_id = VALUES(town_id),
    blueprint_id = VALUES(blueprint_id),
    location = VALUES(location),
    active = VALUES(active),
    last_collection_time = VALUES(last_collection_time),
    successful_upkeep = VALUES(successful_upkeep),
    contributing_bonus_blocks = VALUES(contributing_bonus_blocks);