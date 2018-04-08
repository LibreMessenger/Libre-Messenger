package de.pixart.messenger.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.pixart.messenger.R;
import de.pixart.messenger.services.XmppConnectionService;


public class CreateConferenceDialog extends DialogFragment {

    private static final String ACCOUNTS_LIST_KEY = "activated_accounts_list";
    private CreateConferenceDialogListener mListener;
    public XmppConnectionService xmppConnectionService;

    public static CreateConferenceDialog newInstance(List<String> accounts) {
        CreateConferenceDialog dialog = new CreateConferenceDialog();
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(ACCOUNTS_LIST_KEY, (ArrayList<String>) accounts);
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
        builder.setTitle(R.string.create_conference);
        final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.create_conference_dialog, null);
        final TextView yourAccount = dialogView.findViewById(R.id.your_account);
        final Spinner spinner = dialogView.findViewById(R.id.account);
        final EditText subject = dialogView.findViewById(R.id.subject);
        if (xmppConnectionService != null && xmppConnectionService.multipleAccounts()) {
            yourAccount.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.VISIBLE);
        } else {
            yourAccount.setVisibility(View.GONE);
            spinner.setVisibility(View.GONE);
        }
        ArrayList<String> mActivatedAccounts = getArguments().getStringArrayList(ACCOUNTS_LIST_KEY);
        StartConversationActivity.populateAccountSpinner(getActivity(), mActivatedAccounts, spinner);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.choose_participants, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onCreateDialogPositiveClick(spinner, subject.getText().toString());
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (CreateConferenceDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement CreateConferenceDialogListener");
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

    public interface CreateConferenceDialogListener {
        void onCreateDialogPositiveClick(Spinner spinner, String subject);
    }
}