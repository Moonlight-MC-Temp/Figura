package org.figuramc.figura.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.figuramc.figura.utils.FiguraClientCommandSource;

class UploadCommand {
    public static LiteralArgumentBuilder<FiguraClientCommandSource> getCommand() {
        LiteralArgumentBuilder<FiguraClientCommandSource> upload = LiteralArgumentBuilder.literal("upload");
        upload.executes(context -> {
            Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());

            NetworkStuff.uploadAvatar(avatar);
            AvatarList.selectedEntry = null;
            return 1;
        });
        return upload;
    }
}