package org.figuramc.figura.server.utils;

/**
 * Identifier record made for compatibility
 * @param namespace Namespace of identifier
 * @param path Path of identifier
 */
public record Identifier(String namespace, String path) {
    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    public static Identifier parse(String ident) {
        int i = ident.indexOf(':');
        if (i == -1) return new Identifier("minecraft", ident);
        return new Identifier(ident.substring(0, i), ident.substring(i+1));
    }
}