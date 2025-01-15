package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.CancellableEvent;
import org.figuramc.figura.server.utils.Hash;

import java.util.UUID;

public class StoreAvatarDataEvent extends CancellableEvent {
    private final byte[] avatarData;
    private final Hash avatarHash;

    public StoreAvatarDataEvent(byte[] avatarData, Hash avatarHash) {
        this.avatarData = avatarData;
        this.avatarHash = avatarHash;
    }

    public byte[] avatarData() {
        return avatarData;
    }

    public Hash avatarHash() {
        return avatarHash;
    }
}
