package de.pixart.messenger.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import de.pixart.messenger.R;

import static de.pixart.messenger.ui.XmppActivity.configureActionBar;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setSupportActionBar(findViewById(R.id.toolbar));
        configureActionBar(getSupportActionBar());
    }
}
