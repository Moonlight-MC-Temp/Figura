package org.figuramc.figura.server.commands;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.figuramc.figura.server.FiguraServer;

import java.util.function.Predicate;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

public class FiguraServerCommands {

    public static LiteralArgumentBuilder<FiguraServerCommandSource> getCommand() {
        LiteralArgumentBuilder<FiguraServerCommandSource> root = literal("fsb");
        root.then(FiguraAvatarCommand.getCommand());
        root.then(FiguraBadgesCommand.getCommand());
        return root;
    }

    public static class PermissionPredicate implements Predicate<FiguraServerCommandSource> {
        public final String permission;

        public PermissionPredicate(String permission) {
            this.permission = permission;
        }

        @Override
        public boolean test(FiguraServerCommandSource source) {
            try {
                return source.permission(permission);
            }
            catch (Exception e) {
                FiguraServer.getInstance().logError("Error occured while processing permission check: ", e);
                return false;
            }
        }
    }

    public static PermissionPredicate permissionCheck(String permission) {
        return new PermissionPredicate(permission);
    }
}
