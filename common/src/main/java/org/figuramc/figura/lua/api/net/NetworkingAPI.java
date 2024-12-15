package org.figuramc.figura.lua.api.net;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.utils.ColorUtils;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaFieldDoc;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.permissions.Permissions;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Locale;

@LuaWhitelist
@LuaTypeDoc(
        name = "NetworkingAPI",
        value = "net"
)
public class NetworkingAPI {
    private static FileOutputStream logFileOutputStream;
    private static final String NETWORKING_IS_HOST_ONLY = "NetworkingAPI is only allowed in a host environment!";
    private static final String NETWORKING_DISABLED_ERROR_TEXT = "Networking is disabled in config";
    private static final String NO_PERMISSION_ERROR_TEXT = "This avatar doesn't have networking permissions";
    private static final String NETWORKING_DISALLOWED_FOR_LINK_ERROR = "Networking whitelist/blacklist does not allow access to link: %s";
    final Avatar owner;
    @LuaWhitelist
    @LuaFieldDoc("net.http")
    public final HttpRequestsAPI http;

    public NetworkingAPI(Avatar owner) {
        this.owner = owner;
        http = new HttpRequestsAPI(this);
    }

    public void securityCheck(String link) throws RuntimeException {
        if (!owner.isHost)
            throw new LuaError(NETWORKING_IS_HOST_ONLY);
        if (!Configs.ALLOW_NETWORKING.value)
            throw new LuaError(NETWORKING_DISABLED_ERROR_TEXT);
        if (owner.permissions.get(Permissions.NETWORKING) < 1) {
            owner.noPermissions.add(Permissions.NETWORKING);
            throw new LuaError(NO_PERMISSION_ERROR_TEXT);
        }
        if (!isLinkAllowed(link)) {
            throw new LinkNotAllowedException(NETWORKING_DISALLOWED_FOR_LINK_ERROR.formatted(link));
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "net.is_networking_allowed",
            overloads = @LuaMethodOverload(
                    returnType = Boolean.class
            )
    )
    public boolean isNetworkingAllowed() {
        return owner.isHost && Configs.ALLOW_NETWORKING.value && owner.permissions.get(Permissions.NETWORKING) >= 1;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "net.is_link_allowed",
            overloads = @LuaMethodOverload(
                    argumentNames = "link",
                    argumentTypes = String.class,
                    returnType = Boolean.class
            )
    )
    public boolean isLinkAllowed(String link) {
        if (!owner.isHost)
            throw new LuaError(NETWORKING_IS_HOST_ONLY);

        RestrictionLevel level = RestrictionLevel.getById(Configs.NETWORKING_RESTRICTION.value);
        if (level == null) return false;
        ArrayList<Filter> filters = Configs.NETWORK_FILTER.getFilters();
        try {
            // URL url = new URL(link);
            // if (url.getPort() != -1 && url.getPort() != 80 && url.getPort() != 443)
            //     throw new LuaError("Port %s not allowed, only 80 (HTTP) and 443 (HTTPS) are permitted.".formatted(url.getPort()));

            return switch (level) {
                case WHITELIST -> filters.stream().anyMatch(f -> f.matches(url.getHost()));
                case BLACKLIST -> filters.stream().noneMatch(f -> f.matches(url.getHost()));
                case NONE -> true;
            };
        }
        catch (MalformedURLException e) {
            throw new LinkNotAllowedException(NETWORKING_DISALLOWED_FOR_LINK_ERROR.formatted(link));
        }
    }

    void log(LogSource source, Component text) {
        // 0 - FILE, 1 - FILE + LOGGER, 2 - FILE + LOGGER + CHAT, 3 - NONE
        int log = Configs.LOG_NETWORKING.value;
        if (log == 3) return;
        MutableComponent finalText =
                Component.literal("[networking:%s:%s] ".formatted(source.name().toLowerCase(Locale.US),owner.entityName))
                        .withStyle(ColorUtils.Colors.LUA_PING.style)
                        .append(text.copy().withStyle(ChatFormatting.WHITE));
        String logTextString = finalText.getString();
        switch (log) {
            case 2 -> FiguraMod.sendChatMessage(finalText);
            case 1 -> FiguraMod.LOGGER.info(logTextString);
        }
        if (logFileOutputStream == null) prepareLogStream();
        try {
            LocalTime t = LocalTime.now();
            writeToLogStream("[%02d:%02d:%02d] [INFO] %s\n".formatted(t.getHour(), t.getMinute(),
                    t.getSecond(), finalText.getString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void error(LogSource source, Component text) {
        // 0 - FILE, 1 - FILE + LOGGER, 2 - FILE + LOGGER + CHAT, 3 - NONE
        int log = Configs.LOG_NETWORKING.value;
        if (log == 3) return;
        MutableComponent finalText =
                Component.literal("[networking:%s:%s] ".formatted(source.name().toLowerCase(Locale.US),owner.entityName))
                        .withStyle(ColorUtils.Colors.LUA_ERROR.style)
                        .append(text.copy().withStyle(ChatFormatting.WHITE));
        String logTextString = finalText.getString();
        switch (log) {
            case 2 -> FiguraMod.sendChatMessage(finalText);
            case 1 -> FiguraMod.LOGGER.error(logTextString);
        }
        if (logFileOutputStream == null) prepareLogStream();
        try {
            LocalTime t = LocalTime.now();
            writeToLogStream("[%02d:%02d:%02d] [ERROR] %s\n".formatted(t.getHour(), t.getMinute(),
                    t.getSecond(), finalText.getString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeToLogStream(String s) throws IOException {
        logFileOutputStream.write(s.getBytes(StandardCharsets.UTF_8));
        logFileOutputStream.flush();
    }

    private static void prepareLogStream() {
        try {
            Path p = FiguraMod.getFiguraDirectory().resolve("logs");
            File folder = p.toFile();
            folder.mkdirs();
            LocalDate d = LocalDate.now();
            File logFile = p.resolve(
                    String.format("%d-%02d-%02d.log", d.getYear(), d.getMonthValue(), d.getDayOfMonth())
            ).toFile();
            logFileOutputStream = new FileOutputStream(logFile, true);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Filter {

        private String filterSource;

        public Filter(String source) {
            setSource(source.trim());
        }

        public String getSource() {
            return filterSource;
        }

        public void setSource(String filterSource) {
            this.filterSource = filterSource;
        }

        public boolean matches(String s) {
            return s.trim().equalsIgnoreCase(filterSource);
        }
    }

    public enum RestrictionLevel {
        WHITELIST(0),
        BLACKLIST(1),
        NONE(2);
        private final int id;
        RestrictionLevel(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static RestrictionLevel getById(int id) {
            for (RestrictionLevel t :
                    RestrictionLevel.values()) {
                if (t.id == id) return t;
            }
            return null;
        }
    }

    enum LogSource {
        HTTP, SOCKET
    }

    @LuaWhitelist
    public Object __index(LuaValue key) {
        if (!key.isstring()) return null;
        if (key.tojstring().equals("http")) {
            return http;
        }
        return null;
    }

    static class LinkNotAllowedException extends RuntimeException {
        public final LuaError luaError;
        public LinkNotAllowedException(String message) {
            luaError = new LuaError(message);
        }

        @Override
        public String toString() {
            return "LinkNotAllowedException";
        }
    }

    @Override
    public String toString() {
        return "NetworkingAPI";
    }
}
