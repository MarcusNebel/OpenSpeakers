package com.marcusnebel.openspeakers.contentpack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ein Contentpack: ein Resourcepack-Namensraum mit einer openspeakers.json und den dazugehörigen Sounds. */
public class ContentPack {

    private final String domain;
    private final String name;
    private final List<Announcement> announcements;

    public ContentPack(String domain, String name, List<Announcement> announcements) {
        this.domain = domain;
        this.name = name;
        this.announcements = Collections.unmodifiableList(new ArrayList<>(announcements));
    }

    /** Namensraum des Packs (Ordnername unter assets/). */
    public String getDomain() {
        return domain;
    }

    /** Anzeigename aus der openspeakers.json. */
    public String getName() {
        return name;
    }

    public List<Announcement> getAnnouncements() {
        return announcements;
    }

    /** Eine einzelne Ansage eines Packs. */
    public static class Announcement {
        private final String id;
        private final String name;
        private final String soundName;

        public Announcement(String id, String name, String soundName) {
            this.id = id;
            this.name = name;
            this.soundName = soundName;
        }

        /** Eindeutige ID innerhalb des Packs. */
        public String getId() {
            return id;
        }

        /** Anzeigename. */
        public String getName() {
            return name;
        }

        /** Vollständiger Sound-Name im Format "namensraum:ereignis", wie in der sounds.json des Packs. */
        public String getSoundName() {
            return soundName;
        }
    }
}
