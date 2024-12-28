package org.figuramc.figura.lua.api;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaFieldDoc;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.matrix.FiguraMat3;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.utils.LuaUtils;
import org.luaj.vm2.LuaError;

import java.util.UUID;

@LuaWhitelist
@LuaTypeDoc(
        name = "RendererAPI",
        value = "renderer"
)
public class RendererAPI {

    private final UUID owner;

    public Float shadowRadius, fov;

    @LuaWhitelist
    @LuaFieldDoc("renderer.render_fire")
    public boolean renderFire = true;
    @LuaWhitelist
    @LuaFieldDoc("renderer.render_vehicle")
    public boolean renderVehicle = true;
    @LuaWhitelist
    @LuaFieldDoc("renderer.render_crosshair")
    public boolean renderCrosshair = true;
    @LuaWhitelist
    @LuaFieldDoc("renderer.force_paperdoll")
    public boolean forcePaperdoll;
    @LuaWhitelist
    @LuaFieldDoc("renderer.render_hud")
    public boolean renderHUD = true;

    public FiguraVec3 cameraPos;
    public FiguraVec3 cameraPivot, cameraOffsetPivot;
    public FiguraVec3 cameraRot, cameraOffsetRot;
    public FiguraMat4 cameraMat;
    public FiguraMat3 cameraNormal;
    public ResourceLocation postShader;
    public FiguraVec2 crosshairOffset;
    public FiguraVec3 outlineColor;
    public ResourceLocation fireLayer1, fireLayer2;
    public Boolean renderLeftArm, renderRightArm;
    public FiguraVec3 eyeOffset;
    public FiguraVec4 blockOutlineColor;
    public Boolean upsideDown;
    public Boolean rootRotation;
    public Boolean renderArrows;

    public RendererAPI(Avatar owner) {
        this.owner = owner.owner;
    }

    private static boolean checkCameraOwner(UUID entity) {
        Entity e = Minecraft.getInstance().getCameraEntity();
        return e != null && e.getUUID().equals(entity);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.should_render_fire")
    public boolean shouldRenderFire() {
        return renderFire;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "renderFire"
            ),
            value = "renderer.set_render_fire")
    public RendererAPI setRenderFire(boolean renderFire) {
        this.renderFire = renderFire;
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.should_render_vehicle")
    public boolean shouldRenderVehicle() {
        return renderVehicle;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "renderVehicle"
            ),
            value = "renderer.set_render_vehicle")
    public RendererAPI setRenderVehicle(boolean renderVehicle) {
        this.renderVehicle = renderVehicle;
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.should_render_crosshair")
    public boolean shouldRenderCrosshair() {
        return renderCrosshair;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "renderCrosshair"
            ),
            value = "renderer.set_render_crosshair")
    public RendererAPI setRenderCrosshair(boolean renderCrosshair) {
        this.renderCrosshair = renderCrosshair;
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.should_force_paperdoll")
    public boolean shouldForcePaperdoll() {
        return forcePaperdoll;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "forcePaperdoll"
            ),
            value = "renderer.set_force_paperdoll")
    public RendererAPI setForcePaperdoll(boolean forcePaperdoll) {
        this.forcePaperdoll = forcePaperdoll;
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.should_render_hud")
    public boolean shouldRenderHUD() {
        return renderHUD;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "renderHUD"
            ),
            value = "renderer.set_render_hud")
    public RendererAPI setRenderHUD(boolean renderHUD) {
        this.renderHUD = renderHUD;
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.is_upside_down")
    public boolean isUpsideDown() {
        return upsideDown != null ? upsideDown : false;
    }

    @LuaWhitelist
    @LuaMethodDoc(overloads = @LuaMethodOverload(argumentTypes = Boolean.class, argumentNames = "upsideDown"), aliases = "upsideDown", value = "renderer.set_upside_down")
    public RendererAPI setUpsideDown(Boolean upsideDown) {
        this.upsideDown = upsideDown;
        return this;
    }

    @LuaWhitelist
    public RendererAPI upsideDown(Boolean upsideDown) {
        return setUpsideDown(upsideDown);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Float.class,
                            argumentNames = "radius"
                    )
            },
            aliases = "shadowRadius",
            value = "renderer.set_shadow_radius"
    )
    public RendererAPI setShadowRadius(Float shadowRadius) {
        this.shadowRadius = shadowRadius == null ? null : Mth.clamp(shadowRadius, 0f, 12f);
        return this;
    }

    @LuaWhitelist
    public RendererAPI shadowRadius(Float shadowRadius) {
        return setShadowRadius(shadowRadius);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_shadow_radius")
    public Float getShadowRadius() {
        return this.shadowRadius != null ? this.shadowRadius : 0.5f;
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.is_first_person")
    public boolean isFirstPerson() {
        return checkCameraOwner(this.owner) && Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.is_camera_backwards")
    public boolean isCameraBackwards() {
        return checkCameraOwner(this.owner) && Minecraft.getInstance().options.getCameraType().isMirrored();
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_camera_pos")
    public FiguraVec3 getCameraPos() {
        return this.cameraPos;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            aliases = "cameraPos",
            value = "renderer.set_camera_pos"
    )
    public RendererAPI setCameraPos(Object x, Double y, Double z) {
        this.cameraPos = LuaUtils.nullableVec3("setCameraPos", x, y, z);
        return this;
    }

    @LuaWhitelist
    public RendererAPI cameraPos(Object x, Double y, Double z) {
        return setCameraPos(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_camera_pivot")
    public FiguraVec3 getCameraPivot() {
        return this.cameraPivot;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pivot"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            aliases = "cameraPivot",
            value = "renderer.set_camera_pivot"
    )
    public RendererAPI setCameraPivot(Object x, Double y, Double z) {
        this.cameraPivot = LuaUtils.nullableVec3("setCameraPivot", x, y, z);
        return this;
    }

    @LuaWhitelist
    public RendererAPI cameraPivot(Object x, Double y, Double z) {
        return setCameraPivot(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_camera_offset_pivot")
    public FiguraVec3 getCameraOffsetPivot() {
        return this.cameraOffsetPivot;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pivot"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            aliases = "offsetCameraPivot",
            value = "renderer.set_offset_camera_pivot"
    )
    public RendererAPI setOffsetCameraPivot(Object x, Double y, Double z) {
        this.cameraOffsetPivot = LuaUtils.nullableVec3("setOffsetCameraPivot", x, y, z);
        return this;
    }

    @LuaWhitelist
    public RendererAPI offsetCameraPivot(Object x, Double y, Double z) {
        return setOffsetCameraPivot(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_camera_rot")
    public FiguraVec3 getCameraRot() {
        return this.cameraRot;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "rot"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            aliases = "cameraRot",
            value = "renderer.set_camera_rot"
    )
    public RendererAPI setCameraRot(Object x, Double y, Double z) {
        this.cameraRot = LuaUtils.nullableVec3("setCameraRot", x, y, z);
        return this;
    }

    @LuaWhitelist
    public RendererAPI cameraRot(Object x, Double y, Double z) {
        return setCameraRot(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_camera_offset_rot")
    public FiguraVec3 getCameraOffsetRot() {
        return this.cameraOffsetRot;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "rot"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            aliases = "offsetCameraRot",
            value = "renderer.set_offset_camera_rot"
    )
    public RendererAPI setOffsetCameraRot(Object x, Double y, Double z) {
        this.cameraOffsetRot = LuaUtils.nullableVec3("setOffsetCameraRot", x, y, z);
        return this;
    }

    @LuaWhitelist
    public RendererAPI offsetCameraRot(Object x, Double y, Double z) {
        return setOffsetCameraRot(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_camera_matrix")
    public FiguraMat4 getCameraMatrix() {
        return this.cameraMat;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = FiguraMat4.class,
                    argumentNames = "matrix"
            ),
            aliases = "cameraMatrix",
            value = "renderer.set_camera_matrix"
    )
    public RendererAPI setCameraMatrix(FiguraMat4 matrix) {
        this.cameraMat = matrix;
        return this;
    }

    @LuaWhitelist
    public RendererAPI cameraMatrix(FiguraMat4 matrix) {
        return setCameraMatrix(matrix);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_camera_normal")
    public FiguraMat3 getCameraNormal() {
        return this.cameraNormal;
    }

    @LuaWhitelist
    @LuaMethodDoc(overloads = @LuaMethodOverload(argumentTypes = FiguraMat3.class, argumentNames = "matrix"), aliases = "cameraNormal", value = "renderer.set_camera_normal")
    public RendererAPI setCameraNormal(FiguraMat3 matrix) {
        this.cameraNormal = matrix;
        return this;
    }

    @LuaWhitelist
    public RendererAPI cameraNormal(FiguraMat3 matrix) {
        return setCameraNormal(matrix);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "effect"
            ),
            aliases = "postEffect",
            value = "renderer.set_post_effect"
    )
    public RendererAPI setPostEffect(String effect) {
        this.postShader = effect == null ? null : LuaUtils.parsePath("shaders/post/" + effect + ".json");
        if (postShader != null && Minecraft.getInstance().getResourceManager().getResource(postShader).isEmpty())
            throw new LuaError("The post shader %s does not exist or could not be found".formatted(postShader.toString()));
        return this;
    }

    @LuaWhitelist
    public RendererAPI postEffect(String effect) {
        return setPostEffect(effect);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "bool"
            ),
            aliases = "renderArrows",
            value = "renderer.set_render_arrows"
    )
    public RendererAPI setRenderArrows(Boolean bool) {
        this.renderArrows = bool;
        return this;
    }
    @LuaWhitelist
    public RendererAPI renderArrows(Boolean bool) { return setRenderArrows(bool); }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_fov")
    public Float getFOV() {
        return this.fov;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Float.class,
                            argumentNames = "fov"
                    )
            },
            aliases = "fov",
            value = "renderer.set_fov"
    )
    public RendererAPI setFOV(Float fov) {
        this.fov = fov;
        return this;
    }

    @LuaWhitelist
    public RendererAPI fov(Float fov) {
        return setFOV(fov);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_crosshair_offset")
    public FiguraVec2 getCrosshairOffset() {
        return this.crosshairOffset;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec2.class,
                            argumentNames = "vec"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class},
                            argumentNames = {"x", "y"}
                    )
            },
            aliases = "crosshairOffset",
            value = "renderer.set_crosshair_offset")
    public RendererAPI setCrosshairOffset(Object x, Double y) {
        this.crosshairOffset = x == null ? null : LuaUtils.parseVec2("setCrosshairOffset", x, y);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_outline_color")
    public FiguraVec3 getOutlineColor() {
        return outlineColor;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "rgb"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"r", "g", "b"}
                    )
            },
            aliases = "outlineColor",
            value = "renderer.set_outline_color"
    )
    public RendererAPI setOutlineColor(Object r, Double g, Double b) {
        outlineColor = LuaUtils.nullableVec3("setOutlineColor", r, g, b);
        return this;
    }

    @LuaWhitelist
    public RendererAPI outlineColor(Object r, Double g, Double b) {
        return setOutlineColor(r, g, b);
    }

    @LuaWhitelist
    public RendererAPI crosshairOffset(Object x, Double y) {
        return setCrosshairOffset(x, y);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_primary_fire_texture")
    public String getPrimaryFireTexture() {
        return fireLayer1 != null ? fireLayer1.toString() : "";
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_secondary_fire_texture")
    public String getSecondaryFireTexture() {
        return fireLayer2 != null ? fireLayer2.toString() : "";
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "id"
            ),
            aliases = "primaryFireTexture",
            value = "renderer.set_primary_fire_texture"
    )
    public RendererAPI setPrimaryFireTexture(String id) {
        if (id == null) {
            fireLayer1 = null;
            return this;
        }

        fireLayer1 = LuaUtils.parsePath(id);
        if (fireLayer1.getPath().startsWith("textures/"))
            fireLayer1 = new ResourceLocation(fireLayer1.getNamespace(), fireLayer1.getPath().substring("textures/".length()));

        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "id"
            ),
            aliases = "secondaryFireTexture",
            value = "renderer.set_secondary_fire_texture"
    )
    public RendererAPI setSecondaryFireTexture(String id) {
        if (id == null) {
            fireLayer2 = null;
            return this;
        }

        fireLayer2 = LuaUtils.parsePath(id);
        if (fireLayer2.getPath().startsWith("textures/"))
            fireLayer2 = new ResourceLocation(fireLayer2.getNamespace(), fireLayer2.getPath().substring("textures/".length()));

        return this;
    }

    @LuaWhitelist
    public RendererAPI primaryFireTexture(String id) {
        return setPrimaryFireTexture(id);
    }

    @LuaWhitelist
    public RendererAPI secondaryFireTexture(String id) {
        return setSecondaryFireTexture(id);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "bool"
            ),
            aliases = "renderLeftArm",
            value = "renderer.set_render_left_arm"
    )
    public RendererAPI setRenderLeftArm(Boolean bool) {
        this.renderLeftArm = bool;
        return this;
    }

    @LuaWhitelist
    public RendererAPI renderLeftArm(Boolean bool) {
        return setRenderLeftArm(bool);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_render_left_arm")
    public Boolean getRenderLeftArm() {
        return this.renderLeftArm;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "bool"
            ),
            aliases = "renderRightArm",
            value = "renderer.set_render_right_arm"
    )
    public RendererAPI setRenderRightArm(Boolean bool) {
        this.renderRightArm = bool;
        return this;
    }

    @LuaWhitelist
    public RendererAPI renderRightArm(Boolean bool) {
        return setRenderRightArm(bool);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_render_right_arm")
    public Boolean getRenderRightArm() {
        return renderRightArm;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "pos"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            aliases = "eyeOffset",
            value = "renderer.set_eye_offset"
    )
    public RendererAPI setEyeOffset(Object x, Double y, Double z) {
        this.eyeOffset = LuaUtils.nullableVec3("setEyeOffset", x, y, z);
        return this;
    }

    @LuaWhitelist
    public RendererAPI eyeOffset(Object x, Double y, Double z) {
        return setEyeOffset(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_eye_offset")
    public FiguraVec3 getEyeOffset() {
        return eyeOffset;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "rgb"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec4.class,
                            argumentNames = "rgba"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"r", "g", "b", "a"}
                    )
            },
            aliases = "blockOutlineColor",
            value = "renderer.set_block_outline_color")
    public RendererAPI setBlockOutlineColor(Object r, Double g, Double b, Double a) {
        this.blockOutlineColor = r == null ? null : LuaUtils.parseVec4("setColor", r, g, b, a, 0, 0, 0, 0.4);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_block_outline_color")
    public FiguraVec4 getBlockOutlineColor() {
        return blockOutlineColor;
    }

    @LuaWhitelist
    public RendererAPI blockOutlineColor(Object r, Double g, Double b, Double a) {
        return setBlockOutlineColor(r, g, b, a);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "bool"
            ),
            aliases = "rootRotationAllowed",
            value = "renderer.set_root_rotation_allowed"
    )
    public RendererAPI setRootRotationAllowed(Boolean bool) {
        this.rootRotation = bool;
        return this;
    }

    @LuaWhitelist
    public RendererAPI rootRotationAllowed(Boolean bool) {
        return setRootRotationAllowed(bool);
    }

    @LuaWhitelist
    @LuaMethodDoc("renderer.get_root_rotation_allowed")
    public Boolean getRootRotationAllowed() {
        return rootRotation != null ? rootRotation : true;
    }

    @LuaWhitelist
    public Object __index(String arg) {
        if (arg == null) return null;
        return switch (arg) {
            case "renderFire" -> renderFire;
            case "renderVehicle" -> renderVehicle;
            case "renderCrosshair" -> renderCrosshair;
            case "forcePaperdoll" -> forcePaperdoll;
            case "renderHUD" -> renderHUD;
            default -> null;
        };
    }

    @LuaWhitelist
    public void __newindex(@LuaNotNil String key, boolean value) {
        switch (key) {
            case "renderFire" -> renderFire = value;
            case "renderVehicle" -> renderVehicle = value;
            case "renderCrosshair" -> renderCrosshair = value;
            case "forcePaperdoll" -> forcePaperdoll = value;
            case "renderHUD" -> renderHUD = value;
            default -> throw new LuaError("Cannot assign value on key \"" + key + "\"");
        }
    }

    @Override
    public String toString() {
        return "RendererAPI";
    }
}
