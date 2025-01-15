package org.figuramc.figura.server.events;

import java.util.ArrayList;
import java.util.HashMap;

public final class Events {
    private static final HashMap<Class<? extends Event>, ArrayList<Handler<?>>> handlers = new HashMap<>();

    public static <T extends Event> void registerHandler(Class<T> eventClass, Handler<T> handler) {
        registerHandler(eventClass, handler, 0);
    }

    public static <T extends Event> void registerHandler(Class<T> eventClass, Handler<T> handler, int priority) {
        var handlers = getHandlers(eventClass);
        handlers.add(new RegisteredHandler<>(priority, handler));
        handlers.sort(RegisteredHandler::compareTo);
    }

    public static <T extends Event> void removeHandler(Class<T> eventClass, Handler<T> handler) {
        getHandlers(eventClass).removeIf(r -> r.handler.equals(handler));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Event> T call(T event) {
        var handlers = (ArrayList<RegisteredHandler<T>>) (Object) getHandlers(event.getClass());
        for (RegisteredHandler<T> regHandler: handlers) {
            regHandler.handler.handle(event);
            if (!event.canContinue()) break;
        }
        return event;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> ArrayList<RegisteredHandler<T>> getHandlers(Class<T> clazz) {
        return (ArrayList<RegisteredHandler<T>>) (Object) handlers.computeIfAbsent(clazz, k -> new ArrayList<>());
    }

    public interface Handler<E extends Event> {
        void handle(E event);
    }

    private record RegisteredHandler<T extends Event>(int priority, Handler<T> handler) implements Comparable<RegisteredHandler<T>> {

        @Override
        public int compareTo(RegisteredHandler<T> o) {
            return o.priority - priority;
        }
    }
}
