package de.pixart.messenger.ui;

import android.os.Bundle;

import de.pixart.messenger.R;

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
        this.mTheme = findTheme();
        setTheme(this.mTheme);
        setContentView(R.layout.activity_about);
        setSupportActionBar(findViewById(R.id.toolbar));
        configureActionBar(getSupportActionBar());
    }
}
