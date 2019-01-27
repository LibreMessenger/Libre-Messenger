package de.pixart.messenger.entities;

import android.content.Context;

import java.util.List;

import de.pixart.messenger.services.AvatarService;
import rocks.xmpp.addr.Jid;

public interface ListItem extends Comparable<ListItem>, AvatarService.Avatarable  {
    String getDisplayName();

    int getOffline();

    Jid getJid();

    List<Tag> getTags(Context context);

    final class Tag {
        private final String name;
        private final int color;
        private final int offline;

        public Tag(final String name, final int color, final int offline) {
            this.name = name;
            this.color = color;
            this.offline = offline;
        }

        public int getColor() {
            return this.color;
        }

        public String getName() {
            return this.name;
        }

        public int getOffline() {
            return this.offline;
        }
    }

    boolean match(Context context, final String needle);
}
