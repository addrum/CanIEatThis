package com.adamshort.canieatthis.app.util;

public interface NextPageListener {

    /**
     * @param visibility 0 for visible, 1 for invisible, 2 for gone
     */
    void showMore(int visibility);
}
