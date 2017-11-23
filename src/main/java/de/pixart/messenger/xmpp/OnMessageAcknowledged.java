package de.pixart.messenger.xmpp;

import de.pixart.messenger.entities.Account;

public interface OnMessageAcknowledged {
    void onMessageAcknowledged(Account account, String id);
}
