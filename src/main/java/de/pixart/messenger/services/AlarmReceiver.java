package de.pixart.messenger.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.pixart.messenger.Config;

public class AlarmReceiver extends BroadcastReceiver{
    public static final int SCHEDULE_ALARM_REQUEST_CODE = 523976483;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().contains("exportlogs")) {
            Log.d(Config.LOGTAG, "Received alarm broadcast to export logs");
            Intent i = new Intent(context, ExportLogsService.class);
            context.startService(i);
        }
    }
}
