package org.figuramc.figura.commands.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.figuramc.figura.server.commands.FiguraServerCommandSource;
import org.figuramc.figura.server.commands.FiguraServerCommands;

public class FiguraServerCommandsFabric {
    public static void init() {
        CommandRegistrationCallback.EVENT.register(FiguraServerCommandsFabric::register);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection environment) {
        if (environment == Commands.CommandSelection.DEDICATED) {
            CommandDispatcher<FiguraServerCommandSource> casted = (CommandDispatcher) dispatcher;
            casted.register(FiguraServerCommands.getCommand());
        }
    }
}
