package com.townyblueprints;

import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.townyblueprints.commands.BlueprintCommand;
import com.townyblueprints.commands.BlueprintAdminCommand;
import com.townyblueprints.db.Database;
import com.townyblueprints.db.IStorage;
import com.townyblueprints.handlers.BlueprintPlacementHandler;
import com.townyblueprints.handlers.ResourceCollectionHandler;
import com.townyblueprints.handlers.UpkeepHandler;
import com.townyblueprints.listeners.*;
import com.townyblueprints.managers.*;
import com.townyblueprints.tasks.BlueprintStatusTask;
import com.townyblueprints.util.SchematicUtil;
import com.townyblueprints.visualization.PlacementVisualizer;
import com.townyblueprints.visualization.ExistingBlueprintVisualizer;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.MarkerAPI;

@Getter
public class TownyBlueprints extends JavaPlugin {

    @Getter
    private static TownyBlueprints instance;

    private ConfigManager configManager;
    private BlueprintManager blueprintManager;
    private GUIManager guiManager;
    private BlueprintPlacementHandler placementHandler;
    private ResourceCollectionHandler resourceCollectionHandler;
    private ChatInputListener chatInputListener;
    private ResourceTemplateManager resourceTemplateManager;
    private WarehouseManager warehouseManager;
    private UpkeepHandler upkeepHandler;
    private BlockDefinitionManager blockDefinitionManager;
    private ToolDefinitionManager toolDefinitionManager;
    private SchematicUtil schematicUtil;
    private IStorage database;
    private DynmapListener dynmapListener;
    private TownBuildLoadManager townBuildLoadManager;

    @Override
    public void onEnable() {
        instance = this;
        printSickASCIIArt();

        // Initialize database first
        this.database = new Database(this);
        this.database.init();

        // Initialize managers and handlers
        this.configManager = new ConfigManager(this);
        this.blueprintManager = new BlueprintManager(this);
        this.guiManager = new GUIManager(this);

        // Create visualizers first
        PlacementVisualizer placementVisualizer = new PlacementVisualizer(this);
        ExistingBlueprintVisualizer existingVisualizer = new ExistingBlueprintVisualizer(this);

        // Then create BlueprintPlacementHandler with both visualizers
        this.placementHandler = new BlueprintPlacementHandler(this, placementVisualizer, existingVisualizer);

        this.resourceCollectionHandler = new ResourceCollectionHandler(this);
        this.chatInputListener = new ChatInputListener(this);
        this.resourceTemplateManager = new ResourceTemplateManager(this);
        this.warehouseManager = new WarehouseManager(this);
        this.upkeepHandler = new UpkeepHandler(this);
        this.blockDefinitionManager = new BlockDefinitionManager(this);
        this.toolDefinitionManager = new ToolDefinitionManager(this);
        this.townBuildLoadManager = new TownBuildLoadManager(this);

        // Load configuration and data
        this.configManager.loadConfig();
        this.blueprintManager.loadAll();
        this.resourceTemplateManager.loadTemplates();
        this.blockDefinitionManager.loadDefinitions();
        this.toolDefinitionManager.loadDefinitions();
        this.schematicUtil = new SchematicUtil(this);

        // Load placed blueprints from database
        for (var blueprint : this.database.loadAllBlueprints()) {
            this.blueprintManager.addLoadedBlueprint(blueprint);
        }

        getServer().getScheduler().runTaskLater(this, () -> {
            warehouseManager.loadWarehouses();
        }, 20L);

        // Setup Dynmap integration
        setupDynmap();

        // Register Towny commands
        BlueprintCommand blueprintCommand = new BlueprintCommand(this);
        BlueprintAdminCommand adminCommand = new BlueprintAdminCommand(this);

        // Add town commands
        TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "blueprint", blueprintCommand);
        TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "bp", blueprintCommand);

        // Add admin commands
        TownyCommandAddonAPI.addSubCommand(CommandType.TOWNYADMIN, "blueprint", adminCommand);
        TownyCommandAddonAPI.addSubCommand(CommandType.TOWNYADMIN, "bp", adminCommand);

        // Register listeners
        this.getServer().getPluginManager().registerEvents(new TownEventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new DailyTaskListener(this), this);
        this.getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        this.getServer().getPluginManager().registerEvents(this.chatInputListener, this);
        this.getServer().getPluginManager().registerEvents(this.resourceCollectionHandler, this);
        this.getServer().getPluginManager().registerEvents(new PlayerMovementListener(this), this);
        this.getServer().getPluginManager().registerEvents(new TownStatusListener(this), this);

        // Start blueprint status check based on configuration
        int interval = getConfig().getInt("blueprints.status_check.interval", 100);
        new BlueprintStatusTask(this).runTaskTimer(this, interval, interval);

        this.getLogger().info("TownyBlueprints has been enabled!");
    }

    private void setupDynmap() {
        if (this.getConfigManager().isDynmapEnabled()) {
        Plugin dynmap = getServer().getPluginManager().getPlugin("dynmap");
        if (dynmap != null && dynmap instanceof DynmapCommonAPI) {
            DynmapCommonAPI dynmapCommonAPI = (DynmapCommonAPI) dynmap;

            try {
                // Access the MarkerAPI from the DynmapAPI
                MarkerAPI markerAPI = dynmapCommonAPI.getMarkerAPI();
                if (markerAPI != null) {
                    this.dynmapListener = new DynmapListener(this, markerAPI);
                    getServer().getPluginManager().registerEvents(this.dynmapListener, this);
                    getLogger().info("Dynmap integration enabled successfully!");
                } else {
                    getLogger().warning("Failed to initialize Dynmap integration - MarkerAPI not available");
                }
            } catch (Exception e) {
                getLogger().severe("Error initializing Dynmap integration: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            getLogger().warning("Dynmap plugin not found or is not of the expected type.");
        }
    }
        }

    @Override
    public void onDisable() {
        // Save all data
        if (this.blueprintManager != null) {
            this.blueprintManager.saveAll();
        }

        this.getLogger().info("TownyBlueprints has been disabled!");
    }

    public IStorage getDatabase() {
        return database;
    }
    public SchematicUtil getSchematicUtil() {
        return schematicUtil;
    }
    public TownBuildLoadManager getTownBuildLoadManager() {
        return townBuildLoadManager;
    }
    private void printSickASCIIArt() {
        String gold = ChatColor.GOLD.toString();
        String reset = ChatColor.RESET.toString();

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(gold + "   **------------------------------------------------------- TownyBlueprints -------------------------------------------------**");
        Bukkit.getConsoleSender().sendMessage(gold + "   ** ,--------.                                      ,-----.  ,--.                             ,--.          ,--.            **" + reset);
        Bukkit.getConsoleSender().sendMessage(gold + "   ** '--.  .--',---. ,--.   ,--.,--,--, ,--. ,--.    |  |) /_ |  |,--.,--. ,---.  ,---. ,--.--.`--',--,--, ,-'  '-. ,---.    **" + reset);
        Bukkit.getConsoleSender().sendMessage(gold + "   **    |  |  | .-. ||  |.'.|  ||      | |  '  /     |  .-.  ||  ||  ||  || .-. :| .-. ||  .--',--.|      |'-.  .-'(  .-'    **" + reset);
        Bukkit.getConsoleSender().sendMessage(gold + "   **    |  |  ' '-' '|   .'.   ||  ||  |  |   '      |  '--' /|  |'  ''  '|   --.| '-' '|  |   |  ||  ||  |  |  |  .-'  `)   **" + reset);
        Bukkit.getConsoleSender().sendMessage(gold + "   **    `--'   `---' '--'   '--'`--''--'.-'  /       `------' `--' `----'  `----'|  |-' `--'   `--'`--''--'  `--'  `----'    **" + reset);
        Bukkit.getConsoleSender().sendMessage(gold + "   **-------------------------------------------------- Build Your Town's Future ---------------------------------------------**");
        Bukkit.getConsoleSender().sendMessage(gold + "   **-------------------------------------------------------------------------------------------------------------------------**");
        Bukkit.getConsoleSender().sendMessage("");
    }
}
