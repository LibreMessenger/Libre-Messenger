package de.pixart.messenger.xmpp.stanzas;

import de.pixart.messenger.xml.Element;
import de.pixart.messenger.xmpp.InvalidJid;

abstract public class AbstractAcknowledgeableStanza extends AbstractStanza {

    protected AbstractAcknowledgeableStanza(String name) {
        super(name);
    }


    public String getId() {
        return this.getAttribute("id");
    }

    public void setId(final String id) {
        setAttribute("id", id);
    }

    public Element getError() {
        Element error = findChild("error");
        if (error != null) {
            for (Element element : error.getChildren()) {
                if (!element.getName().equals("text")) {
                    return element;
                }
            }
        }
        return null;
    }

    public boolean valid() {
        return InvalidJid.isValid(getFrom()) && InvalidJid.isValid(getTo());
    }
}
