package org.figuramc.figura.server.utils;

import org.figuramc.figura.server.packets.CloseIncomingStreamPacket;

public enum StatusCode {
    FINISHED(100),
    ALREADY_EXISTS(101),

    AVATAR_DOES_NOT_EXIST(200),

    INVALID_STREAM_ID(301),
    MAX_AVATAR_SIZE_EXCEEDED(302),
    INVALID_HASH(303),
    OWNERSHIP_CHECK_ERROR(304),
    TOO_MANY_AVATARS(305),

    UNKNOWN;

    private final short statusCode;

    StatusCode() {
        statusCode = Short.MAX_VALUE;
    }

    StatusCode(int s) {
        statusCode = (short) s;
    }

    public short statusCode() {
        return statusCode;
    }

    public static StatusCode getFromCode(short code) {
        for (StatusCode v : StatusCode.values()) {
            if (v.statusCode == code) return v;
        }
        return UNKNOWN;
    }
}
