package de.pixart.messenger.services;

import android.content.Context;
import android.support.text.emoji.EmojiCompat;
import android.util.Log;

import de.pixart.messenger.Config;
import de.pixart.messenger.utils.Emoticons;

public abstract class AbstractEmojiService {

    protected final Context context;

    public AbstractEmojiService(Context context) {
        this.context = context;
    }

    protected abstract EmojiCompat.Config buildConfig();

    public void init(boolean useBundledEmoji) {
        Log.d(Config.LOGTAG, "Emojis: use integrated lib " + useBundledEmoji);
        EmojiCompat.Config config = buildConfig();
        config.setReplaceAll(useBundledEmoji);
        EmojiCompat.reset(config);
        EmojiCompat.init(config);
    }
}