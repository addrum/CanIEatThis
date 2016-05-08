package com.adamshort.canieatthis;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
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

import java.util.List;

public class ScanFragment extends Fragment {

    public static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    public static final String EXTENSION = ".json";
    public static final int DOWNLOAD = 0;
    public static final int PRODUCT = 1;

    public static boolean DEBUG;
    private static boolean fragmentCreated = false;
    public static String BASE_URL = "http://world.openfoodfacts.org/api/v0/product/";
    private static String barcode = "";

    private boolean resetIntro = false;

    private static Switch dairyFreeSwitch;
    private static Switch vegetarianSwitch;
    private static Switch veganSwitch;
    private static Switch glutenFreeSwitch;
    private TableLayout switchesTableLayout;
    private TextView introTextView;
    private TextView itemTextView;
    private TextView ingredientsTitleText;
    private TextView ingredientResponseView;
    private TextView tracesTitleText;
    private TextView tracesResponseView;
    private ProgressBar progressBar;
    private DataPasser dataPasser;
    private DataQuerier dataQuerier;

    public void SetItemsFromDataPasser() {
        if (!resetIntro) {
            if (dataPasser == null) dataPasser = DataPasser.getInstance();

            SetDietarySwitches(dataPasser.isDairy(), dataPasser.isVegetarian(), dataPasser.isVegan(), dataPasser.isGluten());

            if (dataPasser.areSwitchesVisible()) {
                SetSwitchesVisibility(View.VISIBLE);
            } else {
                SetSwitchesVisibility(View.INVISIBLE);
            }

            if (itemTextView != null) {
                itemTextView.setText(dataPasser.getQuery());
                if (dataPasser.isItemVisible()) {
                    itemTextView.setVisibility(View.VISIBLE);
                } else {
                    itemTextView.setVisibility(View.INVISIBLE);
                }
            }

            if (introTextView != null) {
                if (dataPasser.isIntroVisible()) {
                    introTextView.setVisibility(View.VISIBLE);
                } else {
                    introTextView.setVisibility(View.INVISIBLE);
                }
            }

            if (dataPasser.isResponseVisible()) {
                SetResponseItemsVisibility(View.VISIBLE);
            } else {
                SetResponseItemsVisibility(View.INVISIBLE);
            }

            if (!dataPasser.isFromSearch()) {
                if (ingredientResponseView != null) {
                    ingredientResponseView.setText(dataPasser.getIngredients());
                }
                if (tracesResponseView != null) {
                    if (dataPasser.getTraces() != null) {
                        if (!dataPasser.getTraces().equals("")) {
                            tracesResponseView.setText(dataPasser.getTraces());
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("ResourceType")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_scan, container, false);

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

        DEBUG = android.os.Debug.isDebuggerConnected();

        dataPasser = DataPasser.getInstance();

        dataQuerier = DataQuerier.getInstance(getActivity());

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            Log.d("setUserVisibleHint", "Fragment is visible.");
            Log.d("setUserVisibleHint", Boolean.toString(fragmentCreated));
            if (fragmentCreated) SetItemsFromDataPasser();
            resetIntro = false;
        } else {
            Log.d("setUserVisibleHint", "Fragment is not visible.");
            SetSwitchesVisibility(View.INVISIBLE);
            SetResponseItemsVisibility(View.INVISIBLE);
            if (itemTextView != null) {
                itemTextView.setVisibility(View.INVISIBLE);
            }
            if (introTextView != null) {
                introTextView.setVisibility(View.VISIBLE);
            }
            resetIntro = true;
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
//            if (DEBUG) {
//                GetBarcodeInformation("5000168183732");
//                startActivity(new Intent(getActivity().getBaseContext(), AddProductActivity.class));
//            } else {
            startActivityForResult(intent, 0);
//            }
        } catch (ActivityNotFoundException anfe) {
            //on catch, show the download dialog
            showDialog(this.getActivity(), "No Scanner Found", "Download a scanner now?", "Yes", "No", DOWNLOAD).show();
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
                        Log.e("showDialog", anfe.toString());
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
                            Log.e("showDialog", anfe.toString());
                        }
                    } catch (Exception e) {
                        Log.e("showDialog", "Couldn't start new AddProductActivity");
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
        if (((MainActivity) getActivity()).hasInternetConnection()) {
            RequestHandler rh = new RequestHandler(getActivity().getBaseContext(), progressBar, new RequestHandler.AsyncResponse() {
                @Override
                public void processFinish(String output) {
                    JSONObject product = dataQuerier.ParseIntoJSON(output);
                    ProcessResponse(product);
                }
            });
            rh.execute(BASE_URL + barcode + EXTENSION);
        } else {
            CSVReader csvReader = new CSVReader(getActivity().getBaseContext(), progressBar, new CSVReader.AsyncResponse() {
                @Override
                public void processFinish(JSONObject output) {
                    ProcessResponse(output);
                }
            });
            csvReader.execute(barcode);
        }
    }

    public void ProcessResponse(JSONObject product) {
        Log.d("ProcessResponse", "Product: " + product);

        try {
            if (product != null) {
                String item = product.getString("product_name");
                String ingredients = product.getString("ingredients_text");
                String traces = product.getString("traces");

                List<String> editedIngredients = IngredientsList.StringToList(ingredients);
                editedIngredients = IngredientsList.RemoveUnwantedCharacters(editedIngredients, "[_]|\\s+$\"", "");
                List<String> editedTraces = IngredientsList.StringToList(traces);
                editedTraces = IngredientsList.RemoveUnwantedCharacters(editedTraces, "[_]|\\s+$\"", "");

                boolean dairy = dataQuerier.IsDairyFree(editedIngredients);
                boolean vegan = dataQuerier.IsVegan(editedIngredients);
                boolean vegetarian = true;
                // if something is vegan it is 100% vegetarian
                if (!vegan) {
                    vegetarian = dataQuerier.IsVegetarian(editedIngredients);
                }
                boolean gluten = dataQuerier.IsGlutenFree(editedIngredients);

                if (editedTraces.size() > 0) {
                    if (!editedTraces.get(0).equals("")) {
                        for (String trace : editedTraces) {
                            boolean d = dataQuerier.IsDairyFree(trace);
                            if (!d) {
                                dairy = false;
                            }
                            boolean v = dataQuerier.IsVegan(trace);
                            if (!v) {
                                vegan = false;
                            }
                            boolean ve = dataQuerier.IsVegetarian(trace);
                            if (!ve) {
                                vegetarian = false;
                            }
                            boolean g = dataQuerier.IsGlutenFree(trace);
                            if (!g) {
                                gluten = false;
                            }
                        }
                    }
                }

                SetItemTitleText(item);
                SetDietarySwitches(dairy, vegetarian, vegan, gluten);
                SetIngredientsResponseTextBox(editedIngredients.toString().replace("[", "").replace("]", ""));
                SetTracesResponseTextBox(editedTraces.toString().replace("[", "").replace("]", ""));

                SetSwitchesVisibility(View.VISIBLE);
                introTextView.setVisibility(View.INVISIBLE);
                SetResponseItemsVisibility(View.VISIBLE);

                if (editedIngredients.size() < 1 || editedIngredients.get(0).equals("")) {
                    SetIngredientsResponseTextBox("No ingredients found");
                }
                if (editedTraces.size() < 1 || editedTraces.get(0).equals("")) {
                    SetTracesResponseTextBox("No traces found");
                }
            } else {
                showDialog(this.getActivity(), "Product Not Found", "Add the product to the database?", "Yes", "No", PRODUCT).show();
            }
        } catch (JSONException e) {
            Log.e("ProcessResponse", "Issue ParseIntoJSON(response)");
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
        itemTextView.setText(item);
        itemTextView.setVisibility(View.VISIBLE);
    }

    public void SetDietarySwitches(boolean dairy, boolean vegetarian, boolean vegan, boolean gluten) {
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
