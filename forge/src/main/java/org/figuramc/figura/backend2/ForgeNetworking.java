package org.figuramc.figura.backend2;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;
import org.figuramc.figura.forge.FiguraModClientForge;
import org.figuramc.figura.forge.FiguraModServerForge;
import org.figuramc.figura.server.packets.Packets;
import org.figuramc.figura.server.packets.Side;
import org.figuramc.figura.server.utils.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.function.BiConsumer;

public class ForgeNetworking {
    private static final HashMap<Identifier, EventNetworkChannel> channels = new HashMap<>();
    public static void init() {
        Side currentSide = currentSide();
        var packetListenerRegisterer = getCurrentListenerRegisterer();
        Packets.forEachPacket((id, desc) -> {
            var resLoc = new ResourceLocation(id.namespace(), id.path());
            EventNetworkChannel channel = NetworkRegistry.newEventChannel(
                    resLoc,
                    () -> NetworkRegistry.ACCEPTVANILLA,
                    NetworkRegistry.acceptMissingOr(NetworkRegistry.ACCEPTVANILLA),
                    NetworkRegistry.acceptMissingOr(NetworkRegistry.ACCEPTVANILLA));
            channels.put(id, channel);
            Side packetSide = desc.side();
            if (packetSide.receivedBy(currentSide)) packetListenerRegisterer.accept(id, channel);
        });
    }

    public static Side currentSide() {
        if (FMLEnvironment.dist == Dist.CLIENT) return Side.CLIENT;
        else return Side.SERVER;
    }

    public static BiConsumer<Identifier, EventNetworkChannel> getCurrentListenerRegisterer() {
        if (FMLEnvironment.dist == Dist.CLIENT) return FiguraModClientForge::registerPacketListener;
        else return FiguraModServerForge::registerPacketListener;
    }

    public static @Nullable EventNetworkChannel getChannel(Identifier id) {
        return channels.get(id);
    }
}
