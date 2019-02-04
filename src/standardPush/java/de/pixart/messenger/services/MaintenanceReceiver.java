package de.pixart.messenger.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;

import de.pixart.messenger.Config;
import de.pixart.messenger.utils.Compatibility;

public class MaintenanceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Config.LOGTAG, "received intent in maintenance receiver");
        if ("de.pixart.messenger.RENEW_INSTANCE_ID".equals(intent.getAction())) {
            renewInstanceToken(context);

        }
    }

    private void renewInstanceToken(final Context context) {
        new Thread(() -> {
            try {
                FirebaseInstanceId.getInstance().deleteInstanceId();
                final Intent intent = new Intent(context, XmppConnectionService.class);
                intent.setAction(XmppConnectionService.ACTION_FCM_TOKEN_REFRESH);
                Compatibility.startService(context, intent);
            } catch (IOException e) {
                Log.d(Config.LOGTAG, "unable to renew instance token", e);
            }
        }).start();

    }
}