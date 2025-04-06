package com.townyblueprints.db;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.db.TownySQLSource;
import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.Blueprint;
import com.townyblueprints.models.PlacedBlueprint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public final class Database implements IStorage {
    public static final int CURRENT_SCHEMA = 1;
    private static String prefix;
    private final TownyBlueprints plugin;
    private final boolean usingSQLDatabase;
    private File dataFile;

    public Database(TownyBlueprints plugin) {
        this.plugin = plugin;
        this.usingSQLDatabase = TownyAPI.getInstance().getDataSource() instanceof TownySQLSource;

        if (!usingSQLDatabase) {
            this.dataFile = new File(plugin.getDataFolder(), "blueprints_data.yml");
        }
    }

    @Override
    public void init() {
        if (usingSQLDatabase) {
            initSQL();
        } else {
            initFlatFile();
        }
    }

    private void initSQL() {
        try {
            prefix = TownySettings.getSQLTablePrefix();

            // Get current schema version
            int currentVersion = 0;
            try {
                String versionQuery = loadSQLFile("check_schema_version.sql");
                TownySQLSource sqlSource = (TownySQLSource) TownyAPI.getInstance().getDataSource();

                try (var conn = sqlSource.getHikariDataSource().getConnection();
                     var stmt = conn.prepareStatement(versionQuery)) {
                    try (var rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            currentVersion = rs.getInt("version");
                        }
                    }
                }
            } catch (SQLException e) {
                // Table probably doesn't exist yet, which is fine
                plugin.getLogger().info("No schema version found, will create new tables");
            }

            // Create or update schema if needed
            if (currentVersion < CURRENT_SCHEMA) {
                plugin.getLogger().info("Initializing database schema...");
                String createSchema = loadSQLFile("create_schema.sql");
                TownySQLSource sqlSource = (TownySQLSource) TownyAPI.getInstance().getDataSource();

                try (var conn = sqlSource.getHikariDataSource().getConnection()) {
                    for (String query : createSchema.split(";")) {
                        String trimmedQuery = query.trim();
                        if (!trimmedQuery.isEmpty()) {
                            try (var stmt = conn.prepareStatement(trimmedQuery)) {
                                stmt.executeUpdate();
                            }
                        }
                    }
                }
                plugin.getLogger().info("Database schema initialized successfully!");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void initFlatFile() {
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                YamlConfiguration config = new YamlConfiguration();
                config.createSection("blueprints");
                config.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create blueprints data file", e);
            }
        }
    }

    @Override
    public Collection<PlacedBlueprint> loadAllBlueprints() {
        if (usingSQLDatabase) {
            return loadBlueprintsSQL();
        } else {
            return loadBlueprintsFlatFile();
        }
    }

    private Collection<PlacedBlueprint> loadBlueprintsSQL() {
        Set<PlacedBlueprint> blueprints = new HashSet<>();
        try {
            String query = loadSQLFile("load_blueprints.sql");
            TownySQLSource sqlSource = (TownySQLSource) TownyAPI.getInstance().getDataSource();

            try (var conn = sqlSource.getHikariDataSource().getConnection();
                 var stmt = conn.prepareStatement(query)) {
                try (var rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String[] locationParts = rs.getString("location").split(";");
                        String blueprintId = rs.getString("blueprint_id");

                        Blueprint blueprint = plugin.getBlueprintManager().getBlueprint(blueprintId);
                        if (blueprint == null) {
                            plugin.getLogger().warning("Blueprint not found: " + blueprintId);
                            continue;
                        }

                        Location location = new Location(
                                Bukkit.getWorld(locationParts[0]),
                                Double.parseDouble(locationParts[1]),
                                Double.parseDouble(locationParts[2]),
                                Double.parseDouble(locationParts[3])
                        );

                        PlacedBlueprint placedBlueprint = new PlacedBlueprint(
                                rs.getString("id"),
                                blueprint,
                                TownyAPI.getInstance().getTown(UUID.fromString(rs.getString("town_id"))),
                                location,
                                rs.getBoolean("active")
                        );

                        placedBlueprint.setLastCollectionTime(rs.getLong("last_collection_time"));
                        placedBlueprint.setSuccessfulUpkeep(rs.getBoolean("successful_upkeep"));

                        blueprints.add(placedBlueprint);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load blueprints from database", e);
            throw new RuntimeException(e);
        }
        return blueprints;
    }

    private Collection<PlacedBlueprint> loadBlueprintsFlatFile() {
        Set<PlacedBlueprint> blueprints = new HashSet<>();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        var blueprintsSection = config.getConfigurationSection("blueprints");
        if (blueprintsSection == null) return blueprints;

        for (String id : blueprintsSection.getKeys(false)) {
            var bpSection = blueprintsSection.getConfigurationSection(id);
            if (bpSection == null) continue;

            String blueprintId = bpSection.getString("blueprint_id");
            String townId = bpSection.getString("town_id");

            Blueprint blueprint = plugin.getBlueprintManager().getBlueprint(blueprintId);
            if (blueprint == null) {
                plugin.getLogger().warning("Blueprint not found: " + blueprintId);
                continue;
            }

            String[] locationParts = bpSection.getString("location", "").split(";");
            if (locationParts.length != 4) continue;

            Location location = new Location(
                    Bukkit.getWorld(locationParts[0]),
                    Double.parseDouble(locationParts[1]),
                    Double.parseDouble(locationParts[2]),
                    Double.parseDouble(locationParts[3])
            );

            PlacedBlueprint placedBlueprint = new PlacedBlueprint(
                    id,
                    blueprint,
                    TownyAPI.getInstance().getTown(UUID.fromString(townId)),
                    location,
                    bpSection.getBoolean("active")
            );

            placedBlueprint.setLastCollectionTime(bpSection.getLong("last_collection_time"));
            placedBlueprint.setSuccessfulUpkeep(bpSection.getBoolean("successful_upkeep"));

            blueprints.add(placedBlueprint);
        }

        return blueprints;
    }

    @Override
    public void saveBlueprint(PlacedBlueprint blueprint) {
        if (usingSQLDatabase) {
            saveBlueprintSQL(blueprint);
        } else {
            saveBlueprintFlatFile(blueprint);
        }
    }

    private void saveBlueprintSQL(PlacedBlueprint blueprint) {
        try {
            String query = loadSQLFile("save_blueprint.sql");
            TownySQLSource sqlSource = (TownySQLSource) TownyAPI.getInstance().getDataSource();

            try (var conn = sqlSource.getHikariDataSource().getConnection();
                 var stmt = conn.prepareStatement(query)) {
                stmt.setString(1, blueprint.getId());
                stmt.setString(2, blueprint.getTown().getUUID().toString());
                stmt.setString(3, blueprint.getBlueprint().getName());

                Location loc = blueprint.getLocation();
                stmt.setString(4, String.format("%s;%f;%f;%f",
                        loc.getWorld().getName(),
                        loc.getX(),
                        loc.getY(),
                        loc.getZ()
                ));

                stmt.setBoolean(5, blueprint.isActive());
                stmt.setLong(6, blueprint.getLastCollectionTime());
                stmt.setBoolean(7, blueprint.isSuccessfulUpkeep());
                stmt.setBoolean(8, plugin.getBlueprintManager().getBonusBlockContributions().getOrDefault(blueprint.getId(), blueprint.isActive()));

                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save blueprint " + blueprint.getId(), e);
            throw new RuntimeException(e);
        }
    }

    private void saveBlueprintFlatFile(PlacedBlueprint blueprint) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String id = blueprint.getId();

        var bpSection = config.createSection("blueprints." + id);
        bpSection.set("blueprint_id", blueprint.getBlueprint().getName());
        bpSection.set("town_id", blueprint.getTown().getUUID().toString());

        Location loc = blueprint.getLocation();
        bpSection.set("location", String.format("%s;%f;%f;%f",
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ()
        ));

        bpSection.set("active", blueprint.isActive());
        bpSection.set("last_collection_time", blueprint.getLastCollectionTime());
        bpSection.set("successful_upkeep", blueprint.isSuccessfulUpkeep());
        bpSection.set("contributing_bonus_blocks", plugin.getBlueprintManager().getBonusBlockContributions().getOrDefault(id, blueprint.isActive()));

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save blueprint " + id, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteBlueprint(String id) {
        if (usingSQLDatabase) {
            deleteBlueprintSQL(id);
        } else {
            deleteBlueprintFlatFile(id);
        }
    }

    private void deleteBlueprintSQL(String id) {
        try {
            String query = loadSQLFile("delete_blueprint.sql");
            TownySQLSource sqlSource = (TownySQLSource) TownyAPI.getInstance().getDataSource();

            try (var conn = sqlSource.getHikariDataSource().getConnection();
                 var stmt = conn.prepareStatement(query)) {
                stmt.setString(1, id);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete blueprint " + id, e);
            throw new RuntimeException(e);
        }
    }

    private void deleteBlueprintFlatFile(String id) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        config.set("blueprints." + id, null);

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete blueprint " + id, e);
            throw new RuntimeException(e);
        }
    }

    private String loadSQLFile(String filename) {
        try (InputStream is = plugin.getResource("sql/" + filename)) {
            if (is == null) {
                throw new RuntimeException("Could not find SQL file: " + filename);
            }
            return new String(is.readAllBytes()).replaceAll("%prefix%", prefix);
        } catch (IOException e) {
            throw new RuntimeException("Error reading SQL file: " + filename, e);
        }
    }
}