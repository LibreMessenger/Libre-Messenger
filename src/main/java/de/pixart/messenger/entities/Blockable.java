package de.pixart.messenger.entities;

import de.pixart.messenger.xmpp.jid.Jid;

public interface Blockable {
    boolean isBlocked();

    boolean isDomainBlocked();

    Jid getBlockedJid();

    Jid getJid();

    Account getAccount();
}
