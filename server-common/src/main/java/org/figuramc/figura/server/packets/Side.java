package org.figuramc.figura.server.packets;

/**
 * Side that will be processing a packet
 */
public enum Side {
    CLIENT,
    SERVER,
    BOTH;

    public boolean receivedBy(Side side) {
        return this == BOTH || this != side;
    }

    public boolean sentBy(Side side) {
        return this == BOTH || this == side;
    }
}
