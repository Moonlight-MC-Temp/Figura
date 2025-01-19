package org.figuramc.figura.lua.api.data;

public class LuaCloseable implements AutoCloseable {
    final AutoCloseable inner;

    public LuaCloseable(AutoCloseable inner) {
        this.inner = inner;
    }

    @Override
    public void close() throws Exception {
        inner.close();
    }
}
