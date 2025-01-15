package org.figuramc.figura.server.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.figuramc.figura.server.FiguraUser;

import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static org.figuramc.figura.server.utils.ComponentUtils.text;

public class FiguraBadgesCommand {
    public static LiteralArgumentBuilder<FiguraServerCommandSource> getCommand() {
        LiteralArgumentBuilder<FiguraServerCommandSource> root = literal("badge");

        LiteralArgumentBuilder<FiguraServerCommandSource> set = literal("set");
        RequiredArgumentBuilder<FiguraServerCommandSource, String> badgeArgument = argument("badge", StringArgumentType.word());
        badgeArgument.executes(FiguraBadgesCommand::setBadge);
        badgeArgument.suggests(FiguraBadgesCommand::suggestBadge);
        set.then(badgeArgument);

        LiteralArgumentBuilder<FiguraServerCommandSource> clear = literal("clear");
        clear.executes(FiguraBadgesCommand::clearBadge);

        root.then(set);
        root.then(clear);

        return root;
    }

    private static CompletableFuture<Suggestions> suggestBadge(CommandContext<FiguraServerCommandSource> ctx, SuggestionsBuilder suggestionsBuilder) {
        String currentBadgeInput;
        try {
            currentBadgeInput = StringArgumentType.getString(ctx, "badge").toLowerCase();
        }
        catch (Exception ignored) {
            currentBadgeInput = "";
        }
        for (PrideBadge badge: PrideBadge.values()) {
            String badgeName = badge.name().toLowerCase();
            if (badgeName.contains(currentBadgeInput)) suggestionsBuilder.suggest(badgeName);
        }
        return CompletableFuture.completedFuture(suggestionsBuilder.build());
    }

    private static int setBadge(CommandContext<FiguraServerCommandSource> ctx) {
        FiguraServerCommandSource source = ctx.getSource();
        FiguraUser user = source.getExecutor();
        String badgeName = StringArgumentType.getString(ctx, "badge").toUpperCase();
        PrideBadge badge;
        try {
            badge = PrideBadge.valueOf(badgeName);
        }
        catch (Exception ignored) {
            source.sendComponent(badgeNotFound(badgeName));
            return -1;
        }
        user.prideBadges().clear();
        int badgeIndex = badge.ordinal();
        user.prideBadges().set(badgeIndex, true);
        source.sendComponent(badgeSetMessage());
        return 0;
    }

    private static int clearBadge(CommandContext<FiguraServerCommandSource> ctx) {
        FiguraServerCommandSource source = ctx.getSource();
        FiguraUser user = source.getExecutor();
        user.prideBadges().clear();
        source.sendComponent(badgeClearMessage());
        return 0;
    }

    private static JsonObject badgeSetMessage() {
        return text("Pride badge successfully changed. Reload your avatar in order to see changes.").color("blue").build();
    }

    private static JsonObject badgeClearMessage() {
        return text("Pride badge successfully cleared. Reload your avatar in order to see changes.").color("blue").build();
    }

    private static JsonObject badgeNotFound(String badgeName) {
        return text("Pride badge with name ").color("red")
                .add(text(badgeName).color("white"))
                .add(text(" not found."))
                .build();
    }

    // BADGES
    private enum PrideBadge {
        AGENDER,
        AROACE,
        AROMANTIC,
        ASEXUAL,
        BIGENDER,
        BISEXUAL,
        DEMIBOY,
        DEMIGENDER,
        DEMIGIRL,
        DEMIROMANTIC,
        DEMISEXUAL,
        DISABILITY,
        FINSEXUAL,
        GAYMEN,
        GENDERFAE,
        GENDERFLUID,
        GENDERQUEER,
        INTERSEX,
        LESBIAN,
        NONBINARY,
        PANSEXUAL,
        PLURAL,
        POLYSEXUAL,
        PRIDE,
        TRANSGENDER
    }
}
