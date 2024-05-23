package org.figuramc.figura.gui.widgets.lists;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.util.Mth;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.gui.screens.AbstractPanelScreen;
import org.figuramc.figura.gui.screens.AvatarWizardScreen;
import org.figuramc.figura.gui.widgets.Button;
import org.figuramc.figura.gui.widgets.SearchBar;
import org.figuramc.figura.gui.widgets.avatar.AbstractAvatarWidget;
import org.figuramc.figura.gui.widgets.avatar.AvatarFolderWidget;
import org.figuramc.figura.gui.widgets.avatar.AvatarWidget;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.ui.UIHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class AvatarList extends AbstractList {

    // -- Variables -- // 
    private final HashMap<Path, AbstractAvatarWidget> avatars = new HashMap<>();
    private final ArrayList<AbstractAvatarWidget> avatarList = new ArrayList<>();

    private int totalHeight = 0;
    private String filter = "";

    public static Path selectedEntry;

    // -- Constructors -- // 

    public AvatarList(int x, int y, int width, int height, AbstractPanelScreen parentScreen) {
        super(x, y, width, height);

        // search bar
        children.add(new SearchBar(x + 4, y + 4, width - 8, 20, s -> {
            if (!filter.equals(s))
                scrollBar.setScrollProgress(0f);
            filter = s;
        }));

        // new avatar
        children.add(new Button(
                x + width / 2 - 46, y + 28,
                16, 16, 0, 32, 16,
                new FiguraIdentifier("textures/gui/button_avatar_management.png"),
                144, 48,
                FiguraText.of("gui.wardrobe.new_avatar.tooltip"),
                button -> Minecraft.getInstance().setScreen(new AvatarWizardScreen(parentScreen)))
        );

        // unselect
        children.add(new Button(
                x + width / 2 - 10, y + 28,
                16, 16, 0, 16, 16,
                new FiguraIdentifier("textures/gui/button_avatar_management.png"),
                144, 48,
                FiguraText.of("gui.wardrobe.unselect.tooltip"),
                button -> {
                    AvatarManager.loadLocalAvatar(null);
                    selectedEntry = null;
                })
        );

        // root folder
        children.add(new Button(
                x + width / 2 + 26, y + 28,
                16, 16, 0, 0, 16,
                new FiguraIdentifier("textures/gui/button_avatar_management.png"),
                144, 48,
                FiguraText.of("gui.wardrobe.folder.tooltip"),
                button -> Util.getPlatform().openUri(LocalAvatarFetcher.getLocalAvatarDirectory().toUri()))
        );

        // scrollbar
        this.scrollBar.setY(y + 48);
        this.scrollBar.setHeight(height - 52);

        // scissors
        this.updateScissors(1, 49, -2, -50);

        // initial load
        loadContents();
        scrollToSelected();
    }

    // -- Functions -- // 
    @Override
    public void tick() {
        // update list
        loadContents();
        super.tick();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        // background and scissors
        UIHelper.blitSliced(gui, x, y, width, height, UIHelper.OUTLINE_FILL);
        enableScissors(gui);

        // scrollbar
        totalHeight = 2;
        for (AbstractAvatarWidget avatar : avatarList)
            totalHeight += avatar.getHeight() + 2;
        int entryHeight = avatarList.isEmpty() ? 0 : totalHeight / avatarList.size();

        scrollBar.setVisible(totalHeight > height - 49);
        scrollBar.setScrollRatio(entryHeight, totalHeight - (height - 49));

        // render list
        int xOffset = scrollBar.isVisible() ? 4 : 11;
        int yOffset = scrollBar.isVisible() ? (int) -(Mth.lerp(scrollBar.getScrollProgress(), -49, totalHeight - height)) : 49;
        boolean hidden = false;

        for (AbstractAvatarWidget avatar : avatarList) {
            if (hidden) continue;

            avatar.setX(x + xOffset);
            avatar.setY(y + yOffset);

            if (avatar.getY() + avatar.getHeight() > y + scissorsY)
                avatar.render(gui, mouseX, mouseY, delta);

            yOffset += avatar.getHeight() + 2;
            if (yOffset > height)
                hidden = true;
        }

        // reset scissor
        gui.disableScissor();

        // render children
        super.render(gui, mouseX, mouseY, delta);

        // loading badge
        if (!LocalAvatarFetcher.isLoaded())
            UIHelper.renderLoading(gui, x + width / 2, y + height / 2);
    }

    private void loadContents() {
        LocalAvatarFetcher.reloadAvatars();

        // Load avatars // 
        HashSet<Path> missingPaths = new HashSet<>(avatars.keySet());
        for (LocalAvatarFetcher.AvatarPath avatar : List.copyOf(LocalAvatarFetcher.ALL_AVATARS)) {
            Path path = avatar.getTheActualPathForThis();

            // filter
            if (!avatar.search(filter))
                continue;

            // update current
            AbstractAvatarWidget widget = avatars.get(path);
            if (widget != null)
                widget.update(avatar, filter);

            // do not remove if passed
            missingPaths.remove(path);

            // add to the avatar list
            avatars.computeIfAbsent(path, p -> {
                int width = this.getWidth() - 22;
                AbstractAvatarWidget entry = avatar instanceof LocalAvatarFetcher.FolderPath folder ? new AvatarFolderWidget(0, width, folder, this) : new AvatarWidget(0, width, avatar, this);

                avatarList.add(entry);
                children.add(entry);

                return entry;
            });
        }

        // remove missing avatars
        for (Path missingPath : missingPaths) {
            AbstractAvatarWidget obj = avatars.remove(missingPath);
            avatarList.remove(obj);
            children.remove(obj);
        }

        // sort lists
        avatarList.sort(AbstractAvatarWidget::compareTo);
        children.sort((children1, children2) -> {
            if (children1 instanceof AbstractAvatarWidget avatar1 && children2 instanceof AbstractAvatarWidget avatar2)
                return avatar1.compareTo(avatar2);
            return 0;
        });
    }

    public void updateScroll() {
        // store old scroll pos
        double pastScroll = (totalHeight - getHeight()) * scrollBar.getScrollProgress();

        // get new height
        totalHeight = 2;
        for (AbstractAvatarWidget avatar : avatarList)
            totalHeight += avatar.getHeight() + 2;

        // set new scroll percentage
        scrollBar.setScrollProgress(pastScroll / (totalHeight - getHeight()));
    }

    public void scrollToSelected() {
        double y = 0;

        // get height
        totalHeight = 0;
        for (AbstractAvatarWidget avatar : avatarList) {
            if (avatar.isOf(selectedEntry))
                y = totalHeight;
            else
                totalHeight += avatar.getHeight() + 2;
        }

        // set scroll
        scrollBar.setScrollProgressNoAnim(y / totalHeight);
    }

    @Override
    public List<? extends GuiEventListener> contents() {
        return avatarList;
    }
}
