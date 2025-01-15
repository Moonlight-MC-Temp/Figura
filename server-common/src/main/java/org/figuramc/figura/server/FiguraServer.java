package org.figuramc.figura.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.avatars.FiguraServerAvatarManager;
import org.figuramc.figura.server.events.Events;
import org.figuramc.figura.server.events.packets.OutcomingPacketEvent;
import org.figuramc.figura.server.json.EHashPairSerializer;
import org.figuramc.figura.server.json.HashSerializer;
import org.figuramc.figura.server.packets.*;
import org.figuramc.figura.server.packets.c2s.*;
import org.figuramc.figura.server.packets.handlers.c2s.*;
import org.figuramc.figura.server.packets.s2c.*;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.server.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;

public abstract class FiguraServer {
    public final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(Hash.class, new HashSerializer())
            .registerTypeAdapter(EHashPair.class, new EHashPairSerializer())
            .create();
    protected static FiguraServer INSTANCE;
    private final FiguraUserManager userManager = new FiguraUserManager(this);
    private final FiguraServerAvatarManager avatarManager = new FiguraServerAvatarManager(this);
    private FiguraServerConfig config = new FiguraServerConfig();
    private final FiguraCustomPackets customPackets = new FiguraCustomPackets();
    protected FiguraServer() {
        if (INSTANCE != null) throw new IllegalStateException("Can't create more than one instance of FiguraServer");
        INSTANCE = this;
    }

    private final HashMap<Identifier, C2SPacketHandler<?>> PACKET_HANDLERS = new HashMap<>() {{
        put(C2SBackendHandshakePacket.PACKET_ID, new C2SHandshakeHandler(FiguraServer.this));
        put(C2SFetchAvatarPacket.PACKET_ID, new C2SFetchAvatarPacketHandler(FiguraServer.this));
        put(C2SFetchUserdataPacket.PACKET_ID, new C2SFetchUserdataPacketHandler(FiguraServer.this));
        put(C2SUploadAvatarPacket.PACKET_ID, new C2SUploadAvatarPacketHandler(FiguraServer.this));
        put(C2SEquipAvatarsPacket.PACKET_ID, new C2SEquipAvatarPacketHandler(FiguraServer.this));
        put(C2SDeleteAvatarPacket.PACKET_ID, new C2SDeleteAvatarPacketHandler(FiguraServer.this));
        put(C2SPingPacket.PACKET_ID, new C2SPingPacketHandler(FiguraServer.this));
        put(CustomFSBPacket.PACKET_ID, new C2SCustomFSBPacketHandler(FiguraServer.this));

        put(AvatarDataPacket.PACKET_ID, new C2SAvatarDataPacketHandler(FiguraServer.this));
    }};

    public void forEachHandler(BiConsumer<Identifier, C2SPacketHandler<?>> consumer) {
        PACKET_HANDLERS.forEach(consumer);
    }

    public static FiguraServer getInstance() {
        return INSTANCE;
    }

    public abstract Path getFiguraFolder();

    public Path getUsersFolder() {
        return getFiguraFolder().resolve("users");
    }

    public Path getAvatarsFolder() {
        return getFiguraFolder().resolve("avatars");
    }

    private Path getConfigFile() {
        return getFiguraFolder().resolve("config.json");
    }

    public FiguraCustomPackets customPackets() {
        return customPackets;
    }

    public Path getAvatar(byte[] hash) {
        return getAvatarsFolder().resolve("%s.nbt".formatted(Utils.hexFromBytes(hash)));
    }

    public Path getAvatarMetadata(byte[] hash) {
        return getAvatarsFolder().resolve("%s.mtd.json".formatted(Utils.hexFromBytes(hash)));
    }

    @Deprecated(forRemoval = true)
    public Path getOldAvatarMetadata(byte[] hash) {
        return getAvatarsFolder().resolve("%s.mtd".formatted(Utils.hexFromBytes(hash)));
    }

    public Path getUserdataFile(UUID user) {
        return getUsersFolder().resolve("%s.pl.json".formatted(Utils.uuidToHex(user)));
    }

    @Deprecated(forRemoval = true)
    public Path getOldUserdataFile(UUID user) {
        return getUsersFolder().resolve("%s.pl".formatted(Utils.uuidToHex(user)));
    }

    public final void init() {
        // TODO: reading config
        getFiguraFolder().toFile().mkdirs();
        loadConfig();
        getUsersFolder().toFile().mkdirs();
        getAvatarsFolder().toFile().mkdirs();
        logInfo("Initialization complete.");
    }

    public static boolean initialized() {
        return INSTANCE != null;
    }

    private void loadConfig() {
        File cfg = getConfigFile().toFile();
        if (cfg.exists()) {
            try (FileInputStream fis = new FileInputStream(cfg)) {
                String configString = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
                config = GSON.fromJson(configString, FiguraServerConfig.class);
                return;
            }
            catch (Exception ignored) {}
        }
        config = new FiguraServerConfig();
        try (FileOutputStream fos = new FileOutputStream(cfg)) {
            String res = GSON.toJson(config);
            fos.write(res.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException ignored) {}
    }

    public final C2SPacketHandler<Packet> getPacketHandler(Identifier id) {
        return (C2SPacketHandler<Packet>) PACKET_HANDLERS.get(id);
    }

    public void close() {
        logInfo("Closing FSB");
        avatarManager.close();
        userManager.close();
        INSTANCE = null;
    }

    public final void tick() {
        avatarManager.tick();
        userManager().tick();
    }

    public final S2CBackendHandshakePacket getHandshake() {
        ArrayList<UUID> connectedUsers = new ArrayList<>();
        userManager.forEachUser(user -> connectedUsers.add(user.uuid()));
        return new S2CBackendHandshakePacket(
                config.pingsRateLimit(),
                config.pingsSizeLimit(),
                config.avatarSizeLimit(),
                config.avatarsCountLimit(),
                connectedUsers
        );
    }

    public FiguraServerConfig config() {
        return config;
    }

    public FiguraUserManager userManager() {
        return userManager;
    }

    public final void sendPacket(UUID receiver, Packet packet) {
        OutcomingPacketEvent event = new OutcomingPacketEvent(receiver, packet);
        Events.call(event);
        if (!event.isCancelled()) {
            sendPacketInternal(receiver, packet);
        }
    }

    protected abstract void sendPacketInternal(UUID receiver, Packet packet);

    public abstract boolean getPermission(UUID player, String permission);
    public abstract void sendMessage(UUID receiver, JsonObject component);

    public FiguraServerAvatarManager avatarManager() {
        return avatarManager;
    }

    public abstract void logInfo(String text);
    public abstract void logError(String text);
    public abstract void logError(String text, Throwable err);
    public abstract void logDebug(String text);
}
