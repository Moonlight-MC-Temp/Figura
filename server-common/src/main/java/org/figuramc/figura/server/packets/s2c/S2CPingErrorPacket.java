package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

public class S2CPingErrorPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "s2c/ping/error");

    private final Error error;

    public S2CPingErrorPacket(Error error) {
        this.error = error;
    }

    public S2CPingErrorPacket(IFriendlyByteBuf buf) {
        error = Error.byCode(buf.readByte());
    }

    public Error error() {
        return error;
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeByte(error.code());
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }

    public enum Error {
        PING_SIZE((byte) 0),
        RATE_LIMIT((byte) 1),
        UNKNOWN;

        private final byte code;

        Error(byte code) {
            this.code = code;
        }

        Error() {
            code = 127;
        }

        public byte code() {
            return code;
        }

        public static Error byCode(byte b) {
            for (Error e: Error.values()) {
                if (e.code == b) {
                    return e;
                }
            }
            return UNKNOWN;
        }
    }
}
