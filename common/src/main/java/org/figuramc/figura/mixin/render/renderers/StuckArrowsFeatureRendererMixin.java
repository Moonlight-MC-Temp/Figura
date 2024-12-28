package org.figuramc.figura.mixin.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.permissions.Permissions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArrowLayer.class)
public abstract class StuckArrowsFeatureRendererMixin {
	@Inject(method = "renderStuckItem", at = @At("HEAD"), cancellable = true)
	private void disableStuckArrowsRendering(PoseStack matrices, MultiBufferSource vertexConsumers, int light, Entity entity, float directionX, float directionY, float directionZ, float tickDelta, CallbackInfo ci) {
		Avatar avatar = AvatarManager.getAvatar(entity);
		if (avatar != null && avatar.luaRuntime != null
			&& avatar.luaRuntime.renderer.renderArrows != null
			&& !avatar.luaRuntime.renderer.renderArrows
			&& avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1) {
				ci.cancel();
		}
	}
}
