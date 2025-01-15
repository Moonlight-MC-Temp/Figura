package org.figuramc.figura.mixin.fabric;

import net.minecraft.server.MinecraftServer;
import org.figuramc.figura.fabric.FiguraServerFabric;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.FiguraServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow public abstract boolean isDedicatedServer();

    @Inject(method = "runServer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;", ordinal = 0))
    private void onServerStart(CallbackInfo ci) {
        if (isDedicatedServer()) {
            var inst = FiguraServerFabric.getInstance();
            inst.init();
            inst.finishInitialization((MinecraftServer) (Object) this);
        }
    }

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void onServerStop(CallbackInfo ci) {
        if (FiguraServer.initialized())
            FiguraServer.getInstance().close();
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void onTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (FiguraServer.initialized())
            FiguraServer.getInstance().tick();
    }
}
