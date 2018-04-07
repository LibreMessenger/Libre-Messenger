package de.pixart.messenger.services;

import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.entities.Account;
import de.pixart.messenger.utils.Namespace;
import de.pixart.messenger.xml.Element;
import de.pixart.messenger.xmpp.XmppConnection;
import de.pixart.messenger.xmpp.forms.Data;
import de.pixart.messenger.xmpp.stanzas.IqPacket;
import rocks.xmpp.addr.Jid;

public class PushManagementService {

    private static final Jid APP_SERVER = Jid.of("push.siacs.eu");

    protected final XmppConnectionService mXmppConnectionService;

    PushManagementService(XmppConnectionService service) {
        this.mXmppConnectionService = service;
    }

    void registerPushTokenOnServer(final Account account) {
        Log.d(Config.LOGTAG, account.getJid().asBareJid() + ": has push support");
        retrieveGcmInstanceToken(token -> {
            final String deviceId = Settings.Secure.getString(mXmppConnectionService.getContentResolver(), Settings.Secure.ANDROID_ID);
            IqPacket packet = mXmppConnectionService.getIqGenerator().pushTokenToAppServer(APP_SERVER, token, deviceId);
            mXmppConnectionService.sendIqPacket(account, packet, (a, p) -> {
                Element command = p.findChild("command", "http://jabber.org/protocol/commands");
                if (p.getType() == IqPacket.TYPE.RESULT && command != null) {
                    Element x = command.findChild("x", Namespace.DATA);
                    if (x != null) {
                        Data data = Data.parse(x);
                        try {
                            String node = data.getValue("node");
                            String secret = data.getValue("secret");
                            Jid jid = Jid.of(data.getValue("jid"));
                            if (node != null && secret != null) {
                                enablePushOnServer(a, jid, node, secret);
                            }
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Log.d(Config.LOGTAG, a.getJid().asBareJid() + ": invalid response from app server");
                }
            });
        });
    }

    private void enablePushOnServer(final Account account, final Jid jid, final String node, final String secret) {
        IqPacket enable = mXmppConnectionService.getIqGenerator().enablePush(jid, node, secret);
        mXmppConnectionService.sendIqPacket(account, enable, (a, p) -> {
            if (p.getType() == IqPacket.TYPE.RESULT) {
                Log.d(Config.LOGTAG, a.getJid().asBareJid() + ": successfully enabled push on server");
            } else if (p.getType() == IqPacket.TYPE.ERROR) {
                Log.d(Config.LOGTAG, a.getJid().asBareJid() + ": enabling push on server failed");
            }
        });
    }

    private void retrieveGcmInstanceToken(final OnGcmInstanceTokenRetrieved instanceTokenRetrieved) {
        new Thread(() -> {
            InstanceID instanceID = InstanceID.getInstance(mXmppConnectionService);
            try {
                String token = instanceID.getToken(mXmppConnectionService.getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                instanceTokenRetrieved.onGcmInstanceTokenRetrieved(token);
            } catch (Exception e) {
                Log.d(Config.LOGTAG, "unable to get push token");
            }
        }).start();

    }


    public boolean available(Account account) {
        final XmppConnection connection = account.getXmppConnection();
        return connection != null
                && connection.getFeatures().sm()
                && connection.getFeatures().push()
                && playServicesAvailable();
    }

    private boolean playServicesAvailable() {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mXmppConnectionService) == ConnectionResult.SUCCESS;
    }

    public boolean isStub() {
        return false;
    }

    interface OnGcmInstanceTokenRetrieved {
        void onGcmInstanceTokenRetrieved(String token);
    }
}