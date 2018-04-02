package de.pixart.messenger.entities;

import rocks.xmpp.addr.Jid;

public interface Blockable {
    boolean isBlocked();

    boolean isDomainBlocked();

    Jid getBlockedJid();

    Jid getJid();

    Account getAccount();
}
