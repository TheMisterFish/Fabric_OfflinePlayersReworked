package com.misterfish.storage.model;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

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
    @Getter
    @Setter
    private UUID id;
    @Getter
    @Setter
    private UUID player;
    @Getter
    @Setter
    private String[] actions;
    @Getter
    @Setter
    private boolean kicked = false;
    @Getter
    @Setter
    private boolean died = false;
    @Getter
    @Setter
    private String deathMessage;
    @Getter
    @Setter
    private double x;
    @Getter
    @Setter
    private double y;
    @Getter
    @Setter
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
}
