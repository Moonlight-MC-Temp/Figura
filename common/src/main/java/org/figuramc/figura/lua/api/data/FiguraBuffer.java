package org.figuramc.figura.lua.api.data;

import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.permissions.Permissions;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Stack;

@LuaWhitelist
@LuaTypeDoc(value = "buffer", name = "Buffer")
public class FiguraBuffer implements AutoCloseable {
    private static final int CAPACITY_INCREASE_STEP = 512;
    private final Avatar parent;
    private int length = 0, position = 0;
    private byte[] buf;
    private boolean isClosed;

    public FiguraBuffer(Avatar parent) {
        this.parent = parent;
        if (parent.openBuffers.size() > getMaxBuffersCount()) {
            parent.noPermissions.add(Permissions.BUFFERS_COUNT);
            throw new LuaError("You have exceed the max amount of open buffers");
        }
        if (CAPACITY_INCREASE_STEP > getMaxCapacity())  {
            parent.noPermissions.add(Permissions.BUFFER_SIZE);
            throw new LuaError("Unable to create buffer because max capacity is less than default buffer size (512)");
        }
        buf = new byte[CAPACITY_INCREASE_STEP];
        parent.openBuffers.add(this);
    }

    public FiguraBuffer(Avatar parent, int cap) {
        this.parent = parent;
        if (cap > getMaxCapacity()) {
            parent.noPermissions.add(Permissions.BUFFER_SIZE);
            throw new LuaError("Unable to create a buffer with capacity %s");
        }
        buf = new byte[cap];
    }

    private void ensureBufCapacity(int cap) {
        if (cap > getMaxCapacity())
            throw new LuaError("Can't increase this buffer capacity to %s, max capacity is %s"
                    .formatted(cap, getMaxCapacity()));
        if (cap > buf.length) {
            buf = Arrays.copyOf(buf, Math.min(buf.length+CAPACITY_INCREASE_STEP, getMaxCapacity()));
        }
    }

    private byte[] readNBytes(int count) {
        byte[] arr = new byte[Math.min(count, available())];
        int b;
        int i = 0;
        for (; i < count && (b = this.read()) != -1; i++) {
            arr[i] = (byte) b;
        }
        if (i < count) {
            byte[] newArr = new byte[i];
            System.arraycopy(arr, 0, newArr ,0, i);
            arr = newArr;
        }
        return arr;
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.read")
    public int read() {
        checkIsClosed();
        if (position == length) {
            return -1;
        }
        int v = buf[position] & 0xff;
        position++;
        return v;
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.read_short")
    public int readShort() {
        checkIsClosed();
        byte[] bytes = readNBytes(2);
        short v = 0;
        for (int i = 0; i < bytes.length; i++) {
            v |= (short) ((bytes[((bytes.length - 1) - i)] & 0xFF) << (i * 8));
        }
        return v;
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.read_ushort")
    public int readUShort() {
        checkIsClosed();
        byte[] bytes = readNBytes(2);
        int v = 0;
        for (int i = 0; i < bytes.length; i++) {
            v |= (bytes[((bytes.length - 1) - i)] & 0xFF) << (i * 8);
        }
        return v;
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.read_int")
    public int readInt() {
        checkIsClosed();
        byte[] bytes = readNBytes(4);
        int v = 0;
        for (int i = 0; i < bytes.length; i++) {
            v |= (bytes[((bytes.length - 1) - i)] & 0xFF) << (i * 8);
        }
        return v;
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.read_long")
    public long readLong() {
        checkIsClosed();
        byte[] bytes = readNBytes(8);
        long v = 0;
        for (int i = 0; i < bytes.length; i++) {
            v |= (long) (bytes[((bytes.length - 1) - i)] & 0xFF) << (i * 8);
        }
        return v;
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.read_float")
    public float readFloat() {
        checkIsClosed();
        return Float.intBitsToFloat(readInt());
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.read_double")
    public double readDouble() {
        checkIsClosed();
        return Double.longBitsToDouble(readLong());
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.read_short_le")
    public int readShortLE() {
        checkIsClosed();
        byte[] bytes = readNBytes(2);
        short v = 0;
        for (int i = 0; i < bytes.length; i++) {
            v |= (short) ((bytes[i] & 0xFF) << (i * 8));
        }
        return v;
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.read_ushort_le")
    public int readUShortLE() {
        checkIsClosed();
        byte[] bytes = readNBytes(2);
        int v = 0;
        for (int i = 0; i < bytes.length; i++) {
            v |= (bytes[i] & 0xFF) << (i * 8);
        }
        return v;
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.read_int_le")
    public int readIntLE() {
        checkIsClosed();
        byte[] bytes = readNBytes(4);
        int v = 0;
        for (int i = 0; i < bytes.length; i++) {
            v |= (bytes[i] & 0xFF) << (i * 8);
        }
        return v;
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.read_long_le")
    public long readLongLE() {
        checkIsClosed();
        byte[] bytes = readNBytes(8);
        long v = 0;
        for (int i = 0; i < bytes.length; i++) {
            v |= (long) (bytes[i] & 0xFF) << (i * 8);
        }
        return v;
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.read_float_le")
    public float readFloatLE() {
        checkIsClosed();
        return Float.intBitsToFloat(readIntLE());
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.read_double_le")
    public double readDoubleLE() {
        checkIsClosed();
        return Double.longBitsToDouble(readLongLE());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.read_string",
            overloads = {
                    @LuaMethodOverload(
                            returnType = String.class
                    ),
                    @LuaMethodOverload(
                            argumentNames = "length",
                            argumentTypes = Integer.class,
                            returnType = String.class
                    ),
                    @LuaMethodOverload(
                            argumentNames = {"length", "encoding"},
                            argumentTypes = {Integer.class, String.class},
                            returnType = String.class
                    )
            }
    )
    public String readString(Integer length, String encoding) {
        checkIsClosed();
        length = length == null ? available() : Math.max(length, 0);
        Charset charset = encoding == null ? StandardCharsets.UTF_8 : switch (encoding.toLowerCase(Locale.US)) {
            case "utf_16", "utf16" -> StandardCharsets.UTF_16;
            case "utf_16be", "utf16be" -> StandardCharsets.UTF_16BE;
            case "utf_16le", "utf16le" -> StandardCharsets.UTF_16LE;
            case "ascii" -> StandardCharsets.US_ASCII;
            case "iso_8859_1", "iso88591" -> StandardCharsets.ISO_8859_1;
            default -> StandardCharsets.UTF_8;
        };
        byte[] strBuf = readNBytes(length);
        return new String(strBuf, charset);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.read_base_64",
            overloads = {
                    @LuaMethodOverload(
                            returnType = String.class
                    ),
                    @LuaMethodOverload(
                            argumentNames = "length",
                            argumentTypes = Integer.class,
                            returnType = String.class
                    )
            }
    )
    public String readBase64(Integer length) {
        length = length == null ? available() : Math.max(length, 0);
        byte[] strBuf = readNBytes(length);
        return Base64.getEncoder().encodeToString(strBuf);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.read_byte_array",
            overloads = {
                    @LuaMethodOverload(
                            returnType = String.class
                    ),
                    @LuaMethodOverload(
                            argumentNames = "length",
                            argumentTypes = Integer.class,
                            returnType = String.class
                    )
            }
    )
    public LuaString readByteArray(Integer length) {
        length = length == null ? available() : Math.max(length, 0);
        byte[] strBuf = readNBytes(length);
        return LuaString.valueOf(strBuf);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_byte",
            overloads = @LuaMethodOverload(
                    argumentNames = "val",
                    argumentTypes = Integer.class
            )
    )
    public void write(@LuaNotNil int val) {
        checkIsClosed();
        if (position == length) {
            length++;
            ensureBufCapacity(length);
        }
        buf[position] = (byte) (val & 0xFF);
        position++;
    }

    public void writeBytes(byte[] bytes) {
        for (byte aByte : bytes) {
            write(aByte & 0xff);
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_short",
            overloads = @LuaMethodOverload(
                    argumentNames = "val",
                    argumentTypes = Integer.class
            )
    )
    public void writeShort(@LuaNotNil Integer val) {
        checkIsClosed();
        if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
            short s = (short) (int) val;
            write((s >> 8) & 0xFF);
            write(s & 0xFF);
        }
        else throw new LuaError("Value %s is out of range [%s; %s]".formatted(val, Short.MIN_VALUE, Short.MAX_VALUE));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_ushort",
            overloads = @LuaMethodOverload(
                    argumentNames = "val",
                    argumentTypes = Integer.class
            )
    )
    public void writeUShort(@LuaNotNil Integer val) {
        checkIsClosed();
        if (val >= 0 && val <= Character.MAX_VALUE) {
            char s = (char) (int) val;
            write((s >> 8) & 0xFF);
            write(s & 0xFF);
        }
        else throw new LuaError("Value %s is out of range [%s; %s]".formatted(val, 0, (int) Character.MAX_VALUE));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_int",
            overloads = @LuaMethodOverload(
                    argumentNames = "val",
                    argumentTypes = Integer.class
            )
    )
    public void writeInt(@LuaNotNil Integer val) {
        checkIsClosed();
        write((val >> 24) & 0xFF);
        write((val >> 16) & 0xFF);
        write((val >> 8) & 0xFF);
        write(val & 0xFF);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_long",
            overloads = @LuaMethodOverload(
                    argumentNames = "val",
                    argumentTypes = Long.class
            )
    )
    public void writeLong(@LuaNotNil Long val) {
        checkIsClosed();
        write((int) ((val >> 56) & 0xFF));
        write((int) ((val >> 48) & 0xFF));
        write((int) ((val >> 40) & 0xFF));
        write((int) ((val >> 32) & 0xFF));
        write((int) ((val >> 24) & 0xFF));
        write((int) ((val >> 16) & 0xFF));
        write((int) ((val >> 8) & 0xFF));
        write((int) (val & 0xFF));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_float",
            overloads = @LuaMethodOverload(
                    argumentNames = "val",
                    argumentTypes = Float.class
            )
    )
    public void writeFloat(@LuaNotNil Float val) {
        checkIsClosed();
        writeInt(Float.floatToIntBits(val));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_double",
            overloads = @LuaMethodOverload(
                    argumentNames = "val",
                    argumentTypes = Double.class
            )
    )
    public void writeDouble(@LuaNotNil Double val) {
        checkIsClosed();
        writeLong(Double.doubleToLongBits(val));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_short_le",
            overloads = @LuaMethodOverload(
                    argumentNames = "val",
                    argumentTypes = Integer.class
            )
    )
    public void writeShortLE(@LuaNotNil Integer val) {
        checkIsClosed();
        if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
            short s = (short) (int) val;
            write(s & 0xFF);
            write((s >> 8) & 0xFF);
        }
        else throw new LuaError("Value %s is out of range [%s; %s]".formatted(val, Short.MIN_VALUE, Short.MAX_VALUE));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_ushort_le",
            overloads = @LuaMethodOverload(
                    argumentNames = "val",
                    argumentTypes = Integer.class
            )
    )
    public void writeUShortLE(@LuaNotNil Integer val) {
        checkIsClosed();
        if (val >= 0 && val <= Character.MAX_VALUE) {
            char s = (char) (int) val;
            write(s & 0xFF);
            write((s >> 8) & 0xFF);
        }
        else throw new LuaError("Value %s is out of range [%s; %s]".formatted(val, 0, (int) Character.MAX_VALUE));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_int_le",
            overloads = @LuaMethodOverload(
                    argumentNames = "val",
                    argumentTypes = Integer.class
            )
    )
    public void writeIntLE(@LuaNotNil Integer val) {
        checkIsClosed();
        write(val & 0xFF);
        write((val >> 8) & 0xFF);
        write((val >> 16) & 0xFF);
        write((val >> 24) & 0xFF);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_long_le",
            overloads = @LuaMethodOverload(
                    argumentNames = "val",
                    argumentTypes = Long.class
            )
    )
    public void writeLongLE(@LuaNotNil Long val) {
        checkIsClosed();
        write((int) (val & 0xFF));
        write((int) ((val >> 8) & 0xFF));
        write((int) ((val >> 16) & 0xFF));
        write((int) ((val >> 24) & 0xFF));
        write((int) ((val >> 32) & 0xFF));
        write((int) ((val >> 40) & 0xFF));
        write((int) ((val >> 48) & 0xFF));
        write((int) ((val >> 56) & 0xFF));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_float_le",
            overloads = @LuaMethodOverload(
                    argumentNames = "val",
                    argumentTypes = Float.class
            )
    )
    public void writeFloatLE(@LuaNotNil Float val) {
        checkIsClosed();
        writeIntLE(Float.floatToIntBits(val));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_double_le",
            overloads = @LuaMethodOverload(
                    argumentNames = "val",
                    argumentTypes = Double.class
            )
    )
    public void writeDoubleLE(@LuaNotNil Double val) {
        checkIsClosed();
        writeLongLE(Double.doubleToLongBits(val));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_string",
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "val",
                            returnType = Integer.class
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, String.class},
                            argumentNames = {"val", "encoding"},
                            returnType = Integer.class
                    )
            }
    )
    public int writeString(@LuaNotNil String val, String encoding) {
        checkIsClosed();
        Charset charset = encoding == null ? StandardCharsets.UTF_8 : switch (encoding.toLowerCase(Locale.US)) {
            case "utf_16", "utf16" -> StandardCharsets.UTF_16;
            case "utf_16be", "utf16be" -> StandardCharsets.UTF_16BE;
            case "utf_16le", "utf16le" -> StandardCharsets.UTF_16LE;
            case "ascii" -> StandardCharsets.US_ASCII;
            case "iso_8859_1", "iso88591" -> StandardCharsets.ISO_8859_1;
            default -> StandardCharsets.UTF_8;
        };
        byte[] strBytes = val.getBytes(charset);
        writeBytes(strBytes);
        return strBytes.length;
    }
    
    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_base_64",
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "base64",
                            returnType = Integer.class
                    )
            }
    )
    public int writeBase64(@LuaNotNil String base64String) {
        checkIsClosed();
        byte[] base64Bytes = Base64.getDecoder().decode(base64String);
        writeBytes(base64Bytes);
        return base64Bytes.length;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_byte_array",
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "array",
                            returnType = Integer.class
                    )
            }
    )
    public int writeByteArray(@LuaNotNil LuaValue val) {
        checkIsClosed();
        if (!(val instanceof LuaString byteArray)) {
            throw new LuaError("Expected string, got %s".formatted(val.typename()));
        }
        else {
            byte[] bytes = new byte[byteArray.length()];
            byteArray.copyInto(0, bytes, 0, bytes.length);
            writeBytes(bytes);
            return bytes.length;
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.get_length")
    public int getLength() {
        checkIsClosed();
        return length;
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.get_position")
    public int getPosition() {
        checkIsClosed();
        return position;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.set_position",
            overloads = @LuaMethodOverload(
                    argumentNames = "position",
                    argumentTypes = Integer.class
            )
    )
    public void setPosition(@LuaNotNil Integer position) {
        checkIsClosed();
        this.position = Math.max(Math.min(position, length), 0);
    }

    @LuaWhitelist
    @LuaMethodDoc("clear")
    public void clear() {
        checkIsClosed();
        position = 0;
        length = 0;
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.available")
    public int available() {
        checkIsClosed();
        return length-position;
    }

    @LuaWhitelist
    @LuaMethodDoc("buffer.get_max_capacity")
    public int getMaxCapacity() {
        return parent.permissions.get(Permissions.BUFFER_SIZE);
    }

    private int getMaxBuffersCount() {
        return parent.permissions.get(Permissions.BUFFERS_COUNT);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.read_from_stream",
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {FiguraInputStream.class, Integer.class},
                            argumentNames = {"stream", "amount"},
                            returnType = Integer.class
                    )
            }
    )
    public int readFromStream(@LuaNotNil FiguraInputStream stream, Integer amount) {
        checkIsClosed();
        if (amount == null) amount = getMaxCapacity()-position;
        else amount = Math.max(Math.min(amount, getMaxCapacity()-position), 0);
        int i = 0;
        for (; i < amount; i++) {
            int b = stream.read();
            if (b == -1) break;
            write(b);
        }
        return i;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "buffer.write_to_stream",
            overloads = {
                    @LuaMethodOverload(
                          argumentTypes = {FiguraOutputStream.class, Integer.class},
                          argumentNames = {"stream", "amount"},
                          returnType = Integer.class
                    )
            }
    )
    public int writeToStream(@LuaNotNil FiguraOutputStream stream, Integer amount) {
        checkIsClosed();
        if (amount == null) amount = available();
        else amount = Math.max(Math.min(amount, available()), -1);
        for (int i = 0; i < amount; i++) {
            stream.write(read());
        }
        return amount;
    }

    @Override
    @LuaWhitelist
    @LuaMethodDoc(value = "buffer.close")
    public void close() {
        baseClose();
        parent.openBuffers.remove(this);
    }

    public void baseClose() {
        if (!isClosed) {
            isClosed = true;
            buf = null;
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(value = "buffer.is_closed")
    public boolean isClosed() {
        return isClosed;
    }

    private void checkIsClosed() {
        if (isClosed) throw new LuaError("This byte buffer is closed and cant be used anymore");
    }

    @Override
    public String toString() {
        return "Buffer";
    }

    public FiguraBufferInputStream asInputStream() {
        return new FiguraBufferInputStream(this);
    }

    public static class FiguraBufferInputStream extends InputStream {
        private final FiguraBuffer parent;
        private final Stack<Mark> marks = new Stack<>();
        public FiguraBufferInputStream(FiguraBuffer figuraBuffer) {
            parent = figuraBuffer;
        }

        @Override
        public int read() throws IOException {
            if (!marks.empty()) {
                Mark m = marks.peek();
                if (parent.getPosition() > m.pos + m.readLimit) return -1;
            }
            return parent.read();
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public void mark(int readlimit) {
            marks.push(new Mark(parent.position, readlimit));
        }

        @Override
        public void reset() throws IOException {
            Mark m = marks.pop();
            parent.setPosition(m.pos);
        }

        @Override
        public long skip(long n) throws IOException {
            long d = Math.min(parent.getLength() - parent.getPosition(), n);
            parent.setPosition((int)(parent.getPosition()+d));
            return d;
        }

        @Override
        public int available() throws IOException {
            return parent.available();
        }

        private record Mark(int pos, int readLimit) {
        }
    }
}
