package org.figuramc.figura.server.utils;

import java.util.UUID;

/**
 * Platform specific FriendlyByteBuf implementation.
 */
public interface IFriendlyByteBuf {
    IFriendlyByteBuf writeByte(int b);
    default IFriendlyByteBuf writeShort(int val) {
        if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
            short s = (short) (int) val;
            writeByte((s >> 8) & 0xFF);
            writeByte(s & 0xFF);
            return this;
        }
        else throw new IllegalArgumentException("Value %s is out of range [%s; %s]".formatted(val, Short.MIN_VALUE, Short.MAX_VALUE));
    }
    default IFriendlyByteBuf writeInt(int val) {
        writeByte((val >> 24) & 0xFF);
        writeByte((val >> 16) & 0xFF);
        writeByte((val >> 8) & 0xFF);
        writeByte(val & 0xFF);
        return this;
    }
    default IFriendlyByteBuf writeVarInt(int value) {
        while((value & -128) != 0) {
            this.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        this.writeByte(value);
        return this;
    }
    default IFriendlyByteBuf writeLong(long val) {
        writeByte((int) ((val >> 56) & 0xFF));
        writeByte((int) ((val >> 48) & 0xFF));
        writeByte((int) ((val >> 40) & 0xFF));
        writeByte((int) ((val >> 32) & 0xFF));
        writeByte((int) ((val >> 24) & 0xFF));
        writeByte((int) ((val >> 16) & 0xFF));
        writeByte((int) ((val >> 8) & 0xFF));
        writeByte((int) (val & 0xFF));
        return this;
    }
    default IFriendlyByteBuf writeVarLong(long value) {
        while((value & -128L) != 0L) {
            this.writeByte((int)(value & 127L) | 128);
            value >>>= 7;
        }

        this.writeByte((int)value);
        return this;
    }
    default IFriendlyByteBuf writeUUID(UUID uuid) {
        writeLong(uuid.getMostSignificantBits());
        writeLong(uuid.getLeastSignificantBits());
        return this;
    }
    default IFriendlyByteBuf writeByteArray(byte[] arr) {
        writeVarInt(arr.length);
        writeBytes(arr);
        return this;
    }
    default IFriendlyByteBuf writeBytes(byte[] arr) {
        for (int i = 0; i < arr.length; i++) {
            writeByte(arr[i] & 0xFF);
        }
        return this;
    }

    byte readByte();
    default byte[] readNBytes(int length) {
        byte[] buf = new byte[length];
        readBytes(buf);
        return buf;
    }
    default short readShort() {
        byte[] bytes = readNBytes(2);
        short v = 0;
        for (int i = 0; i < bytes.length; i++) {
            v |= (short) ((bytes[((bytes.length - 1) - i)] & 0xFF) << (i * 8));
        }
        return v;
    }
    default int readInt() {
        byte[] bytes = readNBytes(4);
        int v = 0;
        for (int i = 0; i < bytes.length; i++) {
            v |= (bytes[((bytes.length - 1) - i)] & 0xFF) << (i * 8);
        }
        return v;
    }
    default int readVarInt() {
        int i = 0;
        int j = 0;

        byte b;
        do {
            b = this.readByte();
            i |= (b & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while((b & 128) == 128);

        return i;
    }
    default long readLong() {
        byte[] bytes = readNBytes(8);
        long v = 0;
        for (int i = 0; i < bytes.length; i++) {
            v |= (long) (bytes[((bytes.length - 1) - i)] & 0xFF) << (i * 8);
        }
        return v;
    }
    default long readVarLong() {
        long l = 0L;
        int i = 0;

        byte b;
        do {
            b = this.readByte();
            l |= (long)(b & 127) << i++ * 7;
            if (i > 10) {
                throw new RuntimeException("VarLong too big");
            }
        } while((b & 128) == 128);

        return l;
    }
    default UUID readUUID() {
        return new UUID(readLong(), readLong());
    }
    default void readBytes(byte[] out) {
        for (int i = 0; i < out.length; i++) {
            out[i] = readByte();
        }
    }
    default byte[] readBytes(int len) {
        byte[] b = new byte[len];
        readBytes(b);
        return b;
    }
    default byte[] readBytes() {
        return readBytes(readableBytes());
    }
    default byte[] readByteArray() {
        return readByteArray(readableBytes());
    }
    default byte[] readByteArray(int maxSize) {
        int len = readVarInt();
        if (len > maxSize) {
            throw new IllegalStateException("Array length (%s) is longer than %s".formatted(len, maxSize));
        }
        else if (len < 0) {
            throw new IllegalStateException("Array length can't be negative");
        }
        byte[] arr = new byte[len];
        readBytes(arr);
        return arr;
    }
    default Hash readHash() {
        byte[] buffer = new byte[32];
        readBytes(buffer);
        return new Hash(buffer);
    }

    int readerIndex();
    IFriendlyByteBuf readerIndex(int i);
    int writerIndex();
    IFriendlyByteBuf writerIndex(int i);
    IFriendlyByteBuf setIndex(int reader, int writer);

    int readableBytes();
    int writableBytes();
    int maxWritableBytes();

    boolean isReadable();
    boolean isReadable(int i);
    boolean isWritable();
    boolean isWritable(int i);
}