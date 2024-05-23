package org.figuramc.figura.gui.widgets.avatar;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.gui.widgets.AbstractContainerElement;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.ContextMenu;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.ui.UIHelper;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Locale;

public abstract class AbstractAvatarWidget extends AbstractContainerElement implements Comparable<AbstractAvatarWidget> {

    protected static final int SPACING = 6;
    protected static final Component FAVOURITE = Component.literal("★").withStyle(Style.EMPTY.withFont(UIHelper.UI_FONT));
    protected static final Component ADD_FAVOURITE = FiguraText.of("gui.context.favorite.add");
    protected static final Component REMOVE_FAVOURITE = FiguraText.of("gui.context.favorite.remove");

    protected final AvatarList parent;
    protected final int depth;
    protected final ContextMenu context;

    protected LocalAvatarFetcher.AvatarPath avatar;
    protected Button button;
    protected String filter = "";
    protected boolean favourite;

    public AbstractAvatarWidget(int depth, int width, int height, LocalAvatarFetcher.AvatarPath avatar, AvatarList parent) {
        super(0, 0, width, height);
        this.parent = parent;
        this.avatar = avatar;
        this.depth = depth;
        this.context = new ContextMenu(this);
        this.favourite = avatar.isFavourite();

        context.addAction(favourite ? REMOVE_FAVOURITE : ADD_FAVOURITE, null, button -> {
            favourite = !favourite;
            avatar.setFavourite(favourite);
            button.setMessage(favourite ? REMOVE_FAVOURITE : ADD_FAVOURITE);
            context.updateDimensions();
        });
        context.addAction(FiguraText.of("gui.context.open_folder"), null, button -> {
            try {
                Util.getPlatform().openUri(avatar.getFSPath().toUri());
            } catch (Exception e) {
                FiguraMod.debug("failed to open avatar folder: ", e.getMessage());
                Util.getPlatform().openUri(LocalAvatarFetcher.getLocalAvatarDirectory().toUri());
            }
        });
        context.addAction(FiguraText.of("gui.context.copy_path"), null, button -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(avatar.getFSPath().toString());
            FiguraToast.sendToast(FiguraText.of("toast.clipboard"));
        });
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        if (!isVisible() || !this.button.isVisible())
            return;

        super.render(gui, mouseX, mouseY, delta);

        if (favourite) {
            Font font = Minecraft.getInstance().font;
            int width = font.width(FAVOURITE);
            int x = this.getX() + this.getWidth() - (width + 2);
            int y = this.getY() + 2;

            gui.drawString(font, FAVOURITE, x, y, 0xFFFFFF, false);

            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + font.lineHeight)
                UIHelper.setTooltip(FiguraText.of("gui.favorited").append(" ").append(FAVOURITE));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isMouseOver(mouseX, mouseY))
            return false;

        if (super.mouseClicked(mouseX, mouseY, button))
            return true;

        // context menu on right click
        if (button == 1) {
            context.setX((int) mouseX);
            context.setY((int) mouseY);
            context.setVisible(true);
            UIHelper.setContext(context);
            return true;
        }
        // hide old context menu
        else if (UIHelper.getContext() == context) {
            context.setVisible(false);
        }

        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.parent.isInsideScissors(mouseX, mouseY) && super.isMouseOver(mouseX, mouseY);
    }

    public void update(LocalAvatarFetcher.AvatarPath path, String filter) {
        this.avatar = path;
        this.filter = filter.toLowerCase(Locale.US);
    }

    public Component getName() {
        return Component.literal(avatar.getName());
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.button.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.button.setY(y);
    }

    public boolean filtered() {
        return this.getName().getString().toLowerCase(Locale.US).contains(filter.toLowerCase(Locale.US));
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible && filtered());
    }

    @Override
    public int compareTo(@NotNull AbstractAvatarWidget other) {
        // compare favourite
        if (this.favourite && !other.favourite)
            return -1;
        else if (other.favourite && !this.favourite)
            return 1;

        // compare types
        if (this instanceof AvatarFolderWidget && other instanceof AvatarWidget)
            return -1;
        else if (this instanceof AvatarWidget && other instanceof AvatarFolderWidget)
            return 1;

        // then compare names
        else return this.getName().getString().toLowerCase(Locale.US).compareTo(other.getName().getString().toLowerCase(Locale.US));
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof AbstractAvatarWidget other && other.avatar != null && this.avatar != null && this.avatar.getPath().equals(other.avatar.getPath());
    }

    public boolean isOf(Path path) {
        return this.avatar != null && this.avatar.getTheActualPathForThis().equals(path);
    }
}
