package de.pixart.messenger.crypto.sasl;

import java.security.SecureRandom;

import de.pixart.messenger.entities.Account;
import de.pixart.messenger.xml.TagWriter;

public class Anonymous extends SaslMechanism {

    public Anonymous(TagWriter tagWriter, Account account, SecureRandom rng) {
        super(tagWriter, account, rng);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getMechanism() {
        return "ANONYMOUS";
    }

    @Override
    public String getClientFirstMessage() {
        return "";
    }
}
