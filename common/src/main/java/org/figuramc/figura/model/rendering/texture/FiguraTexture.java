package org.figuramc.figura.model.rendering.texture;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.mixin.render.TextureManagerAccessor;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.LuaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.*;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.BiFunction;

@SuppressWarnings("resource")
@LuaWhitelist
@LuaTypeDoc(
        name = "Texture",
        value = "texture"
)
public class FiguraTexture extends SimpleTexture {

    /**
     * The ID of the texture, used to register to Minecraft.
     */
    private boolean registered = false;
    private boolean dirty = true;
    private boolean modified = false;
    private final String name;
    private final Avatar owner;

    /**
     * Native image holding the texture data for this texture.
     */
    private final NativeImage texture;
    private NativeImage backup;
    private boolean isClosed = false;

    private WriteOverflowStrategy writeOverflowStrategy = WriteOverflowStrategy.ERROR;

    public FiguraTexture(Avatar owner, String name, byte[] data) {
        super(new FiguraIdentifier("avatar_tex/" + owner.owner + "/" + UUID.randomUUID()));

        // Read image from wrapper
        NativeImage image;
        try {
            ByteBuffer wrapper = BufferUtils.createByteBuffer(data.length);
            wrapper.put(data);
            wrapper.rewind();
            image = NativeImage.read(wrapper);
        } catch (IOException e) {
            FiguraMod.LOGGER.error("", e);
            image = new NativeImage(1, 1, true);
        }

        this.texture = image;
        this.name = name;
        this.owner = owner;
    }

    public FiguraTexture(Avatar owner, String name, int width, int height) {
        super(new FiguraIdentifier("avatar_tex/" + owner.owner + "/" + UUID.randomUUID()));
        this.texture = new NativeImage(width, height, true);
        this.name = name;
        this.owner = owner;
    }

    public FiguraTexture(Avatar owner, String name, NativeImage image) {
        super(new FiguraIdentifier("avatar_tex/" + owner.owner + "/custom/" + UUID.randomUUID()));
        this.texture = image;
        this.name = name;
        this.owner = owner;
    }

    @Override
    public void load(ResourceManager manager) throws IOException {
    }

    @Override
    public void close() {
        // Make sure it doesn't close twice (minecraft tries to close the texture when reloading textures
        if (isClosed) return;

        isClosed = true;

        // Close native images
        texture.close();
        if (backup != null)
            backup.close();

        this.releaseId();
        ((TextureManagerAccessor) Minecraft.getInstance().getTextureManager()).getByPath().remove(this.location);
    }

    public void uploadIfDirty() {
        if (!registered) {
            Minecraft.getInstance().getTextureManager().register(this.location, this);
            registered = true;
        }

        if (dirty && !isClosed) {
            dirty = false;

            RenderCall runnable = () -> {
                // Upload texture to GPU.
                TextureUtil.prepareImage(this.getId(), texture.getWidth(), texture.getHeight());
                texture.upload(0, 0, 0, false);
            };

            if (RenderSystem.isOnRenderThreadOrInit()) {
                runnable.execute();
            } else {
                RenderSystem.recordRenderCall(runnable);
            }
        }
    }

    public void writeTexture(Path dest) throws IOException {
        texture.writeToFile(dest);
    }

    private void backupImage() {
        this.modified = true;
        if (this.backup == null)
            backup = copy();
    }

    public NativeImage copy() {
        NativeImage image = new NativeImage(texture.format(), texture.getWidth(), texture.getHeight(), true);
        image.copyFrom(texture);
        return image;
    }

    @LuaWhitelist
    public int getWidth() {
        return texture.getWidth();
    }

    @LuaWhitelist
    public int getHeight() {
        return texture.getHeight();
    }

    public ResourceLocation getLocation() {
        return this.location;
    }


    // -- lua stuff -- // 


    private FiguraVec4 parseColor(String method, Object r, Double g, Double b, Double a) {
        FiguraVec4 vec4 = LuaUtils.parseVec4(method, r, g, b, a, 0, 0, 0, 1);
        return FiguraVec4.of(
                clamp01(vec4.x),
                clamp01(vec4.y),
                clamp01(vec4.z),
                clamp01(vec4.w)
        );
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.get_name")
    public String getName() {
        return name;
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.get_path")
    public String getPath() {
        return getLocation().toString();
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.get_dimensions")
    public FiguraVec2 getDimensions() {
        return FiguraVec2.of(getWidth(), getHeight());
    }

    public FiguraVec4 getActualPixel(int x, int y) {
        try {
            return ColorUtils.abgrToRGBA(texture.getPixelRGBA(x, y));
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {Integer.class, Integer.class},
                    argumentNames = {"x", "y"}
            ),
            value = "texture.get_pixel")
    public FiguraVec4 getPixel(int x, int y) {
        Pair<Integer, Integer> actual = mapCoordinates(x, y);
        if (actual == null) throw new LuaError(String.format(
                "(%d, %d) is out of bounds on %dx%d texture",
                x, y, getWidth(), getHeight()
        ));
        return getActualPixel(actual.getFirst(), actual.getSecond());
    }

    public FiguraTexture setActualPixel(int x, int y, int color) {
        return setActualPixel(x, y, color, true);
    }

    public FiguraTexture setActualPixel(int x, int y, int color, boolean makeBackup) {
        try {
            if (makeBackup) backupImage();
            texture.setPixelRGBA(x, y, color);
            return this;
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, FiguraVec3.class},
                            argumentNames = {"x", "y", "rgb"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, FiguraVec4.class},
                            argumentNames = {"x", "y", "rgba"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "r", "g", "b", "a"}
                    )
            },
            aliases = "pixel",
            value = "texture.set_pixel")
    public FiguraTexture setPixel(int x, int y, Object r, Double g, Double b, Double a) {
        int color = ColorUtils.rgbaToIntABGR(parseColor("setPixel", r, g, b, a));
        Pair<Integer, Integer> actual = mapCoordinates(x, y);
        if (actual == null) return this;
        return setActualPixel(actual.getFirst(), actual.getSecond(), color);
    }

    @LuaWhitelist
    public FiguraTexture pixel(int x, int y, Object r, Double g, Double b, Double a) {
        return setPixel(x, y, r, g, b, a);
    }

    /**
     * Performs "linear" resizing, also called "no filter" or "nearest". Returns a new texture.
     * For imperfect downscaling (i.e. segments not on pixel edges) may not produce equivalent results to
     * e.g. whatever image processing program you use due to weighting 'partial' pixels equivalent to 'full' pixels
     * when downscaling
     */
    @LuaWhitelist
    public FiguraTexture resize(String outputName, int targetWidth, int targetHeight) {
        // float imprecision strikes again (+/- to prevent rounding to the next number when you're approximately equal)
        final double EPSILON = 1e-6;

        NativeImage internal = new NativeImage(targetWidth, targetHeight, true);
        FiguraTexture result = owner.registerTexture(outputName, internal, false);
        int srcWidth = getWidth(), srcHeight = getHeight();
        for (int outputX = 0; outputX < targetWidth; outputX++) {
            for (int outputY = 0; outputY < targetHeight; outputY++) {
                // map the output pixels onto the input pixels
                int inputX_low = (int) Math.floor((double) outputX / targetWidth * srcWidth + EPSILON);
                int inputX_high = (int) Math.ceil((double) (outputX + 1) / targetWidth * srcWidth - EPSILON);
                int inputY_low = (int) Math.floor((double) outputY / targetHeight * srcHeight + EPSILON);
                int inputY_high = (int) Math.ceil((double) (outputY + 1) / targetHeight * srcHeight - EPSILON);

                // compute average color on corresponding source pixels
                int count = 0;
                double r = 0, g = 0, b = 0, a = 0;
                for (int inputX = inputX_low; inputX < inputX_high; inputX++) {
                    for (int inputY = inputY_low; inputY < inputY_high; inputY++) {
                        FiguraVec4 color = getActualPixel(inputX, inputY);
                        r += color.x;
                        g += color.y;
                        b += color.z;
                        a += color.w;
                        count += 1;
                    }
                }
                r /= count;
                g /= count;
                b /= count;
                a /= count;
                result.setActualPixel(outputX, outputY, ColorUtils.rgbaToIntABGR(FiguraVec4.of(r, g, b, a)), false);
            }
        }
        result.backupImage();
        result.update();
        return result;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, FiguraVec3.class},
                            argumentNames = {"x", "y", "width", "height", "rgb"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, FiguraVec4.class},
                            argumentNames = {"x", "y", "width", "height", "rgba"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "width", "height", "r", "g", "b", "a"}
                    )
            },
            value = "texture.fill")
    public FiguraTexture fill(int x, int y, int width, int height, Object r, Double g, Double b, Double a) {
        try {
            int color = ColorUtils.rgbaToIntABGR(parseColor("fill", r, g, b, a));
            // texture.fillRect just does these loops for us, so we can extract them to add the mapping
            backupImage();
            for (int i = x; i < x + width; i++) {
                for (int j = y; j < y + height; j++) {
                    Pair<Integer, Integer> actual = mapCoordinates(i, j);
                    if (actual == null) continue;
                    // don't make a copy of the image each time, though
                    setActualPixel(actual.getFirst(), actual.getSecond(), color, false);
                }
            }
            return this;
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    public enum BlendMode {
        SOURCE("source"),
        NORMAL("normal"),
        OUT("out"),
        IN("in"),
        ATOP("atop"),
        XOR("xor");

        public final String name;
        public static final HashMap<String, BlendMode> NAMES = new HashMap<>();

        BlendMode(String name) {
            this.name = name;
        }

        static {
            EnumSet<BlendMode> allMembers = EnumSet.allOf(BlendMode.class);
            for (BlendMode mode : allMembers) {
                NAMES.put(mode.name, mode);
            }
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class BlitOptions {

        public final FiguraTexture source;
        public final int x;
        public final int y;
        public final int sourceX;
        public final int sourceY;
        public final int width;
        public final int height;
        public final BlendMode mode;

        public BlitOptions(FiguraTexture source, int x, int y, int sourceX, int sourceY, int width, int height,
                           BlendMode mode) {
            this.source = source;
            this.x = x;
            this.y = y;
            this.sourceX = sourceX;
            this.sourceY = sourceY;
            this.width = width;
            this.height = height;
            this.mode = mode;
        }

        public static class Builder {
            private FiguraTexture source = null;
            private int x = 0;
            private int y = 0;
            private int sourceX = 0;
            private int sourceY = 0;
            private boolean isWidthConfigured = false;
            private int width;
            private boolean isHeightConfigured = false;
            private int height;
            private BlendMode mode = BlendMode.NORMAL;

            public Builder() {
            }

            public Builder from(FiguraTexture source) {
                if (!isWidthConfigured) width = source.getWidth();
                if (!isHeightConfigured) height = source.getHeight();
                this.source = source;
                return this;
            }

            public Builder mode(BlendMode mode) {
                this.mode = mode;
                return this;
            }

            public void setProperty(String property, LuaValue value) {
                switch (property) {
                    case "source": {
                        FiguraTexture texture = (FiguraTexture) value.checkuserdata(FiguraTexture.class);
                        from(texture);
                        break;
                    }
                    case "x":
                        x = value.checkint();
                        break;
                    case "y":
                        y = value.checkint();
                        break;
                    case "sourceX":
                        sourceX = value.checkint();
                        break;
                    case "sourceY":
                        sourceY = value.checkint();
                        break;
                    case "width": {
                        width = value.checkint();
                        isWidthConfigured = true;
                        break;
                    }
                    case "height": {
                        height = value.checkint();
                        isHeightConfigured = true;
                        break;
                    }
                    case "mode": {
                        BlendMode m = BlendMode.NAMES.get(value.checkjstring());
                        if (m == null) throw new LuaError("Unknown blending mode '" + value.checkjstring() + "'.");
                        mode = m;
                    }
                }
            }

            public BlitOptions build() {
                if (source == null) throw new LuaError("No source specified for blit operation");
                return new BlitOptions(
                        source, x, y, sourceX, sourceY, width, height, mode
                );
            }
        }

        public static BlitOptions fromTable(LuaTable table) {
            Builder b = new Builder();
            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs var = table.next(k);
                if ((k = var.arg1()).isnil()) break;
                if (k.isstring()) {
                    LuaValue val = table.get(k);
                    b.setProperty(k.checkjstring(), val);
                }
            }
            return b.build();
        }
    }

    public static FiguraVec4 composeColors(FiguraVec4 target, FiguraVec4 source, BlendMode mode, boolean makeCopy) {
        if (makeCopy) {
            target = target.copy();
            source = source.copy();
        }
        // Premultiply...
        double targetW = target.w;
        double sourceW = source.w;

        target.scale(targetW);
        target.w = targetW;

        source.scale(sourceW);
        source.w = sourceW;
        double sFactor;
        double tFactor;

        // Choose which operator to use
        // Special thanks: https://ciechanow.ski/alpha-compositing/
        switch (mode) {
            case SOURCE:
                sFactor = 1.0;
                tFactor = 0.0;
                break;
            case NORMAL:
                sFactor = 1.0;
                tFactor = 1 - sourceW;
                break;
            case OUT:
                sFactor = 1.0 - targetW;
                tFactor = 0.0;
                break;
            case XOR:
                sFactor = 1.0 - targetW;
                tFactor = 1.0 - sourceW;
                break;
            case IN:
                sFactor = targetW;
                tFactor = 0.0;
                break;
            case ATOP:
                sFactor = targetW;
                tFactor = 1.0 - sourceW;
                break;
            default:
                throw new IllegalArgumentException();
        }

        source.scale(sFactor);
        target.scale(tFactor);
        target.add(source);

        // ...un-premultiply.
        return FiguraVec4.of(
                target.x / target.w,
                target.y / target.w,
                target.z / target.w,
                target.w
        );
    }

    public FiguraTexture blit(BlitOptions options) {
        FiguraTexture source = options.source;
        backupImage();
        for (int x = 0; x < options.width; x++) {
            for (int y = 0; y < options.height; y++) {
                int tX = options.x + x, tY = options.y + y;
                int sX = options.sourceX + x, sY = options.sourceY + y;
                Pair<Integer, Integer> real = mapCoordinates(tX, tY);
                if (real == null) continue;
                FiguraVec4 sColorTrue = source.getPixel(sX, sY);
                FiguraVec4 tColorTrue = getActualPixel(real.getFirst(), real.getSecond());
                setActualPixel(
                        real.getFirst(), real.getSecond(), ColorUtils.rgbaToIntABGR(
                                composeColors(tColorTrue, sColorTrue, options.mode, false)
                        )
                );
            }
        }
        return this;
    }

    @LuaWhitelist
    public FiguraTexture blit(LuaTable options) {
        return blit(BlitOptions.fromTable(options));
    }

    // credit: Wikipedia contributors, http://members.chello.at/easyfilter/bresenham.html
    private void lineReal(int x0, int y0, int x1, int y1, FiguraVec4 color, BlendMode mode) {
        int colorInt = ColorUtils.rgbaToIntABGR(color);
        boolean compositionRequired = color.w < 1.0;
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            Pair<Integer, Integer> actual = mapCoordinates(x0, y0);
            if (actual != null) {
                int thisColor = colorInt;
                if (compositionRequired) {
                    thisColor = ColorUtils.rgbaToIntABGR(composeColors(
                            getActualPixel(actual.getFirst(), actual.getSecond()),
                            color,
                            mode,
                            false
                    ));
                }
                setActualPixel(actual.getFirst(), actual.getSecond(), thisColor, false);
            }
            if (x0 == x1 && y0 == y1) break;
            int error2 = error * 2;
            if (error2 >= dy) {
                error = error + dy;
                x0 = x0 + sx;
            }
            if (error2 <= dx) {
                error = error + dx;
                y0 = y0 + sy;
            }
        }
    }

    private void setPixelMix(int x, int y, FiguraVec4 color, BlendMode mode) {
        Pair<Integer, Integer> actual = mapCoordinates(x, y);
        if (actual == null) return;
        FiguraVec4 baseColor = getActualPixel(actual.getFirst(), actual.getSecond());
        int colorId = ColorUtils.rgbaToIntABGR(composeColors(
                baseColor,
                color,
                mode,
                false
        ));
        setActualPixel(actual.getFirst(), actual.getSecond(), colorId, false);
    }

    private static double fractional(double x) {
        return x - Math.floor(x);
    }

    private static double rFractional(double x) {
        return 1.0 - fractional(x);
    }

    private static FiguraVec4 opacity(FiguraVec4 base, double factor) {
        FiguraVec4 clone = base.copy();
        clone.w *= factor;
        return clone;
    }

    // https://en.wikipedia.org/wiki/Xiaolin_Wu%27s_line_algorithm, Wikipedia contributors
    private void aaLineReal(int x0, int y0, int x1, int y1, FiguraVec4 color, BlendMode mode) {
        boolean isSteep = Math.abs(y1 - y0) > Math.abs(x1 - x0);
        // Shuffle variables
        if (isSteep) {
            int swap = x0;
            //noinspection SuspiciousNameCombination
            x0 = y0;
            y0 = swap;
            swap = x1;
            //noinspection SuspiciousNameCombination
            x1 = y1;
            y1 = swap;
        }
        if (x0 > x1) {
            int swap = x0;
            x0 = x1;
            x1 = swap;
            swap = y0;
            y0 = y1;
            y1 = swap;
        }

        int dx = x1 - x0;
        int dy = y1 - y0;
        double gradient;

        if (dx == 0) {
            gradient = 1.0;
        } else {
            gradient = (double) dy / dx;
        }

        // First endpoint
        double xend = x0;
        double yend = y0 + gradient * (xend - x0);
        double xgap = rFractional(x0 + 0.5);
        int xpxl1 = (int) Math.round(xend);
        int ypxl1 = (int) Math.floor(yend);
        if (isSteep) {
            setPixelMix(ypxl1, xpxl1, opacity(color, rFractional(yend) * xgap), mode);
            setPixelMix(ypxl1 + 1, xpxl1, opacity(color, fractional(yend) * xgap), mode);
        } else {
            setPixelMix(xpxl1, ypxl1, opacity(color, rFractional(yend) * xgap), mode);
            setPixelMix(xpxl1, ypxl1 + 1, opacity(color, fractional(yend) * xgap), mode);
        }

        // First Y intersection
        double intery = yend + gradient;

        // Second endpoint
        xend = x1;
        yend = y1 + gradient * (xend - x1);
        xgap = fractional(x1 + 0.5);
        int xpxl2 = (int) Math.round(xend);
        int ypxl2 = (int) Math.floor(yend);
        if (isSteep) {
            setPixelMix(ypxl2, xpxl2, opacity(color, rFractional(yend) * xgap), mode);
            setPixelMix(ypxl2 + 1, xpxl2, opacity(color, fractional(yend) * xgap), mode);
        } else {
            setPixelMix(xpxl2, ypxl2, opacity(color, rFractional(yend) * xgap), mode);
            setPixelMix(xpxl2, ypxl2, opacity(color, rFractional(yend) * xgap), mode);
        }

        if (isSteep) {
            for (int x = xpxl1 + 1; x < xpxl2; x++) {
                //noinspection SuspiciousNameCombination
                setPixelMix((int) Math.floor(intery), x, opacity(color, rFractional(intery)), mode);
                //noinspection SuspiciousNameCombination
                setPixelMix((int) Math.floor(intery) + 1, x, opacity(color, fractional(intery)), mode);
                intery += gradient;
            }
        } else {
            for (int x = xpxl1 + 1; x < xpxl2; x++) {
                setPixelMix(x, (int) Math.floor(intery), opacity(color, rFractional(intery)), mode);
                setPixelMix(x, (int) Math.floor(intery) + 1, opacity(color, fractional(intery)), mode);
                intery += gradient;
            }
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "texture.line",
            overloads = {
                    @LuaMethodOverload(
                            argumentNames = {"xy0", "xy1", "color"},
                            argumentTypes = {FiguraVec2.class, FiguraVec2.class, FiguraVec4.class}
                    ),
                    @LuaMethodOverload(
                            argumentNames = {"xy0", "xy1", "color", "antialias"},
                            argumentTypes = {FiguraVec2.class, FiguraVec2.class, FiguraVec4.class, Boolean.class}
                    ),
                    @LuaMethodOverload(
                            argumentNames = {"xy0", "xy1", "color", "antialias", "blendMode"},
                            argumentTypes = {FiguraVec2.class, FiguraVec2.class, FiguraVec4.class, Boolean.class, String.class}
                    )
            }
    )
    public void line(
            FiguraVec2 xy0,
            FiguraVec2 xy1,
            FiguraVec4 color,
            @Nullable Boolean antialias,
            String blend
    ) {
        boolean antialiasActual = (antialias != null) && antialias;
        BlendMode blendActual = BlendMode.NORMAL;
        if (blend != null) {
            blendActual = BlendMode.NAMES.get(blend);
            if (blendActual == null) throw new LuaError(String.format("Unknown blending mode '%s'.", blend));
        }
        if (antialiasActual) {
            aaLineReal((int) xy0.x, (int) xy0.y, (int) xy1.x, (int) xy1.y, color, blendActual);
        } else {
            lineReal((int) xy0.x, (int) xy0.y, (int) xy1.x, (int) xy1.y, color, blendActual);
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.update")
    public FiguraTexture update() {
        this.dirty = true;
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.restore")
    public FiguraTexture restore() {
        if (modified) {
            this.texture.copyFrom(backup);
            this.modified = false;
        }
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.save")
    public String save() {
        try {
            return Base64.getEncoder().encodeToString(texture.asByteArray());
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, LuaFunction.class},
                    argumentNames = {"x", "y", "width", "height", "func"}
            ),
            value = "texture.apply_func"
    )
    public FiguraTexture applyFunc(int x, int y, int width, int height, @LuaNotNil LuaFunction function) {
        backupImage();
        for (int i = y; i < y + height; i++) {
            for (int j = x; j < x + width; j++) {
                Pair<Integer, Integer> actual = mapCoordinates(j, i);
                if (actual == null) continue;
                int actualX = actual.getFirst(), actualY = actual.getSecond();
                FiguraVec4 color = getPixel(actualX, actualY);
                LuaValue result = function.call(
                        owner.luaRuntime.typeManager.javaToLua(color).arg1(),
                        LuaValue.valueOf(j),
                        LuaValue.valueOf(i)
                );
                if (!result.isnil() && result.isuserdata(FiguraVec4.class)) {
                    FiguraVec4 userdata = (FiguraVec4) result.checkuserdata(FiguraVec4.class);
                    userdata = FiguraVec4.of(
                            clamp01(userdata.x),
                            clamp01(userdata.y),
                            clamp01(userdata.z),
                            clamp01(userdata.w)
                    );
                    int newColor = ColorUtils.rgbaToIntABGR(userdata);
                    setActualPixel(actualX, actualY, newColor, false);
                }
            }
        }
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, FiguraMat4.class},
                    argumentNames = {"x", "y", "width", "height", "matrix"}
            ),
            value = "texture.apply_matrix"
    )
    public FiguraTexture applyMatrix(int x,
                                     int y,
                                     int width,
                                     int height,
                                     @LuaNotNil FiguraMat4 matrix,
                                     @Nullable Object clip) {
        // remove next major version
        if (clip != null) {
            throw new LuaError(
                    "texture.applyMatrix's 'clip' argument has been removed (clipping is always enabled)");
        }
        backupImage();
        for (int i = y; i < y + height; i++) {
            for (int j = x; j < x + width; j++) {
                Pair<Integer, Integer> actual = mapCoordinates(j, i);
                if (actual == null) continue;
                int realX = actual.getFirst(), realY = actual.getSecond();
                FiguraVec4 color = getPixel(realX, realY);
                color.transform(matrix);

                color.x = Math.max(0, Math.min(color.x, 1));
                color.y = Math.max(0, Math.min(color.y, 1));
                color.z = Math.max(0, Math.min(color.z, 1));
                color.w = Math.max(0, Math.min(color.w, 1));

                setActualPixel(realX, realY, ColorUtils.rgbaToIntABGR(color), false);
            }
        }
        return this;
    }

    private static final HashMap<String, WriteOverflowStrategy> name2OverflowStrategy = new HashMap<>();
    private static final HashMap<WriteOverflowStrategy, String> overflowStrategy2Name = new HashMap<>();

    public enum WriteOverflowStrategy {
        ERROR("error"),
        IGNORE("ignore", "discard"),
        WRAP("wrap"),
        MIRROR("mirror"),
        CLAMP("clamp");

        public final String primaryName;

        WriteOverflowStrategy(String... names) {
            for (String name : names)
                name2OverflowStrategy.put(name, this);
            if (names.length == 0) throw new IllegalArgumentException("at least one name should be specified");
            overflowStrategy2Name.put(this, names[0]);
            primaryName = names[0];
        }
    }

    private @Nullable Pair<Integer, Integer> mapCoordinates(int x, int y) throws LuaError {
        int width = getWidth(), height = getHeight();
        if (x >= 0 && x < width && y >= 0 && y < height) return Pair.of(x, y);
        switch (writeOverflowStrategy) {
            case ERROR:
                throw new LuaError(String.format(
                        "(%d, %d) is out of bounds on %dx%d texture",
                        x, y, width, height
                ));
            case IGNORE:
                return null;
            case WRAP:
                return Pair.of(
                        Math.floorMod(x, width),
                        Math.floorMod(y, height)
                );
            case MIRROR: // but first, we need to talk about parallel universes
                int puX = Math.floorDiv(x, width), puY = Math.floorDiv(y, height);
                // if the original image is PU(0, 0), odd numbered PUs are flipped on one or both axes
                boolean isXFlipped = Math.floorMod(puX, 2) == 1, isYFlipped = Math.floorMod(puY, 2) == 1;
                int localX = Math.floorMod(x, width), localY = Math.floorMod(y, height);
                if (isXFlipped) localX = (width - 1) - localX;
                if (isYFlipped) localY = (height - 1) - localY;
                return Pair.of(localX, localY);
            case CLAMP: // redirect out-of-bounds requests to the closest edge or corner
                return Pair.of(
                        Math.max(Math.min(x, width - 1), 0),
                        Math.max(Math.min(y, height - 1), 0)
                );
            default:
                throw new IllegalArgumentException();
        }
    }

    // Mathematical area operations

    private void assertSameSize(FiguraTexture other) throws LuaError {
        int otherW = other.getWidth(), otherH = other.getHeight();
        int thisW = getWidth(), thisH = getHeight();
        if (thisW != otherW || thisH != otherH) {
            throw new LuaError(String.format(
                    "Expected textures to have equal dimensions, but the target is %dx%d and the provided texture is %dx%d",
                    thisW,
                    thisH,
                    otherW,
                    otherH
            ));
        }
    }

    private static double clamp01(double n) {
        if (n < 0) return 0;
        if (n > 1) return 1;
        return n;
    }

    private FiguraTexture mathApply(@NotNull FiguraTexture other,
                                    BiFunction<FiguraVec4, FiguraVec4, FiguraVec4> transform,
                                    int x,
                                    int y,
                                    int w,
                                    int h) {
        assertSameSize(other);
        backupImage();
        for (int curX = x; curX < x + w; curX++) {
            for (int curY = y; curY < y + h; curY++) {
                Pair<Integer, Integer> actualCoordinates = mapCoordinates(curX, curY);
                if (actualCoordinates == null) continue;
                int actualX = actualCoordinates.getFirst(), actualY = actualCoordinates.getSecond();
                try {
                    FiguraVec4 colorA = ColorUtils.abgrToRGBA(texture.getPixelRGBA(actualX, actualY));
                    FiguraVec4 colorB = ColorUtils.abgrToRGBA(other.texture.getPixelRGBA(actualX, actualY));
                    FiguraVec4 result = transform.apply(colorA, colorB);
                    result = FiguraVec4.of(
                            clamp01(result.x),
                            clamp01(result.y),
                            clamp01(result.z),
                            clamp01(result.w)
                    );
                    texture.setPixelRGBA(actualX, actualY, ColorUtils.rgbaToIntABGR(result));
                } catch (Exception e) {
                    restore();
                    if (curX != actualX || curY != actualY)
                        throw new LuaError(String.format(
                                "While applying pixel at actual(%d, %d) / virtual(%d, %d): %s",
                                actualX, actualY,
                                curX, curY,
                                e.getMessage()
                        ));
                    throw new LuaError(String.format(
                            "While applying pixel at (%d, %d): %s",
                            actualX, actualY, e.getMessage()
                    ));
                }
            }
        }
        return this;
    }

    private static final BiFunction<FiguraVec4, FiguraVec4, FiguraVec4> opMultiply = FiguraVec4::times;
    private static final BiFunction<FiguraVec4, FiguraVec4, FiguraVec4> opDivide = FiguraVec4::dividedBy;
    private static final BiFunction<FiguraVec4, FiguraVec4, FiguraVec4> opAdd = FiguraVec4::plus;
    private static final BiFunction<FiguraVec4, FiguraVec4, FiguraVec4> opSubtract = FiguraVec4::minus;

    private FiguraTexture mathFunction(@NotNull FiguraTexture other,
                                       int x,
                                       int y,
                                       int w,
                                       int h,
                                       BiFunction<FiguraVec4, FiguraVec4, FiguraVec4> transform) {
        return mathApply(other, transform, x, y, w, h);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "texture._math_op",
            overloads = @LuaMethodOverload(
                    argumentNames = {"other", "x", "y", "w", "h"},
                    argumentTypes = {FiguraTexture.class, Integer.class, Integer.class, Integer.class, Integer.class}
            )
    )
    public FiguraTexture multiply(@LuaNotNil @NotNull FiguraTexture other, int x, int y, int w, int h) {
        return mathFunction(other, x, y, w, h, opMultiply);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "texture._math_op",
            overloads = @LuaMethodOverload(
                    argumentNames = {"other", "x", "y", "w", "h"},
                    argumentTypes = {FiguraTexture.class, Integer.class, Integer.class, Integer.class, Integer.class}
            )
    )
    public FiguraTexture divide(@LuaNotNil @NotNull FiguraTexture other, int x, int y, int w, int h) {
        return mathFunction(other, x, y, w, h, opDivide);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "texture._math_op",
            overloads = @LuaMethodOverload(
                    argumentNames = {"other", "x", "y", "w", "h"},
                    argumentTypes = {FiguraTexture.class, Integer.class, Integer.class, Integer.class, Integer.class}
            )
    )
    public FiguraTexture add(@LuaNotNil @NotNull FiguraTexture other, int x, int y, int w, int h) {
        return mathFunction(other, x, y, w, h, opAdd);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "texture._math_op",
            overloads = @LuaMethodOverload(
                    argumentNames = {"other", "x", "y", "w", "h"},
                    argumentTypes = {FiguraTexture.class, Integer.class, Integer.class, Integer.class, Integer.class}
            )
    )
    public FiguraTexture subtract(@LuaNotNil @NotNull FiguraTexture other, int x, int y, int w, int h) {
        return mathFunction(other, x, y, w, h, opSubtract);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "texture.invert",
            overloads = {
                    @LuaMethodOverload(
                            argumentNames = {"x", "y", "w", "h"},
                            argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class}
                    ),
                    @LuaMethodOverload(
                            argumentNames = {"x", "y", "w", "h", "invertAlpha"},
                            argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class, Boolean.class}
                    )
            }
    )
    public FiguraTexture invert(int x, int y, int w, int h, Boolean invertAlpha) {
        boolean invertAlpha_real = (invertAlpha != null && invertAlpha);
        backupImage();
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                Pair<Integer, Integer> actual = mapCoordinates(i, j);
                if (actual == null) continue;
                int realX = actual.getFirst(), realY = actual.getSecond();
                FiguraVec4 current = getActualPixel(realX, realY);
                FiguraVec4 inverted = FiguraVec4.of(
                        1 - current.x,
                        1 - current.y,
                        1 - current.z,
                        invertAlpha_real ? 1 - current.w : current.w
                );
                setActualPixel(realX, realY, ColorUtils.rgbaToIntABGR(inverted), false);
            }
        }
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "texture.set_overflow_mode",
            overloads = @LuaMethodOverload(
                    argumentNames = {"mode"},
                    argumentTypes = {String.class}
            )
    )
    public FiguraTexture setOverflowMode(@LuaNotNil @NotNull String mode) {
        if (!name2OverflowStrategy.containsKey(mode)) {
            int i = 0;
            StringBuilder options = new StringBuilder();
            for (String k : name2OverflowStrategy.keySet()) {
                if (i++ > 0) options.append(", ");
                options.append("'").append(k).append("'");
            }
            throw new LuaError(String.format(
                    "Unknown overflow mode '%s'\n(valid modes are: " + options + ")",
                    mode
            ));
        }
        writeOverflowStrategy = name2OverflowStrategy.get(mode);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("texture.get_overflow_mode")
    public String getOverflowMode() {
        return writeOverflowStrategy.primaryName;
    }

    @LuaWhitelist
    public Object __index(String arg) {
        return "name".equals(arg) ? name : null;
    }

    @Override
    public String toString() {
        return name + " (" + getWidth() + "x" + getHeight() + ") (Texture)";
    }
}