package com.townyblueprints;

import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.townyblueprints.commands.BlueprintCommand;
import com.townyblueprints.commands.BlueprintAdminCommand;
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
import org.bukkit.plugin.java.JavaPlugin;

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

    @Override
    public void onEnable() {
        instance = this;
        printSickASCIIArt();
        
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

        // Load configuration and data
        this.configManager.loadConfig();
        this.blueprintManager.loadAll();
        this.resourceTemplateManager.loadTemplates();
        this.blockDefinitionManager.loadDefinitions();
        this.toolDefinitionManager.loadDefinitions();
        
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

        // Start blueprint status check based on configuration
        int interval = getConfig().getInt("blueprints.status_check.interval", 100);
        new BlueprintStatusTask(this).runTaskTimer(this, interval, interval);

        this.getLogger().info("TownyBlueprints has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all data
        if (this.blueprintManager != null) {
            this.blueprintManager.saveAll();
        }

        this.getLogger().info("TownyBlueprints has been disabled!");
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