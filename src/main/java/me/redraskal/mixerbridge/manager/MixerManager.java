package me.redraskal.mixerbridge.manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import com.mixer.interactive.GameClient;
import lombok.Getter;
import me.redraskal.mixerbridge.MixerBridge;
import me.redraskal.mixerbridge.listener.GameClientListener;
import me.redraskal.mixerbridge.listener.MixerManagerListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Copyright (c) Redraskal 2017.
 * <p>
 * Please do not copy the code below unless you
 * have permission to do so from me.
 */
public class MixerManager {

    @Getter private final MixerBridge mixerBridge;
    @Getter private final MixerManagerListener mixerManagerListener;

    private Map<UUID, GameClient> gameClientMap = new HashMap<>();
    private Map<UUID, GameClientListener> gameClientListenerMap = new HashMap<>();
    private final Cache<UUID, JsonObject> shortCodeCache = CacheBuilder.newBuilder()
            .expireAfterWrite(120, TimeUnit.SECONDS)
            .build();
    private final Cache<UUID, JsonObject> accessTokenCache = CacheBuilder.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .build();

    public MixerManager(MixerBridge mixerBridge) {
        this.mixerBridge = mixerBridge;
        this.mixerManagerListener = new MixerManagerListener(this);

        this.getMixerBridge().getServer().getPluginManager()
                .registerEvents(this.getMixerManagerListener(),
                        this.getMixerBridge());
    }

    public GameClient getGameClient(UUID uuid) {
        if(!gameClientMap.containsKey(uuid)) return null;
        return gameClientMap.get(uuid);
    }

    public GameClient createGameClient(UUID uuid, String access_token,
                                       int versionID, String shareCode)
            throws ExecutionException, InterruptedException {
        GameClient gameClient = new GameClient(versionID);
        if(!gameClient.connect(access_token, shareCode).get()) return null;
        GameClientListener gameClientListener = new GameClientListener(this, uuid, gameClient);
        gameClient.getEventBus().register(gameClientListener);
        gameClientListenerMap.put(uuid, gameClientListener);
        gameClientMap.put(uuid, gameClient);
        return gameClient;
    }

    public void removeGameClient(UUID uuid) throws ExecutionException, InterruptedException {
        if(!gameClientMap.containsKey(uuid)) return;
        if(gameClientMap.get(uuid).isConnected())
            gameClientMap.get(uuid).disconnect().get();
        gameClientMap.remove(uuid);
        gameClientListenerMap.remove(uuid);
    }

    public String requestShortCode(UUID uuid) throws IOException {
        JsonObject shortCodeObject = shortCodeCache.getIfPresent(uuid);
        if(shortCodeObject != null) return shortCodeObject.get("code").getAsString();

        JsonObject jsonObject = this.getMixerBridge()
                .fetchJSON("https://api.frostedmc.com/v1/mixer");

        if(jsonObject == null) return "";
        if(!jsonObject.get("success").getAsBoolean()) return "";

        jsonObject = jsonObject.get("response").getAsJsonObject();
        shortCodeCache.put(uuid, jsonObject);

        return jsonObject.get("code").getAsString();
    }

    public String requestAccessToken(UUID uuid) throws IOException {
        JsonObject shortCodeObject = shortCodeCache.getIfPresent(uuid);
        if(shortCodeObject == null) return "";

        JsonObject accessTokenObject = accessTokenCache.getIfPresent(uuid);
        if(accessTokenObject != null) return accessTokenObject.get("access_token").getAsString();

        JsonObject jsonObject = this.getMixerBridge()
                .fetchJSON("https://api.frostedmc.com/v1/mixer/"
                        + shortCodeObject.get("handle").getAsString());

        if(jsonObject == null) return "";
        if(!jsonObject.get("success").getAsBoolean()) return "";

        jsonObject = jsonObject.get("response").getAsJsonObject();
        accessTokenCache.put(uuid, jsonObject);

        return jsonObject.get("access_token").getAsString();
    }
}