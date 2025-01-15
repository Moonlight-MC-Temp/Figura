package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.avatars.FiguraServerAvatarManager;
import org.figuramc.figura.server.events.CancellableEvent;
import org.figuramc.figura.server.utils.Hash;

public class StoreAvatarMetadataEvent extends CancellableEvent {
    private final Hash hash;
    private final FiguraServerAvatarManager.AvatarMetadata metadata;

    public StoreAvatarMetadataEvent(Hash hash, FiguraServerAvatarManager.AvatarMetadata metadata) {
        this.hash = hash;
        this.metadata = metadata;
    }

    public Hash hash() {
        return hash;
    }

    public FiguraServerAvatarManager.AvatarMetadata metadata() {
        return metadata;
    }
}
