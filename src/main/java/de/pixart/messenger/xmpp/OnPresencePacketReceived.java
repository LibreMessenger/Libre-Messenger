package de.pixart.messenger.xmpp;

import de.pixart.messenger.entities.Account;
import de.pixart.messenger.xmpp.stanzas.PresencePacket;

public interface OnPresencePacketReceived extends PacketReceived {
    void onPresencePacketReceived(Account account, PresencePacket packet);
}
