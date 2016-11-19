package de.pixart.messenger.xmpp.stanzas.csi;

import de.pixart.messenger.xmpp.stanzas.AbstractStanza;

public class ActivePacket extends AbstractStanza {
    public ActivePacket() {
        super("active");
        setAttribute("xmlns", "urn:xmpp:csi:0");
    }
}
