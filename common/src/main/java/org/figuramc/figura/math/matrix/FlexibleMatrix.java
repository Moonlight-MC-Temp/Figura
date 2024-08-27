package org.figuramc.figura.math.matrix;

import org.figuramc.figura.math.vector.FiguraVector;
import org.figuramc.figura.math.vector.FlexibleVector;

/**
 * Not a Lua API, but used internally for things like image filtering.
 * (read: I needed a 5*5 matrix and decided to generalize)
 */
public class FlexibleMatrix extends FiguraMatrix<FlexibleMatrix, FlexibleVector> {
    private final double[][] internal;
    private final double[][] internalTranspose;
    public final int width, height;

    public FlexibleMatrix(int width, int height) {
        internal = new double[height][width];
        internalTranspose = new double[width][height];
        this.width = width;
        this.height = height;
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

    private static <T1 extends FiguraMatrix<T1, ?>, T2 extends FiguraMatrix<T2, ?>> void assertSizeSize(T1 left,
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
    protected double calculateDeterminant() {
        return 0;
    }

    @Override
    protected void resetIdentity() {

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
                double[] row = source.getRow(y).unpack();
                if (row.length != width) throw new IllegalArgumentException(String.format(
                        "inconsistent info: cols() says the width is %d, but unpack() says it's %d",
                        width, row.length
                ));
                internal[y] = row; 
            }
            for (int x = 0; x < width; x++) {
                double[] col = source.getColumn(x).unpack();
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
            for (int x = 0; x < width; x++) {
                double[] col = source.getColumn(x).unpack();
                if (col.length != height) throw new IllegalArgumentException(String.format(
                        "inconsistent info: rows() says the height is %d, but unpack() says it's %d",
                        height, col.length
                ));
                internalTranspose[x] = col;
            }
            regenerateTransposeReverse();
        }
    }

    @Override
    public FlexibleMatrix copy() {
        return null;
    }

    @Override
    public boolean equals(FlexibleMatrix other) {
        return false;
    }

    @Override
    public FlexibleVector getColumn(int col) {
        return null;
    }

    @Override
    public FlexibleVector getRow(int row) {
        return null;
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
    public FlexibleMatrix set(FlexibleMatrix o) {
        return null;
    }

    @Override
    public FlexibleMatrix multiply(FlexibleMatrix o) {
        return null;
    }

    @Override
    public FlexibleMatrix rightMultiply(FlexibleMatrix o) {
        return null;
    }

    @Override
    public FlexibleMatrix transpose() {
        return null;
    }

    @Override
    public FlexibleMatrix invert() {
        return null;
    }

    @Override
    public FlexibleMatrix add(FlexibleMatrix o) {
        return null;
    }

    @Override
    public FlexibleMatrix sub(FlexibleMatrix o) {
        return null;
    }
}
