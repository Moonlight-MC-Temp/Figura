package org.figuramc.figura.server.exceptions;

import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.Utils;

public class HashNotMatchingException extends RuntimeException {
    private final Hash expectedHash;
    private final Hash actualHash;

    public HashNotMatchingException(Hash expectedHash, Hash actualHash) {
        this.expectedHash = expectedHash;
        this.actualHash = actualHash;
    }

    @Override
    public String toString() {
        return "Expected hash %s for avatar data, got %s".formatted(
                expectedHash, actualHash);
    }

    public Hash expectedHash() {
        return expectedHash;
    }

    public Hash actualHash() {
        return actualHash;
    }
}
