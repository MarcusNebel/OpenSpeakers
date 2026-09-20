package com.marcusnebel.openspeakers.contentpack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.marcusnebel.openspeakers.OpenSpeakers;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Liest die Manifeste der Contentpacks aus dem Ordner contentpacks/openspeakers ein, beim Start und bei jedem
 * Neuladen der Ressourcen (F3 + T). Ein Contentpack ist ein Ordner oder eine ZIP-Datei, die in einem Namensraum
 * eine Datei assets/&lt;namensraum&gt;/openspeakers.json enthält. Die Manifeste werden direkt aus den Packs
 * gelesen, unabhängig vom Ressourcen-Manager von Minecraft.
 */
@SideOnly(Side.CLIENT)
public class ContentPackManager implements IResourceManagerReloadListener {

    public static final String MANIFEST_FILE = "openspeakers.json";

    private static final Logger LOGGER = LogManager.getLogger(OpenSpeakers.MODID);

    private static volatile List<ContentPack> packs = Collections.emptyList();

    /** Die Packs aus dem Ordner contentpacks/openspeakers. */
    private final List<IResourcePack> sourcePacks;

    public ContentPackManager(List<IResourcePack> sourcePacks) {
        this.sourcePacks = new ArrayList<>(sourcePacks);
    }

    /** Alle aktuell geladenen Contentpacks, nach Name sortiert. */
    public static List<ContentPack> getPacks() {
        return packs;
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        reload();
    }

    private void reload() {
        List<ContentPack> found = new ArrayList<>();

        for (IResourcePack source : sourcePacks) {
            int before = found.size();
            for (String domain : source.getResourceDomains()) {
                ContentPack pack = loadPack(source, domain);
                if (pack != null) {
                    found.add(pack);
                }
            }
            if (found.size() == before) {
                LOGGER.warn("'{}' enthält kein gültiges Contentpack. Erwartet wird assets/<namensraum>/{} direkt im Pack "
                        + "(gefundene Namensräume: {}). Liegt der Ordner assets direkt im Pack und nicht in einem "
                        + "weiteren Unterordner?", source.getPackName(), MANIFEST_FILE, source.getResourceDomains());
            }
        }

        found.sort(Comparator.comparing(p -> p.getName().toLowerCase(Locale.ROOT)));
        packs = Collections.unmodifiableList(found);

        List<String> names = new ArrayList<>();
        for (ContentPack pack : found) {
            names.add(pack.getName() + " (" + pack.getAnnouncements().size() + " Ansagen)");
        }
        LOGGER.info("{} Contentpack(s) geladen: {}", found.size(), names);
    }

    private static ContentPack loadPack(IResourcePack source, String domain) {
        ResourceLocation manifest = new ResourceLocation(domain, MANIFEST_FILE);
        if (!source.resourceExists(manifest)) {
            return null;
        }

        try (Reader reader = new InputStreamReader(source.getInputStream(manifest), StandardCharsets.UTF_8)) {
            return parse(domain, new JsonParser().parse(reader).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Contentpack '{}' konnte nicht gelesen werden: {}", domain, e.toString());
            return null;
        }
    }

    private static ContentPack parse(String domain, JsonObject root) {
        String packName = optString(root, "name", domain);

        List<ContentPack.Announcement> announcements = new ArrayList<>();
        if (root.has("announcements") && root.get("announcements").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("announcements")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject entry = element.getAsJsonObject();
                String sound = optString(entry, "sound", "");
                if (sound.isEmpty()) {
                    LOGGER.warn("Contentpack '{}': Ansage ohne \"sound\" übersprungen", domain);
                    continue;
                }
                String id = optString(entry, "id", sound);
                String name = optString(entry, "name", id);
                String soundName = (sound.contains(":") ? sound : domain + ":" + sound).toLowerCase(Locale.ROOT);
                announcements.add(new ContentPack.Announcement(id, name, soundName));
            }
        }

        if (announcements.isEmpty()) {
            LOGGER.warn("Contentpack '{}' enthält keine gültigen Ansagen und wird ignoriert", domain);
            return null;
        }
        return new ContentPack(domain, packName, announcements);
    }

    private static String optString(JsonObject object, String key, String fallback) {
        if (object.has(key) && object.get(key).isJsonPrimitive()) {
            String value = object.get(key).getAsString().trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return fallback;
    }
}
