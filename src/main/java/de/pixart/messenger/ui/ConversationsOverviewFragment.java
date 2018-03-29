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

import android.app.Activity;
import android.app.Fragment;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.databinding.FragmentConversationsOverviewBinding;
import de.pixart.messenger.entities.Conversation;
import de.pixart.messenger.ui.adapter.ConversationAdapter;
import de.pixart.messenger.ui.interfaces.OnConversationSelected;
import de.pixart.messenger.ui.util.PendingItem;

public class ConversationsOverviewFragment extends XmppFragment {

    private final List<Conversation> conversations = new ArrayList<>();
    private final PendingItem<Conversation> swipedConversation = new PendingItem<>();
    private FragmentConversationsOverviewBinding binding;
    private ConversationAdapter conversationsAdapter;
    private XmppActivity activity;

    public static Conversation getSuggestion(Activity activity) {
        final Conversation exception;
        Fragment fragment = activity.getFragmentManager().findFragmentById(R.id.main_fragment);
        if (fragment != null && fragment instanceof ConversationsOverviewFragment) {
            exception = ((ConversationsOverviewFragment) fragment).swipedConversation.peek();
        } else {
            exception = null;
        }
        return getSuggestion(activity, exception);
    }

    public static Conversation getSuggestion(Activity activity, Conversation exception) {
        Fragment fragment = activity.getFragmentManager().findFragmentById(R.id.main_fragment);
        if (fragment != null && fragment instanceof ConversationsOverviewFragment) {
            List<Conversation> conversations = ((ConversationsOverviewFragment) fragment).conversations;
            if (conversations.size() > 0) {
                Conversation suggestion = conversations.get(0);
                if (suggestion == exception) {
                    if (conversations.size() > 1) {
                        return conversations.get(1);
                    }
                } else {
                    return suggestion;
                }
            }
        }
        return null;

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof XmppActivity) {
            this.activity = (XmppActivity) activity;
        } else {
            throw new IllegalStateException("Trying to attach fragment to activity that is not an XmppActivity");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.activity = null;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(Config.LOGTAG, "onCreateView");
        this.binding = DataBindingUtil.inflate(inflater, R.layout.fragment_conversations_overview, container, false);
        this.binding.fab.setOnClickListener((view) -> StartConversationActivity.launch(getActivity()));

        this.conversationsAdapter = new ConversationAdapter(this.activity, this.conversations);
        this.binding.list.setAdapter(this.conversationsAdapter);
        this.binding.list.setOnItemClickListener((parent, view, position, id) -> {
            Conversation conversation = this.conversations.get(position);
            if (activity instanceof OnConversationSelected) {
                ((OnConversationSelected) activity).onConversationSelected(conversation);
            } else {
                Log.w(ConversationsOverviewFragment.class.getCanonicalName(), "Activity does not implement OnConversationSelected");
            }
        });

        return binding.getRoot();
    }

    @Override
    void onBackendConnected() {
        Log.d(Config.LOGTAG, "nice!");
        refresh();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(Config.LOGTAG, "ConversationsOverviewFragment.onStart()");
        if (activity.xmppConnectionService != null) {
            refresh();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(Config.LOGTAG, "ConversationsOverviewFragment.onResume()");
    }

    @Override
    void refresh() {
        this.activity.xmppConnectionService.populateWithOrderedConversations(this.conversations);
        this.conversationsAdapter.notifyDataSetChanged();
    }
}