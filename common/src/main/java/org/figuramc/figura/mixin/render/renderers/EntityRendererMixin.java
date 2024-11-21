package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.EntityRendererAccessor;
import org.figuramc.figura.lua.api.nameplate.EntityNameplateCustomization;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.TextUtils;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> implements EntityRendererAccessor {

    @Inject(at = @At("HEAD"), method = "shouldRender", cancellable = true)
    private void shouldRender(T entity, Frustum frustum, double d, double e, double f, CallbackInfoReturnable<Boolean> cir) {
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (avatar != null && avatar.permissions.get(Permissions.OFFSCREEN_RENDERING) == 1)
            cir.setReturnValue(true);
    }

    @Unique
    Avatar figura$avatar;
    @Unique
    boolean figura$hasCustomNameplate;
    @Unique
    boolean figura$enabled;

    @Unique
    EntityNameplateCustomization figura$custom;

    @Unique
    List<Component> figura$textList;

    @Inject(at = @At(value = "HEAD"), method = "renderNameTag")
    private void setupAvatar(T entity, Component text, PoseStack matrices, MultiBufferSource vertexConsumers, int light, float delta, CallbackInfo ci) {
        figura$avatar = AvatarManager.getAvatar(entity);
        figura$custom = figura$avatar == null || figura$avatar.luaRuntime == null ? null : figura$avatar.luaRuntime.nameplate.ENTITY;
        figura$hasCustomNameplate = figura$custom != null && figura$avatar.permissions.get(Permissions.NAMEPLATE_EDIT) == 1;
        figura$enabled =  Configs.ENTITY_NAMEPLATE.value > 0 && !AvatarManager.panic && figura$hasCustomNameplate;


        figura$textList = TextUtils.splitText(text, "\n");
    }

    // Push pivot transformations when the nametag is being pivoted (set to entity height in vanilla)
    @WrapOperation(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"), method = "renderNameTag")
    private void modifyPivot(PoseStack instance, double x, double y, double z, Operation<Void> original) {
        FiguraVec3 pivot = FiguraVec3.of(x, y, z);
        if (figura$enabled && figura$avatar != null) {
            // pivot
            FiguraMod.pushProfiler("pivot");
            if (figura$hasCustomNameplate && figura$custom.getPivot() != null)
                pivot = figura$custom.getPivot();
        }
        original.call(instance, pivot.x, pivot.y, pivot.z);
    }

    // Push position transformations after the nametag has been rotated to face the camera
    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", shift = At.Shift.AFTER), method = "renderNameTag")
    private void modifyPos(T entity, Component text, PoseStack matrices, MultiBufferSource vertexConsumers, int light, float delta, CallbackInfo ci) {
        if (figura$enabled && figura$avatar != null) {
            // pos
            FiguraMod.popPushProfiler("position");
            if (figura$hasCustomNameplate && figura$custom.getPos() != null) {
                FiguraVec3 pos = figura$custom.getPos();
                matrices.translate(pos.x, pos.y, pos.z);
            }
        }
    }

    // push the scale when vanilla does so
    @WrapOperation(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"), method = "renderNameTag")
    private void modifyScale(PoseStack instance, float x, float y, float z, Operation<Void> original) {
        FiguraVec3 scaleVec = FiguraVec3.of(x, y, z);
        if (figura$enabled && figura$avatar != null) {
            // scale
            FiguraMod.popPushProfiler("scale");
            if (figura$hasCustomNameplate && figura$custom.getScale() != null)
                scaleVec.multiply(figura$custom.getScale());
        }
        original.call(instance, (float) scaleVec.x, (float) scaleVec.y, (float) scaleVec.z);
    }



    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;pose()Lorg/joml/Matrix4f;"), method = "renderNameTag")
    private void setShadowMatrix(T entity, Component text, PoseStack matrices, MultiBufferSource vertexConsumers, int light, float delta, CallbackInfo ci, @Share("textMatrix") LocalRef<Matrix4f> textMatrix) {
        textMatrix.set(matrices.last().pose());
        if (figura$enabled && figura$avatar != null && figura$hasCustomNameplate && figura$custom.shadow) {
            matrices.pushPose();
            textMatrix.set(matrices.last().pose());
            matrices.popPose();
        }
    }

    @WrapOperation(method = "renderNameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I", ordinal = 0))
    private int drawWithColor(Font font, Component component, float x, float y, int color, boolean shadow, Matrix4f matrix4f, MultiBufferSource multiBufferSource, Font.DisplayMode displayMode, int backgroundColor, int light, Operation<Integer> original) {
        if (figura$enabled && figura$avatar != null && figura$hasCustomNameplate) {
            light = figura$custom.light != null ? figura$custom.light : light;
            backgroundColor = figura$custom.background != null ? figura$custom.background : backgroundColor;
            boolean deadmau = component.getString().equals("deadmau5");

            // This renders the translucent part of the nametag you see when shifting, and the background
            if (figura$isRenderingName()) {
                int ret = 0;
                // If the player's name is being rendered, render by lines otherwise just render whatever component is being passed. Applies for the rest of the loops below
                for (int i = 0; i < figura$textList.size(); i++) {
                    Component text1 = figura$textList.get(i);

                    if (text1.getString().isEmpty())
                        continue;

                    int line = i - figura$textList.size() + 1;
                    x = -font.width(text1) / 2f;
                    y = (deadmau ? -10f : 0f) + (font.lineHeight + 1) * line;
                    ret = original.call(font, text1, x, y, color, shadow, matrix4f, multiBufferSource, displayMode, backgroundColor, light);
                }
                return ret;
            } else {
                return original.call(font, component, x, y, color, shadow, matrix4f, multiBufferSource, displayMode, backgroundColor, light);
            }
        } else {
            return original.call(font, component, x, y, color, shadow, matrix4f, multiBufferSource, displayMode, backgroundColor, light);
        }
    }

    @WrapOperation(method = "renderNameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I", ordinal = 1))
    private int drawWithOutline(Font font, Component component, float x, float y, int color, boolean shadow, Matrix4f matrix4f, MultiBufferSource multiBufferSource, Font.DisplayMode displayMode, int backgroundColor, int light, Operation<Integer> original, @Share("textMatrix") LocalRef<Matrix4f> textMatrix) {
        light = figura$enabled && figura$avatar != null && figura$hasCustomNameplate && figura$custom.light != null ? figura$custom.light : light;
        shadow = figura$enabled && figura$avatar != null && figura$hasCustomNameplate ? figura$custom.shadow : shadow;
        boolean deadmau = component.getString().equals("deadmau5");

        if (figura$enabled && figura$avatar != null && figura$hasCustomNameplate && figura$custom.outline) {
            // This renders the opaque text with an outline if the player has that enabled.
            int outlineColor = figura$custom.outlineColor != null ? figura$custom.outlineColor : 0x202020;
            if (figura$isRenderingName()) {
                for (int i = 0; i < figura$textList.size(); i++) {
                    Component text1 = figura$textList.get(i);

                    if (text1.getString().isEmpty())
                        continue;

                    int line = i - figura$textList.size() + 1;
                    x = -font.width(text1) / 2f;
                    y = (deadmau ? -10f : 0f) + (font.lineHeight + 1) * line;
                    font.drawInBatch8xOutline(text1.getVisualOrderText(), x, y, color, outlineColor, matrix4f, multiBufferSource, light);
                }
            } else {
                font.drawInBatch8xOutline(component.getVisualOrderText(), x, y, color, outlineColor, matrix4f, multiBufferSource, light);
            }
            return original.call(font, Component.empty(), x, y, color, shadow, textMatrix.get(), multiBufferSource, displayMode, backgroundColor, light);
        } else {
            if (figura$enabled && figura$avatar != null && figura$hasCustomNameplate && figura$isRenderingName()) {
                int ret = 0;
                // This renders the opaque part of the nametag, that is text
                for (int i = 0; i < figura$textList.size(); i++) {
                    Component text1 = figura$textList.get(i);

                    if (text1.getString().isEmpty())
                        continue;

                    int line = i - figura$textList.size() + 1;
                    x = -font.width(text1) / 2f;
                    y = (deadmau ? -10f : 0f) + (font.lineHeight + 1) * line;
                    ret = original.call(font, text1, x, y, color, shadow, textMatrix.get(), multiBufferSource, displayMode, backgroundColor, light);
                }

                return ret;
            } else {
                return original.call(font, component, x, y, color, shadow, textMatrix.get(), multiBufferSource, displayMode, backgroundColor, light);
            }
        }
    }
}
