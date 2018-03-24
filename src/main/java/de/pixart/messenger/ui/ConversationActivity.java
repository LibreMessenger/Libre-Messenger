package de.pixart.messenger.ui;

import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v4.widget.SlidingPaneLayout.PanelSlideListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.entities.Account;
import de.pixart.messenger.entities.Conversation;
import de.pixart.messenger.entities.Message;
import de.pixart.messenger.entities.MucOptions;
import de.pixart.messenger.entities.Presence;
import de.pixart.messenger.services.EmojiService;
import de.pixart.messenger.services.UpdateService;
import de.pixart.messenger.services.XmppConnectionService;
import de.pixart.messenger.services.XmppConnectionService.OnAccountUpdate;
import de.pixart.messenger.services.XmppConnectionService.OnConversationUpdate;
import de.pixart.messenger.services.XmppConnectionService.OnRosterUpdate;
import de.pixart.messenger.ui.adapter.ConversationAdapter;
import de.pixart.messenger.utils.ExceptionHelper;
import de.pixart.messenger.utils.UIHelper;
import de.pixart.messenger.xmpp.OnUpdateBlocklist;
import de.pixart.messenger.xmpp.chatstate.ChatState;
import de.pixart.messenger.xmpp.jid.InvalidJidException;
import de.pixart.messenger.xmpp.jid.Jid;

import static de.pixart.messenger.ui.SettingsActivity.USE_BUNDLED_EMOJIS;

public class ConversationActivity extends XmppActivity
        implements OnAccountUpdate, OnConversationUpdate, OnRosterUpdate, OnUpdateBlocklist, XmppConnectionService.OnShowErrorToast, View.OnClickListener {

    public static final String ACTION_VIEW_CONVERSATION = "de.pixart.messenger.VIEW";
    public static final String ACTION_DESTROY_MUC = "de.pixart.messenger.DESTROY_MUC";
    public static final String CONVERSATION = "conversationUuid";
    public static final String EXTRA_DOWNLOAD_UUID = "de.pixart.messenger.download_uuid";
    public static final String TEXT = "text";
    public static final String NICK = "nick";
    public static final String PRIVATE_MESSAGE = "pm";

    private static final String STATE_OPEN_CONVERSATION = "state_open_conversation";
    private static final String STATE_PANEL_OPEN = "state_panel_open";
    private static final String STATE_PENDING_IMAGE_URI = "state_pending_image_uri";
    private static final String STATE_PENDING_PHOTO_URI = "state_pending_photo_uri";
    private static final String STATE_FIRST_VISIBLE = "first_visible";
    private static final String STATE_OFFSET_FROM_TOP = "offset_from_top";

    private String mOpenConversation = null;
    private boolean mPanelOpen = true;
    private AtomicBoolean mShouldPanelBeOpen = new AtomicBoolean(false);
    private Pair<Integer, Integer> mScrollPosition = null;
    private boolean forbidProcessingPendings = false;

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
    private boolean mUnprocessedNewIntent = false;
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

    @SuppressLint("ClickableViewAccessibility")
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
        }

        setContentView(R.layout.fragment_conversations_overview);

        this.mConversationFragment = new ConversationFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        //transaction.replace(R.id.selected_conversation, this.mConversationFragment, "conversation");
        transaction.commit();

        listView = findViewById(R.id.list);
        this.listAdapter = new ConversationAdapter(this, conversationList);
        listView.setAdapter(this.listAdapter);

        final ActionBar actionBar = getSupportActionBar();
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

        //mContentView = findViewById(R.id.content_view_spl);
        if (mContentView == null) {
            //mContentView = findViewById(R.id.content_view_ll);
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

    public boolean useBundledEmoji() {
        return getPreferences().getBoolean(USE_BUNDLED_EMOJIS, getResources().getBoolean(R.bool.use_bundled_emoji));
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

    protected void AppUpdate(boolean PlayStore) {
        if (PlayStore) {
            return;
        }
        String PREFS_NAME = "UpdateTimeStamp";
        SharedPreferences UpdateTimeStamp = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUpdateTime = UpdateTimeStamp.getLong("lastUpdateTime", 0);
        Log.d(Config.LOGTAG, "AppUpdater: LastUpdateTime: " + lastUpdateTime);
        if ((lastUpdateTime + (Config.UPDATE_CHECK_TIMER * 1000)) < System.currentTimeMillis()) {
            lastUpdateTime = System.currentTimeMillis();
            SharedPreferences.Editor editor = UpdateTimeStamp.edit();
            editor.putLong("lastUpdateTime", lastUpdateTime);
            editor.apply();
            Log.d(Config.LOGTAG, "AppUpdater: CurrentTime: " + lastUpdateTime);
            if (!installFromUnknownSourceAllowed() && !PlayStore) {
                openInstallFromUnknownSourcesDialogIfNeeded();
            } else {
                UpdateService task = new UpdateService(this, PlayStore);
                task.executeOnExecutor(UpdateService.THREAD_POOL_EXECUTOR, "false");
                Log.d(Config.LOGTAG, "AppUpdater started");
            }
        } else {
            Log.d(Config.LOGTAG, "AppUpdater stopped");
            return;
        }
    }

    @Override
    public void switchToConversation(Conversation conversation) {
        setSelectedConversation(conversation);
        runOnUiThread(() -> {
            ConversationActivity.this.mConversationFragment.reInit(getSelectedConversation());
            openConversation();
        });
    }

    private void updateActionBarTitle() {
        updateActionBarTitle(isConversationsOverviewHideable() && !isConversationsOverviewVisable());
    }

    private void updateActionBarTitle(boolean titleShouldBeName) {
        final ActionBar ab = getSupportActionBar();
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            showConversationsOverview();
            return true;
        } else if (item.getItemId() == R.id.action_add) {
            startActivity(new Intent(this, StartConversationActivity.class));
            return true;
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
        /*if (this.mPendingImageUris.size() >= 1) {
            Log.d(Config.LOGTAG, "ConversationActivity.onSaveInstanceState() - saving pending image uri");
            savedInstanceState.putString(STATE_PENDING_IMAGE_URI, this.mPendingImageUris.get(0).toString());
        } else if (this.mPendingPhotoUris.size() >= 1) {
            savedInstanceState.putString(STATE_PENDING_PHOTO_URI, this.mPendingPhotoUris.get(0).toString());
        } else {
            savedInstanceState.remove(STATE_PENDING_IMAGE_URI);
            savedInstanceState.remove(STATE_PENDING_PHOTO_URI);
        }*/
        super.onSaveInstanceState(savedInstanceState);
    }

    private void clearPending() {
        mConversationFragment.clearPending();
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
            //this.mConversationFragment.messagesView.invalidateViews();
            this.mConversationFragment.setupIme();
        }

        if (xmppConnectionService.getAccounts().size() != 0) {
            if (xmppConnectionService.hasInternetConnection()) {
                if (xmppConnectionService.isWIFI() || (xmppConnectionService.isMobile() && !xmppConnectionService.isMobileRoaming())) {
                    if (!xmppConnectionService.installedFromFDroid()) {
                        AppUpdate(xmppConnectionService.installedFromPlayStore());
                    }
                }
            }
        }

        mConversationFragment.onBackendConnected();

        if (!ExceptionHelper.checkForCrash(this, this.xmppConnectionService) && !mRedirected.get()) {
            openBatteryOptimizationDialogIfNeeded();
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
                    //startDownloadable(message);
                }
            }
        } else {
            mUnprocessedNewIntent = false;

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

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            if (requestCode == REQUEST_BATTERY_OP) {
                setNeverAskForBatteryOptimizationsAgain();
            }
        }
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
            builder.setPositiveButton(R.string.next, (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                Uri uri = Uri.parse("package:" + getPackageName());
                intent.setData(uri);
                try {
                    startActivityForResult(intent, REQUEST_BATTERY_OP);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(ConversationActivity.this, R.string.device_does_not_support_battery_op, Toast.LENGTH_SHORT).show();
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                builder.setOnDismissListener(dialog -> setNeverAskForBatteryOptimizationsAgain());
            }
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
                ConversationActivity.this.mConversationFragment.refresh();
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

    @Override
    public void onShowErrorToast(final int resId) {
        runOnUiThread(() -> Toast.makeText(ConversationActivity.this, resId, Toast.LENGTH_SHORT).show());
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
