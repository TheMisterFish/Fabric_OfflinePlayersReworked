package net.lilfish.offlineplayersreworked.mixin;

import com.mojang.authlib.GameProfile;
import net.lilfish.offlineplayersreworked.interfaces.ServerPlayerEntityInterface;
import net.lilfish.offlineplayersreworked.EntityPlayerActionPack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.lilfish.offlineplayersreworked.OfflinePlayers.MOD_ID;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Unique
    public EntityPlayerActionPack actionPack;

    @Override
    public EntityPlayerActionPack getActionPack() {
        return actionPack;
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onServerPlayerEntityConstructor(
            MinecraftServer minecraftServer_1,
            ServerWorld serverWorld_1,
            GameProfile gameProfile_1,
            CallbackInfo ci) {
        this.actionPack = new EntityPlayerActionPack((ServerPlayerEntity) (Object) this);
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void onTick(CallbackInfo ci) {
        try {
            actionPack.onUpdate();
        } catch (StackOverflowError soe) {
            LOGGER.error("StackOverflowError onTick update: ", soe);
        } catch (Throwable exc) {
            LOGGER.error("Throwable error onTick update: ", exc);
        }
    }
}
