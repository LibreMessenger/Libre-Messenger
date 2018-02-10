package de.pixart.messenger.ui;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v4.widget.SlidingPaneLayout.PanelSlideListener;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import net.java.otr4j.session.SessionStatus;

import org.openintents.openpgp.util.OpenPgpApi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.crypto.axolotl.AxolotlService;
import de.pixart.messenger.crypto.axolotl.FingerprintStatus;
import de.pixart.messenger.entities.Account;
import de.pixart.messenger.entities.Blockable;
import de.pixart.messenger.entities.Contact;
import de.pixart.messenger.entities.Conversation;
import de.pixart.messenger.entities.Message;
import de.pixart.messenger.entities.MucOptions;
import de.pixart.messenger.entities.Presence;
import de.pixart.messenger.entities.Transferable;
import de.pixart.messenger.persistance.FileBackend;
import de.pixart.messenger.services.EmojiService;
import de.pixart.messenger.services.UpdateService;
import de.pixart.messenger.services.XmppConnectionService;
import de.pixart.messenger.services.XmppConnectionService.OnAccountUpdate;
import de.pixart.messenger.services.XmppConnectionService.OnConversationUpdate;
import de.pixart.messenger.services.XmppConnectionService.OnRosterUpdate;
import de.pixart.messenger.ui.adapter.ConversationAdapter;
import de.pixart.messenger.utils.ExceptionHelper;
import de.pixart.messenger.utils.FileUtils;
import de.pixart.messenger.utils.UIHelper;
import de.pixart.messenger.xmpp.OnUpdateBlocklist;
import de.pixart.messenger.xmpp.XmppConnection;
import de.pixart.messenger.xmpp.chatstate.ChatState;
import de.pixart.messenger.xmpp.jid.InvalidJidException;
import de.pixart.messenger.xmpp.jid.Jid;

import static de.pixart.messenger.ui.SettingsActivity.USE_BUNDLED_EMOJIS;
import static de.pixart.messenger.ui.ShowFullscreenMessageActivity.getMimeType;

public class ConversationActivity extends XmppActivity
        implements OnAccountUpdate, OnConversationUpdate, OnRosterUpdate, OnUpdateBlocklist, XmppConnectionService.OnShowErrorToast, View.OnClickListener {

    public static final String ACTION_VIEW_CONVERSATION = "de.pixart.messenger.VIEW";
    public static final String ACTION_DESTROY_MUC = "de.pixart.messenger.DESTROY_MUC";
    public static final String CONVERSATION = "conversationUuid";
    public static final String EXTRA_DOWNLOAD_UUID = "de.pixart.messenger.download_uuid";
    public static final String TEXT = "text";
    public static final String NICK = "nick";
    public static final String PRIVATE_MESSAGE = "pm";

    public static final int REQUEST_SEND_MESSAGE = 0x0201;
    public static final int REQUEST_DECRYPT_PGP = 0x0202;
    public static final int REQUEST_ENCRYPT_MESSAGE = 0x0207;
    public static final int REQUEST_TRUST_KEYS_TEXT = 0x0208;
    public static final int REQUEST_TRUST_KEYS_MENU = 0x0209;
    public static final int REQUEST_START_DOWNLOAD = 0x0210;
    public static final int REQUEST_ADD_EDITOR_CONTENT = 0x0211;
    public static final int ATTACHMENT_CHOICE_CHOOSE_IMAGE = 0x0301;
    public static final int ATTACHMENT_CHOICE_TAKE_FROM_CAMERA = 0x0302;
    public static final int ATTACHMENT_CHOICE_CHOOSE_FILE = 0x0303;
    public static final int ATTACHMENT_CHOICE_RECORD_VOICE = 0x0304;
    public static final int ATTACHMENT_CHOICE_LOCATION = 0x0305;
    public static final int ATTACHMENT_CHOICE_CHOOSE_VIDEO = 0x0306;
    public static final int ATTACHMENT_CHOICE_INVALID = 0x0399;
    private static final String STATE_OPEN_CONVERSATION = "state_open_conversation";
    private static final String STATE_PANEL_OPEN = "state_panel_open";
    private static final String STATE_PENDING_IMAGE_URI = "state_pending_image_uri";
    private static final String STATE_PENDING_PHOTO_URI = "state_pending_photo_uri";
    private static final String STATE_FIRST_VISIBLE = "first_visible";
    private static final String STATE_OFFSET_FROM_TOP = "offset_from_top";
    final private List<Uri> mPendingImageUris = new ArrayList<>();
    final private List<Uri> mPendingPhotoUris = new ArrayList<>();
    final private List<Uri> mPendingFileUris = new ArrayList<>();
    private String mOpenConversation = null;
    private boolean mPanelOpen = true;
    private AtomicBoolean mShouldPanelBeOpen = new AtomicBoolean(false);
    private Pair<Integer, Integer> mScrollPosition = null;
    private Uri mPendingGeoUri = null;
    private boolean forbidProcessingPendings = false;
    private Message mPendingDownloadableMessage = null;

    private boolean conversationWasSelectedByKeyboard = false;

    private View mContentView;

    private List<Conversation> conversationList = new ArrayList<>();
    private Conversation swipedConversation = null;
    private Conversation mSelectedConversation = null;
    private ListView listView;
    public ConversationFragment mConversationFragment;

    private ArrayAdapter<Conversation> listAdapter;

    private boolean mActivityPaused = false;
    private AtomicBoolean mRedirected = new AtomicBoolean(false);
    private Pair<Integer, Intent> mPostponedActivityResult;
    private boolean mUnprocessedNewIntent = false;
    public Uri mPendingEditorContent = null;
    private boolean showLastSeen = false;

    long FirstStartTime = -1;

    String PREF_FIRST_START = "FirstStart";

    public Conversation getSelectedConversation() {
        return this.mSelectedConversation;
    }

    public void setSelectedConversation(Conversation conversation) {
        this.mSelectedConversation = conversation;
    }

    public void showConversationsOverview() {
        if (mConversationFragment != null) {
            mConversationFragment.stopScrolling();
            mConversationFragment.hideSearchField();
        }
        if (mContentView instanceof SlidingPaneLayout) {
            SlidingPaneLayout mSlidingPaneLayout = (SlidingPaneLayout) mContentView;
            mShouldPanelBeOpen.set(true);
            mSlidingPaneLayout.openPane();
        }
    }

    @Override
    protected String getShareableUri() {
        Conversation conversation = getSelectedConversation();
        if (conversation != null) {
            return conversation.getAccount().getShareableUri();
        } else {
            return "";
        }
    }

    public void hideConversationsOverview() {
        if (mContentView instanceof SlidingPaneLayout) {
            SlidingPaneLayout mSlidingPaneLayout = (SlidingPaneLayout) mContentView;
            mShouldPanelBeOpen.set(false);
            mSlidingPaneLayout.closePane();
        }
    }

    public boolean isConversationsOverviewHideable() {
        if (mContentView instanceof SlidingPaneLayout) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isConversationsOverviewVisable() {
        if (mContentView instanceof SlidingPaneLayout) {
            return mShouldPanelBeOpen.get();
        } else {
            return true;
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new EmojiService(this).init(useBundledEmoji());
        if (savedInstanceState != null) {
            mOpenConversation = savedInstanceState.getString(STATE_OPEN_CONVERSATION, null);
            mPanelOpen = savedInstanceState.getBoolean(STATE_PANEL_OPEN, true);
            int pos = savedInstanceState.getInt(STATE_FIRST_VISIBLE, -1);
            int offset = savedInstanceState.getInt(STATE_OFFSET_FROM_TOP, 1);
            if (pos >= 0 && offset <= 0) {
                Log.d(Config.LOGTAG, "retrieved scroll position from instanceState " + pos + ":" + offset);
                mScrollPosition = new Pair<>(pos, offset);
            } else {
                mScrollPosition = null;
            }
            String pending_image = savedInstanceState.getString(STATE_PENDING_IMAGE_URI, null);
            if (pending_image != null) {
                Log.d(Config.LOGTAG, "ConversationActivity.onCreate() - restoring pending image uri");
                mPendingImageUris.clear();
                mPendingImageUris.add(Uri.parse(pending_image));
            }
            String pending_photo = savedInstanceState.getString(STATE_PENDING_PHOTO_URI, null);
            if (pending_photo != null) {
                Log.d(Config.LOGTAG, "ConversationActivity.onCreate() - restoring pending photo uri");
                mPendingPhotoUris.clear();
                mPendingPhotoUris.add(Uri.parse(pending_photo));
            }
        }

        setContentView(R.layout.fragment_conversations_overview);

        this.mConversationFragment = new ConversationFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.selected_conversation, this.mConversationFragment, "conversation");
        transaction.commit();

        listView = findViewById(R.id.list);
        this.listAdapter = new ConversationAdapter(this, conversationList);
        listView.setAdapter(this.listAdapter);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        }

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View clickedView,
                                    int position, long arg3) {
                if (getSelectedConversation() != conversationList.get(position)) {
                    ConversationActivity.this.mConversationFragment.stopScrolling();
                    setSelectedConversation(conversationList.get(position));
                    ConversationActivity.this.mConversationFragment.reInit(getSelectedConversation());
                    conversationWasSelectedByKeyboard = false;
                }
                hideConversationsOverview();
                openConversation();
            }
        });

        mContentView = findViewById(R.id.content_view_spl);
        if (mContentView == null) {
            mContentView = findViewById(R.id.content_view_ll);
        }
        if (mContentView instanceof SlidingPaneLayout) {
            SlidingPaneLayout mSlidingPaneLayout = (SlidingPaneLayout) mContentView;
            mSlidingPaneLayout.setShadowResourceLeft(R.drawable.es_slidingpane_shadow);
            mSlidingPaneLayout.setSliderFadeColor(0);
            mSlidingPaneLayout.setPanelSlideListener(new PanelSlideListener() {

                @Override
                public void onPanelOpened(View arg0) {
                    mShouldPanelBeOpen.set(true);
                    updateActionBarTitle();
                    invalidateOptionsMenu();
                    hideKeyboard();
                    if (xmppConnectionServiceBound) {
                        xmppConnectionService.getNotificationService().setOpenConversation(null);
                    }
                    closeContextMenu();
                    mConversationFragment.hideSearchField();
                }

                @Override
                public void onPanelClosed(View arg0) {
                    mShouldPanelBeOpen.set(false);
                    openConversation();
                }

                @Override
                public void onPanelSlide(View arg0, float arg1) {
                    // TODO Auto-generated method stub

                }
            });
        }
    }

    private boolean isPackageInstalled(String targetPackage) {
        List<ApplicationInfo> packages;
        PackageManager pm;
        pm = getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(targetPackage)) return true;
        }
        return false;
    }

    protected void AppUpdate() {
        String PREFS_NAME = "UpdateTimeStamp";
        SharedPreferences UpdateTimeStamp = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUpdateTime = UpdateTimeStamp.getLong("lastUpdateTime", 0);
        Log.d(Config.LOGTAG, "AppUpdater: LastUpdateTime: " + lastUpdateTime);
        if ((lastUpdateTime + (Config.UPDATE_CHECK_TIMER * 1000)) < System.currentTimeMillis()) {
            lastUpdateTime = System.currentTimeMillis();
            SharedPreferences.Editor editor = UpdateTimeStamp.edit();
            editor.putLong("lastUpdateTime", lastUpdateTime);
            editor.commit();
            Log.d(Config.LOGTAG, "AppUpdater: CurrentTime: " + lastUpdateTime);
            UpdateService task = new UpdateService(this);
            task.executeOnExecutor(UpdateService.THREAD_POOL_EXECUTOR, "false");
            Log.d(Config.LOGTAG, "AppUpdater started");
        } else {
            Log.d(Config.LOGTAG, "AppUpdater stopped");
            return;
        }
    }

    @Override
    public void switchToConversation(Conversation conversation) {
        setSelectedConversation(conversation);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ConversationActivity.this.mConversationFragment.reInit(getSelectedConversation());
                openConversation();
            }
        });
    }

    private void updateActionBarTitle() {
        updateActionBarTitle(isConversationsOverviewHideable() && !isConversationsOverviewVisable());
    }

    private void updateActionBarTitle(boolean titleShouldBeName) {
        final ActionBar ab = getActionBar();
        final Conversation conversation = getSelectedConversation();
        if (ab != null) {
            if (titleShouldBeName && conversation != null) {
                if ((ab.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != ActionBar.DISPLAY_HOME_AS_UP) {
                    ab.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
                }
                ab.setDisplayShowTitleEnabled(false);
                ab.setDisplayShowCustomEnabled(true);
                ab.setCustomView(R.layout.ab_title);
                TextView abtitle = findViewById(android.R.id.text1);
                TextView absubtitle = findViewById(android.R.id.text2);
                abtitle.setText(conversation.getName());
                abtitle.setOnClickListener(this);
                abtitle.setSelected(true);
                if (conversation.getMode() == Conversation.MODE_SINGLE && !this.getSelectedConversation().withSelf()) {
                    ChatState state = conversation.getIncomingChatState();
                    if (conversation.getContact().getShownStatus() == Presence.Status.OFFLINE) {
                        absubtitle.setText(getString(R.string.account_status_offline));
                        absubtitle.setSelected(true);
                        absubtitle.setOnClickListener(this);
                    } else {
                        if (state == ChatState.COMPOSING) {
                            absubtitle.setText(getString(R.string.is_typing));
                            absubtitle.setTypeface(null, Typeface.BOLD_ITALIC);
                            absubtitle.setSelected(true);
                            absubtitle.setOnClickListener(this);
                        } else {
                            if (showLastSeen && conversation.getContact().getLastseen() > 0) {
                                absubtitle.setText(UIHelper.lastseen(getApplicationContext(), conversation.getContact().isActive(), conversation.getContact().getLastseen()));
                            } else {
                                absubtitle.setText(getString(R.string.account_status_online));
                            }
                            absubtitle.setSelected(true);
                            absubtitle.setOnClickListener(this);
                        }
                    }
                } else {
                    if (conversation.getParticipants() != null) {
                        ChatState state = ChatState.COMPOSING;
                        List<MucOptions.User> userWithChatStates = conversation.getMucOptions().getUsersWithChatState(state, 5);
                        if (userWithChatStates.size() == 0) {
                            state = ChatState.PAUSED;
                            userWithChatStates = conversation.getMucOptions().getUsersWithChatState(state, 5);
                        }
                        List<MucOptions.User> users = conversation.getMucOptions().getUsers(true);
                        if (state == ChatState.COMPOSING) {
                            if (userWithChatStates.size() > 0) {
                                if (userWithChatStates.size() == 1) {
                                    MucOptions.User user = userWithChatStates.get(0);
                                    absubtitle.setText(getString(R.string.contact_is_typing, UIHelper.getDisplayName(user)));
                                } else {
                                    StringBuilder builder = new StringBuilder();
                                    for (MucOptions.User user : userWithChatStates) {
                                        if (builder.length() != 0) {
                                            builder.append(", ");
                                        }
                                        builder.append(UIHelper.getDisplayName(user));
                                    }
                                    absubtitle.setText(getString(R.string.contacts_are_typing, builder.toString()));
                                }
                            }
                        } else {
                            if (users.size() == 1) {
                                absubtitle.setText(getString(R.string.one_participant));
                            } else {
                                absubtitle.setText(getString(R.string.more_participants, users.size()));
                            }
                        }
                        absubtitle.setSelected(true);
                        absubtitle.setOnClickListener(this);
                    } else {
                        absubtitle.setText(R.string.no_participants);
                        abtitle.setSelected(true);
                        absubtitle.setOnClickListener(this);
                    }
                }

            } else {
                if ((ab.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) == ActionBar.DISPLAY_HOME_AS_UP) {
                    ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
                }
                ab.setDisplayShowTitleEnabled(true);
                ab.setDisplayShowCustomEnabled(false);
                ab.setTitle(R.string.app_name);
                ab.setSubtitle(null);
            }
        }
    }

    private void openConversation() {
        this.updateActionBarTitle();
        this.invalidateOptionsMenu();
        if (xmppConnectionServiceBound) {
            final Conversation conversation = getSelectedConversation();
            xmppConnectionService.getNotificationService().setOpenConversation(conversation);
            sendReadMarkerIfNecessary(conversation);
        }
        listAdapter.notifyDataSetChanged();
    }

    public void sendReadMarkerIfNecessary(final Conversation conversation) {
        if (!mActivityPaused && !mUnprocessedNewIntent && conversation != null) {
            xmppConnectionService.sendReadMarker(conversation);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conversations, menu);
        final MenuItem menuSecure = menu.findItem(R.id.action_security);
        final MenuItem menuArchiveChat = menu.findItem(R.id.action_archive_chat);
        final MenuItem menuArchiveMuc = menu.findItem(R.id.action_archive_muc);
        final MenuItem menuAttach = menu.findItem(R.id.action_attach_file);
        final MenuItem menuClearHistory = menu.findItem(R.id.action_clear_history);
        final MenuItem menuAdd = menu.findItem(R.id.action_add);
        final MenuItem menuInviteContact = menu.findItem(R.id.action_invite);
        final MenuItem menuUpdater = menu.findItem(R.id.action_check_updates);
        final MenuItem menuInviteUser = menu.findItem(R.id.action_invite_user);
        final MenuItem menuSearchHistory = menu.findItem(R.id.action_search_history);

        if (isConversationsOverviewVisable() && isConversationsOverviewHideable()) {
            menuArchiveChat.setVisible(false);
            menuArchiveMuc.setVisible(false);
            menuSecure.setVisible(false);
            menuInviteContact.setVisible(false);
            menuAttach.setVisible(false);
            menuClearHistory.setVisible(false);
            menuSearchHistory.setVisible(false);
            if (xmppConnectionService.installedFromFDroid()) {
                menuUpdater.setVisible(false);
            } else {
                menuUpdater.setVisible(true);
            }
        } else {
            menuAdd.setVisible(!isConversationsOverviewHideable());
            //hide settings, accounts and updater in all menus except in main window
            menuUpdater.setVisible(false);
            menuInviteUser.setVisible(false);

            if (this.getSelectedConversation() != null) {
                if (this.getSelectedConversation().getMode() == Conversation.MODE_SINGLE) {
                    menuArchiveMuc.setVisible(false);
                } else {
                    menuArchiveChat.setVisible(false);
                }
                if (this.getSelectedConversation().getNextEncryption() != Message.ENCRYPTION_NONE) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        menuSecure.setIcon(R.drawable.ic_lock_white_24dp);
                    } else {
                        menuSecure.setIcon(R.drawable.ic_action_secure);
                    }
                }
                if (this.getSelectedConversation().getMode() == Conversation.MODE_MULTI) {
                    menuAttach.setVisible(getSelectedConversation().getAccount().httpUploadAvailable() && getSelectedConversation().getMucOptions().participating());
                    menuInviteContact.setVisible(getSelectedConversation().getMucOptions().canInvite());
                    menuSecure.setVisible((Config.supportOpenPgp() || Config.supportOmemo()) && Config.multipleEncryptionChoices()); //only if pgp is supported we have a choice
                } else {
                    menuSecure.setVisible(Config.multipleEncryptionChoices());
                    menuInviteContact.setVisible(xmppConnectionService != null && xmppConnectionService.findConferenceServer(getSelectedConversation().getAccount()) != null);
                }
            }
        }
        if (Config.supportOmemo()) {
            new Handler().post(addOmemoDebuggerRunnable);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private Runnable addOmemoDebuggerRunnable = new Runnable() {
        @Override
        public void run() {
            View view = findViewById(R.id.action_security);
            if (view != null) {
                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        return v.getId() == R.id.action_security && quickOmemoDebugger(getSelectedConversation());
                    }
                });
            }
        }
    };

    private boolean quickOmemoDebugger(Conversation c) {
        if (c != null) {
            boolean single = c.getMode() == Conversation.MODE_SINGLE;
            AxolotlService axolotlService = c.getAccount().getAxolotlService();
            Pair<AxolotlService.AxolotlCapability, Jid> capabilityJidPair = axolotlService.isConversationAxolotlCapableDetailed(c);
            switch (capabilityJidPair.first) {
                case MISSING_PRESENCE:
                    Toast.makeText(ConversationActivity.this, single ? getString(R.string.missing_presence_subscription) : getString(R.string.missing_presence_subscription_with_x, capabilityJidPair.second.toBareJid().toString()), Toast.LENGTH_SHORT).show();
                    return true;
                case MISSING_KEYS:
                    Toast.makeText(ConversationActivity.this, single ? getString(R.string.missing_omemo_keys) : getString(R.string.missing_keys_from_x, capabilityJidPair.second.toBareJid().toString()), Toast.LENGTH_SHORT).show();
                    return true;
                case WRONG_CONFIGURATION:
                    Toast.makeText(ConversationActivity.this, R.string.wrong_conference_configuration, Toast.LENGTH_SHORT).show();
                    return true;
                case NO_MEMBERS:
                    Toast.makeText(ConversationActivity.this, R.string.this_conference_has_no_members, Toast.LENGTH_SHORT).show();
                    return true;
            }
        }
        return false;
    }

    protected void selectPresenceToAttachFile(final int attachmentChoice, final int encryption) {
        final Conversation conversation = getSelectedConversation();
        final Account account = conversation.getAccount();
        final OnPresenceSelected callback = new OnPresenceSelected() {

            @Override
            public void onPresenceSelected() {
                final Intent intent = new Intent();
                boolean chooser = false;
                String fallbackPackageId = null;
                switch (attachmentChoice) {
                    case ATTACHMENT_CHOICE_CHOOSE_IMAGE:
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        }
                        intent.setType("image/*");
                        chooser = true;
                        break;
                    case ATTACHMENT_CHOICE_CHOOSE_VIDEO:
                        chooser = true;
                        intent.setType("video/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        break;
                    case ATTACHMENT_CHOICE_TAKE_FROM_CAMERA:
                        AlertDialog.Builder builder = new AlertDialog.Builder(ConversationActivity.this);
                        builder.setTitle(getString(R.string.attach_take_from_camera));
                        builder.setNegativeButton(getString(R.string.action_take_photo),
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Uri uri = xmppConnectionService.getFileBackend().getTakePhotoUri();
                                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                                        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                                        mPendingPhotoUris.clear();
                                        mPendingPhotoUris.add(uri);
                                        startActivityForResult(intent, attachmentChoice);
                                    }
                                });
                        builder.setPositiveButton(getString(R.string.action_take_video),
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Uri uri = xmppConnectionService.getFileBackend().getTakeVideoUri();
                                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        intent.setAction(MediaStore.ACTION_VIDEO_CAPTURE);
                                        mPendingFileUris.clear();
                                        mPendingFileUris.add(uri);
                                        startActivityForResult(intent, attachmentChoice);
                                    }
                                });
                        builder.create().show();
                        break;
                    case ATTACHMENT_CHOICE_CHOOSE_FILE:
                        chooser = true;
                        intent.setType("*/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        break;
                    case ATTACHMENT_CHOICE_RECORD_VOICE:
                        startActivityForResult(new Intent(getApplicationContext(), RecordingActivity.class), attachmentChoice);
                        break;
                    case ATTACHMENT_CHOICE_LOCATION:
                        startActivityForResult(new Intent(getApplicationContext(), ShareLocationActivity.class), attachmentChoice);
                        break;
                }
                if (intent.resolveActivity(getPackageManager()) != null) {
                    Log.d(Config.LOGTAG, "Attachment: " + attachmentChoice);
                    if (chooser) {
                        startActivityForResult(
                                Intent.createChooser(intent, getString(R.string.perform_action_with)),
                                attachmentChoice);
                    } else {
                        startActivityForResult(intent, attachmentChoice);
                    }
                } else if (fallbackPackageId != null) {
                    startActivity(getInstallApkIntent(fallbackPackageId));
                }
            }
        };
        if ((account.httpUploadAvailable() || attachmentChoice == ATTACHMENT_CHOICE_LOCATION) && encryption != Message.ENCRYPTION_OTR) {
            conversation.setNextCounterpart(null);
            callback.onPresenceSelected();
        } else {
            selectPresence(conversation, callback);
        }
    }

    private Intent getInstallApkIntent(final String packageId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + packageId));
        if (intent.resolveActivity(getPackageManager()) != null) {
            return intent;
        } else {
            intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + packageId));
            return intent;
        }
    }

    public void attachFile(final int attachmentChoice) {
        if (attachmentChoice != ATTACHMENT_CHOICE_LOCATION) {
            if (!hasStoragePermission(attachmentChoice)) {
                return;
            }
        }
        if (attachmentChoice == ATTACHMENT_CHOICE_RECORD_VOICE) {
            if (!hasMicPermission(attachmentChoice)) {
                return;
            }
        }
        if (attachmentChoice == ATTACHMENT_CHOICE_LOCATION) {
            if (!hasLocationPermission(attachmentChoice)) {
                return;
            }
        }
        switch (attachmentChoice) {
            case ATTACHMENT_CHOICE_LOCATION:
                getPreferences().edit().putString("recently_used_quick_action", "location").apply();
                break;
            case ATTACHMENT_CHOICE_RECORD_VOICE:
                getPreferences().edit().putString("recently_used_quick_action", "voice").apply();
                break;
            case ATTACHMENT_CHOICE_TAKE_FROM_CAMERA:
                getPreferences().edit().putString("recently_used_quick_action", "photo").apply();
                break;
            case ATTACHMENT_CHOICE_CHOOSE_IMAGE:
                getPreferences().edit().putString("recently_used_quick_action", "picture").apply();
                break;
        }
        final Conversation conversation = getSelectedConversation();
        final int encryption = conversation.getNextEncryption();
        final int mode = conversation.getMode();
        if (encryption == Message.ENCRYPTION_PGP) {
            if (hasPgp()) {
                if (mode == Conversation.MODE_SINGLE && conversation.getContact().getPgpKeyId() != 0) {
                    xmppConnectionService.getPgpEngine().hasKey(
                            conversation.getContact(),
                            new UiCallback<Contact>() {

                                @Override
                                public void userInputRequried(PendingIntent pi, Contact contact) {
                                    ConversationActivity.this.runIntent(pi, attachmentChoice);
                                }

                                @Override
                                public void success(Contact contact) {
                                    selectPresenceToAttachFile(attachmentChoice, encryption);
                                }

                                @Override
                                public void error(int error, Contact contact) {
                                    replaceToast(getString(error));
                                }
                            });
                } else if (mode == Conversation.MODE_MULTI && conversation.getMucOptions().pgpKeysInUse()) {
                    if (!conversation.getMucOptions().everybodyHasKeys()) {
                        Toast warning = Toast
                                .makeText(this,
                                        R.string.missing_public_keys,
                                        Toast.LENGTH_LONG);
                        warning.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        warning.show();
                    }
                    selectPresenceToAttachFile(attachmentChoice, encryption);
                } else {
                    final ConversationFragment fragment = (ConversationFragment) getFragmentManager()
                            .findFragmentByTag("conversation");
                    if (fragment != null) {
                        fragment.showNoPGPKeyDialog(false,
                                new OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        conversation.setNextEncryption(Message.ENCRYPTION_NONE);
                                        xmppConnectionService.updateConversation(conversation);
                                        selectPresenceToAttachFile(attachmentChoice, Message.ENCRYPTION_NONE);
                                    }
                                });
                    }
                }
            } else {
                showInstallPgpDialog();
            }
        } else {
            if (encryption != Message.ENCRYPTION_AXOLOTL || !trustKeysIfNeeded(REQUEST_TRUST_KEYS_MENU, attachmentChoice)) {
                selectPresenceToAttachFile(attachmentChoice, encryption);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0)
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestCode == REQUEST_START_DOWNLOAD) {
                    if (this.mPendingDownloadableMessage != null) {
                        startDownloadable(this.mPendingDownloadableMessage);
                    }
                } else if (requestCode == REQUEST_ADD_EDITOR_CONTENT) {
                    if (this.mPendingEditorContent != null) {
                        attachImageToConversation(this.mPendingEditorContent);
                    }
                } else {
                    attachFile(requestCode);
                }
            } else {
                Toast.makeText(this, R.string.no_permission, Toast.LENGTH_SHORT).show();
            }
    }

    public void startDownloadable(Message message) {
        if (!hasStoragePermission(ConversationActivity.REQUEST_START_DOWNLOAD)) {
            this.mPendingDownloadableMessage = message;
            return;
        }
        Transferable transferable = message.getTransferable();
        if (transferable != null) {
            if (!transferable.start()) {
                Toast.makeText(this, R.string.not_connected_try_again, Toast.LENGTH_SHORT).show();
            }
        } else if (message.treatAsDownloadable()) {
            xmppConnectionService.getHttpConnectionManager().createNewDownloadConnection(message, true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            showConversationsOverview();
            return true;
        } else if (item.getItemId() == R.id.action_add) {
            startActivity(new Intent(this, StartConversationActivity.class));
            return true;
        } else if (getSelectedConversation() != null) {
            switch (item.getItemId()) {
                case R.id.action_attach_file:
                    attachFileDialog();
                    break;
                case R.id.action_archive_chat:
                    this.endConversation(getSelectedConversation());
                    break;
                case R.id.action_archive_muc:
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.action_end_conversation_muc));
                    builder.setMessage(getString(R.string.leave_conference_warning));
                    builder.setNegativeButton(getString(R.string.cancel), null);
                    builder.setPositiveButton(getString(R.string.action_end_conversation_muc),
                            new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    endConversation(getSelectedConversation());
                                }
                            });
                    builder.create().show();
                    break;
                case R.id.action_invite:
                    inviteToConversation(getSelectedConversation());
                    break;
                case R.id.action_security:
                    selectEncryptionDialog(getSelectedConversation());
                    break;
                case R.id.action_clear_history:
                    clearHistoryDialog(getSelectedConversation());
                    break;
                case R.id.action_block:
                    BlockContactDialog.show(this, getSelectedConversation());
                    break;
                case R.id.action_unblock:
                    BlockContactDialog.show(this, getSelectedConversation());
                    break;
                case R.id.action_search_history:
                    mConversationFragment.showSearchField();
                    break;
                default:
                    break;
            }
            return super.onOptionsItemSelected(item);
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void endConversation(Conversation conversation) {
        endConversation(conversation, true, true);
    }

    public void endConversation(Conversation conversation, boolean showOverview, boolean reinit) {
        if (showOverview) {
            showConversationsOverview();
        }
        xmppConnectionService.archiveConversation(conversation);
        if (reinit) {
            if (conversationList.size() > 0) {
                setSelectedConversation(conversationList.get(0));
                this.mConversationFragment.reInit(getSelectedConversation());
            } else {
                setSelectedConversation(null);
                if (mRedirected.compareAndSet(false, true)) {
                    Intent intent = new Intent(this, StartConversationActivity.class);
                    intent.putExtra("init", true);
                    startActivity(intent);
                    finish();
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    protected void clearHistoryDialog(final Conversation conversation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.clear_conversation_history));
        View dialogView = getLayoutInflater().inflate(
                R.layout.dialog_clear_history, null);
        final CheckBox endConversationCheckBox = dialogView
                .findViewById(R.id.end_conversation_checkbox);
        if (conversation.getMode() == Conversation.MODE_SINGLE) {
            endConversationCheckBox.setVisibility(View.VISIBLE);
            endConversationCheckBox.setChecked(true);
        }
        builder.setView(dialogView);
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.setPositiveButton(getString(R.string.delete_messages),
                new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ConversationActivity.this.xmppConnectionService.clearConversationHistory(conversation);
                        if (conversation.getMode() == Conversation.MODE_SINGLE) {
                            if (endConversationCheckBox.isChecked()) {
                                endConversation(conversation);
                            } else {
                                updateConversationList();
                                ConversationActivity.this.mConversationFragment.updateMessages();
                            }
                        } else {
                            updateConversationList();
                            ConversationActivity.this.mConversationFragment.updateMessages();
                        }
                    }
                });
        builder.create().show();
    }

    protected void attachFileDialog() {
        View menuAttachFile = findViewById(R.id.action_attach_file);
        if (menuAttachFile == null) {
            return;
        }
        PopupMenu attachFilePopup = new PopupMenu(this, menuAttachFile);
        attachFilePopup.inflate(R.menu.attachment_choices);
        attachFilePopup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.attach_choose_picture:
                        attachFile(ATTACHMENT_CHOICE_CHOOSE_IMAGE);
                        break;
                    case R.id.attach_take_picture:
                        attachFile(ATTACHMENT_CHOICE_TAKE_FROM_CAMERA);
                        break;
                    case R.id.attach_choose_video:
                        attachFile(ATTACHMENT_CHOICE_CHOOSE_VIDEO);
                        break;
                    case R.id.attach_choose_file:
                        attachFile(ATTACHMENT_CHOICE_CHOOSE_FILE);
                        break;
                    case R.id.attach_record_voice:
                        attachFile(ATTACHMENT_CHOICE_RECORD_VOICE);
                        break;
                    case R.id.attach_location:
                        attachFile(ATTACHMENT_CHOICE_LOCATION);
                        break;
                }
                return false;
            }
        });
        UIHelper.showIconsInPopup(attachFilePopup);
        attachFilePopup.show();
    }

    public void verifyOtrSessionDialog(final Conversation conversation, View view) {
        if (!conversation.hasValidOtrSession() || conversation.getOtrSession().getSessionStatus() != SessionStatus.ENCRYPTED) {
            Toast.makeText(this, R.string.otr_session_not_started, Toast.LENGTH_LONG).show();
            return;
        }
        if (view == null) {
            return;
        }
        PopupMenu popup = new PopupMenu(this, view);
        popup.inflate(R.menu.verification_choices);
        popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(ConversationActivity.this, VerifyOTRActivity.class);
                intent.setAction(VerifyOTRActivity.ACTION_VERIFY_CONTACT);
                intent.putExtra("contact", conversation.getContact().getJid().toBareJid().toString());
                intent.putExtra(EXTRA_ACCOUNT, conversation.getAccount().getJid().toBareJid().toString());
                switch (menuItem.getItemId()) {
                    case R.id.scan_fingerprint:
                        intent.putExtra("mode", VerifyOTRActivity.MODE_SCAN_FINGERPRINT);
                        break;
                    case R.id.ask_question:
                        intent.putExtra("mode", VerifyOTRActivity.MODE_ASK_QUESTION);
                        break;
                    case R.id.manual_verification:
                        intent.putExtra("mode", VerifyOTRActivity.MODE_MANUAL_VERIFICATION);
                        break;
                }
                startActivity(intent);
                return true;
            }
        });
        popup.show();
    }

    protected void selectEncryptionDialog(final Conversation conversation) {
        View menuItemView = findViewById(R.id.action_security);
        if (menuItemView == null) {
            return;
        }
        PopupMenu popup = new PopupMenu(this, menuItemView);
        final ConversationFragment fragment = (ConversationFragment) getFragmentManager()
                .findFragmentByTag("conversation");
        if (fragment != null) {
            popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.encryption_choice_none:
                            conversation.setNextEncryption(Message.ENCRYPTION_NONE);
                            item.setChecked(true);
                            break;
                        case R.id.encryption_choice_otr:
                            conversation.setNextEncryption(Message.ENCRYPTION_OTR);
                            item.setChecked(true);
                            break;
                        case R.id.encryption_choice_pgp:
                            if (hasPgp()) {
                                if (conversation.getAccount().getPgpSignature() != null) {
                                    conversation.setNextEncryption(Message.ENCRYPTION_PGP);
                                    item.setChecked(true);
                                } else {
                                    announcePgp(conversation.getAccount(), conversation, null, onOpenPGPKeyPublished);
                                }
                            } else {
                                showInstallPgpDialog();
                            }
                            break;
                        case R.id.encryption_choice_axolotl:
                            Log.d(Config.LOGTAG, AxolotlService.getLogprefix(conversation.getAccount())
                                    + "Enabled axolotl for Contact " + conversation.getContact().getJid());
                            conversation.setNextEncryption(Message.ENCRYPTION_AXOLOTL);
                            item.setChecked(true);
                            break;
                        default:
                            conversation.setNextEncryption(Message.ENCRYPTION_NONE);
                            break;
                    }
                    xmppConnectionService.updateConversation(conversation);
                    fragment.updateChatMsgHint();
                    invalidateOptionsMenu();
                    refreshUi();
                    return true;
                }
            });
            popup.inflate(R.menu.encryption_choices);
            MenuItem otr = popup.getMenu().findItem(R.id.encryption_choice_otr);
            MenuItem none = popup.getMenu().findItem(R.id.encryption_choice_none);
            MenuItem pgp = popup.getMenu().findItem(R.id.encryption_choice_pgp);
            MenuItem axolotl = popup.getMenu().findItem(R.id.encryption_choice_axolotl);
            pgp.setVisible(Config.supportOpenPgp());
            none.setVisible(Config.supportUnencrypted() || conversation.getMode() == Conversation.MODE_MULTI);
            otr.setVisible(Config.supportOtr());
            axolotl.setVisible(Config.supportOmemo());
            if (conversation.getMode() == Conversation.MODE_MULTI) {
                otr.setVisible(false);
            }
            if (!conversation.getAccount().getAxolotlService().isConversationAxolotlCapable(conversation)) {
                axolotl.setEnabled(false);
            }
            switch (conversation.getNextEncryption()) {
                case Message.ENCRYPTION_NONE:
                    none.setChecked(true);
                    break;
                case Message.ENCRYPTION_OTR:
                    otr.setChecked(true);
                    break;
                case Message.ENCRYPTION_PGP:
                    pgp.setChecked(true);
                    break;
                case Message.ENCRYPTION_AXOLOTL:
                    axolotl.setChecked(true);
                    break;
                default:
                    none.setChecked(true);
                    break;
            }
            popup.show();
        }
    }

    @Override
    public void onBackPressed() {
        if (!isConversationsOverviewVisable() && mConversationFragment.isSearchFieldVisible()) {
            mConversationFragment.hideSearchField();
        } else if (!isConversationsOverviewVisable()) {
            showConversationsOverview();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyUp(int key, KeyEvent event) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        final int upKey;
        final int downKey;
        switch (rotation) {
            case Surface.ROTATION_90:
                upKey = KeyEvent.KEYCODE_DPAD_LEFT;
                downKey = KeyEvent.KEYCODE_DPAD_RIGHT;
                break;
            case Surface.ROTATION_180:
                upKey = KeyEvent.KEYCODE_DPAD_DOWN;
                downKey = KeyEvent.KEYCODE_DPAD_UP;
                break;
            case Surface.ROTATION_270:
                upKey = KeyEvent.KEYCODE_DPAD_RIGHT;
                downKey = KeyEvent.KEYCODE_DPAD_LEFT;
                break;
            case Surface.ROTATION_0:
            default:
                upKey = KeyEvent.KEYCODE_DPAD_UP;
                downKey = KeyEvent.KEYCODE_DPAD_DOWN;
        }
        final boolean modifier = event.isCtrlPressed() || (event.getMetaState() & KeyEvent.META_ALT_LEFT_ON) != 0;
        if (modifier && key == KeyEvent.KEYCODE_TAB && isConversationsOverviewHideable()) {
            toggleConversationsOverview();
            return true;
        } else if (modifier && key == KeyEvent.KEYCODE_SPACE) {
            startActivity(new Intent(this, StartConversationActivity.class));
            return true;
        } else if (modifier && key == downKey) {
            if (isConversationsOverviewHideable() && !isConversationsOverviewVisable()) {
                showConversationsOverview();
            }
            return selectDownConversation();
        } else if (modifier && key == upKey) {
            if (isConversationsOverviewHideable() && !isConversationsOverviewVisable()) {
                showConversationsOverview();
            }
            return selectUpConversation();
        } else if (modifier && key == KeyEvent.KEYCODE_1) {
            return openConversationByIndex(0);
        } else if (modifier && key == KeyEvent.KEYCODE_2) {
            return openConversationByIndex(1);
        } else if (modifier && key == KeyEvent.KEYCODE_3) {
            return openConversationByIndex(2);
        } else if (modifier && key == KeyEvent.KEYCODE_4) {
            return openConversationByIndex(3);
        } else if (modifier && key == KeyEvent.KEYCODE_5) {
            return openConversationByIndex(4);
        } else if (modifier && key == KeyEvent.KEYCODE_6) {
            return openConversationByIndex(5);
        } else if (modifier && key == KeyEvent.KEYCODE_7) {
            return openConversationByIndex(6);
        } else if (modifier && key == KeyEvent.KEYCODE_8) {
            return openConversationByIndex(7);
        } else if (modifier && key == KeyEvent.KEYCODE_9) {
            return openConversationByIndex(8);
        } else if (modifier && key == KeyEvent.KEYCODE_0) {
            return openConversationByIndex(9);
        } else {
            return super.onKeyUp(key, event);
        }
    }

    private void toggleConversationsOverview() {
        if (isConversationsOverviewVisable()) {
            hideConversationsOverview();
            if (mConversationFragment != null) {
                mConversationFragment.setFocusOnInputField();
            }
        } else {
            showConversationsOverview();
        }
    }

    private boolean selectUpConversation() {
        if (this.mSelectedConversation != null) {
            int index = this.conversationList.indexOf(this.mSelectedConversation);
            if (index > 0) {
                return openConversationByIndex(index - 1);
            }
        }
        return false;
    }

    private boolean selectDownConversation() {
        if (this.mSelectedConversation != null) {
            int index = this.conversationList.indexOf(this.mSelectedConversation);
            if (index != -1 && index < this.conversationList.size() - 1) {
                return openConversationByIndex(index + 1);
            }
        }
        return false;
    }

    private boolean openConversationByIndex(int index) {
        try {
            this.conversationWasSelectedByKeyboard = true;
            this.mConversationFragment.stopScrolling();
            setSelectedConversation(this.conversationList.get(index));
            this.mConversationFragment.reInit(getSelectedConversation());
            if (index > listView.getLastVisiblePosition() - 1 || index < listView.getFirstVisiblePosition() + 1) {
                this.listView.setSelection(index);
            }
            openConversation();
            return true;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        if (intent != null && ACTION_VIEW_CONVERSATION.equals(intent.getAction())) {
            mOpenConversation = null;
            mUnprocessedNewIntent = true;
            if (xmppConnectionServiceBound) {
                handleViewConversationIntent(intent);
                intent.setAction(Intent.ACTION_MAIN);
            } else {
                setIntent(intent);
            }
        } else if (intent != null && ACTION_DESTROY_MUC.equals(intent.getAction())) {
            final Bundle extras = intent.getExtras();
            if (extras != null && extras.containsKey("MUC_UUID")) {
                Log.d(Config.LOGTAG, "Get " + intent.getAction() + " intent for " + extras.getString("MUC_UUID"));
                Conversation conversation = xmppConnectionService.findConversationByUuid(extras.getString("MUC_UUID"));
                ConversationActivity.this.xmppConnectionService.clearConversationHistory(conversation);
                xmppConnectionService.destroyMuc(conversation);
                endConversation(conversation);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mRedirected.set(false);
        if (this.xmppConnectionServiceBound) {
            this.onBackendConnected();
        }
        if (conversationList.size() >= 1) {
            this.onConversationUpdate();
        }
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.showLastSeen = preferences.getBoolean("last_activity", false);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mActivityPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        final int theme = findTheme();
        final boolean usingEnterKey = usingEnterKey();
        if (this.mTheme != theme || usingEnterKey != mUsingEnterKey) {
            recreate();
        }
        this.mActivityPaused = false;

        if (!isConversationsOverviewVisable() || !isConversationsOverviewHideable()) {
            sendReadMarkerIfNecessary(getSelectedConversation());
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {
        Conversation conversation = getSelectedConversation();
        if (conversation != null) {
            savedInstanceState.putString(STATE_OPEN_CONVERSATION, conversation.getUuid());
            Pair<Integer, Integer> scrollPosition = mConversationFragment.getScrollPosition();
            if (scrollPosition != null) {
                savedInstanceState.putInt(STATE_FIRST_VISIBLE, scrollPosition.first);
                savedInstanceState.putInt(STATE_OFFSET_FROM_TOP, scrollPosition.second);
            }
        } else {
            savedInstanceState.remove(STATE_OPEN_CONVERSATION);
        }
        savedInstanceState.putBoolean(STATE_PANEL_OPEN, isConversationsOverviewVisable());
        if (this.mPendingImageUris.size() >= 1) {
            Log.d(Config.LOGTAG, "ConversationActivity.onSaveInstanceState() - saving pending image uri");
            savedInstanceState.putString(STATE_PENDING_IMAGE_URI, this.mPendingImageUris.get(0).toString());
        } else if (this.mPendingPhotoUris.size() >= 1) {
            savedInstanceState.putString(STATE_PENDING_PHOTO_URI, this.mPendingPhotoUris.get(0).toString());
        } else {
            savedInstanceState.remove(STATE_PENDING_IMAGE_URI);
            savedInstanceState.remove(STATE_PENDING_PHOTO_URI);
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    private void clearPending() {
        mPendingImageUris.clear();
        mPendingFileUris.clear();
        mPendingPhotoUris.clear();
        mPendingGeoUri = null;
        mPostponedActivityResult = null;
    }

    private void redirectToStartConversationActivity(boolean noAnimation) {
        Account pendingAccount = xmppConnectionService.getPendingAccount();
        if (pendingAccount == null) {
            Intent startConversationActivity = new Intent(this, StartConversationActivity.class);
            startConversationActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            if (noAnimation) {
                startConversationActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }
            startConversationActivity.putExtra("init", true);
            startActivity(startConversationActivity);
            if (noAnimation) {
                overridePendingTransition(0, 0);
            }
        } else {
            switchToAccount(pendingAccount, true);
        }
    }

    @Override
    void onBackendConnected() {
        this.xmppConnectionService.getNotificationService().setIsInForeground(true);
        updateConversationList();
        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (extras != null && extras.containsKey(PREF_FIRST_START)) {
                FirstStartTime = extras.getLong(PREF_FIRST_START);
                Log.d(Config.LOGTAG, "Get first start time from StartUI: " + FirstStartTime);
            }
        } else {
            FirstStartTime = System.currentTimeMillis();
            Log.d(Config.LOGTAG, "Device is running Android < SDK 23, no restart required: " + FirstStartTime);
        }

        if (mPendingConferenceInvite != null) {
            if (mPendingConferenceInvite.execute(this)) {
                mToast = Toast.makeText(this, R.string.creating_conference, Toast.LENGTH_LONG);
                mToast.show();
            }
            mPendingConferenceInvite = null;
        }

        if (FirstStartTime == 0) {
            Log.d(Config.LOGTAG, "First start time: " + FirstStartTime + ", restarting App");
            //write first start timestamp to file
            FirstStartTime = System.currentTimeMillis();
            SharedPreferences FirstStart = getApplicationContext().getSharedPreferences(PREF_FIRST_START, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = FirstStart.edit();
            editor.putLong(PREF_FIRST_START, FirstStartTime);
            editor.commit();
            // restart
            Intent restartintent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
            restartintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            restartintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(restartintent);
            System.exit(0);
        }

        if (xmppConnectionService.getAccounts().size() == 0) {
            if (mRedirected.compareAndSet(false, true)) {
                if (Config.X509_VERIFICATION) {
                    Intent redirectionIntent = new Intent(this, ManageAccountActivity.class);
                    redirectionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(redirectionIntent);
                    overridePendingTransition(0, 0);
                } else if (Config.MAGIC_CREATE_DOMAIN != null) {
                    WelcomeActivity.launch(this);
                } else {
                    Intent editAccount = new Intent(this, EditAccountActivity.class);
                    editAccount.putExtra("init", true);
                    editAccount.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(editAccount);
                    overridePendingTransition(0, 0);
                }
            }
        } else if (conversationList.size() <= 0) {
            if (mRedirected.compareAndSet(false, true)) {
                redirectToStartConversationActivity(true);
            }
        } else if (selectConversationByUuid(mOpenConversation)) {
            if (mPanelOpen) {
                showConversationsOverview();
            } else {
                if (isConversationsOverviewHideable()) {
                    openConversation();
                    updateActionBarTitle(true);
                }
            }
            if (this.mConversationFragment.reInit(getSelectedConversation())) {
                Log.d(Config.LOGTAG, "setting scroll position on fragment");
                this.mConversationFragment.setScrollPosition(mScrollPosition);
            }
            mOpenConversation = null;
        } else if (intent != null && ACTION_VIEW_CONVERSATION.equals(intent.getAction())) {
            clearPending();
            handleViewConversationIntent(intent);
            intent.setAction(Intent.ACTION_MAIN);
        } else if (getSelectedConversation() == null) {
            reInitLatestConversation();
        } else {
            this.mConversationFragment.messageListAdapter.updatePreferences();
            this.mConversationFragment.messagesView.invalidateViews();
            this.mConversationFragment.setupIme();
        }

        if (xmppConnectionService.getAccounts().size() != 0) {
            if (xmppConnectionService.hasInternetConnection()) {
                if (xmppConnectionService.isWIFI() || (xmppConnectionService.isMobile() && !xmppConnectionService.isMobileRoaming())) {
                    if (!xmppConnectionService.installedFromFDroid()) {
                        AppUpdate();
                    }
                }
            }
        }

        if (this.mPostponedActivityResult != null) {
            this.onActivityResult(mPostponedActivityResult.first, RESULT_OK, mPostponedActivityResult.second);
        }

        final boolean stopping = isStopping();

        if (!forbidProcessingPendings) {
            int ImageUrisCount = mPendingImageUris.size();
            if (ImageUrisCount == 1) {
                Uri uri = mPendingImageUris.get(0);
                Log.d(Config.LOGTAG, "ConversationActivity.onBackendConnected() - attaching image to conversations. stopping=" + Boolean.toString(stopping));
                attachImageToConversation(getSelectedConversation(), uri, false);
            } else {
                for (Iterator<Uri> i = mPendingImageUris.iterator(); i.hasNext(); i.remove()) {
                    Uri foo = i.next();
                    Log.d(Config.LOGTAG, "ConversationActivity.onBackendConnected() - attaching images to conversations. stopping=" + Boolean.toString(stopping));
                    attachImagesToConversation(getSelectedConversation(), foo);
                }
            }

            for (Iterator<Uri> i = mPendingPhotoUris.iterator(); i.hasNext(); i.remove()) {
                Log.d(Config.LOGTAG, "ConversationActivity.onBackendConnected() - attaching photo to conversations. stopping=" + Boolean.toString(stopping));
                attachPhotoToConversation(getSelectedConversation(), i.next());
            }

            for (Iterator<Uri> i = mPendingFileUris.iterator(); i.hasNext(); i.remove()) {
                Log.d(Config.LOGTAG, "ConversationActivity.onBackendConnected() - attaching file to conversations. stopping=" + Boolean.toString(stopping));
                attachFileToConversation(getSelectedConversation(), i.next());
            }

            if (mPendingGeoUri != null) {
                attachLocationToConversation(getSelectedConversation(), mPendingGeoUri);
                mPendingGeoUri = null;
            }
        }
        forbidProcessingPendings = false;

        if (!ExceptionHelper.checkForCrash(this, this.xmppConnectionService) && !mRedirected.get()) {
            openBatteryOptimizationDialogIfNeeded();
            if (!xmppConnectionService.installedFromFDroid() && !xmppConnectionService.installedFromPlayStore()) {
                openInstallFromUnknownSourcesDialogIfNeeded();
            }
        }

        if (isConversationsOverviewVisable() && isConversationsOverviewHideable()) {
            xmppConnectionService.getNotificationService().setOpenConversation(null);
        } else {
            xmppConnectionService.getNotificationService().setOpenConversation(getSelectedConversation());
        }
    }

    private boolean isStopping() {
        if (Build.VERSION.SDK_INT >= 17) {
            return isFinishing() || isDestroyed();
        } else {
            return isFinishing();
        }
    }

    private void reInitLatestConversation() {
        showConversationsOverview();
        clearPending();
        setSelectedConversation(conversationList.get(0));
        this.mConversationFragment.reInit(getSelectedConversation());
    }

    private void handleViewConversationIntent(final Intent intent) {
        final String uuid = intent.getStringExtra(CONVERSATION);
        final String downloadUuid = intent.getStringExtra(EXTRA_DOWNLOAD_UUID);
        final String text = intent.getStringExtra(TEXT);
        final String nick = intent.getStringExtra(NICK);
        final boolean pm = intent.getBooleanExtra(PRIVATE_MESSAGE, false);
        this.mConversationFragment.stopScrolling();
        if (selectConversationByUuid(uuid)) {
            this.mConversationFragment.reInit(getSelectedConversation());
            if (nick != null) {
                if (pm) {
                    Jid jid = getSelectedConversation().getJid();
                    try {
                        Jid next = Jid.fromParts(jid.getLocalpart(), jid.getDomainpart(), nick);
                        this.mConversationFragment.privateMessageWith(next);
                    } catch (final InvalidJidException ignored) {
                        //do nothing
                    }
                } else {
                    this.mConversationFragment.highlightInConference(nick);
                }
            } else {
                this.mConversationFragment.appendText(text);
            }
            hideConversationsOverview();
            mUnprocessedNewIntent = false;
            openConversation();
            if (mContentView instanceof SlidingPaneLayout) {
                updateActionBarTitle(true); //fixes bug where slp isn't properly closed yet
            }
            if (downloadUuid != null) {
                final Message message = mSelectedConversation.findMessageWithFileAndUuid(downloadUuid);
                if (message != null) {
                    startDownloadable(message);
                }
            } else {
                mUnprocessedNewIntent = false;
            }
        }
    }

    private boolean selectConversationByUuid(String uuid) {
        if (uuid == null) {
            return false;
        }
        for (Conversation aConversationList : conversationList) {
            if (aConversationList.getUuid().equals(uuid)) {
                setSelectedConversation(aConversationList);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void unregisterListeners() {
        super.unregisterListeners();
        xmppConnectionService.getNotificationService().setOpenConversation(null);
    }

    @SuppressLint("NewApi")
    private static List<Uri> extractUriFromIntent(final Intent intent) {
        List<Uri> uris = new ArrayList<>();
        if (intent == null) {
            return uris;
        }
        Uri uri = intent.getData();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && uri == null) {
            final ClipData clipData = intent.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); ++i) {
                    uris.add(clipData.getItemAt(i).getUri());
                }
            }
        } else {
            uris.add(uri);
        }
        return uris;
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_DECRYPT_PGP) {
                mConversationFragment.onActivityResult(requestCode, resultCode, data);
            } else if (requestCode == REQUEST_CHOOSE_PGP_ID) {
                // the user chose OpenPGP for encryption and selected his key in the PGP provider
                if (xmppConnectionServiceBound) {
                    if (data.getExtras().containsKey(OpenPgpApi.EXTRA_SIGN_KEY_ID)) {
                        // associate selected PGP keyId with the account
                        mSelectedConversation.getAccount().setPgpSignId(data.getExtras().getLong(OpenPgpApi.EXTRA_SIGN_KEY_ID));
                        // we need to announce the key as described in XEP-027
                        announcePgp(mSelectedConversation.getAccount(), null, null, onOpenPGPKeyPublished);
                    } else {
                        choosePgpSignId(mSelectedConversation.getAccount());
                    }
                    this.mPostponedActivityResult = null;
                } else {
                    this.mPostponedActivityResult = new Pair<>(requestCode, data);
                }
            } else if (requestCode == REQUEST_ANNOUNCE_PGP) {
                if (xmppConnectionServiceBound) {
                    announcePgp(mSelectedConversation.getAccount(), mSelectedConversation, data, onOpenPGPKeyPublished);
                    this.mPostponedActivityResult = null;
                } else {
                    this.mPostponedActivityResult = new Pair<>(requestCode, data);
                }
            } else if (requestCode == ATTACHMENT_CHOICE_CHOOSE_IMAGE) {
                mPendingImageUris.clear();
                mPendingImageUris.addAll(extractUriFromIntent(data));
                int ImageUrisCount = mPendingImageUris.size();
                if (xmppConnectionServiceBound) {
                    if (ImageUrisCount == 1) {
                        Uri uri = mPendingImageUris.get(0);
                        Log.d(Config.LOGTAG, "ConversationActivity.onActivityResult() - attaching image to conversations. CHOOSE_IMAGE");
                        attachImageToConversation(getSelectedConversation(), uri, false);
                    } else {
                        for (Iterator<Uri> i = mPendingImageUris.iterator(); i.hasNext(); i.remove()) {
                            Log.d(Config.LOGTAG, "ConversationActivity.onActivityResult() - attaching images to conversations. CHOOSE_IMAGES");
                            attachImagesToConversation(getSelectedConversation(), i.next());
                        }
                    }
                }
            } else if (requestCode == ATTACHMENT_CHOICE_CHOOSE_FILE
                    || requestCode == ATTACHMENT_CHOICE_RECORD_VOICE
                    || requestCode == ATTACHMENT_CHOICE_CHOOSE_VIDEO
                    || requestCode == ATTACHMENT_CHOICE_TAKE_FROM_CAMERA) {
                final List<Uri> uris = extractUriFromIntent(data);
                final Conversation c = getSelectedConversation();
                final OnPresenceSelected callback = new OnPresenceSelected() {
                    @Override
                    public void onPresenceSelected() {
                        mPendingFileUris.clear();
                        mPendingFileUris.addAll(uris);
                        if (xmppConnectionServiceBound) {
                            for (Iterator<Uri> i = mPendingFileUris.iterator(); i.hasNext(); i.remove()) {
                                if (requestCode == ATTACHMENT_CHOICE_TAKE_FROM_CAMERA && getMimeType(i.next().toString()).contains("image")) {
                                    return;
                                }
                                Log.d(Config.LOGTAG, "ConversationActivity.onActivityResult() - attaching file to conversations. CHOOSE_FILE/RECORD_VOICE");
                                attachFileToConversation(c, i.next());
                            }
                        }
                    }
                };
                if (c == null || c.getMode() == Conversation.MODE_MULTI
                        || FileBackend.allFilesUnderSize(this, uris, getMaxHttpUploadSize(c))
                        || c.getNextEncryption() == Message.ENCRYPTION_OTR) {
                    callback.onPresenceSelected();
                } else {
                    selectPresence(c, callback);
                }
            } else if (requestCode == ATTACHMENT_CHOICE_TAKE_FROM_CAMERA) {
                if (mPendingPhotoUris.size() == 1) {
                    Uri uri = FileBackend.getIndexableTakePhotoUri(mPendingPhotoUris.get(0));
                    mPendingPhotoUris.set(0, uri);
                    if (xmppConnectionServiceBound) {
                        Log.d(Config.LOGTAG, "ConversationActivity.onActivityResult() - attaching photo to conversations. TAKE_FROM_CAMERA");
                        attachPhotoToConversation(getSelectedConversation(), uri);
                        mPendingPhotoUris.clear();
                    }
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(uri);
                    sendBroadcast(intent);
                } else {
                    mPendingPhotoUris.clear();
                }
            } else if (requestCode == ATTACHMENT_CHOICE_LOCATION) {
                double latitude = data.getDoubleExtra("latitude", 0);
                double longitude = data.getDoubleExtra("longitude", 0);
                this.mPendingGeoUri = Uri.parse("geo:" + String.valueOf(latitude) + "," + String.valueOf(longitude));
                if (xmppConnectionServiceBound) {
                    attachLocationToConversation(getSelectedConversation(), mPendingGeoUri);
                    this.mPendingGeoUri = null;
                }
            } else if (requestCode == REQUEST_TRUST_KEYS_TEXT || requestCode == REQUEST_TRUST_KEYS_MENU) {
                this.forbidProcessingPendings = true;
                if (xmppConnectionServiceBound) {
                    mConversationFragment.onActivityResult(requestCode, resultCode, data);
                    this.mPostponedActivityResult = null;
                } else {
                    this.mPostponedActivityResult = new Pair<>(requestCode, data);
                }

            }
        } else {
            mPendingImageUris.clear();
            mPendingFileUris.clear();
            mPendingPhotoUris.clear();
            if (requestCode == ConversationActivity.REQUEST_DECRYPT_PGP) {
                mConversationFragment.onActivityResult(requestCode, resultCode, data);
            }
            if (requestCode == REQUEST_BATTERY_OP) {
                setNeverAskForBatteryOptimizationsAgain();
            }
        }
    }

    private long getMaxHttpUploadSize(Conversation conversation) {
        final XmppConnection connection = conversation.getAccount().getXmppConnection();
        return connection == null ? -1 : connection.getFeatures().getMaxHttpUploadSize();
    }

    private String getBatteryOptimizationPreferenceKey() {
        @SuppressLint("HardwareIds") String device = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return "show_battery_optimization" + (device == null ? "" : device);
    }

    private void setNeverAskForBatteryOptimizationsAgain() {
        getPreferences().edit().putBoolean(getBatteryOptimizationPreferenceKey(), false).apply();
    }

    private void openBatteryOptimizationDialogIfNeeded() {
        if (hasAccountWithoutPush()
                && isOptimizingBattery()
                && getPreferences().getBoolean(getBatteryOptimizationPreferenceKey(), true)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.battery_optimizations_enabled);
            builder.setMessage(R.string.battery_optimizations_enabled_dialog);
            builder.setPositiveButton(R.string.next, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    Uri uri = Uri.parse("package:" + getPackageName());
                    intent.setData(uri);
                    try {
                        startActivityForResult(intent, REQUEST_BATTERY_OP);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(ConversationActivity.this, R.string.device_does_not_support_battery_op, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        setNeverAskForBatteryOptimizationsAgain();
                    }
                });
            }
            builder.create().show();
        }
    }

    private void openInstallFromUnknownSourcesDialogIfNeeded() {
        final PackageManager packageManager = this.getPackageManager();
        boolean installFromUnknownSource = false;
        int isUnknownAllowed = 0;
        if (Build.VERSION.SDK_INT >= 26) {
            /*
            * On Android 8 with applications targeting lower versions,
            * it's impossible to check unknown sources enabled: using old APIs will always return true
            * and using the new one will always return false,
            * so in order to avoid a stuck dialog that can't be bypassed we will assume true.
            */
            installFromUnknownSource = this.getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.O
                    || packageManager.canRequestPackageInstalls();
        } else if (Build.VERSION.SDK_INT >= 17 && Build.VERSION.SDK_INT < 26) {
            try {
                isUnknownAllowed = Settings.Global.getInt(this.getApplicationContext().getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS);
            } catch (Settings.SettingNotFoundException e) {
                isUnknownAllowed = 0;
                e.printStackTrace();
            }
            installFromUnknownSource = isUnknownAllowed == 1;
        } else {
            try {
                isUnknownAllowed = Settings.Secure.getInt(this.getApplicationContext().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS);
            } catch (Settings.SettingNotFoundException e) {
                isUnknownAllowed = 0;
                e.printStackTrace();
            }
            installFromUnknownSource = isUnknownAllowed == 1;
        }
        Log.d(Config.LOGTAG, "Install from unknown sources for Android SDK " + Build.VERSION.SDK_INT + " allowed: " + installFromUnknownSource);

        if (!installFromUnknownSource) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.install_from_unknown_sources_disabled);
            builder.setMessage(R.string.install_from_unknown_sources_disabled_dialog);
            builder.setPositiveButton(R.string.next, (dialog, which) -> {
                Intent intent = null;
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    Uri uri = Uri.parse("package:" + getPackageName());
                    intent.setData(uri);
                } else {
                    intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                }
                Log.d(Config.LOGTAG, "Allow install from unknown sources for Android SDK " + Build.VERSION.SDK_INT + " intent " + intent.toString());
                try {
                    startActivityForResult(intent, REQUEST_UNKNOWN_SOURCE_OP);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(ConversationActivity.this, R.string.device_does_not_support_battery_op, Toast.LENGTH_SHORT).show();
                }
            });
            builder.create().show();
        }
    }

    private boolean hasAccountWithoutPush() {
        for (Account account : xmppConnectionService.getAccounts()) {
            if (account.getStatus() == Account.State.ONLINE && !xmppConnectionService.getPushManagementService().available(account)) {
                return true;
            }
        }
        return false;
    }

    private void attachLocationToConversation(Conversation conversation, Uri uri) {
        if (conversation == null) {
            return;
        }
        xmppConnectionService.attachLocationToConversation(conversation, uri, new UiCallback<Message>() {

            @Override
            public void success(Message message) {
                xmppConnectionService.sendMessage(message);
            }

            @Override
            public void error(int errorCode, Message object) {

            }

            @Override
            public void userInputRequried(PendingIntent pi, Message object) {

            }
        });
    }

    private void attachFileToConversation(Conversation conversation, Uri uri) {
        if (conversation == null) {
            return;
        }
        final Toast prepareFileToast = Toast.makeText(getApplicationContext(), getText(R.string.preparing_file), Toast.LENGTH_LONG);
        prepareFileToast.show();
        delegateUriPermissionsToService(uri);
        xmppConnectionService.attachFileToConversation(conversation, uri, new UiInformableCallback<Message>() {
            @Override
            public void inform(final String text) {
                hidePrepareFileToast(prepareFileToast);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        replaceToast(text);
                    }
                });
            }

            @Override
            public void success(Message message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideToast();
                    }
                });
                hidePrepareFileToast(prepareFileToast);
                xmppConnectionService.sendMessage(message);
            }

            @Override
            public void error(final int errorCode, Message message) {
                hidePrepareFileToast(prepareFileToast);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        replaceToast(getString(errorCode));
                    }
                });

            }

            @Override
            public void userInputRequried(PendingIntent pi, Message message) {
                hidePrepareFileToast(prepareFileToast);
            }
        });
    }

    private void attachPhotoToConversation(Conversation conversation, Uri uri) {
        if (conversation == null) {
            return;
        }
        final Toast prepareFileToast = Toast.makeText(getApplicationContext(), getText(R.string.preparing_image), Toast.LENGTH_LONG);
        prepareFileToast.show();
        delegateUriPermissionsToService(uri);
        xmppConnectionService.attachImageToConversation(conversation, uri,
                new UiCallback<Message>() {

                    @Override
                    public void userInputRequried(PendingIntent pi, Message object) {
                        hidePrepareFileToast(prepareFileToast);
                    }

                    @Override
                    public void success(Message message) {
                        hidePrepareFileToast(prepareFileToast);
                        xmppConnectionService.sendMessage(message);
                    }

                    @Override
                    public void error(final int error, Message message) {
                        hidePrepareFileToast(prepareFileToast);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                replaceToast(getString(error));
                            }
                        });
                    }
                });
    }

    private void attachImagesToConversation(Conversation conversation, Uri uri) {
        if (conversation == null) {
            return;
        }
        final Toast prepareFileToast = Toast.makeText(getApplicationContext(), getText(R.string.preparing_image), Toast.LENGTH_LONG);
        prepareFileToast.show();
        delegateUriPermissionsToService(uri);
        xmppConnectionService.attachImageToConversation(conversation, uri,
                new UiCallback<Message>() {

                    @Override
                    public void userInputRequried(PendingIntent pi, Message object) {
                        hidePrepareFileToast(prepareFileToast);
                    }

                    @Override
                    public void success(Message message) {
                        hidePrepareFileToast(prepareFileToast);
                        xmppConnectionService.sendMessage(message);
                    }

                    @Override
                    public void error(final int error, Message message) {
                        hidePrepareFileToast(prepareFileToast);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                replaceToast(getString(error));
                            }
                        });
                    }
                });
    }

    public void attachImageToConversation(Uri uri) {
        this.attachImageToConversation(getSelectedConversation(), uri, true);
    }

    private void attachImageToConversation(Conversation conversation, Uri uri, boolean sendAsIs) {
        if (conversation == null) {
            return;
        }
        if (sendAsIs) {
            sendImage(conversation, uri);
            return;
        }
        final Conversation conversation_preview = conversation;
        final Uri uri_preview = uri;
        Bitmap bitmap = BitmapFactory.decodeFile(FileUtils.getPath(this, uri));
        File file = null;
        ExifInterface exif = null;
        int orientation = 0;
        try {
            file = new File(FileUtils.getPath(this, uri));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (file != null) {
            try {
                exif = new ExifInterface(file.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        }
        Log.d(Config.LOGTAG, "EXIF: " + orientation);
        Bitmap rotated_image = null;
        Log.d(Config.LOGTAG, "Rotate image");
        rotated_image = FileBackend.rotateBitmap(file, bitmap, orientation);
        if (rotated_image != null) {
            int scaleSize = 600;
            int originalWidth = rotated_image.getWidth();
            int originalHeight = rotated_image.getHeight();
            int newWidth = -1;
            int newHeight = -1;
            float multFactor;
            if (originalHeight > originalWidth) {
                newHeight = scaleSize;
                multFactor = (float) originalWidth / (float) originalHeight;
                newWidth = (int) (newHeight * multFactor);
            } else if (originalWidth > originalHeight) {
                newWidth = scaleSize;
                multFactor = (float) originalHeight / (float) originalWidth;
                newHeight = (int) (newWidth * multFactor);
            } else if (originalHeight == originalWidth) {
                newHeight = scaleSize;
                newWidth = scaleSize;
            }
            Log.d(Config.LOGTAG, "Scaling preview image from " + originalHeight + "px x " + originalWidth + "px to " + newHeight + "px x " + newWidth + "px");
            Bitmap preview = Bitmap.createScaledBitmap(rotated_image, newWidth, newHeight, false);
            ImageView ImagePreview = new ImageView(this);
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            ImagePreview.setLayoutParams(vp);
            ImagePreview.setMaxWidth(newWidth);
            ImagePreview.setMaxHeight(newHeight);
            //ImagePreview.setScaleType(ImageView.ScaleType.FIT_XY);
            //ImagePreview.setAdjustViewBounds(true);
            ImagePreview.setPadding(5, 5, 5, 5);
            ImagePreview.setImageBitmap(preview);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(ImagePreview);
            builder.setTitle(R.string.send_image);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    sendImage(conversation_preview, uri_preview);
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mPendingImageUris.clear();
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mPendingImageUris.clear();
                    }
                });
            }
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        } else {
            Toast.makeText(getApplicationContext(), getText(R.string.error_file_not_found), Toast.LENGTH_LONG).show();
        }
    }

    private void sendImage(Conversation conversation, Uri uri) {
        final Toast prepareFileToast = Toast.makeText(getApplicationContext(), getText(R.string.preparing_image), Toast.LENGTH_LONG);
        prepareFileToast.show();
        xmppConnectionService.attachImageToConversation(conversation, uri,
                new UiCallback<Message>() {

                    @Override
                    public void userInputRequried(PendingIntent pi, Message object) {
                        hidePrepareFileToast(prepareFileToast);
                    }

                    @Override
                    public void success(Message message) {
                        hidePrepareFileToast(prepareFileToast);
                        xmppConnectionService.sendMessage(message);
                    }

                    @Override
                    public void error(final int error, Message message) {
                        hidePrepareFileToast(prepareFileToast);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                replaceToast(getString(error));
                            }
                        });
                    }
                });
    }

    private void hidePrepareFileToast(final Toast prepareFileToast) {
        if (prepareFileToast != null) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    prepareFileToast.cancel();
                }
            });
        }
    }

    public void updateConversationList() {
        xmppConnectionService.populateWithOrderedConversations(conversationList);
        if (!conversationList.contains(mSelectedConversation)) {
            mSelectedConversation = null;
        }
        if (swipedConversation != null) {
            if (swipedConversation.isRead()) {
                conversationList.remove(swipedConversation);
            }
        }
        listAdapter.notifyDataSetChanged();
    }

    public void runIntent(PendingIntent pi, int requestCode) {
        try {
            this.startIntentSenderForResult(pi.getIntentSender(), requestCode,
                    null, 0, 0, 0);
        } catch (final SendIntentException ignored) {
        }
    }

    public void encryptTextMessage(Message message) {
        xmppConnectionService.getPgpEngine().encrypt(message,
                new UiCallback<Message>() {

                    @Override
                    public void userInputRequried(PendingIntent pi, Message message) {
                        ConversationActivity.this.runIntent(pi, ConversationActivity.REQUEST_SEND_MESSAGE);
                    }

                    @Override
                    public void success(Message message) {
                        message.setEncryption(Message.ENCRYPTION_DECRYPTED);
                        xmppConnectionService.sendMessage(message);
                        runOnUiThread(new Runnable() {
 							@Override
 							public void run() {
                                mConversationFragment.messageSent();
                            }
                        });
                    }

                    @Override
                    public void error(final int error, Message message) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mConversationFragment.doneSendingPgpMessage();
                                Toast.makeText(ConversationActivity.this,
                                        R.string.unable_to_connect_to_keychain,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
                    }
                });
    }

    public boolean useSendButtonToIndicateStatus() {
        return getPreferences().getBoolean("send_button_status", getResources().getBoolean(R.bool.send_button_status));
    }

    public boolean indicateReceived() {
        return getPreferences().getBoolean("indicate_received", getResources().getBoolean(R.bool.indicate_received));
    }

    public boolean useWhiteBackground() {
        return getPreferences().getBoolean("use_white_background", getResources().getBoolean(R.bool.use_white_background));
    }

    public boolean useBundledEmoji() {
        return getPreferences().getBoolean(USE_BUNDLED_EMOJIS, getResources().getBoolean(R.bool.use_bundled_emoji));
    }

    protected boolean trustKeysIfNeeded(int requestCode) {
        return trustKeysIfNeeded(requestCode, ATTACHMENT_CHOICE_INVALID);
    }

    protected boolean trustKeysIfNeeded(int requestCode, int attachmentChoice) {
        AxolotlService axolotlService = mSelectedConversation.getAccount().getAxolotlService();
        final List<Jid> targets = axolotlService.getCryptoTargets(mSelectedConversation);
        boolean hasUnaccepted = !mSelectedConversation.getAcceptedCryptoTargets().containsAll(targets);
        boolean hasUndecidedOwn = !axolotlService.getKeysWithTrust(FingerprintStatus.createActiveUndecided()).isEmpty();
        boolean hasUndecidedContacts = !axolotlService.getKeysWithTrust(FingerprintStatus.createActiveUndecided(), targets).isEmpty();
        boolean hasPendingKeys = !axolotlService.findDevicesWithoutSession(mSelectedConversation).isEmpty();
        boolean hasNoTrustedKeys = axolotlService.anyTargetHasNoTrustedKeys(targets);
        if (hasUndecidedOwn || hasUndecidedContacts || hasPendingKeys || hasNoTrustedKeys || hasUnaccepted) {
            axolotlService.createSessionsIfNeeded(mSelectedConversation);
            Intent intent = new Intent(getApplicationContext(), TrustKeysActivity.class);
            String[] contacts = new String[targets.size()];
            for (int i = 0; i < contacts.length; ++i) {
                contacts[i] = targets.get(i).toString();
            }
            intent.putExtra("contacts", contacts);
            intent.putExtra(EXTRA_ACCOUNT, mSelectedConversation.getAccount().getJid().toBareJid().toString());
            intent.putExtra("choice", attachmentChoice);
            intent.putExtra("conversation", mSelectedConversation.getUuid());
            startActivityForResult(intent, requestCode);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void refreshUiReal() {
        updateConversationList();
        if (conversationList.size() > 0) {
            if (!this.mConversationFragment.isAdded()) {
                Log.d(Config.LOGTAG, "fragment NOT added to activity. detached=" + Boolean.toString(mConversationFragment.isDetached()));
            }
            if (getSelectedConversation() == null) {
                reInitLatestConversation();
            } else {
                ConversationActivity.this.mConversationFragment.updateMessages();
                updateActionBarTitle();
                invalidateOptionsMenu();
            }
        } else {
            if (!isStopping() && mRedirected.compareAndSet(false, true)) {
                redirectToStartConversationActivity(false);
            }
            Log.d(Config.LOGTAG, "not updating conversations fragment because conversations list size was 0");
        }
    }

    @Override
    public void onAccountUpdate() {
        this.refreshUi();
    }

    @Override
    public void onConversationUpdate() {
        this.refreshUi();
    }

    @Override
    public void onRosterUpdate() {
        this.refreshUi();
    }

    @Override
    public void OnUpdateBlocklist(Status status) {
        this.refreshUi();
    }

    public void unblockConversation(final Blockable conversation) {
        xmppConnectionService.sendUnblockRequest(conversation);
    }

    public boolean enterIsSend() {
        return getPreferences().getBoolean("enter_is_send", getResources().getBoolean(R.bool.enter_is_send));
    }

    @Override
    public void onShowErrorToast(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ConversationActivity.this, resId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean highlightSelectedConversations() {
        return !isConversationsOverviewHideable() || this.conversationWasSelectedByKeyboard;
    }

    @Override
    public void onClick(View view) {
        final Conversation conversation = getSelectedConversation();
        if (conversation.getMode() == Conversation.MODE_SINGLE) {
            switchToContactDetails(getSelectedConversation().getContact());
        } else if (conversation.getMode() == Conversation.MODE_MULTI) {
            Intent intent = new Intent(this,
                    ConferenceDetailsActivity.class);
            intent.setAction(ConferenceDetailsActivity.ACTION_VIEW_MUC);
            intent.putExtra("uuid", getSelectedConversation().getUuid());
            startActivity(intent);
        }
    }
}
