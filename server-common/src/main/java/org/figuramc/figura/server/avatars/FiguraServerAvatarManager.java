package org.figuramc.figura.server.avatars;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.events.Events;
import org.figuramc.figura.server.events.avatars.*;
import org.figuramc.figura.server.exceptions.HashNotMatchingException;
import org.figuramc.figura.server.packets.AvatarDataPacket;
import org.figuramc.figura.server.packets.CloseIncomingStreamPacket;
import org.figuramc.figura.server.packets.s2c.S2CInitializeAvatarStreamPacket;
import org.figuramc.figura.server.utils.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public final class FiguraServerAvatarManager {
    private final FiguraServer parent;
    private final IncomingAvatarHandler incomingAvatarHandler = new IncomingAvatarHandler();
    private final HashMap<Hash, AvatarHandle> avatars = new HashMap<>();

    public FiguraServerAvatarManager(FiguraServer parent) {
        this.parent = parent;
    }

    public void sendAvatar(Hash hash, UUID receiver, int streamId) {
        getAvatarHandle(hash).sendTo(receiver, streamId);
    }

    private AvatarHandle getAvatarHandle(Hash hash) {
        return avatars.computeIfAbsent(hash, AvatarHandle::new);
    }

    public synchronized void receiveAvatar(FiguraUser uploader, String avatarId, int streamId, Hash avatarHash, Hash avatarEHash) {
        incomingAvatarHandler.openStream(uploader.uuid(), avatarId, streamId, avatarHash, avatarEHash);
    }

    public void acceptAvatarChunk(FiguraUser uploader, int streamId, byte[] data, boolean finalChunk) {
        incomingAvatarHandler.acceptChunk(uploader.uuid(), streamId, data, finalChunk);
    }

    public boolean avatarExists(Hash hash) {
        var future = Events.call(new AvatarExistenceFetchEvent(hash));
        if (future.returned()) return future.returnValue();
        var file = parent.getAvatar(hash.get()).toFile();
        return file.exists();
    }

    public AvatarMetadata getAvatarMetadata(Hash hash) {
        return getAvatarHandle(hash).getMetadata();
    }

    public void tick() {
        List<AvatarHandle> a = List.copyOf(avatars.values());
        a.forEach(AvatarHandle::tick);
        avatars.entrySet().removeIf((e) -> e.getValue().markedForDeletion);
    }

    public void close() {
        avatars.forEach((hash, handle) -> {
            saveMetadata(hash, handle.metadata);
        });
        avatars.clear();
    }

    public static class AvatarData {
        private int timeWithoutFetching;
        private final byte[] data;

        public AvatarData(byte[] data) {
            this.data = data;
        }

        public byte[] data() {
            timeWithoutFetching = 0;
            return data;
        }

        public void tick() {
            timeWithoutFetching++;
        }
    }
    public static class AvatarMetadata {
        private final HashMap<UUID, Hash> owners;
        private final HashMap<UUID, Hash> equipped;
        private boolean cleanupProtection = false;

        /**
         * Creates empty metadata
         */
        public AvatarMetadata() {
            this.owners = new HashMap<>();
            this.equipped = new HashMap<>();
        }

        /**
         * Creates metadata with avatar owners
         */
        public AvatarMetadata(HashMap<UUID, Hash> owners, HashMap<UUID, Hash> equipped) {
            this.owners = owners;
            this.equipped = equipped;
        }

        /**
         * Map of users who owns this avatar.
         * Avatar will have more than one owner in case if multiple people uploaded the same avatar, so avatar with same hash.
         * @return Map of UUID to EHash
         */
        public synchronized HashMap<UUID, Hash> owners() {
            return owners;
        }

        /**
         * Map of users who has this avatar equipped.
         * Avatar can have more than one user equipping it.
         * @return Map of UUID to EHash
         */
        public synchronized HashMap<UUID, Hash> equipped() {
            return equipped;
        }

        /**
         * If returned value is true, avatar won't be deleted from server cache,
         * even if count of owners and users who equips this avatar will be 0.
         * Otherwise, once avatar is not owned and equipped by anyone, file of this
         * avatar will be deleted from server.
         * @return True if protected from cleanup.
         */
        public boolean cleanupProtection() {
            return cleanupProtection;
        }

        public AvatarMetadata setCleanupProtection(boolean cleanupProtection) {
            this.cleanupProtection = cleanupProtection;
            return this;
        }

        public Hash getOwnerEHash(UUID owner) {
            return owners.getOrDefault(owner, Hash.empty());
        }

        public Hash getEquippedEHash(UUID owner) {
            return owners.get(owner);
        }

        public static AvatarMetadata read(String json) {
            AvatarMetadata metadata = FiguraServer.getInstance().GSON.fromJson(json, AvatarMetadata.class);
            return metadata;
        }


        @Deprecated(forRemoval = true)
        public static AvatarMetadata readOld(byte[] bytes) {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            InputStreamByteBuf byteBuf = new InputStreamByteBuf(bais);

            int ownersCount = byteBuf.readInt();
            HashMap<UUID, Hash> owners = new HashMap<>();
            for (int i = 0; i < ownersCount; i++) {
                UUID owner = byteBuf.readUUID();
                Hash ehash = byteBuf.readHash();
                owners.put(owner, ehash);
            }

            int equippedCount = byteBuf.readInt();
            HashMap<UUID, Hash> equipped = new HashMap<>();
            for (int i = 0; i < equippedCount; i++) {
                UUID owner = byteBuf.readUUID();
                Hash ehash = byteBuf.readHash();
                equipped.put(owner, ehash);
            }

            return new AvatarMetadata(owners, equipped);
        }

        public void write(FileOutputStream fos) throws IOException {
            String data = FiguraServer.getInstance().GSON.toJson(this);
            fos.write(data.getBytes(StandardCharsets.UTF_8));
        }

        public boolean canBeDeleted() {
            return (!cleanupProtection()) && owners.isEmpty() && equipped.isEmpty();
        }
    }

    private static class AvatarOutcomingStream {
        private final UUID receiver;
        private final AvatarData source;
        private final int streamId;
        private final Hash hash;
        private final Hash ehash;
        private int position = 0;

        private AvatarOutcomingStream(UUID receiver, AvatarData source, int streamId, Hash hash, Hash ehash) {
            this.receiver = receiver;
            this.source = source;
            this.streamId = streamId;
            this.hash = hash;
            this.ehash = ehash;
        }

        public void tick() {
            if (!canBeClosed()) {
                var inst = FiguraServer.getInstance();
                if (position == 0) {
                    inst.sendPacket(receiver, new S2CInitializeAvatarStreamPacket(streamId, hash, ehash));
                }
                int chunkSize = nextChunkSize();
                byte[] data = source.data();
                byte[] chunk = new byte[chunkSize];
                System.arraycopy(data, position, chunk, 0, chunk.length);
                position += chunkSize;
                inst.sendPacket(receiver, new AvatarDataPacket(streamId, canBeClosed(), chunk));
            }
        }

        public boolean canBeClosed() {
            return position == source.data().length;
        }

        private int nextChunkSize() {
            return Math.min(AvatarDataPacket.MAX_CHUNK_SIZE, source.data().length - position);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AvatarOutcomingStream stream)) return false;
            return Objects.equals(receiver, stream.receiver) && hash.equals(stream.hash);
        }

        @Override
        public int hashCode() {
            return Objects.hash(receiver, hash);
        }
    }

    private void saveAvatar(Hash avatarHash, byte[] avatarData) {
        if (Events.call(new StoreAvatarDataEvent(avatarData, avatarHash)).isCancelled()) return;
        parent.logInfo("Saving avatar: " + avatarHash);
        var file = parent.getAvatar(avatarHash.get()).toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(avatarData);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveMetadata(Hash avatarHash, AvatarMetadata metadata) {
        if (Events.call(new StoreAvatarMetadataEvent(avatarHash, metadata)).isCancelled()) return;
        var file = parent.getAvatarMetadata(avatarHash.get()).toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            metadata.write(fos);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteAvatar(Hash avatarHash) {
        var f = Events.call(new RemoveAvatarDataEvent(avatarHash));
        if (f.isCancelled()) return;
        parent.getAvatar(avatarHash.get()).toFile().delete();
        parent.getAvatarMetadata(avatarHash.get()).toFile().delete();
        parent.getOldAvatarMetadata(avatarHash.get()).toFile().delete();
    }

    private class AvatarHandle {
        private final Hash hash;
        private AvatarData data;
        private AvatarMetadata metadata;
        private final ArrayList<AvatarOutcomingStream> streams = new ArrayList<>();
        private boolean markedForDeletion = false;

        private AvatarHandle(Hash hash) {
            this.hash = hash;
        }

        private void sendTo(UUID receiver, int streamId) {
            streams.add(new AvatarOutcomingStream(receiver, getAvatarData(), streamId,
                    hash, getMetadata().getOwnerEHash(receiver)));
        }

        private AvatarData getAvatarData() {
            if (data == null) {
                data = loadAvatar();
            }
            return data;
        }

        private AvatarData loadAvatar() {
            var event = Events.call(new StartLoadingAvatarEvent(hash));
            if (event.returned()) checkAndFinishLoadingAvatar(event.returnValue());

            var inst = FiguraServer.getInstance();
            Path avatarFile = inst.getAvatar(hash.get());
            try {
                FileInputStream fis = new FileInputStream(avatarFile.toFile());
                byte[] data = fis.readAllBytes();
                fis.close();
                return checkAndFinishLoadingAvatar(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private AvatarData checkAndFinishLoadingAvatar(byte[] data) {
            Hash hash = Utils.getHash(data);
            if (!hash.equals(this.hash)) throw new HashNotMatchingException(this.hash, hash);
            return new AvatarData(data);
        }

        private AvatarMetadata getMetadata() {
            if (metadata == null) {
                metadata = loadMetadata();
            }
            return metadata;
        }

        private AvatarMetadata loadMetadata() {
            var event = Events.call(new StartLoadingMetadataEvent(hash));
            if (event.returned()) return event.returnValue();

            var inst = FiguraServer.getInstance();
            Path avatarFile = inst.getAvatarMetadata(hash.get());
            try {
                FileInputStream fis = new FileInputStream(avatarFile.toFile());
                byte[] data = fis.readAllBytes();
                fis.close();
                return AvatarMetadata.read(new String(data, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void tick() {
            var data = getAvatarData();
            var metadata = getMetadata();

            data.tick();
            if (metadata.canBeDeleted()) {
                deleteAvatar(hash);
                markedForDeletion = true;
            }
            streams.forEach(AvatarOutcomingStream::tick);
        }
    }

    private class IncomingAvatarHandler {
        private final HashMap<Hash, ArrayList<IncomingAvatarKey>> hashesToUploads = new HashMap<>();
        private final HashMap<IncomingAvatarKey, AvatarIncomingStream> streams = new HashMap<>();

        private void openStream(UUID uploader, String avatarId, int streamId, Hash avatarHash, Hash avatarEHash) {
            var key = new IncomingAvatarKey(uploader, streamId);
            streams.put(new IncomingAvatarKey(uploader, streamId),
                    new AvatarIncomingStream(uploader, streamId, avatarId, avatarHash, avatarEHash)
            );
            hashesToUploads.computeIfAbsent(avatarHash, h -> new ArrayList<>()).add(key);
        }

        public void acceptChunk(UUID uuid, int streamId, byte[] data, boolean finalChunk) {
            var key = new IncomingAvatarKey(uuid, streamId);
            if (!streams.containsKey(key)) {
                parent.sendPacket(uuid, new CloseIncomingStreamPacket(streamId, StatusCode.INVALID_STREAM_ID));
                return;
            }

            var s = streams.get(key);
            if (s.acceptDataChunk(data, finalChunk)) {
                streams.entrySet().removeIf(e -> e.getValue().isFinished());
            }
        }

        private class AvatarIncomingStream {
            private final LinkedList<byte[]> dataChunks = new LinkedList<>();
            private final UUID uploader;
            private final int streamId;
            private final String avatarId;
            private final Hash hash;
            private final Hash ehash;
            private int size = 0;
            private boolean finished;

            private AvatarIncomingStream(UUID uploader, int streamId, String avatarId, Hash hash, Hash ehash) {
                this.uploader = uploader;
                this.streamId = streamId;
                this.avatarId = avatarId;
                this.hash = hash;
                this.ehash = ehash;
            }

            // If this function returns true, it closes the stream and removes it from IncomingAvatarHandler
            private boolean acceptDataChunk(byte[] chunk, boolean finalChunk) {
                size += chunk.length;
                // In case if avatar size is exceeded - closing the stream and removing it from handler.
                if (size > parent.config().avatarSizeLimit() &&
                    !Events.call(new AvatarUploadSizeExceedEvent(uploader, size)).isCancelled()) {
                    close(StatusCode.MAX_AVATAR_SIZE_EXCEEDED);
                    return true;
                }
                dataChunks.add(chunk);


                if (finalChunk) {
                    // Collecting all data chunks in one array
                    int offset = 0;
                    byte[] avatarData = new byte[size];
                    for (byte[] dataChunk: dataChunks) {
                        System.arraycopy(dataChunk, 0, avatarData, offset, dataChunk.length);
                        offset += dataChunk.length;
                    }
                    Hash dataHash = Utils.getHash(avatarData);
                    // In case if resulting data hash doesn't match - rejecting it.
                    // Closing this stream is not required as client should've done it by itself
                    if (!dataHash.equals(hash)) {
                        Events.call(new InvalidIncomingAvatarHashEvent(hash, dataHash));
                        parent.sendPacket(uploader, new CloseIncomingStreamPacket(streamId, StatusCode.INVALID_HASH));
                        return true;
                    }

                    // Writing avatar data
                    saveAvatar(hash, avatarData);

                    // Creating a new avatar handle
                    var avatarHandle = getAvatarHandle(hash);
                    avatarHandle.data = new AvatarData(avatarData);

                    // Creating empty metadata for this avatar with all the avatar owners
                    AvatarMetadata metadata = new AvatarMetadata();
                    for (IncomingAvatarKey key: hashesToUploads.get(hash)) {
                        var stream = streams.get(key);
                        metadata.owners.put(stream.uploader, stream.ehash);
                    }

                    // Writing avatar metadata
                    saveMetadata(hash, metadata);

                    avatarHandle.metadata = metadata;

                    // Finishing work of all streams
                    for (IncomingAvatarKey key: hashesToUploads.get(hash)) {
                        var stream = streams.get(key);
                        stream.finish();
                    }

                    // Removing keys from hash map
                    hashesToUploads.remove(hash);

                    return true;
                }

                return false;
            }

            private void close(StatusCode code) {
                parent.sendPacket(uploader, new CloseIncomingStreamPacket(streamId, code));
            }

            private void finish() {
                parent.userManager().getUser(uploader).replaceOrAddOwnedAvatar(avatarId, hash, ehash);
                finished = true;
                close(StatusCode.FINISHED);
            }

            private boolean isFinished() {
                return finished;
            }
        }

    }

    private record IncomingAvatarKey(UUID uploader, int streamId) {}
}
