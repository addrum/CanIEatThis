package com.adamshort.canieatthis;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class ScanFragment extends Fragment {

    public static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    public static final String EXTENSION = ".json";
    public static final int DOWNLOAD = 0;
    public static final int PRODUCT = 1;

    public static boolean DEBUG;

    public static String BASE_URL = "http://world.openfoodfacts.org/api/v0/product/";

    private static String barcode = "";

    private static Context context;

    private static Switch dairyFreeSwitch;
    private static Switch vegetarianSwitch;
    private static Switch veganSwitch;
    private static Switch glutenFreeSwitch;

    private String response;

    private TableLayout switchesTableLayout;

    private TextView introTextView;
    private TextView itemTextView;
    private TextView ingredientsTitleText;
    private TextView ingredientResponseView;
    private TextView tracesTitleText;
    private TextView tracesResponseView;

    private ProgressBar progressBar;

    private RequestHandler rh;

    private static boolean fragmentCreated = false;

    public void SetItemsFromDataPasser() {
        Log.d("DEBUG: ", "Dairy: " + DataPasser.getInstance().isDairy());
        Log.d("DEBUG: ", "Vegetarian: " + DataPasser.getInstance().isVegetarian());
        Log.d("DEBUG: ", "Vegan: " + DataPasser.getInstance().isVegan());
        Log.d("DEBUG: ", "Gluten: " + DataPasser.getInstance().isGluten());
        SetAllergenSwitches(DataPasser.getInstance().isDairy(), DataPasser.getInstance().isVegetarian(), DataPasser.getInstance().isVegan(), DataPasser.getInstance().isGluten());
        if (DataPasser.getInstance().areSwitchesVisible()) {
            SetSwitchesVisibility(View.VISIBLE);
        } else {
            SetSwitchesVisibility(View.INVISIBLE);
        }

        if (itemTextView != null) {
            itemTextView.setText(DataPasser.getInstance().getQuery());
            if (DataPasser.getInstance().isItemVisible()) {
                itemTextView.setVisibility(View.VISIBLE);
            } else {
                itemTextView.setVisibility(View.INVISIBLE);
            }
        }

        if (introTextView != null) {
            if (DataPasser.getInstance().isIntroVisible()) {
                introTextView.setVisibility(View.VISIBLE);
            } else {
                introTextView.setVisibility(View.INVISIBLE);
            }
        }

        if (DataPasser.getInstance().isResponseVisible()) {
            SetResponseItemsVisibility(View.VISIBLE);
        } else {
            SetResponseItemsVisibility(View.INVISIBLE);
        }

        if (!DataPasser.getInstance().isFromSearch()) {
            if (ingredientResponseView != null) {
                ingredientResponseView.setText(DataPasser.getInstance().getIngredients());
            }
            if (tracesResponseView != null) {
                tracesResponseView.setText(DataPasser.getInstance().getTraces());
            }
        }
    }

    @SuppressWarnings("ResourceType")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.activity_main, container, false);

        switchesTableLayout = (TableLayout) view.findViewById(R.id.switchesTableLayout);

        introTextView = (TextView) view.findViewById(R.id.introTextView);
        itemTextView = (TextView) view.findViewById(R.id.itemTitleText);
        ingredientsTitleText = (TextView) view.findViewById(R.id.ingredientsTitleText);
        ingredientResponseView = (TextView) view.findViewById(R.id.ingredientsResponseView);
        tracesTitleText = (TextView) view.findViewById(R.id.tracesTitleText);
        tracesResponseView = (TextView) view.findViewById(R.id.tracesResponseView);

        Button scanButton = (Button) view.findViewById(R.id.scanButton);

        dairyFreeSwitch = (Switch) view.findViewById(R.id.dairyFreeSwitch);
        vegetarianSwitch = (Switch) view.findViewById(R.id.vegetarianSwitch);
        veganSwitch = (Switch) view.findViewById(R.id.veganSwitch);
        glutenFreeSwitch = (Switch) view.findViewById(R.id.glutenFreeSwitch);

        dairyFreeSwitch.setClickable(false);
        vegetarianSwitch.setClickable(false);
        veganSwitch.setClickable(false);
        glutenFreeSwitch.setClickable(false);

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        SetItemsFromDataPasser();

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBar();
            }
        });

        fragmentCreated = true;

        context = getContext();

        DEBUG = android.os.Debug.isDebuggerConnected();

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            Log.d("ScanFragment", "Fragment is visible.");
            Log.d("FragmentCreated", Boolean.toString(fragmentCreated));
            if (fragmentCreated) SetItemsFromDataPasser();
        } else {
            Log.d("ScanFragment", "Fragment is not visible.");
        }
    }

    @Override
    public void onViewStateRestored(Bundle inState) {
        super.onViewStateRestored(inState);

        SetItemsFromDataPasser();
    }

    //product barcode mode
    public void scanBar() {
        try {
            //start the scanning activity from the com.google.zxing.client.android.SCAN intent
            Intent intent = new Intent(ACTION_SCAN);
            intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
            if (DEBUG) {
                GetBarcodeInformation("5000295008069");
//                startActivity(new Intent(context, AddProductActivity.class));
            } else {
                startActivityForResult(intent, 0);
            }
        } catch (ActivityNotFoundException anfe) {
            //on catch, show the download dialog
            showDialog(this.getActivity(), "No Scanner Found", "Download a scanner code activity?", "Yes", "No", DOWNLOAD).show();
        }
    }

    //alert dialog for downloadDialog
    private static AlertDialog showDialog(final Activity act, CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo, int dialogNumber) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(act);
        dialog.setTitle(title);
        dialog.setMessage(message);

        if (dialogNumber == DOWNLOAD) {
            dialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        act.startActivity(intent);
                    } catch (ActivityNotFoundException anfe) {
                        Log.d("ERROR", anfe.toString());
                    }
                }
            });
            dialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
        }

        if (dialogNumber == PRODUCT) {
            dialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        Intent intent = new Intent(act, AddProductActivity.class);
                        intent.putExtra("barcode", barcode);

                        try {
                            act.startActivity(intent);
                        } catch (ActivityNotFoundException anfe) {
                            Log.d("ERROR", anfe.toString());
                        }
                    } catch (Exception e) {
                        Log.d("PRODUCT Yes", "Couldn't start new AddProductActivity");
                    }
                }
            });
            dialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
        }

        return dialog.show();
    }

    //on ActivityResult method
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                //get the extras that are returned from the intent
                String contents = intent.getStringExtra("SCAN_RESULT");
                //String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

                GetBarcodeInformation(contents);
                barcode = contents;
            }
        }
    }

    public void GetBarcodeInformation(String barcode) {
        boolean connection = false;
        final Activity activity = getActivity();
        if (connection) {
            try {
                rh = new RequestHandler(getContext(), progressBar, new RequestHandler.AsyncResponse() {
                    @Override
                    public void processFinish(String output) {
                        JSONObject product = ResponseQuerier.getInstance(activity).ParseIntoJSON(response);
                        ProcessResponse(product);
                    }
                });
                rh.execute(BASE_URL + barcode + EXTENSION);
            } catch (Exception e) {
                Log.e("ERROR", "Couldn't get a response");
                e.printStackTrace();
            }
        } else {
            try {
                progressBar.setVisibility(View.VISIBLE);
                JSONObject response = ResponseQuerier.getInstance(getActivity()).parseCSV(new InputStreamReader(context.getAssets().open("products.csv")), barcode, progressBar);
                ProcessResponse(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void ProcessResponse(JSONObject product) {
        Log.d("DEBUG", "Product: " + product);
        progressBar.setVisibility(View.INVISIBLE);

        try {
            if (product != null) {
                String item = product.getString("product_name");
                String ingredients = product.getString("ingredients_text");
                String traces = product.getString("traces");

                List<String> editedIngredients = IngredientsList.StringToList(ingredients);
                editedIngredients = IngredientsList.RemoveUnwantedCharacters(editedIngredients, "[_]|\\s+$\"", "");
                List<String> editedTraces = IngredientsList.StringToList(traces);
                editedTraces = IngredientsList.RemoveUnwantedCharacters(editedTraces, "[_]|\\s+$\"", "");

                boolean dairy = ResponseQuerier.getInstance(this.getActivity()).IsDairyFree(editedIngredients);
                boolean vegetarian = ResponseQuerier.getInstance(this.getActivity()).IsVegetarian(editedIngredients);
                boolean vegan = ResponseQuerier.getInstance(this.getActivity()).IsVegan(editedIngredients);
                boolean gluten = ResponseQuerier.getInstance(this.getActivity()).IsGlutenFree(editedIngredients);

                SetItemTitleText(item);
                SetAllergenSwitches(dairy, vegetarian, vegan, gluten);
                SetIngredientsResponseTextBox(editedIngredients.toString());
                SetTracesResponseTextBox(editedTraces.toString());
                SetSwitchesVisibility(View.VISIBLE);
                introTextView.setVisibility(View.INVISIBLE);
                SetResponseItemsVisibility(View.VISIBLE);
            } else {
                showDialog(this.getActivity(), "Product Not Found", "Add the product to the database?", "Yes", "No", PRODUCT).show();
            }
        } catch (JSONException e) {
            Log.e("ERROR", "Issue ParseIntoJSON(response)");
        }
    }


    public void SetSwitchesVisibility(int visibility) {
        if (switchesTableLayout != null) {
            switchesTableLayout.setVisibility(visibility);
        }
        if (dairyFreeSwitch != null) {
            dairyFreeSwitch.setVisibility(visibility);
        }
        if (vegetarianSwitch != null) {
            vegetarianSwitch.setVisibility(visibility);
        }
        if (veganSwitch != null) {
            veganSwitch.setVisibility(visibility);
        }
        if (glutenFreeSwitch != null) {
            glutenFreeSwitch.setVisibility(visibility);
        }
    }

    public void SetResponseItemsVisibility(int visibility) {
        if (ingredientsTitleText != null) {
            ingredientsTitleText.setVisibility(visibility);
        }
        if (ingredientResponseView != null) {
            ingredientResponseView.setVisibility(visibility);
        }
        if (tracesTitleText != null) {
            tracesTitleText.setVisibility(visibility);
        }
        if (tracesResponseView != null) {
            tracesResponseView.setVisibility(visibility);
        }
    }

    public void SetItemTitleText(String item) {
        itemTextView.setText(String.format(getString(R.string.product), item));
        itemTextView.setVisibility(View.VISIBLE);
    }

    public void SetAllergenSwitches(boolean dairy, boolean vegetarian, boolean vegan, boolean gluten) {
        dairyFreeSwitch.setChecked(dairy);
        vegetarianSwitch.setChecked(vegetarian);
        veganSwitch.setChecked(vegan);
        glutenFreeSwitch.setChecked(gluten);
    }

    public void SetIngredientsResponseTextBox(String response) {
        ingredientResponseView.setText(response);
        ingredientResponseView.setVisibility(View.VISIBLE);
    }

    public void SetTracesResponseTextBox(String response) {
        tracesResponseView.setText(response);
        tracesResponseView.setVisibility(View.VISIBLE);
    }
}
