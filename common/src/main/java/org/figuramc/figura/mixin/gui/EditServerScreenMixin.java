package org.figuramc.figura.mixin.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.ducks.ServerDataAccessor;
import org.figuramc.figura.utils.FiguraText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditServerScreen.class)
public abstract class EditServerScreenMixin extends Screen {
    @Shadow private Button addButton;

    @Shadow @Final private ServerData serverData;
    private Checkbox fsbState;

    protected EditServerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/EditServerScreen;setInitialFocus(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V"))
    private void onInit(CallbackInfo ci) {
        int x = this.width / 2 + 102;
        int y = this.height / 4 + 72;
        int width = 20;
        int height = 20;
        ServerDataAccessor data = (ServerDataAccessor) serverData;
        fsbState = new Checkbox(x, y, width, height, FiguraText.of("fsb"), data.figura$allowFigura());
        addRenderableWidget(fsbState);
    }

    @Inject(method = "onAdd", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/booleans/BooleanConsumer;accept(Z)V"))
    private void onAdd(CallbackInfo ci) {
        ServerDataAccessor data = (ServerDataAccessor) serverData;
        data.figura$setAllowFigura(fsbState.selected());
    }
}
