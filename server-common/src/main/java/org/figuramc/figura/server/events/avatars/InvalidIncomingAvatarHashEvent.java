package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.Event;
import org.figuramc.figura.server.utils.Hash;

import java.util.List;
import java.util.UUID;

public class InvalidIncomingAvatarHashEvent extends Event {
    private final Hash expectedHash;
    private final Hash receivedHash;

    public InvalidIncomingAvatarHashEvent(Hash expectedHash, Hash receivedHash) {
        this.expectedHash = expectedHash;
        this.receivedHash = receivedHash;
    }

    public Hash expectedHash() {
        return expectedHash;
    }

    public Hash receivedHash() {
        return receivedHash;
    }
}
