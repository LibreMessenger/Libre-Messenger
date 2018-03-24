/*
 * Copyright (c) 2018, Daniel Gultsch All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.pixart.messenger.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import net.java.otr4j.session.SessionStatus;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.databinding.ActivityConversationsBinding;
import de.pixart.messenger.entities.Conversation;
import de.pixart.messenger.services.EmojiService;
import de.pixart.messenger.services.XmppConnectionService;
import de.pixart.messenger.ui.interfaces.OnConversationArchived;
import de.pixart.messenger.ui.interfaces.OnConversationRead;
import de.pixart.messenger.ui.interfaces.OnConversationSelected;
import de.pixart.messenger.ui.interfaces.OnConversationsListItemUpdated;
import de.pixart.messenger.xmpp.OnUpdateBlocklist;

import static de.pixart.messenger.ui.SettingsActivity.USE_BUNDLED_EMOJIS;

public class ConversationsMainActivity extends XmppActivity implements OnConversationSelected, OnConversationArchived, OnConversationsListItemUpdated, OnConversationRead, XmppConnectionService.OnAccountUpdate, XmppConnectionService.OnConversationUpdate, XmppConnectionService.OnRosterUpdate, OnUpdateBlocklist, XmppConnectionService.OnShowErrorToast {


    //secondary fragment (when holding the conversation, must be initialized before refreshing the overview fragment
    private static final @IdRes
    int[] FRAGMENT_ID_NOTIFICATION_ORDER = {R.id.secondary_fragment, R.id.main_fragment};

    private ActivityConversationsBinding binding;

    @Override
    protected void refreshUiReal() {
        for(@IdRes int id : FRAGMENT_ID_NOTIFICATION_ORDER) {
            refreshFragment(id);
        }
    }

    @Override
    void onBackendConnected() {
        for (@IdRes int id : FRAGMENT_ID_NOTIFICATION_ORDER) {
            notifyFragmentOfBackendConnected(id);
        }
        invalidateActionBarTitle();
    }

    private void notifyFragmentOfBackendConnected(@IdRes int id) {
        final Fragment fragment = getFragmentManager().findFragmentById(id);
        if (fragment != null && fragment instanceof XmppFragment) {
            ((XmppFragment) fragment).onBackendConnected();
        }
    }

    private void refreshFragment(@IdRes int id) {
        final Fragment fragment = getFragmentManager().findFragmentById(id);
        if (fragment != null && fragment instanceof XmppFragment) {
            ((XmppFragment) fragment).refresh();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new EmojiService(this).init(useBundledEmoji());
        this.binding = DataBindingUtil.setContentView(this, R.layout.activity_conversations);
        this.getFragmentManager().addOnBackStackChangedListener(this::invalidateActionBarTitle);
        this.initializeFragments();
        this.invalidateActionBarTitle();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_conversations, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onConversationSelected(Conversation conversation) {
        Log.d(Config.LOGTAG, "selected " + conversation.getName());
        ConversationFragment conversationFragment = (ConversationFragment) getFragmentManager().findFragmentById(R.id.secondary_fragment);
        final boolean mainNeedsRefresh;
        if (conversationFragment == null) {
            mainNeedsRefresh = false;
            conversationFragment = new ConversationFragment();
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.main_fragment, conversationFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        } else {
            mainNeedsRefresh = true;
        }
        conversationFragment.reInit(conversation);
        if (mainNeedsRefresh) {
            refreshFragment(R.id.main_fragment);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fm = getFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                    return true;
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeFragments() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment mainFragment = getFragmentManager().findFragmentById(R.id.main_fragment);
        Fragment secondaryFragment = getFragmentManager().findFragmentById(R.id.secondary_fragment);
        if (mainFragment != null) {
            Log.d(Config.LOGTAG, "initializeFragment(). main fragment exists");
            if (binding.secondaryFragment != null) {
                if (mainFragment instanceof ConversationFragment) {
                    Log.d(Config.LOGTAG, "gained secondary fragment. moving...");
                    getFragmentManager().popBackStack();
                    transaction.remove(mainFragment);
                    transaction.commit();
                    getFragmentManager().executePendingTransactions();
                    transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.secondary_fragment, mainFragment);
                    transaction.replace(R.id.main_fragment, new ConversationsOverviewFragment());
                    transaction.commit();
                    return;
                }
            } else {
                if (secondaryFragment != null && secondaryFragment instanceof ConversationFragment) {
                    Log.d(Config.LOGTAG, "lost secondary fragment. moving...");
                    transaction.remove(secondaryFragment);
                    transaction.commit();
                    getFragmentManager().executePendingTransactions();
                    transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.main_fragment, secondaryFragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                    return;
                }
            }
        } else {
            transaction.replace(R.id.main_fragment, new ConversationsOverviewFragment());
        }

        if (binding.secondaryFragment != null) {
            transaction.replace(R.id.secondary_fragment, new ConversationFragment());
        }
        transaction.commit();
    }

    private void invalidateActionBarTitle() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            Fragment mainFragment = getFragmentManager().findFragmentById(R.id.main_fragment);
            if (mainFragment != null && mainFragment instanceof ConversationFragment) {
                final Conversation conversation = ((ConversationFragment) mainFragment).getConversation();
                if (conversation != null) {
                    actionBar.setTitle(conversation.getName());
                    actionBar.setDisplayHomeAsUpEnabled(true);
                    return;
                }
            }
            actionBar.setTitle(R.string.app_name);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    public boolean useBundledEmoji() {
        return getPreferences().getBoolean(USE_BUNDLED_EMOJIS, getResources().getBoolean(R.bool.use_bundled_emoji));
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
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(ConversationsMainActivity.this, VerifyOTRActivity.class);
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

    @Override
    public void onConversationArchived(Conversation conversation) {

    }

    @Override
    public void onConversationsListItemUpdated() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.main_fragment);
        if (fragment != null && fragment instanceof ConversationsOverviewFragment) {
            ((ConversationsOverviewFragment) fragment).refresh();
        }
    }

    @Override
    public void onConversationRead(Conversation conversation) {
        Log.d(Config.LOGTAG, "read event for " + conversation.getName() + " received");
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
    public void OnUpdateBlocklist(OnUpdateBlocklist.Status status) {
        this.refreshUi();
    }

    @Override
    public void onShowErrorToast(int resId) {
        runOnUiThread(() -> Toast.makeText(this, resId, Toast.LENGTH_SHORT).show());
    }
}