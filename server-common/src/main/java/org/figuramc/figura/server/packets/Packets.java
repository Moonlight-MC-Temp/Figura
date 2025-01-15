package org.figuramc.figura.server.packets;

import org.figuramc.figura.server.packets.c2s.*;
import org.figuramc.figura.server.packets.s2c.*;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.figuramc.figura.server.packets.Side.*;

public class Packets {

    private static final HashMap<Identifier, PacketDescriptor<?>> PACKETS = new HashMap<>() {{
        put(C2SBackendHandshakePacket.PACKET_ID, desc(CLIENT, empty(C2SBackendHandshakePacket::new)));
        put(C2SDeleteAvatarPacket.PACKET_ID, desc(CLIENT, C2SDeleteAvatarPacket::new));
        put(C2SEquipAvatarsPacket.PACKET_ID, desc(CLIENT, C2SEquipAvatarsPacket::new));
        put(C2SFetchAvatarPacket.PACKET_ID, desc(CLIENT, C2SFetchAvatarPacket::new));
        put(C2SFetchOwnedAvatarsPacket.PACKET_ID, desc(CLIENT, empty(C2SFetchOwnedAvatarsPacket::new)));
        put(C2SFetchUserdataPacket.PACKET_ID, desc(CLIENT, C2SFetchUserdataPacket::new));
        put(C2SPingPacket.PACKET_ID, desc(CLIENT, C2SPingPacket::new));
        put(C2SUploadAvatarPacket.PACKET_ID, desc(CLIENT, C2SUploadAvatarPacket::new));

        put(S2CRefusedPacket.PACKET_ID, desc(SERVER, empty(S2CRefusedPacket::new)));
        put(S2CBackendHandshakePacket.PACKET_ID, desc(SERVER, S2CBackendHandshakePacket::new));
        put(S2CConnectedPacket.PACKET_ID, desc(SERVER, S2CConnectedPacket::new));
        put(S2CInitializeAvatarStreamPacket.PACKET_ID, desc(SERVER, S2CInitializeAvatarStreamPacket::new));
        put(S2COwnedAvatarsPacket.PACKET_ID, desc(SERVER, S2COwnedAvatarsPacket::new));
        put(S2CPingErrorPacket.PACKET_ID, desc(SERVER, S2CPingErrorPacket::new));
        put(S2CPingPacket.PACKET_ID, desc(SERVER, S2CPingPacket::new));
        put(S2CUserdataPacket.PACKET_ID, desc(SERVER, S2CUserdataPacket::new));
        put(S2CUserdataNotFoundPacket.PACKET_ID, desc(SERVER, S2CUserdataNotFoundPacket::new));
        put(S2CNotifyPacket.PACKET_ID, desc(SERVER, S2CNotifyPacket::new));

        put(AllowIncomingStreamPacket.PACKET_ID, desc(BOTH, AllowIncomingStreamPacket::new));
        put(AvatarDataPacket.PACKET_ID, desc(BOTH, AvatarDataPacket::new));
        put(CloseIncomingStreamPacket.PACKET_ID, desc(BOTH, CloseIncomingStreamPacket::new));
        put(CloseOutcomingStreamPacket.PACKET_ID, desc(BOTH, CloseOutcomingStreamPacket::new));
        put(CustomFSBPacket.PACKET_ID, desc(BOTH, CustomFSBPacket::new));
    }};

    public static void forEachPacket(BiConsumer<Identifier, PacketDescriptor<?>> consumer) {
        PACKETS.forEach(consumer);
    }

    public static @Nullable PacketDescriptor<?> getPacketDescriptor(Identifier id) {
        return PACKETS.get(id);
    }

    private record EmptyDeserializer<P extends Packet>(Supplier<P> emptyConstructor) implements Packet.Deserializer<P> {
        @Override
            public P read(IFriendlyByteBuf buf) {
                return emptyConstructor.get();
            }
        }

    private static <P extends Packet> PacketDescriptor<P> desc(Side recipientSide, Packet.Deserializer<P> constructor) {
        return new PacketDescriptor<>(recipientSide, constructor);
    }

    private static <P extends Packet> EmptyDeserializer<P> empty(Supplier<P> emptyConstructor) {
        return new EmptyDeserializer<>(emptyConstructor);
    }

    public record PacketDescriptor<P extends Packet>(Side side, Packet.Deserializer<P> constructor) {

    }
}
