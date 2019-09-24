package de.pixart.messenger.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

import de.pixart.messenger.R;

import static de.pixart.messenger.ui.util.IntroHelper.SaveIntroShown;

public class IntroActivity extends AppIntro {
    public static final String ACTIVITY = "activity";
    public static final String MULTICHAT = "multi_chat";
    public static final String START_UI = "StartUI";
    public static final String CONVERSATIONS_ACTIVITY = "ConversationsActivity";
    public static final String CONTACT_DETAILS_ACTIVITY = "ContactDetailsActivity";
    public static final String CONFERENCE_DETAILS_ACTIVITY = "ConferenceDetailsActivity";
    String activity = null;
    boolean mode_multi = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int backgroundColor = getResources().getColor(R.color.primary_dark);
        final int barColor = getResources().getColor(R.color.primary);
        final int separatorColor = getResources().getColor(R.color.accent);

        setBarColor(barColor);
        setSeparatorColor(separatorColor);
        setProgressButtonEnabled(true);
        showSkipButton(false);
        setBackButtonVisibilityWithDone(true);
        setGoBackLock(true);
        setFadeAnimation();

        final Intent intent = getIntent();
        if (intent != null) {
            activity = intent.getStringExtra(ACTIVITY);
            mode_multi = intent.getBooleanExtra(MULTICHAT, false);
        }
        if (activity == null) {
            finish();
        }
        switch (activity) {
            case START_UI:
                SliderPage welcome = new SliderPage();
                welcome.setTitle(getString(R.string.welcome_header));
                welcome.setDescription(getString(R.string.intro_desc_main));
                welcome.setImageDrawable(R.drawable.main_logo);
                welcome.setBgColor(backgroundColor);
                addSlide(AppIntroFragment.newInstance(welcome));

                SliderPage xmpp = new SliderPage();
                xmpp.setTitle(getString(R.string.intro_whats_xmpp));
                xmpp.setDescription(getString(R.string.intro_desc_whats_xmpp));
                xmpp.setImageDrawable(R.drawable.intro_xmpp_icon);
                xmpp.setBgColor(backgroundColor);
                addSlide(AppIntroFragment.newInstance(xmpp));

                SliderPage permissions = new SliderPage();
                permissions.setTitle(getString(R.string.intro_permissions));
                permissions.setDescription(getString(R.string.intro_desc_permissions));
                permissions.setImageDrawable(R.drawable.intro_memory_icon);
                permissions.setBgColor(backgroundColor);
                addSlide(AppIntroFragment.newInstance(permissions));

                SliderPage permissions2 = new SliderPage();
                permissions2.setTitle(getString(R.string.intro_permissions));
                permissions2.setDescription(getString(R.string.intro_desc_contacts_permissions));
                permissions2.setImageDrawable(R.drawable.intro_contacts_icon);
                permissions2.setBgColor(backgroundColor);
                addSlide(AppIntroFragment.newInstance(permissions2));

                SliderPage account = new SliderPage();
                account.setTitle(getString(R.string.intro_account));
                account.setDescription(getString(R.string.intro_desc_account));
                account.setImageDrawable(R.drawable.intro_account_icon);
                account.setBgColor(backgroundColor);
                addSlide(AppIntroFragment.newInstance(account));

                SliderPage account2 = new SliderPage();
                account2.setTitle(getString(R.string.intro_account));
                account2.setDescription(getString(R.string.intro_desc_account2));
                account2.setImageDrawable(R.drawable.intro_account_icon);
                account2.setBgColor(backgroundColor);
                addSlide(AppIntroFragment.newInstance(account2));

                SliderPage startChatting = new SliderPage();
                startChatting.setTitle(getString(R.string.intro_start_chatting));
                startChatting.setDescription(getString(R.string.intro_desc_start_chatting));
                startChatting.setImageDrawable(R.drawable.intro_start_chat_icon);
                startChatting.setBgColor(backgroundColor);
                addSlide(AppIntroFragment.newInstance(startChatting));
                break;
            case CONVERSATIONS_ACTIVITY:
                SliderPage openChat = new SliderPage();
                openChat.setTitle(getString(R.string.intro_start_chatting));
                openChat.setDescription(getString(R.string.intro_desc_open_chat));
                openChat.setImageDrawable(R.drawable.intro_start_chat_icon);
                openChat.setBgColor(backgroundColor);
                addSlide(AppIntroFragment.newInstance(openChat));

                SliderPage chatDetails = new SliderPage();
                chatDetails.setTitle(getString(R.string.intro_chat_details));
                chatDetails.setDescription(getString(R.string.intro_desc_chat_details));
                chatDetails.setImageDrawable(R.drawable.intro_account_details_icon);
                chatDetails.setBgColor(backgroundColor);
                addSlide(AppIntroFragment.newInstance(chatDetails));

                if (mode_multi) {
                    SliderPage highlightUser = new SliderPage();
                    highlightUser.setTitle(getString(R.string.intro_highlight_user));
                    highlightUser.setDescription(getString(R.string.intro_desc_highlight_user));
                    highlightUser.setImageDrawable(R.drawable.intro_account_details_icon);
                    highlightUser.setBgColor(backgroundColor);
                    addSlide(AppIntroFragment.newInstance(highlightUser));
                }
                break;
            case CONTACT_DETAILS_ACTIVITY:
            case CONFERENCE_DETAILS_ACTIVITY:
                SliderPage openChatDetails = new SliderPage();
                openChatDetails.setTitle(getString(R.string.intro_chat_details));
                openChatDetails.setDescription(getString(R.string.intro_desc_open_chat_details));
                openChatDetails.setImageDrawable(R.drawable.intro_account_details_icon);
                openChatDetails.setBgColor(backgroundColor);
                addSlide(AppIntroFragment.newInstance(openChatDetails));
        }
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        SaveIntroShown(getBaseContext(), activity, mode_multi);
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
    }
}
