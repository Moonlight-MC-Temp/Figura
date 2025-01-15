package org.figuramc.figura;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.local.CacheAvatarLoader;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.compat.GeckoLibCompat;
import org.figuramc.figura.compat.SimpleVCCompat;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.entries.EntryPointManager;
import org.figuramc.figura.font.Emojis;
import org.figuramc.figura.lua.FiguraLuaPrinter;
import org.figuramc.figura.lua.docs.FiguraDocsManager;
import org.figuramc.figura.mixin.SkullBlockEntityAccessor;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.resources.FiguraRuntimeResources;
import org.figuramc.figura.utils.*;
import org.figuramc.figura.wizards.AvatarWizard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class FiguraMod {

    public static final String MOD_ID = "figura";
    public static final String MOD_NAME = "Figura";
    public static final FiguraModMetadata METADATA = FiguraModMetadata.getMetadataForMod(MOD_ID);
    public static final Version VERSION = new Version(PlatformUtils.getFiguraModVersionString());
    public static final Calendar CALENDAR = Calendar.getInstance();
    public static final Path GAME_DIR = PlatformUtils.getGameDir().normalize();
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    public static final float VERTEX_OFFSET = -0.0005f;

    public static int ticks;
    public static Entity extendedPickEntity;
    public static Component splashText;
    public static boolean parseMessages = true;
    public static boolean processingKeybind;

    /* For some reason, the mod menu entrypoint (or something) is able to call this before the Config
    class can initialize, meaning Configs.DEBUG_MODE can be null when this is called.... Weird */
    @SuppressWarnings("all")
    public static boolean debugModeEnabled() {
        return Configs.DEBUG_MODE != null && Configs.DEBUG_MODE.value;
    }

    public static void onClientInit() {
        // init managers
        EntryPointManager.init();
        PermissionManager.init();
        LocalAvatarFetcher.init();
        CacheAvatarLoader.init();
        FiguraDocsManager.init();
        FiguraRuntimeResources.init();

        GeckoLibCompat.init();
        SimpleVCCompat.init();
    }

    public static List<FiguraResourceListener> getResourceListeners() {
        List<FiguraResourceListener> listeners = new ArrayList<>();
        listeners.add(LocalAvatarLoader.AVATAR_LISTENER);
        listeners.add(Emojis.RESOURCE_LISTENER);
        listeners.add(AvatarWizard.RESOURCE_LISTENER);
        listeners.add(AvatarManager.RESOURCE_RELOAD_EVENT);
        return listeners;
    }


    public static void tick() {
        pushProfiler("network");
        NetworkStuff.tick();
        FSB.instance().tick();
        popPushProfiler("files");
        LocalAvatarLoader.tick();
        LocalAvatarFetcher.tick();
        if (Minecraft.getInstance().player != null) {
            popPushProfiler("avatars");
            AvatarManager.tickLoadedAvatars();
        }
        popPushProfiler("chatPrint");
        FiguraLuaPrinter.printChatFromQueue();
        popPushProfiler("emojiAnim");
        Emojis.tickAnimations();
        popProfiler();
        ticks++;
    }

    // -- Helper Functions -- //

    // debug print
    public static void debug(String str, Object... args) {
        if (FiguraMod.debugModeEnabled()) LOGGER.info("[DEBUG] " + str, args);
        else LOGGER.debug(str, args);
    }

    // mod root directory
    public static Path getFiguraDirectory() {
        String config = Configs.MAIN_DIR.value;
        Path p = config.isBlank() ? GAME_DIR.resolve(MOD_ID) : Path.of(config);
        return IOUtils.createDirIfNeeded(p);
    }

    // mod cache directory
    public static Path getCacheDirectory() {
        return IOUtils.getOrCreateDir(getFiguraDirectory(), Configs.LOCAL_ASSETS.value ? "local_cache" : "cache");
    }

    // get local player uuid
    public static UUID getLocalPlayerUUID() {
        Entity player = Minecraft.getInstance().player;
        return player != null ? player.getUUID() : Minecraft.getInstance().getUser().getGameProfile().getId();
    }

    public static boolean isLocal(UUID other) {
        return getLocalPlayerUUID().equals(other);
    }

    public static boolean isOffline(UUID other) {
        return !Minecraft.getInstance().getConnection().getOnlinePlayerIds().contains(other);
    }

    /**
     * Sends a chat message right away. Use when you know your message is safe.
     * If your message is unsafe, (generated by a user), use luaSendChatMessage instead.
     *
     * @param message - text to send
     */
    public static void sendChatMessage(Component message) {
        if (Minecraft.getInstance().gui != null) {
            parseMessages = false;
            Minecraft.getInstance().gui.getChat().addMessage(TextUtils.replaceTabs(message));
            parseMessages = true;
        } else {
            LOGGER.info(message.getString());
        }
    }

    /**
     * Converts a player name to UUID using minecraft internal functions.
     *
     * @param playerName - the player name
     * @return - the player's uuid or null
     */
    public static UUID playerNameToUUID(String playerName) {
        GameProfileCache cache = SkullBlockEntityAccessor.getProfileCache();
        if (cache == null) return null;

        var profile = cache.get(playerName);
        return profile.isEmpty() ? null : profile.get().getId();
    }

    public static Style getAccentColor() {
        Avatar avatar = AvatarManager.getAvatarForPlayer(getLocalPlayerUUID());
        int color = avatar != null ? ColorUtils.rgbToInt(ColorUtils.userInputHex(avatar.color, ColorUtils.Colors.AWESOME_BLUE.vec)) : ColorUtils.Colors.AWESOME_BLUE.hex;
        return Style.EMPTY.withColor(color);
    }

    // -- profiler -- //

    public static void pushProfiler(String name) {
        Minecraft.getInstance().getProfiler().push(name);
    }

    public static void pushProfiler(Avatar avatar) {
        Minecraft.getInstance().getProfiler().push(avatar.entityName.isBlank() ? avatar.owner.toString() : avatar.entityName);
    }

    public static void popPushProfiler(String name) {
        Minecraft.getInstance().getProfiler().popPush(name);
    }

    public static void popProfiler() {
        Minecraft.getInstance().getProfiler().pop();
    }

    public static <T> T popReturnProfiler(T var) {
        Minecraft.getInstance().getProfiler().pop();
        return var;
    }

    public static void popProfiler(int times) {
        var profiler = Minecraft.getInstance().getProfiler();
        for (int i = 0; i < times; i++)
            profiler.pop();
    }

    public enum Links {
        Wiki("https://wiki.figuramc.org/", ColorUtils.Colors.AWESOME_BLUE.style),
        Kofi("https://ko-fi.com/skyrina", ColorUtils.Colors.KOFI.style),
        OpenCollective("https://opencollective.com/figura", ColorUtils.Colors.KOFI.style),
        Discord("https://discord.figuramc.org/", ColorUtils.Colors.DISCORD.style),
        Github("https://github.com/FiguraMC/Figura", ColorUtils.Colors.GITHUB.style),
        Modrinth("https://modrinth.com/mod/figura", ColorUtils.Colors.MODRINTH.style),
        Curseforge("https://www.curseforge.com/minecraft/mc-mods/figura", ColorUtils.Colors.CURSEFORGE.style),
        LuaManual("https://www.lua.org/manual/5.2/manual.html", ColorUtils.Colors.LUA_LOG.style);

        public final String url;
        public final Style style;

        Links(String url, Style style) {
            this.url = url;
            this.style = style;
        }
    }
}
