package com.gametest.offlineplayersreworked.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.datafixers.DataFixer;
import com.offlineplayersreworked.storage.OfflinePlayersStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow
    public abstract Path getWorldPath(LevelResource levelResource);

    @Shadow
    public abstract Path getServerDirectory();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory, CallbackInfo ci) {
        Path playerDataDir = this.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        List<Path> cachefiles = List.of(this.getServerDirectory().resolve("usercache.json"),
                this.getServerDirectory().resolve("ops.json"),
                this.getServerDirectory().resolve("whitelist.json"));

        deletePlayerData(playerDataDir, cachefiles);
    }

    @Inject(method = "loadLevel", at = @At("TAIL"))
    private void load(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer)(Object)this;

        OfflinePlayersStorage storage = OfflinePlayersStorage.getStorage(server);
        storage.findAll().forEach(offlinePlayerModel -> {
            storage.remove(offlinePlayerModel.getId());
        });
    }


    @Unique
    private static void deletePlayerData(Path playerDataDir, List<Path> cachefiles) {
        try {
            if (Files.exists(playerDataDir)) {
                try (var stream = Files.list(playerDataDir)) {
                    stream
                            .filter(path -> path.toString().endsWith(".dat") || path.toString().endsWith(".dat_old"))
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to delete " + path, e);
                                }
                            });
                }
            }

            cachefiles.forEach(file -> {
                if (Files.exists(file)) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete " + file, e);
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "getProfileCache", at = @At("HEAD"), cancellable = true)
    private void test(CallbackInfoReturnable<GameProfileCache> cir) {
        File dummyFile = new File("usercache.json");
        GameProfileRepository repo = (names, callback) -> {
            //no-op
        };
        GameProfileCache cache = new GameProfileCache(repo, dummyFile);

        cir.setReturnValue(cache);
    }

}
