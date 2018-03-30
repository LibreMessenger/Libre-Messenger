package de.pixart.messenger.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.media.ExifInterface;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import net.java.otr4j.session.SessionStatus;

import org.openintents.openpgp.util.OpenPgpApi;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.crypto.axolotl.AxolotlService;
import de.pixart.messenger.crypto.axolotl.FingerprintStatus;
import de.pixart.messenger.databinding.FragmentConversationBinding;
import de.pixart.messenger.entities.Account;
import de.pixart.messenger.entities.Blockable;
import de.pixart.messenger.entities.Contact;
import de.pixart.messenger.entities.Conversation;
import de.pixart.messenger.entities.DownloadableFile;
import de.pixart.messenger.entities.Message;
import de.pixart.messenger.entities.MucOptions;
import de.pixart.messenger.entities.Presence;
import de.pixart.messenger.entities.ReadByMarker;
import de.pixart.messenger.entities.Transferable;
import de.pixart.messenger.entities.TransferablePlaceholder;
import de.pixart.messenger.http.HttpDownloadConnection;
import de.pixart.messenger.persistance.FileBackend;
import de.pixart.messenger.services.MessageArchiveService;
import de.pixart.messenger.services.XmppConnectionService;
import de.pixart.messenger.ui.adapter.MessageAdapter;
import de.pixart.messenger.ui.interfaces.OnSwipeTouchListener;
import de.pixart.messenger.ui.util.ActivityResult;
import de.pixart.messenger.ui.util.AttachmentTool;
import de.pixart.messenger.ui.util.ConversationMenuConfigurator;
import de.pixart.messenger.ui.util.PendingItem;
import de.pixart.messenger.ui.util.PresenceSelector;
import de.pixart.messenger.ui.util.SendButtonAction;
import de.pixart.messenger.ui.util.SendButtonTool;
import de.pixart.messenger.ui.widget.EditMessage;
import de.pixart.messenger.utils.FileUtils;
import de.pixart.messenger.utils.MessageUtils;
import de.pixart.messenger.utils.NickValidityChecker;
import de.pixart.messenger.utils.StylingHelper;
import de.pixart.messenger.utils.UIHelper;
import de.pixart.messenger.xmpp.XmppConnection;
import de.pixart.messenger.xmpp.chatstate.ChatState;
import de.pixart.messenger.xmpp.jid.InvalidJidException;
import de.pixart.messenger.xmpp.jid.Jid;

import static de.pixart.messenger.ui.XmppActivity.EXTRA_ACCOUNT;
import static de.pixart.messenger.ui.XmppActivity.REQUEST_ANNOUNCE_PGP;
import static de.pixart.messenger.ui.XmppActivity.REQUEST_CHOOSE_PGP_ID;
import static de.pixart.messenger.xmpp.Patches.ENCRYPTION_EXCEPTIONS;

public class ConversationFragment extends XmppFragment implements EditMessage.KeyboardListener {

    public static final int REQUEST_SEND_MESSAGE = 0x0201;
    public static final int REQUEST_DECRYPT_PGP = 0x0202;
    public static final int REQUEST_ENCRYPT_MESSAGE = 0x0207;
    public static final int REQUEST_TRUST_KEYS_TEXT = 0x0208;
    public static final int REQUEST_TRUST_KEYS_MENU = 0x0209;
    public static final int REQUEST_START_DOWNLOAD = 0x0210;
    public static final int REQUEST_ADD_EDITOR_CONTENT = 0x0211;
    public static final int ATTACHMENT_CHOICE_CHOOSE_IMAGE = 0x0301;
    public static final int ATTACHMENT_CHOICE_TAKE_FROM_CAMERA = 0x0302;
    public static final int ATTACHMENT_CHOICE_CHOOSE_FILE = 0x0303;
    public static final int ATTACHMENT_CHOICE_RECORD_VOICE = 0x0304;
    public static final int ATTACHMENT_CHOICE_LOCATION = 0x0305;
    public static final int ATTACHMENT_CHOICE_CHOOSE_VIDEO = 0x0306;
    public static final int ATTACHMENT_CHOICE_INVALID = 0x0399;

    public static final String RECENTLY_USED_QUICK_ACTION = "recently_used_quick_action";
    public static final String STATE_CONVERSATION_UUID = ConversationFragment.class.getName() + ".uuid";
	public static final String STATE_SCROLL_POSITION = ConversationFragment.class.getName() + ".scroll_position";

    final protected List<Message> messageList = new ArrayList<>();
    final private List<Uri> mPendingImageUris = new ArrayList<>();
    public Uri mPendingEditorContent = null;
    private final PendingItem<ActivityResult> postponedActivityResult = new PendingItem<>();
	private final PendingItem<String> pendingConversationsUuid = new PendingItem<>();
    private final PendingItem<Bundle> pendingExtras = new PendingItem<>();
    protected MessageAdapter messageListAdapter;
    private Conversation conversation;
	public FragmentConversationBinding binding;
    protected Message lastHistoryMessage = null;
    SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd. MMM yyyy", Locale.getDefault());
    private Toast messageLoaderToast;
    private ConversationActivity activity;
    protected OnClickListener clickToVerify = new OnClickListener() {
        @Override
        public void onClick(View v) {
            activity.verifyOtrSessionDialog(conversation, v);
        }
    };
    private OnClickListener clickToMuc = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), ConferenceDetailsActivity.class);
            intent.setAction(ConferenceDetailsActivity.ACTION_VIEW_MUC);
            intent.putExtra("uuid", conversation.getUuid());
            startActivity(intent);
        }
    };
    private OnClickListener leaveMuc = new OnClickListener() {

        @Override
        public void onClick(View v) {
            activity.xmppConnectionService.archiveConversation(conversation);
            activity.onConversationArchived(conversation);
        }
    };
    private OnClickListener joinMuc = new OnClickListener() {

        @Override
        public void onClick(View v) {
            activity.xmppConnectionService.joinMuc(conversation);
        }
    };
    private OnClickListener enterPassword = new OnClickListener() {

        @Override
        public void onClick(View v) {
            MucOptions muc = conversation.getMucOptions();
            String password = muc.getPassword();
            if (password == null) {
                password = "";
            }
            activity.quickPasswordEdit(password, value -> {
                activity.xmppConnectionService.providePasswordForMuc(conversation, value);
                return null;
            });
        }
    };
    private OnScrollListener mOnScrollListener = new OnScrollListener() {

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onScroll(final AbsListView view, int firstVisibleItem,
                             int visibleItemCount, final int totalItemCount) {
            synchronized (ConversationFragment.this.messageList) {
                if (firstVisibleItem < 25 && conversation != null && conversation.messagesLoaded.compareAndSet(true, false) && messageList.size() > 0) {
                    long timestamp;
                    if (messageList.get(0).getType() == Message.TYPE_STATUS && messageList.size() >= 2) {
                        timestamp = messageList.get(1).getTimeSent();
                    } else {
                        timestamp = messageList.get(0).getTimeSent();
                    }
                    activity.xmppConnectionService.loadMoreMessages(conversation, timestamp, new XmppConnectionService.OnMoreMessagesLoaded() {
                        @Override
                        public void onMoreMessagesLoaded(final int c, final Conversation conversation) {
                            if (ConversationFragment.this.conversation != conversation) {
                                conversation.messagesLoaded.set(true);
                                return;
                            }
                            activity.runOnUiThread(() -> {
                                final int oldPosition = binding.messagesView.getFirstVisiblePosition();
                                Message message = null;
                                int childPos;
                                for (childPos = 0; childPos + oldPosition < messageList.size(); ++childPos) {
                                    message = messageList.get(oldPosition + childPos);
                                    if (message.getType() != Message.TYPE_STATUS) {
                                        break;
                                    }
                                }
                                final String uuid = message != null ? message.getUuid() : null;
                                View v = binding.messagesView.getChildAt(childPos);
                                final int pxOffset = (v == null) ? 0 : v.getTop();
                                ConversationFragment.this.conversation.populateWithMessages(ConversationFragment.this.messageList);
                                try {
                                    updateStatusMessages();
                                } catch (IllegalStateException e) {
                                    Log.d(Config.LOGTAG, "caught illegal state exception while updating status messages");
                                }
                                messageListAdapter.notifyDataSetChanged();
                                int pos = Math.max(getIndexOf(uuid, messageList), 0);
                                binding.messagesView.setSmoothScrollbarEnabled(true);
                                binding.messagesView.setFastScrollEnabled(false);
                                binding.messagesView.setSelectionFromTop(pos, pxOffset);
                                if (messageLoaderToast != null) {
                                    messageLoaderToast.cancel();
                                }
                                conversation.messagesLoaded.set(true);
                            });
                        }

                        @Override
                        public void informUser(final int resId) {

                            activity.runOnUiThread(() -> {
                                if (messageLoaderToast != null) {
                                    messageLoaderToast.cancel();
                                }
                                if (ConversationFragment.this.conversation != conversation) {
                                    return;
                                }
                                messageLoaderToast = Toast.makeText(view.getContext(), resId, Toast.LENGTH_LONG);
                                messageLoaderToast.show();
                            });
                        }
                    });
                }
            }
        }
    };
    private EditMessage.OnCommitContentListener mEditorContentListener = new EditMessage.OnCommitContentListener() {
        @Override
        public boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags, Bundle opts, String[] contentMimeTypes) {
            // try to get permission to read the image, if applicable
            if ((flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
                try {
                    inputContentInfo.requestPermission();
                } catch (Exception e) {
                    Log.e(Config.LOGTAG, "InputContentInfoCompat#requestPermission() failed.", e);
                    Toast.makeText(getActivity(), activity.getString(R.string.no_permission_to_access_x, inputContentInfo.getDescription()), Toast.LENGTH_LONG
                    ).show();
                    return false;
                }
            }
            if (activity.hasStoragePermission(REQUEST_ADD_EDITOR_CONTENT)) {
                attachImageToConversation(inputContentInfo.getContentUri());
            } else {
                mPendingEditorContent = inputContentInfo.getContentUri();
            }
            return true;
        }
    };
    private Message selectedMessage;
    private OnClickListener mEnableAccountListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final Account account = conversation == null ? null : conversation.getAccount();
            if (account != null) {
                account.setOption(Account.OPTION_DISABLED, false);
                activity.xmppConnectionService.updateAccount(account);
            }
        }
    };
    private OnClickListener mUnblockClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            v.post(() -> v.setVisibility(View.INVISIBLE));
            if (conversation.isDomainBlocked()) {
                BlockContactDialog.show(activity, conversation);
            } else {
                unblockConversation(conversation);
            }
        }
    };
    private OnClickListener mBlockClickListener = this::showBlockSubmenu;

    private OnClickListener mAddBackClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final Contact contact = conversation == null ? null : conversation.getContact();
            if (contact != null) {
                activity.xmppConnectionService.createContact(contact);
                activity.switchToContactDetails(contact);
            }
        }
    };
    private View.OnLongClickListener mLongPressBlockListener = v -> {
        showBlockSubmenu(v);
        return true;
    };

    private OnClickListener mHideUnencryptionHint = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Config.supportOmemo() && conversation.getAccount().getAxolotlService().isConversationAxolotlCapable(conversation)) {
                conversation.setNextEncryption(Message.ENCRYPTION_AXOLOTL);
                activity.xmppConnectionService.updateConversation(conversation);
                activity.refreshUi();
            }
            hideSnackbar();
        }
    };
    private OnClickListener mAllowPresenceSubscription = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final Contact contact = conversation == null ? null : conversation.getContact();
            if (contact != null) {
                activity.xmppConnectionService.sendPresencePacket(contact.getAccount(),
                        activity.xmppConnectionService.getPresenceGenerator()
                                .sendPresenceUpdatesTo(contact));
                hideSnackbar();
            }
        }
    };
    private OnClickListener mAnswerSmpClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(activity, VerifyOTRActivity.class);
            intent.setAction(VerifyOTRActivity.ACTION_VERIFY_CONTACT);
            intent.putExtra(EXTRA_ACCOUNT, conversation.getAccount().getJid().toBareJid().toString());
            intent.putExtra(VerifyOTRActivity.EXTRA_ACCOUNT, conversation.getAccount().getJid().toBareJid().toString());
            intent.putExtra("mode", VerifyOTRActivity.MODE_ANSWER_QUESTION);
            startActivity(intent);
        }
    };

    protected OnClickListener clickToDecryptListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            PendingIntent pendingIntent = conversation.getAccount().getPgpDecryptionService().getPendingIntent();
            if (pendingIntent != null) {
                try {
                    getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(),
                            REQUEST_DECRYPT_PGP,
                            null,
                            0,
                            0,
                            0);
                } catch (SendIntentException e) {
                    Toast.makeText(getActivity(), R.string.unable_to_connect_to_keychain, Toast.LENGTH_SHORT).show();
                    conversation.getAccount().getPgpDecryptionService().continueDecryption(true);
                }
            }
            updateSnackBar(conversation);
        }
    };
    private AtomicBoolean mSendingPgpMessage = new AtomicBoolean(false);
    private OnEditorActionListener mEditorActionListener = (v, actionId, event) -> {
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            InputMethodManager imm = (InputMethodManager) v.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm.isFullscreenMode()) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            sendMessage();
            return true;
        } else {
            return false;
        }
    };

    private OnClickListener mSendButtonListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Object tag = v.getTag();
            if (tag instanceof SendButtonAction) {
                SendButtonAction action = (SendButtonAction) tag;
                switch (action) {
                    case TAKE_FROM_CAMERA:
                    case SEND_LOCATION:
                    case RECORD_VOICE:
                    case CHOOSE_PICTURE:
                        attachFile(action.toChoice());
                        break;
                    case CANCEL:
                        if (conversation != null) {
                            if (conversation.setCorrectingMessage(null)) {
                                binding.textinput.setText("");
                                binding.textinput.append(conversation.getDraftMessage());
                                conversation.setDraftMessage(null);
                            } else if (conversation.getMode() == Conversation.MODE_MULTI) {
                                conversation.setNextCounterpart(null);
                            }
                            updateChatMsgHint();
                            updateSendButton();
                            updateEditablity();
                        }
                        break;
                    default:
                        sendMessage();
                }
            } else {
                sendMessage();
            }
        }
    };
    private View.OnLongClickListener mSendButtonLongListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            final String body = binding.textinput.getText().toString();
            if (body.length() == 0) {
                binding.textinput.getText().insert(0, Message.ME_COMMAND + " ");
            }
            return true;
        }
    };
    private int completionIndex = 0;
    private int lastCompletionLength = 0;
    private String incomplete;
    private int lastCompletionCursor;
    private boolean firstWord = false;

    private Message mPendingDownloadableMessage;
    private TextWatcher mSearchTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable editable) {
            String query = editable.toString().trim();

            if ((!query.isEmpty() || !query.contains("")) && query.length() >= 3) {
                binding.searchUp.setVisibility(View.VISIBLE);
                binding.searchDown.setVisibility(View.VISIBLE);
                Message found = searchHistory(query);
                if (found != null) {
                    binding.searchUp.setVisibility(View.VISIBLE);
                    binding.searchDown.setVisibility(View.VISIBLE);
                } else {
                    binding.searchUp.setVisibility(View.GONE);
                    binding.searchDown.setVisibility(View.GONE);
                }
                binding.searchUp.setEnabled(found != null);
                binding.searchDown.setEnabled(found != null);
                View.OnClickListener upDownListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String searchQuery = binding.searchfieldInput.getText().toString().trim();
                        if (!searchQuery.isEmpty() || !searchQuery.contains("")) {
                            searchHistory(searchQuery, view.getId() == R.id.search_up);
                        }

                    }
                };
                binding.searchUp.setOnClickListener(upDownListener);
                binding.searchDown.setOnClickListener(upDownListener);
            } else {
                binding.searchUp.setVisibility(View.GONE);
                binding.searchDown.setVisibility(View.GONE);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            activity.refreshUi();
        }
    };

    private int getIndexOf(String uuid, List<Message> messages) {
        if (uuid == null) {
            return messages.size() - 1;
        }
        for (int i = 0; i < messages.size(); ++i) {
            if (uuid.equals(messages.get(i).getUuid())) {
                return i;
            } else {
                Message next = messages.get(i);
                while (next != null && next.wasMergedIntoPrevious()) {
                    if (uuid.equals(next.getUuid())) {
                        return i;
                    }
                    next = next.next();
                }

            }
        }
        return -1;
    }

    public Pair<Integer, Integer> getScrollPosition() {
        if (this.binding.messagesView.getCount() == 0 ||
                this.binding.messagesView.getLastVisiblePosition() == this.binding.messagesView.getCount() - 1) {
            return null;
        } else {
            final int pos = binding.messagesView.getFirstVisiblePosition();
            final View view = binding.messagesView.getChildAt(0);
            if (view == null) {
                return null;
            } else {
                return new Pair<>(pos, view.getTop());
            }
        }
    }

    public void setScrollPosition(Pair<Integer, Integer> scrollPosition) {
        if (scrollPosition != null) {
            this.binding.messagesView.setSelectionFromTop(scrollPosition.first, scrollPosition.second);
        }
    }

    private void attachLocationToConversation(Conversation conversation, Uri uri) {
        if (conversation == null) {
            return;
        }
        activity.xmppConnectionService.attachLocationToConversation(conversation, uri, new UiCallback<Message>() {

            @Override
            public void success(Message message) {
                activity.xmppConnectionService.sendMessage(message);
            }

            @Override
            public void error(int errorCode, Message object) {

            }

            @Override
            public void userInputRequried(PendingIntent pi, Message object) {

            }
        });
    }

    private void attachFileToConversation(Conversation conversation, Uri uri) {
        if (conversation == null) {
            return;
        }
        final Toast prepareFileToast = Toast.makeText(getActivity(), getText(R.string.preparing_file), Toast.LENGTH_LONG);
        prepareFileToast.show();
        activity.delegateUriPermissionsToService(uri);
        activity.xmppConnectionService.attachFileToConversation(conversation, uri, new UiInformableCallback<Message>() {
            @Override
            public void inform(final String text) {
                hidePrepareFileToast(prepareFileToast);
                activity.runOnUiThread(() -> activity.replaceToast(text));
            }

            @Override
            public void success(Message message) {
                activity.runOnUiThread(() -> activity.hideToast());
                hidePrepareFileToast(prepareFileToast);
                activity.xmppConnectionService.sendMessage(message);
            }

            @Override
            public void error(final int errorCode, Message message) {
                hidePrepareFileToast(prepareFileToast);
                activity.runOnUiThread(() -> activity.replaceToast(getString(errorCode)));

            }

            @Override
            public void userInputRequried(PendingIntent pi, Message message) {
                hidePrepareFileToast(prepareFileToast);
            }
        });
    }

    private void attachPhotoToConversation(Conversation conversation, Uri uri) {
        if (conversation == null) {
            return;
        }
        final Toast prepareFileToast = Toast.makeText(getActivity(), getText(R.string.preparing_image), Toast.LENGTH_LONG);
        prepareFileToast.show();
        activity.delegateUriPermissionsToService(uri);
        activity.xmppConnectionService.attachImageToConversation(conversation, uri,
                new UiCallback<Message>() {

                    @Override
                    public void userInputRequried(PendingIntent pi, Message object) {
                        hidePrepareFileToast(prepareFileToast);
                    }

                    @Override
                    public void success(Message message) {
                        hidePrepareFileToast(prepareFileToast);
                        activity.xmppConnectionService.sendMessage(message);
                    }

                    @Override
                    public void error(final int error, Message message) {
                        hidePrepareFileToast(prepareFileToast);
                        getActivity().runOnUiThread(() -> activity.replaceToast(getString(error)));
                    }
                });
    }

    private void attachImagesToConversation(Conversation conversation, Uri uri) {
        if (conversation == null) {
            return;
        }
        final Toast prepareFileToast = Toast.makeText(getActivity(), getText(R.string.preparing_image), Toast.LENGTH_LONG);
        prepareFileToast.show();
        activity.delegateUriPermissionsToService(uri);
        activity.xmppConnectionService.attachImageToConversation(conversation, uri,
                new UiCallback<Message>() {

                    @Override
                    public void userInputRequried(PendingIntent pi, Message object) {
                        hidePrepareFileToast(prepareFileToast);
                    }

                    @Override
                    public void success(Message message) {
                        hidePrepareFileToast(prepareFileToast);
                        activity.xmppConnectionService.sendMessage(message);
                    }

                    @Override
                    public void error(final int error, Message message) {
                        hidePrepareFileToast(prepareFileToast);
                        getActivity().runOnUiThread(() -> activity.replaceToast(getString(error)));
                    }
                });
    }

    public void attachImageToConversation(Uri uri) {
        this.attachImageToConversation(conversation, uri, true);
    }

    private void attachImageToConversation(Conversation conversation, Uri uri, boolean sendAsIs) {
        if (conversation == null) {
            return;
        }
        if (sendAsIs) {
            sendImage(conversation, uri);
            return;
        }
        final Conversation conversation_preview = conversation;
        final Uri uri_preview = uri;
        Bitmap bitmap = BitmapFactory.decodeFile(FileUtils.getPath(activity, uri));
        File file = null;
        ExifInterface exif = null;
        int orientation = 0;
        try {
            file = new File(FileUtils.getPath(activity, uri));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (file != null) {
            try {
                exif = new ExifInterface(file.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        }
        Log.d(Config.LOGTAG, "EXIF: " + orientation);
        Bitmap rotated_image = null;
        Log.d(Config.LOGTAG, "Rotate image");
        rotated_image = FileBackend.rotateBitmap(file, bitmap, orientation);
        if (rotated_image != null) {
            int scaleSize = 600;
            int originalWidth = rotated_image.getWidth();
            int originalHeight = rotated_image.getHeight();
            int newWidth = -1;
            int newHeight = -1;
            float multFactor;
            if (originalHeight > originalWidth) {
                newHeight = scaleSize;
                multFactor = (float) originalWidth / (float) originalHeight;
                newWidth = (int) (newHeight * multFactor);
            } else if (originalWidth > originalHeight) {
                newWidth = scaleSize;
                multFactor = (float) originalHeight / (float) originalWidth;
                newHeight = (int) (newWidth * multFactor);
            } else if (originalHeight == originalWidth) {
                newHeight = scaleSize;
                newWidth = scaleSize;
            }
            Log.d(Config.LOGTAG, "Scaling preview image from " + originalHeight + "px x " + originalWidth + "px to " + newHeight + "px x " + newWidth + "px");
            Bitmap preview = Bitmap.createScaledBitmap(rotated_image, newWidth, newHeight, false);
            ImageView ImagePreview = new ImageView(activity);
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            ImagePreview.setLayoutParams(vp);
            ImagePreview.setMaxWidth(newWidth);
            ImagePreview.setMaxHeight(newHeight);
            //ImagePreview.setScaleType(ImageView.ScaleType.FIT_XY);
            //ImagePreview.setAdjustViewBounds(true);
            ImagePreview.setPadding(5, 5, 5, 5);
            ImagePreview.setImageBitmap(preview);
            getActivity().runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setView(ImagePreview);
                builder.setTitle(R.string.send_image);
                builder.setPositiveButton(R.string.ok, (dialog, which) -> sendImage(conversation_preview, uri_preview));
                builder.setOnCancelListener(dialog -> mPendingImageUris.clear());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    builder.setOnDismissListener(dialog -> mPendingImageUris.clear());
                }
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            });
        } else {
            getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), getText(R.string.error_file_not_found), Toast.LENGTH_LONG).show());
        }
    }

    private void sendImage(Conversation conversation, Uri uri) {
        final Toast prepareFileToast = Toast.makeText(getActivity(), getText(R.string.preparing_image), Toast.LENGTH_LONG);
        prepareFileToast.show();
        activity.xmppConnectionService.attachImageToConversation(conversation, uri,
                new UiCallback<Message>() {

                    @Override
                    public void userInputRequried(PendingIntent pi, Message object) {
                        hidePrepareFileToast(prepareFileToast);
                    }

                    @Override
                    public void success(Message message) {
                        hidePrepareFileToast(prepareFileToast);
                        activity.xmppConnectionService.sendMessage(message);
                    }

                    @Override
                    public void error(final int error, Message message) {
                        hidePrepareFileToast(prepareFileToast);
                        getActivity().runOnUiThread(() -> activity.replaceToast(getString(error)));
                    }
                });
    }

    private void hidePrepareFileToast(final Toast prepareFileToast) {
        if (prepareFileToast != null) {
            getActivity().runOnUiThread(prepareFileToast::cancel);
        }
    }

    private void sendMessage() {
        final String body = binding.textinput.getText().toString();
        final Conversation conversation = this.conversation;
        if (body.length() == 0 || conversation == null) {
            return;
        }
        final Message message;
        if (conversation.getCorrectingMessage() == null) {
            message = new Message(conversation, body, conversation.getNextEncryption());
            if (conversation.getMode() == Conversation.MODE_MULTI) {
                final Jid nextCounterpart = conversation.getNextCounterpart();
                if (nextCounterpart != null) {
                    message.setCounterpart(nextCounterpart);
                    message.setTrueCounterpart(conversation.getMucOptions().getTrueCounterpart(nextCounterpart));
                    message.setType(Message.TYPE_PRIVATE);
                }
            }
        } else {
            message = conversation.getCorrectingMessage();
            message.setBody(body);
            message.setEdited(message.getUuid());
            message.setUuid(UUID.randomUUID().toString());
        }
        switch (message.getConversation().getNextEncryption()) {
            case Message.ENCRYPTION_OTR:
                sendOtrMessage(message);
                break;
            case Message.ENCRYPTION_PGP:
                sendPgpMessage(message);
                break;
            case Message.ENCRYPTION_AXOLOTL:
                if (!trustKeysIfNeeded(REQUEST_TRUST_KEYS_TEXT)) {
                    sendAxolotlMessage(message);
                }
                break;
            default:
                sendPlainTextMessage(message);
        }
    }

    protected boolean trustKeysIfNeeded(int requestCode) {
        return trustKeysIfNeeded(requestCode, ATTACHMENT_CHOICE_INVALID);
    }

    protected boolean trustKeysIfNeeded(int requestCode, int attachmentChoice) {
        AxolotlService axolotlService = conversation.getAccount().getAxolotlService();
        final List<Jid> targets = axolotlService.getCryptoTargets(conversation);
        boolean hasUnaccepted = !conversation.getAcceptedCryptoTargets().containsAll(targets);
        boolean hasUndecidedOwn = !axolotlService.getKeysWithTrust(FingerprintStatus.createActiveUndecided()).isEmpty();
        boolean hasUndecidedContacts = !axolotlService.getKeysWithTrust(FingerprintStatus.createActiveUndecided(), targets).isEmpty();
        boolean hasPendingKeys = !axolotlService.findDevicesWithoutSession(conversation).isEmpty();
        boolean hasNoTrustedKeys = axolotlService.anyTargetHasNoTrustedKeys(targets);
        if (hasUndecidedOwn || hasUndecidedContacts || hasPendingKeys || hasNoTrustedKeys || hasUnaccepted) {
            axolotlService.createSessionsIfNeeded(conversation);
            Intent intent = new Intent(getActivity(), TrustKeysActivity.class);
            String[] contacts = new String[targets.size()];
            for (int i = 0; i < contacts.length; ++i) {
                contacts[i] = targets.get(i).toString();
            }
            intent.putExtra("contacts", contacts);
            intent.putExtra(EXTRA_ACCOUNT, conversation.getAccount().getJid().toBareJid().toString());
            intent.putExtra("choice", attachmentChoice);
            intent.putExtra("conversation", conversation.getUuid());
            startActivityForResult(intent, requestCode);
            return true;
        } else {
            return false;
        }
    }

    public void updateChatMsgHint() {
        final boolean multi = conversation.getMode() == Conversation.MODE_MULTI;
        if (conversation.getCorrectingMessage() != null) {
            this.binding.textinput.setHint(R.string.send_corrected_message);
            hideMessageHint();
        } else if (multi && conversation.getNextCounterpart() != null) {
            this.binding.textinput.setHint(getString(
                    R.string.send_private_message_to,
                    conversation.getNextCounterpart().getResourcepart()));
            showMessageHint(getString(
                    R.string.send_private_message_to,
                    conversation.getNextCounterpart().getResourcepart()));
        } else if (multi && !conversation.getMucOptions().participating()) {
            this.binding.textinput.setHint(R.string.you_are_not_participating);
            hideMessageHint();
        } else {
            this.binding.textinput.setHint(UIHelper.getMessageHint(getActivity(), conversation));
            hideMessageHint();
            getActivity().invalidateOptionsMenu();
        }
    }

    public void setupIme() {
        this.binding.textinput.refreshIme();
    }

    private void handleActivityResult(ActivityResult activityResult) {
        if (activityResult.resultCode == Activity.RESULT_OK) {
            handlePositiveActivityResult(activityResult.requestCode, activityResult.data);
        } else {
            handleNegativeActivityResult(activityResult.requestCode);
        }
    }

    private void handlePositiveActivityResult(int requestCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_DECRYPT_PGP:
                conversation.getAccount().getPgpDecryptionService().continueDecryption(data);
                break;
            case REQUEST_TRUST_KEYS_TEXT:
                final String body = binding.textinput.getText().toString();
                Message message = new Message(conversation, body, conversation.getNextEncryption());
                sendAxolotlMessage(message);
                break;
            case REQUEST_TRUST_KEYS_MENU:
                int choice = data.getIntExtra("choice", ATTACHMENT_CHOICE_INVALID);
                selectPresenceToAttachFile(choice);
                break;
            case REQUEST_CHOOSE_PGP_ID:
                long id = data.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, 0);
                if (id != 0) {
                    conversation.getAccount().setPgpSignId(id);
                    activity.announcePgp(conversation.getAccount(), null, null, activity.onOpenPGPKeyPublished);
                } else {
                    activity.choosePgpSignId(conversation.getAccount());
                }
                break;
            case REQUEST_ANNOUNCE_PGP:
                activity.announcePgp(conversation.getAccount(), conversation, data, activity.onOpenPGPKeyPublished);
                break;
            case ATTACHMENT_CHOICE_CHOOSE_IMAGE:
                List<Uri> imageUris = AttachmentTool.extractUriFromIntent(data);
                for (Iterator<Uri> i = imageUris.iterator(); i.hasNext(); i.remove()) {
                    Log.d(Config.LOGTAG, "ConversationsActivity.onActivityResult() - attaching image to conversations. CHOOSE_IMAGE");
                    attachImageToConversation(conversation, i.next(), false);
                }
                break;
            case ATTACHMENT_CHOICE_TAKE_FROM_CAMERA:
                //TODO save photo uri
                final List<Uri> PhotoUris = AttachmentTool.extractUriFromIntent(data);
                for (Iterator<Uri> i = PhotoUris.iterator(); i.hasNext(); i.remove()) {
                    Log.d(Config.LOGTAG, "ConversationsActivity.onActivityResult() - attaching image to conversations. TAKE_FROM_CAMERA");
                    attachPhotoToConversation(conversation, i.next());
                }
            case ATTACHMENT_CHOICE_CHOOSE_FILE:
            case ATTACHMENT_CHOICE_RECORD_VOICE:
                final List<Uri> fileUris = AttachmentTool.extractUriFromIntent(data);
                final PresenceSelector.OnPresenceSelected callback = () -> {
                    for (Iterator<Uri> i = fileUris.iterator(); i.hasNext(); i.remove()) {
                        Log.d(Config.LOGTAG, "ConversationsActivity.onActivityResult() - attaching file to conversations. CHOOSE_FILE/RECORD_VOICE");
                        attachFileToConversation(conversation, i.next());
                    }
                };
                if (conversation == null || conversation.getMode() == Conversation.MODE_MULTI || FileBackend.allFilesUnderSize(getActivity(), fileUris, getMaxHttpUploadSize(conversation))) {
                    callback.onPresenceSelected();
                } else {
                    activity.selectPresence(conversation, callback);
                }
                break;
            case ATTACHMENT_CHOICE_LOCATION:
                double latitude = data.getDoubleExtra("latitude", 0);
                double longitude = data.getDoubleExtra("longitude", 0);
                Uri geo = Uri.parse("geo:" + String.valueOf(latitude) + "," + String.valueOf(longitude));
                attachLocationToConversation(conversation, geo);
                break;
        }
    }

    private void handleNegativeActivityResult(int requestCode) {
        switch (requestCode) {
            case REQUEST_DECRYPT_PGP:
                // discard the message to prevent decryption being blocked
                conversation.getAccount().getPgpDecryptionService().giveUpCurrentDecryption();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ActivityResult activityResult = ActivityResult.of(requestCode, resultCode, data);
        if (activity != null && activity.xmppConnectionService != null) {
            handleActivityResult(activityResult);
        } else {
            this.postponedActivityResult.push(activityResult);
        }
    }

    public void unblockConversation(final Blockable conversation) {
        activity.xmppConnectionService.sendUnblockRequest(conversation);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(Config.LOGTAG, "ConversationFragment.onAttach()");
        if (activity instanceof ConversationActivity) {
            this.activity = (ConversationActivity) activity;
        } else {
            throw new IllegalStateException("Trying to attach fragment to activity that is not the ConversationActivity");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.activity = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.fragment_conversation, menu);
        final MenuItem menuInviteContact = menu.findItem(R.id.action_invite);
        final MenuItem menuUpdater = menu.findItem(R.id.action_check_updates);
        final MenuItem menuSearchHistory = menu.findItem(R.id.action_search_history);

        if (conversation != null) {
            ConversationMenuConfigurator.configureAttachmentMenu(conversation, menu);
            ConversationMenuConfigurator.configureEncryptionMenu(conversation, menu);
        }
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.binding = DataBindingUtil.inflate(inflater,R.layout.fragment_conversation,container,false);
        binding.getRoot().setOnClickListener(null); //TODO why the fuck did we do this?

        binding.textinput.addTextChangedListener(new StylingHelper.MessageEditorStyler(binding.textinput));
        binding.textinput.setOnEditorActionListener(mEditorActionListener);
        binding.textinput.setRichContentListener(new String[]{"image/*"}, mEditorContentListener);

        binding.textSendButton.setOnClickListener(this.mSendButtonListener);
        binding.textSendButton.setOnLongClickListener(this.mSendButtonLongListener);

        binding.messagesView.setOnScrollListener(mOnScrollListener);
        binding.messagesView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        messageListAdapter = new MessageAdapter((XmppActivity) getActivity(), this.messageList);
        messageListAdapter.setOnContactPictureClicked(message -> {
            final boolean received = message.getStatus() <= Message.STATUS_RECEIVED;
            if (received) {
                if (message.getConversation().getMode() == Conversation.MODE_MULTI) {
                    Jid user = message.getCounterpart();
                    if (user != null && !user.isBareJid()) {
                        if (!message.getConversation().getMucOptions().isUserInRoom(user)) {
                            Toast.makeText(activity, getActivity().getString(R.string.user_has_left_conference, user.getResourcepart()), Toast.LENGTH_SHORT).show();
                        }
                        highlightInConference(user.getResourcepart());
                    }
                    return;
                } else {
                    if (!message.getContact().isSelf()) {
                        String fingerprint;
                        if (message.getEncryption() == Message.ENCRYPTION_PGP
                                || message.getEncryption() == Message.ENCRYPTION_DECRYPTED) {
                            fingerprint = "pgp";
                        } else {
                            fingerprint = message.getFingerprint();
                        }
                        activity.switchToContactDetails(message.getContact(), fingerprint);
                        return;
                    }
                }
            }
            Account account = message.getConversation().getAccount();
            Intent intent;
            if (activity.manuallyChangePresence() && !received) {
                intent = new Intent(activity, SetPresenceActivity.class);
                intent.putExtra(EXTRA_ACCOUNT, account.getJid().toBareJid().toString());
            } else {
                intent = new Intent(activity, EditAccountActivity.class);
                intent.putExtra("jid", account.getJid().toBareJid().toString());
                String fingerprint;
                if (message.getEncryption() == Message.ENCRYPTION_PGP
                        || message.getEncryption() == Message.ENCRYPTION_DECRYPTED) {
                    fingerprint = "pgp";
                } else if (message.getEncryption() == Message.ENCRYPTION_OTR) {
                    fingerprint = "otr";
                } else {
                    fingerprint = message.getFingerprint();
                }
                intent.putExtra("fingerprint", fingerprint);
            }
            startActivity(intent);
        });
        messageListAdapter.setOnContactPictureLongClicked(message -> {
            if (message.getStatus() <= Message.STATUS_RECEIVED) {
                if (message.getConversation().getMode() == Conversation.MODE_MULTI) {
                    final MucOptions mucOptions = conversation.getMucOptions();
                    if (!mucOptions.allowPm()) {
                        Toast.makeText(getActivity(), R.string.private_messages_are_disabled, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Jid user = message.getCounterpart();
                    if (user != null && !user.isBareJid()) {
                        if (mucOptions.isUserInRoom(user)) {
                            privateMessageWith(user);
                        } else {
                            Toast.makeText(getActivity(), activity.getString(R.string.user_has_left_conference, user.getResourcepart()), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } else {
                activity.showQrCode();
            }
        });
        messageListAdapter.setOnQuoteListener(this::quoteText);
        binding.messagesView.setAdapter(messageListAdapter);

        registerForContextMenu(binding.messagesView);
        return binding.getRoot();
    }

    private void quoteText(String text) {
        if (binding.textinput.isEnabled()) {
            text = text.replaceAll("(\n *){2,}", "\n").replaceAll("(^|\n)", "$1> ").replaceAll("\n$", "");
            Editable editable = binding.textinput.getEditableText();
            int position = binding.textinput.getSelectionEnd();
            if (position == -1) position = editable.length();
            if (position > 0 && editable.charAt(position - 1) != '\n') {
                editable.insert(position++, "\n");
            }
            editable.insert(position, text);
            position += text.length();
            editable.insert(position++, "\n");
            if (position < editable.length() && editable.charAt(position) != '\n') {
                editable.insert(position, "\n");
            }
            binding.textinput.setSelection(position);
            binding.textinput.requestFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(binding.textinput, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private void quoteMessage(Message message) {
        quoteText(MessageUtils.prepareQuote(message));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        synchronized (this.messageList) {
            super.onCreateContextMenu(menu, v, menuInfo);
            AdapterView.AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) menuInfo;
            this.selectedMessage = this.messageList.get(acmi.position);
            populateContextMenu(menu);
        }
    }

    private void populateContextMenu(ContextMenu menu) {
        final Message m = this.selectedMessage;
        final Transferable t = m.getTransferable();
        Message relevantForCorrection = m;
        while (relevantForCorrection.mergeable(relevantForCorrection.next())) {
            relevantForCorrection = relevantForCorrection.next();
        }
        if (m.getType() != Message.TYPE_STATUS) {
            final boolean treatAsFile = m.getType() != Message.TYPE_TEXT
                    && m.getType() != Message.TYPE_PRIVATE
                    && t == null;
            final boolean encrypted = m.getEncryption() == Message.ENCRYPTION_DECRYPTION_FAILED
                    || m.getEncryption() == Message.ENCRYPTION_PGP;
            activity.getMenuInflater().inflate(R.menu.message_context, menu);
            menu.setHeaderTitle(R.string.message_options);
            MenuItem copyMessage = menu.findItem(R.id.copy_message);
            MenuItem quoteMessage = menu.findItem(R.id.quote_message);
            MenuItem retryDecryption = menu.findItem(R.id.retry_decryption);
            MenuItem correctMessage = menu.findItem(R.id.correct_message);
            MenuItem shareWith = menu.findItem(R.id.share_with);
            MenuItem sendAgain = menu.findItem(R.id.send_again);
            MenuItem copyUrl = menu.findItem(R.id.copy_url);
            MenuItem cancelTransmission = menu.findItem(R.id.cancel_transmission);
            MenuItem downloadFile = menu.findItem(R.id.download_file);
            MenuItem deleteFile = menu.findItem(R.id.delete_file);
            MenuItem showErrorMessage = menu.findItem(R.id.show_error_message);
            if (!treatAsFile && !encrypted && !m.isGeoUri() && !m.treatAsDownloadable()) {
                copyMessage.setVisible(true);
                quoteMessage.setVisible(MessageUtils.prepareQuote(m).length() > 0);
            }
            if (m.getEncryption() == Message.ENCRYPTION_DECRYPTION_FAILED) {
                retryDecryption.setVisible(true);
            }
            if (relevantForCorrection.getType() == Message.TYPE_TEXT
                    && relevantForCorrection.isLastCorrectableMessage()
                    && (m.getConversation().getMucOptions().nonanonymous() || m.getConversation().getMode() == Conversation.MODE_SINGLE)) {
                correctMessage.setVisible(true);
            }
            if (treatAsFile || (m.getType() == Message.TYPE_TEXT && !m.treatAsDownloadable())) {
                shareWith.setVisible(true);

            }
            if (m.getStatus() == Message.STATUS_SEND_FAILED && !m.isFileOrImage()) {
                sendAgain.setVisible(true);
            }
            if (m.hasFileOnRemoteHost()
                    || m.isGeoUri()
                    || m.isXmppUri()
                    || m.treatAsDownloadable()
                    || (t != null && t instanceof HttpDownloadConnection)) {
                copyUrl.setVisible(true);
            }
            if ((m.isFileOrImage() && t instanceof TransferablePlaceholder && m.hasFileOnRemoteHost())) {
                downloadFile.setVisible(true);
                downloadFile.setTitle(activity.getString(R.string.download_x_file, UIHelper.getFileDescriptionString(activity, m)));
            }
            boolean waitingOfferedSending = m.getStatus() == Message.STATUS_WAITING
                    || m.getStatus() == Message.STATUS_UNSEND
                    || m.getStatus() == Message.STATUS_OFFERED;
            if ((t != null && !(t instanceof TransferablePlaceholder)) || waitingOfferedSending && m.needsUploading()) {
                cancelTransmission.setVisible(true);
            }
            if (treatAsFile) {
                String path = m.getRelativeFilePath();
                Log.d(Config.LOGTAG, "Path = " + path);
                if (path == null || !path.startsWith("/") || path.contains(FileBackend.getConversationsDirectory("null", false))) {
                    deleteFile.setVisible(true);
                    deleteFile.setTitle(activity.getString(R.string.delete_x_file, UIHelper.getFileDescriptionString(activity, m)));
                }
            }
            if (m.getStatus() == Message.STATUS_SEND_FAILED && m.getErrorMessage() != null) {
                showErrorMessage.setVisible(true);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share_with:
                shareWith(selectedMessage);
                return true;
            case R.id.correct_message:
                correctMessage(selectedMessage);
                return true;
            case R.id.copy_message:
                copyMessage(selectedMessage);
                return true;
            case R.id.quote_message:
                quoteMessage(selectedMessage);
                return true;
            case R.id.send_again:
                resendMessage(selectedMessage);
                return true;
            case R.id.copy_url:
                copyUrl(selectedMessage);
                return true;
            case R.id.download_file:
                downloadFile(selectedMessage);
                return true;
            case R.id.cancel_transmission:
                cancelTransmission(selectedMessage);
                return true;
            case R.id.retry_decryption:
                retryDecryption(selectedMessage);
                return true;
            case R.id.delete_file:
                deleteFile(selectedMessage);
                return true;
            case R.id.show_error_message:
                showErrorMessage(selectedMessage);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (conversation == null) {
            return super.onOptionsItemSelected(item);
        }
        switch (item.getItemId()) {
            case R.id.encryption_choice_axolotl:
            case R.id.encryption_choice_otr:
            case R.id.encryption_choice_pgp:
            case R.id.encryption_choice_none:
                handleEncryptionSelection(item);
                break;
            case R.id.attach_choose_picture:
            case R.id.attach_take_picture:
            case R.id.attach_choose_file:
            case R.id.attach_record_voice:
            case R.id.attach_location:
                handleAttachmentSelection(item);
                break;
            case R.id.action_archive_chat:
                activity.xmppConnectionService.archiveConversation(conversation);
                activity.onConversationArchived(conversation);
                break;
            case R.id.action_archive_muc:
                activity.runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getString(R.string.action_end_conversation_muc));
                    builder.setMessage(getString(R.string.leave_conference_warning));
                    builder.setNegativeButton(getString(R.string.cancel), null);
                    builder.setPositiveButton(getString(R.string.action_end_conversation_muc),
                            (dialog, which) -> {
                                activity.xmppConnectionService.archiveConversation(conversation);
                                activity.onConversationArchived(conversation);
                            });
                    builder.create().show();
                });
                break;
            case R.id.action_invite:
                activity.inviteToConversation(conversation);
                break;
            case R.id.action_clear_history:
                clearHistoryDialog(conversation);
                break;
            case R.id.action_block:
            case R.id.action_unblock:
                final Activity activity = getActivity();
                if (activity instanceof XmppActivity) {
                    BlockContactDialog.show((XmppActivity) activity, conversation);
                }
                break;
            case R.id.action_search_history:
                showSearchField();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleAttachmentSelection(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.attach_choose_picture:
                attachFile(ATTACHMENT_CHOICE_CHOOSE_IMAGE);
                break;
            case R.id.attach_take_picture:
                attachFile(ATTACHMENT_CHOICE_TAKE_FROM_CAMERA);
                break;
            case R.id.attach_choose_file:
                attachFile(ATTACHMENT_CHOICE_CHOOSE_FILE);
                break;
            case R.id.attach_record_voice:
                attachFile(ATTACHMENT_CHOICE_RECORD_VOICE);
                break;
            case R.id.attach_location:
                attachFile(ATTACHMENT_CHOICE_LOCATION);
                break;
        }
    }

    private void handleEncryptionSelection(MenuItem item) {
        if (conversation == null) {
            return;
        }
        switch (item.getItemId()) {
            case R.id.encryption_choice_none:
                conversation.setNextEncryption(Message.ENCRYPTION_NONE);
                item.setChecked(true);
                break;
            case R.id.encryption_choice_otr:
                conversation.setNextEncryption(Message.ENCRYPTION_OTR);
                item.setChecked(true);
                break;
            case R.id.encryption_choice_pgp:
                if (activity.hasPgp()) {
                    if (conversation.getAccount().getPgpSignature() != null) {
                        conversation.setNextEncryption(Message.ENCRYPTION_PGP);
                        item.setChecked(true);
                    } else {
                        activity.announcePgp(conversation.getAccount(), conversation, null, activity.onOpenPGPKeyPublished);
                    }
                } else {
                    activity.showInstallPgpDialog();
                }
                break;
            case R.id.encryption_choice_axolotl:
                Log.d(Config.LOGTAG, AxolotlService.getLogprefix(conversation.getAccount())
                        + "Enabled axolotl for Contact " + conversation.getContact().getJid());
                conversation.setNextEncryption(Message.ENCRYPTION_AXOLOTL);
                item.setChecked(true);
                break;
            default:
                conversation.setNextEncryption(Message.ENCRYPTION_NONE);
                break;
        }
        activity.xmppConnectionService.updateConversation(conversation);
        updateChatMsgHint();
        getActivity().invalidateOptionsMenu();
        activity.refreshUi();
    }

    public void attachFile(final int attachmentChoice) {
        if (attachmentChoice != ATTACHMENT_CHOICE_LOCATION) {
            if (!Config.ONLY_INTERNAL_STORAGE && !activity.hasStoragePermission(attachmentChoice)) {
                return;
            }
        }
        if (attachmentChoice == ATTACHMENT_CHOICE_RECORD_VOICE) {
            if (!activity.hasMicPermission(attachmentChoice)) {
                return;
            }
        }
        if (attachmentChoice == ATTACHMENT_CHOICE_LOCATION) {
            if (!activity.hasLocationPermission(attachmentChoice)) {
                return;
            }
        }
        try {
            activity.getPreferences().edit()
                    .putString(RECENTLY_USED_QUICK_ACTION, SendButtonAction.of(attachmentChoice).toString())
                    .apply();
        } catch (IllegalArgumentException e) {
            //just do not save
        }
        final int encryption = conversation.getNextEncryption();
        final int mode = conversation.getMode();
        if (encryption == Message.ENCRYPTION_PGP) {
            if (activity.hasPgp()) {
                if (mode == Conversation.MODE_SINGLE && conversation.getContact().getPgpKeyId() != 0) {
                    activity.xmppConnectionService.getPgpEngine().hasKey(
                            conversation.getContact(),
                            new UiCallback<Contact>() {

                                @Override
                                public void userInputRequried(PendingIntent pi, Contact contact) {
                                    startPendingIntent(pi, attachmentChoice);
                                }

                                @Override
                                public void success(Contact contact) {
                                    selectPresenceToAttachFile(attachmentChoice);
                                }

                                @Override
                                public void error(int error, Contact contact) {
                                    activity.replaceToast(getString(error));
                                }
                            });
                } else if (mode == Conversation.MODE_MULTI && conversation.getMucOptions().pgpKeysInUse()) {
                    if (!conversation.getMucOptions().everybodyHasKeys()) {
                        getActivity().runOnUiThread(() -> {
                            Toast warning = Toast.makeText(activity, R.string.missing_public_keys, Toast.LENGTH_LONG);
                            warning.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                            warning.show();
                        });
                    }
                    selectPresenceToAttachFile(attachmentChoice);
                } else {
                    final ConversationFragment fragment = (ConversationFragment) getFragmentManager()
                            .findFragmentByTag("conversation");
                    if (fragment != null) {
                        fragment.showNoPGPKeyDialog(false, (dialog, which) -> {
                            conversation.setNextEncryption(Message.ENCRYPTION_NONE);
                            activity.xmppConnectionService.updateConversation(conversation);
                            selectPresenceToAttachFile(attachmentChoice);
                        });
                    }
                }
            } else {
                activity.showInstallPgpDialog();
            }
        } else {
            if (encryption != Message.ENCRYPTION_AXOLOTL || !trustKeysIfNeeded(REQUEST_TRUST_KEYS_MENU, attachmentChoice)) {
                selectPresenceToAttachFile(attachmentChoice);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestCode == REQUEST_START_DOWNLOAD) {
                    if (this.mPendingDownloadableMessage != null) {
                        startDownloadable(this.mPendingDownloadableMessage);
                    }
                } else if (requestCode == REQUEST_ADD_EDITOR_CONTENT) {
                    if (this.mPendingEditorContent != null) {
                        attachImageToConversation(this.mPendingEditorContent);
                    }
                } else {
                    attachFile(requestCode);
                }
            } else {
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), R.string.no_permission, Toast.LENGTH_SHORT).show());
            }
        }
    }

    public void startDownloadable(Message message) {
        if (!Config.ONLY_INTERNAL_STORAGE && !activity.hasStoragePermission(REQUEST_START_DOWNLOAD)) {
            this.mPendingDownloadableMessage = message;
            return;
        }
        Transferable transferable = message.getTransferable();
        if (transferable != null) {
            if (!transferable.start()) {
                Toast.makeText(getActivity(), R.string.not_connected_try_again, Toast.LENGTH_SHORT).show();
            }
        } else if (message.treatAsDownloadable()) {
            activity.xmppConnectionService.getHttpConnectionManager().createNewDownloadConnection(message, true);
        }
    }

    @SuppressLint("InflateParams")
    protected void clearHistoryDialog(final Conversation conversation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.clear_conversation_history));
        final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_clear_history, null);
        final CheckBox endConversationCheckBox = dialogView.findViewById(R.id.end_conversation_checkbox);
        if (conversation.getMode() == Conversation.MODE_SINGLE) {
            endConversationCheckBox.setVisibility(View.VISIBLE);
            endConversationCheckBox.setChecked(true);
        }
        builder.setView(dialogView);
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.setPositiveButton(getString(R.string.delete_messages), (dialog, which) -> {
            this.activity.xmppConnectionService.clearConversationHistory(conversation);
            if (endConversationCheckBox.isChecked()) {
                this.activity.xmppConnectionService.archiveConversation(conversation);
                this.activity.onConversationArchived(conversation);
            } else {
                activity.onConversationsListItemUpdated();
                refresh();
            }
        });
        builder.create().show();
    }

    protected void selectPresenceToAttachFile(final int attachmentChoice) {
        final int encryption = conversation.getNextEncryption();
        final Account account = conversation.getAccount();
        final PresenceSelector.OnPresenceSelected callback = () -> {
            final Intent intent = new Intent();
            boolean chooser = false;
            String fallbackPackageId = null;
            switch (attachmentChoice) {
                case ATTACHMENT_CHOICE_CHOOSE_VIDEO:
                    chooser = true;
                    intent.setType("video/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    break;
                case ATTACHMENT_CHOICE_TAKE_FROM_CAMERA:
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getString(R.string.attach_take_from_camera));
                    builder.setNegativeButton(getString(R.string.action_take_photo),
                            (dialog, which) -> {
                                Uri uri = activity.xmppConnectionService.getFileBackend().getTakePhotoUri();
                                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                                startActivityForResult(intent, attachmentChoice);
                            });
                    builder.setPositiveButton(getString(R.string.action_take_video),
                            (dialog, which) -> {
                                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                intent.setAction(MediaStore.ACTION_VIDEO_CAPTURE);
                                startActivityForResult(intent, attachmentChoice);
                            });
                    builder.create().show();
                    break;
                case ATTACHMENT_CHOICE_CHOOSE_FILE:
                    chooser = true;
                    intent.setType("*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    break;
                case ATTACHMENT_CHOICE_RECORD_VOICE:
                    startActivityForResult(new Intent(getActivity(), RecordingActivity.class), attachmentChoice);
                    break;
                case ATTACHMENT_CHOICE_LOCATION:
                    startActivityForResult(new Intent(getActivity(), ShareLocationActivity.class), attachmentChoice);
                    break;
            }
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                Log.d(Config.LOGTAG, "Attachment: " + attachmentChoice);
                if (chooser) {
                    startActivityForResult(
                            Intent.createChooser(intent, getString(R.string.perform_action_with)),
                            attachmentChoice);
                } else {
                    startActivityForResult(intent, attachmentChoice);
                }
            } else if (fallbackPackageId != null) {
                startActivity(getInstallApkIntent(fallbackPackageId));
            }
        };
        if (account.httpUploadAvailable() || attachmentChoice == ATTACHMENT_CHOICE_LOCATION) {
        if ((account.httpUploadAvailable() || attachmentChoice == ATTACHMENT_CHOICE_LOCATION) && encryption != Message.ENCRYPTION_OTR) {
                conversation.setNextCounterpart(null);
                callback.onPresenceSelected();
            } else {
                activity.selectPresence(conversation, callback);
            }
        }
    }

    private Intent getInstallApkIntent(final String packageId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + packageId));
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            return intent;
        } else {
            intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + packageId));
            return intent;
        }
    }

    @Override
    public void onResume() {
        new Handler().post(() -> {
            final Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            getActivity().invalidateOptionsMenu();
        });
        super.onResume();
    }

    private void showErrorMessage(final Message message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.error_message);
        builder.setMessage(message.getErrorMessage());
        builder.setPositiveButton(R.string.ok, null);
        builder.create().show();
    }

    private void shareWith(Message message) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        if (message.isGeoUri() || message.isXmppUri()) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, message.getBody());
            shareIntent.setType("text/plain");
        } else if (!message.isFileOrImage()) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, message.getMergedBody().toString());
            shareIntent.setType("text/plain");
        } else {
            final DownloadableFile file = activity.xmppConnectionService.getFileBackend().getFile(message);
            try {
                shareIntent.putExtra(Intent.EXTRA_STREAM, FileBackend.getUriForFile(getActivity(), file));
            } catch (SecurityException e) {
                Toast.makeText(getActivity(), activity.getString(R.string.no_permission_to_access_x, file.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                return;
            }
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String mime = message.getMimeType();
            if (mime == null) {
                mime = "*/*";
            }
            shareIntent.setType(mime);
        }
        try {
            startActivity(Intent.createChooser(shareIntent, getText(R.string.share_with)));
        } catch (ActivityNotFoundException e) {
            //This should happen only on faulty androids because normally chooser is always available
            Toast.makeText(getActivity(), R.string.no_application_found_to_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    private void copyMessage(Message message) {
        if (activity.copyTextToClipboard(message.getMergedBody().toString(), R.string.message)) {
            Toast.makeText(getActivity(), R.string.message_copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteFile(Message message) {
        if (activity.xmppConnectionService.getFileBackend().deleteFile(message)) {
            message.setTransferable(new TransferablePlaceholder(Transferable.STATUS_DELETED));
            activity.onConversationsListItemUpdated();
            refresh();
        }
    }

    public void resendMessage(final Message message) {
        if (message.isFileOrImage()) {
            DownloadableFile file = activity.xmppConnectionService.getFileBackend().getFile(message);
            if (file.exists()) {
                final Conversation conversation = message.getConversation();
                final XmppConnection xmppConnection = conversation.getAccount().getXmppConnection();
                if (!message.hasFileOnRemoteHost()
                        && xmppConnection != null
                        && !xmppConnection.getFeatures().httpUpload(message.getFileParams().size)) {
                    activity.selectPresence(conversation, () -> {
                        message.setCounterpart(conversation.getNextCounterpart());
                        activity.xmppConnectionService.resendFailedMessages(message);
                    });
                    return;
                }
            } else {
                Toast.makeText(activity, R.string.file_deleted, Toast.LENGTH_SHORT).show();
                message.setTransferable(new TransferablePlaceholder(Transferable.STATUS_DELETED));
                activity.onConversationsListItemUpdated();
                refresh();
                return;
            }
        }
        activity.xmppConnectionService.resendFailedMessages(message);
    }

    private void copyUrl(Message message) {
        final String url;
        final int resId;
        if (message.isGeoUri()) {
            resId = R.string.location;
            url = message.getBody();
        } else if (message.isXmppUri()) {
            resId = R.string.contact;
            url = message.getBody();
        } else if (message.hasFileOnRemoteHost()) {
            resId = R.string.file_url;
            url = message.getFileParams().url.toString();
        } else {
            url = message.getBody().trim();
            resId = R.string.file_url;
        }
        if (activity.copyTextToClipboard(url, resId)) {
            Toast.makeText(getActivity(), R.string.url_copied_to_clipboard,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadFile(Message message) {
        activity.xmppConnectionService.getHttpConnectionManager().createNewDownloadConnection(message, true);
    }

    public static void downloadFile(Activity activity, Message message) {
        Fragment fragment = activity.getFragmentManager().findFragmentById(R.id.main_fragment);
        if (fragment != null && fragment instanceof ConversationFragment) {
            ((ConversationFragment) fragment).downloadFile(message);
            return;
        }
        fragment = activity.getFragmentManager().findFragmentById(R.id.secondary_fragment);
        if (fragment != null && fragment instanceof ConversationFragment) {
            ((ConversationFragment) fragment).downloadFile(message);
        }
    }

    private void cancelTransmission(Message message) {
        Transferable transferable = message.getTransferable();
        if (transferable != null) {
            transferable.cancel();
        } else if (message.getStatus() != Message.STATUS_RECEIVED) {
            activity.xmppConnectionService.markMessage(message, Message.STATUS_SEND_FAILED);
        }
    }

    private void retryDecryption(Message message) {
        message.setEncryption(Message.ENCRYPTION_PGP);
        activity.onConversationsListItemUpdated();
        refresh();
        conversation.getAccount().getPgpDecryptionService().decrypt(message, false);
    }

    private void privateMessageWith(final Jid counterpart) {
        if (conversation.setOutgoingChatState(Config.DEFAULT_CHATSTATE)) {
            activity.xmppConnectionService.sendChatState(conversation);
        }
        this.binding.textinput.setText("");
        this.conversation.setNextCounterpart(counterpart);
        updateChatMsgHint();
        updateSendButton();
        updateEditablity();
    }

    private void correctMessage(Message message) {
        while (message.mergeable(message.next())) {
            message = message.next();
        }
        this.conversation.setCorrectingMessage(message);
        final Editable editable = binding.textinput.getText();
        this.conversation.setDraftMessage(editable.toString());
        this.binding.textinput.setText("");
        this.binding.textinput.append(message.getBody());

    }

    private void highlightInConference(String nick) {
        final Editable editable = this.binding.textinput.getText();
        String oldString = editable.toString().trim();
        final int pos = this.binding.textinput.getSelectionStart();
        if (oldString.isEmpty() || pos == 0) {
            editable.insert(0, nick + ": ");
        } else {
            final char before = editable.charAt(pos - 1);
            final char after = editable.length() > pos ? editable.charAt(pos) : '\0';
            if (before == '\n') {
                editable.insert(pos, nick + ": ");
            } else {
                if (pos > 2 && editable.subSequence(pos - 2, pos).toString().equals(": ")) {
                    if (NickValidityChecker.check(conversation, Arrays.asList(editable.subSequence(0, pos - 2).toString().split(", ")))) {
                        editable.insert(pos - 2, ", " + nick);
                        return;
                    }
                }
                editable.insert(pos, (Character.isWhitespace(before) ? "" : " ") + nick + (Character.isWhitespace(after) ? "" : " "));
                if (Character.isWhitespace(after)) {
                    this.binding.textinput.setSelection(this.binding.textinput.getSelectionStart() + 1);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (conversation != null) {
            outState.putString(STATE_CONVERSATION_UUID, conversation.getUuid());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null) {
            return;
        }
        String uuid = savedInstanceState.getString(STATE_CONVERSATION_UUID);
        if (uuid != null) {
            this.pendingConversationsUuid.push(uuid);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        reInit(conversation);
        final Bundle extras = pendingExtras.pop();
        if (extras != null) {
            processExtras(extras);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        if (activity == null || !activity.isChangingConfigurations()) {
            messageListAdapter.stopAudioPlayer();
        }
        if (this.conversation != null) {
            final String msg = this.binding.textinput.getText().toString();
            if (this.conversation.setNextMessage(msg)) {
                this.activity.xmppConnectionService.updateConversation(this.conversation);
            }
            updateChatState(this.conversation, msg);
            this.activity.xmppConnectionService.getNotificationService().setOpenConversation(null);
        }
    }

    private void updateChatState(final Conversation conversation, final String msg) {
        ChatState state = msg.length() == 0 ? Config.DEFAULT_CHATSTATE : ChatState.PAUSED;
        Account.State status = conversation.getAccount().getStatus();
        if (status == Account.State.ONLINE && conversation.setOutgoingChatState(state)) {
            activity.xmppConnectionService.sendChatState(conversation);
        }
    }

    public void reInit(Conversation conversation, Bundle extras) {
        this.reInit(conversation);
        if (extras != null) {
            if (activity != null) {
                processExtras(extras);
            } else {
                pendingExtras.push(extras);
            }
        }
    }

    private void reInit(Conversation conversation) {
        reInit(conversation, false);
    }

    private void reInit(Conversation conversation, boolean restore) {
        if (conversation == null) {
            return;
        }
        if (this.activity == null) {
            this.conversation = conversation;
            return;
        }
        Log.d(Config.LOGTAG, "reInit(restore=" + Boolean.toString(restore) + ")");
        setupIme();
        if (this.conversation != null) {
            final String msg = this.binding.textinput.getText().toString();
            if (this.conversation.setNextMessage(msg)) {
                activity.xmppConnectionService.updateConversation(conversation);
            }
            if (this.conversation != conversation && !restore) {
                updateChatState(this.conversation, msg);
                messageListAdapter.stopAudioPlayer();
            }
            if (!restore) {
                this.conversation.trim();
            }
        }

        if (activity != null) {
            this.binding.textSendButton.setContentDescription(activity.getString(R.string.send_message_to_x, conversation.getName()));
        }

        this.conversation = conversation;
        this.binding.textinput.setKeyboardListener(null);
        this.binding.textinput.setText("");
        this.binding.textinput.append(this.conversation.getNextMessage());
        this.binding.textinput.setKeyboardListener(this);
        messageListAdapter.updatePreferences();
        this.binding.messagesView.setAdapter(messageListAdapter);
        refresh(false);
        refresh();
        this.conversation.messagesLoaded.set(true);
        final boolean isAtBottom;
        this.binding.messagesView.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeRight() {
                Log.d(Config.LOGTAG, "Swipe right detected");
                activity.onBackPressed();
            }
        });
        synchronized (this.messageList) {
            final Message first = conversation.getFirstUnreadMessage();
            final int bottom = Math.max(0, this.messageList.size() - 1);
            final int pos;
            if (first == null) {
                pos = bottom;
            } else {
                int i = getIndexOf(first.getUuid(), this.messageList);
                pos = i < 0 ? bottom : i;
            }
            this.binding.messagesView.setSelection(pos);
            isAtBottom = pos == bottom;
        }
        if (activity != null) {
            activity.onConversationRead(this.conversation);
            //TODO if we only do this when this fragment is running on main it won't *bing* in tablet layout which might be unnecessary since we can *see* it
            activity.xmppConnectionService.getNotificationService().setOpenConversation(this.conversation);
        }
    }

    private void processExtras(Bundle extras) {
        final String downloadUuid = extras.getString(ConversationActivity.EXTRA_DOWNLOAD_UUID);
        final String text = extras.getString(ConversationActivity.EXTRA_TEXT);
        final String nick = extras.getString(ConversationActivity.EXTRA_NICK);
        final boolean pm = extras.getBoolean(ConversationActivity.EXTRA_IS_PRIVATE_MESSAGE, false);
        if (nick != null) {
            if (pm) {
                Jid jid = conversation.getJid();
                try {
                    Jid next = Jid.fromParts(jid.getLocalpart(), jid.getDomainpart(), nick);
                    privateMessageWith(next);
                } catch (final InvalidJidException ignored) {
                    //do nothing
                }
            } else {
                highlightInConference(nick);
            }
        } else {
            appendText(text);
        }
        final Message message = downloadUuid == null ? null : conversation.findMessageWithFileAndUuid(downloadUuid);
        if (message != null) {
            startDownloadable(message);
        }
    }

    private boolean showBlockSubmenu(View view) {
        final Jid jid = conversation.getJid();
        if (jid.isDomainJid()) {
            BlockContactDialog.show(activity, conversation);
        } else {
            PopupMenu popupMenu = new PopupMenu(getActivity(), view);
            popupMenu.inflate(R.menu.block);
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                Blockable blockable;
                switch (menuItem.getItemId()) {
                    case R.id.block_domain:
                        blockable = conversation.getAccount().getRoster().getContact(jid.toDomainJid());
                        break;
                    default:
                        blockable = conversation;
                }
                BlockContactDialog.show(activity, blockable);
                return true;
            });
            popupMenu.show();
        }
        return true;
    }

    private void updateSnackBar(final Conversation conversation) {
        final Account account = conversation.getAccount();
        final XmppConnection connection = account.getXmppConnection();
        final Contact contact = conversation.getContact();
        final int mode = conversation.getMode();
        if (account.getStatus() == Account.State.DISABLED) {
            showSnackbar(R.string.this_account_is_disabled, R.string.enable, this.mEnableAccountListener);
        } else if (conversation.isBlocked()) {
            showSnackbar(R.string.contact_blocked, R.string.unblock, this.mUnblockClickListener);
        } else if (contact != null && !contact.showInRoster() && contact.getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST)) {
            showSnackbar(R.string.contact_added_you, R.string.add_back, this.mAddBackClickListener, this.mLongPressBlockListener);
        } else if (contact != null && contact.getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST)) {
            showSnackbar(R.string.contact_asks_for_presence_subscription, R.string.allow, this.mAllowPresenceSubscription, this.mLongPressBlockListener);
        } else if (mode == Conversation.MODE_MULTI
                && !conversation.getMucOptions().online()
                && account.getStatus() == Account.State.ONLINE) {
            switch (conversation.getMucOptions().getError()) {
                case NICK_IN_USE:
                    showSnackbar(R.string.nick_in_use, R.string.edit, clickToMuc);
                    break;
                case NO_RESPONSE:
                    showSnackbar(R.string.joining_conference, 0, null);
                    break;
                case SERVER_NOT_FOUND:
                    if (conversation.receivedMessagesCount() > 0) {
                        showSnackbar(R.string.remote_server_not_found, R.string.try_again, joinMuc);
                    } else {
                        showSnackbar(R.string.remote_server_not_found, R.string.leave, leaveMuc);
                    }
                    break;
                case PASSWORD_REQUIRED:
                    showSnackbar(R.string.conference_requires_password, R.string.enter_password, enterPassword);
                    break;
                case BANNED:
                    showSnackbar(R.string.conference_banned, R.string.leave, leaveMuc);
                    break;
                case MEMBERS_ONLY:
                    showSnackbar(R.string.conference_members_only, R.string.leave, leaveMuc);
                    break;
                case KICKED:
                    showSnackbar(R.string.conference_kicked, R.string.join, joinMuc);
                    break;
                case UNKNOWN:
                    showSnackbar(R.string.conference_unknown_error, R.string.leave, leaveMuc);
                    break;
                case INVALID_NICK:
                    showSnackbar(R.string.invalid_muc_nick, R.string.edit, clickToMuc);
                case SHUTDOWN:
                    showSnackbar(R.string.conference_shutdown, R.string.try_again, joinMuc);
                    break;
                default:
                    hideSnackbar();
                    break;
            }
        } else if ((mode == Conversation.MODE_MULTI
                && !conversation.getMucOptions().participating())) {
            showSnackbar(R.string.no_write_access_in_public_muc, R.string.ok, clickToMuc);
        } else if (account.hasPendingPgpIntent(conversation)) {
            showSnackbar(R.string.openpgp_messages_found, R.string.decrypt, clickToDecryptListener);
        } else if (mode == Conversation.MODE_SINGLE
                && conversation.smpRequested()) {
            showSnackbar(R.string.smp_requested, R.string.verify, this.mAnswerSmpClickListener);
        } else if (mode == Conversation.MODE_SINGLE
                && conversation.hasValidOtrSession()
                && (conversation.getOtrSession().getSessionStatus() == SessionStatus.ENCRYPTED)
                && (!conversation.isOtrFingerprintVerified())) {
            showSnackbar(R.string.unknown_otr_fingerprint, R.string.verify, clickToVerify);
        } else if (connection != null
                && connection.getFeatures().blocking()
                && conversation.countMessages() != 0
                && !conversation.isBlocked()
                && conversation.isWithStranger()) {
            showSnackbar(R.string.received_message_from_stranger, R.string.block, mBlockClickListener);
        } else if (activity.xmppConnectionService.warnUnecryptedChat()) {
            AxolotlService axolotlService = account.getAxolotlService();
            if ((mode == Conversation.MODE_SINGLE) && (conversation.getNextEncryption() == Message.ENCRYPTION_NONE &&
                    ((Config.supportOmemo() && axolotlService != null && conversation.getAccount().getAxolotlService().isConversationAxolotlCapable(conversation)) ||
                            (Config.supportOpenPgp() && account.isPgpDecryptionServiceConnected()) ||
                            Config.supportOtr()))) {
                if (ENCRYPTION_EXCEPTIONS.contains(conversation.getJid().toString()) || conversation.getJid().toString().equals(account.getJid().getDomainpart())) {
                    hideSnackbar();
                } else {
                    showSnackbar(R.string.conversation_unencrypted_hint, R.string.ok, mHideUnencryptionHint, null);
                }
            } else if ((mode == Conversation.MODE_MULTI && conversation.getMucOptions().membersOnly() && conversation.getMucOptions().nonanonymous()) &&
                    (conversation.getNextEncryption() == Message.ENCRYPTION_NONE &&
                            ((Config.supportOmemo() && axolotlService != null && conversation.getAccount().getAxolotlService().isConversationAxolotlCapable(conversation)) ||
                                    (Config.supportOpenPgp() && account.isPgpDecryptionServiceConnected())))) {
                if (ENCRYPTION_EXCEPTIONS.contains(conversation.getJid().toString()) || conversation.getJid().toString().equals(account.getJid().getDomainpart())) {
                    Log.d(Config.LOGTAG, "Don't show unencrypted warning because " + conversation.getJid().toString() + " is on exception list");
                    hideSnackbar();
                } else {
                    showSnackbar(R.string.conversation_unencrypted_hint, R.string.ok, mHideUnencryptionHint, null);
                }
            } else {
                hideSnackbar();
            }
        } else {
            hideSnackbar();
        }
    }

    @Override
    public void refresh() {
        this.refresh(true);
    }


    private void refresh(boolean notifyConversationRead) {
        synchronized (this.messageList) {
            if (this.conversation != null) {
                conversation.populateWithMessages(ConversationFragment.this.messageList);
                updateSnackBar(conversation);
                updateStatusMessages();
                this.messageListAdapter.notifyDataSetChanged();
                updateChatMsgHint();
                if (notifyConversationRead && activity != null) {
                    activity.onConversationRead(this.conversation);
                }
                updateSendButton();
                updateEditablity();
            }
        }
    }

    protected void messageSent() {
        mSendingPgpMessage.set(false);
        this.binding.textinput.setText("");
        if (conversation.setCorrectingMessage(null)) {
            this.binding.textinput.append(conversation.getDraftMessage());
            conversation.setDraftMessage(null);
        }
        if (conversation.setNextMessage(this.binding.textinput.getText().toString())) {
            activity.xmppConnectionService.updateConversation(conversation);
        }
        updateChatMsgHint();
        new Handler().post(() -> {
            int size = messageList.size();
            this.binding.messagesView.setSelection(size - 1);
        });
    }

    public void setFocusOnInputField() {
        this.binding.textinput.requestFocus();
    }

    public void doneSendingPgpMessage() {
        mSendingPgpMessage.set(false);
    }

    public long getMaxHttpUploadSize(Conversation conversation) {
        final XmppConnection connection = conversation.getAccount().getXmppConnection();
        return connection == null ? -1 : connection.getFeatures().getMaxHttpUploadSize();
    }

    private void updateEditablity() {
        boolean canWrite = this.conversation.getMode() == Conversation.MODE_SINGLE || this.conversation.getMucOptions().participating() || this.conversation.getNextCounterpart() != null;
        this.binding.textinput.setFocusable(canWrite);
        this.binding.textinput.setFocusableInTouchMode(canWrite);
        this.binding.textSendButton.setEnabled(canWrite);
        this.binding.textinput.setCursorVisible(canWrite);
    }

    public void updateSendButton() {
        boolean useSendButtonToIndicateStatus = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("send_button_status", getResources().getBoolean(R.bool.send_button_status));
        final Conversation c = this.conversation;
        final Presence.Status status;
        final String text = this.binding.textinput == null ? "" : this.binding.textinput.getText().toString();
        final SendButtonAction action = SendButtonTool.getAction(getActivity(), c, text);
        if (useSendButtonToIndicateStatus && c.getAccount().getStatus() == Account.State.ONLINE) {
            if (activity.xmppConnectionService != null && activity.xmppConnectionService.getMessageArchiveService().isCatchingUp(c)) {
                status = Presence.Status.OFFLINE;
            } else if (c.getMode() == Conversation.MODE_SINGLE) {
                status = c.getContact().getShownStatus();
            } else {
                status = c.getMucOptions().online() ? Presence.Status.ONLINE : Presence.Status.OFFLINE;
            }
        } else {
            status = Presence.Status.OFFLINE;
        }
        this.binding.textSendButton.setTag(action);
        this.binding.textSendButton.setImageResource(SendButtonTool.getSendButtonImageResource(getActivity(), action, status));
    }

    protected void updateStatusMessages() {
        updateDateBubbles();
        synchronized (this.messageList) {
            if (showLoadMoreMessages(conversation)) {
                this.messageList.add(0, Message.createLoadMoreMessage(conversation));
            }
            if (conversation.getMode() == Conversation.MODE_MULTI) {
                final MucOptions mucOptions = conversation.getMucOptions();
                final List<MucOptions.User> allUsers = mucOptions.getUsers();
                final Set<ReadByMarker> addedMarkers = new HashSet<>();
                ChatState state = ChatState.COMPOSING;
                List<MucOptions.User> users = conversation.getMucOptions().getUsersWithChatState(state, 5);
                if (users.size() == 0) {
                    state = ChatState.PAUSED;
                    users = conversation.getMucOptions().getUsersWithChatState(state, 5);
                }
                if (mucOptions.isPrivateAndNonAnonymous()) {
                    for (int i = this.messageList.size() - 1; i >= 0; --i) {
                        final Set<ReadByMarker> markersForMessage = messageList.get(i).getReadByMarkers();
                        final List<MucOptions.User> shownMarkers = new ArrayList<>();
                        for (ReadByMarker marker : markersForMessage) {
                            if (!ReadByMarker.contains(marker, addedMarkers)) {
                                addedMarkers.add(marker); //may be put outside this condition. set should do dedup anyway
                                MucOptions.User user = mucOptions.findUser(marker);
                                shownMarkers.add(user);
                            }
                        }
                        final ReadByMarker markerForSender = ReadByMarker.from(messageList.get(i));
                        final Message statusMessage;
                        final int size = shownMarkers.size();
                        if (size > 1) {
                            final String body;
                            if (size <= 4) {
                                body = getString(R.string.contacts_have_read_up_to_this_point, UIHelper.concatNames(shownMarkers));
                            } else {
                                body = getString(R.string.contacts_and_n_more_have_read_up_to_this_point, UIHelper.concatNames(shownMarkers, 3), size - 3);
                            }
                            statusMessage = Message.createStatusMessage(conversation, body);
                            statusMessage.setCounterparts(shownMarkers);
                        } else if (size == 1) {
                            statusMessage = Message.createStatusMessage(conversation, getString(R.string.contact_has_read_up_to_this_point, UIHelper.getDisplayName(shownMarkers.get(0))));
                            statusMessage.setCounterpart(shownMarkers.get(0).getFullJid());
                            statusMessage.setTrueCounterpart(shownMarkers.get(0).getRealJid());
                        } else {
                            statusMessage = null;
                        }
                        if (statusMessage != null) {
                            this.messageList.add(i + 1, statusMessage);
                        }
                        addedMarkers.add(markerForSender);
                        if (ReadByMarker.allUsersRepresented(allUsers, addedMarkers)) {
                            break;
                        }
                    }
                }
            }
        }
    }

    protected void updateDateBubbles() {
        synchronized (this.messageList) {
            for (int i = 0; i < this.messageList.size(); ++i) {
                final Message current = this.messageList.get(i);
                if (i == 0 || !UIHelper.sameDay(this.messageList.get(i - 1).getTimeSent(), current.getTimeSent())) {
                    this.messageList.add(i, Message.createDateSeparator(current));
                    i++;
                }
            }
        }
    }

    public void stopScrolling() {
        long now = SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        binding.messagesView.dispatchTouchEvent(cancel);
    }

    private boolean showLoadMoreMessages(final Conversation c) {
        final boolean mam = hasMamSupport(c) && !c.getContact().isBlocked();
        final MessageArchiveService service = activity.xmppConnectionService.getMessageArchiveService();
        return mam && (c.getLastClearHistory().getTimestamp() != 0 || (c.countMessages() == 0 && c.messagesLoaded.get() && c.hasMessagesLeftOnServer() && !service.queryInProgress(c)));
    }

    private boolean hasMamSupport(final Conversation c) {
        if (c.getMode() == Conversation.MODE_SINGLE) {
            final XmppConnection connection = c.getAccount().getXmppConnection();
            return connection != null && connection.getFeatures().mam();
        } else {
            return c.getMucOptions().mamSupport();
        }
    }

    protected void showSnackbar(final int message, final int action, final OnClickListener clickListener) {
        showSnackbar(message, action, clickListener, null);
    }

    protected void showSnackbar(final int message, final int action, final OnClickListener clickListener, final View.OnLongClickListener longClickListener) {
        this.binding.snackbar.setVisibility(View.VISIBLE);
        this.binding.snackbar.setOnClickListener(null);
        this.binding.snackbarMessage.setText(message);
        this.binding.snackbarMessage.setOnClickListener(null);
        this.binding.snackbarAction.setVisibility(clickListener == null ? View.GONE : View.VISIBLE);
        if (action != 0) {
            this.binding.snackbarAction.setText(action);
        }
        this.binding.snackbarAction.setOnClickListener(clickListener);
        this.binding.snackbarAction.setOnLongClickListener(longClickListener);
    }

    protected void hideSnackbar() {
        this.binding.snackbar.setVisibility(View.GONE);
    }

    protected void showMessageHint(final String message) {
        this.binding.messagehint.setVisibility(View.VISIBLE);
        this.binding.messagehintMessage.setText(message);
    }

    protected void hideMessageHint() {
        this.binding.messagehint.setVisibility(View.GONE);
    }

    protected void showSearchField() {
        this.binding.textsend.setVisibility(View.GONE);
        this.binding.searchfield.setVisibility(View.VISIBLE);
        this.binding.searchfieldInput.addTextChangedListener(mSearchTextWatcher);
        this.binding.searchfieldInput.requestFocus();
        final InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(this.binding.searchfieldInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    protected void hideSearchField() {
        this.binding.textsend.setVisibility(View.VISIBLE);
        this.binding.searchfield.setVisibility(View.GONE);
        if (activity != null) {
            final InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(this.binding.searchfieldInput.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        }
        this.binding.searchfieldInput.setText("");
    }

    protected boolean isSearchFieldVisible() {
        return this.binding.searchfield.getVisibility() == View.VISIBLE;
    }

    protected void sendPlainTextMessage(Message message) {
        activity.xmppConnectionService.sendMessage(message);
        messageSent();
    }

    protected void sendPgpMessage(final Message message) {
        final XmppConnectionService xmppService = activity.xmppConnectionService;
        final Contact contact = message.getConversation().getContact();
        if (!activity.hasPgp()) {
            activity.showInstallPgpDialog();
            return;
        }
        if (conversation.getAccount().getPgpSignature() == null) {
            activity.announcePgp(conversation.getAccount(), conversation, null, activity.onOpenPGPKeyPublished);
            return;
        }
        if (!mSendingPgpMessage.compareAndSet(false, true)) {
            Log.d(Config.LOGTAG, "sending pgp message already in progress");
        }
        if (conversation.getMode() == Conversation.MODE_SINGLE) {
            if (contact.getPgpKeyId() != 0) {
                xmppService.getPgpEngine().hasKey(contact,
                        new UiCallback<Contact>() {

                            @Override
                            public void userInputRequried(PendingIntent pi, Contact contact) {
                                startPendingIntent(pi, REQUEST_ENCRYPT_MESSAGE);
                            }

                            @Override
                            public void success(Contact contact) {
                                encryptTextMessage(message);
                            }

                            @Override
                            public void error(int error, Contact contact) {
                                activity.runOnUiThread(() -> Toast.makeText(activity,
                                        R.string.unable_to_connect_to_keychain,
                                        Toast.LENGTH_SHORT
                                ).show());
                                mSendingPgpMessage.set(false);
                            }
                        });

            } else {
                showNoPGPKeyDialog(false,
                        (dialog, which) -> {
                            conversation.setNextEncryption(Message.ENCRYPTION_NONE);
                            xmppService.updateConversation(conversation);
                            message.setEncryption(Message.ENCRYPTION_NONE);
                            xmppService.sendMessage(message);
                            messageSent();
                        });
            }
        } else {
            if (conversation.getMucOptions().pgpKeysInUse()) {
                if (!conversation.getMucOptions().everybodyHasKeys()) {
                    Toast warning = Toast
                            .makeText(getActivity(),
                                    R.string.missing_public_keys,
                                    Toast.LENGTH_LONG);
                    warning.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                    warning.show();
                }
                encryptTextMessage(message);
            } else {
                showNoPGPKeyDialog(true,
                        (dialog, which) -> {
                            conversation.setNextEncryption(Message.ENCRYPTION_NONE);
                            message.setEncryption(Message.ENCRYPTION_NONE);
                            xmppService.updateConversation(conversation);
                            xmppService.sendMessage(message);
                            messageSent();
                        });
            }
        }
    }

    public void encryptTextMessage(Message message) {
        activity.xmppConnectionService.getPgpEngine().encrypt(message,
                new UiCallback<Message>() {

                    @Override
                    public void userInputRequried(PendingIntent pi, Message message) {
                        startPendingIntent(pi, REQUEST_SEND_MESSAGE);
                    }

                    @Override
                    public void success(Message message) {
                        message.setEncryption(Message.ENCRYPTION_DECRYPTED);
                        activity.xmppConnectionService.sendMessage(message);
                        getActivity().runOnUiThread(() -> messageSent());
                    }

                    @Override
                    public void error(final int error, Message message) {
                        getActivity().runOnUiThread(() -> {
                            doneSendingPgpMessage();
                            Toast.makeText(getActivity(), R.string.unable_to_connect_to_keychain, Toast.LENGTH_SHORT).show();
                        });

                    }
                });
    }

    public void showNoPGPKeyDialog(boolean plural, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        if (plural) {
            builder.setTitle(getString(R.string.no_pgp_keys));
            builder.setMessage(getText(R.string.contacts_have_no_pgp_keys));
        } else {
            builder.setTitle(getString(R.string.no_pgp_key));
            builder.setMessage(getText(R.string.contact_has_no_pgp_key));
        }
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.setPositiveButton(getString(R.string.send_unencrypted), listener);
        builder.create().show();
    }

    protected void sendAxolotlMessage(final Message message) {
        activity.xmppConnectionService.sendMessage(message);
        messageSent();
    }

    protected void sendOtrMessage(final Message message) {
        final ConversationActivity activity = (ConversationActivity) getActivity();
        final XmppConnectionService xmppService = activity.xmppConnectionService;
        activity.selectPresence(message.getConversation(),
                () -> {
                    message.setCounterpart(conversation.getNextCounterpart());
                    xmppService.sendMessage(message);
                    messageSent();
                });
    }

    public void appendText(String text) {
        if (text == null) {
            return;
        }
        String previous = this.binding.textinput.getText().toString();
        if (previous.length() != 0 && !previous.endsWith(" ")) {
            text = " " + text;
        }
        this.binding.textinput.append(text);
    }

    @Override
    public boolean onEnterPressed() {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final boolean enterIsSend = p.getBoolean("enter_is_send", getResources().getBoolean(R.bool.enter_is_send));
        if (enterIsSend) {
            sendMessage();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onTypingStarted() {
        final boolean broadcastLastActivity = activity.xmppConnectionService.broadcastLastActivity();
        Account.State status = conversation.getAccount().getStatus();
        if (status == Account.State.ONLINE && conversation.setOutgoingChatState(ChatState.COMPOSING)) {
            activity.xmppConnectionService.sendChatState(conversation);
        }
        if (broadcastLastActivity) {
            activity.xmppConnectionService.sendPresence(conversation.getAccount(), false); //send new presence but don't include idle because we are not
        }
        updateSendButton();
    }

    @Override
    public void onTypingStopped() {
        Account.State status = conversation.getAccount().getStatus();
        if (status == Account.State.ONLINE && conversation.setOutgoingChatState(ChatState.PAUSED)) {
            activity.xmppConnectionService.sendChatState(conversation);
        }
    }

    @Override
    public void onTextDeleted() {
        Account.State status = conversation.getAccount().getStatus();
        if (status == Account.State.ONLINE && conversation.setOutgoingChatState(Config.DEFAULT_CHATSTATE)) {
            activity.xmppConnectionService.sendChatState(conversation);
        }
        updateSendButton();
    }

    @Override
    public void onTextChanged() {
        if (conversation != null && conversation.getCorrectingMessage() != null) {
            updateSendButton();
        }
    }

    @Override
    public boolean onTabPressed(boolean repeated) {
        if (conversation == null || conversation.getMode() == Conversation.MODE_SINGLE) {
            return false;
        }
        if (repeated) {
            completionIndex++;
        } else {
            lastCompletionLength = 0;
            completionIndex = 0;
            final String content = this.binding.textinput.getText().toString();
            lastCompletionCursor = this.binding.textinput.getSelectionEnd();
            int start = lastCompletionCursor > 0 ? content.lastIndexOf(" ", lastCompletionCursor - 1) + 1 : 0;
            firstWord = start == 0;
            incomplete = content.substring(start, lastCompletionCursor);
        }
        List<String> completions = new ArrayList<>();
        for (MucOptions.User user : conversation.getMucOptions().getUsers()) {
            String name = user.getName();
            if (name != null && name.startsWith(incomplete)) {
                completions.add(name + (firstWord ? ": " : " "));
            }
        }
        Collections.sort(completions);
        if (completions.size() > completionIndex) {
            String completion = completions.get(completionIndex).substring(incomplete.length());
            this.binding.textinput.getEditableText().delete(lastCompletionCursor, lastCompletionCursor + lastCompletionLength);
            this.binding.textinput.getEditableText().insert(lastCompletionCursor, completion);
            lastCompletionLength = completion.length();
        } else {
            completionIndex = -1;
            this.binding.textinput.getEditableText().delete(lastCompletionCursor, lastCompletionCursor + lastCompletionLength);
            lastCompletionLength = 0;
        }
        return true;
    }

    public Message searchHistory(String query) {
        return searchHistory(query, null);
    }

    public Message searchHistory(String query, Boolean ascendingSearch) {
        return searchHistory(query, lastHistoryMessage, ascendingSearch);
    }

    /**
     * Search through history from message basis either ascending or descending
     *
     * @param query           search term
     * @param basis           message to start from. If null, start from last recent message
     * @param ascendingSearch do we want to ascend or descend in our search?
     *                        If this is null, ascend to first match and return.
     * @return match or null
     */
    public Message searchHistory(String query, Message basis, Boolean ascendingSearch) {
        int entryIndex;
        Message message;
        lastHistoryMessage = basis;
        if (messageList.size() == 0) {
            return null;
        }
        if (basis == null) {
            entryIndex = messageList.size() - 1;
        } else {
            int in = getIndexOf(basis.getUuid(), messageList);
            entryIndex = (in != -1 ? in : messageList.size() - 1);
        }

        int firstMatchIndex = entryIndex;
        boolean entryIndexWasMatch = true;
        do {
            message = messageList.get(firstMatchIndex);
            if (message.getType() == Message.TYPE_TEXT && messageContainsQuery(message, query)) {
                lastHistoryMessage = message;
                break;
            }
            entryIndexWasMatch = false;
            firstMatchIndex = (messageList.size() + firstMatchIndex - 1) % messageList.size();
        } while (entryIndex != firstMatchIndex);

        if (!entryIndexWasMatch && entryIndex == firstMatchIndex) {
            //No matches
            return null;
        }

        if (ascendingSearch != null) {
            int direction = ascendingSearch ? -1 : 1;
            int nextMatchIndex = firstMatchIndex;
            do {
                nextMatchIndex = (messageList.size() + nextMatchIndex + direction) % messageList.size();
                message = messageList.get(nextMatchIndex);
                if (message.getType() == Message.TYPE_TEXT && messageContainsQuery(message, query)) {
                    lastHistoryMessage = message;
                    break;
                }
            } while (nextMatchIndex != entryIndex);
        }

        if (lastHistoryMessage != null) {
            int pos = getIndexOf(lastHistoryMessage.getUuid(), messageList);
            setScrollPosition(new Pair<>(pos, pos));
            this.binding.messagesView.setSelection(pos);
        }
        return lastHistoryMessage;
    }

    private boolean messageContainsQuery(Message m, String q) {
        return m != null && m.getMergedBody().toString().toLowerCase().contains(q.toLowerCase());
    }

    private void startPendingIntent(PendingIntent pendingIntent, int requestCode) {
        try {
            getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(), requestCode, null, 0, 0, 0);
        } catch (final SendIntentException ignored) {
        }
    }

    @Override
    public void onBackendConnected() {
        Log.d(Config.LOGTAG, "ConversationFragment.onBackendConnected()");
        String uuid = pendingConversationsUuid.pop();
        if (uuid != null) {
            Conversation conversation = activity.xmppConnectionService.findConversationByUuid(uuid);
            if (conversation == null) {
                Log.d(Config.LOGTAG, "unable to restore activity");
                clearPending();
                return;
            }
            reInit(conversation, true);
        }
        ActivityResult activityResult = postponedActivityResult.pop();
        if (activityResult != null) {
            handleActivityResult(activityResult);
        }
    }

    public void clearPending() {
        if (postponedActivityResult.pop() != null) {
            Log.d(Config.LOGTAG, "cleared pending intent with unhandled result left");
        }
    }

    public static Conversation getConversation(Activity activity) {
        Fragment fragment = activity.getFragmentManager().findFragmentById(R.id.secondary_fragment);
        if (fragment != null && fragment instanceof ConversationFragment) {
            return ((ConversationFragment) fragment).getConversation();
        } else {
            return null;
        }
    }

    public Conversation getConversation() {
        return conversation;
    }
}
