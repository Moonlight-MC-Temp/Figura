package org.figuramc.figura.server;

import org.figuramc.figura.server.utils.Pair;

import java.util.List;

public class FiguraPermissions {
    public static final List<Pair<String, Boolean>> PERMISSIONS_LIST = List.of(
            Pair.of("figura.avatars.immortalize", false),
            Pair.of("figura.avatars.set", false)
    );
}
