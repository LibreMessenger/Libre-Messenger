package de.pixart.messenger.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import de.pixart.messenger.R;
import de.pixart.messenger.services.XmppConnectionService;
import de.pixart.messenger.ui.adapter.KnownHostsAdapter;
import de.pixart.messenger.ui.util.DelayedHintHelper;

public class JoinConferenceDialog extends DialogFragment {

    private static final String PREFILLED_JID_KEY = "prefilled_jid";
    private static final String ACCOUNTS_LIST_KEY = "activated_accounts_list";
    private static final String CONFERENCE_HOSTS_KEY = "known_conference_hosts";
    private JoinConferenceDialogListener mListener;
    public XmppConnectionService xmppConnectionService;

    public static JoinConferenceDialog newInstance(String prefilledJid, List<String> accounts, Collection<String> conferenceHosts) {
        JoinConferenceDialog dialog = new JoinConferenceDialog();
        Bundle bundle = new Bundle();
        bundle.putString(PREFILLED_JID_KEY, prefilledJid);
        bundle.putStringArrayList(ACCOUNTS_LIST_KEY, (ArrayList<String>) accounts);
        bundle.putSerializable(CONFERENCE_HOSTS_KEY, (HashSet) conferenceHosts);
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.join_conference);
        final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.join_conference_dialog, null);
        final TextView yourAccount = dialogView.findViewById(R.id.your_account);
        final Spinner spinner = dialogView.findViewById(R.id.account);
        final AutoCompleteTextView jid = dialogView.findViewById(R.id.jid);
        DelayedHintHelper.setHint(R.string.conference_address_example, jid);
        jid.setAdapter(new KnownHostsAdapter(getActivity(), R.layout.simple_list_item, (Collection<String>) getArguments().getSerializable(CONFERENCE_HOSTS_KEY)));
        String prefilledJid = getArguments().getString(PREFILLED_JID_KEY);
        if (prefilledJid != null) {
            jid.append(prefilledJid);
        }
        if (xmppConnectionService != null && xmppConnectionService.multipleAccounts()) {
            yourAccount.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.VISIBLE);
        } else {
            yourAccount.setVisibility(View.GONE);
            spinner.setVisibility(View.GONE);
        }
        final Checkable bookmarkCheckBox = (CheckBox) dialogView
                .findViewById(R.id.bookmark);
        StartConversationActivity.populateAccountSpinner(getActivity(), getArguments().getStringArrayList(ACCOUNTS_LIST_KEY), spinner);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.join, null);
        builder.setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onJoinDialogPositiveClick(dialog, spinner, jid, bookmarkCheckBox.isChecked());
            }
        });
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (JoinConferenceDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement JoinConferenceDialogListener");
        }
    }

    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }

    public interface JoinConferenceDialogListener {
        void onJoinDialogPositiveClick(Dialog dialog, Spinner spinner, AutoCompleteTextView jid, boolean isBookmarkChecked);
    }
}