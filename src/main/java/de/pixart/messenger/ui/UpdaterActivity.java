package de.pixart.messenger.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.persistance.FileBackend;

public class UpdaterActivity extends Activity {

    static final private String FileName = "update.apk";
    String appURI = "";
    String changelog = "";
    Integer filesize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set activity
        setContentView(R.layout.activity_updater);
        TextView textView = (TextView) findViewById(R.id.updater);
        textView.setText(R.string.update_info);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getIntent() != null && getIntent().getStringExtra("update").equals("PixArtMessenger_UpdateService")) {
            try {
                appURI = getIntent().getStringExtra("url");
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getText(R.string.failed), Toast.LENGTH_LONG).show();
                UpdaterActivity.this.finish();
            }
            try {
                changelog = getIntent().getStringExtra("changelog");
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getText(R.string.failed), Toast.LENGTH_LONG).show();
                UpdaterActivity.this.finish();
            }
            //delete old downloaded localVersion files
            File dir = new File(FileBackend.getConversationsDirectory("Update", false));
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (String aChildren : children) {
                    Log.d(Config.LOGTAG, "AppUpdater: delete old update files " + aChildren + " in " + dir);
                    new File(dir, aChildren).delete();
                }
            }

            //oh yeah we do need an upgrade, let the user know send an alert message
            AlertDialog.Builder builder = new AlertDialog.Builder(UpdaterActivity.this);
            builder.setCancelable(false);
            builder.setMessage(getString(R.string.install_update))
                    .setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                        //if the user agrees to upgrade
                        public void onClick(DialogInterface dialog, int id) {
                            Log.d(Config.LOGTAG, "AppUpdater: downloading " + FileName + " from " + appURI);
                            //ask for permissions on devices >= SDK 23
                            if (isStoragePermissionGranted() && isNetworkAvailable(getApplicationContext())) {
                                //start downloading the file using the download manager
                                DownloadFromUrl(appURI, FileName);
                                Toast.makeText(getApplicationContext(), getText(R.string.download_started), Toast.LENGTH_LONG).show();
                            } else {
                                Log.d(Config.LOGTAG, "AppUpdater: failed - has storage permissions " + isStoragePermissionGranted() + " and internet " + isNetworkAvailable(getApplicationContext()));
                            }
                        }
                    })
                    .setNeutralButton(R.string.changelog, new DialogInterface.OnClickListener() {
                        //open link to changelog
                        public void onClick(DialogInterface dialog, int id) {
                            Uri uri = Uri.parse("https://github.com/kriztan/Conversations/blob/master/CHANGELOG.md"); // missing 'http://' will cause crashed
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            //restart updater to show dialog again after coming back after opening changelog
                            recreate();
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
        } else {
            Toast.makeText(getApplicationContext(), getText(R.string.failed), Toast.LENGTH_LONG).show();
            UpdaterActivity.this.finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    public void DownloadFromUrl(final String DownloadUrl, final String FileName) {
        final Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + FileBackend.getDirectoryName("Update", false));

                    URL url = new URL(DownloadUrl);
                    File file = new File(dir, FileName);
                    Log.d(Config.LOGTAG, "AppUpdater: save file to " + file.toString());

                    long startTime = System.currentTimeMillis();
                    Log.d(Config.LOGTAG, "AppUpdater: download update from url: " + url + " to file name: " + file.toString());

                    // Open a connection to that URL.
                    URLConnection connection = url.openConnection();

                    //Define InputStreams to read from the URLConnection.
                    InputStream is = connection.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is);

                    //Read file size
                    List values = connection.getHeaderFields().get("content-Length");
                    if (values != null && !values.isEmpty()) {
                        String sLength = (String) values.get(0);
                        if (sLength != null) {
                            filesize = Integer.parseInt(sLength);
                        }
                    }
                    //Read bytes to the Buffer until there is nothing more to read(-1).
                    ByteArrayBuffer baf = new ByteArrayBuffer(5000);
                    int current = 0;
                    while ((current = bis.read()) != -1) {
                        baf.append((byte) current);
                    }

                    // Convert the Bytes read to a String.
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(baf.toByteArray());
                    fos.flush();
                    fos.close();
                    Log.d(Config.LOGTAG, "AppUpdater: download ready in" + ((System.currentTimeMillis() - startTime) / 1000) + " sec");

                    //start the installation of the latest localVersion
                    Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    installIntent.setDataAndType(FileBackend.getUriForFile(UpdaterActivity.this, file), "application/vnd.android.package-archive");
                    installIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                    installIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(installIntent);
                    UpdaterActivity.this.finish();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.d(Config.LOGTAG, "AppUpdater: Error: " + e);
                }
            }
        });
        thread.start();
    }

    //check for internet connection
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    //show warning on back pressed
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.cancel_update)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        UpdaterActivity.this.finish();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
