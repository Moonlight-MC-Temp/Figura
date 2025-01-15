package org.figuramc.figura.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.backend2.HttpAPI;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.utils.FiguraClientCommandSource;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

public class BadgeCommand {
    public static LiteralArgumentBuilder<FiguraClientCommandSource> getCommand() {
        LiteralArgumentBuilder<FiguraClientCommandSource> badgeCommand = literal("badge");

        for (Badges.Pride badge: Badges.Pride.values()) {
            LiteralArgumentBuilder<FiguraClientCommandSource> badgeArgument = literal(badge.name().toLowerCase());
            badgeArgument.executes(new BadgeCommandExecutor(badge));

            badgeCommand.then(badgeArgument);
        }

        LiteralArgumentBuilder<FiguraClientCommandSource> clearBadge = literal("clear");
        clearBadge.executes(BadgeCommand::clearBadge);
        badgeCommand.then(clearBadge);

        return badgeCommand;
    }

    private static int clearBadge(CommandContext<FiguraClientCommandSource> ctx) {
        NetworkStuff.clearBadge();
        return 0;
    }

    private record BadgeCommandExecutor(Badges.Pride badge) implements Command<FiguraClientCommandSource> {
        @Override
        public int run(CommandContext<FiguraClientCommandSource> ctx) throws CommandSyntaxException {
            NetworkStuff.setBadge(badge.ordinal());
            return 0;
        }
    }
}
