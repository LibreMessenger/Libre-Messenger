package de.pixart.messenger.utils;

import android.app.Activity;
import android.content.Intent;

import de.pixart.messenger.Config;
import de.pixart.messenger.entities.Account;
import de.pixart.messenger.services.XmppConnectionService;
import de.pixart.messenger.ui.ConversationsActivity;
import de.pixart.messenger.ui.EditAccountActivity;
import de.pixart.messenger.ui.MagicCreateActivity;
import de.pixart.messenger.ui.ManageAccountActivity;
import de.pixart.messenger.ui.StartConversationActivity;
import de.pixart.messenger.ui.WelcomeActivity;

public class SignupUtils {

    public static boolean isSupportTokenRegistry() {
        return true;
    }

    public static Intent getTokenRegistrationIntent(final Activity activity, String domain, String preAuth) {
        final Intent intent = new Intent(activity, MagicCreateActivity.class);
        intent.putExtra(MagicCreateActivity.EXTRA_DOMAIN, domain);
        intent.putExtra(MagicCreateActivity.EXTRA_PRE_AUTH, preAuth);
        return intent;
    }

    public static Intent getSignUpIntent(final Activity activity) {
        final Intent intent = new Intent(activity, WelcomeActivity.class);
        return intent;
    }

    public static Intent getRedirectionIntent(final ConversationsActivity activity) {
        final XmppConnectionService service = activity.xmppConnectionService;
        Account pendingAccount = AccountUtils.getPendingAccount(service);
        Intent intent;
        if (pendingAccount != null) {
            intent = new Intent(activity, EditAccountActivity.class);
            intent.putExtra("jid", pendingAccount.getJid().asBareJid().toString());
        } else {
            if (service.getAccounts().size() == 0) {
                if (Config.X509_VERIFICATION) {
                    intent = new Intent(activity, ManageAccountActivity.class);
                } else if (Config.MAGIC_CREATE_DOMAIN != null) {
                    intent = getSignUpIntent(activity);
                } else {
                    intent = new Intent(activity, EditAccountActivity.class);
                }
            } else {
                intent = new Intent(activity, StartConversationActivity.class);
            }
        }
        intent.putExtra("init", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }
}