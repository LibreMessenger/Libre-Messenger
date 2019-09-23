package de.pixart.messenger.services;

import android.content.Context;
import android.os.Build;

import androidx.emoji.text.EmojiCompat;

public class EmojiService {

    private final Context context;

    public EmojiService(Context context) {
        this.context = context;
    }

    public void init(boolean useBundledEmoji) {
        final FontRequest fontRequest = new FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.font_certs);
        FontRequestEmojiCompatConfig fontRequestEmojiCompatConfig = new FontRequestEmojiCompatConfig(context, fontRequest);
        fontRequestEmojiCompatConfig.registerInitCallback(initCallback);
        //On recent Androids we assume to have the latest emojis
        //there are some annoying bugs with emoji compat that make it a safer choice not to use it when possible
        // a) when using the ondemand emoji font (play store) flags don’t work
        // b) the text preview has annoying glitches when the cut of text contains emojis (the emoji will be half visible)
        // c) can trigger a hardware rendering bug https://issuetracker.google.com/issues/67102093
        fontRequest.setReplaceAll(useBundledEmoji && Build.VERSION.SDK_INT < Build.VERSION_CODES.O);
        EmojiCompat.init(config);
    }
}