package de.pixart.messenger.xmpp.jingle;

public interface OnPrimaryCandidateFound {
    void onPrimaryCandidateFound(boolean success, JingleCandidate canditate);
}
