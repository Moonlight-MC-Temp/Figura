package org.figuramc.figura.server.utils;

import java.util.Arrays;
import java.util.Objects;

public class Hash {
    private final byte[] hash;

    public Hash(byte[] hash) {
        if (hash.length != 32) throw new IllegalArgumentException("Invalid hash length");
        this.hash = hash;
    }

    public byte[] get() {
        return Utils.copyBytes(hash);
    }

    @Override
    public String toString() {
        return Utils.hexFromBytes(hash);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hash hash1)) return false;
        return Arrays.equals(hash, hash1.hash);
    }

    public static Hash empty() {
        return new Hash(new byte[32]);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(hash);
    }
}
