package org.figuramc.figura.mixin.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.component.DyedItemColor;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.compat.GeckoLibCompat;
import org.figuramc.figura.lua.api.vanilla_model.VanillaPart;
import org.figuramc.figura.mixin.render.layers.HumanoidArmorLayerAccessor;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.FiguraArmorPartRenderer;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HumanoidArmorLayer.class, priority = 900)
public abstract class HumanoidArmorLayerMixinForge<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> implements HumanoidArmorLayerAccessor<T, M, A> {

    @Shadow
    protected abstract A getArmorModel(EquipmentSlot slot);

    @Shadow
    @Final
    private TextureAtlas armorTrimAtlas;

    @Shadow
    protected abstract void renderArmorPiece(PoseStack matrices, MultiBufferSource vertexConsumers, T entity, EquipmentSlot armorSlot, int light, A model);

    @Shadow @Final private A innerModel;
    @Shadow @Final private A outerModel;

    @Unique
    private boolean figura$renderingVanillaArmor;

    @Unique
    private Avatar figura$avatar;

    public HumanoidArmorLayerMixinForge(RenderLayerParent<T, M> context) {
        super(context);
    }

    @Inject(at = @At(value = "HEAD"), method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V")
    public void setAvatar(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        figura$avatar = AvatarManager.getAvatar(livingEntity);
    }

    @Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 3, target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V"), method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V")
    public void onRenderEnd(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        if (figura$avatar == null) return;

        figura$tryRenderArmorPart(EquipmentSlot.HEAD,  this::figura$helmetRenderer, poseStack, livingEntity, multiBufferSource, i, ParentType.HelmetPivot);
        figura$tryRenderArmorPart(EquipmentSlot.CHEST, this::figura$chestplateRenderer, poseStack, livingEntity, multiBufferSource, i, ParentType.LeftShoulderPivot, ParentType.ChestplatePivot, ParentType.RightShoulderPivot);
        figura$tryRenderArmorPart(EquipmentSlot.LEGS,  this::figura$leggingsRenderer, poseStack, livingEntity, multiBufferSource, i, ParentType.LeftLeggingPivot, ParentType.RightLeggingPivot, ParentType.LeggingsPivot);
        figura$tryRenderArmorPart(EquipmentSlot.FEET,  this::figura$bootsRenderer, poseStack, livingEntity, multiBufferSource, i, ParentType.LeftBootPivot, ParentType.RightBootPivot);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;usesInnerModel(Lnet/minecraft/world/entity/EquipmentSlot;)Z"), method = "renderArmorPiece")
    public void onRenderArmorPiece(PoseStack poseStack, MultiBufferSource multiBufferSource, T livingEntity, EquipmentSlot equipmentSlot, int i, A humanoidModel, CallbackInfo ci) {
        if (figura$avatar == null) return;

        VanillaPart part = RenderUtils.partFromSlot(figura$avatar, equipmentSlot);
        if (part != null) {
            part.save(humanoidModel);
            part.preTransform(humanoidModel);
            part.posTransform(humanoidModel);
        }
    }


    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;setPartVisibility(Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/world/entity/EquipmentSlot;)V", shift = At.Shift.AFTER), method = "renderArmorPiece", cancellable = true)
    public void renderArmorPieceHijack(PoseStack poseStack, MultiBufferSource multiBufferSource, T livingEntity, EquipmentSlot equipmentSlot, int i, A humanoidModel, CallbackInfo ci) {
        if (figura$avatar == null) return;

        if (!figura$renderingVanillaArmor) {
            ci.cancel();
        }
    }


    @Inject(at = @At("RETURN"), method = "renderArmorPiece")
    public void postRenderArmorPiece(PoseStack poseStack, MultiBufferSource multiBufferSource, T livingEntity, EquipmentSlot equipmentSlot, int i, A humanoidModel, CallbackInfo ci) {
        if (figura$avatar == null) return;

        VanillaPart part = RenderUtils.partFromSlot(figura$avatar, equipmentSlot);
        if (part != null)
            part.restore(humanoidModel);
    }

    @Unique
    private void figura$tryRenderArmorPart(EquipmentSlot slot, FiguraArmorPartRenderer<T, M, A> renderer, PoseStack vanillaPoseStack, T entity, MultiBufferSource vertexConsumers, int light, ParentType... parentTypes) {
        if (slot == null) return; // ?

        ItemStack itemStack = entity.getItemBySlot(slot);

        // Make sure the item in the equipment slot is actually a piece of armor
        if ((itemStack.getItem() instanceof ArmorItem armorItem && armorItem.getEquipmentSlot() == slot)) {
            A armorModel = getArmorModel(slot);

            // Bones have to be their defaults to prevent issues with clipping
            armorModel.body.xRot = 0.0f;
            armorModel.rightLeg.z = 0.0f;
            armorModel.leftLeg.z = 0.0f;
            armorModel.rightLeg.y = 12.0f;
            armorModel.leftLeg.y = 12.0f;
            armorModel.head.y = 0.0f;
            armorModel.body.y = 0.0f;
            armorModel.leftArm.y = 2.0f;
            armorModel.rightArm.y = 2.0f;
            armorModel.leftArm.x = 5.0f;
            armorModel.rightArm.x = -5.0f;
            armorModel.leftArm.z = 0.0f;
            armorModel.rightArm.z = 0.0f;

            boolean allFailed = true;
            VanillaPart mainPart = RenderUtils.partFromSlot(figura$avatar, slot);
            int armorEditPermission = figura$avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT);
            if (armorEditPermission == 1 && mainPart != null && !mainPart.checkVisible()) return;

            // Don't render armor if GeckoLib is already doing the rendering
            if (!GeckoLibCompat.armorHasCustomModel(itemStack)) {
                // Go through each parent type needed to render the current piece of armor
                for (ParentType parentType : parentTypes) {
                    // Skip the part if it's hidden
                    VanillaPart part = RenderUtils.pivotToPart(figura$avatar, parentType);
                    if (armorEditPermission == 1 && part != null && !part.checkVisible()) continue;
                    boolean renderedPivot = false;
                    // If the user has no permission disable pivot
                    if (armorEditPermission == 1) {
                        // Try to render the pivot part
                        renderedPivot = figura$avatar.pivotPartRender(parentType, stack -> {
                            stack.pushPose();
                            figura$prepareArmorRender(stack);
                            renderer.renderArmorPart(stack, vertexConsumers, light, armorModel, entity, itemStack, slot, armorItem, parentType);
                            stack.popPose();
                        });
                    }
                    if (renderedPivot) {
                        allFailed = false;
                    }
                }
            }
            // As a fallback, render armor the vanilla way
            if (allFailed) {
                figura$renderingVanillaArmor = true;
                renderArmorPiece(vanillaPoseStack, vertexConsumers, entity, slot, light, armorModel);
                figura$renderingVanillaArmor = false;
            }
        }

    }

    // Prepare the transformations for rendering armor on the avatar
    @Unique
    private void figura$prepareArmorRender(PoseStack stack) {
        stack.scale(16, 16, 16);
        stack.mulPose(Axis.XP.rotationDegrees(180f));
        stack.mulPose(Axis.YP.rotationDegrees(180f));
    }

    @Unique
    private void figura$helmetRenderer(PoseStack poseStack, MultiBufferSource vertexConsumers, int light, A model, T entity, ItemStack itemStack, EquipmentSlot armorSlot, ArmorItem armorItem, ParentType parentType) {
        if (parentType == ParentType.HelmetPivot) {
            figura$renderArmorPart(model.head, poseStack, vertexConsumers, light, entity, itemStack, armorSlot, armorItem);
            figura$renderArmorPart(model.hat, poseStack, vertexConsumers, light, entity, itemStack, armorSlot, armorItem);
        }
    }

    @Unique
    private void figura$chestplateRenderer(PoseStack poseStack, MultiBufferSource vertexConsumers, int light, A model, T entity, ItemStack itemStack, EquipmentSlot armorSlot, ArmorItem armorItem, ParentType parentType) {
        if (parentType == ParentType.ChestplatePivot) {
            figura$renderArmorPart(model.body, poseStack, vertexConsumers, light, entity, itemStack, armorSlot, armorItem);
        }

        if (parentType == ParentType.LeftShoulderPivot) {
            poseStack.pushPose();
            poseStack.translate(-6 / 16f, 0f, 0f);
            figura$renderArmorPart(model.leftArm, poseStack, vertexConsumers, light, entity, itemStack, armorSlot, armorItem);
            poseStack.popPose();
        }

        if (parentType == ParentType.RightShoulderPivot) {
            poseStack.pushPose();
            poseStack.translate(6 / 16f, 0f, 0f);
            figura$renderArmorPart(model.rightArm, poseStack, vertexConsumers, light, entity, itemStack, armorSlot, armorItem);
            poseStack.popPose();
        }
    }

    @Unique
    private void figura$leggingsRenderer(PoseStack poseStack, MultiBufferSource vertexConsumers, int light, A model, T entity, ItemStack itemStack, EquipmentSlot armorSlot, ArmorItem armorItem, ParentType parentType) {
        if (parentType == ParentType.LeggingsPivot) {
            poseStack.pushPose();
            poseStack.translate(0, -12 / 16f, 0);
            figura$renderArmorPart(model.body, poseStack, vertexConsumers, light, entity, itemStack, armorSlot, armorItem);
            poseStack.popPose();
        }

        if (parentType == ParentType.LeftLeggingPivot) {
            poseStack.pushPose();
            poseStack.translate(-2 / 16f, -12 / 16f, 0);
            figura$renderArmorPart(model.leftLeg, poseStack, vertexConsumers, light, entity, itemStack, armorSlot, armorItem);
            poseStack.popPose();
        }

        if (parentType == ParentType.RightLeggingPivot) {
            poseStack.pushPose();
            poseStack.translate(2 / 16f, -12 / 16f, 0);
            figura$renderArmorPart(model.rightLeg, poseStack, vertexConsumers, light, entity, itemStack, armorSlot, armorItem);
            poseStack.popPose();
        }
    }

    @Unique
    private void figura$bootsRenderer(PoseStack poseStack, MultiBufferSource vertexConsumers, int light, A model, T entity, ItemStack itemStack, EquipmentSlot armorSlot, ArmorItem armorItem, ParentType parentType) {
        if (parentType == ParentType.LeftBootPivot) {
            poseStack.pushPose();
            poseStack.translate(-2 / 16f, -24 / 16f, 0);
            figura$renderArmorPart(model.leftLeg, poseStack, vertexConsumers, light, entity, itemStack, armorSlot, armorItem);
            poseStack.popPose();
        }

        if (parentType == ParentType.RightBootPivot) {
            poseStack.pushPose();
            poseStack.translate(2 / 16f, -24 / 16f, 0);
            figura$renderArmorPart(model.rightLeg, poseStack, vertexConsumers, light, entity, itemStack, armorSlot, armorItem);
            poseStack.popPose();
        }
    }


    // Similar to vanilla's renderArmorModel, but it renders each part individually, instead of the whole model at once.
    // Could be optimized by calculating the tint, overlays, and trims beforehand instead of re-calculating for each ModelPart, but it's not super important.
    @Unique
    private void figura$renderArmorPart(ModelPart modelPart, PoseStack poseStack, MultiBufferSource vertexConsumers, int light, T entity, ItemStack itemStack, EquipmentSlot armorSlot, ArmorItem armorItem) {
        boolean bl = this.usesInnerModel(armorSlot);
        boolean hasGlint = itemStack.hasFoil();

        modelPart.visible = true;
        modelPart.xRot = 0;
        modelPart.yRot = 0;
        modelPart.zRot = 0;

        int i = itemStack.is(ItemTags.DYEABLE) ? DyedItemColor.getOrDefault(itemStack, -6265536) : -1;

        ArmorMaterial material = armorItem.getMaterial().value();
        for(ArmorMaterial.Layer layer : material.layers()) {
            ResourceLocation normalArmorResource = RenderUtils.getArmorResource(entity, itemStack, armorItem, armorSlot, bl, layer);

            VertexConsumer regularArmorConsumer = vertexConsumers.getBuffer(RenderType.armorCutoutNoCull(normalArmorResource));
            modelPart.render(poseStack, regularArmorConsumer, light, OverlayTexture.NO_OVERLAY, i);
        }

        ArmorTrim trim = itemStack.get(DataComponents.TRIM);
        if (trim != null) {
            var armorMaterial = armorItem.getMaterial();
            TextureAtlasSprite trimAtlas = this.armorTrimAtlas.getSprite(bl ? trim.innerTexture(armorMaterial) : trim.outerTexture(armorMaterial));
            VertexConsumer trimConsumer = trimAtlas.wrap(vertexConsumers.getBuffer(Sheets.armorTrimsSheet(false)));
            modelPart.render(poseStack, trimConsumer, light, OverlayTexture.NO_OVERLAY, -1);
        }

        if (hasGlint) {
            modelPart.render(poseStack, vertexConsumers.getBuffer(RenderType.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY, -1);
        }
    }
}
