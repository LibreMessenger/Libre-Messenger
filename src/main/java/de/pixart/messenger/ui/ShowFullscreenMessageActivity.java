package de.pixart.messenger.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;
import com.github.rtoshiro.view.video.FullscreenVideoLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.persistance.FileBackend;
import de.pixart.messenger.utils.ExifHelper;

import static de.pixart.messenger.persistance.FileBackend.close;

public class ShowFullscreenMessageActivity extends XmppActivity {

    Integer oldOrientation;
    PhotoView mImage;
    FullscreenVideoLayout mVideo;
    ImageView mFullscreenbutton;
    Uri mFileUri;
    File mFile;
    FloatingActionButton fab;
    int height = 0;
    int width = 0;
    int rotation = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mTheme = findTheme();
        setTheme(this.mTheme);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && actionBar.isShowing()) {
            actionBar.hide();
        }

        oldOrientation = getRequestedOrientation();

        WindowManager.LayoutParams layout = getWindow().getAttributes();
        if (useMaxBrightness()) {
            layout.screenBrightness = 1;
        }
        getWindow().setAttributes(layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_fullscreen_message);
        mImage = findViewById(R.id.message_image_view);
        mVideo = findViewById(R.id.message_video_view);
        mFullscreenbutton = findViewById(R.id.vcv_img_fullscreen);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            mVideo.reset();
            shareWith(mFile);
        });
    }

    private void shareWith(File mFile) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(getMimeType(mFile.toString()));
        share.putExtra(Intent.EXTRA_STREAM, FileBackend.getUriForFile(this, mFile));
        try {
            startActivity(Intent.createChooser(share, getText(R.string.share_with)));
        } catch (ActivityNotFoundException e) {
            //This should happen only on faulty androids because normally chooser is always available
            Toast.makeText(this, R.string.no_application_found_to_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    public static String getMimeType(String path) {
        try {
            String type = null;
            String extension = path.substring(path.lastIndexOf(".") + 1, path.length());
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
            return type;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void refreshUiReal() {

    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("image")) {
                mFileUri = intent.getParcelableExtra("image");
                mFile = new File(mFileUri.getPath());
                if (mFileUri != null && mFile.exists() && mFile.length() > 0) {
                    try {
                        DisplayImage(mFile);
                    } catch (Exception e) {
                        Log.d(Config.LOGTAG, "Illegal exeption :" + e);
                        Toast.makeText(ShowFullscreenMessageActivity.this, getString(R.string.error_file_corrupt), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(ShowFullscreenMessageActivity.this, getString(R.string.file_deleted), Toast.LENGTH_SHORT).show();
                }
            } else if (intent.hasExtra("video")) {
                mFileUri = intent.getParcelableExtra("video");
                mFile = new File(mFileUri.getPath());
                if (mFileUri != null && mFile.exists() && mFile.length() > 0) {
                    try {
                        DisplayVideo(mFileUri);
                    } catch (Exception e) {
                        Log.d(Config.LOGTAG, "Illegal exeption :" + e);
                        Toast.makeText(ShowFullscreenMessageActivity.this, getString(R.string.error_file_corrupt), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(ShowFullscreenMessageActivity.this, getString(R.string.file_deleted), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void DisplayImage(final File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(new File(file.getPath()).getAbsolutePath(), options);
        height = options.outHeight;
        width = options.outWidth;
        rotation = getRotation(Uri.parse("file://" + file.getAbsolutePath()));
        Log.d(Config.LOGTAG, "Image height: " + height + ", width: " + width + ", rotation: " + rotation);
        if (useAutoRotateScreen()) {
            rotateScreen(width, height, rotation);
        }
        final PhotoViewAttacher mAttacher = new PhotoViewAttacher(mImage);
        mImage.setVisibility(View.VISIBLE);
        try {
            Glide.with(this)
                    .load(file)
                    .dontAnimate()
                    .into(new GlideDrawableImageViewTarget(mImage) {
                        @Override
                        public void onResourceReady(GlideDrawable resource,  GlideAnimation<? super GlideDrawable> animation) {
                            super.onResourceReady(resource, animation);
                            mAttacher.update();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_file_corrupt), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void DisplayVideo(final Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.getPath());
        Bitmap bitmap = null;
        try {
            bitmap = retriever.getFrameAtTime();
            height = bitmap.getHeight();
            width = bitmap.getWidth();
        } catch (Exception e) {
            height = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            width = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        rotation = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        Log.d(Config.LOGTAG, "Video height: " + height + ", width: " + width + ", rotation: " + rotation);
        if (useAutoRotateScreen()) {
            rotateScreen(width, height, rotation);
        }
        try {
            mVideo.setVisibility(View.VISIBLE);
            mVideo.setVideoURI(uri);
            mFullscreenbutton.setVisibility(View.INVISIBLE);
            mVideo.setShouldAutoplay(true);

        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.error_file_corrupt), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private int getRotation(Uri image) {
        InputStream is = null;
        try {
            is = this.getContentResolver().openInputStream(image);
            return ExifHelper.getOrientation(is);
        } catch (FileNotFoundException e) {
            return 0;
        } finally {
            close(is);
        }
    }

    private void rotateScreen(final int width, final int height, final int rotation) {
        if (width > height) {
            if (rotation == 0 || rotation == 180) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        } else if (width <= height) {
            if (rotation == 90 || rotation == 270) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume() {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        if (useMaxBrightness()) {
            layout.screenBrightness = 1;
        }
        getWindow().setAttributes(layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mVideo.setShouldAutoplay(true);
        super.onResume();
    }

    @Override
    public void onPause() {
        mVideo.reset();
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        if (useMaxBrightness()) {
            layout.screenBrightness = -1;
        }
        getWindow().setAttributes(layout);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(oldOrientation);
        super.onPause();
    }

    @Override
    public void onStop() {
        mVideo.reset();
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        if (useMaxBrightness()) {
            layout.screenBrightness = -1;
        }
        getWindow().setAttributes(layout);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(oldOrientation);
        super.onStop();
    }

    @Override
    void onBackendConnected() {

    }

    public boolean useMaxBrightness() {
        return getPreferences().getBoolean("use_max_brightness", getResources().getBoolean(R.bool.use_max_brightness));
    }

    public boolean useAutoRotateScreen() {
        return getPreferences().getBoolean("use_auto_rotate", getResources().getBoolean(R.bool.auto_rotate));
    }

    protected SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }
}