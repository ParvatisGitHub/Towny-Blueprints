package com.townyblueprints.listeners;

import com.townyblueprints.TownyBlueprints;
import com.townyblueprints.models.Blueprint;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class ChatInputListener implements Listener {
    private final TownyBlueprints plugin;
    private final Map<UUID, BlueprintEditSession> editSessions = new HashMap<>();

    public enum InputState {
        NONE,
        BLUEPRINT_NAME,
        BLUEPRINT_DIMENSIONS,
        BLUEPRINT_BLOCKS,
        BLUEPRINT_INCOME,
        BLUEPRINT_UPKEEP,
        BLUEPRINT_COST
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        BlueprintEditSession session = editSessions.get(player.getUniqueId());

        if (session != null) {
            event.setCancelled(true);
            String message = event.getMessage().trim();

            if (message.equalsIgnoreCase("cancel")) {
                editSessions.remove(player.getUniqueId());
                player.sendMessage("§cBlueprint editing cancelled.");
                return;
            }

            if (message.equalsIgnoreCase("done")) {
                finishEditing(player);
                return;
            }

            switch (session.getCurrentField()) {
                case NAME:
                    handleNameInput(player, session, message);
                    break;
                case DESCRIPTION:
                    handleDescriptionInput(player, session, message);
                    break;
                case SIZE:
                    handleSizeInput(player, session, message);
                    break;
                case REQUIRED_BLOCKS:
                    handleBlocksInput(player, session, message);
                    break;
                case INCOME:
                    handleIncomeInput(player, session, message);
                    break;
                case UPKEEP:
                    handleUpkeepInput(player, session, message);
                    break;
                case COST:
                    handleCostInput(player, session, message);
                    break;
            }
        }
    }

    private void handleNameInput(Player player, BlueprintEditSession session, String input) {
        if (input.length() < 3 || input.length() > 32) {
            player.sendMessage("§cBlueprint name must be between 3 and 32 characters!");
            return;
        }
        session.getBlueprint().setName(input);
        player.sendMessage("§aName set to: §f" + input);
        session.setCurrentField(EditField.DESCRIPTION);
        player.sendMessage("§eEnter the blueprint description, or type 'done' to finish:");
    }

    private void handleDescriptionInput(Player player, BlueprintEditSession session, String input) {
        session.getBlueprint().setDescription(input);
        player.sendMessage("§aDescription set to: §f" + input);
        session.setCurrentField(EditField.SIZE);
        player.sendMessage("§eEnter the size (format: x,y,z), or type 'done' to finish:");
    }

    private void handleSizeInput(Player player, BlueprintEditSession session, String input) {
        String[] parts = input.split(",");
        if (parts.length == 3) {
            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());

                if (x < 1 || y < 1 || z < 1 || x > 32 || y > 32 || z > 32) {
                    player.sendMessage("§cDimensions must be between 1 and 32!");
                    return;
                }

                session.getBlueprint().setSizeX(x);
                session.getBlueprint().setSizeY(y);
                session.getBlueprint().setSizeZ(z);

                player.sendMessage(String.format("§aSize set to: §f%d x %d x %d", x, y, z));
                session.setCurrentField(EditField.REQUIRED_BLOCKS);
                player.sendMessage("§eEnter required blocks (format: MATERIAL:amount), or type 'done' to finish:");
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid format. Use numbers separated by commas (e.g., 5,3,5)");
            }
        } else {
            player.sendMessage("§cInvalid format. Use x,y,z (e.g., 5,3,5)");
        }
    }

    private void handleBlocksInput(Player player, BlueprintEditSession session, String input) {
        String[] parts = input.split(":");
        if (parts.length == 2) {
            try {
                String blockType = parts[0].trim().toUpperCase();
                int amount = Integer.parseInt(parts[1].trim());

                if (amount < 1) {
                    player.sendMessage("§cAmount must be greater than 0!");
                    return;
                }

                // Check if it's a valid block definition
                if (!plugin.getBlockDefinitionManager().isBlockDefinition(blockType)) {
                    // If not a definition, validate as a material
                    try {
                        Material.valueOf(blockType);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid block type! Use a valid material name or block definition.");
                        return;
                    }
                }

                session.getBlueprint().addRequiredBlock(blockType, amount);
                player.sendMessage(String.format("§aAdded requirement: §f%s x%d", blockType, amount));
                player.sendMessage("§eEnter another block requirement, or type 'done' to finish:");
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount. Use format MATERIAL:amount (e.g., STONE:10)");
            }
        } else {
            player.sendMessage("§cInvalid format. Use MATERIAL:amount (e.g., STONE:10)");
        }
    }

    private void handleIncomeInput(Player player, BlueprintEditSession session, String input) {
        String[] parts = input.split(" ");
        if (parts.length == 2) {
            try {
                double amount = Double.parseDouble(parts[0]);
                String type = parts[1].toUpperCase();

                if (amount < 0) {
                    player.sendMessage("§cAmount cannot be negative!");
                    return;
                }

                if (!type.equals("MONEY") && !type.startsWith("template:")) {
                    try {
                        Material.valueOf(type);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid resource type!");
                        return;
                    }
                }

                session.getBlueprint().setDailyIncome(amount);
                session.getBlueprint().setIncomeType(type);
                player.sendMessage(String.format("§aDaily income set to: §f%s %s", amount, type));
                session.setCurrentField(EditField.UPKEEP);
                player.sendMessage("§eEnter daily upkeep (format: amount type), or type 'done' to finish:");
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount format!");
            }
        } else {
            player.sendMessage("§cPlease use format: amount type (e.g., 100 MONEY or 10 DIAMOND)");
        }
    }

    private void handleUpkeepInput(Player player, BlueprintEditSession session, String input) {
        String[] parts = input.split(" ");
        if (parts.length == 2) {
            try {
                double amount = Double.parseDouble(parts[0]);
                String type = parts[1].toUpperCase();

                if (amount < 0) {
                    player.sendMessage("§cAmount cannot be negative!");
                    return;
                }

                if (!type.equals("MONEY") && !type.startsWith("template:")) {
                    try {
                        Material.valueOf(type);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid resource type!");
                        return;
                    }
                }

                session.getBlueprint().setDailyUpkeep(amount);
                session.getBlueprint().setUpkeepType(type);
                player.sendMessage(String.format("§aDaily upkeep set to: §f%s %s", amount, type));
                session.setCurrentField(EditField.COST);
                player.sendMessage("§eEnter placement cost (in coins), or type 'done' to finish:");
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount format!");
            }
        } else {
            player.sendMessage("§cPlease use format: amount type (e.g., 50 MONEY or 5 COAL)");
        }
    }

    private void handleCostInput(Player player, BlueprintEditSession session, String input) {
        try {
            double cost = Double.parseDouble(input);
            if (cost < 0) {
                player.sendMessage("§cPlacement cost cannot be negative!");
                return;
            }

            session.getBlueprint().setPlacementCost(cost);
            player.sendMessage(String.format("§aPlacement cost set to: §f%s", cost));
            finishEditing(player);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid cost format! Please enter a number.");
        }
    }

    private void finishEditing(Player player) {
        BlueprintEditSession session = editSessions.get(player.getUniqueId());
        if (session != null) {
            Blueprint blueprint = session.getBlueprint();
            plugin.getBlueprintManager().saveBlueprint(blueprint);
            editSessions.remove(player.getUniqueId());
            player.sendMessage("§aBlueprint saved successfully!");
        }
    }

    public void startBlueprintCreation(Player player) {
        Blueprint blueprint = new Blueprint();
        editSessions.put(player.getUniqueId(), new BlueprintEditSession(blueprint));
        player.sendMessage("§eEnter the blueprint name, or type 'cancel' to stop:");
    }

    public void setPlayerState(Player player, InputState state) {
        BlueprintEditSession session = editSessions.get(player.getUniqueId());
        if (session != null) {
            session.setCurrentField(EditField.valueOf(state.name()));
        }
    }

    public void clearPlayerState(Player player) {
        editSessions.remove(player.getUniqueId());
    }

    private static class BlueprintEditSession {
        private final Blueprint blueprint;
        private EditField currentField;

        public BlueprintEditSession(Blueprint blueprint) {
            this.blueprint = blueprint;
            this.currentField = EditField.NAME;
        }

        public Blueprint getBlueprint() {
            return blueprint;
        }

        public EditField getCurrentField() {
            return currentField;
        }

        public void setCurrentField(EditField field) {
            this.currentField = field;
        }
    }

    private enum EditField {
        NAME,
        DESCRIPTION,
        SIZE,
        REQUIRED_BLOCKS,
        INCOME,
        UPKEEP,
        COST
    }
}