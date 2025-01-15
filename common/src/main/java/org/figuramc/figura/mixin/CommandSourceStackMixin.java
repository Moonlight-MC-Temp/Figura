package org.figuramc.figura.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.server.commands.FiguraServerCommandSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackMixin implements FiguraServerCommandSource {
    @Shadow @Final @Nullable
    private Entity entity;

    @Shadow public abstract boolean hasPermission(int level);

    @Override
    public UUID getExecutorUUID() {
        return entity != null ? entity.getUUID() : null;
    }

    @Override
    public boolean permission(String permission) {
        return hasPermission(4) || (entity != null && FiguraServerCommandSource.super.permission(permission));
    }
}
