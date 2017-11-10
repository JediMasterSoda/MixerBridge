package me.redraskal.mixerbridge.listener;

import com.google.common.eventbus.Subscribe;
import com.mixer.interactive.GameClient;
import com.mixer.interactive.event.control.input.ControlMouseDownInputEvent;
import com.mixer.interactive.resources.control.ButtonControl;
import com.mixer.interactive.resources.control.InteractiveControl;
import lombok.Getter;
import me.redraskal.mixerbridge.manager.MixerManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Copyright (c) Redraskal 2017.
 * <p>
 * Please do not copy the code below unless you
 * have permission to do so from me.
 */
public class GameClientListener {

    @Getter private final MixerManager mixerManager;
    @Getter private final UUID uuid;
    @Getter private final GameClient gameClient;

    private Map<String, InteractiveControl> controls = new HashMap<>();

    public GameClientListener(MixerManager mixerManager, UUID uuid, GameClient gameClient) {
        this.mixerManager = mixerManager;
        this.uuid = uuid;
        this.gameClient = gameClient;
        this.getSceneControls();
        this.getGameClient().ready(true);
    }

    public void getSceneControls() {
        controls.clear();
        try {
            for(InteractiveControl interactiveControl : this.getGameClient().getServiceManager()
                    .get(GameClient.CONTROL_SERVICE_PROVIDER).getControls().get()) {
                controls.put(interactiveControl.getControlID(), interactiveControl);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onMouseDown(ControlMouseDownInputEvent event) {
        try {
            ButtonControl buttonControl = (ButtonControl) controls.get(event.getControlInput().getControlID());
            buttonControl.setCooldown(System.currentTimeMillis()+(buttonControl.getMeta().get("cooldown")
                    .getAsJsonObject().get("value").getAsInt()*1000));
            buttonControl.update(this.getGameClient());
            new BukkitRunnable() {
                public void run() {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                            buttonControl.getMeta().get("command").getAsJsonObject().get("value").getAsString());
                }
            }.runTaskLater(this.getMixerManager().getMixerBridge(), 1L);
            if(event.getTransaction() != null)
                event.getTransaction().capture(this.getGameClient()).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}