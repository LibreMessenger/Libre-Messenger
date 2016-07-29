package de.pixart.messenger.xmpp.jingle;

import de.pixart.messenger.entities.Account;
import de.pixart.messenger.xmpp.PacketReceived;
import de.pixart.messenger.xmpp.jingle.stanzas.JinglePacket;

public interface OnJinglePacketReceived extends PacketReceived {
	void onJinglePacketReceived(Account account, JinglePacket packet);
}
