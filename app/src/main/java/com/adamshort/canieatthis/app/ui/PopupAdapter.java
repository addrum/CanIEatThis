package com.adamshort.canieatthis.app.ui;

/***
 * Copyright (c) 2012-2014 CommonsWare, LLC
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 * <p/>
 * From _The Busy Coder's Guide to Android Development_
 * https://commonsware.com/Android
 */


import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.adamshort.canieatthis.R;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

public class PopupAdapter implements InfoWindowAdapter {

    private View popup = null;
    private LayoutInflater inflater = null;

    public PopupAdapter(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return (null);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getInfoContents(Marker marker) {
        if (popup == null) {
            popup = inflater.inflate(R.layout.popup, null);
        }

        TextView title_view = (TextView) popup.findViewById(R.id.title);
        title_view.setText(marker.getTitle());

        String[] snippet = marker.getSnippet().split(",");
        int length = snippet.length;

        TextView open_now_view = (TextView) popup.findViewById(R.id.open_now);
        if (length > 0 && !snippet[0].equals("")) {
            open_now_view.setText(snippet[0]);
            open_now_view.setVisibility(View.VISIBLE);
        } else {
            open_now_view.setVisibility(View.GONE);
        }

        TextView rating_view = (TextView) popup.findViewById(R.id.rating);
        if (length > 1) {
            rating_view.setText(snippet[1]);
            rating_view.setVisibility(View.VISIBLE);
        } else {
            rating_view.setVisibility(View.GONE);
        }

        TextView view1 = (TextView) popup.findViewById(R.id.view1);
        if (length > 2) {
            view1.setText(snippet[2]);
            view1.setVisibility(View.VISIBLE);
        } else {
            view1.setVisibility(View.GONE);
        }

        TextView view2 = (TextView) popup.findViewById(R.id.view2);
        if (length > 3) {
            view2.setText(snippet[3]);
            view2.setVisibility(View.VISIBLE);
        } else {
            view2.setVisibility(View.GONE);
        }

        TextView view3 = (TextView) popup.findViewById(R.id.view3);
        if (length > 4) {
            view3.setText(snippet[4]);
            view3.setVisibility(View.VISIBLE);
        } else {
            view3.setVisibility(View.GONE);
        }

        TextView view4 = (TextView) popup.findViewById(R.id.view4);
        if (length > 5) {
            view4.setText(snippet[5]);
            view4.setVisibility(View.VISIBLE);
        } else {
            view4.setVisibility(View.GONE);
        }

        return (popup);
    }
}
