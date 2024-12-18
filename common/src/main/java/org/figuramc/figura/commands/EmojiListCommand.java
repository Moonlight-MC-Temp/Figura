package org.figuramc.figura.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.font.EmojiContainer;
import org.figuramc.figura.font.EmojiUnicodeLookup;
import org.figuramc.figura.font.Emojis;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraClientCommandSource;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static net.minecraft.network.chat.Component.literal;

class EmojiListCommand {
    private static final Component COMMA_SPACE = literal(", ").withStyle(ChatFormatting.GRAY);

    public static LiteralArgumentBuilder<FiguraClientCommandSource> getCommand() {
        LiteralArgumentBuilder<FiguraClientCommandSource> load = LiteralArgumentBuilder.literal("emojis");

        RequiredArgumentBuilder<FiguraClientCommandSource, String> path = RequiredArgumentBuilder.argument("category", StringArgumentType.greedyString());
        path.suggests(EmojiListCommand::getSuggestions);
        path.executes(EmojiListCommand::listCategory);

        return load.then(path);
    }

    public static CompletableFuture<Suggestions> getSuggestions(CommandContext<FiguraClientCommandSource> context, SuggestionsBuilder builder) {
        builder.suggest("all");
        Emojis.getCategoryNames().forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static int listCategory(CommandContext<FiguraClientCommandSource> context) {
        FiguraClientCommandSource src = context.getSource();
        String category = context.getArgument("category", String.class);
        if (Objects.equals(category, "all")) {
            for (String curCategory : Emojis.getCategoryNames()) {
                if (!printEmojis(curCategory, src::figura$sendFeedback, src::figura$sendError)) {
                    return 0;
                }
                src.figura$sendFeedback(literal(""));
            }

            return 1;
        }
        return printEmojis(category, src::figura$sendFeedback, src::figura$sendError) ? 1 : 0;
    }


    private static boolean printEmojis(String category, Consumer<Component> feedback, Consumer<Component> error) {
        if (!Emojis.hasCategory(category)) {
            error.accept(literal("Emoji category \"" + category + "\" doesn't exist!"));
            return false;
        }

        EmojiContainer container = Emojis.getCategory(category);
        Collection<String> unicodeValues = container.getLookup().unicodeValues();
        EmojiUnicodeLookup lookup = container.getLookup();

        // give the category a title
        feedback.accept(literal(String.format("--- %s (%s) ---", container.name, unicodeValues.size())).withStyle(ColorUtils.Colors.AWESOME_BLUE.style));


        // Gather each emoji name and append it into a single message
        MutableComponent comp = literal("");
        unicodeValues.stream().sorted().forEach(unicode -> {
            String[] aliases = lookup.getNames(unicode);
            if (aliases != null) {
                MutableComponent msg = literal("");
                for (int i = 0; i < aliases.length; i++) {
                    msg.append(literal(aliases[i]).withStyle(ColorUtils.Colors.AWESOME_BLUE.style));
                    if (i < aliases.length - 1) {
                        msg.append(COMMA_SPACE);
                    }
                }
                msg.append(literal("\ncodepoint: " + unicode.codePointAt(0)).withStyle(ChatFormatting.GRAY));
                comp.append(Emojis.getEmoji(aliases[0], Style.EMPTY, msg));
            }
        });

        feedback.accept(comp);

        return true;
    }
}
