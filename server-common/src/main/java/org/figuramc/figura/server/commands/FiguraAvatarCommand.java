package org.figuramc.figura.server.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.Pair;
import org.figuramc.figura.server.utils.Utils;

import java.util.UUID;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static org.figuramc.figura.server.commands.FiguraServerCommands.permissionCheck;
import static org.figuramc.figura.server.utils.ComponentUtils.text;

public class FiguraAvatarCommand {
    public static LiteralArgumentBuilder<FiguraServerCommandSource> getCommand() {
        LiteralArgumentBuilder<FiguraServerCommandSource> avatarCommand = literal("avatar");

        avatarCommand.then(immortalizeCommand());
        avatarCommand.then(setAvatarCommand());
        avatarCommand.then(clearAvatarCommand());

        return avatarCommand;
    }



    // COMMAND BUILDERS

    private static LiteralArgumentBuilder<FiguraServerCommandSource> immortalizeCommand() {
        LiteralArgumentBuilder<FiguraServerCommandSource> immortalize = literal("immortalize");
        immortalize.requires(permissionCheck("figura.avatars.immortalize"));
        immortalize.executes(FiguraAvatarCommand::immortalizeEquippedAvatar);

        RequiredArgumentBuilder<FiguraServerCommandSource, String> immortalizeSpecific = argument("avatar_hash", StringArgumentType.string());
        immortalizeSpecific.executes(FiguraAvatarCommand::immortalizeSpecificAvatar);

        immortalize.then(immortalizeSpecific);

        return immortalize;
    }

    private static LiteralArgumentBuilder<FiguraServerCommandSource> setAvatarCommand() {
        LiteralArgumentBuilder<FiguraServerCommandSource> setAvatar = literal("set");
        setAvatar.requires(permissionCheck("figura.avatars.set"));

        RequiredArgumentBuilder<FiguraServerCommandSource, String> target = argument("target", StringArgumentType.string());
        target.executes(FiguraAvatarCommand::setAvatarEquipped);
        setAvatar.then(target);

        RequiredArgumentBuilder<FiguraServerCommandSource, String> avatarHash = argument("avatar", StringArgumentType.string());
        avatarHash.executes(FiguraAvatarCommand::setAvatarSpecific);
        target.then(avatarHash);

        return setAvatar;
    }

    private static LiteralArgumentBuilder<FiguraServerCommandSource> clearAvatarCommand() {
        LiteralArgumentBuilder<FiguraServerCommandSource> clearAvatar = literal("clear");
        clearAvatar.requires(permissionCheck("figura.avatars.clear"));

        RequiredArgumentBuilder<FiguraServerCommandSource, String> target = argument("target", StringArgumentType.string());
        target.executes(FiguraAvatarCommand::clearAvatar);
        clearAvatar.then(target);

        return clearAvatar;
    }


    // IMMORTALIZED AVATAR EXECUTORS

    private static int immortalizeEquippedAvatar(CommandContext<FiguraServerCommandSource> ctx) {
        FiguraServerCommandSource source = ctx.getSource();
        FiguraUser executor = source.getExecutor();
        var equipped = executor.equippedAvatar();
        Hash avatar = equipped != null ? equipped.right().hash() : null;
        if (avatar != null) {
            return immortalize(avatar, source);
        }
        source.sendComponent(immortalizedAvatarNotEquippedError());
        return -1;
    }

    private static int immortalizeSpecificAvatar(CommandContext<FiguraServerCommandSource> ctx) {
        FiguraServerCommandSource source = ctx.getSource();
        Hash avatar;
        try {
            avatar = Utils.parseHash(StringArgumentType.getString(ctx, "avatar_hash"));
        }
        catch (Exception ignored) {
            source.sendComponent(unableToParseHash());
            return -1;
        }
        if (!source.getServer().avatarManager().avatarExists(avatar)) {
            source.sendComponent(avatarDoesntExist());
            return -1;
        }

        return immortalize(avatar, source);
    }

    private static int immortalize(Hash avatar, FiguraServerCommandSource source) {
        FiguraServer server = source.getServer();
        UUID fakeUserUUID = UUID.randomUUID();
        FiguraUser fakeUser = server.userManager().getUser(fakeUserUUID);
        fakeUser.setEquippedAvatar("immortalized", avatar, Hash.empty());
        source.sendComponent(immortalizedAvatarMessage(fakeUserUUID));
        return 0;
    }

    // SET AVATAR EXECUTORS

    private static int setAvatarEquipped(CommandContext<FiguraServerCommandSource> ctx) {
        FiguraServerCommandSource source = ctx.getSource();
        FiguraServer server = source.getServer();
        FiguraUser executor = source.getExecutor();
        UUID targetUUID;
        Pair<String, EHashPair> equippedAvatar = executor.equippedAvatar();
        if (equippedAvatar == null) {
            source.sendComponent(setAvatarNotEquippedError());
            return -1;
        }
        try {
            targetUUID = UUID.fromString(StringArgumentType.getString(ctx, "target"));
        }
        catch (Exception ignored) {
            source.sendComponent(failedToParseTarget());
            return -1;
        }
        FiguraUser target = server.userManager().getUser(targetUUID);
        return setAvatar(source, target, equippedAvatar.right().hash());
    }

    private static int setAvatarSpecific(CommandContext<FiguraServerCommandSource> ctx) {
        FiguraServerCommandSource source = ctx.getSource();
        FiguraServer server = source.getServer();
        UUID targetUUID;
        try {
            targetUUID = UUID.fromString(StringArgumentType.getString(ctx, "target"));
        }
        catch (Exception ignored) {
            source.sendComponent(failedToParseTarget());
            return -1;
        }
        Hash avatar;
        try {
            avatar = Utils.parseHash(StringArgumentType.getString(ctx, "avatar"));
        }
        catch (Exception ignored) {
            source.sendComponent(unableToParseHash());
            return -1;
        }
        if (!server.avatarManager().avatarExists(avatar)) {
            source.sendComponent(avatarDoesntExist());
            return -1;
        }

        FiguraUser target = server.userManager().getUser(targetUUID);
        return setAvatar(source, target, avatar);
    }

    private static int setAvatar(FiguraServerCommandSource source, FiguraUser target, Hash avatar) {
        target.setEquippedAvatar("avatar", avatar, Hash.empty());
        source.sendComponent(setAvatarMessage(target.uuid()));
        return 0;
    }

    // CLEAR AVATAR EXECUTOR

    private static int clearAvatar(CommandContext<FiguraServerCommandSource> ctx) {
        FiguraServerCommandSource source = ctx.getSource();
        FiguraServer server = source.getServer();
        UUID targetUUID;
        try {
            targetUUID = UUID.fromString(StringArgumentType.getString(ctx, "target"));
        }
        catch (Exception ignored) {
            source.sendComponent(failedToParseTarget());
            return -1;
        }
        FiguraUser target = server.userManager().getUser(targetUUID);
        target.removeEquippedAvatar();
        source.sendComponent(text("Avatar for specified target has successfully been cleared.").color("blue").build());
        return 0;
    }


    // COMMON MESSAGE BUILDERS

    private static JsonObject unableToParseHash() {
        return text("Error occurred while parsing avatar hash.").color("red").build();
    }

    private static JsonObject avatarDoesntExist() {
        return text("Avatar with specified hash does not exist..").color("red").build();
    }

    private static JsonObject failedToParseTarget() {
        return text("Failed to parse target UUID").color("red").build();
    }

    // IMMORTALIZED AVATAR MESSAGE BUILDERS

    private static JsonObject immortalizedAvatarMessage(UUID uuid) {
        String uuidHex = uuid.toString();
        return text("Immortalized avatar ").color("blue")
                .add(text(uuidHex).color("green").style("bold", true)
                        .clickEvent("copy_to_clipboard", uuidHex)
                        .tooltip(text("Click to copy")))
                .add(text(" is successfully created!")).build();
    }

    private static JsonObject immortalizedAvatarNotEquippedError() {
        return text("You have to equip avatar in order to make it immortalized.").color("red").build();
    }

    // SET AVATAR MESSAGE BUILDERS

    private static JsonObject setAvatarMessage(UUID uuid) {
        String uuidHex = Utils.uuidToHex(uuid);
        return text("Equipped avatar for ").color("blue")
                .add(text(uuidHex).color("green").style("bold", true))
                .add(text(" is successfully set!")).build();
    }

    private static JsonObject setAvatarNotEquippedError() {
        return text("You have to equip avatar in order to set it to someone.").color("red").build();
    }
}
