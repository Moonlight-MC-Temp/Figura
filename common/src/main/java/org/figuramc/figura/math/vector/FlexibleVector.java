package org.figuramc.figura.math.vector;

import org.figuramc.figura.math.matrix.FlexibleMatrix;

public class FlexibleVector extends FiguraVector<FlexibleVector, FlexibleMatrix> {
    private final double[] internal;
    public final int size;

    public FlexibleVector(int size) {
        internal = new double[size];
        this.size = size;
    }

    @Override
    public double lengthSquared() {
        return 0;
    }
    
    private static <T1 extends FiguraVector<T1, ?>, T2 extends FiguraVector<T2, ?>> void assertSizeEqual(T1 left, T2 right) {
        if (left.size() != right.size()) throw new IllegalArgumentException(String.format(
                "both vectors need to be the same size, but one is %d and the other is %d",
                left.size(), right.size()
        ));
    }
    private static void assertSizeEqual(FlexibleVector left, FlexibleVector right) {
        if (left.size != right.size) throw new IllegalArgumentException(String.format(
                "both vectors need to be the same size, but one is %d and the other is %d",
                left.size, right.size
        ));
    }

    public void strictCopyFrom(FlexibleVector source) {
        assertSizeEqual(this, source);
        System.arraycopy(source.internal, 0, internal, 0, size);
    }
    public <T extends FiguraVector<T, ?>> void strictCopyFrom(T source) {
        assertSizeEqual(this, source);
        lenientCopyFrom(source);
    }

    /**
     * Copies values from the FlexibleVector in 'source'.
     * <ul>
     * <li>If the source and target vectors are the same size, behaves as in {@code strictCopyFrom}.</li>
     * <li>If the source vector is smaller than the target vector, all values are copied starting at index 0, and
     * values not copied from the source are left unchanged.</li>
     * <li>If the source vector is larger than the target vector, extra values in the source vector are discarded.</li>
     * </ul>
     *
     * @param source Source vector.
     */
    public void lenientCopyFrom(FlexibleVector source) {
        System.arraycopy(source.internal, 0, internal, 0, Math.min(size, source.size));
    }
    
    public <T extends FiguraVector<T, ?>> void lenientCopyFrom(T source) {
        double[] array = source.unpack();
        System.arraycopy(array, 0, internal, 0, Math.min(size, source.size()));
    }

    @Override
    public FlexibleVector copy() {
        FlexibleVector newVec = new FlexibleVector(size);
        newVec.lenientCopyFrom(this);
        return newVec;
    }

    @Override
    public double dot(FlexibleVector other) {
        assertSizeEqual(this, other);
        double total = 0.0;
        for (int i = 0; i < size; i++) {
            total += internal[i] * other.internal[i];
        }
        return total;
    }

    @Override
    public FlexibleVector set(FlexibleVector other) {
        strictCopyFrom(other);
        return this;
    }

    @Override
    public FlexibleVector add(FlexibleVector other) {
        assertSizeEqual(this, other);
        for (int i = 0; i < size; i++) {
            internal[i] += other.internal[i];
        }
        return this;
    }

    @Override
    public FlexibleVector subtract(FlexibleVector other) {
        assertSizeEqual(this, other);
        for (int i = 0; i < size; i++) {
            internal[i] -= other.internal[i];
        }
        return this;
    }

    @Override
    public FlexibleVector offset(double factor) {
        for (int i = 0; i < size; i++) {
            internal[i] += factor;
        }
        return this;
    }

    @Override
    public FlexibleVector multiply(FlexibleVector other) {
        assertSizeEqual(this, other);
        for (int i = 0; i < size; i++) {
            internal[i] *= other.internal[i];
        }
        return this;
    }

    @Override
    public FlexibleVector transform(FlexibleMatrix mat) {
        // TODO
        throw new RuntimeException("TODO");
    }

    @Override
    public FlexibleVector divide(FlexibleVector other) {
        assertSizeEqual(this, other);
        for (int i = 0; i < size; i++) {
            internal[i] /= other.internal[i];
        }
        return this;
    }

    @Override
    public FlexibleVector reduce(FlexibleVector other) {
        assertSizeEqual(this, other);
        for (int i = 0; i < size; i++) {
            double m = other.internal[i];
            // mod (positive remainder)
            internal[i] = (((internal[i] % m) + m) % m);
        }
        return this;
    }

    @Override
    public FlexibleVector scale(double factor) {
        for (int i = 0; i < size; i++) {
            internal[i] *= factor;
        }
        return this;
    }

    @Override
    public double[] unpack() {
        return internal.clone();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public double index(int i) {
        return internal[i];
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other instanceof FiguraVector<?, ?> v) {
            double[] otherArr = v.unpack();
            if (otherArr.length != size) return false;
            for (int i = 0; i < size; i++) {
                // strict double equality? in MY math types?
                if (otherArr[i] != internal[i]) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder preview = new StringBuilder();
        boolean commaFlag = false;
        final int LIMIT = 5;
        for (int i = 0; i < Math.min(size, LIMIT); i++) {
            if (commaFlag) {
                preview.append(", ");
            } else {
                commaFlag = true;
            }
            preview.append(String.format("%.4f", internal[i]));
        }
        if (size > LIMIT) preview.append(", ...");
        return String.format(
                "FlexibleVector<%d>{%s}",
                size, preview
        );
    }
}
