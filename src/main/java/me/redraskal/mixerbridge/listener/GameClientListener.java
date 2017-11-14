package me.redraskal.mixerbridge.listener;

import com.google.common.eventbus.Subscribe;
import com.mixer.interactive.GameClient;
import com.mixer.interactive.event.control.input.ControlMouseDownInputEvent;
import com.mixer.interactive.event.participant.ParticipantJoinEvent;
import com.mixer.interactive.event.participant.ParticipantLeaveEvent;
import com.mixer.interactive.resources.control.ButtonControl;
import com.mixer.interactive.resources.control.InteractiveControl;
import com.mixer.interactive.resources.group.InteractiveGroup;
import com.mixer.interactive.resources.participant.InteractiveParticipant;
import com.mixer.interactive.resources.scene.InteractiveScene;
import lombok.Getter;
import me.redraskal.mixerbridge.TeamworkButton;
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
    @Getter private Map<String, TeamworkButton> teamworkControls = new HashMap<>();
    @Getter private Map<String, InteractiveScene> scenes = new HashMap<>();
    @Getter private Map<String, String> participants = new HashMap<>();

    @Getter private long timeOffset = 0L;

    public GameClientListener(MixerManager mixerManager, UUID uuid, GameClient gameClient) {
        this.mixerManager = mixerManager;
        this.uuid = uuid;
        this.gameClient = gameClient;
        this.fetchScenes();
        this.updateSceneControls();
        this.updateTimeOffset();
        this.getGameClient().ready(true);
    }

    public void updateTimeOffset() {
        try {
            long requestStart = System.currentTimeMillis();
            long mixerTime = this.getGameClient().getTime().get();
            long requestLatency = (System.currentTimeMillis()-requestStart);
            this.timeOffset = (mixerTime-System.currentTimeMillis())+requestLatency;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void updateSceneControls() {
        controls.clear();
        teamworkControls.clear();
        try {
            for(InteractiveControl interactiveControl : this.getGameClient().getServiceManager()
                    .get(GameClient.CONTROL_SERVICE_PROVIDER).getControls().get()) {
                if(!interactiveControl.getSceneID().equalsIgnoreCase(this.getCurrentScene())) continue;
                controls.put(interactiveControl.getControlID(), interactiveControl);
                if(interactiveControl instanceof ButtonControl) {
                    ButtonControl buttonControl = (ButtonControl) interactiveControl;
                    if(buttonControl.getMeta().has("clicks_needed")) {
                        teamworkControls.put(buttonControl.getControlID(),
                                new TeamworkButton(buttonControl.getMeta()
                                        .get("clicks_needed").getAsJsonObject().get("value").getAsInt()));
                    }
                }
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
    public void onParticipantJoin(ParticipantJoinEvent event) {
        for(InteractiveParticipant interactiveParticipant : event.getParticipants()) {
            participants.put(interactiveParticipant.getSessionID(), interactiveParticipant.getUsername());
        }
    }

    @Subscribe
    public void onParticipantLeave(ParticipantLeaveEvent event) {
        for(InteractiveParticipant interactiveParticipant : event.getParticipants()) {
            if(participants.containsKey(interactiveParticipant.getSessionID()))
                participants.remove(interactiveParticipant.getSessionID());
        }
    }

    @Subscribe
    public void onMouseDown(ControlMouseDownInputEvent event) {
        if(!controls.containsKey(event.getControlInput().getControlID())
                || !(controls.get(event.getControlInput().getControlID()) instanceof ButtonControl)) return;
        try {
            ButtonControl buttonControl = (ButtonControl) controls.get(event.getControlInput().getControlID());
            String clickers = "unknown";
            boolean applyCooldown = true;

            if(participants.containsKey(event.getParticipantID())) clickers = participants.get(event.getParticipantID());

            if(teamworkControls.containsKey(event.getControlInput().getControlID())) {
                TeamworkButton teamworkButton = teamworkControls.get(event.getControlInput().getControlID());
                if(participants.containsKey(event.getParticipantID())) {
                    if(teamworkButton.addClick(participants.get(event.getParticipantID()))) {
                        clickers = teamworkButton.reset();
                        applyCooldown = true;
                        buttonControl.setProgress(0f);
                    } else {
                        buttonControl.setProgress(teamworkButton.getPercentageFilled());
                        applyCooldown = false;
                    }
                }
            }

            if(buttonControl.getMeta().has("cooldown") && applyCooldown) {
                long mixerTime = System.currentTimeMillis()+this.getTimeOffset();
                buttonControl.setCooldown(mixerTime+(buttonControl.getMeta().get("cooldown")
                        .getAsJsonObject().get("value").getAsNumber().doubleValue()*1000));
            }
            buttonControl.update(this.getGameClient());

            final String f_clickers = clickers;
            if(buttonControl.getMeta().has("command") && applyCooldown) {
                new BukkitRunnable() {
                    public void run() {
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                                buttonControl.getMeta().get("command").getAsJsonObject().get("value").getAsString()
                                        .replace("<username>", f_clickers));
                    }
                }.runTaskLater(this.getMixerManager().getMixerBridge(), 1L);
            }

            if(applyCooldown) {
                final String spyMessage = this.getMixerManager().getMixerBridge().buildMessage(
                        this.getMixerManager().getMixerBridge().getConfig().getString("spy-message"))
                        .replace("<username>", f_clickers)
                        .replace("<button>", event.getControlInput().getControlID());
                Bukkit.getOnlinePlayers().forEach(player -> {
                    if(player.hasPermission("mixer.spy")) {
                        player.sendMessage(spyMessage);
                    }
                });
            }

            if(event.getTransaction() != null)
                event.getTransaction().capture(this.getGameClient()).complete(true);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Mixer] Spark transaction issue: " + e.getMessage());
        }
    }
}