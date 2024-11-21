package org.figuramc.figura.wizards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.codec.binary.Base64;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.exporters.BlockBenchModel;
import org.figuramc.figura.exporters.BlockBenchModel.Cube;
import org.figuramc.figura.exporters.BlockBenchModel.Group;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.utils.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.BiFunction;
import static org.figuramc.figura.model.ParentType.*;

public class AvatarWizard {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private static String playerTexture = "";
    private static String playerTextureSlim = "";
    private static String capeTexture = "";
    private static byte[] iconTexture;

    private static final BiFunction<ResourceManager, String, String> GET_TEXTURE_DATA = (manager, path) -> {
        byte[] bytes = ResourceUtils.getResource(manager, new FiguraIdentifier(path));
        return bytes != null ? Base64.encodeBase64String(bytes) : "";
    };

    public static final FiguraResourceListener RESOURCE_LISTENER = FiguraResourceListener.createResourceListener("avatar_wizard", manager -> {
        playerTexture = GET_TEXTURE_DATA.apply(manager, "textures/avatar_wizard/texture.png");
        playerTextureSlim = GET_TEXTURE_DATA.apply(manager, "textures/avatar_wizard/texture_slim.png");
        capeTexture = GET_TEXTURE_DATA.apply(manager, "textures/avatar_wizard/cape.png");
        iconTexture = ResourceUtils.getResource(manager, new FiguraIdentifier("textures/avatar_wizard/icon.png"));
    });

    private final HashMap<WizardEntry, Object> map = new HashMap<>();

    public void changeEntry(WizardEntry entry, Object value) {
        map.put(entry, value);
    }

    public Object getEntry(WizardEntry entry, Object fallback) {
        return map.getOrDefault(entry, fallback);
    }

    public boolean canBuild() {
        String name = (String) map.get(WizardEntry.NAME);
        return name != null && !name.isBlank();
    }

    public boolean checkDependency(WizardEntry entry) {
        if (entry.dependencies == null)
            return true;

        for (WizardEntry dependency : entry.dependencies) {
            if (!checkDependency(dependency))
                return false;

            if (dependency.type == WizardEntry.Type.CATEGORY)
                continue;

            Object obj = map.get(dependency);
            if (obj == null || !dependency.validate(obj))
                return false;

            boolean bl = switch (dependency.type) {
                case TOGGLE -> (boolean) obj;
                case TEXT -> !((String) obj).isBlank();
                default -> true;
            };
            if (!bl) return false;
        }

        return true;
    }

    public void build() throws IOException {
        //file io
        Path root = LocalAvatarFetcher.getLocalAvatarDirectory();
        String name = (String) map.get(WizardEntry.NAME);
        String filename = name.replaceAll(IOUtils.INVALID_FILENAME_REGEX, "_");

        Path folder = root.resolve(filename);
        int i = 1;
        while (Files.exists(folder)) {
            folder = root.resolve(filename + "_" + i);
            i++;
        }

        //metadata
        byte[] metadata = buildMetadata(name);

        //script
        byte[] script = null;
        if (WizardEntry.DUMMY_SCRIPT.asBool(map))
            script = buildScript();

        //model
        byte[] model = null;
        if (WizardEntry.DUMMY_MODEL.asBool(map))
            model = buildModel();

        //write files
        new IOUtils.DirWrapper(folder)
                .create()
                .write("avatar.json", metadata)
                .write("script.lua", script)
                .write("model.bbmodel", model)
                .write("avatar.png", iconTexture);

        //open file manager
        Util.getPlatform().openUri(folder.toUri());
    }

    private byte[] buildMetadata(String name) {
        JsonObject root = new JsonObject();

        //name
        root.addProperty("name", name);

        //description
        String description = (String) map.get(WizardEntry.DESCRIPTION);
        root.addProperty("description", description == null ? "" : description);

        //authors
        String authorStr = (String) map.get(WizardEntry.AUTHORS);
        String playerName = Minecraft.getInstance().player.getName().getString();
        String[] authors = authorStr == null ? new String[]{playerName} : authorStr.split(",");
        if (authors.length == 0) authors = new String[]{playerName};

        JsonArray authorsJson = new JsonArray();
        for (String author : authors)
            authorsJson.add(author.trim());

        root.add("authors", authorsJson);

        //color
        root.addProperty("color", "#" + ColorUtils.rgbToHex(ColorUtils.Colors.random().vec));

        //return
        return GSON.toJson(root).getBytes();
    }

    private byte[] buildScript() {
        String script = "-- Auto generated script file --\n";

        boolean hasPlayerModel = WizardEntry.PLAYER_MODEL.asBool(map);

        //hide player
        if (hasPlayerModel && WizardEntry.HIDE_PLAYER.asBool(map))
            script += """

                    --hide vanilla model
                    vanilla_model.PLAYER:setVisible(false)
                    """;

        //hide armor
        boolean hideArmor = WizardEntry.HIDE_ARMOR.asBool(map);
        if (hideArmor)
            script += """

                    --hide vanilla armor model
                    vanilla_model.ARMOR:setVisible(false)
                    """;

        //helmet item fix :3
        if (hasPlayerModel && hideArmor && WizardEntry.HELMET_ITEM_PIVOT.asBool(map))
            script += """
                    --re-enable the helmet item
                    vanilla_model.HELMET_ITEM:setVisible(true)
                    """;

        //hide cape
        if (WizardEntry.HIDE_CAPE.asBool(map))
            script += """

                    --hide vanilla cape model
                    vanilla_model.CAPE:setVisible(false)
                    """;

        //hide cape
        if (WizardEntry.HIDE_ELYTRA.asBool(map))
            script += """

                    --hide vanilla elytra model
                    vanilla_model.ELYTRA:setVisible(false)
                    """;

        //empty events
        if (WizardEntry.EMPTY_EVENTS.asBool(map))
            script += """

                    --entity init event, used for when the avatar entity is loaded for the first time
                    function events.entity_init()
                      --player functions goes here
                    end

                    --tick event, called 20 times per second
                    function events.tick()
                      --code goes here
                    end

                    --render event, called every time your avatar is rendered
                    --it have two arguments, "delta" and "context"
                    --"delta" is the percentage between the last and the next tick (as a decimal value, 0.0 to 1.0)
                    --"context" is a string that tells from where this render event was called (the paperdoll, gui, player render, first person)
                    function events.render(delta, context)
                      --code goes here
                    end
                    """;

        //return
        return script.getBytes();
    }

    private byte[] buildModel() {
        boolean hasPlayer = WizardEntry.PLAYER_MODEL.asBool(map);
        boolean hasElytra = WizardEntry.ELYTRA.asBool(map);
        boolean hasCape = WizardEntry.CAPE.asBool(map);
        boolean hasCapeOrElytra = hasCape || hasElytra;
        boolean slim = WizardEntry.SLIM.asBool(map);
        boolean hasArmor = WizardEntry.ARMOR_PIVOTS.asBool(map);

        //model
        BlockBenchModel model = new BlockBenchModel("free");

        //textures
        int playerTex = hasPlayer ? model.addImage("Skin", slim ? playerTextureSlim : playerTexture, 64, 64) : -1;
        int capeTex = hasCapeOrElytra ? model.addImage("Cape", capeTexture, 64, 32) : -1;

        //resolution
        if (hasPlayer)
            model.setResolution(64, 64);
        else if (hasCapeOrElytra)
            model.setResolution(64, 32);

        //base bones
        Group root = model.addGroup("root", FiguraVec3.of());

        Group head = model.addGroup(Head, FiguraVec3.of(0, 24, 0), root);
        Group body = model.addGroup(Body, FiguraVec3.of(0, 24, 0), root);
        Group leftArm = model.addGroup(LeftArm, FiguraVec3.of(-5, 22, 0), root);
        Group rightArm = model.addGroup(RightArm, FiguraVec3.of(5, 22, 0), root);
        Group leftLeg = model.addGroup(LeftLeg, FiguraVec3.of(-1.9, 12, 0), root);
        Group rightLeg = model.addGroup(RightLeg, FiguraVec3.of(1.9, 12, 0), root);

        //player
        if (hasPlayer) {
            generateCubeAndLayer(model, "Hat", FiguraVec3.of(-4, 24, -4), FiguraVec3.of(8, 8, 8), 0.5, head, 0, 0, 32, 0, playerTex);
            generateCubeAndLayer(model, "Jacket", FiguraVec3.of(-4, 12, -2), FiguraVec3.of(8, 12, 4), 0.25, body, 16, 16, 16, 32, playerTex);

            FiguraVec3 armSize = FiguraVec3.of(slim ? 3 : 4, 12, 4);
            generateCubeAndLayer(model, "Left Sleeve", FiguraVec3.of(slim ? -7 : -8, 12, -2), armSize, 0.25, leftArm, 32, 48, 48, 48, playerTex);
            generateCubeAndLayer(model, "Right Sleeve", FiguraVec3.of(4, 12, -2), armSize, 0.25, rightArm, 40, 16, 40, 32, playerTex);

            generateCubeAndLayer(model, "Left Pants", FiguraVec3.of(-3.9, 0, -2), FiguraVec3.of(4, 12, 4), 0.25, leftLeg, 16, 48, 0, 48, playerTex);
            generateCubeAndLayer(model, "Right Pants", FiguraVec3.of(-0.1, 0, -2), FiguraVec3.of(4, 12, 4), 0.25, rightLeg, 0, 16, 0, 32, playerTex);
        }

        //cape
        if (hasCape) {
            Group cape = model.addGroup(Cape, FiguraVec3.of(0, 24, 2), root);
            Cube cube = model.addCube("Cape", FiguraVec3.of(-5, 8, 2), FiguraVec3.of(10, 16, 1), cape);
            cube.generateBoxFaces(0, 0, capeTex, 1, 1);
        }

        //elytra
        if (hasElytra) {
            Group elytra = model.addGroup("Elytra", FiguraVec3.of(0, 24, 2), root);

            //left wing
            Group leftElytra = model.addGroup(LeftElytra, FiguraVec3.of(-5, 24, 2), elytra);
            Cube cube = model.addCube(FiguraVec3.of(-5, 4, 2), FiguraVec3.of(10, 20, 2), leftElytra);
            cube.inflate = 1;
            cube.generateBoxFaces(22, 0, capeTex, 1, 1);

            //right wing
            Group rightElytra = model.addGroup(RightElytra, FiguraVec3.of(5, 24, 2), elytra);
            cube = model.addCube(FiguraVec3.of(-5, 4, 2), FiguraVec3.of(10, 20, 2), rightElytra);
            cube.inflate = 1;
            cube.generateBoxFaces(22, 0, capeTex, -1, 1);
        }

        //pivots
        if (WizardEntry.ITEMS_PIVOT.asBool(map)) {
            model.addGroup(LeftItemPivot, FiguraVec3.of(slim ? -5.5 : -6, 12, -2), leftArm);
            model.addGroup(RightItemPivot, FiguraVec3.of(slim ? 5.5 : 6, 12, -2), rightArm);
        }

        if (WizardEntry.SPYGLASS_PIVOT.asBool(map)) {
            model.addGroup(LeftSpyglassPivot, FiguraVec3.of(-2, 28, -4), head);
            model.addGroup(RightSpyglassPivot, FiguraVec3.of(2, 28, -4), head);
        }

        if (WizardEntry.HELMET_ITEM_PIVOT.asBool(map)) {
            model.addGroup(HelmetItemPivot, FiguraVec3.of(0, 24, 0), head);
        }

        if (WizardEntry.PARROTS_PIVOT.asBool(map)) {
            model.addGroup(LeftParrotPivot, FiguraVec3.of(-6, 24, 0), body);
            model.addGroup(RightParrotPivot, FiguraVec3.of(6, 24, 0), body);
        }

        if (hasArmor) {
            model.addGroup(HelmetPivot, FiguraVec3.of(0, 24, 0), head);
            model.addGroup(ChestplatePivot, FiguraVec3.of(0, 24, 0), body);
            model.addGroup(LeftElytraPivot, FiguraVec3.of(0, 24, 0), body);
            model.addGroup(RightElytraPivot, FiguraVec3.of(0, 24, 0), body);
            model.addGroup(LeftShoulderPivot, FiguraVec3.of(-6, 24, 0), leftArm);
            model.addGroup(RightShoulderPivot, FiguraVec3.of(6, 24, 0), rightArm);
            model.addGroup(LeggingsPivot, FiguraVec3.of(0, 12, 0), body);
            model.addGroup(LeftLeggingPivot, FiguraVec3.of(-2, 12, 0), leftLeg);
            model.addGroup(RightLeggingPivot, FiguraVec3.of(2, 12, 0), rightLeg);
            model.addGroup(LeftBootPivot, FiguraVec3.of(-2, 0, 0), leftLeg);
            model.addGroup(RightBootPivot, FiguraVec3.of(2, 0, 0), rightLeg);
        }

        //return
        return GSON.toJson(model.build()).getBytes();
    }

    private static void generateCubeAndLayer(BlockBenchModel model, String layerName, FiguraVec3 position, FiguraVec3 size, double inflation, Group parent, int x1, int y1, int x2, int y2, int texture) {
        Cube c = model.addCube(position, size, parent);
        c.generateBoxFaces(x1, y1, texture);
        Cube l = model.addCube(layerName, position, size, parent);
        l.inflate = inflation;
        l.generateBoxFaces(x2, y2, texture);
    }
}
