package de.pixart.messenger.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import de.pixart.messenger.R;
import de.pixart.messenger.entities.Conversation;
import de.pixart.messenger.entities.Message;
import de.pixart.messenger.entities.MucOptions;
import de.pixart.messenger.entities.Transferable;
import de.pixart.messenger.ui.ConversationFragment;
import de.pixart.messenger.ui.XmppActivity;
import de.pixart.messenger.ui.util.Color;
import de.pixart.messenger.utils.EmojiWrapper;
import de.pixart.messenger.utils.UIHelper;
import de.pixart.messenger.xmpp.chatstate.ChatState;

public class ConversationAdapter extends ArrayAdapter<Conversation> {

    private XmppActivity activity;
    private Conversation selectedConversation = null;

    public ConversationAdapter(XmppActivity activity,
                               List<Conversation> conversations) {
        super(activity, 0, conversations);
        this.activity = activity;
    }

    public static boolean cancelPotentialWork(Conversation conversation, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final Conversation oldConversation = bitmapWorkerTask.conversation;
            if (oldConversation == null || conversation != oldConversation) {
                bitmapWorkerTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.conversation_list_row, parent, false);
        }
        ViewHolder viewHolder = ViewHolder.get(view);
        Conversation conversation = getItem(position);
        if (this.activity instanceof XmppActivity) {
            int c = Color.get(activity, conversation == selectedConversation ? R.attr.color_background_secondary: R.attr.color_background_primary);
            viewHolder.swipeableItem.setBackgroundColor(c);
        }
        if (conversation.getMode() == Conversation.MODE_SINGLE || activity.useSubjectToIdentifyConference()) {
            viewHolder.name.setText(EmojiWrapper.transform(conversation.getName()));
        } else {
            viewHolder.name.setText(conversation.getJid().asBareJid().toString());
        }

        Message message = conversation.getLatestMessage();
        int unreadcount = conversation.unreadCount();
        int failedcount = conversation.failedCount();

        viewHolder.receivedStatus.setVisibility(View.GONE);
        viewHolder.readStatus.setVisibility(View.GONE);

        if (!conversation.isRead()) {
            viewHolder.name.setTypeface(null, Typeface.BOLD);
        } else {
            viewHolder.name.setTypeface(null, Typeface.NORMAL);
        }

        if (unreadcount > 0) {
            viewHolder.unreadCount.setVisibility(View.VISIBLE);
            viewHolder.unreadCount.setText(unreadcount > 99 ? "\u221E" : String.valueOf(unreadcount));
        } else {
            viewHolder.unreadCount.setVisibility(View.GONE);
        }
        if (failedcount > 0) {
            viewHolder.failedCount.setVisibility(View.VISIBLE);
            viewHolder.failedCount.setText(failedcount > 99 ? "\u221E" : String.valueOf(failedcount));
        } else {
            viewHolder.failedCount.setVisibility(View.GONE);
        }

        final boolean fileAvailable = message.getTransferable() == null || message.getTransferable().getStatus() != Transferable.STATUS_DELETED;
        final boolean showPreviewText;
        if (fileAvailable && (message.isFileOrImage() || message.treatAsDownloadable() || message.isGeoUri())) {
            final int imageResource;
            if (message.isGeoUri()) {
                imageResource = activity.getThemeResource(R.attr.ic_attach_location, R.drawable.ic_attach_location);
                showPreviewText = false;
            } else {
                final String mime = message.getMimeType();
                switch (mime == null ? "" : mime.split("/")[0]) {
                    case "image":
                        imageResource = activity.getThemeResource(R.attr.ic_attach_photo, R.drawable.ic_attach_photo);
                        showPreviewText = false;
                        break;
                    case "video":
                        imageResource = activity.getThemeResource(R.attr.ic_attach_video, R.drawable.ic_attach_video);
                        showPreviewText = false;
                        break;
                    case "audio":
                        imageResource = activity.getThemeResource(R.attr.ic_attach_record, R.drawable.ic_attach_record);
                        showPreviewText = false;
                        break;
                    default:
                        imageResource = activity.getThemeResource(R.attr.ic_attach_document, R.drawable.ic_attach_document);
                        showPreviewText = true;
                        break;
                }
            }
            viewHolder.lastMessageIcon.setImageResource(imageResource);
            viewHolder.lastMessageIcon.setVisibility(View.VISIBLE);
        } else {
            viewHolder.lastMessageIcon.setVisibility(View.GONE);
            showPreviewText = true;
        }
        final Pair<String, Boolean> preview = UIHelper.getMessagePreview(activity, message);
        if (showPreviewText) {
            viewHolder.lastMessage.setText(EmojiWrapper.transform(preview.first));
        } else {
            viewHolder.lastMessageIcon.setContentDescription(preview.first);
        }
        viewHolder.lastMessage.setVisibility(showPreviewText ? View.VISIBLE : View.GONE);
        if (preview.second) {
            if (conversation.isRead()) {
                viewHolder.lastMessage.setTypeface(null, Typeface.ITALIC);
                viewHolder.sender.setTypeface(null, Typeface.NORMAL);
            } else {
                viewHolder.lastMessage.setTypeface(null, Typeface.BOLD_ITALIC);
                viewHolder.sender.setTypeface(null, Typeface.BOLD);
            }
        } else {
            if (conversation.isRead()) {
                viewHolder.lastMessage.setTypeface(null, Typeface.NORMAL);
                viewHolder.sender.setTypeface(null, Typeface.NORMAL);
            } else {
                viewHolder.lastMessage.setTypeface(null, Typeface.BOLD);
                viewHolder.sender.setTypeface(null, Typeface.BOLD);
            }
        }
        if (message.getStatus() == Message.STATUS_RECEIVED) {
            if (conversation.getMode() == Conversation.MODE_MULTI) {
                viewHolder.sender.setVisibility(View.VISIBLE);
                viewHolder.sender.setText(UIHelper.getMessageDisplayName(message).split("\\s+")[0] + ':');
            } else {
                viewHolder.sender.setVisibility(View.GONE);
            }
        } else if (message.getType() != Message.TYPE_STATUS) {
            viewHolder.sender.setVisibility(View.VISIBLE);
            viewHolder.sender.setText(activity.getString(R.string.me) + ':');
        } else {
            viewHolder.sender.setVisibility(View.GONE);
        }

        long muted_till = conversation.getLongAttribute(Conversation.ATTRIBUTE_MUTED_TILL, 0);
        if (muted_till == Long.MAX_VALUE) {
            viewHolder.notificationIcon.setVisibility(View.VISIBLE);
            viewHolder.notificationIcon.setImageResource(R.drawable.ic_notifications_off_grey600_24dp);
        } else if (muted_till >= System.currentTimeMillis()) {
            viewHolder.notificationIcon.setVisibility(View.VISIBLE);
            viewHolder.notificationIcon.setImageResource(R.drawable.ic_notifications_paused_grey600_24dp);
        } else if (conversation.alwaysNotify()) {
            viewHolder.notificationIcon.setVisibility(View.GONE);
        } else {
            viewHolder.notificationIcon.setVisibility(View.VISIBLE);
            viewHolder.notificationIcon.setImageResource(R.drawable.ic_notifications_none_grey600_24dp);
        }

        viewHolder.timestamp.setText(UIHelper.readableTimeDifference(activity, conversation.getLatestMessage().getTimeSent()));
        loadAvatar(conversation, viewHolder.avatar);

        if (conversation.getMode() == Conversation.MODE_SINGLE && ShowPresenceColoredNames()) {
            switch (conversation.getContact().getPresences().getShownStatus()) {
                case CHAT:
                case ONLINE:
                    viewHolder.name.setTextColor(ContextCompat.getColor(activity, R.color.online));
                    break;
                case AWAY:
                    viewHolder.name.setTextColor(ContextCompat.getColor(activity, R.color.away));
                    break;
                case XA:
                case DND:
                    viewHolder.name.setTextColor(ContextCompat.getColor(activity, R.color.notavailable));
                    break;
                default:
                    viewHolder.name.setTextColor(ContextCompat.getColor(activity, R.color.black87));
                    break;
            }
        } else {
            viewHolder.name.setTextColor(ContextCompat.getColor(activity, R.color.black87));
        }

        if (activity.xmppConnectionService.indicateReceived()) {
            switch (message.getMergedStatus()) {
                case Message.STATUS_SEND_RECEIVED:
                    viewHolder.receivedStatus.setVisibility(View.VISIBLE);
                    break;
                case Message.STATUS_SEND_DISPLAYED:
                    viewHolder.receivedStatus.setVisibility(View.VISIBLE);
                    viewHolder.readStatus.setVisibility(View.VISIBLE);
                    break;
            }
        }
        if (conversation.getMode() == Conversation.MODE_SINGLE) {
            if (conversation.getIncomingChatState().equals(ChatState.COMPOSING)) {
                viewHolder.lastMessage.setText(R.string.is_typing);
                viewHolder.lastMessage.setTypeface(null, Typeface.BOLD_ITALIC);
                viewHolder.sender.setVisibility(View.GONE);
            }
        } else {
            if (conversation.getParticipants() != null) {
                ChatState state = ChatState.COMPOSING;
                List<MucOptions.User> userWithChatStates = conversation.getMucOptions().getUsersWithChatState(state, 5);
                if (userWithChatStates.size() == 0) {
                    state = ChatState.PAUSED;
                    userWithChatStates = conversation.getMucOptions().getUsersWithChatState(state, 5);
                }
                if (state == ChatState.COMPOSING) {
                    if (userWithChatStates.size() > 0) {
                        if (userWithChatStates.size() == 1) {
                            MucOptions.User user = userWithChatStates.get(0);
                            viewHolder.lastMessage.setText(activity.getString(R.string.contact_is_typing, UIHelper.getDisplayName(user)));
                            viewHolder.lastMessage.setTypeface(null, Typeface.BOLD_ITALIC);
                            viewHolder.sender.setVisibility(View.GONE);
                        } else {
                            StringBuilder builder = new StringBuilder();
                            for (MucOptions.User user : userWithChatStates) {
                                if (builder.length() != 0) {
                                    builder.append(", ");
                                }
                                builder.append(UIHelper.getDisplayName(user));
                            }
                            viewHolder.lastMessage.setText(activity.getString(R.string.contacts_are_typing, builder.toString()));
                            viewHolder.lastMessage.setTypeface(null, Typeface.BOLD_ITALIC);
                            viewHolder.sender.setVisibility(View.GONE);
                        }
                    }
                }
            }
        }
        return view;
    }

    public void loadAvatar(Conversation conversation, ImageView imageView) {
        if (cancelPotentialWork(conversation, imageView)) {
            final Bitmap bm = activity.avatarService().get(conversation, activity.getPixel(56), true);
            if (bm != null) {
                cancelPotentialWork(conversation, imageView);
                imageView.setImageBitmap(bm);
                imageView.setBackgroundColor(0x00000000);
            } else {
                imageView.setBackgroundColor(UIHelper.getColorForName(conversation.getName()));
                imageView.setImageDrawable(null);
                final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
                final AsyncDrawable asyncDrawable = new AsyncDrawable(activity.getResources(), null, task);
                imageView.setImageDrawable(asyncDrawable);
                try {
                    task.executeOnExecutor(BitmapWorkerTask.THREAD_POOL_EXECUTOR, conversation);
                } catch (final RejectedExecutionException ignored) {
                }
            }
        }
    }

    @Override
    public void notifyDataSetChanged() {
        this.selectedConversation = ConversationFragment.getConversation(activity);
        super.notifyDataSetChanged();
    }

    public static class ViewHolder {
        private View swipeableItem;
        private TextView name;
        private TextView lastMessage;
        private ImageView lastMessageIcon;
        private TextView timestamp;
        private TextView sender;
        private ImageView notificationIcon;
        private TextView unreadCount;
        private TextView failedCount;
        private ImageView receivedStatus;
        private ImageView readStatus;
        private ImageView avatar;

        private ViewHolder() {

        }

        public static ViewHolder get(View layout) {
            ViewHolder viewHolder = (ViewHolder) layout.getTag();
            if (viewHolder == null) {
                viewHolder = new ViewHolder();
                viewHolder.swipeableItem = layout.findViewById(R.id.swipeable_item);
                viewHolder.name = layout.findViewById(R.id.conversation_name);
                viewHolder.lastMessage = layout.findViewById(R.id.conversation_lastmsg);
                viewHolder.lastMessageIcon = layout.findViewById(R.id.conversation_lastmsg_img);
                viewHolder.timestamp = layout.findViewById(R.id.conversation_lastupdate);
                viewHolder.avatar = layout.findViewById(R.id.conversation_image);
                viewHolder.sender = layout.findViewById(R.id.sender_name);
                viewHolder.notificationIcon = layout.findViewById(R.id.notification_status);
                viewHolder.unreadCount = layout.findViewById(R.id.conversation_unread);
                viewHolder.failedCount = layout.findViewById(R.id.conversation_failed);
                viewHolder.receivedStatus = layout.findViewById(R.id.indicator_received);
                viewHolder.readStatus = layout.findViewById(R.id.indicator_read);
                layout.setTag(viewHolder);
            }
            return viewHolder;
        }
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    class BitmapWorkerTask extends AsyncTask<Conversation, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private Conversation conversation = null;

        public BitmapWorkerTask(ImageView imageView) {
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(Conversation... params) {
            this.conversation = params[0];
            return activity.avatarService().get(this.conversation, activity.getPixel(56), isCancelled());
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                    imageView.setBackgroundColor(0x00000000);
                }
            }
        }
    }

    public boolean ShowPresenceColoredNames() {
        return getPreferences().getBoolean("presence_colored_names", activity.getResources().getBoolean(R.bool.presence_colored_names));
    }

    protected SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
    }
}