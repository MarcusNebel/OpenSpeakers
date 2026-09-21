package com.marcusnebel.openspeakers.client.gui;

import com.marcusnebel.openspeakers.ChatUtil;
import com.marcusnebel.openspeakers.OpenSpeakers;
import com.marcusnebel.openspeakers.contentpack.ContentPack;
import com.marcusnebel.openspeakers.contentpack.ContentPackManager;
import com.marcusnebel.openspeakers.network.MessageSetAnnouncement;
import com.marcusnebel.openspeakers.network.NetworkHandler;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI des Ansagen-Blocks im grauen Vanilla-Stil: Text-Tabs oben und eine Kachel (Vanilla-Button) pro Eintrag.
 * Tab "Linking": verlinkte Lautsprecher. Tab "Announcements": Contentpacks und deren Ansagen.
 */
@SideOnly(Side.CLIENT)
public class GuiEmitter extends GuiScreen {

    /** Vanilla-Grafik der Buttons (dieselbe, die auch GuiButton verwendet). */
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");

    private static final int PANEL_MAX_WIDTH = 320;
    private static final int PANEL_MAX_HEIGHT = 220;
    private static final int MARGIN = 8;

    private static final int BUTTON_HEIGHT = 20;

    // Positionen relativ zur oberen linken Ecke des Panels
    private static final int TAB_TOP = 22;
    private static final int TAB_GAP = 4;
    private static final int TAB_MIN_WIDTH = 60;

    private static final int CONTENT_TOP = 50;
    private static final int LIST_TOP = 64;
    private static final int TILE_GAP = 4;
    private static final int SCROLLBAR_WIDTH = 8;

    private static final int COLOR_TEXT = 0xFF404040;
    private static final int COLOR_TEXT_LIGHT = 0xFF606060;

    private enum Tab {
        LINKING("gui.openspeakers.tab.linking"),
        ANNOUNCEMENTS("gui.openspeakers.tab.announcements");

        final String titleKey;

        Tab(String titleKey) {
            this.titleKey = titleKey;
        }
    }

    /** Eine Kachel in der Liste. Ohne Aktion reagiert sie nicht auf Klicks. */
    private static class Entry {
        final String label;
        final boolean enabled;
        final Runnable action;

        Entry(String label, boolean enabled, Runnable action) {
            this.label = label;
            this.enabled = enabled;
            this.action = action;
        }
    }

    private final BlockPos emitterPos;
    private final List<BlockPos> speakers;
    private final List<ContentPack> packs;

    private String selectedSound;
    private String selectedLabel;

    private Tab currentTab = Tab.LINKING;
    /** Aktuell geöffnetes Pack im Tab "Announcements", null = Liste aller Packs. */
    private ContentPack currentPack = null;

    private int linkingScroll = 0;
    private int packScroll = 0;
    private int announcementScroll = 0;
    private boolean draggingScrollbar = false;

    private int guiLeft;
    private int guiTop;
    private int panelWidth;
    private int panelHeight;
    private int tabsLeft;
    private int[] tabWidths = new int[0];
    private int visibleRows = 1;
    private int tileWidth;

    public GuiEmitter(BlockPos emitterPos, List<BlockPos> speakers, String selectedSound, String selectedLabel) {
        this.emitterPos = emitterPos;
        this.speakers = new ArrayList<>(speakers);
        this.packs = ContentPackManager.getPacks();
        this.selectedSound = selectedSound;
        this.selectedLabel = selectedLabel;
    }

    @Override
    public void initGui() {
        super.initGui();
        panelWidth = Math.min(PANEL_MAX_WIDTH, width - 16);
        panelHeight = Math.min(PANEL_MAX_HEIGHT, height - 16);
        guiLeft = (width - panelWidth) / 2;
        guiTop = (height - panelHeight) / 2;

        // Tabs: Breite passt sich dem Text an, alle zusammen sitzen mittig im Panel
        Tab[] tabs = Tab.values();
        tabWidths = new int[tabs.length];
        int total = 0;
        for (int i = 0; i < tabs.length; i++) {
            tabWidths[i] = Math.max(TAB_MIN_WIDTH, fontRenderer.getStringWidth(I18n.format(tabs[i].titleKey)) + 16);
            total += tabWidths[i] + (i > 0 ? TAB_GAP : 0);
        }
        tabsLeft = guiLeft + (panelWidth - total) / 2;

        // Wie viele Kacheln passen untereinander in das Panel?
        int available = panelHeight - LIST_TOP - MARGIN;
        visibleRows = Math.max(1, (available + TILE_GAP) / (BUTTON_HEIGHT + TILE_GAP));
        tileWidth = panelWidth - 2 * MARGIN - SCROLLBAR_WIDTH - 4;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ---------- Inhalt ----------

    /** Baut die Kacheln des aktuell sichtbaren Tabs. */
    private List<Entry> buildEntries() {
        List<Entry> entries = new ArrayList<>();

        if (currentTab == Tab.LINKING) {
            for (int i = 0; i < speakers.size(); i++) {
                BlockPos p = speakers.get(i);
                long distance = Math.round(Math.sqrt(emitterPos.distanceSq(p)));
                entries.add(new Entry(
                        I18n.format("gui.openspeakers.linking.entry", i + 1, ChatUtil.fmt(p), distance), true, null));
            }
        } else if (currentPack == null) {
            entries.add(new Entry(OpenSpeakers.DEFAULT_SOUND_LABEL,
                    !OpenSpeakers.DEFAULT_SOUND_NAME.equals(selectedSound), this::selectDefault));
            for (ContentPack pack : packs) {
                entries.add(new Entry(
                        I18n.format("gui.openspeakers.announcements.pack", pack.getName(), pack.getAnnouncements().size()),
                        true, () -> openPack(pack)));
            }
        } else {
            ContentPack pack = currentPack;
            entries.add(new Entry(I18n.format("gui.openspeakers.announcements.back"), true, this::closePack));
            for (ContentPack.Announcement announcement : pack.getAnnouncements()) {
                // Die aktuell gewählte Ansage erscheint als deaktivierte Kachel
                boolean selected = announcement.getSoundName().equals(selectedSound);
                entries.add(new Entry(announcement.getName(), !selected, () -> select(pack, announcement)));
            }
        }
        return entries;
    }

    /** Gibt es im aktuellen Tab eine Kachelliste (sonst wird nur ein Hinweistext gezeigt)? */
    private boolean hasList() {
        if (currentTab == Tab.LINKING) {
            return !speakers.isEmpty();
        }
        return true;
    }

    private void openPack(ContentPack pack) {
        currentPack = pack;
        announcementScroll = 0;
        draggingScrollbar = false;
    }

    private void closePack() {
        currentPack = null;
        draggingScrollbar = false;
    }

    private void select(ContentPack pack, ContentPack.Announcement announcement) {
        selectedSound = announcement.getSoundName();
        selectedLabel = pack.getName() + ": " + announcement.getName();
        NetworkHandler.CHANNEL.sendToServer(new MessageSetAnnouncement(emitterPos, selectedSound, selectedLabel));
    }

    private void selectDefault() {
        selectedSound = OpenSpeakers.DEFAULT_SOUND_NAME;
        selectedLabel = OpenSpeakers.DEFAULT_SOUND_LABEL;
        NetworkHandler.CHANNEL.sendToServer(new MessageSetAnnouncement(emitterPos, selectedSound, selectedLabel));
    }

    // ---------- Scrollen ----------

    private int getScroll() {
        if (currentTab == Tab.LINKING) {
            return linkingScroll;
        }
        return currentPack == null ? packScroll : announcementScroll;
    }

    private void setScroll(int value) {
        if (currentTab == Tab.LINKING) {
            linkingScroll = value;
        } else if (currentPack == null) {
            packScroll = value;
        } else {
            announcementScroll = value;
        }
    }

    private void clampScroll(int entryCount) {
        int maxScroll = Math.max(0, entryCount - visibleRows);
        setScroll(Math.max(0, Math.min(getScroll(), maxScroll)));
    }

    private int listHeight() {
        return visibleRows * (BUTTON_HEIGHT + TILE_GAP) - TILE_GAP;
    }

    private int scrollbarX() {
        return guiLeft + panelWidth - MARGIN - SCROLLBAR_WIDTH;
    }

    private int thumbHeight(int entryCount) {
        return Math.max(12, listHeight() * visibleRows / entryCount);
    }

    private void updateScrollFromMouse(int mouseY) {
        int entryCount = buildEntries().size();
        int maxScroll = entryCount - visibleRows;
        if (maxScroll <= 0) {
            return;
        }
        int thumbHeight = thumbHeight(entryCount);
        float fraction = (mouseY - (guiTop + LIST_TOP) - thumbHeight / 2.0F) / (listHeight() - thumbHeight);
        setScroll(Math.round(fraction * maxScroll));
        clampScroll(entryCount);
    }

    // ---------- Zeichnen ----------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawPanel(guiLeft, guiTop, panelWidth, panelHeight);

        fontRenderer.drawString(I18n.format("gui.openspeakers.title", ChatUtil.fmt(emitterPos)),
                guiLeft + MARGIN, guiTop + 8, COLOR_TEXT);

        drawTabs(mouseX, mouseY);
        drawHeader();
        drawContent(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** Grauer Vanilla-Hintergrund: schwarzer Rand, Licht oben/links, Schatten unten/rechts. */
    private void drawPanel(int x, int y, int w, int h) {
        drawRect(x, y, x + w, y + h, 0xFF000000);
        drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFFFFFFF);
        drawRect(x + 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
        drawRect(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFC6C6C6);
    }

    /** Vertiefte Fläche (wie ein Inventar-Slot): dunkel oben/links, hell unten/rechts. */
    private void drawInset(int x1, int y1, int x2, int y2) {
        drawRect(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xFF373737);
        drawRect(x1, y1, x2 + 1, y2 + 1, 0xFFFFFFFF);
        drawRect(x1, y1, x2, y2, 0xFF8B8B8B);
    }

    /**
     * Zeichnet einen Button genau wie GuiButton: Grafik aus widgets.png, weißer Text,
     * gelb beim Überfahren, grau wenn deaktiviert.
     */
    private void drawButton(int x, int y, int w, boolean enabled, boolean hover, String text) {
        mc.getTextureManager().bindTexture(WIDGETS);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        int state = !enabled ? 0 : (hover ? 2 : 1);
        int left = w / 2;
        int right = w - left;
        drawTexturedModalRect(x, y, 0, 46 + state * 20, left, BUTTON_HEIGHT);
        drawTexturedModalRect(x + left, y, 200 - right, 46 + state * 20, right, BUTTON_HEIGHT);

        int color = !enabled ? 0xFFA0A0A0 : (hover ? 0xFFFFFFA0 : 0xFFE0E0E0);
        drawCenteredString(fontRenderer, text, x + w / 2, y + (BUTTON_HEIGHT - 8) / 2, color);
    }

    private int tabX(int index) {
        int x = tabsLeft;
        for (int i = 0; i < index; i++) {
            x += tabWidths[i] + TAB_GAP;
        }
        return x;
    }

    private int tabY() {
        return guiTop + TAB_TOP;
    }

    private void drawTabs(int mouseX, int mouseY) {
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            boolean active = tabs[i] == currentTab;
            boolean hover = isInside(mouseX, mouseY, tabX(i), tabY(), tabWidths[i], BUTTON_HEIGHT);
            // Der aktive Tab erscheint wie in Vanilla als "gedrückter", deaktivierter Button
            drawButton(tabX(i), tabY(), tabWidths[i], !active, hover, I18n.format(tabs[i].titleKey));
        }
    }

    /** Überschrift unter den Tabs. */
    private void drawHeader() {
        String header;
        if (currentTab == Tab.LINKING) {
            header = I18n.format("gui.openspeakers.linking.title", speakers.size());
        } else if (selectedSound.isEmpty()) {
            header = I18n.format("gui.openspeakers.announcements.none");
        } else {
            header = I18n.format("gui.openspeakers.announcements.selected",
                    selectedLabel.isEmpty() ? selectedSound : selectedLabel);
        }
        fontRenderer.drawString(fontRenderer.trimStringToWidth(header, panelWidth - 2 * MARGIN),
                guiLeft + MARGIN, guiTop + CONTENT_TOP, COLOR_TEXT_LIGHT);
    }

    private void drawContent(int mouseX, int mouseY) {
        if (!hasList()) {
            if (currentTab == Tab.LINKING) {
                drawCenteredString(fontRenderer, I18n.format("gui.openspeakers.linking.empty"),
                        guiLeft + panelWidth / 2, guiTop + LIST_TOP + 6, COLOR_TEXT_LIGHT);
            } else {
                List<String> lines = fontRenderer.listFormattedStringToWidth(
                        I18n.format("gui.openspeakers.announcements.nopacks"), panelWidth - 2 * MARGIN);
                for (int i = 0; i < lines.size(); i++) {
                    fontRenderer.drawString(lines.get(i), guiLeft + MARGIN, guiTop + LIST_TOP + 2 + i * 12,
                            COLOR_TEXT_LIGHT);
                }
            }
            return;
        }

        List<Entry> entries = buildEntries();
        clampScroll(entries.size());

        int left = guiLeft + MARGIN;
        for (int row = 0; row < visibleRows; row++) {
            int index = getScroll() + row;
            if (index >= entries.size()) {
                break;
            }
            Entry entry = entries.get(index);
            int y = guiTop + LIST_TOP + row * (BUTTON_HEIGHT + TILE_GAP);
            boolean hover = isInside(mouseX, mouseY, left, y, tileWidth, BUTTON_HEIGHT);
            String text = fontRenderer.trimStringToWidth(entry.label, tileWidth - 8);
            drawButton(left, y, tileWidth, entry.enabled, hover, text);
        }

        if (entries.size() > visibleRows) {
            drawScrollbar(entries.size());
        }
    }

    private void drawScrollbar(int entryCount) {
        int x = scrollbarX();
        int top = guiTop + LIST_TOP;
        int maxScroll = entryCount - visibleRows;
        int thumbHeight = thumbHeight(entryCount);
        int thumbTop = top + (listHeight() - thumbHeight) * getScroll() / maxScroll;

        drawInset(x, top, x + SCROLLBAR_WIDTH, top + listHeight());

        // Griff im Vanilla-Stil: hell oben/links, dunkel unten/rechts
        drawRect(x + 1, thumbTop, x + SCROLLBAR_WIDTH - 1, thumbTop + thumbHeight, 0xFF555555);
        drawRect(x + 1, thumbTop, x + SCROLLBAR_WIDTH - 2, thumbTop + thumbHeight - 1, 0xFFFFFFFF);
        drawRect(x + 2, thumbTop + 1, x + SCROLLBAR_WIDTH - 2, thumbTop + thumbHeight - 1, 0xFFC6C6C6);
    }

    // ---------- Eingaben ----------

    private void playClickSound() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }

        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            if (isInside(mouseX, mouseY, tabX(i), tabY(), tabWidths[i], BUTTON_HEIGHT)) {
                if (currentTab != tabs[i]) {
                    currentTab = tabs[i];
                    draggingScrollbar = false;
                    playClickSound();
                }
                return;
            }
        }

        if (!hasList()) {
            return;
        }
        List<Entry> entries = buildEntries();

        int left = guiLeft + MARGIN;
        for (int row = 0; row < visibleRows; row++) {
            int index = getScroll() + row;
            if (index >= entries.size()) {
                break;
            }
            int y = guiTop + LIST_TOP + row * (BUTTON_HEIGHT + TILE_GAP);
            if (isInside(mouseX, mouseY, left, y, tileWidth, BUTTON_HEIGHT)) {
                Entry entry = entries.get(index);
                if (entry.action != null && entry.enabled) {
                    playClickSound();
                    entry.action.run();
                }
                return;
            }
        }

        if (entries.size() > visibleRows
                && isInside(mouseX, mouseY, scrollbarX() - 2, guiTop + LIST_TOP, SCROLLBAR_WIDTH + 4, listHeight())) {
            draggingScrollbar = true;
            updateScrollFromMouse(mouseY);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (draggingScrollbar) {
            updateScrollFromMouse(mouseY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            draggingScrollbar = false;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && hasList()) {
            setScroll(getScroll() + (wheel > 0 ? -1 : 1));
            clampScroll(buildEntries().size());
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        // wie bei Vanilla-Containern schließt auch die Inventar-Taste (E) die GUI
        if (mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
            mc.displayGuiScreen(null);
        }
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
