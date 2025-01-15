package org.figuramc.figura.server.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamByteBuf implements IFriendlyByteBuf {
    private final OutputStream baos;

    public OutputStreamByteBuf(OutputStream baos) {
        this.baos = baos;
    }

    @Override
    public IFriendlyByteBuf writeByte(int b) {
        try {
            baos.write(b);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public byte readByte() {
        throw new UnsupportedOperationException("Read operations are unsupported on BAOSByteBuf");
    }

    @Override
    public int readerIndex() {
        return 0;
    }

    @Override
    public IFriendlyByteBuf readerIndex(int i) {
        throw new UnsupportedOperationException("Unable to set readerIndex on stream");
    }

    @Override
    public int writerIndex() {
        return 0;
    }

    @Override
    public IFriendlyByteBuf writerIndex(int i) {
        throw new UnsupportedOperationException("Unable to set writeIndex on stream");
    }

    @Override
    public IFriendlyByteBuf setIndex(int reader, int writer) {
        throw new UnsupportedOperationException("Unable to set indexes on stream");
    }

    @Override
    public int readableBytes() {
        return 0;
    }

    @Override
    public int writableBytes() {
        return 0;
    }

    @Override
    public int maxWritableBytes() {
        return 0;
    }

    @Override
    public boolean isReadable() {
        return false;
    }

    @Override
    public boolean isReadable(int i) {
        return false;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean isWritable(int i) {
        return true;
    }
}
