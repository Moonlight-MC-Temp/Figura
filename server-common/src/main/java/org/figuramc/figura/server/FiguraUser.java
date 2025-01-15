package org.figuramc.figura.server;

import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.json.FiguraUserStruct;
import org.figuramc.figura.server.packets.CustomFSBPacket;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.*;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class FiguraUser {
    private final UUID player;
    private boolean online;
    private final PingCounter pingCounter = new PingCounter();
    private final BitSet prideBadges;
    private @Nullable Pair<String, EHashPair> equippedAvatar;

    private final HashMap<String, EHashPair> ownedAvatars;

    public FiguraUser(UUID player, BitSet prideBadges, Pair<String, EHashPair> equippedAvatar, HashMap<String, EHashPair> ownedAvatars) {
        this.player = player;
        this.online = false;
        this.prideBadges = prideBadges;
        this.equippedAvatar = equippedAvatar;
        this.ownedAvatars = ownedAvatars;
    }

    public UUID uuid() {
        return player;
    }

    public boolean online() {
        return online;
    }

    public boolean offline() {
        return !online;
    }

    public PingCounter pingCounter() {
        return pingCounter;
    }

    public BitSet prideBadges() {
        return prideBadges;
    }

    public @Nullable Pair<String, EHashPair> equippedAvatar() {
        return equippedAvatar;
    }

    public HashMap<String, EHashPair> ownedAvatars() {
        return ownedAvatars;
    }

    public void sendPacket(Packet packet) {
        FiguraServer.getInstance().sendPacket(player, packet);
    }

    public void save(Path file) {
        file.getParent().toFile().mkdirs();
        File playerFile = file.toFile();
        try {
            FiguraUserStruct struct = new FiguraUserStruct();
            if (equippedAvatar != null) {
                struct.equippedAvatar = equippedAvatar.left();
                struct.avatarHash = equippedAvatar.right();
            }
            struct.prideBadges = prideBadges;
            struct.ownedAvatars = ownedAvatars;
            FileOutputStream fos = new FileOutputStream(playerFile);
            fos.write(FiguraServer.getInstance().GSON.toJson(struct).getBytes(UTF_8));
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FiguraUser load(UUID player, Path file) {
        file.getParent().toFile().mkdirs();
        File playerFile = file.toFile();
        try {
            FileInputStream fis = new FileInputStream(playerFile);
            String str = new String(fis.readAllBytes(), UTF_8);
            fis.close();
            FiguraUserStruct struct = FiguraServer.getInstance().GSON.fromJson(str, FiguraUserStruct.class);
            Pair<String, EHashPair> avatar = struct.equippedAvatar != null ? new Pair<>(struct.equippedAvatar, struct.avatarHash) : null;
            return new FiguraUser(player, struct.prideBadges, avatar, struct.ownedAvatars);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated(forRemoval = true)
    public static FiguraUser loadByteBuf(UUID player, Path playerFile) {
        try (FileInputStream fis = new FileInputStream(playerFile.toFile())) {
            InputStreamByteBuf buf = new InputStreamByteBuf(fis);
            return loadByteBuf(player, buf);
        } catch (FileNotFoundException e) {
            return new FiguraUser(player, new BitSet(), null, new HashMap<>());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated(forRemoval = true)
    public static FiguraUser loadByteBuf(UUID player, IFriendlyByteBuf buf) {
        int length = buf.readVarInt();
        byte[] arr = buf.readBytes(length);
        BitSet prideBadges = BitSet.valueOf(arr);
        int equippedAvatarsCount = buf.readVarInt();
        Pair<String, EHashPair> equippedAvatar = null;
        for (int i = 0; i < equippedAvatarsCount; i++) {
            String id = new String(buf.readByteArray(256), UTF_8);
            Hash hash = buf.readHash();
            Hash ehash = buf.readHash();
            if (equippedAvatar == null) equippedAvatar = new Pair<>(id, new EHashPair(hash, ehash));
        }
        HashMap<String, EHashPair> ownedAvatars = new HashMap<>();
        int ownedAvatarsCount = buf.readVarInt();
        for (int i = 0; i < ownedAvatarsCount; i++) {
            String id = new String(buf.readByteArray(256), UTF_8);
            Hash hash = buf.readHash();
            Hash ehash = buf.readHash();
            ownedAvatars.put(id, new EHashPair(hash, ehash));
        }
        return new FiguraUser(player, prideBadges, equippedAvatar, ownedAvatars);
    }

    public Hash findEHash(Hash hash) {
        var avatar = equippedAvatar();
        if (avatar != null) {
            var pair = avatar.right();
            if (pair.hash().equals(hash)) return pair.ehash();
        }
        for (EHashPair pair: ownedAvatars.values()) {
            if (pair.hash().equals(hash)) return pair.ehash();
        }
        return null;
    }

    public void update() {

    }

    public void setOnline() {
        online = true;
    }

    public void setOffline() {
        online = false;
    }

    public void removeOwnedAvatar(String avatarId) {
        if (ownedAvatars.containsKey(avatarId)) {
            EHashPair avatar = ownedAvatars.remove(avatarId);
            FiguraServer.getInstance().avatarManager().getAvatarMetadata(avatar.hash()).owners().remove(uuid());
        }
    }

    public void removeEquippedAvatar() {
        if (equippedAvatar != null) {
            FiguraServer.getInstance().avatarManager().getAvatarMetadata(equippedAvatar.right().hash()).equipped().remove(uuid());
            equippedAvatar = null;
        }
    }

    public void replaceOrAddOwnedAvatar(String avatarId, Hash hash, Hash ehash) {
        ownedAvatars.put(avatarId, new EHashPair(hash, ehash));
        FiguraServer.getInstance().avatarManager().getAvatarMetadata(hash).owners().put(uuid(), ehash);
    }

    public void setEquippedAvatar(String avatarId, Hash hash, Hash ehash) {
        equippedAvatar = new Pair<>(avatarId, new EHashPair(hash, ehash));
        FiguraServer.getInstance().avatarManager().getAvatarMetadata(hash).equipped().put(uuid(), ehash);
    }

    public int getAvatarsCountWithId(String avatarId) {
        return ownedAvatars().size() + (ownedAvatars().containsKey(avatarId) ? 0 : 1);
    }

    public void sendFSBPacket(String id, byte[] data) {
        sendPacket(new CustomFSBPacket(id.hashCode(), data));
    }

    public static class PingCounter {
        private int bytesSent; // Amount of total bytes sent in last 20 ticks
        private int pingsSent; // Amount of pings sent in last 20 ticks

        public int bytesSent() {
            return bytesSent;
        }

        public int pingsSent() {
            return pingsSent;
        }

        public void addPing(int size) {
            pingsSent++;
            bytesSent += size;
        }

        public void reset() {
            bytesSent = 0;
            pingsSent = 0;
        }
    }
}
