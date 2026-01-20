package com.offlineplayersreworked.core;

import com.offlineplayersreworked.core.interfaces.ServerPlayerInterface;
import com.offlineplayersreworked.utils.ActionMapper;
import com.offlineplayersreworked.utils.Tracer;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.TestOnly;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class EntityPlayerActionPack {
    private final ServerPlayer player;
    private final Map<ActionType, Action> actions = new EnumMap<>(ActionType.class);
    private BlockPos currentBlock;
    private int blockHitDelay;
    private boolean isHittingBlock;
    private float curBlockDamageMP;
    private boolean sneaking;
    private boolean sprinting;
    private float forward;
    private float strafing;
    private int itemUseCooldown;

    public EntityPlayerActionPack(ServerPlayer playerIn) {
        player = playerIn;
        stopAll();
    }

    public void copyFrom(EntityPlayerActionPack other) {
        actions.putAll(other.actions);
        currentBlock = other.currentBlock;
        blockHitDelay = other.blockHitDelay;
        isHittingBlock = other.isHittingBlock;
        curBlockDamageMP = other.curBlockDamageMP;

        sneaking = other.sneaking;
        sprinting = other.sprinting;
        forward = other.forward;
        strafing = other.strafing;

        itemUseCooldown = other.itemUseCooldown;
    }

    public EntityPlayerActionPack start(ActionType type, Action action) {
        Action previous = actions.remove(type);
        if (previous != null) type.stop(player, previous);
        if (action != null) {
            actions.put(type, action);
            type.start(player, action); // noop
        }
        return this;
    }

    public EntityPlayerActionPack setSneaking(boolean doSneak) {
        sneaking = doSneak;
        player.setShiftKeyDown(doSneak);
        if (sprinting && sneaking)
            setSprinting(false);
        return this;
    }

    public EntityPlayerActionPack setSprinting(boolean doSprint) {
        sprinting = doSprint;
        player.setSprinting(doSprint);
        if (sneaking && sprinting)
            setSneaking(false);
        return this;
    }

    public EntityPlayerActionPack look(float yaw, float pitch) {
        player.setYRot(yaw % 360); //setYaw
        player.setXRot(Mth.clamp(pitch, -90, 90)); // setPitch
        return this;
    }

    public EntityPlayerActionPack stopMovement() {
        setSneaking(false);
        setSprinting(false);
        forward = 0.0F;
        strafing = 0.0F;
        return this;
    }


    public EntityPlayerActionPack stopAll() {
        for (ActionType type : actions.keySet()) type.stop(player, actions.get(type));
        actions.clear();
        return stopMovement();
    }

    public void onUpdate() {
        Map<ActionType, Boolean> actionAttempts = new HashMap<>();
        actions.values().removeIf(e -> e.done);
        for (Map.Entry<ActionType, Action> e : actions.entrySet()) {
            ActionType type = e.getKey();
            Action action = e.getValue();
            // skipping attack if use was successful
            if (!(actionAttempts.getOrDefault(ActionType.USE, false) && type == ActionType.ATTACK)) {
                Boolean actionStatus = action.tick(this, type);
                if (actionStatus != null)
                    actionAttempts.put(type, actionStatus);
            }
            // optionally retrying use after successful attack and unsuccessful use
            if (type == ActionType.ATTACK
                    && actionAttempts.getOrDefault(ActionType.ATTACK, false)
                    && !actionAttempts.getOrDefault(ActionType.USE, true)) {
                // according to MinecraftClient.handleInputEvents
                Action using = actions.get(ActionType.USE);
                if (using != null) // this is always true - we know use worked, but just in case
                {
                    using.retry(this, ActionType.USE);
                }
            }
        }

        Vec3 movement = Vec3.ZERO;
        if (actions.containsKey(ActionType.MOVE_FORWARD)) {
            movement = movement.add(player.getLookAngle().normalize());
        }
        if (actions.containsKey(ActionType.MOVE_BACKWARD)) {
            movement = movement.subtract(player.getLookAngle().normalize());
        }

        if (!movement.equals(Vec3.ZERO)) {
            double walkingSpeed = 0.175;
            movement = movement.normalize().scale(walkingSpeed);

            player.move(MoverType.PLAYER, movement);
        }

        float vel = sneaking ? 0.3F : 1.0F;
        // The != 0.0F checks are needed given else real players can't control minecarts, however it works with fakes and else they don't stop immediately
        if (forward != 0.0F || player instanceof OfflinePlayer) {
            player.zza = forward * vel;
        }
        if (strafing != 0.0F || player instanceof OfflinePlayer) {
            player.xxa = strafing * vel;
        }
    }

    static HitResult getTarget(ServerPlayer player) {
        double reach = player.gameMode.isCreative() ? 5 : 4.5f;
        return Tracer.rayTrace(player, 1, reach, false);
    }

    public enum ActionType {
        USE(true) {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                EntityPlayerActionPack ap = ((ServerPlayerInterface) player).getActionPack();
                if (ap.itemUseCooldown > 0) {
                    ap.itemUseCooldown--;
                    return true;
                }
                if (player.isUsingItem()) {
                    return true;
                }
                HitResult hit = getTarget(player);
                for (InteractionHand hand : InteractionHand.values()) {
                    switch (hit.getType()) {
                        case BLOCK -> {
                            player.resetLastActionTime();
                            ServerLevel world = player.level();
                            BlockHitResult blockHit = (BlockHitResult) hit;
                            BlockPos pos = blockHit.getBlockPos();
                            Direction side = blockHit.getDirection();
                            if (pos.getY() < player.level().getMaxY() - (side == Direction.UP ? 1 : 0) && world.mayInteract(player, pos)) {
                                InteractionResult result = player.gameMode.useItemOn(player, world, player.getItemInHand(hand), hand, blockHit);
                                if (result.consumesAction()) {
                                    ap.itemUseCooldown = 3;
                                    return true;
                                }
                            }
                        }
                        case ENTITY -> {
                            player.resetLastActionTime();
                            EntityHitResult entityHit = (EntityHitResult) hit;
                            Entity entity = entityHit.getEntity();
                            boolean handWasEmpty = player.getItemInHand(hand).isEmpty();
                            boolean itemFrameEmpty = (entity instanceof ItemFrame) && ((ItemFrame) entity).getItem().isEmpty();
                            Vec3 relativeHitPos = entityHit.getLocation().subtract(entity.getX(), entity.getY(), entity.getZ());
                            if (entity.interactAt(player, relativeHitPos, hand).consumesAction()) {
                                ap.itemUseCooldown = 3;
                                return true;
                            }
                            // fix for SS itemframe always returns CONSUME even if no action is performed
                            if (player.interactOn(entity, hand).consumesAction() && !(handWasEmpty && itemFrameEmpty)) {
                                ap.itemUseCooldown = 3;
                                return true;
                            }
                        }
                    }
                    ItemStack handItem = player.getItemInHand(hand);
                    if (player.gameMode.useItem(player, player.level(), handItem, hand).consumesAction()) {
                        ap.itemUseCooldown = 3;
                        return true;
                    }
                }
                return false;
            }

            @Override
            void inactiveTick(ServerPlayer player, Action action) {
                EntityPlayerActionPack ap = ((ServerPlayerInterface) player).getActionPack();
                ap.itemUseCooldown = 0;
                player.releaseUsingItem();
            }
        },
        ATTACK(true) {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                HitResult hit = getTarget(player);

                switch (hit.getType()) {
                    case ENTITY -> {
                        EntityHitResult entityHit = (EntityHitResult) hit;
                        Entity target = entityHit.getEntity();

                        LivingEntity living = (target instanceof LivingEntity le) ? le : null;
                        float beforeHealth = (living != null) ? living.getHealth() : Float.NaN;

                        if (!action.isContinuous) {
                            player.attack(target);
                            player.swing(InteractionHand.MAIN_HAND);
                        }

                        if (living != null && living.getHealth() != beforeHealth) {
                            player.resetAttackStrengthTicker();
                        }
                        player.resetLastActionTime();

                        return true;
                    }
                    case BLOCK -> {
                        EntityPlayerActionPack ap = ((ServerPlayerInterface) player).getActionPack();
                        if (ap.blockHitDelay > 0) {
                            ap.blockHitDelay--;
                            return false;
                        }
                        BlockHitResult blockHit = (BlockHitResult) hit;
                        BlockPos pos = blockHit.getBlockPos();
                        Direction side = blockHit.getDirection();
                        if (player.blockActionRestricted(player.level(), pos, player.gameMode.getGameModeForPlayer()))
                            return false;
                        if (ap.currentBlock != null && player.level().getBlockState(ap.currentBlock).isAir()) {
                            ap.currentBlock = null;
                            return false;
                        }
                        BlockState state = player.level().getBlockState(pos);
                        boolean blockBroken = false;
                        if (player.gameMode.getGameModeForPlayer().isCreative()) {
                            player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, side, player.level().getMaxY(), -1);
                            ap.blockHitDelay = 5;
                            blockBroken = true;
                        } else if (ap.currentBlock == null || !ap.currentBlock.equals(pos)) {
                            if (ap.currentBlock != null) {
                                player.gameMode.handleBlockBreakAction(ap.currentBlock, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, side, player.level().getMaxY(), -1);
                            }
                            player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, side, player.level().getMaxY(), -1);
                            boolean notAir = !state.isAir();
                            if (notAir && ap.curBlockDamageMP == 0) {
                                state.attack(player.level(), pos, player);
                            }
                            if (notAir && state.getDestroyProgress(player, player.level(), pos) >= 1) {
                                ap.currentBlock = null;
                                //instamine??
                                blockBroken = true;
                            } else {
                                ap.currentBlock = pos;
                                ap.curBlockDamageMP = 0;
                            }
                        } else {
                            ap.curBlockDamageMP += state.getDestroyProgress(player, player.level(), pos);
                            if (ap.curBlockDamageMP >= 1) {
                                player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, side, player.level().getMaxY(), -1);
                                ap.currentBlock = null;
                                ap.blockHitDelay = 5;
                                blockBroken = true;
                            }
                            player.level().destroyBlockProgress(-1, pos, (int) (ap.curBlockDamageMP * 10));

                        }
                        player.resetLastActionTime();
                        player.swing(InteractionHand.MAIN_HAND);
                        return blockBroken;
                    }
                }
                return false;
            }

            @Override
            void inactiveTick(ServerPlayer player, Action action) {
                EntityPlayerActionPack ap = ((ServerPlayerInterface) player).getActionPack();
                if (ap.currentBlock == null) return;
                player.level().destroyBlockProgress(-1, ap.currentBlock, -1);
                player.gameMode.handleBlockBreakAction(ap.currentBlock, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, Direction.DOWN, player.level().getMaxY(), -1);
                ap.currentBlock = null;
            }
        },
        JUMP(true) {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                if (player.onGround()) {
                    player.jumpFromGround(); // onGround
                    player.setJumping(true);
                }
                return false;
            }

            @Override
            void inactiveTick(ServerPlayer player, Action action) {
                player.setJumping(false);
            }
        },
        CROUCH(true) {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                player.setShiftKeyDown(true);
                player.resetLastActionTime();
                return false;
            }
        },
        EAT(true) {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                ItemStack mainHandItemStack = player.getMainHandItem();
                ItemStack offHandItemStack = player.getOffhandItem();

                if (canConsumeItem(player, mainHandItemStack)) {
                    if (!player.isUsingItem()) {
                        player.startUsingItem(InteractionHand.MAIN_HAND);
                    }
                } else if (canConsumeItem(player, offHandItemStack)) {
                    if (!player.isUsingItem()) {
                        player.startUsingItem(InteractionHand.OFF_HAND);
                    }
                }

                player.resetLastActionTime();
                return false;
            }
        },
        DROP_ITEM(true) {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                player.resetLastActionTime();
                player.drop(false); // dropSelectedItem
                return false;
            }
        },
        DROP_STACK(true) {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                player.resetLastActionTime();
                player.drop(true); // dropSelectedItem
                return false;
            }
        },
        MOVE_FORWARD(true) {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                return false;
            }
        },
        MOVE_BACKWARD(true) {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                return false;
            }
        },
        DISCONNECT(false) {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                Component reason = Component.literal(player.getName().getString() + " automatically disconnected.");
                player.connection.onDisconnect(new DisconnectionDetails(reason));
                player.disconnect();
                return true;
            }
        };

        public final boolean preventSpectator;

        ActionType(boolean preventSpectator) {
            this.preventSpectator = preventSpectator;
        }

        void start(ServerPlayer player, Action action) {
        }

        abstract boolean execute(ServerPlayer player, Action action);

        void inactiveTick(ServerPlayer player, Action action) {
        }

        void stop(ServerPlayer player, Action action) {
            inactiveTick(player, action);
        }

        private static boolean canConsumeItem(ServerPlayer player, ItemStack itemStack) {
            if (itemStack.isEmpty()) {
                return false;
            }

            Item item = itemStack.getItem();

            if (isAlwaysConsumableItem(item, player)) {
                return true;
            }

            FoodProperties foodProperties = itemStack.get(DataComponents.FOOD);
            if (foodProperties != null) {
                return player.canEat(foodProperties.canAlwaysEat());
            }

            return false;
        }

        private static boolean isAlwaysConsumableItem(Item item, ServerPlayer player) {
            if (item instanceof PotionItem && !(item instanceof SplashPotionItem)) {
                return true;
            }
            if (item.toString().equals("minecraft:ominous_bottle")) {
                return !player.hasEffect(MobEffects.BAD_OMEN) && !player.hasEffect(MobEffects.RAID_OMEN);
            }
            return item.toString().equals("minecraft:honey_bottle") || item.toString().equals("minecraft:milk_bucket");
        }

    }

    public static class Action {
        public boolean done = false;
        public final int limit;
        public final int interval;
        public final int offset;
        private int count;
        private int next;
        private final boolean isContinuous;
        public boolean isBreakAction;

        private Action(int limit, int interval, int offset, boolean continuous) {
            this.limit = limit;
            this.interval = interval;
            this.offset = offset;
            next = interval + offset;
            isContinuous = continuous;
            isBreakAction = false;
        }

        public static Action once() {
            return new Action(1, 1, 0, false);
        }

        public static Action continuous() {
            return new Action(-1, 1, 0, true);
        }

        public static Action interval(int interval) {
            return new Action(-1, interval, 0, false);
        }

        public static Action interval(int interval, int offset) {
            return new Action(-1, interval, offset, false);
        }

        Boolean tick(EntityPlayerActionPack actionPack, ActionType type) {
            next--;
            Boolean cancel = null;
            if (next <= 0) {
                if (interval == 1 && !isContinuous) {
                    // need to allow entity to tick, otherwise won't have effect (bow)
                    // actions are 20 tps, so need to clear status mid tick, allowing entities process it till next time
                    if (!type.preventSpectator || !actionPack.player.isSpectator()) {
                        type.inactiveTick(actionPack.player, this);
                    }
                }

                if (!type.preventSpectator || !actionPack.player.isSpectator()) {
                    cancel = type.execute(actionPack.player, this);
                }
                count++;

                // If it's not a break action OR if the block was successfully broken set next as interval.
                // Otherwise (it's a break action and the block wasn't broken), set next to 0.
                next = (!isBreakAction || (cancel != null && cancel)) ? interval : 0;

                if (count == limit && next == interval) {
                    type.stop(actionPack.player, null);
                    done = true;
                }
            } else {
                if (!type.preventSpectator || !actionPack.player.isSpectator()) {
                    type.inactiveTick(actionPack.player, this);
                }
            }

            return cancel;
        }

        void retry(EntityPlayerActionPack actionPack, ActionType type) {
            //assuming action run but was unsuccessful that tick, but opportunity emerged to retry it, lets retry it.
            if (!type.preventSpectator || !actionPack.player.isSpectator()) {
                type.execute(actionPack.player, this);
            }
            count++;
            if (count == limit) {
                type.stop(actionPack.player, null);
                done = true;
            }
        }
    }

    public static Pair<ActionType, Action> getActionPair(String actionString, int intervalInteger, int offsetInteger) {
        Action actionInterval = Action.once();

        if (intervalInteger > 0) {
            if (offsetInteger > 0) {
                actionInterval = Action.interval(intervalInteger, offsetInteger);
            } else {
                actionInterval = Action.interval(intervalInteger);
            }
        } else if (intervalInteger == 0) {
            actionInterval = Action.continuous();
        }

        ActionType actionType = ActionMapper.getActionType(actionString);
        actionInterval.isBreakAction = actionString.equalsIgnoreCase("break");

        return Pair.of(actionType, actionInterval);
    }

    @TestOnly
    public Map<ActionType, Action> getActions() {
        return actions;
    }
}
