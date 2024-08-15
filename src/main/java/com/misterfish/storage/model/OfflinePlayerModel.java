package com.misterfish.storage.model;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * offlinePlayer DB model
 *
 * @version 1.0 08-08-2024
 */
@Document(collection = "offlinePlayerModels", schemaVersion = "1.0")
@RequiredArgsConstructor
public class OfflinePlayerModel {
    @Id
    private UUID id;
    private UUID player;
    private String[] actions;
    private boolean kicked = false;
    private boolean died = false;
    private String deathMessage;

    private double x;
    private double y;
    private double z;

    public OfflinePlayerModel(UUID id) {
        this.id = id;
    }

    public OfflinePlayerModel(UUID id, UUID player, String[] actions, double x, double y, double z) {
        this.id = id;
        this.player = player;
        this.actions = actions;
        this.died = false;
        this.deathMessage = "";
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPlayer() {
        return player;
    }

    public void setPlayer(UUID player) {
        this.player = player;
    }

    public String[] getActions() {
        return actions;
    }

    public void setActions(String[] actions) {
        this.actions = actions;
    }

    public String getDeathMessage() {
        return deathMessage;
    }

    public void setDeathMessage(String deathMessage) {
        this.deathMessage = deathMessage;
    }

    public boolean isKicked() {
        return kicked;
    }

    public void setKicked(boolean kicked) {
        this.kicked = kicked;
    }

    public boolean getDied() {
        return died;
    }

    public void setDied(boolean died) {
        this.died = died;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
