package org.figuramc.figura.lua.api.data;
<<<<<<< HEAD

import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@LuaWhitelist
@LuaTypeDoc(name = "InputStream", value = "input_stream")
public class FiguraInputStream extends InputStream {
    private final InputStream sourceStream;
    private final boolean asyncOnly;
    private final Avatar parent;
    public FiguraInputStream(Avatar parent, InputStream sourceStream) {
        this(parent, sourceStream, false);
    }

    public FiguraInputStream(Avatar parent, InputStream sourceStream, boolean asyncOnly) {
        this.sourceStream = sourceStream;
        this.asyncOnly = asyncOnly;
        this.parent = parent;
        parent.openInputStreams.add(this);
    }

    @Override
    @LuaWhitelist
    @LuaMethodDoc("input_stream.read")
    public int read() {
        try {
            if (asyncOnly) throw new IOException("This stream supports only async read");
            return sourceStream.read();
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("input_stream.read_async")
    public FiguraFuture<LuaString> readAsync(Integer limit) {
        final int finalLimit = limit != null ? limit : available();
        // Future handle that will be returned
        FiguraFuture<LuaString> future = new FiguraFuture<>(parent);
        // Calling an async read that will be put in a future results
        CompletableFuture.supplyAsync(() -> {
            try {
                byte[] buf = new byte[finalLimit];
                int len = sourceStream.read(buf);
                // If nothing is read - returning an empty string
                if (len == -1) return LuaString.valueOf("");
                // Resizing a buffer if read length is less than expected
                if (len < buf.length) buf = Arrays.copyOf(buf, len);
                // Returning a string byte array
                return LuaString.valueOf(buf);
            } catch (IOException e) {
                throw new LuaError(e);
            }
        }).whenCompleteAsync(future::complete);
        return future;
    }

    @Override
    @LuaWhitelist
    @LuaMethodDoc(
            value = "input_stream.skip",
            overloads = @LuaMethodOverload(
                    argumentNames = "n",
                    argumentTypes = Long.class,
                    returnType = Long.class
            )
    )
    public long skip(long n) throws IOException {
        return sourceStream.skip(n);
    }

    @Override
    @LuaWhitelist
    @LuaMethodDoc("input_stream.available")
    public int available() {
        try {
            return sourceStream.available();
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @Override
    @LuaWhitelist
    @LuaMethodDoc("input_stream.close")
    public void close() throws IOException {
        sourceStream.close();
        parent.openInputStreams.remove(this);
    }

    @Override
    @LuaWhitelist
    @LuaMethodDoc(
            value = "input_stream.mark",
            overloads = @LuaMethodOverload(
                    argumentNames = "readLimit",
                    argumentTypes = Integer.class
            )
    )
    public void mark(int readlimit) {
        sourceStream.mark(readlimit);
    }

    @Override
    @LuaWhitelist
    @LuaMethodDoc("input_stream.reset")
    public void reset() throws IOException {
        sourceStream.reset();
    }

    @Override
    @LuaWhitelist
    @LuaMethodDoc("input_stream.mark_supported")
    public boolean markSupported() {
        return sourceStream.markSupported();
    }

    @LuaWhitelist
    @LuaMethodDoc("input_stream.is_async_only")
    public boolean isAsyncOnly() {
        return asyncOnly;
    }

    @Override
    @LuaWhitelist
    @LuaMethodDoc(
            value = "input_stream.transfer_to",
            overloads = @LuaMethodOverload(
                    argumentNames = "out",
                    argumentTypes = FiguraOutputStream.class,
                    returnType = Long.class
            )
    )
    public long transferTo(OutputStream out) throws IOException {
        return sourceStream.transferTo(out);
    }

    @Override
    public String toString() {
        return "InputStream";
    }
}
