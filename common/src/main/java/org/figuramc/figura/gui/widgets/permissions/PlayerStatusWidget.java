package org.figuramc.figura.gui.widgets.permissions;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.gui.widgets.StatusWidget;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class PlayerStatusWidget extends StatusWidget {

    private static final List<Function<Avatar, MutableComponent>> HOVER_TEXT = List.of(
            avatar -> FiguraText.of("gui.permissions.size")
                    .append("\n• ").append(MathUtils.asFileSize(avatar.fileSize)),
            avatar -> FiguraText.of("gui.permissions.complexity")
                    .append("\n• ").append(String.valueOf(avatar.complexity.pre)),
            avatar -> FiguraText.of("gui.permissions.init")
                    .append("\n• ").append(FiguraText.of("gui.permissions.init.root", avatar.init.pre))
                    .append("\n• ").append(FiguraText.of("gui.permissions.init.entity", avatar.init.post)),
            avatar -> FiguraText.of("gui.permissions.tick")
                    .append("\n• ").append(FiguraText.of("gui.permissions.tick.world", avatar.worldTick.pre))
                    .append("\n• ").append(FiguraText.of("gui.permissions.tick.entity", avatar.tick.pre)),
            avatar -> FiguraText.of("gui.permissions.render")
                    .append("\n• ").append(FiguraText.of("gui.permissions.render.world", avatar.worldRender.pre))
                    .append("\n• ").append(FiguraText.of("gui.permissions.render.entity", avatar.render.pre))
                    .append("\n• ").append(FiguraText.of("gui.permissions.render.post_entity", avatar.render.post))
                    .append("\n• ").append(FiguraText.of("gui.permissions.render.post_world", avatar.worldRender.post))
                    .append("\n• ").append(FiguraText.of("gui.permissions.render.animations", avatar.animation.pre))
    );

    private final UUID owner;
    private Avatar avatar;
    private int size, complexity, init, tick, render;

    public PlayerStatusWidget(int x, int y, int width, UUID owner) {
        super(x, y, width, HOVER_TEXT.size());
        setBackground(false);

        this.owner = owner;
    }

    @Override
    public void tick() {
        avatar = AvatarManager.getAvatarForPlayer(owner);
        if (avatar == null || avatar.nbt == null) {
            status = 0;
            return;
        }

        // size
        size = !FiguraMod.isLocal(owner) ? 3 : avatar.fileSize > NetworkStuff.getSizeLimit() ? 1 : avatar.fileSize > NetworkStuff.getSizeLimit() * 0.75 ? 2 : 3;

        // complexity
        complexity = avatar.renderer == null ? 0 : avatar.complexity.pre >= avatar.permissions.get(Permissions.COMPLEXITY) ? 1 : 3;

        // script init
        init = avatar.scriptError ? 1 : avatar.luaRuntime == null ? 0 : avatar.init.getTotal() >= avatar.permissions.get(Permissions.INIT_INST) * 0.75 ? 2 : 3;

        // script tick
        tick = avatar.scriptError ? 1 : avatar.luaRuntime == null ? 0 : avatar.tick.getTotal() >= avatar.permissions.get(Permissions.TICK_INST) * 0.75 || avatar.worldTick.getTotal() >= avatar.permissions.get(Permissions.WORLD_TICK_INST) * 0.75 ? 2 : 3;

        // script render
        render = avatar.scriptError ? 1 : avatar.luaRuntime == null ? 0 : avatar.render.getTotal() >= avatar.permissions.get(Permissions.RENDER_INST) * 0.75 || avatar.worldRender.getTotal() >= avatar.permissions.get(Permissions.WORLD_RENDER_INST) * 0.75 ? 2 : 3;
    }

    @Override
    public MutableComponent getStatusIcon(int type) {
        return Component.literal(String.valueOf(STATUS_INDICATORS.charAt(switch (type) {
            case 0 -> size;
            case 1 -> complexity;
            case 2 -> init;
            case 3 -> tick;
            case 4 -> render;
            default -> 0;
        }))).setStyle(Style.EMPTY.withFont(UIHelper.UI_FONT));
    }

    @Override
    public Component getTooltipFor(int i) {
        int color = switch (i) {
            case 0 -> size;
            case 1 -> complexity;
            case 2 -> init;
            case 3 -> tick;
            case 4 -> render;
            default -> 0;
        };
        return avatar == null ? null : HOVER_TEXT.get(i).apply(avatar).setStyle(TEXT_COLORS.get(color));
    }
}
