package me.redraskal.mixerbridge.listener;

import lombok.Getter;
import me.redraskal.mixerbridge.manager.MixerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.ExecutionException;

/**
 * Copyright (c) Redraskal 2017.
 * <p>
 * Please do not copy the code below unless you
 * have permission to do so from me.
 */
public class MixerManagerListener implements Listener {

    @Getter private final MixerManager mixerManager;

    public MixerManagerListener(MixerManager mixerManager) {
        this.mixerManager = mixerManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            this.getMixerManager().removeGameClient(event.getPlayer().getUniqueId());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}