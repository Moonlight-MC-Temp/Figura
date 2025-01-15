package org.figuramc.figura.lua.docs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;

import net.minecraft.world.level.levelgen.Heightmap;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.animation.Animation;
import org.figuramc.figura.mixin.input.KeyMappingAccessor;
import org.figuramc.figura.mixin.render.GameRendererAccessor;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;
import org.figuramc.figura.model.rendering.texture.FiguraTextureSet;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraClientCommandSource;
import org.figuramc.figura.utils.FiguraText;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

/**
 * Adds docs for functions that have specific set of String names.
 */
public class FiguraListDocs {

    // -- types --// 

    public static final LinkedHashSet<String> KEYBINDS = new LinkedHashSet<>();
    private static final LinkedHashMap<String, List<String>> PARENT_TYPES = new LinkedHashMap<>() {{
        for (ParentType value : ParentType.values())
            put(value.name(), Arrays.asList(value.aliases));
    }};
    private static final LinkedHashMap<String, List<String>> STRING_ENCODINGS = new LinkedHashMap<>() {{
        put("utf8", List.of("utf_8"));
        put("utf16", List.of("utf_16"));
        put("utf16be", List.of("utf_16_be"));
        put("utf16le", List.of("utf_16_le"));
        put("ascii", List.of());
        put("iso88591", List.of("iso_8859_1"));
    }};
    private static final LinkedHashSet<String> RENDER_TYPES = new LinkedHashSet<>() {{
        for (RenderTypes value : RenderTypes.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> TEXTURE_TYPES = new LinkedHashSet<>() {{
        for (FiguraTextureSet.OverrideType value : FiguraTextureSet.OverrideType.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> ENTITY_POSES = new LinkedHashSet<>() {{
        for (Pose value : Pose.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> ITEM_DISPLAY_MODES = new LinkedHashSet<>() {{
        for (ItemDisplayContext value : ItemDisplayContext.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> POST_EFFECTS = new LinkedHashSet<>() {{
        for (ResourceLocation effect : GameRendererAccessor.getEffects()) {
            String[] split = effect.getPath().split("/");
            String name = split[split.length - 1];
            add(name.split("\\.")[0]);
        }
    }};
    private static final LinkedHashSet<String> PLAY_STATES = new LinkedHashSet<>() {{
        for (Animation.PlayState value : Animation.PlayState.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> LOOP_MODES = new LinkedHashSet<>() {{
        for (Animation.LoopMode value : Animation.LoopMode.values())
            add(value.name());
    }};
    private static final LinkedHashMap<String, List<String>> COLORS = new LinkedHashMap<>() {{
        for (ColorUtils.Colors value : ColorUtils.Colors.values())
            put(value.name(), List.of(value.name()));
    }};
    private static final LinkedHashSet<String> PLAYER_MODEL_PARTS = new LinkedHashSet<>() {{
        for (PlayerModelPart value : PlayerModelPart.values()) {
            String name = value.name();
            add(name.endsWith("_LEG") ? name.substring(0, name.length() - 4) : name);
        }
    }};
    private static final LinkedHashSet<String> USE_ACTIONS = new LinkedHashSet<>() {{
        for (UseAnim value : UseAnim.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> RENDER_MODES = new LinkedHashSet<>() {{
        for (EntityRenderMode value : EntityRenderMode.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> BLOCK_RAYCAST_TYPE = new LinkedHashSet<>() {{
        for (ClipContext.Block value : ClipContext.Block.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> FLUID_RAYCAST_TYPE = new LinkedHashSet<>() {{
        for (ClipContext.Fluid value : ClipContext.Fluid.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> HEIGHTMAP_TYPE = new LinkedHashSet<>() {{
        for (Heightmap.Types value : Heightmap.Types.values())
            add(value.name());
    }};
    private static final LinkedHashSet<String> REGISTRIES = new LinkedHashSet<>() {{
        for (ResourceLocation resourceLocation : BuiltInRegistries.REGISTRY.keySet())
            add(resourceLocation.getPath());
    }};
    private static final LinkedHashSet<String> WRITE_OVERFLOW_STRETEGIES = new LinkedHashSet<String>() {{
        for (FiguraTexture.WriteOverflowStrategy strategy : FiguraTexture.WriteOverflowStrategy.values())
            add(strategy.primaryName);
    }};
    private static final LinkedHashSet<String> BLEND_MODES = new LinkedHashSet<String>() {{
        for (FiguraTexture.BlendMode mode : FiguraTexture.BlendMode.values())
            add(mode.name);
    }};

    private enum ListDoc {
        KEYBINDS(() -> FiguraListDocs.KEYBINDS, "Keybinds", "keybinds", 2),
        PARENT_TYPES(() -> FiguraListDocs.PARENT_TYPES, "ParentTypes", "parent_types", 1),
        RENDER_TYPES(() -> FiguraListDocs.RENDER_TYPES, "RenderTypes", "render_types", 1),
        TEXTURE_TYPES(() -> FiguraListDocs.TEXTURE_TYPES, "TextureTypes", "texture_types", 1),
        KEY_IDS(() -> new LinkedHashSet<>() {{this.addAll(KeyMappingAccessor.getAll().keySet());}}, "KeyIDs", "key_ids", 2),
        ENTITY_POSES(() -> FiguraListDocs.ENTITY_POSES, "EntityPoses", "entity_poses", 2),
        ITEM_RENDER_TYPES(() -> FiguraListDocs.ITEM_DISPLAY_MODES, "ItemDisplayModes", "item_display_modes", 1),
        POST_EFFECTS(() -> FiguraListDocs.POST_EFFECTS, "PostEffects", "post_effects", 2),
        PLAY_STATES(() -> FiguraListDocs.PLAY_STATES, "PlayStates", "play_states", 1),
        LOOP_MODES(() -> FiguraListDocs.LOOP_MODES, "LoopModes", "loop_modes", 1),
        COLORS(() -> FiguraListDocs.COLORS, "Colors", "colors", 1),
        PLAYER_MODEL_PARTS(() -> FiguraListDocs.PLAYER_MODEL_PARTS, "PlayerModelParts", "player_model_parts", 1),
        USE_ACTIONS(() -> FiguraListDocs.USE_ACTIONS, "UseActions", "use_actions", 1),
        RENDER_MODES(() -> FiguraListDocs.RENDER_MODES, "RenderModes", "render_modes", 1),
        STRING_ENCODINGS(() -> FiguraListDocs.STRING_ENCODINGS, "StringEncodings", "string_encodings", 1),
        BLOCK_RAYCAST_TYPE(() -> FiguraListDocs.BLOCK_RAYCAST_TYPE, "BlockRaycastTypes", "block_raycast_types", 1),
        FLUID_RAYCAST_TYPE(() -> FiguraListDocs.FLUID_RAYCAST_TYPE, "FluidRaycastTypes", "fluid_raycast_types", 1),
        HEIGHTMAP_TYPE(() -> FiguraListDocs.HEIGHTMAP_TYPE, "HeightmapTypes", "heightmap_types", 1),
        REGISTRIES(() -> FiguraListDocs.REGISTRIES, "Registries", "registries", 1),
        WRITE_OVERFLOW_STRATEGIES(() -> FiguraListDocs.WRITE_OVERFLOW_STRETEGIES, "WriteOverflowStrategies", "write_overflow_strategies", 1),
        BLEND_MODES(() -> FiguraListDocs.BLEND_MODES, "BlendModes", "blend_modes", 1);

        private final Supplier<Object> supplier;
        private final String name, id;
        private final int split;

        ListDoc(Supplier<Object> supplier, String name, String id, int split) {
            this.supplier = supplier;
            this.name = name;
            this.id = id;
            this.split = split;
        }

        private Collection<?> get() {
            Object obj = supplier.get();
            if (obj instanceof LinkedHashSet<?> set)
                return set;
            else if (obj instanceof Map<?, ?> map)
                return map.entrySet();
            else
                throw new UnsupportedOperationException("Invalid object " + obj);
        }

        private JsonElement generateJson(boolean translate) {
            JsonObject object = new JsonObject();

            // list properties
            object.addProperty("name", name);
            object.addProperty("description", translate ? Language.getInstance().getOrDefault(FiguraText.of("docs.enum." + id).getString()) : FiguraMod.MOD_ID + "." + "docs.enum." + id);

            // list entries
            Collection<?> coll = get();
            if (coll.size() == 0)
                return object;

            JsonArray entries = new JsonArray();
            for (Object o : coll) {
                if (o instanceof Map.Entry e) {
                    entries.add(e.getKey().toString());
                    for (String s : (List<String>) e.getValue())
                        entries.add(s);
                } else {
                    entries.add(o.toString());
                }
            }

            object.add("entries", entries);
            return object;
        }

        private LiteralArgumentBuilder<FiguraClientCommandSource> generateCommand() {
            // command
            LiteralArgumentBuilder<FiguraClientCommandSource> command = LiteralArgumentBuilder.literal(id);

            // display everything
            command.executes(context -> {
                Collection<?> coll = get();
                if (coll.size() == 0) {
                    FiguraMod.sendChatMessage(FiguraText.of("docs.enum.empty"));
                    return 0;
                }

                MutableComponent text = FiguraDoc.HEADER.copy()
                        .append("\n\n")
                        .append(Component.literal("• ")
                                .append(FiguraText.of("docs.text.description"))
                                .append(":")
                                .withStyle(ColorUtils.Colors.PURPLE.style))
                        .append("\n\t")
                        .append(Component.literal("• ")
                                .append(FiguraText.of("docs.enum." + id))
                                .withStyle(ColorUtils.Colors.FIGURA_BLUE.style))
                        .append("\n\n")
                        .append(Component.literal("• ")
                                .append(FiguraText.of("docs.text.entries"))
                                .append(":")
                                .withStyle(ColorUtils.Colors.PURPLE.style));

                int i = 0;
                for (Object o : coll) {
                    MutableComponent component;

                    if (o instanceof Map.Entry e) {
                        component = Component.literal(e.getKey().toString()).withStyle(ChatFormatting.WHITE);
                        for (String s : (List<String>) e.getValue()) {
                            component.append(Component.literal(" | ").withStyle(ChatFormatting.YELLOW))
                                    .append(Component.literal(s).withStyle(ChatFormatting.GRAY));
                        }
                    } else {
                        component = Component.literal(o.toString()).withStyle(ChatFormatting.WHITE);
                    }

                    text.append(i % split == 0 ? "\n\t" : "\t");
                    text.append(Component.literal("• ").withStyle(ChatFormatting.YELLOW)).append(component);
                    i++;
                }

                FiguraMod.sendChatMessage(text);
                return 1;
            });

            // add collection as child for easy navigation
            Collection<?> coll = get();
            for (Object o : coll) {
                String text = o instanceof Map.Entry e ? e.getKey().toString() : o.toString();
                LiteralArgumentBuilder<FiguraClientCommandSource> entry = LiteralArgumentBuilder.literal(text);
                entry.executes(context -> {
                    FiguraMod.sendChatMessage(Component.literal(text).withStyle(ColorUtils.Colors.AWESOME_BLUE.style));
                    return 1;
                });

                if (o instanceof Map.Entry e) {
                    for (String s : (List<String>) e.getValue()) {
                        LiteralArgumentBuilder<FiguraClientCommandSource> child = LiteralArgumentBuilder.literal(s);
                        child.executes(context -> {
                            FiguraMod.sendChatMessage(Component.literal(s).withStyle(ColorUtils.Colors.AWESOME_BLUE.style));
                            return 1;
                        });
                        entry.then(child);
                    }
                }

                command.then(entry);
            }

            // return
            return command;
        }
    }

    // -- doc methods -- // 

    public static LiteralArgumentBuilder<FiguraClientCommandSource> getCommand() {
        // self
        LiteralArgumentBuilder<FiguraClientCommandSource> root = LiteralArgumentBuilder.literal("enums");
        root.executes(context -> {
            FiguraMod.sendChatMessage(FiguraDoc.HEADER.copy()
                    .append("\n\n")
                    .append(Component.literal("• ")
                            .append(FiguraText.of("docs.text.type"))
                            .append(":")
                            .withStyle(ColorUtils.Colors.PURPLE.style))
                    .append("\n\t")
                    .append(Component.literal("• ")
                            .append(Component.literal("enumerators"))
                            .withStyle(ColorUtils.Colors.FIGURA_BLUE.style))

                    .append("\n\n")
                    .append(Component.literal("• ")
                            .append(FiguraText.of("docs.text.description"))
                            .append(":")
                            .withStyle(ColorUtils.Colors.PURPLE.style))
                    .append("\n\t")
                    .append(Component.literal("• ")
                            .append(FiguraText.of("docs.enum"))
                            .withStyle(ColorUtils.Colors.FIGURA_BLUE.style))
            );
            return 1;
        });

        for (ListDoc value : ListDoc.values())
            root.then(value.generateCommand());

        return root;
    }

    public static List<String> getEnumValues(String enumName) {
        try {
            ListDoc enumListDoc = ListDoc.valueOf(enumName.toUpperCase(Locale.US));

            Collection<?> enumValues = enumListDoc.get();
            List<String> enumValueList = new ArrayList<>();
            for (Object value : enumValues) {
                if (value instanceof Map.Entry<?, ?> entry) {
                    enumValueList.add(entry.getKey().toString());
                    if (entry.getValue() instanceof Collection<?>) {
                        for (Object alias : (Collection<?>) entry.getValue()) {
                            enumValueList.add(alias.toString());
                        }
                    }
                } else {
                    enumValueList.add(value.toString());
                }
            }

            return enumValueList;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Enum " + enumName + " does not exist");
        }
    }

    public static JsonElement toJson(boolean translate) {
        JsonArray array = new JsonArray();
        for (ListDoc value : ListDoc.values())
            array.add(value.generateJson(translate));
        return array;
    }
}
