package me.redraskal.mixerbridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import me.redraskal.mixerbridge.manager.MixerManager;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Copyright (c) Redraskal 2017.
 * <p>
 * Please do not copy the code below unless you
 * have permission to do so from me.
 */
public class MixerBridge extends JavaPlugin {

    @Getter private MixerManager mixerManager;
    private JsonParser jsonParser;

    public void onEnable() {
        this.saveDefaultConfig();
        this.jsonParser = new JsonParser();

        this.mixerManager = new MixerManager(this);

        this.getCommand("mixer").setExecutor(new MixerCommand(this));
    }

    public String buildMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message
                        .replace("<prefix>",
                                this.getConfig().getString("prefix")));
    }

    public JsonObject fetchJSON(String address) throws IOException {
        URL url = new URL(address);
        HttpsURLConnection request = (HttpsURLConnection) url.openConnection();
        request.setRequestProperty("User-Agent", "Minecraft");

        return jsonParser.parse(new InputStreamReader(request.getInputStream()))
                .getAsJsonObject();
    }
}