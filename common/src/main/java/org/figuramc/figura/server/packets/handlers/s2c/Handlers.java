package org.figuramc.figura.server.packets.handlers.s2c;

import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.server.packets.*;
import org.figuramc.figura.server.packets.s2c.*;
import org.figuramc.figura.server.utils.Identifier;

import java.util.HashMap;
import java.util.function.BiConsumer;

public class Handlers {
    private static final HashMap<Identifier, S2CPacketHandler<?>> PACKET_HANDLERS = new HashMap<>() {{
        put(S2CBackendHandshakePacket.PACKET_ID, new S2CHandshakeHandler());
        put(S2CRefusedPacket.PACKET_ID, new S2CRefusalHandler());
        put(S2CUserdataPacket.PACKET_ID, new S2CUserdataHandler());
        put(S2CConnectedPacket.PACKET_ID, new S2CConnectedHandler());
        put(S2CUserdataNotFoundPacket.PACKET_ID, new S2CUserdataNotFoundHandler());
        put(AvatarDataPacket.PACKET_ID, new S2CAvatarDataPacketHandler());
        put(CloseIncomingStreamPacket.PACKET_ID, new CloseIncomingStreamPacketHandler());
        put(CloseOutcomingStreamPacket.PACKET_ID, new CloseOutcomingStreamPacketHandler());
        put(AllowIncomingStreamPacket.PACKET_ID, new AllowOutcomingStreamPacketHandler());
        put(S2CPingPacket.PACKET_ID, new S2CPingPacketHandler());
        put(S2CPingErrorPacket.PACKET_ID, new S2CPingErrorPacketHandler());
        put(S2CNotifyPacket.PACKET_ID, new S2CNotifyPacketHandler());
        put(CustomFSBPacket.PACKET_ID, new S2CCustomFSBPacketHandler());
    }};

    public static void forEachHandler(BiConsumer<Identifier, S2CPacketHandler<?>> consumer) {
        PACKET_HANDLERS.forEach(consumer);
    }

    public static S2CPacketHandler<Packet> getHandler(Identifier identifier) {
        return (S2CPacketHandler<Packet>) PACKET_HANDLERS.get(identifier);
    }

    public static S2CPacketHandler<Packet> getHandler(ResourceLocation resLoc) {
        Identifier identifier = new Identifier(resLoc.getNamespace(), resLoc.getPath());
        return getHandler(identifier);
    }
}
