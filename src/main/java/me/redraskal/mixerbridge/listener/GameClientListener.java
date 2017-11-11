package me.redraskal.mixerbridge.listener;

import com.google.common.eventbus.Subscribe;
import com.mixer.interactive.GameClient;
import com.mixer.interactive.event.control.input.ControlMouseDownInputEvent;
import com.mixer.interactive.resources.control.ButtonControl;
import com.mixer.interactive.resources.control.InteractiveControl;
import com.mixer.interactive.resources.group.InteractiveGroup;
import com.mixer.interactive.resources.scene.InteractiveScene;
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
    @Getter private String currentScene = "";

    @Getter private Map<String, InteractiveControl> controls = new HashMap<>();
    @Getter private Map<String, InteractiveScene> scenes = new HashMap<>();

    public GameClientListener(MixerManager mixerManager, UUID uuid, GameClient gameClient) {
        this.mixerManager = mixerManager;
        this.uuid = uuid;
        this.gameClient = gameClient;
        this.fetchScenes();
        this.updateSceneControls();
        this.getGameClient().ready(true);
    }

    public void updateSceneControls() {
        controls.clear();
        try {
            for(InteractiveControl interactiveControl : this.getGameClient().getServiceManager()
                    .get(GameClient.CONTROL_SERVICE_PROVIDER).getControls().get()) {
                if(!interactiveControl.getSceneID().equalsIgnoreCase(this.getCurrentScene())) continue;
                controls.put(interactiveControl.getControlID(), interactiveControl);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void fetchScenes() {
        scenes.clear();
        try {
            for(InteractiveScene interactiveScene : this.getGameClient().getServiceManager()
                    .get(GameClient.SCENE_SERVICE_PROVIDER).getScenes().get()) {
                scenes.put(interactiveScene.getSceneID(), interactiveScene);
                if(interactiveScene.isDefault()) currentScene = interactiveScene.getSceneID();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public boolean setCurrentScene(String sceneID) {
        if(!this.getScenes().containsKey(sceneID)) return false;

        this.currentScene = sceneID;
        try {
            for(InteractiveGroup interactiveGroup : this.getGameClient().getServiceManager()
                    .get(GameClient.GROUP_SERVICE_PROVIDER).getGroups().get()) {
                interactiveGroup.setScene(this.getCurrentScene());
                interactiveGroup.update(this.getGameClient()).get();
            }
            this.updateSceneControls();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Subscribe
    public void onMouseDown(ControlMouseDownInputEvent event) {
        if(!controls.containsKey(event.getControlInput().getControlID())
                || !(controls.get(event.getControlInput().getControlID()) instanceof ButtonControl)) return;
        try {
            ButtonControl buttonControl = (ButtonControl) controls.get(event.getControlInput().getControlID());
            if(buttonControl.getMeta().has("cooldown")) {
                buttonControl.setCooldown(System.currentTimeMillis()+(buttonControl.getMeta().get("cooldown")
                        .getAsJsonObject().get("value").getAsInt()*1000));
            }
            buttonControl.update(this.getGameClient());
            if(buttonControl.getMeta().has("command")) {
                new BukkitRunnable() {
                    public void run() {
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                                buttonControl.getMeta().get("command").getAsJsonObject().get("value").getAsString());
                    }
                }.runTaskLater(this.getMixerManager().getMixerBridge(), 1L);
            }
            if(event.getTransaction() != null)
                event.getTransaction().capture(this.getGameClient()).complete(true);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Mixer] Spark transaction issue: " + e.getMessage());
        }
    }
}