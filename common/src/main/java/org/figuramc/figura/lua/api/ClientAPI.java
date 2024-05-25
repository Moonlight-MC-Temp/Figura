package org.figuramc.figura.lua.api;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.lua.api.entity.ViewerAPI;
import org.figuramc.figura.lua.docs.FiguraListDocs;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.mixin.gui.GuiAccessor;
import org.figuramc.figura.mixin.gui.PlayerTabOverlayAccessor;
import org.figuramc.figura.mixin.render.ModelManagerAccessor;
import org.figuramc.figura.utils.*;
import org.joml.Vector3f;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@LuaWhitelist
@LuaTypeDoc(
        name = "ClientAPI",
        value = "client"
)
public class ClientAPI {

    public static final ClientAPI INSTANCE = new ClientAPI();
    private static final HashMap<String, Boolean> LOADED_MODS = new HashMap<>();
    private static final boolean HAS_IRIS = PlatformUtils.isModLoaded("iris") || PlatformUtils.isModLoaded("oculus"); // separated to avoid indexing the list every frame
    public static final Supplier<Boolean> OPTIFINE_LOADED = Suppliers.memoize(() ->
    {
        try
        {
            Class.forName("net.optifine.Config");
            return true;
        }
        catch (ClassNotFoundException ignored)
        {
            return false;
        }
    });
    public static boolean hasOptifineShader() {
        try
        {
            Field shaderPackLoadedField = Class.forName("net.optifine.shaders.Shaders").getField("shaderPackLoaded");
            Class<?> shaderClass = shaderPackLoadedField.getType();
            if (shaderClass == boolean.class)
                return shaderPackLoadedField.getBoolean(null);
        }
        catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {}
        return false;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_fps")
    public static int getFPS() {
        String s = getFPSString();
        if (s.length() == 0)
            return 0;
        return Integer.parseInt(s.split(" ")[0]);
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_fps_string")
    public static String getFPSString() {
        return Minecraft.getInstance().fpsString;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.is_paused")
    public static boolean isPaused() {
        return Minecraft.getInstance().isPaused();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_version")
    public static String getVersion() {
        return SharedConstants.getCurrentVersion().getId();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_version_name")
    public static String getVersionName() {
        return SharedConstants.getCurrentVersion().getName();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.is_snapshot")
    public static boolean isSnapshot() {
        return !SharedConstants.getCurrentVersion().isStable();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_client_brand")
    public static String getClientBrand() {
        return ClientBrandRetriever.getClientModName();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_server_brand")
    public static String getServerBrand() {
        if (Minecraft.getInstance().player == null)
            return null;

        return Minecraft.getInstance().getSingleplayerServer() == null ? Minecraft.getInstance().player.getServerBrand() : "Integrated";
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_chunk_statistics")
    public static String getChunkStatistics() {
        return Minecraft.getInstance().levelRenderer.getChunkStatistics();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_entity_statistics")
    public static String getEntityStatistics() {
        return Minecraft.getInstance().levelRenderer.getEntityStatistics();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_sound_statistics")
    public static String getSoundStatistics() {
        return Minecraft.getInstance().getSoundManager().getDebugString();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_entity_count")
    public static int getEntityCount() {
        if (Minecraft.getInstance().level == null)
            return 0;

        return Minecraft.getInstance().level.getEntityCount();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_particle_count")
    public static String getParticleCount() {
        return Minecraft.getInstance().particleEngine.countParticles();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_current_effect")
    public static String getCurrentEffect() {
        if (Minecraft.getInstance().gameRenderer.currentEffect() == null)
            return null;

        return Minecraft.getInstance().gameRenderer.currentEffect().getName();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_java_version")
    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_used_memory")
    public static long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_max_memory")
    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_allocated_memory")
    public static long getAllocatedMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.is_window_focused")
    public static boolean isWindowFocused() {
        return Minecraft.getInstance().isWindowActive();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.is_hud_enabled")
    public static boolean isHudEnabled() {
        return Minecraft.renderNames();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.is_debug_overlay_enabled")
    public static boolean isDebugOverlayEnabled() {
        return Minecraft.getInstance().options.renderDebug;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_window_size")
    public static FiguraVec2 getWindowSize() {
        Window window = Minecraft.getInstance().getWindow();
        return FiguraVec2.of(window.getWidth(), window.getHeight());
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_fov")
    public static double getFOV() {
        return Minecraft.getInstance().options.fov().get();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_system_time")
    public static long getSystemTime() {
        return System.currentTimeMillis();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_mouse_pos")
    public static FiguraVec2 getMousePos() {
        MouseHandler mouse = Minecraft.getInstance().mouseHandler;
        return FiguraVec2.of(mouse.xpos(), mouse.ypos());
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_scaled_window_size")
    public static FiguraVec2 getScaledWindowSize() {
        Window window = Minecraft.getInstance().getWindow();
        return FiguraVec2.of(window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_gui_scale")
    public static double getGuiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_camera_pos")
    public static FiguraVec3 getCameraPos() {
        Vec3 pos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return FiguraVec3.fromVec3(pos);
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_camera_rot")
    public static FiguraVec3 getCameraRot() {
        var quaternion = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        Vector3f vec = new Vector3f();
        quaternion.getEulerAnglesYXZ(vec);
        double f = 180d / Math.PI;
        return FiguraVec3.fromVec3f(vec).multiply(f, -f, f); // degrees, and negate y
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_camera_dir")
    public static FiguraVec3 getCameraDir() {
        return FiguraVec3.fromVec3f(Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            value = "client.get_text_width"
    )
    public static int getTextWidth(@LuaNotNil String text) {
        return TextUtils.getWidth(TextUtils.splitText(TextUtils.tryParseJson(text), "\n"), Minecraft.getInstance().font);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            value = "client.get_text_height"
    )
    public static int getTextHeight(String text) {
        return TextUtils.getHeight(TextUtils.splitText(TextUtils.tryParseJson(text), "\n"), Minecraft.getInstance().font);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "text"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Integer.class, Boolean.class},
                            argumentNames = {"text", "maxWidth", "wrap"}
                    )
            },
            value = "client.get_text_dimensions"
    )
    public static FiguraVec2 getTextDimensions(@LuaNotNil String text, int maxWidth, Boolean wrap) {
        Component component = TextUtils.tryParseJson(text);
        Font font = Minecraft.getInstance().font;
        List<Component> list = TextUtils.formatInBounds(component, font, maxWidth, wrap == null || wrap);
        int x = TextUtils.getWidth(list, font);
        int y = TextUtils.getHeight(list, font);
        return FiguraVec2.of(x, y);
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_active_lang")
    public static String getActiveLang() {
        return Minecraft.getInstance().options.languageCode;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "modID"
            ),
            value = "client.is_mod_loaded"
    )
    public static boolean isModLoaded(String id) {
        if (Objects.equals(id, "optifine") || Objects.equals(id, "optifabric"))
            return OPTIFINE_LOADED.get();

        LOADED_MODS.putIfAbsent(id, PlatformUtils.isModLoaded(id));
        return LOADED_MODS.get(id);
    }

    @LuaWhitelist
    @LuaMethodDoc("client.has_shader_pack_mod")
    public static boolean hasShaderPackMod() {
        return HAS_IRIS || OPTIFINE_LOADED.get();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.has_shader_pack")
    public static boolean hasShaderPack() {
        return HAS_IRIS && net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse() || OPTIFINE_LOADED.get() && hasOptifineShader();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_shader_pack_name")
    public static String getShaderPackName() {
        try {
            if (HAS_IRIS) {
                return net.irisshaders.iris.Iris.getCurrentPackName();
            } else if (OPTIFINE_LOADED.get()) {
                Field shaderNameField = Class.forName("net.optifine.shaders.Shaders").getField("currentShaderName");
                Class<?> shaderClass = shaderNameField.getType();
                if (shaderClass == String.class)
                    return (String) shaderNameField.get(null);
            }
        }catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {
        }
        return "";
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "path"
            ),
            value = "client.has_resource"
    )
    public static boolean hasResource(@LuaNotNil String path) {
        ResourceLocation resource = LuaUtils.parsePath(path);
        try {
            return Minecraft.getInstance().getResourceManager().getResource(resource).isPresent();
        } catch (Exception ignored) {
            return false;
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_active_resource_packs")
    public static List<String> getActiveResourcePacks() {
        List<String> list = new ArrayList<>();

        for (Pack pack : Minecraft.getInstance().getResourcePackRepository().getSelectedPacks())
            list.add(pack.getTitle().getString());

        return list;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_figura_version")
    public static String getFiguraVersion() {
        return FiguraMod.VERSION.toString();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {String.class, String.class},
                    argumentNames = {"version1", "version2"}
            ),
            value = "client.compare_versions")
    public static int compareVersions(@LuaNotNil String ver1, @LuaNotNil String ver2) {
        Version v1 = new Version(ver1);
        Version v2 = new Version(ver2);

        if (v1.invalid)
            throw new LuaError("Cannot parse version " + "\"" + ver1 + "\"");
        if (v2.invalid)
            throw new LuaError("Cannot parse version " + "\"" + ver2 + "\"");

        return v1.compareTo(v2);
    }

    @LuaWhitelist
    @LuaMethodDoc("client.generate_uuid")
    public static int[] generateUUID(){
        return UUIDUtil.uuidToIntArray(UUID.randomUUID());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {Integer.class, Integer.class, Integer.class, Integer.class},
                    argumentNames = {"a", "b", "c", "d"}
            ),
            value = "client.int_uuid_to_string")
    public static String intUUIDToString(int a, int b, int c, int d) {
        try {
            UUID uuid = UUIDUtil.uuidFromIntArray(new int[]{a, b, c, d});
            return uuid.toString();
        } catch (Exception ignored) {
            throw new LuaError("Failed to parse uuid");
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "uuid"
            ),
            value = "client.uuid_to_int_array")
    public static int[] uuidToIntArray(String uuid) {
        try {
            UUID id = UUID.fromString(uuid);
            return UUIDUtil.uuidToIntArray(id);
        } catch (Exception ignored) {
            throw new LuaError("Failed to parse uuid");
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_viewer")
    public static ViewerAPI getViewer() {
        return new ViewerAPI(Minecraft.getInstance().player);
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_camera_entity")
    public static EntityAPI<?> getCameraEntity() {
        return EntityAPI.wrap(Minecraft.getInstance().getCameraEntity());
    }

    @LuaWhitelist
    @LuaMethodDoc("client.is_integrated_server")
    public static Boolean isIntegratedServer() {
    	return Minecraft.getInstance().getSingleplayerServer() != null;
    }
    @LuaWhitelist
    @LuaMethodDoc("client.get_server_data")
    public static Map<String, String> getServerData() {
        Map<String, String> map = new HashMap<>();

        IntegratedServer iServer = Minecraft.getInstance().getSingleplayerServer();
        if (iServer != null) {
            map.put("name", iServer.getWorldData().getLevelName());
            map.put("ip", iServer.getLocalIp());
            map.put("motd", iServer.getMotd());
            return map;
        }

        ServerData mServer = Minecraft.getInstance().getCurrentServer();
        if (mServer != null) {
            if (mServer.name != null)
                map.put("name", mServer.name);
            if (mServer.ip != null)
                map.put("ip", mServer.ip);
            if (mServer.motd != null)
                map.put("motd", mServer.motd.getString());
        }

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_date")
    public static Map<String, Object> getDate() {
        Map<String, Object> map = new HashMap<>();

        Calendar calendar = FiguraMod.CALENDAR;
        Date date = new Date();
        calendar.setTime(date);

        map.put("day", calendar.get(Calendar.DAY_OF_MONTH));
        map.put("month", calendar.get(Calendar.MONTH) + 1);
        map.put("year", calendar.get(Calendar.YEAR));
        map.put("hour", calendar.get(Calendar.HOUR_OF_DAY));
        map.put("minute", calendar.get(Calendar.MINUTE));
        map.put("second", calendar.get(Calendar.SECOND));
        map.put("millisecond", calendar.get(Calendar.MILLISECOND));
        map.put("week", calendar.get(Calendar.WEEK_OF_YEAR));
        map.put("year_day", calendar.get(Calendar.DAY_OF_YEAR));
        map.put("week_day", calendar.get(Calendar.DAY_OF_WEEK));
        map.put("daylight_saving", calendar.getTimeZone().inDaylightTime(date));

        SimpleDateFormat format = new SimpleDateFormat("Z|zzzz|G|MMMM|EEEE", Locale.US);
        String[] f = format.format(date).split("\\|");

        map.put("timezone", f[0]);
        map.put("timezone_name", f[1]);
        map.put("era", f[2]);
        map.put("month_name", f[3]);
        map.put("day_name", f[4]);

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_frame_time")
    public static double getFrameTime() {
        return Minecraft.getInstance().getFrameTime();
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_actionbar")
    public static Component getActionbar() {
        Gui gui = Minecraft.getInstance().gui;
        return ((GuiAccessor) gui).getActionbarTime() > 0 ? ((GuiAccessor) gui).getActionbar() : null;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_title")
    public static Component getTitle() {
        Gui gui = Minecraft.getInstance().gui;
        return ((GuiAccessor) gui).getTime() > 0 ? ((GuiAccessor) gui).getTitle() : null;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_subtitle")
    public static Component getSubtitle() {
        Gui gui = Minecraft.getInstance().gui;
        return ((GuiAccessor) gui).getTime() > 0 ? ((GuiAccessor) gui).getSubtitle() : null;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_scoreboard")
    public static Map<String, Map<String, Object>> getScoreboard() {
        Map<String, Map<String, Object>> map = new HashMap<>();

        assert Minecraft.getInstance().level != null;
        Scoreboard scoreboard = Minecraft.getInstance().level.getScoreboard();

        Map<String, Objective> objectives = new HashMap<>();

        // sidebars for different team colours
        assert Minecraft.getInstance().player != null;
        PlayerTeam playerTeam = scoreboard.getPlayersTeam(Minecraft.getInstance().player.getScoreboardName());
        if (playerTeam != null) {
            int id = playerTeam.getColor().getId();
            if (id >= 0) {
                objectives.put("sidebar_team_" + playerTeam.getColor().getName(), scoreboard.getDisplayObjective(3 + id));
            }
        }

        objectives.put("list", scoreboard.getDisplayObjective(0));
        objectives.put("sidebar", scoreboard.getDisplayObjective(1));
        objectives.put("below_name", scoreboard.getDisplayObjective(2));

        for (Map.Entry<String, Objective> entry : objectives.entrySet()) {
            String key = entry.getKey();
            Objective objective = entry.getValue();

            if (objective != null) {
                Map<String, Object> objectiveMap = new HashMap<>();

                objectiveMap.put("name", objective.getName());
                objectiveMap.put("display_name", objective.getFormattedDisplayName());
                objectiveMap.put("criteria", objective.getCriteria().getName());
                objectiveMap.put("render_type", objective.getRenderType().getSerializedName());

                Map<String, Integer> scoreMap = new HashMap<>();
                for (Score score : scoreboard.getPlayerScores(objective)) {
                    scoreMap.put(score.getOwner(), score.getScore());
                }

                objectiveMap.put("scores", scoreMap);

                map.put(key, objectiveMap);
            }
        }

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc("client.list_atlases")
    public static List<String> listAtlases() {
        List<String> list = new ArrayList<>();
        for (ResourceLocation res : ModelManagerAccessor.getVanillaAtlases().keySet())
            list.add(res.toString());
        return list;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "path"
            ),
            value = "client.get_atlas"
    )
    public static TextureAtlasAPI getAtlas(@LuaNotNil String atlas) {
        ResourceLocation path = LuaUtils.parsePath(atlas);
        try {
            return new TextureAtlasAPI(Minecraft.getInstance().getModelManager().getAtlas(path));
        } catch (Exception ignored) {
            return null;
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("client.get_tab_list")
    public static Map<String, Object> getTabList() {
        Map<String, Object> map = new HashMap<>();
        PlayerTabOverlayAccessor accessor = (PlayerTabOverlayAccessor) Minecraft.getInstance().gui.getTabList();

        // header
        Component header = accessor.getHeader();
        if (header != null) {
            map.put("header", header.getString());
            map.put("headerJson", header);
        }

        // players
        List<String> list = new ArrayList<>();
        for (PlayerInfo entry : EntityUtils.getTabList())
            list.add(entry.getTabListDisplayName() != null ? entry.getTabListDisplayName().getString() : entry.getProfile().getName());
        map.put("players", list);

        // footer
        Component footer = accessor.getFooter();
        if (footer != null) {
            map.put("footer", footer.getString());
            map.put("footerJson", footer);
        }

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(argumentTypes = String.class, argumentNames = "text"),
                    @LuaMethodOverload(argumentTypes = {String.class, LuaValue.class}, argumentNames = {"text", "args"})
            },
            value = "client.get_translated_string"
    )
    public static String getTranslatedString(@LuaNotNil String text, LuaValue args) {
        Component component;

        if (args == null) {
            component = Component.translatable(text);
        } else if (!args.istable()) {
            component = Component.translatable(text, args.tojstring());
        } else {
            int len = args.length();
            Object[] arguments = new Object[len];

            for (int i = 0; i < len; i++)
                arguments[i] = args.get(i + 1).tojstring();

            component = Component.translatable(text, arguments);
        }

        return component.getString();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(argumentTypes = String.class, argumentNames = "registryName"),
            },
            value = "client.get_registry"
    )
    public static List<String> getRegistry(@LuaNotNil String registryName) {
        Registry<?> registry = BuiltInRegistries.REGISTRY.get(new ResourceLocation(registryName));

        if (registry != null) {
            return registry.keySet().stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.toList());
        } else {
            throw new LuaError("Registry " + registryName + " does not exist");
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(argumentTypes = String.class, argumentNames = "enumName"),
            },
            value = "client.getEnum"
    )
    public static List<String> getEnum(@LuaNotNil String enumName) {
        try {
            return FiguraListDocs.getEnumValues(enumName);
        } catch (Exception e) {
            throw new LuaError("Enum " + enumName + " does not exist");
        }
    }

    @Override
    public String toString() {
        return "ClientAPI";
    }
}
