package me.redraskal.mixerbridge;

import com.mixer.interactive.GameClient;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Copyright (c) Redraskal 2017.
 * <p>
 * Please do not copy the code below unless you
 * have permission to do so from me.
 */
public class MixerCommand implements CommandExecutor {

    @Getter private final MixerBridge mixerBridge;

    public MixerCommand(MixerBridge mixerBridge) {
        this.mixerBridge = mixerBridge;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to execute this command.");
            return false;
        }
        Player player = (Player) sender;
        if(args.length > 0) {
            if(args[0].equalsIgnoreCase("start")) {
                if(args.length > 2) {
                    try {
                        int versionID = Integer.parseInt(args[1]);
                        String shareCode = args[2];

                        if(this.getMixerBridge().getMixerManager().getGameClient(player.getUniqueId()) != null) {
                            player.sendMessage(this.getMixerBridge().buildMessage(
                                    this.getMixerBridge().getConfig().getString("already-connected-message")));
                        } else {
                            String accessToken = this.getMixerBridge().getMixerManager().requestAccessToken(player.getUniqueId());
                            if(accessToken.isEmpty()) {
                                String code = this.getMixerBridge().getMixerManager().requestShortCode(player.getUniqueId());
                                if(code.isEmpty()) {
                                    player.sendMessage(this.getMixerBridge().buildMessage(
                                            this.getMixerBridge().getConfig().getString("backend-error-message")));
                                } else {
                                    player.sendMessage(this.getMixerBridge().buildMessage(
                                            this.getMixerBridge().getConfig().getString("link-message")
                                                    .replace("<code>", code)));
                                }
                            } else {
                                GameClient gameClient = this.getMixerBridge().getMixerManager().createGameClient(player.getUniqueId(),
                                        accessToken, versionID, shareCode);
                                if(gameClient != null) {
                                    player.sendMessage(this.getMixerBridge().buildMessage(
                                            this.getMixerBridge().getConfig().getString("start-message")));
                                } else {
                                    player.sendMessage(this.getMixerBridge().buildMessage(
                                            this.getMixerBridge().getConfig().getString("backend-error-message")));
                                }
                            }
                        }
                    } catch (Exception e) {
                        player.sendMessage(this.getMixerBridge().buildMessage(
                                this.getMixerBridge().getConfig().getString("invalid-info-message")));
                    }
                } else {
                    player.sendMessage(this.getMixerBridge().buildMessage(
                            this.getMixerBridge().getConfig().getString("invalid-arguments-message")));
                }
                return false;
            }
            if(args[0].equalsIgnoreCase("stop")) {
                if(this.getMixerBridge().getMixerManager().getGameClient(player.getUniqueId()) != null) {
                    try {
                        this.getMixerBridge().getMixerManager().removeGameClient(player.getUniqueId());
                        player.sendMessage(this.getMixerBridge().buildMessage(
                                this.getMixerBridge().getConfig().getString("stop-message")));
                    } catch (Exception e) {
                        e.printStackTrace();
                        player.sendMessage(this.getMixerBridge().buildMessage(
                                this.getMixerBridge().getConfig().getString("backend-error-message")));
                    }
                } else {
                    player.sendMessage(this.getMixerBridge().buildMessage(
                            this.getMixerBridge().getConfig().getString("not-connected-message")));
                }
                return false;
            }
            if(args[0].equalsIgnoreCase("scene")) {
                //TODO
                return false;
            }
            player.sendMessage(this.getMixerBridge().buildMessage(
                    this.getMixerBridge().getConfig().getString("invalid-arguments-message")));
        } else {
            for(String line : this.getMixerBridge().getConfig().getStringList("help-message")) {
                player.sendMessage(this.getMixerBridge().buildMessage(line));
            }
        }
        return false;
    }
}