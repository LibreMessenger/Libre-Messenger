package de.pixart.messenger.ui;

import android.support.v7.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.duenndns.ssl.MemorizingTrustManager;
import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.entities.Account;
import de.pixart.messenger.services.ExportLogsService;
import de.pixart.messenger.xmpp.XmppConnection;
import de.pixart.messenger.xmpp.jid.InvalidJidException;
import de.pixart.messenger.xmpp.jid.Jid;

public class SettingsActivity extends XmppActivity implements
        OnSharedPreferenceChangeListener {

    public static final String AWAY_WHEN_SCREEN_IS_OFF = "away_when_screen_off";
    public static final String TREAT_VIBRATE_AS_SILENT = "treat_vibrate_as_silent";
    public static final String DND_ON_SILENT_MODE = "dnd_on_silent_mode";
    public static final String MANUALLY_CHANGE_PRESENCE = "manually_change_presence";
    public static final String BLIND_TRUST_BEFORE_VERIFICATION = "btbv";
    public static final String AUTOMATIC_MESSAGE_DELETION = "automatic_message_deletion";
    public static final String BROADCAST_LAST_ACTIVITY = "last_activity";
    public static final String WARN_UNENCRYPTED_CHAT = "warn_unencrypted_chat";
    public static final String THEME = "theme";
    public static final String SHOW_DYNAMIC_TAGS = "show_dynamic_tags";
    public static final String SHOW_FOREGROUND_SERVICE = "show_foreground_service";
    public static final String USE_BUNDLED_EMOJIS = "use_bundled_emoji";
    public static final String USE_MULTI_ACCOUNTS = "use_multi_accounts";

    public static final int REQUEST_WRITE_LOGS = 0xbf8701;
    private SettingsFragment mSettingsFragment;

    Preference multiAccountPreference;
    boolean isMultiAccountChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fm = getFragmentManager();
        mSettingsFragment = (SettingsFragment) fm.findFragmentById(android.R.id.content);
        if (mSettingsFragment == null || !mSettingsFragment.getClass().equals(SettingsFragment.class)) {
            mSettingsFragment = new SettingsFragment();
            fm.beginTransaction().replace(android.R.id.content, mSettingsFragment).commit();
        }
    }

    @Override
    void onBackendConnected() {

    }

    @Override
    public void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        multiAccountPreference = mSettingsFragment.findPreference("enable_multi_accounts");
        isMultiAccountChecked = ((CheckBoxPreference) multiAccountPreference).isChecked();

        ListPreference resources = (ListPreference) mSettingsFragment.findPreference("resource");
        if (resources != null) {
            ArrayList<CharSequence> entries = new ArrayList<>(Arrays.asList(resources.getEntries()));
            if (!entries.contains(Build.MODEL)) {
                entries.add(0, Build.MODEL);
                resources.setEntries(entries.toArray(new CharSequence[entries.size()]));
                resources.setEntryValues(entries.toArray(new CharSequence[entries.size()]));
            }
        }

        if (Config.FORCE_ORBOT) {
            PreferenceCategory connectionOptions = (PreferenceCategory) mSettingsFragment.findPreference("connection_options");
            PreferenceScreen expert = (PreferenceScreen) mSettingsFragment.findPreference("expert");
            if (connectionOptions != null) {
                expert.removePreference(connectionOptions);
            }
        }

        //this feature is only available on Huawei Android 6.
        PreferenceScreen huaweiPreferenceScreen = (PreferenceScreen) mSettingsFragment.findPreference("huawei");
        if (huaweiPreferenceScreen != null) {
            Intent intent = huaweiPreferenceScreen.getIntent();
            //remove when Api version is above M (Version 6.0) or if the intent is not callable
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M || !isCallable(intent)) {
                PreferenceCategory generalCategory = (PreferenceCategory) mSettingsFragment.findPreference("general");
                generalCategory.removePreference(huaweiPreferenceScreen);
            }
        }

        PreferenceCategory connectionOptions = (PreferenceCategory) mSettingsFragment.findPreference("connection_options");
        PreferenceScreen expert = (PreferenceScreen) mSettingsFragment.findPreference("expert");
        if (connectionOptions != null) {
            expert.removePreference(connectionOptions);
        }

        final Preference removeCertsPreference = mSettingsFragment.findPreference("remove_trusted_certificates");
        removeCertsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final MemorizingTrustManager mtm = xmppConnectionService.getMemorizingTrustManager();
                final ArrayList<String> aliases = Collections.list(mtm.getCertificates());
                if (aliases.size() == 0) {
                    displayToast(getString(R.string.toast_no_trusted_certs));
                    return true;
                }
                final ArrayList selectedItems = new ArrayList<>();
                final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SettingsActivity.this);
                dialogBuilder.setTitle(getResources().getString(R.string.dialog_manage_certs_title));
                dialogBuilder.setMultiChoiceItems(aliases.toArray(new CharSequence[aliases.size()]), null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int indexSelected,
                                                boolean isChecked) {
                                if (isChecked) {
                                    selectedItems.add(indexSelected);
                                } else if (selectedItems.contains(indexSelected)) {
                                    selectedItems.remove(Integer.valueOf(indexSelected));
                                }
                                if (selectedItems.size() > 0)
                                    ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                                else {
                                    ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                                }
                            }
                        });

                dialogBuilder.setPositiveButton(
                        getResources().getString(R.string.dialog_manage_certs_positivebutton), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int count = selectedItems.size();
                                if (count > 0) {
                                    for (int i = 0; i < count; i++) {
                                        try {
                                            Integer item = Integer.valueOf(selectedItems.get(i).toString());
                                            String alias = aliases.get(item);
                                            mtm.deleteCertificate(alias);
                                        } catch (KeyStoreException e) {
                                            e.printStackTrace();
                                            displayToast("Error: " + e.getLocalizedMessage());
                                        }
                                    }
                                    if (xmppConnectionServiceBound) {
                                        reconnectAccounts();
                                    }
                                    displayToast(getResources().getQuantityString(R.plurals.toast_delete_certificates, count, count));
                                }
                            }
                        });
                dialogBuilder.setNegativeButton(getResources().getString(R.string.dialog_manage_certs_negativebutton), null);
                AlertDialog removeCertsDialog = dialogBuilder.create();
                removeCertsDialog.show();
                removeCertsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                return true;
            }
        });

        final Preference exportLogsPreference = mSettingsFragment.findPreference("export_logs");
        exportLogsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (hasStoragePermission(REQUEST_WRITE_LOGS)) {
                    startExport();
                }
                return true;
            }
        });

        final Preference deleteOmemoPreference = mSettingsFragment.findPreference("delete_omemo_identities");
        deleteOmemoPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                deleteOmemoIdentities();
                return true;
            }
        });

        final Preference enableMultiAccountsPreference = mSettingsFragment.findPreference("enable_multi_accounts");
        Log.d(Config.LOGTAG, "Multi account checkbox checked: " + isMultiAccountChecked);
        if (isMultiAccountChecked) {
            enableMultiAccountsPreference.setEnabled(false);
            /*
            if (xmppConnectionServiceBound) { // todo doesn't work --> it seems the service is never bound
                final List<Account> accounts = xmppConnectionService.getAccounts();
                Log.d(Config.LOGTAG, "Disabled multi account: Number of accounts " + accounts.size());
                if (accounts.size() > 1) {
                    Log.d(Config.LOGTAG, "Disabled multi account not possible because you have more than one account");
                    enableMultiAccountsPreference.setEnabled(false);
                } else {
                    Log.d(Config.LOGTAG, "Disabled multi account possible because you have one account");
                    enableMultiAccountsPreference.setEnabled(true);
                }
            } else {
                enableMultiAccountsPreference.setEnabled(false);
            }
            */
        } else {
            enableMultiAccountsPreference.setEnabled(true);
            enableMultiAccountsPreference.setOnPreferenceClickListener(preference -> {
                enableMultiAccounts();
                return true;
            });
        }
    }

    private boolean isCallable(final Intent i) {
        return i != null && getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }

    private void deleteOmemoIdentities() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.pref_delete_omemo_identities);
        final List<CharSequence> accounts = new ArrayList<>();
        for (Account account : xmppConnectionService.getAccounts()) {
            if (account.isEnabled()) {
                accounts.add(account.getJid().toBareJid().toString());
            }
        }
        final boolean[] checkedItems = new boolean[accounts.size()];
        builder.setMultiChoiceItems(accounts.toArray(new CharSequence[accounts.size()]), checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                checkedItems[which] = isChecked;
                final AlertDialog alertDialog = (AlertDialog) dialog;
                for (boolean item : checkedItems) {
                    if (item) {
                        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                        return;
                    }
                }
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.delete_selected_keys, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (int i = 0; i < checkedItems.length; ++i) {
                    if (checkedItems[i]) {
                        try {
                            Jid jid = Jid.fromString(accounts.get(i).toString());
                            Account account = xmppConnectionService.findAccountByJid(jid);
                            if (account != null) {
                                account.getAxolotlService().regenerateKeys(true);
                            }
                        } catch (InvalidJidException e) {
                            //
                        }

                    }
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private void enableMultiAccounts() {
        if (!isMultiAccountChecked) {
            multiAccountPreference.setEnabled(true);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setTitle(R.string.pref_enable_multi_accounts_title);
            builder.setMessage(R.string.pref_enable_multi_accounts_summary);
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
                ((CheckBoxPreference) multiAccountPreference).setChecked(false);
            });
            builder.setPositiveButton(R.string.enter_password, (dialog, which) -> {
                ((CheckBoxPreference) multiAccountPreference).setChecked(false);
                enterPasswordDialog();
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public void enterPasswordDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.password, null);

        final Preference preference = mSettingsFragment.findPreference("enable_multi_accounts");

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);
        final EditText password = promptsView.findViewById(R.id.password);
        final EditText confirm_password = promptsView.findViewById(R.id.confirm_password);
        confirm_password.setVisibility(View.VISIBLE);
        alertDialogBuilder.setTitle(R.string.enter_password);
        alertDialogBuilder.setMessage(R.string.enter_password);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(R.string.ok,
                        (dialog, id) -> {
                            final String pw1 = password.getText().toString();
                            final String pw2 = confirm_password.getText().toString();
                            if (!pw1.equals(pw2)) {
                                ((CheckBoxPreference) preference).setChecked(false);
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setTitle(R.string.error);
                                builder.setMessage(R.string.passwords_do_not_match);
                                builder.setNegativeButton(R.string.cancel, null);
                                builder.setPositiveButton(R.string.try_again, (dialog12, id12) -> enterPasswordDialog());
                                builder.create().show();
                            } else if (pw1.trim().isEmpty()) {
                                ((CheckBoxPreference) preference).setChecked(false);
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setTitle(R.string.error);
                                builder.setMessage(R.string.password_should_not_be_empty);
                                builder.setNegativeButton(R.string.cancel, null);
                                builder.setPositiveButton(R.string.try_again, (dialog1, id1) -> enterPasswordDialog());
                                builder.create().show();
                            } else {
                                ((CheckBoxPreference) preference).setChecked(true);
                                SharedPreferences multiaccount_prefs = getApplicationContext().getSharedPreferences(USE_MULTI_ACCOUNTS, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = multiaccount_prefs.edit();
                                editor.putString("BackupPW", pw1);
                                editor.commit();
                            }
                        })
                .setNegativeButton(R.string.cancel, null);
        alertDialogBuilder.create().show();

    }



    @Override
    public void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String name) {
        final List<String> resendPresence = Arrays.asList(
                "confirm_messages",
                DND_ON_SILENT_MODE,
                AWAY_WHEN_SCREEN_IS_OFF,
                "allow_message_correction",
                TREAT_VIBRATE_AS_SILENT,
                MANUALLY_CHANGE_PRESENCE,
                BROADCAST_LAST_ACTIVITY);
        if (name.equals("resource")) {
            String resource = preferences.getString("resource", "mobile")
                    .toLowerCase(Locale.US);
            if (xmppConnectionServiceBound) {
                for (Account account : xmppConnectionService.getAccounts()) {
                    if (account.setResource(resource)) {
                        if (account.isEnabled()) {
                            XmppConnection connection = account.getXmppConnection();
                            if (connection != null) {
                                connection.resetStreamId();
                            }
                            xmppConnectionService.reconnectAccountInBackground(account);
                        }
                    }
                }
            }
        } else if (name.equals(SHOW_FOREGROUND_SERVICE)) {
            xmppConnectionService.toggleForegroundService();
        } else if (resendPresence.contains(name)) {
            if (xmppConnectionServiceBound) {
                if (name.equals(AWAY_WHEN_SCREEN_IS_OFF)
                        || name.equals(MANUALLY_CHANGE_PRESENCE)) {
                    xmppConnectionService.toggleScreenEventReceiver();
                }
                if (name.equals(MANUALLY_CHANGE_PRESENCE) && !noAccountUsesPgp()) {
                    Toast.makeText(this, R.string.republish_pgp_keys, Toast.LENGTH_LONG).show();
                }
                xmppConnectionService.refreshAllPresences();
            }
        } else if (name.equals("dont_trust_system_cas")) {
            xmppConnectionService.updateMemorizingTrustmanager();
            reconnectAccounts();
        } else if (name.equals("use_tor")) {
            reconnectAccounts();
        } else if (name.equals(AUTOMATIC_MESSAGE_DELETION)) {
            xmppConnectionService.expireOldMessages(true);
        } else if (name.equals(THEME)) {
            final int theme = findTheme();
            if (this.mTheme != theme) {
                recreate();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0)
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestCode == REQUEST_WRITE_LOGS) {
                    startExport();
                }
            } else {
                Toast.makeText(this, R.string.no_storage_permission, Toast.LENGTH_SHORT).show();
            }
    }

    private void startExport() {
        startService(new Intent(getApplicationContext(), ExportLogsService.class));
    }

    private void displayToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void reconnectAccounts() {
        for (Account account : xmppConnectionService.getAccounts()) {
            if (account.isEnabled()) {
                xmppConnectionService.reconnectAccountInBackground(account);
            }
        }
    }

    public void refreshUiReal() {
        //nothing to do. This Activity doesn't implement any listeners
    }
}
