package org.figuramc.figura.forge;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.event.EventNetworkChannel;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.backend2.FSBForge;
import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.config.forge.ModConfig;
import org.figuramc.figura.gui.forge.GuiOverlay;
import org.figuramc.figura.gui.forge.GuiUnderlay;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.s2c.Handlers;
import org.figuramc.figura.server.packets.handlers.s2c.S2CPacketHandler;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;
import org.figuramc.figura.utils.forge.FiguraResourceListenerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = FiguraMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FiguraModClientForge extends FiguraMod {
    // keybinds stored here
    public static List<KeyMapping> KEYBINDS = new ArrayList<>();

    @SubscribeEvent
    public static void onInitializeClient(FMLClientSetupEvent event) {
        onClientInit();
        ModConfig.registerConfigScreen();
        for (VanillaGuiOverlay overlay : VanillaGuiOverlay.values()) {
            vanillaOverlays.add(overlay.type());
        }
    }

    @SubscribeEvent
    public static void registerResourceListener(RegisterClientReloadListenersEvent event) {
        getResourceListeners().forEach(figuraResourceListener -> event.registerReloadListener((FiguraResourceListenerImpl)figuraResourceListener));
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("figura_overlay", new GuiOverlay());
        event.registerBelowAll("figura_underlay", new GuiUnderlay());
    }

    private static final List<NamedGuiOverlay> vanillaOverlays = new ArrayList<>();

    public static void cancelVanillaOverlays(RenderGuiOverlayEvent.Pre event) {
        if (vanillaOverlays.contains(event.getOverlay())) {
            Entity entity = Minecraft.getInstance().getCameraEntity();
            Avatar avatar = entity == null ? null : AvatarManager.getAvatar(entity);
            if (avatar != null && avatar.luaRuntime != null && !avatar.luaRuntime.renderer.renderHUD) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void registerKeyBinding(RegisterKeyMappingsEvent event) {
        // Config has to be initialized here, so that the keybinds exist on time
        ConfigManager.init();
        for (KeyMapping value : KEYBINDS) {
            if(value != null)
                event.register(value);
        }
    }

    static void initClient() {
        MinecraftForge.EVENT_BUS.addListener(FiguraModClientForge::cancelVanillaOverlays);
        new FSBForge();
    }

    public static void registerPacketListener(Identifier id, EventNetworkChannel channel) {
        var handler = Handlers.getHandler(id);
        if (handler != null) channel.addListener(new ForgeNetworkListener<>(id, handler));
    }

    private static final class ForgeNetworkListener<P extends Packet> implements Consumer<NetworkEvent> {
        private final Identifier id;
        private final S2CPacketHandler<P> handler;

        private ForgeNetworkListener(Identifier id, S2CPacketHandler<P> handler) {
            this.id = id;
            this.handler = handler;
        }

        @Override
        public void accept(NetworkEvent event) {
            if (event.getPayload() == null) return;
            var ctx = event.getSource().get();
            if (ctx.getDirection().equals(NetworkDirection.PLAY_TO_CLIENT)) {
                try {
                    P packet = handler.serialize(new FriendlyByteBufWrapper(event.getPayload()));
                    Minecraft.getInstance().execute(() -> handler.handle(packet));
                    ctx.setPacketHandled(true);
                }
                catch (Exception e) {
                    FiguraMod.LOGGER.error("Failed to handle packet %s".formatted(id), e);
                }
            }
        }
    }
}
