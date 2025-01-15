package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class S2COwnedAvatarsPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "s2c/owned_avatars");
    private final HashMap<String, EHashPair> ownedAvatars;

    public S2COwnedAvatarsPacket(HashMap<String, EHashPair> ownedAvatars) {
        this.ownedAvatars = (HashMap<String, EHashPair>) ownedAvatars.clone();
    }

    public S2COwnedAvatarsPacket(IFriendlyByteBuf buf) {
        int avatarsCount = buf.readInt();
        HashMap<String, EHashPair> ownedAvatars = new HashMap<>();
        for (int i = 0; i < avatarsCount; i++) {
            String id = new String(buf.readByteArray(256), UTF_8);
            Hash hash = buf.readHash();
            Hash ehash = buf.readHash();
            ownedAvatars.put(id, new EHashPair(hash, ehash));
        }
        this.ownedAvatars = ownedAvatars;
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeInt(ownedAvatars.size());
        for (Map.Entry<String, EHashPair> avatar : ownedAvatars.entrySet()) {
            buf.writeByteArray(avatar.getKey().getBytes(UTF_8));
            buf.writeBytes(avatar.getValue().hash().get());
            buf.writeBytes(avatar.getValue().ehash().get());
        }
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
