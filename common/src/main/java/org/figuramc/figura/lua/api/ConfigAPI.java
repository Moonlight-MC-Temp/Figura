package org.figuramc.figura.lua.api;

import com.google.gson.*;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.ReadOnlyLuaTable;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.matrix.FiguraMatrix;
import org.figuramc.figura.math.vector.FiguraVector;
import org.figuramc.figura.utils.IOUtils;
import org.figuramc.figura.utils.MathUtils;
import org.luaj.vm2.*;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@LuaWhitelist
@LuaTypeDoc(
        name = "ConfigAPI",
        value = "config"
)
public class ConfigAPI {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private enum Type {
        BOOL,
        INT,
        DOUBLE,
        STRING,
        TABLE,
        VECTOR,
        MATRIX
    }

    private final Avatar owner;
    private final boolean isHost;
    private LuaTable luaTable;
    private String name;
    private boolean loaded = false;

    public ConfigAPI(Avatar owner) {
        this.owner = owner;
        this.isHost = owner.isHost;
        this.name = owner.name;
    }


    // -- IO -- // 


    public static Path getConfigDataDir() {
        return IOUtils.getOrCreateDir(FiguraMod.getFiguraDirectory(), "config");
    }

    public static void clearAllData() {
        IOUtils.deleteFile(getConfigDataDir());
    }

    private Path getPath() {
        try {
            Path dir = getConfigDataDir().toAbsolutePath();
            Path path = dir.resolve(Paths.get(name + ".json")).toAbsolutePath();

            if (dir.compareTo(path.getParent()) != 0)
                throw new Exception();

            return path;
        } catch (Exception ignored) {
            throw new LuaError("Failed to parse file name \"" + name + "\"");
        }
    }

    private void writeWithRetries(Path path, byte[] data, int retries) throws Exception {
        try (OutputStream fs = Files.newOutputStream(path)) {
            fs.write(data);    
        } catch (Exception e) {
            if (retries > 0) {
                Thread.sleep(5); // this feels suspicious, but it shouldn't be noticable (25ms at most, and only on save)
                writeWithRetries(path, data, retries - 1);
            } else {
                throw e;
            }
        }
    }
        
    // write
    private void write() {
        // parse file target
        Path path = getPath();

        // parse the table
        JsonObject root = new JsonObject();
        for (LuaValue key : luaTable.keys())
            root.add(key.toString(), writeArg(luaTable.get(key), new JsonObject()));

        // write file
        try {
            writeWithRetries(path, GSON.toJson(root).getBytes(), 5);
        } catch (Exception e) {
            FiguraMod.LOGGER.error("", e);
            throw new LuaError("Failed to save avatar data file: " + e);
        }
    }

    private static JsonElement writeArg(LuaValue val, JsonObject obj) {
        if (val.isboolean()) {
            obj.addProperty("type", Type.BOOL.name());
            obj.addProperty("data", val.checkboolean());
        } else if (val instanceof LuaString str) {
            writeString(str, obj);
        } else if (val.isint()) {
            obj.addProperty("type", Type.INT.name());
            obj.addProperty("data", val.checkinteger().v);
        } else if (val.isnumber()) {
            obj.addProperty("type", Type.DOUBLE.name());
            obj.addProperty("data", val.checkdouble());
        } else if (val.istable()) {
            writeTable(val.checktable(), obj);
        } else if (val.isuserdata(FiguraVector.class)) {
            writeVec((FiguraVector<?, ?>) val.checkuserdata(), obj);
        } else if (val.isuserdata(FiguraMatrix.class)) {
            writeMat((FiguraMatrix<?, ?>) val.checkuserdata(), obj);
        } else {
            return JsonNull.INSTANCE;
        }

        return obj;
    }

    private static void writeString(LuaString string, JsonObject obj) {
        int len = string.length();
        byte[] copyTarget = new byte[len];
        string.copyInto(0, copyTarget, 0, len);
        String b64 = Base64.getEncoder().encodeToString(copyTarget);

        obj.addProperty("type", Type.STRING.name());
        obj.addProperty("data", b64);
    }

    private static void writeTable(LuaTable table, JsonObject obj) {
        JsonArray tbl = new JsonArray();
        for (LuaValue key : table.keys()) {
            JsonElement val = writeArg(table.get(key), new JsonObject());
            if (val == JsonNull.INSTANCE)
                continue;

            JsonObject children = new JsonObject();
            children.add("key", writeArg(key, new JsonObject()));
            children.add("value", val);
            tbl.add(children);
        }

        obj.addProperty("type", Type.TABLE.name());
        obj.add("data", tbl);
    }

    private static void writeVec(FiguraVector<?, ?> vector, JsonObject obj) {
        JsonArray vec = new JsonArray();
        for (int i = 0; i < vector.size(); i++)
            vec.add(vector.index(i));

        obj.addProperty("type", Type.VECTOR.name());
        obj.add("data", vec);
    }

    private static void writeMat(FiguraMatrix<?, ?> matrix, JsonObject obj) {
        JsonArray mat = new JsonArray();
        for (int i = 0; i < matrix.cols(); i++) {
            JsonObject vec = new JsonObject();
            writeVec(matrix.getColumn(i + 1), vec);
            mat.add(vec);
        }

        obj.addProperty("type", Type.MATRIX.name());
        obj.add("data", mat);
    }

    // read
    private void init() {
        if (loaded) return;
        luaTable = new LuaTable();

        // read file
        Path path = getPath();
        JsonObject root;

        if (!Files.exists(path))
            return;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element.isJsonNull())
                return;

            root = element.getAsJsonObject();
            for (String key : root.keySet())
                luaTable.set(key, readArg(root.get(key), owner));
        } catch (Exception e) {
            FiguraMod.LOGGER.error("", e);
            throw new LuaError("Failed to load avatar data file");
        }

        loaded = true;
    }

    private static LuaValue readArg(JsonElement json, Avatar owner) {
        JsonObject obj = json.getAsJsonObject();
        Type type = Type.valueOf(obj.get("type").getAsString());
        JsonElement data = obj.get("data");
        return switch (type) {
            case BOOL -> LuaBoolean.valueOf(data.getAsBoolean());
            case INT -> LuaInteger.valueOf(data.getAsInt());
            case DOUBLE -> LuaDouble.valueOf(data.getAsDouble());
            case STRING -> LuaString.valueOf(Base64.getDecoder().decode(data.getAsString()));
            case TABLE -> readTable(data.getAsJsonArray(), owner);
            case VECTOR -> owner.luaRuntime.typeManager.javaToLua(readVec(data.getAsJsonArray())).arg1();
            case MATRIX -> owner.luaRuntime.typeManager.javaToLua(readMat(data.getAsJsonArray())).arg1();
        };
    }

    private static LuaValue readTable(JsonArray arr, Avatar owner) {
        LuaTable table = new LuaTable();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject entry = arr.get(i).getAsJsonObject();
            LuaValue key = readArg(entry.get("key"), owner);
            LuaValue val = readArg(entry.get("value"), owner);
            table.set(key, val);
        }
        return table;
    }

    private static FiguraVector<?, ?> readVec(JsonArray arr) {
        double[] array = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++)
            array[i] = arr.get(i).getAsDouble();

        return MathUtils.sizedVector(array);
    }

    private static FiguraMatrix<?, ?> readMat(JsonArray arr) {
        FiguraVector<? ,?>[] vectors = new FiguraVector[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            JsonObject vec = arr.get(i).getAsJsonObject();
            vectors[i] = readVec(vec.get("data").getAsJsonArray());
        }

        return MathUtils.sizedMat(vectors);
    }


    // -- lua -- // 


    @LuaWhitelist
    @LuaMethodDoc("config.get_name")
    public String getName() {
        return name;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "name"
            ),
            aliases = "name",
            value = "config.set_name"
    )
    public ConfigAPI setName(@LuaNotNil String name) {
        if (!isHost) return this;
        this.name = name;
        this.loaded = false;
        return this;
    }

    @LuaWhitelist
    public ConfigAPI name(@LuaNotNil String name) {
        return setName(name);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = {String.class, LuaValue.class},
                    argumentNames = {"key", "value"}
            ),
            value = "config.save"
    )
    public ConfigAPI save(@LuaNotNil String key, LuaValue val) {
        if (!isHost)
            return this;

        init();

        val = val != null && (val.isboolean() || val.isstring() || val.isnumber() || val.istable() || val.isuserdata(FiguraVector.class) || val.isuserdata(FiguraMatrix.class)) ? val : LuaValue.NIL;
        luaTable.set(key, val);
        write();

        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            returnType = LuaTable.class
                    ),
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "key",
                            returnType = Object.class
                    )
            },
            value = "config.load"
    )
    public Object load(String key) {
        if (!isHost)
            return null;

        init();
        return key != null ? luaTable.get(key) : new ReadOnlyLuaTable(luaTable);
    }

    @Override
    public String toString() {
        return "ConfigAPI";
    }
}
