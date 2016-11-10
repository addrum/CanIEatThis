package com.adamshort.canieatthis.app.ui;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.devicebridge.R;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PopupAdapter implements InfoWindowAdapter {

    private View mPopup = null;
    private LayoutInflater mInflater = null;

    public PopupAdapter(LayoutInflater inflater) {
        mInflater = inflater;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return (null);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getInfoContents(Marker marker) {
        if (mPopup == null) {
            mPopup = mInflater.inflate(R.layout.popup, null);
        }

        TextView title_view = (TextView) mPopup.findViewById(R.id.title);
        title_view.setText(marker.getTitle());

        String[] snippet = marker.getSnippet().split(",");

        // now this may look a little weird as to why it manually sets the visibility to gone
        // but this is because without it, the text boxes don't get hidden again
        // this caused a weird bug in that info from another marker would be shown on a completely
        // different marker

        TextView rating_view = (TextView) mPopup.findViewById(R.id.rating);
        rating_view.setVisibility(View.GONE);

        TextView open_now_view = (TextView) mPopup.findViewById(R.id.open_now);
        open_now_view.setVisibility(View.GONE);

        TextView view1 = (TextView) mPopup.findViewById(R.id.view1);
        view1.setVisibility(View.GONE);

        TextView view2 = (TextView) mPopup.findViewById(R.id.view2);
        view2.setVisibility(View.GONE);

        TextView view3 = (TextView) mPopup.findViewById(R.id.view3);
        view3.setVisibility(View.GONE);

        TextView view4 = (TextView) mPopup.findViewById(R.id.view4);
        view4.setVisibility(View.GONE);

        TextView no_info = (TextView) mPopup.findViewById(R.id.no_info);
        no_info.setVisibility(View.GONE);

        List<TextView> textViews = new ArrayList<>(Arrays.asList(view1, view2, view3, view4));

        int textViewToSet = 0;

        for (String aSnippet : snippet) {
            if (!TextUtils.isEmpty(aSnippet)) {
                if (aSnippet.contains("Rating")) {
                    rating_view.setText(aSnippet);
                    rating_view.setVisibility(View.VISIBLE);
                } else if (aSnippet.contains("Open Now")) {
                    open_now_view.setText(aSnippet);
                    open_now_view.setVisibility(View.VISIBLE);
                } else if (aSnippet.equals("We currently don't have enough information about this place to display dietary suitability.")) {
                    no_info.setText(aSnippet);
                    no_info.setVisibility(View.VISIBLE);
                } else {
                    if (textViewToSet < textViews.size()) {
                        TextView textView = textViews.get(textViewToSet);
                        textView.setText(aSnippet);
                        textView.setVisibility(View.VISIBLE);
                        textViewToSet++;
                    }
                }
            }
        }

        return (mPopup);
    }
}
