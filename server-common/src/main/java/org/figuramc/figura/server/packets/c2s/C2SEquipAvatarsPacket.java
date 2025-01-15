package org.figuramc.figura.server.packets.c2s;

import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class C2SEquipAvatarsPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "c2s/avatars/equip");
    private final HashMap<String, EHashPair> avatars;

    public C2SEquipAvatarsPacket(HashMap<String, EHashPair> pairs) {
        avatars = pairs;
    }

    public C2SEquipAvatarsPacket(IFriendlyByteBuf byteBuf) {
        int count = byteBuf.readByte();
        HashMap<String, EHashPair> pairs = new HashMap<>();
        for (int i = 0; i < count; i++) {
            pairs.put(new String(byteBuf.readByteArray(256)),
                    new EHashPair(byteBuf.readHash(), byteBuf.readHash()));
        }
        avatars = pairs;
    }

    public HashMap<String, EHashPair> avatars() {
        return avatars;
    }

    public void write(IFriendlyByteBuf byteBuf) {
        byteBuf.writeByte(avatars.size());
        for (Map.Entry<String, EHashPair> entry: avatars.entrySet()) {
            byteBuf.writeByteArray(entry.getKey().getBytes(StandardCharsets.UTF_8));
            byteBuf.writeBytes(entry.getValue().hash().get());
            byteBuf.writeBytes(entry.getValue().ehash().get());
        }
    }

    public Identifier getId() {
        return PACKET_ID;
    }
}
