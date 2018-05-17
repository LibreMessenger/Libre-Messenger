package de.pixart.messenger.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.support.annotation.BoolRes;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.NoSuchPaddingException;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.entities.Account;
import de.pixart.messenger.entities.Conversation;
import de.pixart.messenger.entities.Message;
import de.pixart.messenger.persistance.DatabaseBackend;
import de.pixart.messenger.persistance.FileBackend;
import de.pixart.messenger.utils.EncryptDecryptFile;
import de.pixart.messenger.utils.WakeLockHelper;
import rocks.xmpp.addr.Jid;

import static de.pixart.messenger.ui.SettingsActivity.USE_MULTI_ACCOUNTS;

public class ExportLogsService extends Service {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final String DIRECTORY_STRING_FORMAT = FileBackend.getConversationsDirectory("Chats", false) + "%s";
    private static final String MESSAGE_STRING_FORMAT = "(%s) %s: %s\n";
    private static final int NOTIFICATION_ID = 1;
    private static AtomicBoolean running = new AtomicBoolean(false);
    private DatabaseBackend mDatabaseBackend;
    private List<Account> mAccounts;
    boolean ReadableLogsEnabled = false;
    private WakeLock wakeLock;
    private PowerManager pm;
    XmppConnectionService mXmppConnectionService;

    @Override
    public void onCreate() {
        mDatabaseBackend = DatabaseBackend.getInstance(getBaseContext());
        mAccounts = mDatabaseBackend.getAccounts();
        final SharedPreferences ReadableLogs = PreferenceManager.getDefaultSharedPreferences(this);
        ReadableLogsEnabled = ReadableLogs.getBoolean("export_plain_text_logs", getResources().getBoolean(R.bool.plain_text_logs));
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ExportLogsService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (running.compareAndSet(false, true)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    export();
                    stopForeground(true);
                    WakeLockHelper.release(wakeLock);
                    running.set(false);
                    stopSelf();
                }
            }).start();
        }
        return START_NOT_STICKY;
    }

    private void export() {
        wakeLock.acquire();
        List<Conversation> conversations = mDatabaseBackend.getConversations(Conversation.STATUS_AVAILABLE);
        conversations.addAll(mDatabaseBackend.getConversations(Conversation.STATUS_ARCHIVED));
        NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getBaseContext());
        mBuilder.setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_export_logs_title))
                .setSmallIcon(R.drawable.ic_import_export_white_24dp)
                .setProgress(0, 0, true);
        startForeground(NOTIFICATION_ID, mBuilder.build());
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        if (ReadableLogsEnabled) {
            for (Conversation conversation : conversations) {
                writeToFile(conversation);
            }
        }
        if (mAccounts.size() == 1) {
            try {
                ExportDatabase();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeToFile(Conversation conversation) {
        Jid accountJid = resolveAccountUuid(conversation.getAccountUuid());
        Jid contactJid = conversation.getJid();

        File dir = new File(String.format(DIRECTORY_STRING_FORMAT, accountJid.asBareJid().toString()));
        dir.mkdirs();

        BufferedWriter bw = null;
        try {
            for (Message message : mDatabaseBackend.getMessagesIterable(conversation)) {
                if (message == null)
                    continue;
                if (message.getType() == Message.TYPE_TEXT || message.hasFileOnRemoteHost()) {
                    String date = simpleDateFormat.format(new Date(message.getTimeSent()));
                    if (bw == null) {
                        bw = new BufferedWriter(new FileWriter(
                                new File(dir, contactJid.asBareJid().toString() + ".txt")));
                    }
                    String jid = null;
                    switch (message.getStatus()) {
                        case Message.STATUS_RECEIVED:
                            jid = getMessageCounterpart(message);
                            break;
                        case Message.STATUS_SEND:
                        case Message.STATUS_SEND_RECEIVED:
                        case Message.STATUS_SEND_DISPLAYED:
                            jid = accountJid.asBareJid().toString();
                            break;
                    }
                    if (jid != null) {
                        String body = message.hasFileOnRemoteHost() ? message.getFileParams().url.toString() : message.getBody();
                        bw.write(String.format(MESSAGE_STRING_FORMAT, date, jid,
                                body.replace("\\\n", "\\ \n").replace("\n", "\\ \n")));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private Jid resolveAccountUuid(String accountUuid) {
        for (Account account : mAccounts) {
            if (account.getUuid().equals(accountUuid)) {
                return account.getJid();
            }
        }
        return null;
    }

    private String getMessageCounterpart(Message message) {
        String trueCounterpart = (String) message.getContentValues().get(Message.TRUE_COUNTERPART);
        if (trueCounterpart != null) {
            return trueCounterpart;
        } else {
            return message.getCounterpart().toString();
        }
    }

    public void ExportDatabase() throws IOException {
        Account mAccount = mAccounts.get(0);
        String EncryptionKey = null;
        // Get hold of the db:
        FileInputStream InputFile = new FileInputStream(this.getDatabasePath(DatabaseBackend.DATABASE_NAME));
        // Set the output folder on the SDcard
        File directory = new File(FileBackend.getConversationsDirectory("Database", false));
        // Create the folder if it doesn't exist:
        if (!directory.exists()) {
            directory.mkdirs();
        }
        //Delete old database export file
        File temp_db_file = new File(directory + "/database.bak");
        if (temp_db_file.exists()) {
            Log.d(Config.LOGTAG, "Delete temp database backup file from " + temp_db_file.toString());
            temp_db_file.delete();
        }
        // Set the output file stream up:
        FileOutputStream OutputFile = new FileOutputStream(directory.getPath() + "/database.db.crypt");

        if (mAccounts.size() == 1 && !multipleAccounts()) {
            EncryptionKey = mAccount.getPassword(); //get account password
        } else {
            SharedPreferences multiaccount_prefs = getApplicationContext().getSharedPreferences(USE_MULTI_ACCOUNTS, Context.MODE_PRIVATE);
            String password = multiaccount_prefs.getString("BackupPW", null);
            if (password == null) {
                Log.d(Config.LOGTAG, "Database exporter: failed to write encryted backup to sdcard because of missing password");
                return;
            }
            EncryptionKey = password; //get previously set backup password
        }

        // encrypt database from the input file to the output file
        try {
            EncryptDecryptFile.encrypt(InputFile, OutputFile, EncryptionKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.d(Config.LOGTAG, "Database exporter: encryption failed with " + e);
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            Log.d(Config.LOGTAG, "Database exporter: encryption failed (invalid key) with " + e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(Config.LOGTAG, "Database exporter: encryption failed (IO) with " + e);
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    public boolean getBooleanPreference(String name, @BoolRes int res) {
        return getPreferences().getBoolean(name, getResources().getBoolean(res));
    }

    public boolean multipleAccounts() {
        return getBooleanPreference("enable_multi_accounts", R.bool.confirm_messages);
    }
}
