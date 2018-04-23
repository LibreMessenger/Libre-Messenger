package de.pixart.messenger.ui;

import android.os.Bundle;

import de.pixart.messenger.R;
import de.pixart.messenger.utils.ThemeHelper;

public class AboutActivity extends XmppActivity {

    @Override
    protected void refreshUiReal() {

    }

    @Override
    void onBackendConnected() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeHelper.find(this));
        setContentView(R.layout.activity_about);
        setSupportActionBar(findViewById(R.id.toolbar));
        configureActionBar(getSupportActionBar());
    }
}