package de.pixart.messenger.ui;

import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import com.wefika.flowlayout.FlowLayout;

import org.openintents.openpgp.util.OpenPgpUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.crypto.axolotl.AxolotlService;
import de.pixart.messenger.crypto.axolotl.FingerprintStatus;
import de.pixart.messenger.crypto.axolotl.XmppAxolotlSession;
import de.pixart.messenger.entities.Account;
import de.pixart.messenger.entities.Contact;
import de.pixart.messenger.entities.Conversation;
import de.pixart.messenger.entities.ListItem;
import de.pixart.messenger.services.XmppConnectionService.OnAccountUpdate;
import de.pixart.messenger.services.XmppConnectionService.OnRosterUpdate;
import de.pixart.messenger.utils.CryptoHelper;
import de.pixart.messenger.utils.Namespace;
import de.pixart.messenger.utils.UIHelper;
import de.pixart.messenger.utils.XmppUri;
import de.pixart.messenger.xmpp.OnKeyStatusUpdated;
import de.pixart.messenger.xmpp.OnUpdateBlocklist;
import de.pixart.messenger.xmpp.XmppConnection;
import de.pixart.messenger.xmpp.jid.InvalidJidException;
import de.pixart.messenger.xmpp.jid.Jid;

public class ContactDetailsActivity extends OmemoActivity implements OnAccountUpdate, OnRosterUpdate, OnUpdateBlocklist, OnKeyStatusUpdated {
    public static final String ACTION_VIEW_CONTACT = "view_contact";

    private Contact contact;
    private Conversation mConversation;

    private DialogInterface.OnClickListener removeFromRoster = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            xmppConnectionService.deleteContactOnServer(contact);
        }
    };
    private OnCheckedChangeListener mOnSendCheckedChange = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {
            if (isChecked) {
                if (contact
                        .getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST)) {
                    xmppConnectionService.sendPresencePacket(contact
                                    .getAccount(),
                            xmppConnectionService.getPresenceGenerator()
                                    .sendPresenceUpdatesTo(contact));
                } else {
                    contact.setOption(Contact.Options.PREEMPTIVE_GRANT);
                }
            } else {
                contact.resetOption(Contact.Options.PREEMPTIVE_GRANT);
                xmppConnectionService.sendPresencePacket(contact.getAccount(),
                        xmppConnectionService.getPresenceGenerator()
                                .stopPresenceUpdatesTo(contact));
            }
        }
    };
    private OnCheckedChangeListener mOnReceiveCheckedChange = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {
            if (isChecked) {
                xmppConnectionService.sendPresencePacket(contact.getAccount(),
                        xmppConnectionService.getPresenceGenerator()
                                .requestPresenceUpdatesFrom(contact));
            } else {
                xmppConnectionService.sendPresencePacket(contact.getAccount(),
                        xmppConnectionService.getPresenceGenerator()
                                .stopPresenceUpdatesFrom(contact));
            }
        }
    };
    private Jid accountJid;
    private TextView lastseen;
    private Jid contactJid;
    private TextView contactDisplayName;
    private TextView contactJidTv;
    private TextView accountJidTv;
    private TextView statusMessage;
    private TextView resource;
    private CheckBox send;
    private CheckBox receive;
    private Button addContactButton;
    private Button mShowInactiveDevicesButton;
    private QuickContactBadge badge;
    private LinearLayout keys;
    private LinearLayout keysWrapper;
    private FlowLayout tags;
    private boolean showDynamicTags = false;
    private boolean showLastSeen = false;
    private boolean showInactiveOmemo = false;
    private String messageFingerprint;
    private TextView mNotifyStatusText;
    private ImageButton mNotifyStatusButton;

    private DialogInterface.OnClickListener addToPhonebook = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
            intent.setType(Contacts.CONTENT_ITEM_TYPE);
            intent.putExtra(Intents.Insert.IM_HANDLE, contact.getJid().toString());
            intent.putExtra(Intents.Insert.IM_PROTOCOL,
                    CommonDataKinds.Im.PROTOCOL_JABBER);
            intent.putExtra("finishActivityOnSaveCompleted", true);
            ContactDetailsActivity.this.startActivityForResult(intent, 0);
        }
    };

    private OnClickListener onBadgeClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Uri systemAccount = contact.getSystemAccount();
            if (systemAccount == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        ContactDetailsActivity.this);
                builder.setTitle(getString(R.string.action_add_phone_book));
                builder.setMessage(getString(R.string.add_phone_book_text,
                        contact.getDisplayJid()));
                builder.setNegativeButton(getString(R.string.cancel), null);
                builder.setPositiveButton(getString(R.string.add), addToPhonebook);
                builder.create().show();
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(systemAccount);
                startActivity(intent);
            }
        }
    };

    private OnClickListener mNotifyStatusClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ContactDetailsActivity.this);
            builder.setTitle(R.string.pref_notification_settings);
            String[] choices = {
                    getString(R.string.notify_on_all_messages),
                    getString(R.string.notify_never)
            };
            final AtomicInteger choice;
            if (mConversation.alwaysNotify()) {
                choice = new AtomicInteger(0);
            } else {
                choice = new AtomicInteger(1);
            }
            builder.setSingleChoiceItems(choices, choice.get(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    choice.set(which);
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (choice.get() == 1) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ContactDetailsActivity.this);
                        builder.setTitle(R.string.disable_notifications);
                        final int[] durations = getResources().getIntArray(R.array.mute_options_durations);
                        builder.setItems(R.array.mute_options_descriptions,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(final DialogInterface dialog, final int which) {
                                        final long till;
                                        if (durations[which] == -1) {
                                            till = Long.MAX_VALUE;
                                        } else {
                                            till = System.currentTimeMillis() + (durations[which] * 1000);
                                        }
                                        mConversation.setMutedTill(till);
                                        xmppConnectionService.updateConversation(mConversation);
                                        populateView();
                                    }
                                });
                        builder.create().show();
                    } else {
                        mConversation.setMutedTill(0);
                        mConversation.setAttribute(Conversation.ATTRIBUTE_ALWAYS_NOTIFY, String.valueOf(choice.get() == 0));
                    }
                    xmppConnectionService.updateConversation(mConversation);
                    populateView();
                }
            });
            builder.create().show();
        }
    };

    @Override
    public void onRosterUpdate() {
        refreshUi();
    }

    @Override
    public void onAccountUpdate() {
        refreshUi();
    }

    @Override
    public void OnUpdateBlocklist(final Status status) {
        refreshUi();
    }

    @Override
    protected void refreshUiReal() {
        invalidateOptionsMenu();
        populateView();
    }

    @Override
    protected String getShareableUri(boolean http) {
        final String prefix = http ? Config.inviteUserURL : "xmpp:";
        if (contact != null) {
            return prefix + contact.getJid().toBareJid().toString();
        } else {
            return "";
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showInactiveOmemo = savedInstanceState != null && savedInstanceState.getBoolean("show_inactive_omemo", false);
        if (getIntent().getAction().equals(ACTION_VIEW_CONTACT)) {
            try {
                this.accountJid = Jid.fromString(getIntent().getExtras().getString(EXTRA_ACCOUNT));
            } catch (final InvalidJidException ignored) {
            }
            try {
                this.contactJid = Jid.fromString(getIntent().getExtras().getString("contact"));
            } catch (final InvalidJidException ignored) {
            }
        }
        this.messageFingerprint = getIntent().getStringExtra("fingerprint");
        setContentView(R.layout.activity_contact_details);

        contactDisplayName = findViewById(R.id.contact_display_name);
        contactJidTv = findViewById(R.id.details_contactjid);
        accountJidTv = findViewById(R.id.details_account);
        lastseen = findViewById(R.id.details_lastseen);
        statusMessage = findViewById(R.id.status_message);
        resource = findViewById(R.id.resource);
        send = findViewById(R.id.details_send_presence);
        receive = findViewById(R.id.details_receive_presence);
        badge = findViewById(R.id.details_contact_badge);
        addContactButton = findViewById(R.id.add_contact_button);
        keys = findViewById(R.id.details_contact_keys);
        keysWrapper = findViewById(R.id.keys_wrapper);
        tags = findViewById(R.id.tags);
        mShowInactiveDevicesButton = findViewById(R.id.show_inactive_devices);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mShowInactiveDevicesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showInactiveOmemo = !showInactiveOmemo;
                populateView();
            }
        });
        this.mNotifyStatusButton = findViewById(R.id.notification_status_button);
        this.mNotifyStatusButton.setOnClickListener(this.mNotifyStatusClickListener);
        this.mNotifyStatusText = findViewById(R.id.notification_status_text);
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {
        savedInstanceState.putBoolean("show_inactive_omemo", showInactiveOmemo);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        final int theme = findTheme();
        if (this.mTheme != theme) {
            recreate();
        } else {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            this.showDynamicTags = preferences.getBoolean(SettingsActivity.SHOW_DYNAMIC_TAGS, false);
            this.showLastSeen = preferences.getBoolean("last_activity", false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setNegativeButton(getString(R.string.cancel), null);
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_share_http:
                shareLink(true);
                break;
            case R.id.action_share_uri:
                shareLink(false);
                break;
            case R.id.action_edit_contact:
                Uri systemAccount = contact.getSystemAccount();
                if (systemAccount == null) {
                    quickEdit(contact.getDisplayName(), 0, new OnValueEdited() {

                        @Override
                        public String onValueEdited(String value) {
                            contact.setServerName(value);
                            ContactDetailsActivity.this.xmppConnectionService.pushContactToServer(contact);
                            populateView();
                            return null;
                        }
                    });
                } else {
                    Intent intent = new Intent(Intent.ACTION_EDIT);
                    intent.setDataAndType(systemAccount, Contacts.CONTENT_ITEM_TYPE);
                    intent.putExtra("finishActivityOnSaveCompleted", true);
                    startActivity(intent);
                }
                break;
            case R.id.action_block:
                BlockContactDialog.show(this, contact);
                break;
            case R.id.action_unblock:
                BlockContactDialog.show(this, contact);
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.contact_details, menu);
        MenuItem block = menu.findItem(R.id.action_block);
        MenuItem unblock = menu.findItem(R.id.action_unblock);
        MenuItem edit = menu.findItem(R.id.action_edit_contact);
        if (contact == null) {
            return true;
        }
        final XmppConnection connection = contact.getAccount().getXmppConnection();
        if (connection != null && connection.getFeatures().blocking()) {
            if (this.contact.isBlocked()) {
                block.setVisible(false);
            } else {
                unblock.setVisible(false);
            }
        } else {
            unblock.setVisible(false);
            block.setVisible(false);
        }
        if (!contact.showInRoster()) {
            edit.setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void populateView() {
        if (contact == null) {
            return;
        }

        long mutedTill = mConversation.getLongAttribute(Conversation.ATTRIBUTE_MUTED_TILL, 0);
        if (mutedTill == Long.MAX_VALUE) {
            mNotifyStatusText.setText(R.string.notify_never);
            mNotifyStatusButton.setImageResource(R.drawable.ic_notifications_off_grey600_24dp);
        } else if (System.currentTimeMillis() < mutedTill) {
            mNotifyStatusText.setText(R.string.notify_paused);
            mNotifyStatusButton.setImageResource(R.drawable.ic_notifications_paused_grey600_24dp);
        } else {
            mNotifyStatusButton.setImageResource(R.drawable.ic_notifications_grey600_24dp);
            mNotifyStatusText.setText(R.string.notify_on_all_messages);
        }
        if (getSupportActionBar() != null) {
            final ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setCustomView(R.layout.ab_title);
                ab.setDisplayShowCustomEnabled(true);
                TextView abtitle = findViewById(android.R.id.text1);
                TextView absubtitle = findViewById(android.R.id.text2);
                abtitle.setText(contact.getDisplayName());
                abtitle.setSelected(true);
                abtitle.setClickable(false);
                absubtitle.setVisibility(View.GONE);
                absubtitle.setClickable(false);
            }
        }

        invalidateOptionsMenu();
        setTitle(contact.getDisplayName());
        if (contact.getServer().toString().toLowerCase().equals(accountJid.getDomainpart().toLowerCase())) {
            contactDisplayName.setText(contact.getDisplayName());
        } else {
            contactDisplayName.setText(contact.getDisplayJid());
        }
        if (contact.showInRoster()) {
            send.setVisibility(View.VISIBLE);
            receive.setVisibility(View.VISIBLE);
            addContactButton.setVisibility(View.VISIBLE);
            addContactButton.setText(getString(R.string.action_delete_contact));
            addContactButton.getBackground().setColorFilter(getWarningButtonColor(), PorterDuff.Mode.MULTIPLY);
            final AlertDialog.Builder deleteFromRosterDialog = new AlertDialog.Builder(ContactDetailsActivity.this);
            addContactButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteFromRosterDialog.setNegativeButton(getString(R.string.cancel), null);
                    deleteFromRosterDialog.setTitle(getString(R.string.action_delete_contact))
                            .setMessage(
                                    getString(R.string.remove_contact_text,
                                            contact.getDisplayJid()))
                            .setPositiveButton(getString(R.string.delete),
                                    removeFromRoster).create().show();
                }
            });
            send.setOnCheckedChangeListener(null);
            receive.setOnCheckedChangeListener(null);

            List<String> statusMessages = contact.getPresences().getStatusMessages();
            if (statusMessages.size() == 0) {
                statusMessage.setVisibility(View.GONE);
            } else {
                StringBuilder builder = new StringBuilder();
                statusMessage.setVisibility(View.VISIBLE);
                int s = statusMessages.size();
                for (int i = 0; i < s; ++i) {
                    if (s > 1) {
                        builder.append("• ");
                    }
                    builder.append(statusMessages.get(i));
                    if (i < s - 1) {
                        builder.append("\n");
                    }
                }
                statusMessage.setText(builder);
            }

            String resources = contact.getPresences().getMostAvailableResource();
            if (resources.length() == 0) {
                resource.setVisibility(View.GONE);
            } else {
                resource.setVisibility(View.VISIBLE);
                resource.setText(resources);
            }

            if (contact.getOption(Contact.Options.FROM)) {
                send.setText(R.string.send_presence_updates);
                send.setChecked(true);
            } else if (contact.getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST)) {
                send.setChecked(false);
                send.setText(R.string.send_presence_updates);
            } else {
                send.setText(R.string.preemptively_grant);
                if (contact.getOption(Contact.Options.PREEMPTIVE_GRANT)) {
                    send.setChecked(true);
                } else {
                    send.setChecked(false);
                }
            }
            if (contact.getOption(Contact.Options.TO)) {
                receive.setText(R.string.receive_presence_updates);
                receive.setChecked(true);
            } else {
                receive.setText(R.string.ask_for_presence_updates);
                if (contact.getOption(Contact.Options.ASKING)) {
                    receive.setChecked(true);
                } else {
                    receive.setChecked(false);
                }
            }
            if (contact.getAccount().isOnlineAndConnected()) {
                receive.setEnabled(true);
                send.setEnabled(true);
            } else {
                receive.setEnabled(false);
                send.setEnabled(false);
            }
            send.setOnCheckedChangeListener(this.mOnSendCheckedChange);
            receive.setOnCheckedChangeListener(this.mOnReceiveCheckedChange);
        } else {
            addContactButton.setVisibility(View.VISIBLE);
            addContactButton.setText(getString(R.string.add_contact));
            addContactButton.getBackground().clearColorFilter();
            addContactButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    showAddToRosterDialog(contact);
                }
            });
            send.setVisibility(View.GONE);
            receive.setVisibility(View.GONE);
            statusMessage.setVisibility(View.GONE);
        }

        if (contact.isBlocked() && !this.showDynamicTags) {
            lastseen.setVisibility(View.VISIBLE);
            lastseen.setText(R.string.contact_blocked);
        } else {
            if (showLastSeen
                    && contact.getLastseen() > 0
                    && contact.getPresences().allOrNonSupport(Namespace.IDLE)) {
                lastseen.setVisibility(View.VISIBLE);
                lastseen.setText(UIHelper.lastseen(getApplicationContext(), contact.isActive(), contact.getLastseen()));
            } else {
                lastseen.setVisibility(View.GONE);
            }
        }

        if (contact.getPresences().size() > 1) {
            contactJidTv.setText(contact.getDisplayJid() + " ("
                    + contact.getPresences().size() + ")");
        } else {
            contactJidTv.setText(contact.getDisplayJid());
        }
        String account;
        if (Config.DOMAIN_LOCK != null) {
            account = contact.getAccount().getJid().getLocalpart();
        } else {
            account = contact.getAccount().getJid().toBareJid().toString();
        }
        accountJidTv.setText(getString(R.string.using_account, account));
        badge.setImageBitmap(avatarService().get(contact, getPixel(Config.AVATAR_SIZE)));
        badge.setOnClickListener(this.onBadgeClick);

        keys.removeAllViews();
        boolean hasKeys = false;
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (Config.supportOtr()) {
            for (final String otrFingerprint : contact.getOtrFingerprints()) {
                hasKeys = true;
                View view = inflater.inflate(R.layout.contact_key, keys, false);
                TextView key = view.findViewById(R.id.key);
                TextView keyType = view.findViewById(R.id.key_type);
                ImageButton removeButton = view
                        .findViewById(R.id.button_remove);
                removeButton.setVisibility(View.VISIBLE);
                key.setText(CryptoHelper.prettifyFingerprint(otrFingerprint));
                if (otrFingerprint != null && otrFingerprint.equalsIgnoreCase(messageFingerprint)) {
                    keyType.setText(R.string.otr_fingerprint_selected_message);
                    keyType.setTextColor(ContextCompat.getColor(this, R.color.accent));
                } else {
                    keyType.setText(R.string.otr_fingerprint);
                }
                keys.addView(view);
                removeButton.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        confirmToDeleteFingerprint(otrFingerprint);
                    }
                });
            }
        }
        final AxolotlService axolotlService = contact.getAccount().getAxolotlService();
        if (Config.supportOmemo() && axolotlService != null) {
            boolean skippedInactive = false;
            boolean showsInactive = false;
            for (final XmppAxolotlSession session : axolotlService.findSessionsForContact(contact)) {
                final FingerprintStatus trust = session.getTrust();
                hasKeys |= !trust.isCompromised();
                if (!trust.isActive()) {
                    if (showInactiveOmemo) {
                        showsInactive = true;
                    } else {
                        skippedInactive = true;
                        continue;
                    }
                }
                if (!trust.isCompromised()) {
                    boolean highlight = session.getFingerprint().equals(messageFingerprint);
                    addFingerprintRow(keys, session, highlight);
                }
            }
            if (showsInactive || skippedInactive) {
                mShowInactiveDevicesButton.setText(showsInactive ? R.string.hide_inactive_devices : R.string.show_inactive_devices);
                mShowInactiveDevicesButton.setVisibility(View.VISIBLE);
            } else {
                mShowInactiveDevicesButton.setVisibility(View.GONE);
            }
        } else {
            mShowInactiveDevicesButton.setVisibility(View.GONE);
        }
        if (Config.supportOpenPgp() && contact.getPgpKeyId() != 0) {
            hasKeys = true;
            View view = inflater.inflate(R.layout.contact_key, keys, false);
            TextView key = view.findViewById(R.id.key);
            TextView keyType = view.findViewById(R.id.key_type);
            keyType.setText(R.string.openpgp_key_id);
            if ("pgp".equals(messageFingerprint)) {
                keyType.setTextColor(ContextCompat.getColor(this, R.color.accent));
            }
            key.setText(OpenPgpUtils.convertKeyIdToHex(contact.getPgpKeyId()));
            final OnClickListener openKey = new OnClickListener() {

                @Override
                public void onClick(View v) {
                    launchOpenKeyChain(contact.getPgpKeyId());
                }
            };
            view.setOnClickListener(openKey);
            key.setOnClickListener(openKey);
            keyType.setOnClickListener(openKey);
            keys.addView(view);
        }
        keysWrapper.setVisibility(hasKeys ? View.VISIBLE : View.GONE);

        List<ListItem.Tag> tagList = contact.getTags(this);
        if (tagList.size() == 0 || !this.showDynamicTags) {
            tags.setVisibility(View.GONE);
        } else {
            tags.setVisibility(View.VISIBLE);
            tags.removeAllViewsInLayout();
            for (final ListItem.Tag tag : tagList) {
                final TextView tv = (TextView) inflater.inflate(R.layout.list_item_tag, tags, false);
                tv.setText(tag.getName());
                tv.setBackgroundColor(tag.getColor());
                tags.addView(tv);
            }
        }
    }

    protected void confirmToDeleteFingerprint(final String fingerprint) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_fingerprint);
        builder.setMessage(R.string.sure_delete_fingerprint);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.delete,
                new android.content.DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (contact.deleteOtrFingerprint(fingerprint)) {
                            populateView();
                            xmppConnectionService.syncRosterToDisk(contact.getAccount());
                        }
                    }

                });
        builder.create().show();
    }

    public void onBackendConnected() {
        if (accountJid != null && contactJid != null) {
            Account account = xmppConnectionService.findAccountByJid(accountJid);
            if (account == null) {
                return;
            }
            this.mConversation = xmppConnectionService.findConversation(account, contactJid, false);
            if (this.mConversation != null) {
                populateView();
            }
            this.contact = account.getRoster().getContact(contactJid);
            if (mPendingFingerprintVerificationUri != null) {
                processFingerprintVerification(mPendingFingerprintVerificationUri);
                mPendingFingerprintVerificationUri = null;
            }
            populateView();
        }
    }

    @Override
    public void onKeyStatusUpdated(AxolotlService.FetchStatus report) {
        refreshUi();
    }

    @Override
    protected void processFingerprintVerification(XmppUri uri) {
        if (contact != null && contact.getJid().toBareJid().equals(uri.getJid()) && uri.hasFingerprints()) {
            if (xmppConnectionService.verifyFingerprints(contact, uri.getFingerprints())) {
                Toast.makeText(this, R.string.verified_fingerprints, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.invalid_barcode, Toast.LENGTH_SHORT).show();
        }
    }
}
