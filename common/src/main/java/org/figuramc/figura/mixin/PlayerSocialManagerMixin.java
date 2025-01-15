package org.figuramc.figura.mixin;

import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.backend2.FSB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerSocialManager.class)
public class PlayerSocialManagerMixin {
    @Inject(method = "addPlayer", at = @At("HEAD"))
    private void onPlayerJoin(PlayerInfo player, CallbackInfo ci) {
        UUID id = player.getProfile().getId();
        FSB.instance().reset(id);
        AvatarManager.clearAvatars(id);
    }

    @Inject(method = "removePlayer", at = @At("HEAD"))
    private void onPlayerJoin(UUID player, CallbackInfo ci) {
        FSB.instance().reset(player);
        AvatarManager.clearAvatars(player);
    }
}
