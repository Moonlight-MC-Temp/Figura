package org.figuramc.figura.commands.forge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.server.commands.FiguraServerCommandSource;
import org.figuramc.figura.server.commands.FiguraServerCommands;

@Mod.EventBusSubscriber(modid = FiguraMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class FiguraServerCommandsForge {

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandDispatcher<FiguraServerCommandSource> casted = (CommandDispatcher) dispatcher;
        casted.register(FiguraServerCommands.getCommand());
    }
}
