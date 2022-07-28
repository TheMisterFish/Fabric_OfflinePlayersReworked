package net.lilfish.offlineplayersreworked;


import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import net.lilfish.offlineplayersreworked.interfaces.ServerPlayerEntityInterface;
import net.lilfish.offlineplayersreworked.npc.Tracer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;

public class EntityPlayerActionPack
{
    private final ServerPlayerEntity player;

    private final Map<ActionType, Action> actions = new TreeMap<>();

    private BlockPos currentBlock;
    private int blockHitDelay;
    private float curBlockDamageMP;

    private boolean sneaking;
    private boolean sprinting;
    private float forward;
    private float strafing;
    private int itemUseCooldown;

    public EntityPlayerActionPack(ServerPlayerEntity playerIn)
    {
        player = playerIn;
        stopAll();
    }

    public EntityPlayerActionPack start(ActionType type, Action action)
    {
        Action previous = actions.remove(type);
        if (previous != null) type.stop(player, previous);
        if (action != null)
        {
            actions.put(type, action);
            type.start(player, action); // noop
        }
        return this;
    }

    public EntityPlayerActionPack setSneaking(boolean doSneak)
    {
        sneaking = doSneak;
        player.setSneaking(doSneak);
        if (sprinting && sneaking)
            setSprinting(false);
        return this;
    }
    public EntityPlayerActionPack setSprinting(boolean doSprint)
    {
        sprinting = doSprint;
        player.setSprinting(doSprint);
        if (sneaking && sprinting)
            setSneaking(false);
        return this;
    }

    public EntityPlayerActionPack stopMovement()
    {
        setSneaking(false);
        setSprinting(false);
        forward = 0.0F;
        strafing = 0.0F;
        return this;
    }


    public EntityPlayerActionPack stopAll()
    {
        for (ActionType type : actions.keySet()) type.stop(player, actions.get(type));
        actions.clear();
        return stopMovement();
    }

    public void onUpdate()
    {
        Map<ActionType, Boolean> actionAttempts = new HashMap<>();
        actions.entrySet().removeIf((e) -> e.getValue().done);
        for (Map.Entry<ActionType, Action> e : actions.entrySet())
        {
            Action action = e.getValue();
            // skipping attack if use was successful
            if (!(actionAttempts.getOrDefault(ActionType.USE, false) && e.getKey() == ActionType.ATTACK))
            {
                Boolean actionStatus = action.tick(this, e.getKey());
                if (actionStatus != null)
                    actionAttempts.put(e.getKey(), actionStatus);
            }
            // optionally retrying use after successful attack and unsuccessful use
            if ( e.getKey() == ActionType.ATTACK
                    && actionAttempts.getOrDefault(ActionType.ATTACK, false)
                    && !actionAttempts.getOrDefault(ActionType.USE, true) )
            {
                // according to MinecraftClient.handleInputEvents
                Action using = actions.get(ActionType.USE);
                if (using != null) // this is always true - we know use worked, but just in case
                {
                    using.retry(this);
                }
            }
        }
        if (forward != 0.0F)
        {
            player.forwardSpeed = forward*(sneaking?0.3F:1.0F);
        }
        if (strafing != 0.0F)
        {
            player.sidewaysSpeed = strafing*(sneaking?0.3F:1.0F);
        }
    }

    static HitResult getTarget(ServerPlayerEntity player)
    {
        double reach = player.interactionManager.isCreative() ? 5 : 4.5f;
        return Tracer.rayTrace(player, 1, reach, false);
    }

    public enum ActionType
    {
        USE(true)
                {
                    @Override
                    boolean execute(ServerPlayerEntity player, Action action)
                    {
                        EntityPlayerActionPack ap = ((ServerPlayerEntityInterface) player).getActionPack();
                        if (ap.itemUseCooldown > 0)
                        {
                            ap.itemUseCooldown--;
                            return true;
                        }
                        if (player.isUsingItem())
                        {
                            return true;
                        }
                        HitResult hit = getTarget(player);
                        for (Hand hand : Hand.values())
                        {
                            switch (hit.getType())
                            {
                                case BLOCK:
                                {
                                    player.updateLastActionTime();
                                    ServerWorld world = player.getWorld();
                                    BlockHitResult blockHit = (BlockHitResult) hit;
                                    BlockPos pos = blockHit.getBlockPos();
                                    Direction side = blockHit.getSide();
                                    if (pos.getY() < player.getWorld().getTopY() - (side == Direction.UP ? 1 : 0) && world.canPlayerModifyAt(player, pos))
                                    {
                                        ActionResult result = player.interactionManager.interactBlock(player, world, player.getStackInHand(hand), hand, blockHit);
                                        if (result.isAccepted())
                                        {
                                            if (result.shouldSwingHand()) player.swingHand(hand);
                                            ap.itemUseCooldown = 3;
                                            return true;
                                        }
                                    }
                                    break;
                                }
                                case ENTITY:
                                {
                                    player.updateLastActionTime();
                                    EntityHitResult entityHit = (EntityHitResult) hit;
                                    Entity entity = entityHit.getEntity();
                                    boolean handWasEmpty = player.getStackInHand(hand).isEmpty();
                                    boolean itemFrameEmpty = (entity instanceof ItemFrameEntity) && ((ItemFrameEntity) entity).getHeldItemStack().isEmpty();
                                    Vec3d relativeHitPos = entityHit.getPos().subtract(entity.getX(), entity.getY(), entity.getZ());
                                    if (entity.interactAt(player, relativeHitPos, hand).isAccepted())
                                    {
                                        ap.itemUseCooldown = 3;
                                        return true;
                                    }
                                    // fix for SS itemframe always returns CONSUME even if no action is performed
                                    if (player.interact(entity, hand).isAccepted() && !(handWasEmpty && itemFrameEmpty))
                                    {
                                        ap.itemUseCooldown = 3;
                                        return true;
                                    }
                                    break;
                                }
                            }
                            ItemStack handItem = player.getStackInHand(hand);
                            if (player.interactionManager.interactItem(player, player.getWorld(), handItem, hand).isAccepted())
                            {
                                ap.itemUseCooldown = 3;
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    void inactiveTick(ServerPlayerEntity player, Action action)
                    {
                        EntityPlayerActionPack ap = ((ServerPlayerEntityInterface) player).getActionPack();
                        ap.itemUseCooldown = 0;
                        player.stopUsingItem();
                    }
                },
        ATTACK(true) {
            @Override
            boolean execute(ServerPlayerEntity player, Action action) {
                HitResult hit = getTarget(player);
                switch (hit.getType()) {
                    case ENTITY: {
                        EntityHitResult entityHit = (EntityHitResult) hit;
                        player.attack(entityHit.getEntity());
                        player.swingHand(Hand.MAIN_HAND);
                        player.resetLastAttackedTicks();
                        player.updateLastActionTime();
                        return true;
                    }
                    case BLOCK: {
                        EntityPlayerActionPack ap = ((ServerPlayerEntityInterface) player).getActionPack();
                        if (ap.blockHitDelay > 0)
                        {
                            ap.blockHitDelay--;
                            return false;
                        }
                        BlockHitResult blockHit = (BlockHitResult) hit;
                        BlockPos pos = blockHit.getBlockPos();
                        Direction side = blockHit.getSide();
                        if (player.isBlockBreakingRestricted(player.world, pos, player.interactionManager.getGameMode())) return false;
                        if (ap.currentBlock != null && player.world.getBlockState(ap.currentBlock).isAir())
                        {
                            ap.currentBlock = null;
                            return false;
                        }
                        BlockState state = player.world.getBlockState(pos);
                        boolean blockBroken = false;
                        if (player.interactionManager.getGameMode().isCreative())
                        {
                            player.interactionManager.processBlockBreakingAction(pos, PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, side, player.getWorld().getTopY());
                            ap.blockHitDelay = 5;
                            blockBroken = true;
                        }
                        else  if (ap.currentBlock == null || !ap.currentBlock.equals(pos))
                        {
                            if (ap.currentBlock != null)
                            {
                                player.interactionManager.processBlockBreakingAction(ap.currentBlock, PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, side, player.getWorld().getTopY());
                            }
                            player.interactionManager.processBlockBreakingAction(pos, PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, side, player.getWorld().getTopY());
                            boolean notAir = !state.isAir();
                            if (notAir && ap.curBlockDamageMP == 0)
                            {
                                state.onBlockBreakStart(player.world, pos, player);
                            }
                            if (notAir && state.calcBlockBreakingDelta(player, player.world, pos) >= 1)
                            {
                                ap.currentBlock = null;
                                //instamine??
                                blockBroken = true;
                            }
                            else
                            {
                                ap.currentBlock = pos;
                                ap.curBlockDamageMP = 0;
                            }
                        }
                        else
                        {
                            ap.curBlockDamageMP += state.calcBlockBreakingDelta(player, player.world, pos);
                            if (ap.curBlockDamageMP >= 1)
                            {
                                player.interactionManager.processBlockBreakingAction(pos, PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, side, player.getWorld().getTopY());
                                player.interactionManager.tryBreakBlock(ap.currentBlock);
                                ap.currentBlock = null;
                                ap.blockHitDelay = 5;
                                blockBroken = true;
                            }
                            player.world.setBlockBreakingInfo(-1, pos, (int) (ap.curBlockDamageMP * 10));

                        }
                        player.updateLastActionTime();
                        player.swingHand(Hand.MAIN_HAND);
                        return blockBroken;
                    }
                }
                return false;
            }

            @Override
            void inactiveTick(ServerPlayerEntity player, Action action)
            {
                EntityPlayerActionPack ap = ((ServerPlayerEntityInterface) player).getActionPack();
                if (ap.currentBlock == null) return;
                player.world.setBlockBreakingInfo(-1, ap.currentBlock, -1);
                player.interactionManager.processBlockBreakingAction(ap.currentBlock, PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, Direction.DOWN, player.getWorld().getTopY());
                ap.currentBlock = null;
            }
        },
        JUMP(true)
                {
                    @Override
                    boolean execute(ServerPlayerEntity player, Action action)
                    {
                        if (action.limit == 1)
                        {
                            if (player.isOnGround()) player.jump(); // onGround
                        }
                        else
                        {
                            player.setJumping(true);
                        }
                        return false;
                    }

                    @Override
                    void inactiveTick(ServerPlayerEntity player, Action action)
                    {
                        player.setJumping(false);
                    }
                },
        DROP_ITEM(true)
                {
                    @Override
                    boolean execute(ServerPlayerEntity player, Action action)
                    {
                        player.updateLastActionTime();
                        player.dropSelectedItem(false); // dropSelectedItem
                        return false;
                    }
                };

        public final boolean preventSpectator;

        ActionType(boolean preventSpectator)
        {
            this.preventSpectator = preventSpectator;
        }

        void start(ServerPlayerEntity player, Action action) {}
        abstract boolean execute(ServerPlayerEntity player, Action action);
        void inactiveTick(ServerPlayerEntity player, Action action) {}
        void stop(ServerPlayerEntity player, Action action)
        {
            inactiveTick(player, action);
        }
    }

    public static class Action
    {
        public boolean done = false;
        public final int limit;
        public final int interval;
        public final int offset;
        private int count;
        private int next;
        private final boolean isContinuous;

        private Action(int limit, int interval, int offset, boolean continuous)
        {
            this.limit = limit;
            this.interval = interval;
            this.offset = offset;
            next = interval + offset;
            isContinuous = continuous;
        }

        public static Action continuous()
        {
            return new Action(-1, 1, 0, true);
        }

        public static Action interval(int interval)
        {
            return new Action(-1, interval, 0, false);
        }

        public static Action interval(int interval, int offset)
        {
            return new Action(-1, interval, offset, false);
        }

        Boolean tick(EntityPlayerActionPack actionPack, ActionType type)
        {
            next--;
            Boolean cancel = null;
            if (next <= 0)
            {
                if (interval == 1 && !isContinuous)
                {
                    // need to allow entity to tick, otherwise won't have effect (bow)
                    // actions are 20 tps, so need to clear status mid tick, allowing entities process it till next time
                    if (!type.preventSpectator || !actionPack.player.isSpectator())
                    {
                        type.inactiveTick(actionPack.player, this);
                    }
                }

                if (!type.preventSpectator || !actionPack.player.isSpectator())
                {
                    cancel = type.execute(actionPack.player, this);
                }
                count++;
                if (count == limit)
                {
                    type.stop(actionPack.player, null);
                    done = true;
                    return cancel;
                }
                next = interval;
            }
            else
            {
                if (!type.preventSpectator || !actionPack.player.isSpectator())
                {
                    type.inactiveTick(actionPack.player, this);
                }
            }
            return cancel;
        }

        void retry(EntityPlayerActionPack actionPack)
        {
            //assuming action run but was unsuccesful that tick, but opportunity emerged to retry it, lets retry it.
            if (!ActionType.USE.preventSpectator || !actionPack.player.isSpectator())
            {
                ActionType.USE.execute(actionPack.player, this);
            }
            count++;
            if (count == limit)
            {
                ActionType.USE.stop(actionPack.player, null);
                done = true;
            }
        }
    }
}
