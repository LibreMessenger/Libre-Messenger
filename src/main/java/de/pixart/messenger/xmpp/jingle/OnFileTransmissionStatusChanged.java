package de.pixart.messenger.xmpp.jingle;

import de.pixart.messenger.entities.DownloadableFile;

public interface OnFileTransmissionStatusChanged {
    void onFileTransmitted(DownloadableFile file);

    void onFileTransferAborted();
}
