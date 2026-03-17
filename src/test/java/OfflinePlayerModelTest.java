import com.offlineplayersreworked.storage.model.OfflinePlayerModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OfflinePlayerModelTest {

    @Test
    public void toTag_and_fromTag_roundtrip_preservesAllFields() {
        UUID id = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        String[] actions = new String[]{"jump", "sit", "wave"};
        double x = 12.34;
        double y = 56.78;
        double z = -9.01;

        OfflinePlayerModel original = new OfflinePlayerModel(id, player, List.of(actions), x, y, z);
        original.setDied(true);
        original.setDeathMessage("fell into void");
        original.setKicked(true);

        CompoundTag tag = original.toTag();
        OfflinePlayerModel restored = OfflinePlayerModel.fromTag(tag);

        assertEquals(original.getId(), restored.getId(), "id should roundtrip");
        assertEquals(original.getPlayer(), restored.getPlayer(), "player UUID should roundtrip");

        assertArrayEquals(original.getActions().toArray(new String[0]), restored.getActions().toArray(new String[0]), "actions should roundtrip");

        assertEquals(original.getX(), restored.getX(), 1e-9, "x should roundtrip");
        assertEquals(original.getY(), restored.getY(), 1e-9, "y should roundtrip");
        assertEquals(original.getZ(), restored.getZ(), 1e-9, "z should roundtrip");

        assertTrue(restored.isDied(), "died flag should roundtrip");
        assertEquals("fell into void", restored.getDeathMessage(), "deathMessage should roundtrip");
        assertTrue(restored.isKicked(), "kicked flag should roundtrip");
    }

    @Test
    public void fromTag_handlesEmptyActionsList() {
        UUID id = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        String[] actions = new String[0];
        OfflinePlayerModel original = new OfflinePlayerModel(id, player, List.of(actions), 0.0, 0.0, 0.0);

        CompoundTag tag = original.toTag();

        ListTag actionsList = tag.getList("actions").orElseThrow(NullPointerException::new);
        assertEquals(0, actionsList.size(), "actions list should be empty in tag");

        OfflinePlayerModel restored = OfflinePlayerModel.fromTag(tag);
        assertNotNull(restored.getActions(), "actions array should not be null after fromTag");
        assertEquals(0, restored.getActions().size(), "actions array should be empty after fromTag");
    }
}
