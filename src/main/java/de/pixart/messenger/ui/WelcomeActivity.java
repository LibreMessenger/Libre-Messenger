package de.pixart.messenger.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.NoSuchPaddingException;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.persistance.DatabaseBackend;
import de.pixart.messenger.persistance.FileBackend;
import de.pixart.messenger.utils.EncryptDecryptFile;

public class WelcomeActivity extends Activity {

    private static final int REQUEST_READ_EXTERNAL_STORAGE = 0XD737;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        //check if there is a backed up database --
        if (hasStoragePermission(REQUEST_READ_EXTERNAL_STORAGE)) {
            BackupAvailable();
        }


        final Button ImportDatabase = (Button) findViewById(R.id.import_database);
        final TextView ImportText = (TextView) findViewById(R.id.import_text);

        if (BackupAvailable()) {
            ImportDatabase.setVisibility(View.VISIBLE);
            ImportText.setVisibility(View.VISIBLE);
        }

        ImportDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterPasswordDialog();
            }
        });

        final Button createAccount = (Button) findViewById(R.id.create_account);
        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, MagicCreateActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            }
        });
        final Button useOwnProvider = (Button) findViewById(R.id.use_existing_account);
        useOwnProvider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, EditAccountActivity.class));
            }
        });

    }

    public void enterPasswordDialog() {
        LayoutInflater li = LayoutInflater.from(WelcomeActivity.this);
        View promptsView = li.inflate(R.layout.password, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(WelcomeActivity.this);
        alertDialogBuilder.setView(promptsView);
        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.password);
        alertDialogBuilder.setTitle(R.string.enter_password);
        alertDialogBuilder.setMessage(R.string.enter_account_password);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                final String password = userInput.getText().toString();
                                final ProgressDialog pd = ProgressDialog.show(WelcomeActivity.this, getString(R.string.please_wait), getString(R.string.databaseimport_started), true);
                                if (!password.isEmpty()) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                checkDatabase(password);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            pd.dismiss();
                                        }
                                    }).start();
                                } else {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                                    builder.setTitle(R.string.error);
                                    builder.setMessage(R.string.password_should_not_be_empty);
                                    builder.setNegativeButton(R.string.cancel, null);
                                    builder.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            enterPasswordDialog();
                                        }
                                    });
                                    builder.create().show();
                                }
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                Toast.makeText(WelcomeActivity.this, R.string.import_canceled, Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            }
                        }
                );
        WelcomeActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                // show it
                alertDialog.show();
            }
        });
    }

    private boolean BackupAvailable() {
        // Set the folder on the SDcard
        File filePath = new File(FileBackend.getConversationsDirectory() + "/database/database.db.crypt");
        Log.d(Config.LOGTAG,"DB Path: " + filePath.toString());
        if(filePath.exists()) {
            Log.d(Config.LOGTAG,"DB Path existing");
            return true;
        } else {
            Log.d(Config.LOGTAG,"DB Path not existing");
            return false;
        }
    }

    private void checkDatabase(String DecryptionKey) throws IOException {
        // Set the folder on the SDcard
        File directory = new File(FileBackend.getConversationsDirectory() + "/database/");
        // Set the input file stream up:
        FileInputStream InputFile = new FileInputStream(directory.getPath() + "/database.db.crypt");
        // Temp output for DB checks
        File TempFile = new File(directory.getPath() + "/database.bak");
        FileOutputStream OutputTemp = new FileOutputStream(TempFile);

        try {
            EncryptDecryptFile.decrypt(InputFile, OutputTemp, DecryptionKey);
        } catch (NoSuchAlgorithmException e) {
            Log.d(Config.LOGTAG,"Database importer: decryption failed with " + e);
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            Log.d(Config.LOGTAG,"Database importer: decryption failed with " + e);
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            Log.d(Config.LOGTAG,"Database importer: decryption failed (invalid key) with " + e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(Config.LOGTAG,"Database importer: decryption failed (IO) with " + e);
            e.printStackTrace();
        }

        SQLiteDatabase checkDB = null;
        int DB_Version = DatabaseBackend.DATABASE_VERSION;
        int Backup_DB_Version = 0;

        try {
            String dbPath = TempFile.toString();
            checkDB = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
            Backup_DB_Version = checkDB.getVersion();
            Log.d(Config.LOGTAG, "Backup found: " + checkDB + " Version: " + checkDB.getVersion());

        } catch (SQLiteException e) {
            //database does't exist yet.
            Log.d(Config.LOGTAG, "No backup found: " + checkDB);
        }

        if (checkDB != null) {
            checkDB.close();
        }
        Log.d(Config.LOGTAG, "checkDB = " + checkDB.toString() + ", Backup DB = " + Backup_DB_Version + ", DB = " + DB_Version);
        if (checkDB != null && Backup_DB_Version != 0 && Backup_DB_Version <= DB_Version) {
            ImportDatabase();
        } else if (checkDB != null && Backup_DB_Version == 0) {
            WelcomeActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(WelcomeActivity.this, R.string.Password_wrong, Toast.LENGTH_LONG).show();
                }
            });
            enterPasswordDialog();
        } else {
            WelcomeActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(WelcomeActivity.this, R.string.Import_failed, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void ImportDatabase() throws IOException {
        // Set location for the db:
        final OutputStream OutputFile = new FileOutputStream(this.getDatabasePath(DatabaseBackend.DATABASE_NAME));
        // Set the folder on the SDcard
        File directory = new File(FileBackend.getConversationsDirectory() + "/database/");
        // Set the input file stream up:
        final InputStream InputFile = new FileInputStream(directory.getPath() + "/database.bak");
        //set temp file
        File TempFile = new File(directory.getPath() + "/database.bak");

        // Transfer bytes from the input file to the output file
        byte[] buffer = new byte[1024];
        int length;
        while ((length = InputFile.read(buffer)) > 0) {
            OutputFile.write(buffer, 0, length);
        }
        if (TempFile.exists()) {
            Log.d(Config.LOGTAG, "Delete temp file from " + TempFile.toString());
            TempFile.delete();
        }

        Log.d(Config.LOGTAG, "New Features - Uninstall old version of Pix-Art Messenger");
        if (isPackageInstalled("eu.siacs.conversations")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.uninstall_app_text)
                    .setPositiveButton(R.string.uninstall, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //start the deinstallation of old version
                            if (isPackageInstalled("eu.siacs.conversations")) {
                                Uri packageURI_VR = Uri.parse("package:eu.siacs.conversations");
                                Intent uninstallIntent_VR = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI_VR);
                                if (uninstallIntent_VR.resolveActivity(getPackageManager()) != null) {
                                    startActivity(uninstallIntent_VR);
                                }
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.d(Config.LOGTAG, "New Features - Uninstall cancled");
                            restart();
                        }
                    });
            builder.create().show();
        } else {
            restart();
        }
    }

    private void restart() {
        //restart app
        Log.d(Config.LOGTAG, "Restarting " + getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName()));
        Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        System.exit(0);
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

    public boolean hasStoragePermission(int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

}