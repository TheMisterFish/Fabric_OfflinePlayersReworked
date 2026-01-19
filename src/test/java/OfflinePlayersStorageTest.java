import com.offlineplayersreworked.storage.OfflinePlayersStorage;
import com.offlineplayersreworked.storage.model.OfflinePlayerModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OfflinePlayersStorageTest {

    private static String[] sampleActions() {
        return new String[] { "jump", "sit" };
    }

    @Test
    public void create_findAll_and_findByPlayerUUID_work() {
        OfflinePlayersStorage storage = new OfflinePlayersStorage();

        UUID offlineId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        storage.create(offlineId, playerId, sampleActions(), 1.0, 2.0, 3.0);

        List<OfflinePlayerModel> all = storage.findAll();
        assertEquals(1, all.size(), "There should be one stored offline player");

        OfflinePlayerModel found = storage.findByPlayerUUID(playerId);
        assertNotNull(found, "findByPlayerUUID should return the created model");
        assertEquals(offlineId, found.getId(), "IDs should match");
        assertArrayEquals(sampleActions(), found.getActions(), "Actions should match");
        assertEquals(1.0, found.getX(), 1e-9);
        assertEquals(2.0, found.getY(), 1e-9);
        assertEquals(3.0, found.getZ(), 1e-9);
    }

    @Test
    public void remove_marksDirty_and_removesEntry() {
        OfflinePlayersStorage storage = new OfflinePlayersStorage();

        UUID offlineId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        storage.create(offlineId, playerId, sampleActions(), 0, 0, 0);
        assertEquals(1, storage.findAll().size());

        storage.remove(offlineId);
        assertEquals(0, storage.findAll().size(), "remove should remove the entry by id");
    }

    @Test
    public void killByIdWithDeathMessage_updatesModelFields() {
        OfflinePlayersStorage storage = new OfflinePlayersStorage();

        UUID offlineId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        storage.create(offlineId, playerId, sampleActions(), 0.0, 0.0, 0.0);

        Vec3 pos = new Vec3(10.5, 64.0, -3.25);
        String deathMessage = "fell into the void";

        storage.killByIdWithDeathMessage(offlineId, pos, deathMessage);

        OfflinePlayerModel model = storage.findByPlayerUUID(playerId);
        assertNotNull(model);
        assertTrue(model.isDied(), "Model should be marked as died");
        assertEquals(deathMessage, model.getDeathMessage(), "Death message should be set");
        assertEquals(pos.x, model.getX(), 1e-9);
        assertEquals(pos.y, model.getY(), 1e-9);
        assertEquals(pos.z, model.getZ(), 1e-9);
    }

    @Test
    public void kicked_marksModelKick() {
        OfflinePlayersStorage storage = new OfflinePlayersStorage();

        UUID offlineId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        storage.create(offlineId, playerId, sampleActions(), 0.0, 0.0, 0.0);

        storage.kick(offlineId);

        OfflinePlayerModel model = storage.findByPlayerUUID(playerId);
        assertNotNull(model);
        assertTrue(model.isKicked(), "Model should be marked as kicked");
    }

    @Test
    public void save_and_load_roundtrip_preservesEntries() {
        OfflinePlayersStorage storage = new OfflinePlayersStorage();

        UUID offlineId1 = UUID.randomUUID();
        UUID playerId1 = UUID.randomUUID();
        storage.create(offlineId1, playerId1, new String[] { "a", "b" }, 1.1, 2.2, 3.3);

        UUID offlineId2 = UUID.randomUUID();
        UUID playerId2 = UUID.randomUUID();
        storage.create(offlineId2, playerId2, new String[] { "x" }, -1.0, -2.0, -3.0);

        storage.killByIdWithDeathMessage(offlineId1, new Vec3(5, 6, 7), "boom");
        storage.kick(offlineId2);

        CompoundTag tag = new CompoundTag();
        storage.save(tag, null); // provider is not used by save implementation

        assertTrue(tag.contains("OfflinePlayers"), "Saved tag should contain OfflinePlayers list");
        ListTag list = tag.getList("OfflinePlayers", 10);
        assertEquals(2, list.size(), "There should be two saved player entries");

        OfflinePlayersStorage loaded = OfflinePlayersStorage.load(tag, null);

        assertEquals(2, loaded.findAll().size(), "Loaded storage should contain two entries");

        OfflinePlayerModel loaded1 = loaded.findByPlayerUUID(playerId1);
        assertNotNull(loaded1);
        assertTrue(loaded1.isDied());
        assertEquals("boom", loaded1.getDeathMessage());
        assertEquals(5.0, loaded1.getX(), 1e-9);
        assertEquals(6.0, loaded1.getY(), 1e-9);
        assertEquals(7.0, loaded1.getZ(), 1e-9);

        OfflinePlayerModel loaded2 = loaded.findByPlayerUUID(playerId2);
        assertNotNull(loaded2);
        assertTrue(loaded2.isKicked());
        assertArrayEquals(new String[] { "x" }, loaded2.getActions());
        assertEquals(-1.0, loaded2.getX(), 1e-9);
        assertEquals(-2.0, loaded2.getY(), 1e-9);
        assertEquals(-3.0, loaded2.getZ(), 1e-9);
    }
}
