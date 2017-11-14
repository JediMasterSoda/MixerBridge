package me.redraskal.mixerbridge;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) Redraskal 2017.
 * <p>
 * Please do not copy the code below unless you
 * have permission to do so from me.
 */
public class TeamworkButton {

    @Getter private final int clicks_needed;
    private int clicks = 0;
    private List<String> participants = new ArrayList<>();

    public TeamworkButton(int clicks_needed) {
        this.clicks_needed = clicks_needed;
    }

    public boolean addClick(String username) {
        if(!participants.contains(username)) participants.add(username);
        clicks++;
        if(clicks != clicks_needed) return false;
        return true;
    }

    public float getPercentageFilled() {
        return ((float) clicks / (float) clicks_needed);
    }

    public String reset() {
        String result = "";

        for(String participant : participants) {
            if(!result.isEmpty()) result+=",";
            result+=participant;
        }

        participants.clear();
        clicks = 0;
        return result;
    }
}
