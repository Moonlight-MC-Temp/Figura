package org.figuramc.figura.server.packets.c2s;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

/**
 * Packet sent to server to acknowledge that client allows server to work as Figura backend.
 */
public class C2SBackendHandshakePacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "c2s/handshake");

    public C2SBackendHandshakePacket() {

    }


    @Override
    public void write(IFriendlyByteBuf byteBuf) {

    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}