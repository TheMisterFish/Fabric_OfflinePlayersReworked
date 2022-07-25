package net.lilfish.offlineplayersreworked.storage;

import io.jsondb.InvalidJsonDbApiUsageException;
import io.jsondb.JsonDBTemplate;
import io.jsondb.query.Update;
import net.lilfish.offlineplayersreworked.npc.NPCClass;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.Text;
import net.lilfish.offlineplayersreworked.storage.models.NPCModel;
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
            if(!jsonDBTemplate.collectionExists(NPCModel.class)) {
                LOGGER.info("Initializing Json DB.");
                jsonDBTemplate.createCollection(NPCModel.class);
            } else {
                LOGGER.info("Json DB already exists, reusing DB.");

            }
        } catch (InvalidJsonDbApiUsageException invalidJsonDbApiUsageException) {
            LOGGER.error(String.valueOf(invalidJsonDbApiUsageException));
        }
    }

    public List<NPCModel> getAllNPC() {
        return jsonDBTemplate.findAll(NPCModel.class);
    }

    public void addNPC(UUID player_id, UUID npc_id) {
        if (jsonDBTemplate.findById(player_id, NPCModel.class) != null) {
            this.removeNPC(player_id);
        }
        NPCModel instance = new NPCModel();
        instance.setId(player_id);
        instance.setNpc_id(npc_id);
        instance.setDead(false);
        jsonDBTemplate.insert(instance);
    }

    public void removeNPC(UUID uuid) {
        NPCModel instance = new NPCModel();
        instance.setId(uuid);
        jsonDBTemplate.remove(instance, NPCModel.class);
    }

    public NPCModel findNPCByPlayer(UUID uuid){
        String jxQuery = String.format("/.[id='%s']", uuid);
        return jsonDBTemplate.findOne(jxQuery, NPCModel.class);
    }

    public NPCModel findNPCByNPC(UUID uuid){
        String jxQuery = String.format("/.[npc_id='%s']", uuid);
        return jsonDBTemplate.findOne(jxQuery, NPCModel.class);
    }
    public void saveDeathNPC(NPCClass npc, Text deathMessage) {
//      Seat Dead
        Collection<NPCModel.NPCItem> inventoryItemCollection = new ArrayList<NPCModel.NPCItem>();
//      Set inventory
        Update update = Update.update("dead", true);
        for(ItemStack npcItem : npc.getInventory().main){
            inventoryItemCollection.add(getNPCItem(npcItem));
        }
        update.set("inventory", inventoryItemCollection);

//      Set Armor and offhand
        update.set("armor_CHEST", getNPCItem(npc.getEquippedStack(EquipmentSlot.CHEST)));
        update.set("armor_LEGS", getNPCItem(npc.getEquippedStack(EquipmentSlot.LEGS)));
        update.set("armor_HEAD", getNPCItem(npc.getEquippedStack(EquipmentSlot.HEAD)));
        update.set("armor_FEET", getNPCItem(npc.getEquippedStack(EquipmentSlot.FEET)));
        update.set("offhand", getNPCItem(npc.getEquippedStack(EquipmentSlot.OFFHAND)));

//      Set XP
        update.set("XPlevel", npc.experienceLevel);
        update.set("XPpoints", npc.getNextLevelExperience() * npc.experienceProgress);
//      Set XYZ
        update.set("x", npc.getX());
        update.set("y", npc.getY());
        update.set("z", npc.getZ());
        update.set("deathMessage", deathMessage.getString());

        String jxQuery = String.format("/.[npc_id='%s']", npc.getUuid());
        jsonDBTemplate.findAndModify(jxQuery, update, "npcdata");
    }


    public PlayerInventory getNPCInventory(NPCModel npc)
    {

        PlayerInventory inv = new PlayerInventory(null);
        ArrayList<NPCModel.NPCItem> inventory = npc.getInventory();
        for (int i = 0; i < inv.main.size(); i++) {
            NPCModel.NPCItem npcItem = inventory.get(i);
            inv.main.set(i, this.getItemStack(npcItem));
        }
        //set offhand
        inv.offHand.set(0, this.getItemStack(npc.getOffhand()));
        //set armor
        inv.armor.set(0, this.getItemStack(npc.getArmor_FEET()));
        inv.armor.set(1, this.getItemStack(npc.getArmor_LEGS()));
        inv.armor.set(2, this.getItemStack(npc.getArmor_CHEST()));
        inv.armor.set(3, this.getItemStack(npc.getArmor_HEAD()));
        return inv;
    }

    private ItemStack getItemStack(NPCModel.NPCItem npcItem){
        try {
            var itemStack = new ItemStack(Registry.ITEM.get(npcItem.itemid), npcItem.count);
            if(npcItem.nbttag != null){
                NbtCompound tags = StringNbtReader.parse(npcItem.nbttag);
                itemStack.setNbt(tags);
            }
            return itemStack;
        } catch (Exception ignore) {}
        return new ItemStack(Items.AIR, 1);
    }

    private NPCModel.NPCItem getNPCItem(ItemStack itemStack){
        NPCModel.NPCItem newItem = new NPCModel.NPCItem();
        newItem.itemid = Item.getRawId(itemStack.getItem());
        newItem.count = itemStack.getCount();
        if (itemStack.hasNbt() && itemStack.getNbt() != null)
            newItem.nbttag = itemStack.getNbt().asString();
        return newItem;
    }
}
