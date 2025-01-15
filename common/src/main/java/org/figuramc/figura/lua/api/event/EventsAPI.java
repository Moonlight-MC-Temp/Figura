package org.figuramc.figura.lua.api.event;

import com.mojang.datafixers.util.Pair;
import org.figuramc.figura.entries.FiguraEvent;
import org.figuramc.figura.entries.FiguraVanillaPart;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.api.vanilla_model.VanillaPart;
import org.figuramc.figura.lua.docs.LuaFieldDoc;
import org.figuramc.figura.lua.docs.LuaMetamethodDoc;
import org.figuramc.figura.lua.docs.LuaMetamethodDoc.LuaMetamethodOverload;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;

import java.util.*;

@LuaWhitelist
@LuaTypeDoc(
        name = "EventsAPI",
        value = "events"
)
public class EventsAPI {

    // docs only :woozy:
    @LuaWhitelist
    @LuaFieldDoc("events.entity_init")
    public final LuaEvent ENTITY_INIT = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.tick")
    public final LuaEvent TICK = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.world_tick")
    public final LuaEvent WORLD_TICK = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.render")
    public final LuaEvent RENDER = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.post_render")
    public final LuaEvent POST_RENDER = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.world_render")
    public final LuaEvent WORLD_RENDER = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.post_world_render")
    public final LuaEvent POST_WORLD_RENDER = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.chat_send_message")
    public final LuaEvent CHAT_SEND_MESSAGE = new LuaEvent(true);
    @LuaWhitelist
    @LuaFieldDoc("events.chat_receive_message")
    public final LuaEvent CHAT_RECEIVE_MESSAGE = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.skull_render")
    public final LuaEvent SKULL_RENDER = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.mouse_scroll")
    public final LuaEvent MOUSE_SCROLL = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.mouse_move")
    public final LuaEvent MOUSE_MOVE = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.mouse_press")
    public final LuaEvent MOUSE_PRESS = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.key_press")
    public final LuaEvent KEY_PRESS = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.char_typed")
    public final LuaEvent CHAR_TYPED = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.use_item")
    public final LuaEvent USE_ITEM = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.arrow_render")
    public final LuaEvent ARROW_RENDER = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.trident_render")
    public final LuaEvent TRIDENT_RENDER = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.item_render")
    public final LuaEvent ITEM_RENDER = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.on_play_sound")
    public final LuaEvent ON_PLAY_SOUND = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.particle_created")
    public final LuaEvent PARTICLE_CREATED = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.resource_reload")
    public final LuaEvent RESOURCE_RELOAD = new LuaEvent();
    @LuaWhitelist
    @LuaFieldDoc("events.totem")
    public final LuaEvent TOTEM = new LuaEvent();
    @LuaFieldDoc("events.damage")
    public final LuaEvent DAMAGE = new LuaEvent();

    private final Map<String, LuaEvent> events = new HashMap<>();
    
    public EventsAPI() {
        events.put("ENTITY_INIT", ENTITY_INIT);
        events.put("TICK", TICK);
        events.put("WORLD_TICK", WORLD_TICK);
        events.put("RENDER", RENDER);
        events.put("POST_RENDER", POST_RENDER);
        events.put("WORLD_RENDER", WORLD_RENDER);
        events.put("POST_WORLD_RENDER", POST_WORLD_RENDER);
        events.put("CHAT_SEND_MESSAGE", CHAT_SEND_MESSAGE);
        events.put("CHAT_RECEIVE_MESSAGE", CHAT_RECEIVE_MESSAGE);
        events.put("SKULL_RENDER", SKULL_RENDER);
        events.put("MOUSE_SCROLL", MOUSE_SCROLL);
        events.put("MOUSE_MOVE", MOUSE_MOVE);
        events.put("MOUSE_PRESS", MOUSE_PRESS);
        events.put("KEY_PRESS", KEY_PRESS);
        events.put("CHAR_TYPED", CHAR_TYPED);
        events.put("USE_ITEM", USE_ITEM);
        events.put("ARROW_RENDER", ARROW_RENDER);
        events.put("TRIDENT_RENDER", TRIDENT_RENDER);
        events.put("ITEM_RENDER", ITEM_RENDER);
        events.put("ON_PLAY_SOUND", ON_PLAY_SOUND);
        events.put("PARTICLE_CREATED", PARTICLE_CREATED);
        events.put("RESOURCE_RELOAD", RESOURCE_RELOAD);
        events.put("TOTEM", TOTEM);
        events.put("DAMAGE", DAMAGE);

        for (FiguraEvent entrypoint : ENTRYPOINTS) {
            String ID = entrypoint.getID().toUpperCase(Locale.US);
            for (Pair<String, LuaEvent> event : entrypoint.getEvents()) {
                String name = ID + "." + event.getFirst().toUpperCase(Locale.US);
                events.put(name, event.getSecond());
            }
        }
    }

    private static final List<FiguraEvent> ENTRYPOINTS = new ArrayList<>();
    public static void initEntryPoints(Set<FiguraEvent> set) {
        ENTRYPOINTS.addAll(set);
    }

    
    @LuaWhitelist
    @LuaMethodDoc("events.get_events")
    public Map<String, LuaEvent> getEvents() {
        return events;
    }

    @LuaWhitelist
    @LuaMetamethodDoc(overloads = @LuaMetamethodOverload(
            types = {LuaEvent.class, EventsAPI.class, String.class},
            comment = "events.__index.comment1"
    ))
    public LuaEvent __index(String key) {
        if (key == null) return null;
        return events.get(key.toUpperCase(Locale.US));
    }

    @LuaWhitelist
    public void __newindex(@LuaNotNil String key, LuaFunction func) {
        LuaEvent event = __index(key.toUpperCase(Locale.US));
        if (event != null)
            event.register(func, null);
        else throw new LuaError("Cannot assign value on key \"" + key + "\"");
    }

    @Override
    public String toString() {
        return "EventsAPI";
    }
}
