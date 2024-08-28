package org.figuramc.figura.math.matrix;

import org.figuramc.figura.math.vector.FiguraVector;
import org.figuramc.figura.math.vector.FlexibleVector;
import org.jetbrains.annotations.Contract;

/**
 * Not a Lua API, but used internally for things like image filtering.
 * (read: I needed a 5*5 matrix and decided to generalize)
 */
public class FlexibleMatrix extends FiguraMatrix<FlexibleMatrix, FlexibleVector> {
    /**
     * index internal[ROW][COL] or internal[Y][X]
     */
    private final double[][] internal;
    /**
     * index internalTranspose[COL][ROW] or internal[X][Y]
     */
    private final double[][] internalTranspose;
    public final int width, height;

    public FlexibleMatrix(int width, int height) {
        internal = new double[height][width];
        internalTranspose = new double[width][height];
        this.width = width;
        this.height = height;
    }

    public static <VectorT extends FiguraVector<VectorT, MatrixT>, MatrixT extends FiguraMatrix<MatrixT, VectorT>> FlexibleMatrix from(
            MatrixT mat) {
        if (mat instanceof FlexibleMatrix) return (FlexibleMatrix) mat;
        int width = mat.cols();
        int height = mat.rows();
        FlexibleMatrix newMat = new FlexibleMatrix(width, height);
        newMat.lenientCopyFrom(mat);
        return newMat;
    }

    private void regenerateTranspose() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                internalTranspose[x][y] = internal[y][x];
            }
        }
    }

    private void regenerateTransposeReverse() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                internal[y][x] = internalTranspose[x][y];
            }
        }
    }

    private static <T1 extends FiguraMatrix<T1, ?>, T2 extends FiguraMatrix<T2, ?>> void assertSameSize(T1 left,
                                                                                                        T2 right) {
        if (left.rows() != right.rows() || left.cols() != right.cols())
            throw new IllegalArgumentException(String.format(
                    "both matrices need to be the same size, but one is %dx%d and the other is %dx%d",
                    left.cols(), left.rows(), right.cols(), right.rows()
            ));
    }

    private static void assertSameSize(FlexibleMatrix left, FlexibleMatrix right) {
        if (left.width != right.width || left.height != right.height) throw new IllegalArgumentException(String.format(
                "both matrices need to be the same size, but one is %dx%d and the other is %dx%d",
                left.width, left.height, right.width, right.height
        ));
    }

    @Override
    @Contract("-> fail")
    public double det() {
        throw new IllegalStateException("det() not supported on FlexibleMatrix");
    }

    @Override
    @Contract("-> fail")
    protected double calculateDeterminant() {
        throw new IllegalStateException("det() not supported on FlexibleMatrix");
    }

    @Override
    protected void resetIdentity() {
        if (width == height) {
            for (int xy = 0; xy < height; xy++) {
                internal[xy] = new double[width];
                internal[xy][xy] = 1;
                internalTranspose[xy] = new double[height];
                internalTranspose[xy][xy] = 1;
            }
        } else throw new IllegalStateException("cannot reset to identity non-square matrix");
    }

    public void strictCopyFrom(FlexibleMatrix source) {
        assertSameSize(this, source);
        lenientCopyFrom(source);
    }

    public <VectorT extends FiguraVector<VectorT, MatrixT>, MatrixT extends FiguraMatrix<MatrixT, VectorT>>
    void strictCopyFrom(MatrixT source) {
        assertSameSize(this, source);
        lenientCopyFrom(source);
    }

    public void lenientCopyFrom(FlexibleMatrix source) {
        // fastest (no recalculation of the transpose)
        if (source.width == this.width && source.height == this.height) {
            for (int y = 0; y < height; y++) {
                internal[y] = source.internal[y].clone();
            }
            for (int x = 0; x < width; x++) {
                internalTranspose[x] = source.internalTranspose[x].clone();
            }
            return;
        }
        // faster?
        if (source.height == this.height) {
            for (int x = 0; x < Math.min(width, source.width); x++) {
                internalTranspose[x] = source.internalTranspose[x].clone();
            }
            regenerateTransposeReverse();
            return;
        }
        if (source.width == this.width) {
            for (int y = 0; y < Math.min(height, source.height); y++) {
                internal[y] = source.internal[y].clone();
            }
            regenerateTranspose();
            return;
        }

        for (int y = 0; y < Math.min(height, source.height); y++) {
            for (int x = 0; x < Math.min(width, source.width); y++) {
                internal[y][x] = source.internal[y][x];
                internalTranspose[x][y] = source.internalTranspose[x][y];
            }
        }
    }

    public <VectorT extends FiguraVector<VectorT, MatrixT>, MatrixT extends FiguraMatrix<MatrixT, VectorT>>
    void lenientCopyFrom(MatrixT source) {
        if (source.rows() == height && source.cols() == width) {
            for (int y = 0; y < height; y++) {
                // this had BETTER clone it
                double[] row = source.getRow(y + 1).unpack();
                if (row.length != width) throw new IllegalArgumentException(String.format(
                        "inconsistent info: cols() says the width is %d, but unpack() says it's %d",
                        width, row.length
                ));
                internal[y] = row;
            }
            for (int x = 0; x < width; x++) {
                double[] col = source.getColumn(x + 1).unpack();
                if (col.length != height) throw new IllegalArgumentException(String.format(
                        "inconsistent info: rows() says the height is %d, but unpack() says it's %d",
                        height, col.length
                ));
                internalTranspose[x] = col;
            }
            return;
        }
        if (source.rows() == height) {
            // copy columns into transpose table and then flip it
            for (int x = 0; x < Math.min(width, source.cols()); x++) {
                double[] col = source.getColumn(x + 1).unpack();
                if (col.length != height) throw new IllegalArgumentException(String.format(
                        "inconsistent info: rows() says the height is %d, but unpack() says it's %d",
                        height, col.length
                ));
                internalTranspose[x] = col;
            }
            regenerateTransposeReverse();
            return;
        }
        if (source.cols() == width) {
            // same but with rows
            for (int y = 0; y < Math.min(height, source.rows()); y++) {
                double[] row = source.getRow(y + 1).unpack();
                if (row.length != width) throw new IllegalArgumentException(String.format(
                        "inconsistent info: cols() says the width is %d, but unpack() says it's %d",
                        width, row.length
                ));
                internal[y] = row;
            }
            regenerateTranspose();
            return;
        }
        // just iterate
        for (int y = 0; y < Math.min(height, source.rows()); y++) {
            double[] row = source.getRow(y + 1).unpack();
            // don't risk an out-of-bounds index
            for (int x = 0; x < Math.min(width, row.length); x++) {
                internal[y][x] = row[x];
                internalTranspose[x][y] = row[x];
            }
        }
    }

    @Override
    @Contract(value = "-> new")
    public FlexibleMatrix copy() {
        FlexibleMatrix newMat = new FlexibleMatrix(width, height);
        for (int y = 0; y < height; y++) newMat.internal[y] = internal[y].clone();
        for (int x = 0; x < width; x++) newMat.internalTranspose[x] = internalTranspose[x].clone();
        return newMat;
    }

    @Override
    public boolean equals(FlexibleMatrix other) {
        if (other == null) return false;
        if (other.width != width || other.height != height) return false;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (internal[y][x] != other.internal[y][x]) return false;
            }
        }
        return true;
    }

    @Override
    public FlexibleVector getColumn(int col) {
        return FlexibleVector.of(internalTranspose[col - 1]);
    }

    @Override
    public FlexibleVector getRow(int row) {
        return FlexibleVector.of(internal[row - 1]);
    }

    @Override
    public int rows() {
        return height;
    }

    @Override
    public int cols() {
        return width;
    }

    @Override
    @Contract(value = "_ -> this")
    public FlexibleMatrix set(FlexibleMatrix o) {
        strictCopyFrom(o);
        return this;
    }

    public FlexibleMatrix set(double[][] items) {
        return set(FlexibleMatrix.of(items));
    }

    /**
     * Expects items[Y][X]; height is {@code items.length} and width is {@code items[0].length} (or 0 if empty)
     *
     * @param items values to create with
     * @return new matrix
     */
    @Contract("_ -> new")
    public static FlexibleMatrix of(double[][] items) {
        int height = items.length;
        if (height == 0) return new FlexibleMatrix(0, 0);
        int width = items[0].length;
        FlexibleMatrix target = new FlexibleMatrix(width, height);
        for (int y = 0; y < height; y++) {
            target.internal[y] = items[y].clone();
        }
        target.regenerateTranspose();
        return target;
    }

    public void put(int y, int x, double value) {
        if (y > height || y < 0) throw new IllegalArgumentException(String.format(
                "y (%d) out of range [0, %d)",
                y, height
        ));
        if (x > width || x < 0) throw new IllegalArgumentException(String.format(
                "x (%d) out of range [0, %d)",
                x, width
        ));
        internal[y][x] = value;
        internalTranspose[x][y] = value;
    }

    public double get(int y, int x) {
        if (y > height || y < 0) throw new IllegalArgumentException(String.format(
                "y (%d) out of range [0, %d)",
                y, height
        ));
        if (x > width || x < 0) throw new IllegalArgumentException(String.format(
                "x (%d) out of range [0, %d)",
                x, width
        ));
        return internal[y][x];
    }

    /**
     * Watch out - makes a copy!
     *
     * @param right right side of multiplication
     * @return result
     */
    @Override
    @Contract("_ -> new")
    public FlexibleMatrix multiply(FlexibleMatrix right) {
        if (width != right.height) throw new IllegalArgumentException(String.format(
                "cannot multiply matrices: [%d]x%d times %dx[%d]: [bracketed numbers] need to match",
                width, height, right.width, right.height
        ));
        FlexibleMatrix output = new FlexibleMatrix(right.width, height);
        for (int rowY = 0; rowY < height; rowY++) {
            for (int colX = 0; colX < right.width; colX++) {
                output.put(rowY, colX, getRow(rowY).dot(right.getColumn(colX)));
            }
        }
        return output;
    }

    /**
     * Watch out - makes a copy!
     *
     * @param left left side of multiplication
     * @return result
     */
    @Override
    @Contract("_ -> new")
    public FlexibleMatrix rightMultiply(FlexibleMatrix left) {
        return left.multiply(this);
    }

    /**
     * WATCH OUT! this makes a copy! and doesn't mutate!
     *
     * @return the new, transposed matrix
     */
    @Override
    @Contract("-> new")
    public FlexibleMatrix transpose() {
        @SuppressWarnings("SuspiciousNameCombination") // yes they're switched
        FlexibleMatrix target = new FlexibleMatrix(height, width);
        for (int y = 0; y < height; y++) target.internalTranspose[y] = internal[y].clone();
        for (int x = 0; x < width; x++) target.internal[x] = internalTranspose[x].clone();
        return target;
    }

    @Override
    @Contract("-> fail")
    public FlexibleMatrix invert() {
        throw new IllegalStateException("invert() not supported on FlexibleMatrix");
    }

    @Override
    @Contract(value = "_ -> this")
    public FlexibleMatrix add(FlexibleMatrix o) {
        assertSameSize(this, o);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                internal[y][x] += o.internal[y][x];
                internalTranspose[y][x] += o.internalTranspose[y][x];
            }
        }
        return this;
    }

    @Override
    @Contract(value = "_ -> this")
    public FlexibleMatrix sub(FlexibleMatrix o) {
        assertSameSize(this, o);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                internal[y][x] -= o.internal[y][x];
                internalTranspose[y][x] -= o.internalTranspose[y][x];
            }
        }
        return this;
    }
}
