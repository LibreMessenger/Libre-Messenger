package de.pixart.messenger.crypto.axolotl;

public interface OnMessageCreatedCallback {
    void run(XmppAxolotlMessage message);
}
