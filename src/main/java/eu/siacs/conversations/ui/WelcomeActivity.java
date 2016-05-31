package eu.siacs.conversations.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eu.siacs.conversations.R;
import eu.siacs.conversations.persistance.DatabaseBackend;

public class WelcomeActivity extends Activity {

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome);
        boolean dbExist = checkDatabase();
        boolean backup_existing = false;

        //check if there is a backed up database --
        if (dbExist) {
            //copy db from public storage to private storage
            backup_existing = true;
        } else {
            //if copy fails, show dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.import_failed)
                    .setCancelable(false)
                    .setPositiveButton(R.string.create_account, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(WelcomeActivity.this, MagicCreateActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.use_existing_accout, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivity(new Intent(WelcomeActivity.this, EditAccountActivity.class));
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            //throw new Error("Error copying database");
        }

        final Button ImportDatabase = (Button) findViewById(R.id.import_database);
        final TextView ImportText = (TextView) findViewById(R.id.import_text);

        if (backup_existing) {
            ImportDatabase.setVisibility(View.VISIBLE);
            ImportText.setVisibility(View.VISIBLE);
        }

        ImportDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //ToDo add import DB from local storage to system storage and wait until copy is complete
                try {
                    ImportDatabase();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //ask user to uninstall old eu.siacs.conversations before restart

                //restart app
                Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
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

    private boolean checkDatabase() {

        SQLiteDatabase checkDB = null;
        String DB_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pix-Art Messenger/.Database/";
        String DB_NAME = "Database.bak";

        try {
            String myPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
            //database does't exist yet.
        }

        if (checkDB != null) {
            checkDB.close();
        }
        return checkDB != null ? true : false;
    }

    private void ImportDatabase() throws IOException {

        // Set location for the db:
        OutputStream myOutput = new FileOutputStream(this.getDatabasePath(DatabaseBackend.DATABASE_NAME));

        // Set the folder on the SDcard
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pix-Art Messenger/.Database/");

        // Set the input file stream up:
        InputStream myInput = new FileInputStream(directory.getPath() + "/Database.bak");

        // Transfer bytes from the input file to the output file
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        // Close and clear the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }


}
