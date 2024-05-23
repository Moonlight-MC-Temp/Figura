package org.figuramc.figura.gui.widgets.lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.gui.screens.PermissionsScreen;
import org.figuramc.figura.gui.widgets.SearchBar;
import org.figuramc.figura.gui.widgets.SwitchButton;
import org.figuramc.figura.gui.widgets.permissions.AbstractPermPackElement;
import org.figuramc.figura.gui.widgets.permissions.CategoryPermPackElement;
import org.figuramc.figura.gui.widgets.permissions.PlayerPermPackElement;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.permissions.PermissionPack;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.*;

public class PlayerList extends AbstractList {

    private final HashMap<UUID, PlayerPermPackElement> players = new HashMap<>();
    private final HashSet<UUID> missingPlayers = new HashSet<>();

    private final ArrayList<AbstractPermPackElement> permissionsList = new ArrayList<>();

    public final PermissionsScreen parent;
    private final SearchBar searchBar;
    private final SwitchButton showFigura, showDisconnected;
    private static boolean showFiguraBl, showDisconnectedBl;
    private final int entryWidth;

    private int totalHeight = 0;
    private AbstractPermPackElement maxCategory;
    public AbstractPermPackElement selectedEntry;
    private String filter = "";

    public PlayerList(int x, int y, int width, int height, PermissionsScreen parent) {
        super(x, y, width, height);
        updateScissors(1, 24, -2, -25);

        this.parent = parent;
        this.entryWidth = Math.min(width - scrollBar.getWidth() - 12, 174);

        // fix scrollbar y and height
        scrollBar.setY(y + 28);
        scrollBar.setHeight(height - 32);

        // search bar
        children.add(searchBar = new SearchBar(x + 4, y + 4, width - 56, 20, s -> {
            if (!filter.equals(s))
                scrollBar.setScrollProgress(0f);
            filter = s;
        }));

        // show figura only button
        children.add(showFigura = new SwitchButton(x + width - 48, y + 4, 16, 16, 0, 0, 16, new FiguraIdentifier("textures/gui/button_social.png"), 96, 32, FiguraText.of("gui.permissions.figura_only.tooltip"), button -> showFiguraBl = ((SwitchButton) button).isToggled()));
        showFigura.setToggled(showFiguraBl);

        // show disconnected button
        children.add(showDisconnected = new SwitchButton(x + width - 24, y + 4, 16, 16, 48, 0, 16, new FiguraIdentifier("textures/gui/button_social.png"), 96, 32, FiguraText.of("gui.permissions.disconnected.tooltip"), button -> showDisconnectedBl = ((SwitchButton) button).isToggled()));
        showDisconnected.setToggled(showDisconnectedBl);

        // initial load
        loadGroups();
        loadPlayers();

        // select self
        selectLocalPlayer();
    }

    @Override
    public void tick() {
        // update players
        loadPlayers();
        super.tick();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        // background
        UIHelper.blitSliced(gui, x, y, width, height, UIHelper.OUTLINE_FILL);

        totalHeight = 0;
        for (AbstractPermPackElement pack : permissionsList) {
            if (pack.isVisible())
                totalHeight += pack.getHeight() + 8;
        }

        // scrollbar visible
        boolean hasScrollbar = totalHeight > height - 32;
        scrollBar.setVisible(hasScrollbar);
        scrollBar.setScrollRatio(permissionsList.isEmpty() ? 0f : (float) totalHeight / permissionsList.size(), totalHeight - (height - 32));

        // scissors
        this.scissorsWidth = hasScrollbar ? -scrollBar.getWidth() - 5 : -2;
        enableScissors(gui);

        // render stuff
        int xOffset = (width - entryWidth - (scrollBar.isVisible() ? 13 : 0)) / 2;
        int playerY = scrollBar.isVisible() ? (int) -(Mth.lerp(scrollBar.getScrollProgress(), -32, totalHeight - height)) : 32;

        int minY = y + scissorsY;
        int maxY = minY + height + scissorsHeight;
        for (AbstractPermPackElement pack : permissionsList) {
            if (!pack.isVisible())
                continue;

            pack.setX(x + xOffset);
            pack.setY(y + playerY);

            if (pack.getY() + pack.getHeight() > minY && pack.getY() < maxY)
                pack.render(gui, mouseX, mouseY, delta);

            playerY += pack.getHeight() + 8;
        }

        // reset scissor
        gui.disableScissor();

        // render children
        super.render(gui, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends GuiEventListener> contents() {
        return permissionsList;
    }

    private void loadGroups() {
        for (PermissionPack container : PermissionManager.CATEGORIES.values()) {
            CategoryPermPackElement group = new CategoryPermPackElement(entryWidth, container, this);
            permissionsList.add(group);
            children.add(group);
            maxCategory = group;
        }
    }

    private void loadPlayers() {
        // reset missing players
        missingPlayers.clear();
        missingPlayers.addAll(players.keySet());

        // for all players
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        List<UUID> playerList = connection == null ? List.of() : new ArrayList<>(connection.getOnlinePlayerIds());
        for (UUID uuid : playerList) {
            // get player
            PlayerInfo player = connection.getPlayerInfo(uuid);
            if (player == null)
                continue;

            // get player data
            String name = player.getProfile().getName();
            ResourceLocation skin = player.getSkinLocation();
            Avatar avatar = AvatarManager.getAvatarForPlayer(uuid);

            // filter check
            if ((!name.toLowerCase(Locale.US).contains(filter.toLowerCase(Locale.US)) && !uuid.toString().contains(filter.toLowerCase(Locale.US))) || (showFigura.isToggled() && !FiguraMod.isLocal(uuid) && (avatar == null || avatar.nbt == null)))
                continue;

            // player is not missing
            missingPlayers.remove(uuid);

            PlayerPermPackElement element = players.computeIfAbsent(uuid, uuid1 -> {
                PlayerPermPackElement entry = new PlayerPermPackElement(entryWidth, name, PermissionManager.get(uuid1), skin, uuid1, this);

                permissionsList.add(entry);
                children.add(entry);

                return entry;
            });
            element.disconnected = false;
        }

        if (filter.isEmpty() && showDisconnected.isToggled()) {
            for (Avatar avatar : AvatarManager.getLoadedAvatars()) {
                UUID id = avatar.owner;

                if (playerList.contains(id))
                    continue;

                missingPlayers.remove(id);

                PlayerPermPackElement element = players.computeIfAbsent(id, uuid -> {
                    PlayerPermPackElement entry = new PlayerPermPackElement(entryWidth, avatar.entityName, PermissionManager.get(uuid), null, uuid, this);

                    permissionsList.add(entry);
                    children.add(entry);

                    return entry;
                });
                element.disconnected = true;
            }
        }

        // remove missing players
        for (UUID missingID : missingPlayers) {
            PlayerPermPackElement entry = players.remove(missingID);
            permissionsList.remove(entry);
            children.remove(entry);
        }

        sortList();

        // select local if current selected is missing
        if (selectedEntry instanceof PlayerPermPackElement player && missingPlayers.contains(player.getOwner()))
            selectLocalPlayer();
    }

    private void sortList() {
        permissionsList.sort(AbstractPermPackElement::compareTo);
        children.sort((element1, element2) -> {
            if (element1 instanceof AbstractPermPackElement container1 && element2 instanceof AbstractPermPackElement container2)
                return container1.compareTo(container2);
            return 0;
        });
    }

    private void selectLocalPlayer() {
        PlayerPermPackElement local = Minecraft.getInstance().player != null ? players.get(Minecraft.getInstance().player.getUUID()) : null;
        if (local != null) {
            local.onPress();
        } else {
            maxCategory.onPress();
        }

        scrollToSelected();
    }

    public void updateScroll() {
        // store old scroll pos
        double pastScroll = (totalHeight - getHeight()) * scrollBar.getScrollProgress();

        // get new height
        totalHeight = 0;
        for (AbstractPermPackElement pack : permissionsList) {
            if (pack.isVisible())
                totalHeight += pack.getHeight() + 8;
        }

        // set new scroll percentage
        scrollBar.setScrollProgress(pastScroll / (totalHeight - getHeight()));
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        scrollBar.setY(y + 28);
        searchBar.setY(y + 4);
        showFigura.setY(y + 4);
        showDisconnected.setY(y + 4);
    }

    public int getCategoryAt(double y) {
        int ret = -1;
        for (AbstractPermPackElement element : permissionsList)
            if (element instanceof CategoryPermPackElement group && group.isVisible() && y >= group.getY())
                ret++;
        return Math.max(ret, 0);
    }

    public void scrollToSelected() {
        double y = 0;

        // get height
        totalHeight = 0;
        for (AbstractPermPackElement pack : permissionsList) {
            if (pack instanceof PlayerPermPackElement && !pack.isVisible())
                continue;

            if (pack == selectedEntry)
                y = totalHeight;
            else
                totalHeight += pack.getHeight() + 8;
        }

        // set scroll
        scrollBar.setScrollProgressNoAnim(y / totalHeight);
    }

}
