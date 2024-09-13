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
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Base64;
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

    public int getWidth() {
        return texture.getWidth();
    }

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
                        r += color.x; g += color.y; b += color.z; a += color.w;
                        count += 1;
                    }
                }
                r /= count; g /= count; b /= count; a /= count;
                result.setActualPixel(outputX, outputY, ColorUtils.rgbaToIntABGR(FiguraVec4.of(r, g, b, a)));
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
        DISCARD("ignore", "discard"),
        WRAP("wrap"),
        MIRROR("mirror"),
        CLAMP("clamp");

        WriteOverflowStrategy(String... names) {
            for (String name : names)
                name2OverflowStrategy.put(name, this);
            if (names.length == 0) throw new IllegalArgumentException("at least one name should be specified");
            overflowStrategy2Name.put(this, names[0]);
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
            case DISCARD:
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
    public FiguraTexture multiply(@LuaNotNil @NotNull FiguraTexture other, int x, int y, int w, int h) {
        return mathFunction(other, x, y, w, h, opMultiply);
    }

    @LuaWhitelist
    public FiguraTexture divide(@LuaNotNil @NotNull FiguraTexture other, int x, int y, int w, int h) {
        return mathFunction(other, x, y, w, h, opDivide);
    }

    @LuaWhitelist
    public FiguraTexture add(@LuaNotNil @NotNull FiguraTexture other, int x, int y, int w, int h) {
        return mathFunction(other, x, y, w, h, opAdd);
    }

    @LuaWhitelist
    public FiguraTexture subtract(@LuaNotNil @NotNull FiguraTexture other, int x, int y, int w, int h) {
        return mathFunction(other, x, y, w, h, opSubtract);
    }

    @LuaWhitelist
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
    public FiguraTexture setOverflowMode(@LuaNotNil @NotNull String mode) {
        if (!name2OverflowStrategy.containsKey(mode)) {
            int i = 0;
            StringBuilder options = new StringBuilder();
            for (String k : name2OverflowStrategy.keySet()) {
                if (i++ > 0) options.append(", ");
                options.append("'").append(k).append("'");
            }
            throw new LuaError(String.format(
                    "Unknown wrapping mode '%s'\n(known: " + options + ")",
                    mode
            ));
        }
        writeOverflowStrategy = name2OverflowStrategy.get(mode);
        return this;
    }

    @LuaWhitelist
    public String getOverflowMode() {
        return overflowStrategy2Name.get(writeOverflowStrategy);
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