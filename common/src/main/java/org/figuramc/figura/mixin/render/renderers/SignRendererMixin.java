package org.figuramc.figura.mixin.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignText;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.font.Emojis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignRenderer.class)
public class SignRendererMixin {

    @Unique private SignText figura$signText;

    @Inject(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/SignText;getRenderMessages(ZLjava/util/function/Function;)[Lnet/minecraft/util/FormattedCharSequence;", shift = At.Shift.BEFORE))
    private void captureSignText(BlockPos pos, SignText text, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int lineHeight, int lineWidth, boolean front, CallbackInfo ci) {
        figura$signText = text;
    }

    // method_45799 corresponds to fabric intermediary, lambda$renderSignText$2 is the unmapped OF name, m_276705_ is the SRG name for Forge
    @ModifyArg(method = {"method_45799", "lambda$renderSignText$2", "m_276705_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;", remap = true), remap = false)
    private FormattedText modifyText(FormattedText charSequence) {
        if (!(Configs.EMOJIS.value > 0 && charSequence instanceof Component text)) return charSequence;

        MutableComponent test = MutableComponent.create(text.getContents());
        if (figura$signText.getColor() == DyeColor.BLACK) {
            test = test.withStyle(Style.EMPTY);
        } else {
            test = test.withStyle(Style.EMPTY.withColor(figura$signText.getColor().getTextColor()));
        }


        return Emojis.applyEmojis(test);
    }
}
