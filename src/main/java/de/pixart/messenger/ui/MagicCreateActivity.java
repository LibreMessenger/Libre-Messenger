package de.pixart.messenger.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.entities.Account;
import de.pixart.messenger.utils.CryptoHelper;
import rocks.xmpp.addr.Jid;

public class MagicCreateActivity extends XmppActivity implements TextWatcher, AdapterView.OnItemSelectedListener {

    private TextView mFullJidDisplay;
    private EditText mUsername;
    private Spinner mServer;
    String domain = null;

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
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magic_create);
        setSupportActionBar(findViewById(R.id.toolbar));
        configureActionBar(getSupportActionBar());
        mFullJidDisplay = findViewById(R.id.full_jid);
        final List<String> domains = Arrays.asList(getResources().getStringArray(R.array.domains));
        Collections.sort(domains, String::compareToIgnoreCase);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_selectable_list_item, domains);
        int defaultServer = adapter.getPosition("blabber.im");
        mUsername = findViewById(R.id.username);
        mServer = findViewById(R.id.server);
        mServer.setAdapter(adapter);
        mServer.setSelection(defaultServer);
        mServer.setOnItemSelectedListener(this);
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        Button next = findViewById(R.id.create_account);
        next.setOnClickListener(v -> {
            try {
                String username = mUsername.getText().toString();
                if (domain == null) {
                    domain = Config.MAGIC_CREATE_DOMAIN;
                }
                Jid jid = Jid.of(username.toLowerCase(), domain, null);
                if (!jid.getEscapedLocal().equals(jid.getLocal()) || username.length() < 3) {
                    mUsername.setError(getString(R.string.invalid_username));
                    mUsername.requestFocus();
                } else {
                    mUsername.setError(null);
                    Account account = xmppConnectionService.findAccountByJid(jid);
                    String password = CryptoHelper.createPassword(new SecureRandom());
                    if (account == null) {
                        account = new Account(jid, password);
                        account.setOption(Account.OPTION_REGISTER, true);
                        account.setOption(Account.OPTION_DISABLED, true);
                        account.setOption(Account.OPTION_MAGIC_CREATE, true);
                        xmppConnectionService.createAccount(account);
                    }
                    Intent intent = new Intent(MagicCreateActivity.this, EditAccountActivity.class);
                    intent.putExtra("jid", account.getJid().asBareJid().toString());
                    intent.putExtra("init", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.create_account));
                    builder.setCancelable(false);
                    StringBuilder messasge = new StringBuilder();
                    messasge.append(getString(R.string.secure_password_generated));
                    messasge.append("\n\n");
                    messasge.append(getString(R.string.password));
                    messasge.append(": ");
                    messasge.append(password);
                    builder.setMessage(messasge);
                    builder.setPositiveButton(getString(R.string.copy_to_clipboard), (dialogInterface, i) -> {
                        if (copyTextToClipboard(password, R.string.create_account)) {
                            StartConversationActivity.addInviteUri(intent, getIntent());
                            startActivity(intent);
                            overridePendingTransition(R.animator.fade_in, R.animator.fade_out);
                            finish();
                            overridePendingTransition(R.animator.fade_in, R.animator.fade_out);
                        }
                    });
                    builder.create().show();
                }
            } catch (IllegalArgumentException e) {
                mUsername.setError(getString(R.string.invalid_username));
                mUsername.requestFocus();
            }
        });
        mUsername.addTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        generateJID(s.toString());
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        generateJID(mUsername.getText().toString());
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        generateJID(mUsername.getText().toString());
    }

    private void generateJID(String s) {
        domain = mServer.getSelectedItem().toString();
        if (s.trim().length() > 0) {
            try {
                mFullJidDisplay.setVisibility(View.VISIBLE);
                if (domain == null) {
                    domain = Config.MAGIC_CREATE_DOMAIN;
                }
                Jid jid = Jid.of(s.toLowerCase(), domain, null);
                mFullJidDisplay.setText(getString(R.string.your_full_jid_will_be, jid.toEscapedString()));
            } catch (IllegalArgumentException e) {
                mFullJidDisplay.setVisibility(View.INVISIBLE);
            }

        } else {
            mFullJidDisplay.setVisibility(View.INVISIBLE);
        }
    }
}
