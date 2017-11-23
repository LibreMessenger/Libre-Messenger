package de.pixart.messenger.xmpp;

import de.pixart.messenger.entities.Account;

public interface OnStatusChanged {
    void onStatusChanged(Account account);
}
