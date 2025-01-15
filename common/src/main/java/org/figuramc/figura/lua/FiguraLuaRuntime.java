package org.figuramc.figura.lua;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.api.*;
import org.figuramc.figura.lua.api.action_wheel.ActionWheelAPI;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.lua.api.entity.NullEntity;
import org.figuramc.figura.lua.api.event.EventsAPI;
import org.figuramc.figura.lua.api.event.LuaEvent;
import org.figuramc.figura.lua.api.keybind.KeybindAPI;
import org.figuramc.figura.lua.api.nameplate.NameplateAPI;
import org.figuramc.figura.lua.api.ping.PingAPI;
import org.figuramc.figura.lua.api.vanilla_model.VanillaModelAPI;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.PathUtils;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JseStringLib;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;

/**
 * One runtime per avatar
 */
public class FiguraLuaRuntime {

    // Global API instances
    //---------------------------------
    public EntityAPI<?> entityAPI;
    public EventsAPI events;
    public VanillaModelAPI vanilla_model;
    public KeybindAPI keybinds;
    public HostAPI host;
    public NameplateAPI nameplate;
    public RendererAPI renderer;
    public ActionWheelAPI action_wheel;
    public AvatarAPI avatar_meta;
    public PingAPI ping;
    public ServerPacketsAPI serverPackets;
    public TextureAPI texture;

    //---------------------------------

    public final Avatar owner;
    private final Globals userGlobals = new Globals();
    private final LuaFunction setHookFunction;
    private final LuaFunction getInfoFunction;
    protected final Map<String, String> scripts = new HashMap<>();
    private final Map<String, Varargs> loadedScripts = new HashMap<>();
    private final Stack<String> loadingScripts = new Stack<>();
    public final LuaTypeManager typeManager = new LuaTypeManager();

    public FiguraLuaRuntime(Avatar avatar, Map<String, String> scripts) {
        this.owner = avatar;
        this.scripts.putAll(scripts);

        // Each user gets their own set of globals as well.
        userGlobals.load(new JseBaseLib());
        userGlobals.load(new Bit32Lib());
        userGlobals.load(new TableLib());
        userGlobals.load(new JseStringLib());
        userGlobals.load(new JseMathLib());

        LuaC.install(userGlobals);

        userGlobals.load(new DebugLib());
        LuaTable debugLib = userGlobals.get("debug").checktable();
        setHookFunction = debugLib.get("sethook").checkfunction();
        getInfoFunction = debugLib.get("getinfo").checkfunction();

        setupFiguraSandbox();

        FiguraAPIManager.setupTypesAndAPIs(this);
        setUser(null);

        loadExtraLibraries();

        LuaTable figuraMetatables = new LuaTable();
        typeManager.dumpMetatables(figuraMetatables);
        setGlobal("figuraMetatables", figuraMetatables);
    }

    public void registerClass(Class<?> clazz) {
        typeManager.generateMetatableFor(clazz);
    }

    public void setGlobal(String name, Object obj) {
        userGlobals.rawset(name, typeManager.javaToLua(obj).arg1());
    }

    public void setUser(Entity user) {
        Object val;
        if (user == null) {
            entityAPI = null;
            val = NullEntity.INSTANCE;
        } else {
            val = entityAPI = EntityAPI.wrap(user);
        }

        userGlobals.rawset("user", typeManager.javaToLua(val).arg1());
        userGlobals.rawset("player", userGlobals.rawget("user"));
    }

    public Entity getUser() {
        return entityAPI != null && entityAPI.isLoaded() ? entityAPI.getEntity() : null;
    }

    // init runtime //

    private void setupFiguraSandbox() {
        // actual sandbox file
        try (InputStream inputStream = FiguraMod.class.getResourceAsStream("/assets/" + FiguraMod.MOD_ID + "/scripts/sandbox.lua")) {
            if (inputStream == null) throw new IOException("Unable to get resource");
            userGlobals.load(new String(inputStream.readAllBytes()), "sandbox").call();
        } catch (Exception e) {
            error(new LuaError("Failed to load builtin sandbox script:\n" + e.getMessage()));
        }

        // read only string metatable
        LuaString.s_metatable = new ReadOnlyLuaTable(LuaString.s_metatable);
    }

    private void loadExtraLibraries() {
        // require
        this.setGlobal("require", require);

        // listFiles
        this.setGlobal("listFiles", listFiles);

        // custom loadstring
        LuaValue loadstring = loadstringConstructor.apply(this);
        this.setGlobal("load", loadstring);
        this.setGlobal("loadstring", loadstring);

        // load print functions
        FiguraLuaPrinter.loadPrintFunctions(this);

        // load JSON functions
        FiguraLuaJson.loadFunctions(this);

        // load math library
        try (InputStream inputStream = FiguraMod.class.getResourceAsStream("/assets/" + FiguraMod.MOD_ID + "/scripts/math.lua")) {
            if (inputStream == null) throw new IOException("Unable to get resource");
            userGlobals.load(new String(inputStream.readAllBytes()), "math").call();
        } catch (Exception e) {
            error(new LuaError("Failed to load builtin math script:\n" + e.getMessage()));
        }

        // Change the type() function
        setGlobal("type", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if (arg.type() == LuaValue.TUSERDATA)
                    return LuaString.valueOf(typeManager.getTypeName(arg.checkuserdata().getClass()));
                if (arg.type() == LuaValue.TTABLE && arg.getmetatable() != null) {
                    LuaValue __type = arg.getmetatable().rawget("__type");
                    if (!__type.isnil())
                        return __type;
                }
                return LuaString.valueOf(arg.typename());
            }

            @Override
            public String tojstring() {
                return typename() + ": type";
            }
        });

        // Change the pairs() function
        LuaFunction globalPairs = userGlobals.get("pairs").checkfunction();
        setGlobal("pairs", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs varargs) {
                LuaValue arg1 = varargs.arg1();
                int type = arg1.type();
                if ((type == LuaValue.TTABLE || type == LuaValue.TUSERDATA) && arg1.getmetatable() != null) {
                    LuaValue __pairs = arg1.getmetatable().rawget("__pairs");
                    if (__pairs.isfunction())
                        return __pairs.invoke(varargs);
                }
                return globalPairs.invoke(varargs);
            }

            @Override
            public String tojstring() {
                return typename() + ": pairs";
            }
        });

        // Change the ipairs() function
        LuaFunction globalIPairs = userGlobals.get("ipairs").checkfunction();
        setGlobal("ipairs", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs varargs) {
                LuaValue arg1 = varargs.arg1();
                int type = arg1.type();
                if ((type == LuaValue.TTABLE || type == LuaValue.TUSERDATA) && arg1.getmetatable() != null) {
                    LuaValue __ipairs = arg1.getmetatable().rawget("__ipairs");
                    if (__ipairs.isfunction())
                        return __ipairs.invoke(varargs);
                }
                return globalIPairs.invoke(varargs);
            }

            @Override
            public String tojstring() {
                return typename() + ": ipairs";
            }
        });
    }

    private final VarArgFunction require = new VarArgFunction() {
        @Override
        public Varargs invoke(Varargs arg) {
            Path path = PathUtils.getPath(arg.checkstring(1));
            Path dir = PathUtils.getWorkingDirectory(getInfoFunction);
            String scriptName = PathUtils.computeSafeString(
                PathUtils.isAbsolute(path) ? path : dir.resolve(path)
            );

            if (loadingScripts.contains(scriptName))
                throw new LuaError("Detected circular dependency in script " + loadingScripts.peek());

            return initializeScript(scriptName);
        }

        @Override
        public String tojstring() {
            return "function: require";
        }
    };

    private final TwoArgFunction listFiles = new TwoArgFunction() {
        @Override
        public LuaValue call(LuaValue folderPath, LuaValue includeSubfolders) {
            Path path = PathUtils.getPath(folderPath);
            Path dir = PathUtils.getWorkingDirectory(getInfoFunction);

            Path targetPath = (PathUtils.isAbsolute(path) ? path : dir.resolve(path)).normalize();

            boolean subFolders = !includeSubfolders.isnil() && includeSubfolders.checkboolean();

            // iterate over all script names and add them if their name starts with the path query
            int i = 1;
            LuaTable table = new LuaTable();
            for (String s : scripts.keySet()) {
                Path scriptPath = PathUtils.getPath(s);

                // Add to table only if the beginning of the path matches
                if (!(scriptPath.startsWith(targetPath)))
                    continue;
                // remove the common parent
                Path result = targetPath.relativize(scriptPath);
                // Paths always have at least one name.
                // If we do not allow subfolders, only allow paths that directly point to a file
                if (!subFolders && result.getNameCount()!=1)
                    continue;
                table.set(i++, LuaValue.valueOf(s.replace('/', '.')));
            }

            return table;
        }
    };
    
    private static final Function<FiguraLuaRuntime, LuaValue> loadstringConstructor = runtime -> new VarArgFunction() {
        @Override
        public Varargs invoke(Varargs args) {
            try {
                // Get source provider function or get string value and create input stream out of that
                LuaValue val = args.arg(1);
                InputStream ld;
                if (val.isfunction()) {
                    ld = new FuncStream(val.checkfunction());
                } else if (val.isstring()) {
                    ld = new ByteArrayInputStream(val.checkstring().m_bytes);
                } else {
                    throw new LuaError("chunk source is neither a string nor function");
                }

                // Get chunk name (this is what it will display as in the source name, like script)
                val = args.arg(2);
                String chunkName = val.isstring() ? val.tojstring() : "loadstring";

                // get environment in which will be used to get global values from, does not make extra lookups outside this table
                val = args.arg(3);
                LuaTable environment = val.istable() ? val.checktable() : runtime.userGlobals;

                // create the function from arguments
                return runtime.userGlobals.load(ld, chunkName, "t", environment);
            } catch (LuaError e) {
                return varargsOf(NIL, e.getMessageObject());
            }
        }

        @Override
        public String tojstring() {
            return "function: loadstring";
        }

        // Class that creates input stream from 
        private static class FuncStream extends InputStream {
            private final LuaFunction function;
            // start at the end of empty string so next index will get first result
            private String string = "";
            private int index = 0;

            public FuncStream(LuaFunction function) {
                this.function = function;
            }

            @Override
            public int read() {
                // if next index is out of bounds
                if (++index >= string.length()) {
                    // reset index
                    index = 0;
                    // fetch next functon value
                    Varargs result = function.invoke();
                    // check if we hit the end, that is nil, no value or empty string
                    if (!result.isstring(1) || result.arg1().length() < 1)
                        return -1;
                    // get string from result of calling function
                    string = new String(result.checkstring(1).m_bytes, StandardCharsets.UTF_8);
                }
                // return next index
                return string.charAt(index);
            }
        }
    };

    // init event //

    private Varargs initializeScript(String str){
        Path path = PathUtils.getPath(str);
        String name = PathUtils.computeSafeString(path);

        // already loaded
        Varargs val = loadedScripts.get(name);
        if (val != null)
            return val;

        // not found
        String src = scripts.get(name);
        if (src == null)
            throw new LuaError("Tried to require nonexistent script \"" + name + "\"!");

        this.loadingScripts.push(name);

        // load
        String directory = PathUtils.computeSafeString(path.getParent());
        String fileName = PathUtils.computeSafeString(path.getFileName());
        Varargs value = userGlobals.load(src, name).invoke(LuaValue.varargsOf(LuaValue.valueOf(directory), LuaValue.valueOf(fileName)));
        if (value == LuaValue.NIL)
            value = LuaValue.TRUE;

        // cache and return
        loadedScripts.put(name, value);
        loadingScripts.pop();
        return value;
    };

    public boolean init(ListTag autoScripts) {
        if (scripts.isEmpty())
            return false;

        owner.luaRuntime = this;

        try {
            if (autoScripts == null) {
                for (String name : scripts.keySet())
                    initializeScript(name);
            } else {
                for (Tag name : autoScripts)
                    initializeScript(name.getAsString());
            }
        } catch (Exception | StackOverflowError e) {
            error(e);
            return false;
        }

        return true;
    }

    // error ^-^ //

    public void error(Throwable e) {
        FiguraLuaPrinter.sendLuaError(parseError(e), owner);
        owner.scriptError = true;
        owner.luaRuntime = null;
        owner.clearParticles();
        owner.clearSounds();
        owner.closeBuffers();
        owner.closeStreams();
    }

    public static LuaError parseError(Throwable e) {
        return e instanceof LuaError lua ? lua : e instanceof StackOverflowError ? new LuaError("Stack Overflow") : new LuaError(e);
    }

    // avatar limiting //

    private final ZeroArgFunction onReachedLimit = new ZeroArgFunction() {
        @Override
        public LuaValue call() {
            FiguraMod.LOGGER.warn("Avatar {} bypassed resource limits with {} instructions", owner.owner, getInstructions());
            LuaError error = new LuaError("Script overran resource limits!");
            owner.noPermissions.add(Permissions.INIT_INST);
            setInstructionLimit(1);
            throw error;
        }
    };

    public void setInstructionLimit(int limit) {
        userGlobals.running.state.bytecodes = 0;
        setHookFunction.invoke(LuaValue.varargsOf(onReachedLimit, LuaValue.EMPTYSTRING, LuaValue.valueOf(Math.max(limit, 1))));
    }

    public int getInstructions() {
        return userGlobals.running.state.bytecodes;
    }

    public void takeInstructions(int amount) {
        userGlobals.running.state.bytecodes += amount;
    }

    // script execution //

    public LuaValue load(String name, String src) {
        return userGlobals.load(src, name, userGlobals);
    }

    public Varargs run(Object toRun, Avatar.Instructions limit, Object... args) {
        // parse args
        LuaValue[] values = new LuaValue[args.length];
        for (int i = 0; i < values.length; i++)
            values[i] = typeManager.javaToLua(args[i]).arg1();

        Varargs val = LuaValue.varargsOf(values);

        // set instructions limit
        setInstructionLimit(limit.remaining);

        // get and call event
        try {
            Varargs ret;
            if (toRun instanceof LuaEvent event)
                ret = event.call(val);
            else if (toRun instanceof String event)
                ret = events.__index(event).call(val);
            else if (toRun instanceof LuaValue func)
                ret = func.invoke(val);
            else
                throw new IllegalArgumentException("Internal event error - Invalid type to run! (" + toRun.getClass().getSimpleName() + ")");

            // use instructions
            limit.use(getInstructions());
            // and return the value
            return ret;
        } catch (Exception | StackOverflowError e) {
            error(e);
        }

        // failsafe return
        return null;
    }
}
