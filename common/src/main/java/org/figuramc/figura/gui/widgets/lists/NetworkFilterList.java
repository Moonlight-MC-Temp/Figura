package org.figuramc.figura.gui.widgets.lists;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.config.ConfigType;
import org.figuramc.figura.gui.widgets.*;
import org.figuramc.figura.lua.api.net.NetworkingAPI;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.ArrayList;
import java.util.List;

public class NetworkFilterList extends AbstractList {
    private static final int LIST_ELEMENT_Y_GAP = 4;

    private final ConfigType.NetworkFilterConfig config;
    private final List<NetworkFilterEntry> entries = new ArrayList<>();
    private final Button addEntryButton;
    private final TextField searchTextField;
    private int scroll, listElementsHeightDifference;
    public NetworkFilterList(int x, int y, int width, int height, ConfigType.NetworkFilterConfig config) {
        super(x, y, width, height);
        scrollBar.setVisible(true);
        scrollBar.setHeight(height - 56);
        scrollBar.setY(y+28);
        scrollBar.setAction(this::onScroll);
        this.config = config;
        children.add(addEntryButton = new Button(x + 4,y + height - 24, width - 8, 20, FiguraText.of("gui.network_filter.list.add_filter_entry"), null, this::onEntryAddClick));
        for (NetworkingAPI.Filter filter :
                config.getFilters()) {
            NetworkFilterEntry entry;
            entries.add(entry = new NetworkFilterEntry(this, x,y, width - 8 - scrollBar.getWidth(), 20, filter));
            children.add(entry);
        }
        children.add(searchTextField = new TextField(x + 4, y + 4, width - 8, 20, TextField.HintType.SEARCH, this::onSearch));
        repositionContents();
        updateContentsHeightDiff();
        updateScrollBar();
    }

    private void onSearch(final String s) {
        if (s.isBlank() || s.isEmpty()) entries.forEach(e -> e.setVisible(true));
        else entries.forEach(e -> e.setVisible(e.getSourceFilter().getSource().contains(s)));
        repositionContents();
        updateContentsHeightDiff();
        updateScrollBar();
    }

    private void onScroll(ScrollBarWidget scrollBarWidget) {
        scroll = (int) (scrollBarWidget.getScrollProgress() * listElementsHeightDifference);
        repositionContents();
    }

    private void onDelete(NetworkFilterEntry networkFilterEntry) {
        entries.remove(networkFilterEntry);
        children.remove(networkFilterEntry);
        config.getFilters().remove(networkFilterEntry.getSourceFilter());
        repositionContents();
        updateContentsHeightDiff();
        updateScrollBar();
    }

    private void onEntryAddClick(net.minecraft.client.gui.components.Button button) {
        NetworkFilterEntry entry = new NetworkFilterEntry(this, getX() + 4, entries.size() * 24,  getWidth() - 22,20);
        entries.add(entry);
        children.add(entry);
        config.getFilters().add(entry.getSourceFilter());
        repositionContents();
        updateContentsHeightDiff();
        updateScrollBar();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        // background and scissors
        UIHelper.blitSliced(gui, x, y, width, height, UIHelper.OUTLINE_FILL);

        super.render(gui, mouseX, mouseY, delta);

        updateScissors(4,28, -18, -56);
        enableScissors(gui);
        for (NetworkFilterEntry entry :
                contents()) {
            if (!entry.isVisible()) continue;
            entry.render(gui, mouseX, mouseY, delta);
        }
        gui.disableScissor();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        repositionChildren();
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        repositionChildren();
        updateContentsHeightDiff();
        updateScrollBar();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        repositionChildren();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        repositionChildren();
    }

    private void updateScrollBar() {
        if (!entries.isEmpty() && listElementsHeightDifference > 0) {
            scrollBar.setVisible(true);
            scrollBar.setScrollProgress(Math.min(1, (double)scroll / listElementsHeightDifference));
            scrollBar.setScrollRatio(20, listElementsHeightDifference);
        }
        else {
            scrollBar.setVisible(false);
        }
    }

    private void repositionChildren() {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        scrollBar.setX(x + width - 14);
        scrollBar.setY(y + 28);
        scrollBar.setHeight(height - 56);

        addEntryButton.setX(x + 4);
        addEntryButton.setY(y + height - 24);
        addEntryButton.setWidth(width - 8);

        searchTextField.setX(x + 4);
        searchTextField.setY(y + 4);
        searchTextField.setWidth(width - 8);

        repositionContents();
    }

    private void updateContentsHeightDiff() {
        int listElementsTotalHeight = 0;
        for (NetworkFilterEntry entry :
                entries) {
            if (entry.isVisible()) listElementsTotalHeight += 24;
        }
        if (listElementsTotalHeight > 0) listElementsTotalHeight -= 4;
        listElementsHeightDifference = Math.max(0, listElementsTotalHeight - (getHeight() - 60));
    }

    private void repositionContents() {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int yOffset = 28 - scroll;
        for (NetworkFilterEntry entry :
                contents()) {
            if (!entry.isVisible()) continue;
            entry.setX(x + 4);
            entry.setY(y + yOffset);
            entry.setWidth(width - 22);
            yOffset += 20 + LIST_ELEMENT_Y_GAP;
        }
    }

    @Override
    public List<NetworkFilterEntry> contents() {
        return entries;
    }

    public static class NetworkFilterEntry extends AbstractContainerElement {
        private static final ResourceLocation deleteButtonLocation = new ResourceLocation("figura", "textures/gui/delete.png");
        private final NetworkingAPI.Filter sourceFilter;
        private final IconButton deleteButton;
        private final TextField filterTextField;
        private final NetworkFilterList parent;
        public NetworkFilterEntry(NetworkFilterList parent, int x, int y, int width, int height) {
            this(parent, x, y, width, height, new NetworkingAPI.Filter("example.com"));
        }

        public NetworkFilterEntry(NetworkFilterList parent, int x, int y, int width, int height, NetworkingAPI.Filter sourceFilter) {
            super(x, y, width, height);
            this.parent = parent;
            this.sourceFilter = sourceFilter;
            children.add(filterTextField = new TextField(x, y, width - 118, 20, TextField.HintType.IP, this::onSourceChange));
            children.add(
                    deleteButton = new IconButton(x + width - 20, y, 20, 20,
                            0,0, 24,
                            deleteButtonLocation,72, 24,
                            FiguraText.of("gui.network_filter.list.delete"), null, this::onDelete)
            );
            filterTextField.getField().setValue(sourceFilter.getSource());
        }

        private void onDelete(net.minecraft.client.gui.components.Button button) {
            parent.onDelete(this);
        }

        @Override
        public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
            super.render(gui, mouseX, mouseY, delta);
        }

        @Override
        public int getHeight() {
            return 20;
        }

        private void onSourceChange(String s) {
            sourceFilter.setSource(s);
        }

        private void repositionChildren() {
            int x = getX(), y = getY(), width = getWidth();
            filterTextField.setX(x);
            filterTextField.setY(y);
            filterTextField.setWidth(width - 118);
            deleteButton.setX(x+width-20);
            deleteButton.setY(y);
        }

        @Override
        public void setX(int x) {
            super.setX(x);
            repositionChildren();
        }

        @Override
        public void setY(int y) {
            super.setY(y);
            repositionChildren();
        }

        @Override
        public void setWidth(int width) {
            super.setWidth(width);
            repositionChildren();
        }

        @Override
        public void setHeight(int height) {
            super.setHeight(height);
            repositionChildren();
        }

        public NetworkingAPI.Filter getSourceFilter() {
            return sourceFilter;
        }
    }
}
