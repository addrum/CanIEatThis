package com.adamshort.canieatthis.app.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.devicebridge.R;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

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
        int length = snippet.length;

        TextView rating_view = (TextView) mPopup.findViewById(R.id.rating);
        if (length > 0) {
            rating_view.setText(snippet[0]);
            rating_view.setVisibility(View.VISIBLE);
        } else {
            rating_view.setVisibility(View.GONE);
        }

        TextView open_now_view = (TextView) mPopup.findViewById(R.id.open_now);
        if (length > 1 && !snippet[1].equals("")) {
            open_now_view.setText(snippet[1]);
            open_now_view.setVisibility(View.VISIBLE);
        } else {
            open_now_view.setVisibility(View.GONE);
        }

        TextView view1 = (TextView) mPopup.findViewById(R.id.view1);
        if (length > 2) {
            view1.setText(snippet[2]);
            view1.setVisibility(View.VISIBLE);
        } else {
            view1.setVisibility(View.GONE);
        }

        TextView view2 = (TextView) mPopup.findViewById(R.id.view2);
        if (length > 3) {
            view2.setText(snippet[3]);
            view2.setVisibility(View.VISIBLE);
        } else {
            view2.setVisibility(View.GONE);
        }

        TextView view3 = (TextView) mPopup.findViewById(R.id.view3);
        if (length > 4) {
            view3.setText(snippet[4]);
            view3.setVisibility(View.VISIBLE);
        } else {
            view3.setVisibility(View.GONE);
        }

        TextView view4 = (TextView) mPopup.findViewById(R.id.view4);
        if (length > 5) {
            view4.setText(snippet[5]);
            view4.setVisibility(View.VISIBLE);
        } else {
            view4.setVisibility(View.GONE);
        }

        return (mPopup);
    }
}
