package de.pixart.messenger.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.pixart.messenger.Config;
import de.pixart.messenger.utils.Compatibility;

public class AlarmReceiver extends BroadcastReceiver {
    public static final int SCHEDULE_ALARM_REQUEST_CODE = 523976483;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().contains("exportlogs")) {
            Log.d(Config.LOGTAG, "Received alarm broadcast to export logs");
            final Intent backupIntent = new Intent(context, ExportBackupService.class);
            backupIntent.putExtra("NOTIFY_ON_BACKUP_COMPLETE", false);
            Compatibility.startService(context, backupIntent);
        }
    }
}
