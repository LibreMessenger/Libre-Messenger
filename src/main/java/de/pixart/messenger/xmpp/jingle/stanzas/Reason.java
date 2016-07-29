package de.pixart.messenger.xmpp.jingle.stanzas;

import de.pixart.messenger.xml.Element;

public class Reason extends Element {
	private Reason(String name) {
		super(name);
	}

	public Reason() {
		super("reason");
	}
}
