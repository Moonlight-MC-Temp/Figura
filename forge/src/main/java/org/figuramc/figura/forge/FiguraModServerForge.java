package org.figuramc.figura.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.event.EventNetworkChannel;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.c2s.C2SPacketHandler;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = FiguraModServer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.DEDICATED_SERVER)
public class FiguraModServerForge {
    private static FiguraServerForge fsbInstance;

    static void initServer() {
        fsbInstance = new FiguraServerForge();
        fsbInstance.init();
        MinecraftForge.EVENT_BUS.addListener(FiguraModServerForge::onInitializeServer);
        MinecraftForge.EVENT_BUS.addListener(FiguraModServerForge::onTick);
        MinecraftForge.EVENT_BUS.addListener(FiguraModServerForge::onServerStop);
        MinecraftForge.EVENT_BUS.register(new FiguraForgePermissions());
    }

    public static void registerPacketListener(Identifier id, EventNetworkChannel channel) {
        var handler = fsbInstance.getPacketHandler(id);
        if (handler != null) channel.addListener(new ForgeNetworkListener<>(id, handler));
    }

    public static void onInitializeServer(ServerStartedEvent event) {
        fsbInstance.finishInitialization(event.getServer());
    }

    public static void onServerStop(ServerStoppingEvent event) {
        fsbInstance.close();
    }

    public static void onTick(TickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.type == TickEvent.Type.SERVER) {
            if (fsbInstance != null) fsbInstance.tick();
        }
    }

    private static final class ForgeNetworkListener<P extends Packet> implements Consumer<NetworkEvent> {
        private final Identifier id;
        private final C2SPacketHandler<P> handler;

        private ForgeNetworkListener(Identifier id, C2SPacketHandler<P> handler) {
            this.id = id;
            this.handler = handler;
        }

        @Override
        public void accept(NetworkEvent event) {
            if (event.getPayload() == null) return;
            var ctx = event.getSource().get();
            if (ctx.getDirection().equals(NetworkDirection.PLAY_TO_SERVER)) {
                try {
                    P packet = handler.serialize(new FriendlyByteBufWrapper(event.getPayload()));
                    ServerPlayer sender = ctx.getSender();
                    handler.handle(sender.getUUID(), packet);
                    ctx.setPacketHandled(true);
                }
                catch (Exception e) {
                    fsbInstance.logError("Failed to handle packet %s".formatted(id), e);
                }
            }
        }
    }
}
