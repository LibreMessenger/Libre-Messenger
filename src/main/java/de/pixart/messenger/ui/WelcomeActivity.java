package de.pixart.messenger.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.databinding.WelcomeBinding;
import de.pixart.messenger.services.InstallReferrerService;
import de.pixart.messenger.ui.util.IntroHelper;
import de.pixart.messenger.utils.SignupUtils;
import de.pixart.messenger.utils.XmppUri;
import rocks.xmpp.addr.Jid;

import static de.pixart.messenger.Config.DISALLOW_REGISTRATION_IN_UI;
import static de.pixart.messenger.utils.PermissionUtils.allGranted;
import static de.pixart.messenger.utils.PermissionUtils.readGranted;

public class WelcomeActivity extends XmppActivity {

    private static final int REQUEST_IMPORT_BACKUP = 0x63fb;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 0XD737;

    private XmppUri inviteUri;

    private BroadcastReceiver installReferrerBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent data) {
            final String invite = data.getStringExtra(StartConversationActivity.EXTRA_INVITE_URI);
            if (invite == null) {
                return;
            }
            Log.d(Config.LOGTAG, "welcome activity received install referrer uri: " + invite);
            final XmppUri xmppUri = new XmppUri(invite);
            processXmppUri(xmppUri);
        }
    };

    private boolean processXmppUri(final XmppUri xmppUri) {
        if (xmppUri.isValidJid()) {
            final String preauth = xmppUri.getParamater("preauth");
            final Jid jid = xmppUri.getJid();
            final Intent intent;
            if (xmppUri.isAction(XmppUri.ACTION_REGISTER)) {
                intent = SignupUtils.getTokenRegistrationIntent(this, jid, preauth);
            } else if (xmppUri.isAction(XmppUri.ACTION_ROSTER) && "y".equals(xmppUri.getParamater("ibr"))) {
                intent = SignupUtils.getTokenRegistrationIntent(this, Jid.ofDomain(jid.getDomain()), preauth);
                intent.putExtra(StartConversationActivity.EXTRA_INVITE_URI, xmppUri.toString());
            } else {
                intent = null;
            }
            if (intent != null) {
                startActivity(intent);
                finish();
                return true;
            }
            this.inviteUri = xmppUri;
        }
        return false;
    }

    @Override
    protected void refreshUiReal() {
    }

    @Override
    void onBackendConnected() {
    }

    @Override
    public void onStart() {
        super.onStart();
        final int theme = findTheme();
        if (this.mTheme != theme) {
            recreate();
        }
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InstallReferrerService.INSTALL_REFERRER_BROADCAST_ACTION);
        registerReceiver(installReferrerBroadcastReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        unregisterReceiver(installReferrerBroadcastReceiver);
        super.onStop();
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (intent != null) {
            setIntent(intent);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        super.onCreate(savedInstanceState);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String referrer = preferences.getString(SignupUtils.INSTALL_REFERRER, null);
        final XmppUri referrerUri = referrer == null ? null : new XmppUri(referrer);
        if (referrerUri != null && processXmppUri(referrerUri)) {
            return;
        }
        WelcomeBinding binding = DataBindingUtil.setContentView(this, R.layout.welcome);
        setSupportActionBar(findViewById(R.id.toolbar));
        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(false);
            ab.setDisplayHomeAsUpEnabled(false);
        }
        IntroHelper.showIntro(this, false);
        if (hasStoragePermission(REQUEST_IMPORT_BACKUP)) {
            binding.importDatabase.setVisibility(View.VISIBLE);
            binding.importText.setVisibility(View.VISIBLE);
        }
        binding.importDatabase.setOnClickListener(v -> startActivity(new Intent(this, ImportBackupActivity.class)));


        binding.createAccount.setOnClickListener(v -> {
            final Intent intent = new Intent(WelcomeActivity.this, MagicCreateActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            addInviteUri(intent);
            startActivity(intent);
        });
        if (DISALLOW_REGISTRATION_IN_UI) {
            binding.createAccount.setVisibility(View.GONE);
        }
        binding.useExistingAccount.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, EditAccountActivity.class);
            intent.putExtra("init", true);
            intent.putExtra("existing", true);
            addInviteUri(intent);
            startActivity(intent);
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out);
            finish();
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out);
        });

    }

    public void addInviteUri(Intent to) {
        final Intent from = getIntent();
        if (from != null && from.hasExtra(StartConversationActivity.EXTRA_INVITE_URI)) {
            final String invite = from.getStringExtra(StartConversationActivity.EXTRA_INVITE_URI);
            to.putExtra(StartConversationActivity.EXTRA_INVITE_URI, invite);
        } else if (this.inviteUri != null) {
            Log.d(Config.LOGTAG, "injecting referrer uri into on-boarding flow");
            to.putExtra(StartConversationActivity.EXTRA_INVITE_URI, this.inviteUri.toString());
        }
    }

    public static void launch(AppCompatActivity activity) {
        Intent intent = new Intent(activity, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            if (allGranted(grantResults)) {
                switch (requestCode) {
                    case REQUEST_IMPORT_BACKUP:
                        startActivity(new Intent(this, ImportBackupActivity.class));
                        break;
                }
            } else {
                Toast.makeText(this, R.string.no_storage_permission, Toast.LENGTH_SHORT).show();
            }
        }
        if (readGranted(grantResults, permissions)) {
            if (xmppConnectionService != null) {
                xmppConnectionService.restartFileObserver();
            }
        }
    }
}