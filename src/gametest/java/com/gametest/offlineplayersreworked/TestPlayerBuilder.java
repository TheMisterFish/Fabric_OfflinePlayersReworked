package com.gametest.offlineplayersreworked;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Consumer;

import static net.minecraft.world.level.Level.OVERWORLD;

public class TestPlayerBuilder {
    public UUID uuid = UUID.randomUUID();
    public String name = "TestPlayer";
    public GameProfile gameProfile = new GameProfile(uuid, name);
    public TestClientConnection connection = new TestClientConnection(PacketFlow.SERVERBOUND);
    public CommonListenerCookie cookie = new CommonListenerCookie(gameProfile, 0, ClientInformation.createDefault(), false);
    public ResourceKey<Level> dimension = OVERWORLD;
    public List<Consumer<FakePlayer>> inventoryOps = new ArrayList<>();
    public int randomItemCount = 10; // default amount
    public List<MobEffectInstance> effects = new ArrayList<>();
    public Map<Integer, Map<Holder<Enchantment>, Integer>> enchantments = new HashMap<>();
    public Float health = 20.0f;
    public Integer food = 20;
    public GameType gamemode = GameType.SURVIVAL;

    public TestPlayerBuilder setUuid(UUID uuid) {
        this.uuid = uuid;
        this.gameProfile = new GameProfile(uuid, this.name);
        this.cookie = new CommonListenerCookie(gameProfile, 0, ClientInformation.createDefault(), false);
        return this;
    }

    public TestPlayerBuilder setGameProfile(GameProfile gameProfile) {
        this.gameProfile = gameProfile;
        this.cookie = new CommonListenerCookie(gameProfile, 0, ClientInformation.createDefault(), false);
        return this;
    }

    public TestPlayerBuilder setName(String name) {
        this.name = name;
        this.gameProfile = new GameProfile(this.uuid, name);
        this.cookie = new CommonListenerCookie(gameProfile, 0, ClientInformation.createDefault(), false);
        return this;
    }

    public TestPlayerBuilder setConnection(TestClientConnection connection) {
        this.connection = connection;
        return this;
    }

    public TestPlayerBuilder setCookie(CommonListenerCookie cookie) {
        this.cookie = cookie;
        return this;
    }

    public TestPlayerBuilder setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
        return this;
    }

    public TestPlayerBuilder addItem(int slot, ItemStack stack) {
        inventoryOps.add(player -> player.getInventory().setItem(slot, stack));
        return this;
    }

    public TestPlayerBuilder setInventoryOps(List<Consumer<FakePlayer>> inventoryOps) {
        this.inventoryOps = inventoryOps;
        return this;
    }

    public TestPlayerBuilder generateRandomInventory() {
        inventoryOps.add(player -> {
            RandomSource random = RandomSource.create();

            List<Item> allItems = BuiltInRegistries.ITEM.stream()
                    .filter(i -> i != Items.AIR && i.getDefaultMaxStackSize() > 0)
                    .toList();

            if (allItems.isEmpty()) return;

            Inventory inv = player.getInventory();
            int containerSize = inv.getContainerSize();

            for (int i = 0; i < randomItemCount; i++) {
                int slot = random.nextInt(containerSize);
                Item item = allItems.get(random.nextInt(allItems.size()));
                int max = Math.max(1, item.getDefaultMaxStackSize());
                int count = 1 + random.nextInt(max); // 1..max
                inv.setItem(slot, new ItemStack(item, count));
            }
        });
        return this;
    }

    public TestPlayerBuilder setRandomItemCount(int count) {
        this.randomItemCount = count;
        return this;
    }

    public TestPlayerBuilder randomArmorAndWeapons() {
        inventoryOps.add(player -> {
            RandomSource random = RandomSource.create();

            List<Item> helmets = List.of(
                    Items.DIAMOND_HELMET, Items.NETHERITE_HELMET,
                    Items.IRON_HELMET, Items.GOLDEN_HELMET,
                    Items.CHAINMAIL_HELMET, Items.LEATHER_HELMET
            );

            List<Item> chestplates = List.of(
                    Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE,
                    Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE,
                    Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE
            );

            List<Item> leggings = List.of(
                    Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS,
                    Items.IRON_LEGGINGS, Items.GOLDEN_LEGGINGS,
                    Items.CHAINMAIL_LEGGINGS, Items.LEATHER_LEGGINGS
            );

            List<Item> boots = List.of(
                    Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS,
                    Items.IRON_BOOTS, Items.GOLDEN_BOOTS,
                    Items.CHAINMAIL_BOOTS, Items.LEATHER_BOOTS
            );

            List<Item> weapons = List.of(
                    Items.DIAMOND_SWORD, Items.NETHERITE_SWORD,
                    Items.IRON_SWORD, Items.GOLDEN_SWORD,
                    Items.STONE_SWORD, Items.WOODEN_SWORD,
                    Items.BOW, Items.CROSSBOW, Items.TRIDENT,
                    Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE
            );

            List<Item> offhandItems = List.of(
                    Items.SHIELD, Items.TORCH, Items.TOTEM_OF_UNDYING,
                    Items.GOLDEN_APPLE, Items.ENDER_PEARL
            );

            Inventory inv = player.getInventory();

            inv.armor.set(3, new ItemStack(helmets.get(random.nextInt(helmets.size()))));
            inv.armor.set(2, new ItemStack(chestplates.get(random.nextInt(chestplates.size()))));
            inv.armor.set(1, new ItemStack(leggings.get(random.nextInt(leggings.size()))));
            inv.armor.set(0, new ItemStack(boots.get(random.nextInt(boots.size()))));

            int selected = Math.max(0, Math.min(inv.getContainerSize() - 1, inv.selected));
            inv.setItem(selected, new ItemStack(weapons.get(random.nextInt(weapons.size()))));

            inv.offhand.set(0, new ItemStack(offhandItems.get(random.nextInt(offhandItems.size()))));
        });

        return this;
    }


    public TestPlayerBuilder addEffect(Holder<MobEffect> effect, int duration, int amplifier) {
        effects.add(new MobEffectInstance(effect, duration, amplifier));
        return this;
    }

    public TestPlayerBuilder addEffect(MobEffectInstance instance) {
        effects.add(instance);
        return this;
    }

    public TestPlayerBuilder setEffects(List<MobEffectInstance> effects) {
        this.effects = effects;
        return this;
    }

    public TestPlayerBuilder enchantItem(int slot, Holder<Enchantment> enchantment, int level) {
        enchantments
                .computeIfAbsent(slot, s -> new HashMap<>())
                .put(enchantment, level);
        return this;
    }

    public TestPlayerBuilder setEnchantments(Map<Integer, Map<Holder<Enchantment>, Integer>> enchantments) {
        this.enchantments = enchantments;
        return this;
    }

    public TestPlayerBuilder setHealth(Float health) {
        this.health = health;
        return this;
    }

    public TestPlayerBuilder setFood(Integer food) {
        this.food = food;
        return this;
    }

    public TestPlayerBuilder setGamemode(GameType gamemode) {
        this.gamemode = gamemode;
        return this;
    }

    public FakePlayer build(MinecraftServer server) {
        FakePlayer fake = FakePlayer.get(Objects.requireNonNull(server.getLevel(dimension)), gameProfile);
        fake.connection = new ServerGamePacketListenerImpl(server, connection, fake, cookie);
        applyInventory(fake);
        applyEffects(fake);
        applyEnchantments(fake);

        fake.setHealth(health);
        fake.getFoodData().setFoodLevel(food);
        fake.setGameMode(gamemode);

        return fake;
    }

    public FakePlayer place(MinecraftServer server) {
        FakePlayer fake = this.build(server);

        server.getPlayerList().placeNewPlayer(connection, fake, cookie);

        return fake;
    }

    private void applyInventory(FakePlayer fake) {
        for (var op : inventoryOps) op.accept(fake);
    }

    private void applyEffects(FakePlayer fake) {
        for (MobEffectInstance effect : effects) {
            fake.addEffect(effect);
        }
    }

    private void applyEnchantments(FakePlayer fake) {
        Inventory inv = fake.getInventory();

        for (var entry : enchantments.entrySet()) {
            int slot = entry.getKey();
            ItemStack stack = inv.getItem(slot);

            if (!stack.isEmpty()) {
                for (var ench : entry.getValue().entrySet()) {
                    stack.enchant(ench.getKey(), ench.getValue());
                }
            }
        }
    }

    public static class TestClientConnection extends Connection {
        public TestClientConnection(PacketFlow p) {
            super(p);
            EmbeddedChannel ch = new EmbeddedChannel();
            ch.pipeline().addLast("packet_handler", this);
            ch.pipeline().fireChannelActive();
        }
    }
}
