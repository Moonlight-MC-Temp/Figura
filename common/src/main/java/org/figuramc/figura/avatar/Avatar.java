package org.figuramc.figura.avatar;

import com.mojang.blaze3d.audio.SoundBuffer;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.animation.Animation;
import org.figuramc.figura.animation.AnimationPlayer;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.lua.FiguraLuaPrinter;
import org.figuramc.figura.lua.FiguraLuaRuntime;
import org.figuramc.figura.lua.api.TextureAPI;
import org.figuramc.figura.lua.api.data.FiguraBuffer;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.lua.api.particle.ParticleAPI;
import org.figuramc.figura.lua.api.ping.PingArg;
import org.figuramc.figura.lua.api.ping.PingFunction;
import org.figuramc.figura.lua.api.sound.SoundAPI;
import org.figuramc.figura.lua.api.world.BlockStateAPI;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.math.matrix.FiguraMat3;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.model.PartCustomization;
import org.figuramc.figura.model.rendering.AvatarRenderer;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.figuramc.figura.model.rendering.ImmediateAvatarRenderer;
import org.figuramc.figura.model.rendering.PartFilterScheme;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.permissions.PermissionPack;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.EntityUtils;
import org.figuramc.figura.utils.PathUtils;
import org.figuramc.figura.utils.RefilledNumber;
import org.figuramc.figura.utils.Version;
import org.figuramc.figura.utils.ui.UIHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

// the avatar class
// contains all things related to the avatar
// and also related to the owner, like its permissions
public class Avatar {

    private static CompletableFuture<Void> tasks;
    public static boolean firstPerson;

    // properties
    public final UUID owner;
    public final EntityType<?> entityType;
    public CompoundTag nbt;
    public boolean loaded = true;
    public final boolean isHost;

    //metadata
    public String name, entityName;
    public String authors;
    public Version version;
    public String id;
    public int fileSize;
    public String color;
    public Map<String, String> badgeToColor = new HashMap<>();
    public Map<String, byte[]> resources = new HashMap<>();

    public boolean minify;

    // Runtime data
    private final Queue<Runnable> events = new ConcurrentLinkedQueue<>();
    public final ArrayList<FiguraBuffer> openBuffers = new ArrayList<>();
    public AvatarRenderer renderer;
    public FiguraLuaRuntime luaRuntime;
    public EntityRenderMode renderMode = EntityRenderMode.OTHER;

    public final PermissionPack.PlayerPermissionPack permissions;

    public final Map<String, SoundBuffer> customSounds = new HashMap<>();
    public final Map<Integer, Animation> animations = new HashMap<>();

    // runtime status
    public boolean hasTexture, scriptError;
    public Component errorText;
    public Set<Permissions> noPermissions = new HashSet<>();
    public Set<Permissions> permissionsToTick = new HashSet<>();
    public int lastPlayingSound = 0;
    public int versionStatus = 0;

    // limits
    public int animationComplexity;
    public final Instructions complexity;
    public final Instructions init, render, worldRender, tick, worldTick, animation;
    public final Map<String, Instructions> customInstructions = new HashMap<>();
    public final RefilledNumber particlesRemaining, soundsRemaining;
    private Avatar(UUID owner, EntityType<?> type, String name) {
        this.owner = owner;
        this.entityType = type;
        this.isHost = type == EntityType.PLAYER && FiguraMod.isLocal(owner);
        this.permissions = type == EntityType.PLAYER ? PermissionManager.get(owner) : PermissionManager.getMobPermissions(owner);
        this.complexity = new Instructions(permissions.get(Permissions.COMPLEXITY));
        this.init = new Instructions(permissions.get(Permissions.INIT_INST));
        this.render = new Instructions(permissions.get(Permissions.RENDER_INST));
        this.worldRender = new Instructions(permissions.get(Permissions.WORLD_RENDER_INST));
        this.tick = new Instructions(permissions.get(Permissions.TICK_INST));
        this.worldTick = new Instructions(permissions.get(Permissions.WORLD_TICK_INST));
        this.animation = new Instructions(permissions.get(Permissions.ANIMATION_INST));
        this.particlesRemaining = new RefilledNumber(permissions.get(Permissions.PARTICLES));
        this.soundsRemaining = new RefilledNumber(permissions.get(Permissions.SOUNDS));
        this.entityName = name == null ? "" : name;

        for (Collection<Permissions> pluginPermissions : PermissionManager.CUSTOM_PERMISSIONS.values()) {
            for (Permissions customPermission : pluginPermissions) {
                customInstructions.putIfAbsent(customPermission.name, new Instructions(permissions.get(customPermission)));
            }
        }
    }

    public Avatar(UUID owner) {
        this(owner, EntityType.PLAYER, EntityUtils.getNameForUUID(owner));
    }

    public Avatar(Entity entity) {
        this(entity.getUUID(), entity.getType(), entity.getName().getString());
    }

    public void load(CompoundTag nbt) {
        Runnable toRun = () -> {
            this.nbt = nbt;
            loaded = false;
        };

        if (tasks == null || tasks.isDone()) {
            tasks = CompletableFuture.runAsync(toRun);
        } else {
            tasks.thenRun(toRun);
        }

        tasks.join();

        if (nbt == null) {
            loaded = true;
            return;
        }

        tasks.thenRun(() -> {
            try {
                // metadata
                CompoundTag metadata = nbt.getCompound("metadata");
                name = metadata.getString("name");
                authors = metadata.getString("authors");
                version = new Version(metadata.getString("ver"));
                if (metadata.contains("id"))
                    id = metadata.getString("id");
                if (metadata.contains("color"))
                    color = metadata.getString("color");
                if (metadata.contains("minify"))
                    minify = metadata.getBoolean("minify");
                if (nbt.contains("resources")) {
                    CompoundTag res = nbt.getCompound("resources");
                    for (String k :
                            res.getAllKeys()) {
                        resources.put(k, res.getByteArray(k));
                    }
                }
                for (String key : metadata.getAllKeys()) {
                    if (key.contains("badge_color_")) {
                        badgeToColor.put(key.replace("badge_color_", ""), metadata.getString(key));
                    }
                }
                fileSize = getFileSize();
                versionStatus = getVersionStatus();
                if (entityName.isBlank())
                    entityName = name;

                // animations and models
                loadAnimations();
                renderer = new ImmediateAvatarRenderer(this);

                // sounds and script
                loadCustomSounds();
                createLuaRuntime();
            } catch (Exception e) {
                FiguraMod.LOGGER.error("", e);
                clean();
                this.nbt = null;
                this.renderer = null;
                this.luaRuntime = null;
            }

            loaded = true;
        });
    }

    public void tick() {
        if (scriptError || luaRuntime == null || !loaded)
            return;

        // fetch this avatar entity
        if (luaRuntime.getUser() == null) {
            Entity entity = EntityUtils.getEntityByUUID(owner);
            if (entity != null) {
                luaRuntime.setUser(entity);
                run("ENTITY_INIT", init.post());
            }
        }

        // tick permissions
        for (Permissions t : permissionsToTick) {
            if (permissions.get(t) > 0) {
                noPermissions.remove(t);
            } else {
                noPermissions.add(t);
            }
        }
        if (lastPlayingSound > 0)
            lastPlayingSound--;

        // sound
        particlesRemaining.set(permissions.get(Permissions.PARTICLES));
        particlesRemaining.tick();

        // particles
        soundsRemaining.set(permissions.get(Permissions.SOUNDS));
        soundsRemaining.tick();

        // call events
        FiguraMod.pushProfiler("worldTick");
        worldTick.reset(permissions.get(Permissions.WORLD_TICK_INST));
        run("WORLD_TICK", worldTick);

        FiguraMod.popPushProfiler("tick");
        tick.reset(permissions.get(Permissions.TICK_INST));
        tickEvent();

        FiguraMod.popProfiler();
    }

    public void render(float delta) {
        if (complexity.remaining <= 0) {
            noPermissions.add(Permissions.COMPLEXITY);
        } else {
            noPermissions.remove(Permissions.COMPLEXITY);
        }

        complexity.reset(permissions.get(Permissions.COMPLEXITY));

        if (scriptError || luaRuntime == null || !loaded)
            return;

        render.reset(permissions.get(Permissions.RENDER_INST));
        worldRender.reset(permissions.get(Permissions.WORLD_RENDER_INST));
        run("WORLD_RENDER", worldRender, delta);
    }

    public void runPing(int id, byte[] data) {
        events.offer(() -> {
            if (scriptError || luaRuntime == null || !loaded)
                return;

            LuaValue[] args = PingArg.fromByteArray(data, this);
            String name = luaRuntime.ping.getName(id);
            PingFunction function = luaRuntime.ping.get(name);
            if (args == null || function == null)
                return;

            FiguraLuaPrinter.sendPingMessage(this, name, data.length, args);
            luaRuntime.run(function.func, tick, (Object[]) args);
        });
    }

    public LuaValue loadScript(String name, String chunk) {
        return scriptError || luaRuntime == null || !loaded ? null : luaRuntime.load(name, chunk);
    }

    private void flushQueuedEvents() {
        // run all queued events
        Runnable e;
        while ((e = events.poll()) != null) {
            try {
                e.run();
            } catch (Exception | StackOverflowError ex) {
                if (luaRuntime != null)
                    luaRuntime.error(ex);
            }
        }
    }

    @Nullable
    public Varargs run(Object toRun, Instructions limit, Object... args) {
        // stuff that was not run yet
        flushQueuedEvents();

        if (scriptError || luaRuntime == null || !loaded)
            return null;

        // run event
        Varargs ret = luaRuntime.run(toRun, limit, args);

        // stuff that this run produced
        flushQueuedEvents();

        // return
        return ret;
    }

    public void punish(int amount) {
        if (luaRuntime != null)
            luaRuntime.takeInstructions(amount);
    }

    // -- script events -- // 

    private boolean isCancelled(Varargs args) {
        if (args == null)
            return false;
        for (int i = 1; i <= args.narg(); i++) {
            if (args.arg(i).isboolean() && args.arg(i).checkboolean())
                return true;
        }
        return false;
    }

    public void tickEvent() {
        if (loaded && luaRuntime != null && luaRuntime.getUser() != null)
            run("TICK", tick);
    }

    public void renderEvent(float delta, FiguraMat4 poseMatrix) {
        if (loaded && luaRuntime != null && luaRuntime.getUser() != null)
            run("RENDER", render, delta, renderMode.name(), poseMatrix);
    }

    public void postRenderEvent(float delta, FiguraMat4 poseMatrix) {
        if (loaded && luaRuntime != null && luaRuntime.getUser() != null)
            run("POST_RENDER", render.post(), delta, renderMode.name(), poseMatrix);
        renderMode = EntityRenderMode.OTHER;
    }

    public void postWorldRenderEvent(float delta) {
        if (!loaded)
            return;

        if (renderer != null)
            renderer.allowMatrixUpdate = false;

        run("POST_WORLD_RENDER", worldRender.post(), delta);
    }

    public boolean skullRenderEvent(float delta, BlockStateAPI block, ItemStackAPI item, EntityAPI<?> entity, String mode) {
        Varargs result = null;
        if (loaded && renderer != null && renderer.interceptRendersIntoFigura)
            result = run("SKULL_RENDER", render, delta, block, item, entity, mode);
        return isCancelled(result);
    }

    public boolean useItemEvent(ItemStackAPI stack, String type, int particleCount) {
        Varargs result = loaded ? run("USE_ITEM", tick, stack, type, particleCount) : null;
        return isCancelled(result);
    }

    public boolean arrowRenderEvent(float delta, EntityAPI<?> arrow) {
        Varargs result = null;
        if (loaded) result = run("ARROW_RENDER", render, delta, arrow);
        return isCancelled(result);
    }

    public boolean tridentRenderEvent(float delta, EntityAPI<?> trident) {
        Varargs result = null;
        if (loaded) result = run("TRIDENT_RENDER", render, delta, trident);
        return isCancelled(result);
    }

    public boolean itemRenderEvent(ItemStackAPI item, String mode, FiguraVec3 pos, FiguraVec3 rot, FiguraVec3 scale, boolean leftHanded, PoseStack stack, MultiBufferSource bufferSource, int light, int overlay) {
        if (!loaded || renderer == null || !renderer.interceptRendersIntoFigura) {
            return false;
        }
        Varargs result = run("ITEM_RENDER", render, item, mode, pos, rot, scale, leftHanded);

        if(result == null)
            return false;

        boolean rendered = false;
        for (int i = 1; i <= result.narg(); i++) {
            if (result.arg(i).isuserdata(FiguraModelPart.class))
                rendered |= renderItem(stack, bufferSource, (FiguraModelPart) result.arg(i).checkuserdata(FiguraModelPart.class), light, overlay);
        }
        return rendered;
    }

    public boolean playSoundEvent(String id, FiguraVec3 pos, float vol, float pitch, boolean loop, String category, String file) {
        Varargs result = null;
        if (loaded) result = run("ON_PLAY_SOUND", tick, id, pos, vol, pitch, loop, category, file);
        return isCancelled(result);
    }

    public void resourceReloadEvent() {
        if (loaded) run("RESOURCE_RELOAD", tick);
    }

    public void damageEvent(String sourceType, EntityAPI<?> sourceCause, EntityAPI<?> sourceDirect, FiguraVec3 sourcePosition) {
        if (loaded) run("DAMAGE", tick, sourceType, sourceCause, sourceDirect, sourcePosition);
    }

    // -- host only events -- //

    public String chatSendMessageEvent(String message) { // piped event
        Varargs val = loaded ? run("CHAT_SEND_MESSAGE", tick, message) : null;
        return val == null || (!val.isnil(1) && !Configs.CHAT_MESSAGES.value) ? message : val.isnil(1) ? "" : val.arg(1).tojstring();
    }

    public Pair<String, Integer> chatReceivedMessageEvent(String message, String json) { // special case
        Varargs val = loaded ? run("CHAT_RECEIVE_MESSAGE", tick, message, json) : null;
        if (val == null)
            return null;

        if (val.arg(1).isboolean() && !val.arg(1).checkboolean())
            return Pair.of(null, null);

        String msg = val.isnil(1) ? json : val.arg(1).tojstring();
        Integer color = null;
        if (val.arg(2).isuserdata(FiguraVec3.class))
            color = ColorUtils.rgbToInt((FiguraVec3) val.arg(2).checkuserdata(FiguraVec3.class));

        return Pair.of(msg, color);
    }

    public boolean mouseScrollEvent(double delta) {
        Varargs result = loaded ? run("MOUSE_SCROLL", tick, delta) : null;
        return isCancelled(result);
    }

    public boolean mouseMoveEvent(double x, double y) {
        Varargs result = loaded ? run("MOUSE_MOVE", tick, x, y) : null;
        return isCancelled(result);
    }

    public boolean mousePressEvent(int button, int action, int modifiers) {
        Varargs result = loaded ? run("MOUSE_PRESS", tick, button, action, modifiers) : null;
        return isCancelled(result);
    }

    public boolean keyPressEvent(int key, int action, int modifiers) {
        Varargs result = loaded ? run("KEY_PRESS", tick, key, action, modifiers) : null;
        return isCancelled(result);
    }

    public void charTypedEvent(String chars, int modifiers, int codePoint) {
        if (loaded) run("CHAR_TYPED", tick, chars, modifiers, codePoint);
    }

    public boolean totemEvent() {
        return isCancelled(loaded ? run("TOTEM",tick) : null);
    }

    // -- rendering events -- //

    private void render() {
        if (renderMode == EntityRenderMode.RENDER || renderMode == EntityRenderMode.FIRST_PERSON) {
            complexity.use(renderer.render());
            return;
        }

        int prev = complexity.remaining;
        complexity.remaining = permissions.get(Permissions.COMPLEXITY);
        renderer.render();
        complexity.remaining = prev;
    }

    public void render(Entity entity, float yaw, float delta, float alpha, PoseStack stack, MultiBufferSource bufferSource, int light, int overlay, LivingEntityRenderer<?, ?> entityRenderer, PartFilterScheme filter, boolean translucent, boolean glowing) {
        if (renderer == null || !loaded)
            return;

        renderer.vanillaModelData.update(entityRenderer);
        renderer.yaw = yaw;
        renderer.entity = entity;

        renderer.setupRenderer(
                filter, bufferSource, stack,
                delta, light, alpha, overlay,
                translucent, glowing
        );

        render();
    }

    public synchronized void worldRender(Entity entity, double camX, double camY, double camZ, PoseStack stack, MultiBufferSource bufferSource, int lightFallback, float tickDelta, EntityRenderMode mode) {
        if (renderer == null || !loaded)
            return;

        EntityRenderMode prevRenderMode = renderMode;
        renderMode = mode;
        boolean update = prevRenderMode != EntityRenderMode.OTHER || renderMode == EntityRenderMode.FIRST_PERSON_WORLD;

        renderer.pivotCustomizations.values().clear();
        renderer.allowMatrixUpdate = renderer.updateLight = update;
        renderer.entity = entity;

        renderer.setupRenderer(
                PartFilterScheme.WORLD, bufferSource, stack,
                tickDelta, lightFallback, 1f, OverlayTexture.NO_OVERLAY,
                false, false,
                camX, camY, camZ
        );

        complexity.use(renderer.renderSpecialParts());

        renderMode = prevRenderMode;
        renderer.updateLight = false;
    }

    public void capeRender(Entity entity, MultiBufferSource bufferSource, PoseStack stack, int light, float tickDelta, ModelPart cloak) {
        if (renderer == null || !loaded)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(this);
        FiguraMod.pushProfiler("capeRender");

        renderer.vanillaModelData.update(ParentType.Cape, cloak);
        renderer.entity = entity;

        renderer.setupRenderer(
                PartFilterScheme.CAPE, bufferSource, stack,
                tickDelta, light, 1f, OverlayTexture.NO_OVERLAY,
                renderer.translucent, renderer.glowing
        );

        render();

        FiguraMod.popProfiler(3);
    }

    public void elytraRender(Entity entity, MultiBufferSource bufferSource, PoseStack stack, int light, float tickDelta, EntityModel<?> model) {
        if (renderer == null || !loaded)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(this);
        FiguraMod.pushProfiler("elytraRender");

        renderer.entity = entity;

        renderer.setupRenderer(
                PartFilterScheme.LEFT_ELYTRA, bufferSource, stack,
                tickDelta, light, 1f, OverlayTexture.NO_OVERLAY,
                renderer.translucent, renderer.glowing
        );

        // left
        FiguraMod.pushProfiler("leftWing");
        renderer.vanillaModelData.update(ParentType.LeftElytra, model);
        renderer.renderSpecialParts();

        // right
        FiguraMod.popPushProfiler("rightWing");
        renderer.vanillaModelData.update(ParentType.RightElytra, model);
        renderer.currentFilterScheme = PartFilterScheme.RIGHT_ELYTRA;
        renderer.renderSpecialParts();

        FiguraMod.popProfiler(4);
    }

    public void firstPersonWorldRender(Entity watcher, MultiBufferSource bufferSource, PoseStack matrices, Camera camera, float tickDelta) {
        if (renderer == null || !loaded)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(this);
        FiguraMod.pushProfiler("firstPersonWorldRender");

        int light = Minecraft.getInstance().getEntityRenderDispatcher().getPackedLightCoords(watcher, tickDelta);
        Vec3 camPos = camera.getPosition();

        worldRender(watcher, camPos.x, camPos.y, camPos.z, matrices, bufferSource, light, tickDelta, EntityRenderMode.FIRST_PERSON_WORLD);

        FiguraMod.popProfiler(3);
    }

    public void firstPersonRender(PoseStack stack, MultiBufferSource bufferSource, Player player, PlayerRenderer playerRenderer, ModelPart arm, int light, float tickDelta) {
        if (renderer == null || !loaded)
            return;

        boolean lefty = arm == playerRenderer.getModel().leftArm;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(this);
        FiguraMod.pushProfiler("firstPersonRender");
        FiguraMod.pushProfiler(lefty ? "leftArm" : "rightArm");

        PartFilterScheme filter = lefty ? PartFilterScheme.LEFT_ARM : PartFilterScheme.RIGHT_ARM;
        boolean config = Configs.ALLOW_FP_HANDS.value;
        renderer.allowHiddenTransforms = config;
        renderer.allowMatrixUpdate = false;
        renderer.ignoreVanillaVisibility = true;

        stack.pushPose();
        if (!config) {
            stack.mulPose(Axis.ZP.rotation(arm.zRot));
            stack.mulPose(Axis.YP.rotation(arm.yRot));
            stack.mulPose(Axis.XP.rotation(arm.xRot));
        }
        render(player, 0f, tickDelta, 1f, stack, bufferSource, light, OverlayTexture.NO_OVERLAY, playerRenderer, filter, false, false);
        stack.popPose();

        renderer.allowHiddenTransforms = true;
        renderer.ignoreVanillaVisibility = false;

        FiguraMod.popProfiler(4);
    }

    public void hudRender(PoseStack stack, MultiBufferSource bufferSource, Entity entity, float tickDelta) {
        if (renderer == null || !loaded)
            return;

        FiguraMod.pushProfiler(this);
        FiguraMod.pushProfiler("hudRender");

        stack.pushPose();
        stack.last().pose().scale(16, 16, -16);
        stack.last().normal().scale(1, 1, -1);

        Lighting.setupForFlatItems();
        RenderSystem.disableDepthTest();

        renderer.entity = entity;

        renderer.setupRenderer(
                PartFilterScheme.HUD, bufferSource, stack,
                tickDelta, LightTexture.FULL_BRIGHT, 1f, OverlayTexture.NO_OVERLAY,
                false, false
        );

        if (renderer.renderSpecialParts() > 0)
            ((MultiBufferSource.BufferSource) renderer.bufferSource).endBatch();

        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        stack.popPose();

        FiguraMod.popProfiler(2);
    }

    public boolean skullRender(PoseStack stack, MultiBufferSource bufferSource, int light, Direction direction, float yaw) {
        if (renderer == null || !loaded || !renderer.interceptRendersIntoFigura)
            return false;

        stack.pushPose();

        if (direction == null)
            stack.translate(0.5d, 0d, 0.5d);
        else
            stack.translate((0.5d - direction.getStepX() * 0.25d), 0.25d, (0.5d - direction.getStepZ() * 0.25d));

        stack.scale(-1f, -1f, 1f);
        stack.mulPose(Axis.YP.rotationDegrees(yaw));

        renderer.allowPivotParts = false;

        renderer.setupRenderer(
                PartFilterScheme.SKULL, bufferSource, stack,
                1f, light, 1f, OverlayTexture.NO_OVERLAY,
                false, false
        );

        int comp = renderer.renderSpecialParts();
        complexity.use(comp);

        // head
        boolean bool = comp > 0 || headRender(stack, bufferSource, light, true);

        renderer.allowPivotParts = true;
        stack.popPose();
        return bool;
    }

    public boolean headRender(PoseStack stack, MultiBufferSource bufferSource, int light, boolean useComplexity) {
        if (renderer == null || !loaded)
            return false;

        boolean oldMat = renderer.allowMatrixUpdate;

        // pre render
        renderer.setupRenderer(
                PartFilterScheme.HEAD, bufferSource, stack,
                1f, light, 1f, OverlayTexture.NO_OVERLAY,
                false, false
        );

        renderer.allowHiddenTransforms = false;
        renderer.allowMatrixUpdate = false;
        renderer.ignoreVanillaVisibility = true;

        // render
        int comp = renderer.render();
        if (useComplexity)
            complexity.use(comp);

        // pos render
        renderer.allowMatrixUpdate = oldMat;
        renderer.allowHiddenTransforms = true;
        renderer.ignoreVanillaVisibility = false;

        return comp > 0 && luaRuntime != null && !luaRuntime.vanilla_model.HEAD.checkVisible();
    }

    public boolean renderPortrait(GuiGraphics gui, int x, int y, int size, float modelScale, boolean upsideDown) {
        if (!Configs.AVATAR_PORTRAIT.value || renderer == null || !loaded)
            return false;

        // matrices
        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(x, y, 0d);
        pose.scale(modelScale, modelScale * (upsideDown ? 1 : -1), modelScale);
        pose.mulPose(Axis.XP.rotationDegrees(180f));

        // scissors
        Vector3f pos = pose.last().pose().transformPosition(new Vector3f());

        int x1 = (int) pos.x;
        int y1 = (int) pos.y;
        int x2 = (int) pos.x + size;
        int y2 = (int) pos.y + size;

        gui.enableScissor(x1, y1, x2, y2);
        UIHelper.paperdoll = true;
        UIHelper.dollScale = 16f;

        // setup render
        pose.translate(4d / 16d, upsideDown ? 0 : (8d / 16d), 0d);

        Lighting.setupForFlatItems();

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        int light = LightTexture.FULL_BRIGHT;

        renderer.allowPivotParts = false;

        renderer.setupRenderer(
                PartFilterScheme.PORTRAIT, buffer, pose,
                1f, light, 1f, OverlayTexture.NO_OVERLAY,
                false, false
        );

        // render
        int comp = renderer.renderSpecialParts();
        boolean ret = comp > 0 || headRender(pose, buffer, light, false);

        // after render
        buffer.endBatch();
        pose.popPose();

        gui.disableScissor();
        UIHelper.paperdoll = false;

        renderer.allowPivotParts = true;

        // return
        return ret;
    }

    public boolean renderArrow(PoseStack stack, MultiBufferSource bufferSource, float delta, int light) {
        if (renderer == null || !loaded)
            return false;

        stack.pushPose();
        Quaternionf quaternionf = Axis.XP.rotationDegrees(135f);
        Quaternionf quaternionf2 = Axis.YP.rotationDegrees(-90f);
        quaternionf.mul(quaternionf2);
        stack.mulPose(quaternionf);

        renderer.setupRenderer(
                PartFilterScheme.ARROW, bufferSource, stack,
                delta, light, 1f, OverlayTexture.NO_OVERLAY,
                false, false
        );

        int comp = renderer.renderSpecialParts();

        stack.popPose();
        return comp > 0;
    }

    public boolean renderTrident(PoseStack stack, MultiBufferSource bufferSource, float delta, int light) {
        if (renderer == null || !loaded)
            return false;

        stack.pushPose();
        Quaternionf quaternionf = Axis.ZP.rotationDegrees(90f);
        Quaternionf quaternionf2 = Axis.YP.rotationDegrees(90f);
        quaternionf.mul(quaternionf2);
        stack.mulPose(quaternionf);

        renderer.setupRenderer(
                PartFilterScheme.TRIDENT, bufferSource, stack,
                delta, light, 1f, OverlayTexture.NO_OVERLAY,
                false, false
        );

        int comp = renderer.renderSpecialParts();

        stack.popPose();
        return comp > 0;
    }

    public boolean renderItem(PoseStack stack, MultiBufferSource bufferSource, FiguraModelPart part, int light, int overlay) {
        if (renderer == null || !loaded || part.parentType != ParentType.Item)
            return false;

        stack.pushPose();
        stack.mulPose(Axis.ZP.rotationDegrees(180f));

        renderer.setupRenderer(
                PartFilterScheme.ITEM, bufferSource, stack,
                1f, light, 1f, overlay,
                false, false
        );

        renderer.itemToRender = part;

        int ret = renderer.renderSpecialParts();

        stack.popPose();
        return ret > 0;
    }

    private static final PartCustomization PIVOT_PART_RENDERING_CUSTOMIZATION = new PartCustomization();
    public synchronized boolean pivotPartRender(ParentType parent, Consumer<PoseStack> consumer) {
        if (renderer == null || !loaded || !parent.isPivot)
            return false;

        Queue<Pair<FiguraMat4, FiguraMat3>> queue = renderer.pivotCustomizations.computeIfAbsent(parent, p -> new ConcurrentLinkedQueue<>());

        if (queue.isEmpty())
            return false;

        int i = 0;
        while (!queue.isEmpty() && i++ < 1000) { // limit of 1000 pivot part renders, just in case something goes infinitely somehow
            Pair<FiguraMat4, FiguraMat3> matrixPair = queue.poll();
            PIVOT_PART_RENDERING_CUSTOMIZATION.setPositionMatrix(matrixPair.getFirst());
            PIVOT_PART_RENDERING_CUSTOMIZATION.setNormalMatrix(matrixPair.getSecond());
            PIVOT_PART_RENDERING_CUSTOMIZATION.needsMatrixRecalculation = false;
            PoseStack stack = PIVOT_PART_RENDERING_CUSTOMIZATION.copyIntoGlobalPoseStack();
            consumer.accept(stack);
        }

        queue.clear();
        return true;
    }

    public void updateMatrices(LivingEntityRenderer<?, ?> entityRenderer, PoseStack stack) {
        if (renderer == null || !loaded)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(this);
        FiguraMod.pushProfiler("updateMatrices");

        renderer.vanillaModelData.update(entityRenderer);
        renderer.currentFilterScheme = PartFilterScheme.MODEL;
        renderer.setMatrices(stack);
        renderer.updateMatrices();

        FiguraMod.popProfiler(3);
    }


    // -- animations -- // 


    public void applyAnimations() {
        if (!loaded || scriptError)
            return;

        animation.reset(permissions.get(Permissions.ANIMATION_INST));

        int animationsLimit = permissions.get(Permissions.BB_ANIMATIONS);
        int limit = animationsLimit;
        for (Animation animation : animations.values())
            limit = AnimationPlayer.tick(animation, limit);
        animationComplexity = animationsLimit - limit;

        if (limit <= 0) {
            noPermissions.add(Permissions.BB_ANIMATIONS);
        } else {
            noPermissions.remove(Permissions.BB_ANIMATIONS);
        }
    }

    public void clearAnimations() {
        if (!loaded || scriptError)
            return;

        for (Animation animation : animations.values())
            AnimationPlayer.clear(animation);
    }

    // -- functions -- // 

    /**
     * We should call this whenever an avatar is no longer reachable!
     * It free()s all the CachedType used inside of the avatar, and also
     * closes the native texture resources.
     * also closes and stops this avatar sounds
     */
    public void clean() {
        if (renderer != null)
            renderer.invalidate();

        clearSounds();
        clearParticles();
        closeBuffers();

        events.clear();
    }

    public void clearSounds() {
        SoundAPI.getSoundEngine().figura$stopSound(owner, null);
        if (SoundAPI.getSoundEngine().figura$isEngineActive()) {
            for (SoundBuffer value : customSounds.values())
                value.releaseAlBuffer();
        }
    }

    public void closeBuffers() {
        for (FiguraBuffer buffer :
                openBuffers) {
            if (!buffer.isClosed()) {
                try {
                    buffer.baseClose();
                } catch (Exception ignored) {}
            }
        }
        openBuffers.clear();
    }

    public void clearParticles() {
        ParticleAPI.getParticleEngine().figura$clearParticles(owner);
    }

    private int getFileSize() {
        try {
            // get size
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.writeCompressed(nbt, baos);
            return baos.size();
        } catch (Exception e) {
            FiguraMod.LOGGER.warn("Failed to generate file size for model " + this.name, e);
            return 0;
        }
    }

    private int getVersionStatus() {
        if (version == null || (NetworkStuff.latestVersion != null && version.compareTo(NetworkStuff.latestVersion) > 0))
            return 0;
        return version.compareTo(FiguraMod.VERSION);
    }

    // -- loading -- // 

    private void createLuaRuntime() {
        if (!nbt.contains("scripts"))
            return;

        Map<String, String> scripts = new HashMap<>();
        CompoundTag scriptsNbt = nbt.getCompound("scripts");
        for (String s : scriptsNbt.getAllKeys())
            scripts.put(PathUtils.computeSafeString(s), new String(scriptsNbt.getByteArray(s), StandardCharsets.UTF_8));

        CompoundTag metadata = nbt.getCompound("metadata");

        ListTag autoScripts;
        if (metadata.contains("autoScripts"))
            autoScripts = metadata.getList("autoScripts", Tag.TAG_STRING);
        else
            autoScripts = null;

        FiguraLuaRuntime runtime = new FiguraLuaRuntime(this, scripts);
        if (renderer != null && renderer.root != null)
            runtime.setGlobal("models", renderer.root);

        init.reset(permissions.get(Permissions.INIT_INST));
        runtime.setInstructionLimit(init.remaining);

        events.offer(() -> {
            if (runtime.init(autoScripts))
                init.use(runtime.getInstructions());
        });
    }

    private void loadAnimations() {
        if (!nbt.contains("animations"))
            return;

        ArrayList<String> autoAnims = new ArrayList<>();
        CompoundTag metadata = nbt.getCompound("metadata");
        if (metadata.contains("autoAnims")) {
            for (Tag name : metadata.getList("autoAnims", Tag.TAG_STRING))
                autoAnims.add(name.getAsString());
        }

        ListTag root = nbt.getList("animations", Tag.TAG_COMPOUND);
        for (int i = 0; i < root.size(); i++) {
            try {
                CompoundTag animNbt = root.getCompound(i);

                if (!animNbt.contains("mdl") || !animNbt.contains("name"))
                    continue;

                String mdl = animNbt.getString("mdl");
                String name = animNbt.getString("name");
                Animation.LoopMode loop = Animation.LoopMode.ONCE;
                if (animNbt.contains("loop")) {
                    try {
                        loop = Animation.LoopMode.valueOf(animNbt.getString("loop").toUpperCase(Locale.US));
                    } catch (Exception ignored) {}
                }

                Animation animation = new Animation(this,
                        mdl, name, loop,
                        animNbt.contains("ovr") && animNbt.getBoolean("ovr"),
                        animNbt.contains("len") ? animNbt.getFloat("len") : 0f,
                        animNbt.contains("off") ? animNbt.getFloat("off") : 0f,
                        animNbt.contains("bld") ? animNbt.getFloat("bld") : 1f,
                        animNbt.contains("sdel") ? animNbt.getFloat("sdel") : 0f,
                        animNbt.contains("ldel") ? animNbt.getFloat("ldel") : 0f
                );

                if (animNbt.contains("code")) {
                    for (Tag code : animNbt.getList("code", Tag.TAG_COMPOUND)) {
                        CompoundTag compound = (CompoundTag) code;
                        animation.newCode(compound.getFloat("time"), compound.getString("src"));
                    }
                }

                animations.put(i, animation);

                if (autoAnims.contains(mdl + "." + name))
                    animation.play();
            } catch (Exception ignored) {}
        }
    }

    private void loadCustomSounds() {
        if (!nbt.contains("sounds"))
            return;

        CompoundTag root = nbt.getCompound("sounds");
        for (String key : root.getAllKeys()) {
            try {
                loadSound(key, root.getByteArray(key));
            } catch (Exception e) {
                FiguraMod.LOGGER.warn("Failed to load custom sound \"" + key + "\"", e);
            }
        }
    }

    public void loadSound(String name, byte[] data) throws Exception {
        if (SoundAPI.getSoundEngine().figura$isEngineActive()) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data); JOrbisAudioStream oggAudioStream = new JOrbisAudioStream(inputStream)) {
                SoundBuffer sound = new SoundBuffer(oggAudioStream.readAll(), oggAudioStream.getFormat());
                this.customSounds.put(name, sound);
            }
        } else {
            FiguraMod.LOGGER.error("Sound is not supported or enabled on this system but a custom sound tried to load anyway, scripts may break.");
        }
    }

    public FiguraTexture registerTexture(String name, NativeImage image, boolean ignoreSize) {
        int max = permissions.get(Permissions.TEXTURE_SIZE);
        if (!ignoreSize && (image.getWidth() > max || image.getHeight() > max)) {
            noPermissions.add(Permissions.TEXTURE_SIZE);
            throw new LuaError("Texture exceeded max size of " + max + " x " + max + " resolution, got " + image.getWidth() + " x " + image.getHeight());
        }

        FiguraTexture oldText = renderer.customTextures.get(name);
        if (oldText != null)
            oldText.close();

        if (renderer.customTextures.size() > TextureAPI.TEXTURE_LIMIT)
            throw new LuaError("Maximum amount of textures reached!");

        FiguraTexture texture = new FiguraTexture(this, name, image);
        renderer.customTextures.put(name, texture);
        return texture;
    }

    public static class Instructions {

        public int max, remaining;
        private int currPre, currPost;
        public int pre, post;
        private boolean inverted;

        public Instructions(int remaining) {
            reset(remaining);
        }

        public Instructions post() {
            inverted = true;
            return this;
        }

        public int getTotal() {
            return pre + post;
        }

        public void reset(int remaining) {
            this.max = this.remaining = remaining;
            currPre = currPost = 0;
        }

        public void use(int amount) {
            remaining -= amount;

            if (!inverted) {
                currPre += amount;
                pre = currPre;
            } else {
                currPost += amount;
                post = currPost;
                inverted = false;
            }
        }
    }
}
