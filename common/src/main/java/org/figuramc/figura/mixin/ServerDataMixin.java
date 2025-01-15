package org.figuramc.figura.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.CompoundTag;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.ducks.ServerDataAccessor;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerData.class)
public abstract class ServerDataMixin implements ServerDataAccessor {
    @Unique
    public boolean figura$allowFSB = true;

    @Inject(method = "read", at = @At("RETURN"))
    private static void onRead(CompoundTag root, CallbackInfoReturnable<ServerData> cir, @Local ServerData serverData) {
        if (root.contains("fsb")) {
            ((ServerDataAccessor)serverData).figura$setAllowFigura(root.getBoolean("fsb"));
        }
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void onWrite(CallbackInfoReturnable<CompoundTag> cir, @Local CompoundTag tag) {
        tag.putBoolean("fsb", figura$allowFSB);
    }

    @Unique
    public boolean figura$allowFigura() {
        return figura$allowFSB;
    }

    @Unique
    public void figura$setAllowFigura(boolean allow) {
        this.figura$allowFSB = allow;
    }

    @Inject(method = "copyFrom", at = @At("RETURN"))
    private void onCopy(ServerData serverInfo, CallbackInfo ci) {
        figura$allowFSB = ((ServerDataAccessor) serverInfo).figura$allowFigura();
    }
}
