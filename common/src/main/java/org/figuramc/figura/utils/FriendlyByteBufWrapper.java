package org.figuramc.figura.utils;

import net.minecraft.network.FriendlyByteBuf;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

import java.util.UUID;

public class FriendlyByteBufWrapper implements IFriendlyByteBuf {
    private final FriendlyByteBuf byteBuf;

    public FriendlyByteBufWrapper(FriendlyByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    @Override
    public IFriendlyByteBuf writeByte(int b) {
        byteBuf.writeByte(b);
        return this;
    }

    @Override
    public IFriendlyByteBuf writeShort(int val) {
        byteBuf.writeShort(val);
        return this;
    }

    @Override
    public IFriendlyByteBuf writeInt(int val) {
        byteBuf.writeInt(val);
        return this;
    }

    @Override
    public IFriendlyByteBuf writeVarInt(int value) {
        byteBuf.writeVarInt(value);
        return this;
    }

    @Override
    public IFriendlyByteBuf writeLong(long val) {
        byteBuf.writeLong(val);
        return this;
    }

    @Override
    public IFriendlyByteBuf writeVarLong(long value) {
        byteBuf.writeVarLong(value);
        return this;
    }

    @Override
    public IFriendlyByteBuf writeUUID(UUID uuid) {
        byteBuf.writeUUID(uuid);
        return this;
    }

    @Override
    public IFriendlyByteBuf writeByteArray(byte[] arr) {
        byteBuf.writeByteArray(arr);
        return this;
    }

    @Override
    public IFriendlyByteBuf writeBytes(byte[] arr) {
        byteBuf.writeBytes(arr);
        return this;
    }

    @Override
    public byte readByte() {
        return byteBuf.readByte();
    }

    @Override
    public byte[] readNBytes(int length) {
        byte[] buf = new byte[length];
        byteBuf.readBytes(buf);
        return buf;
    }

    @Override
    public short readShort() {
        return byteBuf.readShort();
    }

    @Override
    public int readInt() {
        return byteBuf.readInt();
    }

    @Override
    public int readVarInt() {
        return byteBuf.readVarInt();
    }

    @Override
    public long readLong() {
        return byteBuf.readLong();
    }

    @Override
    public long readVarLong() {
        return byteBuf.readVarLong();
    }

    @Override
    public UUID readUUID() {
        return byteBuf.readUUID();
    }

    @Override
    public void readBytes(byte[] out) {
        byteBuf.readBytes(out);
    }

    @Override
    public byte[] readBytes(int len) {
        return readNBytes(len);
    }

    @Override
    public byte[] readBytes() {
        return this.readBytes(readableBytes());
    }

    @Override
    public byte[] readByteArray() {
        return byteBuf.readByteArray();
    }

    @Override
    public byte[] readByteArray(int maxSize) {
        return byteBuf.readByteArray(maxSize);
    }

    @Override
    public Hash readHash() {
        return IFriendlyByteBuf.super.readHash();
    }

    @Override
    public int readerIndex() {
        return byteBuf.readerIndex();
    }

    @Override
    public IFriendlyByteBuf readerIndex(int i) {
        byteBuf.readerIndex(i);
        return this;
    }

    @Override
    public int writerIndex() {
        return byteBuf.writerIndex();
    }

    @Override
    public IFriendlyByteBuf writerIndex(int i) {
        byteBuf.writerIndex(i);
        return this;
    }

    @Override
    public IFriendlyByteBuf setIndex(int reader, int writer) {
        byteBuf.setIndex(reader, writer);
        return this;
    }

    @Override
    public int readableBytes() {
        return byteBuf.readableBytes();
    }

    @Override
    public int writableBytes() {
        return byteBuf.writableBytes();
    }

    @Override
    public int maxWritableBytes() {
        return byteBuf.maxWritableBytes();
    }

    @Override
    public boolean isReadable() {
        return byteBuf.isReadable();
    }

    @Override
    public boolean isReadable(int i) {
        return byteBuf.isReadable(i);
    }

    @Override
    public boolean isWritable() {
        return byteBuf.isWritable();
    }

    @Override
    public boolean isWritable(int i) {
        return byteBuf.isWritable(i);
    }
}
