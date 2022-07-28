package net.lilfish.offlineplayersreworked.storage;

import io.jsondb.InvalidJsonDbApiUsageException;
import io.jsondb.JsonDBTemplate;
import io.jsondb.query.Update;
import net.lilfish.offlineplayersreworked.npc.Npc;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.Text;
import net.lilfish.offlineplayersreworked.storage.models.NpcModel;
import net.minecraft.util.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static net.lilfish.offlineplayersreworked.OfflinePlayers.MOD_ID;

public class OfflineDatabase {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    //Actual location on disk for database files, process should have read-write permissions to this folder
    String dbFilesLocation = "./offlinedatabase/";

    //Java package name where POJO's are present
    String baseScanPackage = "net.lilfish.offlineplayersreworked.storage.models";

    public Items items = new Items();

    //Optionally a Cipher object if you need Encryption
    JsonDBTemplate jsonDBTemplate = new JsonDBTemplate(dbFilesLocation, baseScanPackage);
    public void init() {
        try {
            if(!jsonDBTemplate.collectionExists(NpcModel.class)) {
                LOGGER.info("Initializing Json DB.");
                jsonDBTemplate.createCollection(NpcModel.class);
            } else {
                LOGGER.info("Json DB already exists, reusing DB.");

            }
        } catch (InvalidJsonDbApiUsageException invalidJsonDbApiUsageException) {
            LOGGER.error(String.valueOf(invalidJsonDbApiUsageException));
        }
    }

    public List<NpcModel> getAllNPC() {
        return jsonDBTemplate.findAll(NpcModel.class);
    }

    public void addNPC(UUID player_id, UUID npc_id) {
        if (jsonDBTemplate.findById(player_id, NpcModel.class) != null) {
            this.removeNPC(player_id);
        }
        NpcModel npcModel = new NpcModel();
        npcModel.setId(player_id);
        npcModel.setNpc_id(npc_id);
        npcModel.setDead(false);
        jsonDBTemplate.insert(npcModel);
    }

    public void removeNPC(UUID uuid) {
        NpcModel npcModel = new NpcModel();
        npcModel.setId(uuid);
        jsonDBTemplate.remove(npcModel, NpcModel.class);
    }

    public NpcModel findNPCByPlayer(UUID uuid){
        String jxQuery = String.format("/.[id='%s']", uuid);
        return jsonDBTemplate.findOne(jxQuery, NpcModel.class);
    }

    public NpcModel findNPCByNPC(UUID uuid){
        String jxQuery = String.format("/.[npc_id='%s']", uuid);
        return jsonDBTemplate.findOne(jxQuery, NpcModel.class);
    }
    public void saveDeathNPC(Npc npc, Text deathMessage) {
        Collection<NpcModel.NPCItem> inventoryItemCollection = new ArrayList<NpcModel.NPCItem>();

        Update update = Update.update("dead", true);
        for(ItemStack npcItem : npc.getInventory().main){
            inventoryItemCollection.add(getNPCItem(npcItem));
        }
        update.set("inventory", inventoryItemCollection);

        update.set("armor_CHEST", getNPCItem(npc.getEquippedStack(EquipmentSlot.CHEST)));
        update.set("armor_LEGS", getNPCItem(npc.getEquippedStack(EquipmentSlot.LEGS)));
        update.set("armor_HEAD", getNPCItem(npc.getEquippedStack(EquipmentSlot.HEAD)));
        update.set("armor_FEET", getNPCItem(npc.getEquippedStack(EquipmentSlot.FEET)));
        update.set("offhand", getNPCItem(npc.getEquippedStack(EquipmentSlot.OFFHAND)));

        update.set("XPlevel", npc.experienceLevel);
        update.set("XPpoints", npc.getNextLevelExperience() * npc.experienceProgress);

        update.set("x", npc.getX());
        update.set("y", npc.getY());
        update.set("z", npc.getZ());
        update.set("deathMessage", deathMessage.getString());

        String jxQuery = String.format("/.[npc_id='%s']", npc.getUuid());
        jsonDBTemplate.findAndModify(jxQuery, update, "npcdata");
    }


    public PlayerInventory getNPCInventory(NpcModel npc)
    {
        PlayerInventory playerInventory = new PlayerInventory(null);
        ArrayList<NpcModel.NPCItem> inventory = npc.getInventory();
        for (int i = 0; i < playerInventory.main.size(); i++) {
            NpcModel.NPCItem npcItem = inventory.get(i);
            playerInventory.main.set(i, this.getItemStack(npcItem));
        }

        playerInventory.offHand.set(0, this.getItemStack(npc.getOffhand()));

        playerInventory.armor.set(0, this.getItemStack(npc.getArmor_FEET()));
        playerInventory.armor.set(1, this.getItemStack(npc.getArmor_LEGS()));
        playerInventory.armor.set(2, this.getItemStack(npc.getArmor_CHEST()));
        playerInventory.armor.set(3, this.getItemStack(npc.getArmor_HEAD()));
        return playerInventory;
    }

    private ItemStack getItemStack(NpcModel.NPCItem npcItem){
        try {
            var itemStack = new ItemStack(Registry.ITEM.get(npcItem.itemid), npcItem.count);
            if(npcItem.nbttag != null)
                itemStack.setNbt(StringNbtReader.parse(npcItem.nbttag));
            itemStack.setDamage(npcItem.damage);
            return itemStack;
        } catch (Exception ignore) {}
        return new ItemStack(Items.AIR, 1);
    }

    private NpcModel.NPCItem getNPCItem(ItemStack itemStack){
        NpcModel.NPCItem newItem = new NpcModel.NPCItem();
        newItem.itemid = Item.getRawId(itemStack.getItem());
        newItem.count = itemStack.getCount();
        if (itemStack.hasNbt() && itemStack.getNbt() != null)
            newItem.nbttag = itemStack.getNbt().asString();
        newItem.damage = itemStack.getDamage();
        return newItem;
    }
}
