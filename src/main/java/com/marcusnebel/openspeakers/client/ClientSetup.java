package com.marcusnebel.openspeakers.client;

import com.marcusnebel.openspeakers.OpenSpeakers;
import com.marcusnebel.openspeakers.contentpack.ContentPackManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Client-only Initialisierung. */
@SideOnly(Side.CLIENT)
public class ClientSetup {

    private static final Logger LOGGER = LogManager.getLogger(OpenSpeakers.MODID);

    /** Contentpacks liegen in: <Minecraft-Ordner>/contentpacks/openspeakers */
    private static final String PARENT_FOLDER = "contentpacks";
    private static final String SUB_FOLDER = "openspeakers";

    public static void init() {
        Minecraft mc = Minecraft.getMinecraft();

        File folder = new File(new File(mc.mcDataDir, PARENT_FOLDER), SUB_FOLDER);
        if (!folder.isDirectory() && !folder.mkdirs()) {
            LOGGER.warn("Ordner {} konnte nicht angelegt werden", folder.getAbsolutePath());
        }
        LOGGER.info("Contentpack-Ordner: {}", folder.getAbsolutePath());

        List<IResourcePack> packs = registerPacks(mc, folder);

        // Der Manager liest die Manifeste der Packs beim Start und bei jedem Neuladen der Ressourcen (F3 + T)
        ((IReloadableResourceManager) mc.getResourceManager())
                .registerReloadListener(new ContentPackManager(packs));
    }

    /**
     * Hängt jeden Ordner und jede ZIP-Datei aus dem Contentpack-Ordner als Resourcepack ein, damit Minecraft
     * ihre sounds.json und Sounds kennt. Dafür sind zwei Schritte nötig:
     * 1. In die Liste der Standard-Packs, damit die Packs bei jedem späteren Neuladen der Ressourcen (F3 + T)
     *    dabei sind.
     * 2. Direkt in den Ressourcen-Manager, weil Minecraft die Ressourcen schon vor preInit einmal geladen hat.
     *    Ohne diesen Schritt kennt der Sound-Handler die Sounds der Packs nicht ("Unable to play unknown
     *    soundEvent").
     * @return die eingehängten Packs
     */
    private static List<IResourcePack> registerPacks(Minecraft mc, File folder) {
        List<IResourcePack> registered = new ArrayList<>();

        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            LOGGER.info("Der Contentpack-Ordner ist leer");
            return registered;
        }
        Arrays.sort(files);

        List<IResourcePack> defaultPacks;
        try {
            // Minecraft.defaultResourcePacks (private): Packs, die immer geladen werden, wie die Ressourcen von Mods
            defaultPacks = ReflectionHelper.getPrivateValue(Minecraft.class, mc, "defaultResourcePacks", "field_110449_ao");
        } catch (RuntimeException e) {
            LOGGER.error("Contentpacks konnten nicht eingehängt werden (Zugriff auf defaultResourcePacks fehlgeschlagen)", e);
            return registered;
        }

        IResourceManager resourceManager = mc.getResourceManager();
        SimpleReloadableResourceManager directManager = resourceManager instanceof SimpleReloadableResourceManager
                ? (SimpleReloadableResourceManager) resourceManager : null;
        if (directManager == null) {
            LOGGER.warn("Der Ressourcen-Manager ist unerwartet vom Typ {}, Sounds werden eventuell erst nach F3 + T bekannt",
                    resourceManager.getClass().getName());
        }

        for (File file : files) {
            IResourcePack pack;
            if (file.isDirectory()) {
                pack = new FolderResourcePack(file);
            } else if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                pack = new FileResourcePack(file);
            } else {
                LOGGER.info("Ignoriert (weder Ordner noch ZIP-Datei): {}", file.getName());
                continue;
            }

            defaultPacks.add(pack);
            if (directManager != null) {
                directManager.reloadResourcePack(pack);
            }
            registered.add(pack);
            LOGGER.info("Contentpack eingehängt: {} (Namensräume: {})", file.getName(), pack.getResourceDomains());
        }
        return registered;
    }
}
