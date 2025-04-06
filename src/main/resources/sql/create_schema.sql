-- Create schema version tracking table
CREATE TABLE IF NOT EXISTS TOWNY_BLUEPRINTS_SCHEMA (
    version INT PRIMARY KEY
);

-- Create main blueprints table 
CREATE TABLE IF NOT EXISTS TOWNY_BLUEPRINTS (
    id VARCHAR(36) PRIMARY KEY,
    town_id VARCHAR(36) NOT NULL,
    blueprint_id VARCHAR(255) NOT NULL,
    location TEXT NOT NULL,
    active BOOLEAN DEFAULT false,
    last_collection_time BIGINT DEFAULT 0,
    successful_upkeep BOOLEAN DEFAULT false,
    contributing_bonus_blocks BOOLEAN DEFAULT false
);

-- Insert initial schema version
INSERT INTO TOWNY_BLUEPRINTS_SCHEMA (version) VALUES (1);
