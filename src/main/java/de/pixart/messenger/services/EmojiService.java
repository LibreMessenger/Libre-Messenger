package de.pixart.messenger.services;

import android.content.Context;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.bundled.BundledEmojiCompatConfig;

public class EmojiService extends AbstractEmojiService {

    public EmojiService(Context context) {
        super(context);
    }

    @Override
    protected EmojiCompat.Config buildConfig() {
        return new BundledEmojiCompatConfig(context);
    }
}