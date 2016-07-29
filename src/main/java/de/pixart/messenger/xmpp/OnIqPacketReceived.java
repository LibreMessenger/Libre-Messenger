package de.pixart.messenger.xmpp;

import de.pixart.messenger.entities.Account;
import de.pixart.messenger.xmpp.stanzas.IqPacket;

public interface OnIqPacketReceived extends PacketReceived {
	public void onIqPacketReceived(Account account, IqPacket packet);
}
