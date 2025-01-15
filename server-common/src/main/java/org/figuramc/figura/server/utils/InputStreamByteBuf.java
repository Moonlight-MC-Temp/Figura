package org.figuramc.figura.server.utils;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamByteBuf implements IFriendlyByteBuf{
    private final InputStream is;

    public InputStreamByteBuf(InputStream is) {
        this.is = is;
    }

    @Override
    public IFriendlyByteBuf writeByte(int b) {
        throw new UnsupportedOperationException("Unable to write to input stream");
    }

    @Override
    public byte readByte() {
        try {
            return (byte) is.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int readerIndex() {
        return 0;
    }

    @Override
    public IFriendlyByteBuf readerIndex(int i) {
        throw new UnsupportedOperationException("Unable to set index for a stream");
    }

    @Override
    public int writerIndex() {
        return 0;
    }

    @Override
    public IFriendlyByteBuf writerIndex(int i) {
        throw new UnsupportedOperationException("Unable to set index for a stream");
    }

    @Override
    public IFriendlyByteBuf setIndex(int reader, int writer) {
        throw new UnsupportedOperationException("Unable to set index for a stream");
    }

    @Override
    public int readableBytes() {
        try {
            return is.available();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        return true;
    }

    @Override
    public boolean isReadable(int i) {
        return true;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isWritable(int i) {
        return false;
    }


}
