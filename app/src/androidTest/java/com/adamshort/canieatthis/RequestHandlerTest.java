package com.adamshort.canieatthis;

import android.test.ActivityUnitTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;

/**
 * Created by Adam on 17/02/2016.
 */
public class RequestHandlerTest extends ActivityUnitTestCase<ScanFragment> {

    public RequestHandlerTest() {
        super(ScanFragment.class);
    }

    @SmallTest
    public void testGetBarcodeInformation() {
        ScanFragment mainActivity = new ScanFragment();
        String barcode = "5054267003378";
        mainActivity.GetBarcodeInformation(barcode);
        Assert.assertEquals(mainActivity.ingredientResponseView.getText(), "");
    }

}
