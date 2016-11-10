package com.adamshort.canieatthis.app.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.CheckBox;

import com.adamshort.canieatthis.R;
import com.adamshort.canieatthis.app.data.Installation;
import com.adamshort.canieatthis.app.util.SubmitPlacesInfoFirebaseAsync;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;

public class AddPlacesInfoDialogFragment extends DialogFragment {

    private boolean mSubmitted;

    private LatLng mLatLng;
    private OnCompleteListener mListener;

    public interface OnCompleteListener {
        void onComplete(boolean successful);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

//        Intent intent = activity.getIntent()Z;
        Bundle args = getArguments();
        String name = args.getString("name");
        //noinspection ConstantConditions
        String[] latLngStr = args.getString("latlng").replace("lat/lng: ", "").replace("(", "")
                .replace(")", "").split(",");
        mLatLng = new LatLng(Double.parseDouble(latLngStr[0]), Double.parseDouble(latLngStr[1]));

        builder.setView(inflater.inflate(R.layout.dialog_fragment_add_places_info, null))
                .setTitle(name)
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            Dialog dialog = getDialog();
                            final CheckBox lactose_free_checkbox = (CheckBox) dialog.findViewById(R.id.lactoseFreeCheckBox);
                            final CheckBox vegetarian_checkbox = (CheckBox) dialog.findViewById(R.id.vegetarianCheckBox);
                            final CheckBox vegan_checkbox = (CheckBox) dialog.findViewById(R.id.veganCheckBox);
                            final CheckBox gluten_free_checkbox = (CheckBox) dialog.findViewById(R.id.glutenFreeCheckBox);
                            //noinspection ConstantConditions
                            if (lactose_free_checkbox != null && vegetarian_checkbox != null &&
                                    vegan_checkbox != null && gluten_free_checkbox != null) {
                                boolean[] values = new boolean[]{
                                        lactose_free_checkbox.isChecked(),
                                        vegetarian_checkbox.isChecked() || vegan_checkbox.isChecked(),
                                        vegan_checkbox.isChecked(),
                                        gluten_free_checkbox.isChecked()};

                                SubmitPlacesInfoFirebaseAsync fb = new SubmitPlacesInfoFirebaseAsync(getContext(), mLatLng);
                                fb.execute(values);

                                // write latlng to install file so we know which places an installation
                                // has submitted info for
                                File file = new File(getContext().getFilesDir(), Installation.getInstallation());
                                Installation.writeInstallationFile(file, "\n" + mLatLng.toString(), true);

                                mListener.onComplete(true);
                                mSubmitted = true;
                            }
                        } catch (Exception e) {
                            Log.e("onClick", e.toString());
                        }
                    }
                });
        return builder.create();
    }

    @Override
    public void onDismiss(final DialogInterface arg0) {
        // call super otherwise dialog fragment will reappear after returning to activity
        super.onDismiss(arg0);
        if (!mSubmitted) {
            mListener.onComplete(false);
        }
    }

    // make sure the Activity implemented it
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity activity;

        if (context instanceof Activity) {
            activity = (Activity) context;

            try {
                this.mListener = (OnCompleteListener) activity;
            } catch (final ClassCastException e) {
                throw new ClassCastException(activity.toString() + " must implement OnCompleteListener");
            }
        }
    }
}
