package org.figuramc.figura.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.local.CacheAvatarLoader;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.entries.EntryPointManager;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.gui.screens.ConfigScreen;
import org.figuramc.figura.lua.FiguraLuaPrinter;
import org.figuramc.figura.lua.api.ConfigAPI;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.resources.FiguraRuntimeResources;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Configs {

    // mod config version
    // only change this if you rename old configs
    public static final int CONFIG_VERSION = 1;

    // config update hashmap; <version number, <actual config, old config name>>
    public static final HashMap<Integer, HashMap<ConfigType<?>, String>> CONFIG_UPDATES = new HashMap<>();

    // code to run when the config is initialized
    public static void init() {
        // test for unused configs
        if (FiguraMod.debugModeEnabled()) {
            ConfigType.Category debug = new ConfigType.Category("debug");
            new ConfigType.ColorConfig("color_test", debug, ColorUtils.Colors.AWESOME_BLUE.hex);
            new ConfigType.StringConfig("string_test", debug, "text");
            new ConfigType.IntConfig("int_test", debug, 2147483647);
        }
    }


    // -- categories -- // 


    public static final ConfigType.Category
            NAMEPLATE = new ConfigType.Category("nameplate"),
            SCRIPT = new ConfigType.Category("script"),
            RENDERING = new ConfigType.Category("rendering"),
            ACTION_WHEEL = new ConfigType.Category("action_wheel"),
            UI = new ConfigType.Category("ui"),
            PAPERDOLL = new ConfigType.Category("paperdoll"),
            MISC = new ConfigType.Category("misc"),
            DEV = new ConfigType.Category("dev") {{
                this.name = this.name.copy().withStyle(ChatFormatting.RED);
            }},
            NETWORKING = new ConfigType.Category("networking") {{
                this.name = this.name.copy().withStyle(ChatFormatting.RED);
                this.tooltip = this.tooltip.copy().withStyle(ChatFormatting.RED);
            }};


    // -- nameplate -- // 


    public static final ConfigType.BoolConfig
            SELF_NAMEPLATE = new ConfigType.BoolConfig("self_nameplate", NAMEPLATE, false),
            PREVIEW_NAMEPLATE = new ConfigType.BoolConfig("preview_nameplate", NAMEPLATE, false),
            SOUND_BADGE = new ConfigType.BoolConfig("sound_badge", NAMEPLATE, true);
    private static final String NAMEPLATE_PATH = "config.nameplate_level.";
    private static final List<Component> NAMEPLATE_ENUM = List.of(
            FiguraText.of(NAMEPLATE_PATH + "1"),
            FiguraText.of(NAMEPLATE_PATH + "2"),
            FiguraText.of(NAMEPLATE_PATH + "3")
    );
    private static final List<Component> NAMEPLATE_TOOLTIP = List.of(
            FiguraText.of(NAMEPLATE_PATH + "1.tooltip"),
            FiguraText.of(NAMEPLATE_PATH + "2.tooltip"),
            FiguraText.of(NAMEPLATE_PATH + "3.tooltip")
    );
    public static final ConfigType.EnumConfig
            NAMEPLATE_RENDER = new ConfigType.EnumConfig("nameplate_render", NAMEPLATE, 0, 3),
            CHAT_NAMEPLATE = new ConfigType.EnumConfig("chat_nameplate", NAMEPLATE, 2, 3) {{
                this.enumList = NAMEPLATE_ENUM;
                this.enumTooltip = NAMEPLATE_TOOLTIP;
            }},
            ENTITY_NAMEPLATE = new ConfigType.EnumConfig("entity_nameplate", NAMEPLATE, 2, 3) {{
                this.enumList = NAMEPLATE_ENUM;
                this.enumTooltip = NAMEPLATE_TOOLTIP;
            }},
            LIST_NAMEPLATE = new ConfigType.EnumConfig("list_nameplate", NAMEPLATE, 2, 3) {{
                this.enumList = NAMEPLATE_ENUM;
                this.enumTooltip = NAMEPLATE_TOOLTIP;
            }};


    // -- script -- // 


    public static final ConfigType.EnumConfig
            LOG_LOCATION = new ConfigType.EnumConfig("log_location", SCRIPT, 0, 2),
            FORMAT_SCRIPT = new ConfigType.EnumConfig("format_script", SCRIPT, 1, 4) {
                {
                    String tooltip = "config.format_script.tooltip.";
                    this.tooltip = FiguraText.of(tooltip + "1")
                            .append("\n")
                            .append(FiguraText.of(tooltip + "2").withStyle(ChatFormatting.RED));
                }

                @Override
                public void onChange() {
                    if (!AvatarManager.localUploaded)
                        AvatarManager.reloadAvatar(FiguraMod.getLocalPlayerUUID());
                }
            };
    public static final ConfigType.PositiveIntConfig
            LOG_NUMBER_LENGTH = new ConfigType.PositiveIntConfig("log_number_length", SCRIPT, 5) {
        @Override
        public void onChange() {
            super.onChange();
            FiguraLuaPrinter.updateDecimalFormatting();
        }
    };


    // -- RENDERING -- // 


    public static final ConfigType.EnumConfig
            IRIS_COMPATIBILITY_FIX = new ConfigType.EnumConfig("iris_compatibility_fix", RENDERING, 1, 3),
            RENDER_DEBUG_PARTS_PIVOT = new ConfigType.EnumConfig("render_debug_parts_pivot", RENDERING, 1, 3) {{
                String tooltip = "config.render_debug_parts_pivot.tooltip";
                this.tooltip = FiguraText.of(tooltip,
                        FiguraText.of(tooltip + ".cubes").setStyle(ColorUtils.Colors.AWESOME_BLUE.style),
                        FiguraText.of(tooltip + ".groups").setStyle(ColorUtils.Colors.BLUE.style));
            }};
    public static final ConfigType.BoolConfig
            ALLOW_FP_HANDS = new ConfigType.BoolConfig("allow_fp_hands", RENDERING, false),
            FIRST_PERSON_MATRICES = new ConfigType.BoolConfig("first_person_matrices", RENDERING, true),
            INVENTORY_SCISSOR = new ConfigType.BoolConfig("inventory_scissor", RENDERING, true);


    // -- ACTION WHEEL -- // 


    public static final ConfigType.KeybindConfig
            ACTION_WHEEL_BUTTON = new ConfigType.KeybindConfig("action_wheel_button", ACTION_WHEEL, "key.keyboard.b");
    public static final ConfigType.EnumConfig
            ACTION_WHEEL_MODE = new ConfigType.EnumConfig("action_wheel_mode", ACTION_WHEEL, 0, 4);
    public static final ConfigType.PositiveFloatConfig
            ACTION_WHEEL_SCALE = new ConfigType.PositiveFloatConfig("action_wheel_scale", ACTION_WHEEL, 1f);
    public static final ConfigType.EnumConfig
            ACTION_WHEEL_TITLE = new ConfigType.EnumConfig("action_wheel_title", ACTION_WHEEL, 0, 7),
            ACTION_WHEEL_SLOTS_INDICATOR = new ConfigType.EnumConfig("action_wheel_slots_indicator", ACTION_WHEEL, 0, 3);
    public static final ConfigType.BoolConfig
            ACTION_WHEEL_DECORATIONS = new ConfigType.BoolConfig("action_wheel_decorations", ACTION_WHEEL, true);


    // -- UI -- // 


    public static final ConfigType.BoolConfig
            FIGURA_INVENTORY = new ConfigType.BoolConfig("figura_inventory", UI, true),
            PREVIEW_HEAD_ROTATION = new ConfigType.BoolConfig("preview_head_rotation", UI, false),
            AVATAR_PORTRAIT = new ConfigType.BoolConfig("avatar_portrait", UI, true),
            WARDROBE_FILE_NAMES = new ConfigType.BoolConfig("wardrobe_file_names", UI, false);
    public static final ConfigType.FloatConfig
            BACKGROUND_SCROLL_SPEED = new ConfigType.FloatConfig("background_scroll_speed", UI, 1f);
    public static final ConfigType.PositiveFloatConfig
            POPUP_SCALE = new ConfigType.PositiveFloatConfig("popup_scale", UI, 1f),
            POPUP_MIN_SIZE = new ConfigType.PositiveFloatConfig("popup_min_size", UI, 1f),
            POPUP_MAX_SIZE = new ConfigType.PositiveFloatConfig("popup_max_size", UI, 6f),
            TOAST_TIME = new ConfigType.PositiveFloatConfig("toast_time", UI, 5f),
            TOAST_TITLE_TIME = new ConfigType.PositiveFloatConfig("toast_title_time", UI, 2f),
            TEXT_SCROLL_SPEED = new ConfigType.PositiveFloatConfig("text_scroll_speed", UI, 1f);
    public static final ConfigType.PositiveIntConfig
            TEXT_SCROLL_DELAY = new ConfigType.PositiveIntConfig("text_scroll_delay", UI, 20);
    public static final ConfigType.BoolConfig
            REDUCED_MOTION = new ConfigType.BoolConfig("reduced_motion", UI, false);


    // -- PAPERDOLL -- // 


    public static final ConfigType.BoolConfig
            HAS_PAPERDOLL = new ConfigType.BoolConfig("has_paperdoll", PAPERDOLL, false),
            PAPERDOLL_ALWAYS_ON = new ConfigType.BoolConfig("paperdoll_always_on", PAPERDOLL, false),
            FIRST_PERSON_PAPERDOLL = new ConfigType.BoolConfig("first_person_paperdoll", PAPERDOLL, true),
            PAPERDOLL_INVISIBLE = new ConfigType.BoolConfig("paperdoll_invisible", PAPERDOLL, false);
    public static final ConfigType.FloatConfig
            PAPERDOLL_SCALE = new ConfigType.FloatConfig("paperdoll_scale", PAPERDOLL, 1f),
            PAPERDOLL_X = new ConfigType.FloatConfig("paperdoll_x", PAPERDOLL, 0f),
            PAPERDOLL_Y = new ConfigType.FloatConfig("paperdoll_y", PAPERDOLL, 0f),
            PAPERDOLL_PITCH = new ConfigType.FloatConfig("paperdoll_pitch", PAPERDOLL, 0f),
            PAPERDOLL_YAW = new ConfigType.FloatConfig("paperdoll_yaw", PAPERDOLL, 20);


    // -- MISC -- // 


    public static final ConfigType.KeybindConfig
            POPUP_BUTTON = new ConfigType.KeybindConfig("popup_button", MISC, "key.keyboard.r"),
            RELOAD_BUTTON = new ConfigType.KeybindConfig("reload_button", MISC, "key.keyboard.unknown"),
            PANIC_BUTTON = new ConfigType.KeybindConfig("panic_button", MISC, "key.keyboard.unknown"),
            WARDROBE_BUTTON = new ConfigType.KeybindConfig("wardrobe_button", MISC, "key.keyboard.unknown");
    public static final ConfigType.EnumConfig
            BUTTON_LOCATION = new ConfigType.EnumConfig("button_location", MISC, 0, 5),
            UPDATE_CHANNEL = new ConfigType.EnumConfig("update_channel", MISC, 1, 3) {
                @Override
                public void onChange() {
                    super.onChange();
                    NetworkStuff.checkVersion();
                }
            },
            DEFAULT_PERMISSION_LEVEL = new ConfigType.EnumConfig("default_permission_level", MISC, 2, Permissions.Category.values().length) {
                {
                    List<Component> list = new ArrayList<>();
                    Permissions.Category[] categories = Permissions.Category.values();
                    for (Permissions.Category category : categories)
                        list.add(category.text.copy());
                    this.enumList = list;
                    this.enumTooltip = null;
                }

                @Override
                public void onChange() {
                    super.onChange();
                    PermissionManager.saveToDisk();
                }
            },
            EMOJIS = new ConfigType.EnumConfig("emojis", MISC, 1, 3);
    public static final ConfigType.BoolConfig
            EASTER_EGGS = new ConfigType.BoolConfig("easter_eggs", MISC, true);


    // -- DEV -- //
    public static final ConfigType.BoolConfig
            DEBUG_MODE = new ConfigType.BoolConfig("debug_mode", DEV, false, false);
    public static final ConfigType.BoolConfig
            LOCAL_ASSETS = new ConfigType.BoolConfig("local_assets", DEV, false, false);

    public static final ConfigType.BoolConfig
            CONNECTION_TOASTS = new ConfigType.BoolConfig("connection_toasts", DEV, true),
            LOG_OTHERS = new ConfigType.BoolConfig("log_others", DEV, false);
    public static final ConfigType.EnumConfig
            LOG_PINGS = new ConfigType.EnumConfig("log_pings", DEV, 0, 3);
    public static final ConfigType.BoolConfig
            SYNC_PINGS = new ConfigType.BoolConfig("sync_pings", DEV, false) {{
        String tooltip = "config.sync_pings.tooltip.";
        this.tooltip = FiguraText.of(tooltip + "1")
                .append("\n")
                .append(FiguraText.of(tooltip + "2").withStyle(ChatFormatting.RED));
    }},
            CHAT_MESSAGES = new ConfigType.BoolConfig("chat_messages", DEV, false) {{
                this.name = this.name.copy().withStyle(ChatFormatting.RED);
                String tooltip = "config.chat_messages.tooltip.";
                this.tooltip = FiguraText.of(tooltip + "1")
                        .append("\n\n")
                        .append(FiguraText.of(tooltip + "2").withStyle(ChatFormatting.RED))
                        .append("\n\n")
                        .append(FiguraText.of(tooltip + "3").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }};
    public static final ConfigType.FolderConfig
            MAIN_DIR = new ConfigType.FolderConfig("main_dir", DEV, "") {
        @Override
        public void onChange() {
            super.onChange();
            PermissionManager.reinit();
            LocalAvatarFetcher.reinit();
            EntryPointManager.reinit();
        }
    };
    public static final ConfigType.IPConfig
            SERVER_IP = new ConfigType.IPConfig("server_ip", DEV, "figura.moonlight-devs.org") {
        @Override
        public void onChange() {
            super.onChange();
            NetworkStuff.reAuth();
        }
    };
    @SuppressWarnings("unused")
    public static final ConfigType.ButtonConfig
            CLEAR_CACHE = new ConfigType.ButtonConfig("clear_cache", DEV, () -> {
        CacheAvatarLoader.clearCache();
        LocalAvatarFetcher.clearCache();
        ConfigScreen.clearCache();
        FiguraRuntimeResources.clearCache();
        FiguraToast.sendToast(FiguraText.of("toast.cache_clear"));
    }),
            REDOWNLOAD_ASSETS = new ConfigType.ButtonConfig("redownload_assets", DEV, () -> {
                FiguraRuntimeResources.init();
                Minecraft.getInstance().reloadResourcePacks();
            }),
            CLEAR_AVATAR_DATA = new ConfigType.ButtonConfig("clear_avatar_data", DEV, () -> {
                ConfigAPI.clearAllData();
                FiguraToast.sendToast(FiguraText.of("toast.avatar_data_clear"));
            });
    public static final ConfigType.BoolConfig
            FORCE_SMOOTH_AVATAR = new ConfigType.BoolConfig("force_smooth_avatar", DEV, false),
            GUI_FPS = new ConfigType.BoolConfig("gui_fps", DEV, false);

    // -- NETWORKING -- //
    public static final ConfigType.BoolConfig ALLOW_NETWORKING =
            new ConfigType.BoolConfig("allow_networking", NETWORKING, false);
    public static final ConfigType.EnumConfig NETWORKING_RESTRICTION = new ConfigType.EnumConfig("networking_restriction", NETWORKING, 0, 3);

    public static final ConfigType.NetworkFilterConfig NETWORK_FILTER = new ConfigType.NetworkFilterConfig("network_filter", NETWORKING);

    public static final ConfigType.EnumConfig LOG_NETWORKING = new ConfigType.EnumConfig("networking_logging", NETWORKING, 0, 4);
}
