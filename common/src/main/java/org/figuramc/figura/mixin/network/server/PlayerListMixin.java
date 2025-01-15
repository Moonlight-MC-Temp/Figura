package org.figuramc.figura.mixin.network.server;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.FiguraServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void onPlayerDisconnect(ServerPlayer player, CallbackInfo ci) {
        var srv = FiguraModServer.getInstance();
        if (FiguraServer.initialized()) {
            srv.userManager().onUserLeave(player.getUUID());
        }
    }
}
