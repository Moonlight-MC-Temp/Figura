package org.figuramc.figura.model.rendering.texture;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

import java.util.OptionalDouble;
import java.util.function.BiFunction;
import java.util.function.Function;

public enum RenderTypes {
    NONE(null),

    CUTOUT(RenderType::entityCutoutNoCull),
    CUTOUT_CULL(RenderType::entityCutout),
    CUTOUT_EMISSIVE_SOLID(resourceLocation -> FiguraRenderType.CUTOUT_EMISSIVE_SOLID.apply(resourceLocation, true)),

    TRANSLUCENT(RenderType::entityTranslucent),
    TRANSLUCENT_CULL(RenderType::itemEntityTranslucentCull),

    EMISSIVE(RenderType::eyes),
    EMISSIVE_SOLID(resourceLocation -> RenderType.beaconBeam(resourceLocation, false)),
    EYES(RenderType::eyes),

    END_PORTAL(t -> RenderType.endPortal(), false),
    END_GATEWAY(t -> RenderType.endGateway(), false),
    TEXTURED_PORTAL(FiguraRenderType.TEXTURED_PORTAL),

    GLINT(t -> RenderType.entityGlint(), false, false),
    GLINT2(t -> RenderType.glint(), false, false),
    TEXTURED_GLINT(FiguraRenderType.TEXTURED_GLINT, true, false),

    LINES(t -> RenderType.lines(), false),
    LINES_STRIP(t -> RenderType.lineStrip(), false),
    SOLID(t -> FiguraRenderType.SOLID, false),

    BLURRY(FiguraRenderType.BLURRY);

    private final Function<ResourceLocation, RenderType> func;
    private final boolean texture, offset;

    RenderTypes(Function<ResourceLocation, RenderType> func) {
        this(func, true);
    }

    RenderTypes(Function<ResourceLocation, RenderType> func, boolean texture) {
        this(func, texture, true);
    }

    RenderTypes(Function<ResourceLocation, RenderType> func, boolean texture, boolean offset) {
        this.func = func;
        this.texture = texture;
        this.offset = offset;
    }

    public boolean isOffset() {
        return offset;
    }

    public RenderType get(ResourceLocation id) {
        if (!texture)
            return func.apply(id);

        return id == null || func == null ? null : func.apply(id);
    }

    private static class FiguraRenderType extends RenderType {

        public FiguraRenderType(String name, VertexFormat vertexFormat, VertexFormat.Mode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
            super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
        }

        public static final RenderType SOLID = create(
                "figura_solid",
                DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.QUADS,
                256,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.empty()))
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .setCullState(NO_CULL)
                        .createCompositeState(false)
        );

        private static final BiFunction<ResourceLocation, Boolean, RenderType> CUTOUT_EMISSIVE_SOLID = Util.memoize(
                (texture, affectsOutline) ->
                        create("figura_cutout_emissive_solid", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 256, true, true,
                                CompositeState.builder()
                                        .setShaderState(RenderStateShard.RENDERTYPE_BEACON_BEAM_SHADER)
                                        .setTextureState(new RenderStateShard.TextureStateShard(texture, TriState.FALSE, false))
                                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                        .setCullState(NO_CULL)
                                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                                        .setOverlayState(OVERLAY)
                                        .createCompositeState(affectsOutline)));


        public static final Function<ResourceLocation, RenderType> TEXTURED_PORTAL = Util.memoize(
                texture -> create(
                        "figura_textured_portal",
                        DefaultVertexFormat.POSITION,
                        VertexFormat.Mode.QUADS,
                        256,
                        false,
                        false,
                        CompositeState.builder()
                                .setShaderState(RENDERTYPE_END_GATEWAY_SHADER)
                                .setTextureState(
                                        MultiTextureStateShard.builder()
                                                .add(texture, false, false)
                                                .add(texture, false, false)
                                                .build()
                                )
                                .createCompositeState(false)
                )
        );

        public static final Function<ResourceLocation, RenderType> BLURRY = Util.memoize(
                texture -> create(
                        "figura_blurry",
                        DefaultVertexFormat.NEW_ENTITY,
                        VertexFormat.Mode.QUADS,
                        256,
                        true,
                        true,
                        CompositeState.builder()
                                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                                .setTextureState(new TextureStateShard(texture, TriState.TRUE, false))
                                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                .setCullState(NO_CULL)
                                .setLightmapState(LIGHTMAP)
                                .setOverlayState(OVERLAY)
                                .createCompositeState(true)
                )
        );

        public static final Function<ResourceLocation, RenderType> TEXTURED_GLINT = Util.memoize(
                texture -> create(
                        "figura_textured_glint_direct",
                        DefaultVertexFormat.POSITION_TEX,
                        VertexFormat.Mode.QUADS,
                        256,
                        false,
                        false,
                        RenderType.CompositeState.builder()
                                .setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
                                .setTextureState(new TextureStateShard(texture, TriState.FALSE, false))
                                .setWriteMaskState(COLOR_WRITE)
                                .setCullState(NO_CULL)
                                .setDepthTestState(EQUAL_DEPTH_TEST)
                                .setTransparencyState(GLINT_TRANSPARENCY)
                                .setTexturingState(ENTITY_GLINT_TEXTURING)
                                .createCompositeState(false)
                )
        );
    }
}