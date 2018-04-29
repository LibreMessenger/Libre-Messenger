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

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.databinding.ActivitySearchBinding;
import de.pixart.messenger.entities.Message;
import de.pixart.messenger.ui.adapter.MessageAdapter;
import de.pixart.messenger.ui.util.Color;
import de.pixart.messenger.ui.util.Drawable;

import static de.pixart.messenger.ui.util.SoftKeyboardUtils.hideSoftKeyboard;
import static de.pixart.messenger.ui.util.SoftKeyboardUtils.showKeyboard;

public class SearchActivity extends XmppActivity implements TextWatcher {

    private final List<Message> messages = new ArrayList<>();
    private ActivitySearchBinding binding;
    private MessageAdapter messageListAdapter;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = DataBindingUtil.setContentView(this, R.layout.activity_search);
        setSupportActionBar((Toolbar) this.binding.toolbar);
        configureActionBar(getSupportActionBar());
        this.messageListAdapter = new MessageAdapter(this, this.messages);
        this.binding.searchResults.setAdapter(messageListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_search, menu);
        MenuItem searchActionMenuItem = menu.findItem(R.id.action_search);
        EditText searchField = searchActionMenuItem.getActionView().findViewById(R.id.search_field);
        searchField.addTextChangedListener(this);
        searchField.setHint(R.string.search_messages);
        showKeyboard(searchField);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            hideSoftKeyboard(this);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void refreshUiReal() {

    }

    @Override
    void onBackendConnected() {

    }

    private void changeBackground(boolean hasSearch, boolean hasResults) {
        if (hasSearch) {
            if (hasResults) {
                binding.searchResults.setBackgroundColor(Color.get(this, R.attr.color_background_secondary));
            } else {
                binding.searchResults.setBackground(Drawable.get(this, R.attr.activity_background_no_results));
            }
        } else {
            binding.searchResults.setBackground(Drawable.get(this, R.attr.activity_background_search));
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        Log.d(Config.LOGTAG, "searching for " + s);
    }

}