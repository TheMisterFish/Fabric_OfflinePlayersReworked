package com.misterfish.storage;

import com.misterfish.config.Config;
import com.misterfish.storage.model.OfflinePlayerModel;
import io.jsondb.InvalidJsonDbApiUsageException;
import io.jsondb.JsonDBTemplate;
import io.jsondb.query.Update;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static com.misterfish.OfflinePlayersReworked.MOD_ID;

public class OfflinePlayersReworkedStorage {

    private static final String DB_LOCATION = Config.databaseLocation;
    private static final String BASE_SCAN_PACKAGE = "com.misterfish.storage.model";
    private final JsonDBTemplate jsonDBTemplate = new JsonDBTemplate(DB_LOCATION, BASE_SCAN_PACKAGE);
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    public void init() {

        try {
            if (!jsonDBTemplate.collectionExists(OfflinePlayerModel.class)) {
                LOGGER.debug("Initializing Json DB.");
                jsonDBTemplate.createCollection(OfflinePlayerModel.class);
            } else {
                LOGGER.debug("Json DB already exists, reusing DB.");

            }
        } catch (InvalidJsonDbApiUsageException invalidJsonDbApiUsageException) {
            LOGGER.error(String.valueOf(invalidJsonDbApiUsageException));
        }
    }

    public List<OfflinePlayerModel> findAll() {
        return jsonDBTemplate.findAll(OfflinePlayerModel.class);
    }

    public void create(UUID offlinePlayerUUID, UUID playerUUID, String[] actions, double x, double y, double z) {
        OfflinePlayerModel offlinePlayer = new OfflinePlayerModel(offlinePlayerUUID, playerUUID, actions, x, y, z);
        jsonDBTemplate.upsert(offlinePlayer);
    }

    public void remove(UUID uuid) {
        OfflinePlayerModel modelToRemove = new OfflinePlayerModel(uuid);
        if (jsonDBTemplate.findById(uuid.toString(), OfflinePlayerModel.class) != null) {
            jsonDBTemplate.remove(modelToRemove, OfflinePlayerModel.class);
        }
    }

    public OfflinePlayerModel findByPlayerUUID(UUID uuid) {
        String jxQuery = String.format("/.[player='%s']", uuid);
        return jsonDBTemplate.findOne(jxQuery, OfflinePlayerModel.class);
    }

    public void killByIdWithDeathMessage(UUID uuid, Vec3 position, String deathMessage) {
        Update update = Update.update("died", true);
        update.set("deathMessage", deathMessage);
        update.set("x", position.x);
        update.set("y", position.y);
        update.set("z", position.z);

        String jxQuery = String.format("/.[id='%s']", uuid);
        jsonDBTemplate.findAndModify(jxQuery, update, "offlinePlayerModels");
    }

    public void kicked(UUID uuid){
        Update update = Update.update("kicked", true);

        String jxQuery = String.format("/.[id='%s']", uuid);
        jsonDBTemplate.findAndModify(jxQuery, update, "offlinePlayerModels");
    }
}
