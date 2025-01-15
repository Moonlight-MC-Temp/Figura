package org.figuramc.figura.server.utils;

public class Either<A, B> {
    private Object value;
    private boolean b;

    protected Either(Object value, boolean isB) {
        this.value = value;
        b = isB;
    }

    public Object value() {
        return value;
    }

    public boolean isA() {
        return !b;
    }

    public boolean isB() {
        return b;
    }

    @SuppressWarnings("unchecked")
    public A a() {
        return !b ? (A) value : null;
    }

    @SuppressWarnings("unchecked")
    public B b() {
        return b ? (B) value : null;
    }

    public void setA(A value) {
        this.value = value;
        b = false;
    }

    public void setB(B value) {
        this.value = value;
        b = true;
    }

    public static <A, B> Either<A, B> newA(A value) {
        return new Either<>(value, false);
    }

    public static <A, B> Either<A, B> newB(B value) {
        return new Either<>(value, true);
    }
}
