package de.pixart.messenger.xmpp.jingle;

public interface OnTransportConnected {
    void failed();

    void established();
}
