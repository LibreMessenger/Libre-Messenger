package de.pixart.messenger.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import de.pixart.messenger.R;
import de.pixart.messenger.services.EmojiService;
import de.pixart.messenger.utils.MenuDoubleTabUtil;

import static de.pixart.messenger.ui.SettingsActivity.USE_BUNDLED_EMOJIS;

public class ShowLocationActivity extends XmppActivity {
    private Location location;
    private String mLocationName;

    private static String getAddress(Context context, Location location) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        String address = "";
        if (latitude != 0 && longitude != 0) {
            Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
            try {
                List<Address> addresses = geoCoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && addresses.size() > 0) {
                    Address Address = addresses.get(0);
                    StringBuilder strAddress = new StringBuilder("");

                    if (Address.getAddressLine(0).length() > 0) {
                        strAddress.append(Address.getAddressLine(0));
                    }
                    address = strAddress.toString().replace(", ", "<br>");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return address;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean useBundledEmoji = getPreferences().getBoolean(USE_BUNDLED_EMOJIS, getResources().getBoolean(R.bool.use_bundled_emoji));
        new EmojiService(this).init(useBundledEmoji);
        setContentView(R.layout.activity_show_locaction);
        setTitle(getString(R.string.show_location));
        setSupportActionBar(findViewById(R.id.toolbar));
        configureActionBar(getSupportActionBar());

        Intent intent = getIntent();

        this.mLocationName = intent != null ? intent.getStringExtra("name") : null;

        if (intent != null && intent.hasExtra("longitude") && intent.hasExtra("latitude")) {
            double longitude = intent.getDoubleExtra("longitude", 0);
            double latitude = intent.getDoubleExtra("latitude", 0);
            this.location = new Location("");
            this.location.setLatitude(latitude);
            this.location.setLongitude(longitude);
        }
        markAndCenterOnLocation(location);
    }

    private void markAndCenterOnLocation(final Location location) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        if (latitude != 0 && longitude != 0) {
            new getAddressAsync(this).execute();
        }
    }

    ;

    protected SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    protected void refreshUiReal() {

    }

    @Override
    void onBackendConnected() {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (MenuDoubleTabUtil.shouldIgnoreTap()) {
            return false;
        }
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_navigate:
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                try {
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("geo:" + String.valueOf(latitude) + "," + String.valueOf(longitude)));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.no_application_found_to_display_location, Toast.LENGTH_SHORT).show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.showlocation, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void showLocation(@Nullable Location location, @Nullable String address) {
        if (location == null && TextUtils.isEmpty(address)) { // no location and no address available
            final WebView webView = findViewById(R.id.webView);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadUrl("file:///android_asset/map.html");
        } else if (location != null && TextUtils.isEmpty(address)) { // location but no address available
            String LocationName = "<b>" + mLocationName + "</b>";
            final WebView webView = findViewById(R.id.webView);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadUrl("file:///android_asset/map.html?lat=" + location.getLatitude() + "&lon=" + location.getLongitude() + "&name=" + LocationName);
        } else if (location != null && !TextUtils.isEmpty(address)) { // location and address available
            String LocationName = "<b>" + mLocationName + "</b><br>" + address;
            final WebView webView = findViewById(R.id.webView);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadUrl("file:///android_asset/map.html?lat=" + location.getLatitude() + "&lon=" + location.getLongitude() + "&name=" + LocationName);
        }
    }

    private class getAddressAsync extends AsyncTask<Void, Void, Void> {
        String address = null;

        private WeakReference<ShowLocationActivity> activityReference;

        getAddressAsync(ShowLocationActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLocation(location, null);
        }

        @Override
        protected Void doInBackground(Void... params) {
            address = getAddress(ShowLocationActivity.this, location);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            showLocation(location, address);
        }
    }
}