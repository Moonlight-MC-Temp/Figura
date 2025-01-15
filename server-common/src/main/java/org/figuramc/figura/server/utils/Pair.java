package org.figuramc.figura.server.utils;

public record Pair<A, B>(A left, B right) {
    public static <A, B> Pair<A, B> of(A left, B right) {
        return new Pair<>(left, right);
    }
}
