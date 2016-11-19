package de.pixart.messenger.xmpp.stanzas.csi;

import de.pixart.messenger.xmpp.stanzas.AbstractStanza;

public class InactivePacket extends AbstractStanza {
    public InactivePacket() {
        super("inactive");
        setAttribute("xmlns", "urn:xmpp:csi:0");
    }
}
