package de.pixart.messenger.ui;

import android.app.Dialog;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import de.pixart.messenger.R;
import de.pixart.messenger.databinding.CreateConferenceDialogBinding;
import de.pixart.messenger.services.XmppConnectionService;


public class CreateConferenceDialog extends DialogFragment {

    private static final String ACCOUNTS_LIST_KEY = "activated_accounts_list";
    public XmppConnectionService xmppConnectionService;
    private CreateConferenceDialogListener mListener;

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
        //final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.create_conference_dialog, null);
        //final TextView yourAccount = dialogView.findViewById(R.id.your_account);
        //final Spinner spinner = dialogView.findViewById(R.id.account);
        //final EditText subject = dialogView.findViewById(R.id.subject);
        CreateConferenceDialogBinding binding = DataBindingUtil.inflate(getActivity().getLayoutInflater(), R.layout.create_conference_dialog, null, false);
        if (xmppConnectionService != null && xmppConnectionService.multipleAccounts()) {
            binding.yourAccount.setVisibility(View.VISIBLE);
            binding.account.setVisibility(View.VISIBLE);
        } else {
            binding.yourAccount.setVisibility(View.GONE);
            binding.account.setVisibility(View.GONE);
        }
        ArrayList<String> mActivatedAccounts = getArguments().getStringArrayList(ACCOUNTS_LIST_KEY);
        StartConversationActivity.populateAccountSpinner(getActivity(), mActivatedAccounts, binding.account);
        builder.setView(binding.getRoot());
        builder.setPositiveButton(R.string.choose_participants, (dialog, which) -> mListener.onCreateDialogPositiveClick(binding.account, binding.subject.getText().toString()));
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