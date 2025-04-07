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
import com.townyblueprints.visualization.PlacementVisualizer;
import com.townyblueprints.visualization.ExistingBlueprintVisualizer;
import lombok.Getter;
import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
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
    private IStorage database;
    private static boolean dynmapTownyInstalled;
    private static boolean slimeFunInstalled;
    private static boolean mythicMobsInstalled;
    private static boolean mmmoItemsInstalled;
    private static boolean itemsAdderInstalled;
    private static boolean oraxenInstalled;

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

        // Setup integrations with other plugins
        setupIntegrationsWithOtherPlugins();

        // Load configuration and data
        this.configManager.loadConfig();
        this.blueprintManager.loadAll();
        this.resourceTemplateManager.loadTemplates();
        this.blockDefinitionManager.loadDefinitions();
        this.toolDefinitionManager.loadDefinitions();

        // Load placed blueprints from database
        for (var blueprint : this.database.loadAllBlueprints()) {
            this.blueprintManager.addLoadedBlueprint(blueprint);
        }

        getServer().getScheduler().runTaskLater(this, () -> {
            warehouseManager.loadWarehouses();
        }, 20L);

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
        if(dynmapTownyInstalled) {
            MarkerAPI markerAPI = (MarkerAPI) Bukkit.getPluginManager().getPlugin("Dynmap");
            if (markerAPI != null) {
                this.getServer().getPluginManager().registerEvents(new DynmapListener(this, markerAPI), this);
            }
        }
        // Start blueprint status check based on configuration
        int interval = getConfig().getInt("blueprints.status_check.interval", 100);
        new BlueprintStatusTask(this).runTaskTimer(this, interval, interval);

        this.getLogger().info("TownyBlueprints has been enabled!");
    }

    private void setupIntegrationsWithOtherPlugins() {
        // Check for Dynmap-Towny
        Plugin dynmapTowny = Bukkit.getPluginManager().getPlugin("Dynmap-Towny");
        dynmapTownyInstalled = dynmapTowny != null;
        if(dynmapTownyInstalled)
            info("  DynmapTowny Integration Enabled");
        Plugin slimeFun = Bukkit.getPluginManager().getPlugin("Slimefun");
        slimeFunInstalled = slimeFun != null;
        if(slimeFunInstalled)
            info("  Slimefun Integration Enabled");

        Plugin mythicMobs = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if(mythicMobs != null) {
            String className = Bukkit.getServer().getPluginManager().getPlugin("MythicMobs").getClass().getName();
            if (className.equals("io.lumine.mythic.bukkit.MythicBukkit")) {
                mythicMobsInstalled = true;
                info("  Mythic Mobs Integration Enabled");
            } else {
                mythicMobsInstalled = false;
                this.getLogger().severe("Problem enabling mythic mobs");
            }
        }

        Plugin itemsAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        itemsAdderInstalled = itemsAdder != null;
        if (itemsAdderInstalled)
            info("  ItemsAdder Integration Enabled");

        Plugin oraxen = Bukkit.getPluginManager().getPlugin("Oraxen");
        oraxenInstalled = oraxen != null;
        if (oraxenInstalled)
            info("  Oraxen Integration Enabled");

        Plugin mmmoItems = Bukkit.getPluginManager().getPlugin("MMOItems");
        mmmoItemsInstalled = mmmoItems != null;
        if (mmmoItemsInstalled)
            info("  MMOItems Integration Enabled");
    }

    public static void info(String message) {
        TownyBlueprints.getInstance().getLogger().info(message);
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
