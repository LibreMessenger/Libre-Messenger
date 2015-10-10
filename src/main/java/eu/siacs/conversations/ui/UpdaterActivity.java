package eu.siacs.conversations.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.R;
import eu.siacs.conversations.services.UpdaterWebService;

public class UpdaterActivity extends Activity {

    private static final String LOG_TAG = Config.LOGTAG + "AppUpgrade";
    private MyWebReceiver receiver;
    private int versionCode = 0;
    String appURI = "";


    /*
        // run AppUpdater
        Log.d(Config.LOGTAG, "Start automatic AppUpdater");
        Intent AppUpdater = new Intent(this, UpdaterActivity.class);
        startActivity(AppUpdater);
     */

    private DownloadManager downloadManager;
    private long downloadReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //disable touch events
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        //Overall information about the contents of a package
        //This corresponds to all of the information collected from AndroidManifest.xml.
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        }
        catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //get the app version Name for display
        String version = pInfo.versionName;
        //get the app version Code for checking
        versionCode = pInfo.versionCode;
        /*
        //display the current version in a TextView
        TextView currentversionText = (TextView) findViewById(R.id.current_versionName);
        currentversionText.setText(getText(R.string.current_version) + version);
*/
        //Broadcast receiver for our Web Request
        IntentFilter filter = new IntentFilter(MyWebReceiver.PROCESS_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new MyWebReceiver();
        registerReceiver(receiver, filter);

        //Broadcast receiver for the download manager
        filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);

        //check of internet is available before making a web service request
        if(isNetworkAvailable(this)){
            Intent msgIntent = new Intent(this, UpdaterWebService.class);
            msgIntent.putExtra(UpdaterWebService.REQUEST_STRING, Config.UPDATE_URL);
            
            Toast.makeText(getApplicationContext(),
                                getText(R.string.checking_for_updates),
                                Toast.LENGTH_LONG).show();
            startService(msgIntent);
        }
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
*/
    @Override
    public void onDestroy() {
        //unregister your receivers
        this.unregisterReceiver(receiver);
        this.unregisterReceiver(downloadReceiver);
        super.onDestroy();
        //enable touch events
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

        //check for internet connection
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    Log.d(Config.LOGTAG, "AppUpdater: " + String.valueOf(i));
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        Log.d(Config.LOGTAG, "AppUpdater: connected to update Server!");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //broadcast receiver to get notification when the web request finishes
    public class MyWebReceiver extends BroadcastReceiver {

        public static final String PROCESS_RESPONSE = "eu.siacs.conversations.intent.action.PROCESS_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {

            String reponseMessage = intent.getStringExtra(UpdaterWebService.RESPONSE_MESSAGE);
            Log.d(Config.LOGTAG, "AppUpdater: " + reponseMessage);

            //parse the JSON response
            JSONObject responseObj;
            try {
                responseObj = new JSONObject(reponseMessage);
                boolean success = responseObj.getBoolean("success");
                //if the reponse was successful check further
                if(success){
                    //Overall information about the contents of a package
                    //This corresponds to all of the information collected from AndroidManifest.xml.
                    PackageInfo pInfo = null;
                    try {
                        pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    }
                    catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    //get the app version Name for display
                    String version = pInfo.versionName;
                    //get the latest version from the JSON string
                    int latestVersionCode = responseObj.getInt("latestVersionCode");
                    String latestVersion = responseObj.getString("latestVersion");
                    String changelog = responseObj.getString("changelog");
                    /*
                    //display the new version in a TextView
                    TextView versionText = (TextView) findViewById(R.id.versionName);
                    versionText.setText(getText(R.string.new_version) + latestVersion);
                    */
                    //get the lastest application URI from the JSON string
                    appURI = responseObj.getString("appURI");
                    //check if we need to upgrade?
                    if(latestVersionCode > versionCode){
                        Log.d(Config.LOGTAG, "AppUpdater: update available");
                        //enable touch events
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        
                        //oh yeah we do need an upgrade, let the user know send an alert message
                        AlertDialog.Builder builder = new AlertDialog.Builder(UpdaterActivity.this);
                        builder.setCancelable(false);

                        String UpdateMessageInfo = getResources().getString(R.string.update_available);
                        builder.setMessage(String.format(UpdateMessageInfo, latestVersion, changelog, version))
                                .setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                                    //if the user agrees to upgrade
                                    public void onClick(DialogInterface dialog, int id) {
                                        //start downloading the file using the download manager
                                        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                        Uri Download_Uri = Uri.parse(appURI);
                                        DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
                                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
                                        request.setAllowedOverRoaming(false);
                                        request.setTitle("Conversations Update");
                                        request.setDestinationInExternalFilesDir(UpdaterActivity.this, Environment.DIRECTORY_DOWNLOADS, "Conversations.apk");
                                        //delete old downloaded version files
                                        File dir = new File(getExternalFilesDir(null), Environment.DIRECTORY_DOWNLOADS);
                                        Log.d(Config.LOGTAG, "AppUpdater - Delete old update files in: " + dir);
                                        if (dir.isDirectory())
                                        {
                                            String[] children = dir.list();
                                            for (int i = 0; i < children.length; i++)
                                            {
                                                new File(dir, children[i]).delete();
                                            }
                                        }

                                        downloadReference = downloadManager.enqueue(request);
                                        Toast.makeText(getApplicationContext(),
                                                getText(R.string.download_started),
                                                Toast.LENGTH_LONG).show();
                                    }
                                })
                                .setNegativeButton(R.string.remind_later, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User cancelled the dialog
                                        UpdaterActivity.this.finish();
                                    }
                                });
                        //show the alert message
                        builder.create().show();
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(),
                                getText(R.string.no_update_available),
                                Toast.LENGTH_SHORT).show();
                        Log.d(Config.LOGTAG, "AppUpdater: no update available");
                        UpdaterActivity.this.finish();
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }

    //broadcast receiver to get notification about ongoing downloads
    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            //check if the broadcast message is for our Enqueued download
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if(downloadReference == referenceId){

                Log.d(Config.LOGTAG, "AppUpdater: Downloading of the new app version complete. Starting installation");
                //start the installation of the latest version
                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                installIntent.setDataAndType(downloadManager.getUriForDownloadedFile(downloadReference),
                        "application/vnd.android.package-archive");
                installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(installIntent);
            }
        }
    };
}