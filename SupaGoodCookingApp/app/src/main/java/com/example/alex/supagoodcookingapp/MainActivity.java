package com.example.alex.supagoodcookingapp;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int RESULT_LOAD_IMAGE = 1;
    private static RequestQueue requestQueue;

    private final List<String> foodNames = new ArrayList<>();

    ImageView imageToUpload;
    Button identifyImage;
    TextView outputTextBox;
    ClarifaiClient client;
    Uri selectedImage;
    EditText toAdd;
    Button add;

    Activity mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        client = new ClarifaiBuilder(BuildConfig.ClarifaiApiKey).buildSync();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        toAdd = findViewById(R.id.toAdd);
        imageToUpload = findViewById(R.id.imageToUpload);
        requestQueue = Volley.newRequestQueue(this);
        identifyImage = findViewById(R.id.identifyImage);
        add = findViewById(R.id.add);

        imageToUpload.setOnClickListener(this);
        identifyImage.setOnClickListener(this);
        add.setOnClickListener(this);

        outputTextBox = findViewById(R.id.outputText);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageToUpload:
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
                break;
            case R.id.identifyImage:
                new IdentifyImageConnection().execute();
                break;
            case R.id.add:
                Log.d("toAdd", toAdd.getText().toString());
                String addMe = toAdd.getText().toString();
                if (addMe.charAt(0) == '-') {
                    foodNames.remove(addMe.substring(1));
                } else if (!foodNames.contains(addMe)) {
                foodNames.add(addMe);
                }
                String display = "Ingredients to search:\n";
                for (int i = 0; i < foodNames.size(); i++) {
                    display += (i + 1) + ")" + " " + foodNames.get(i) + "\n";
                }
                outputTextBox.setText(display);
                toAdd.setText("");
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedImage = data.getData();
            imageToUpload.setImageURI(selectedImage);
        }
    }

    /**
     * From SO Post: https://stackoverflow.com/questions/15432592/get-file-path-of-image-on-android
     *
     * @param uri uri to image
     * @return Return the selected ImagePath
     */
    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        String result = "";
        if (cursor != null) {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }


    void startAPICall(View view) {

        // create request URL from the food names
        String requestURL = String.format("https://www.food2fork.com/api/search?key=%s&q=",
                BuildConfig.Food2ForkApiKey);
        for (String foodName : foodNames) {
            String spaceReplaced = foodName.replace(" ", "%20");
            requestURL += spaceReplaced + ",";
        }
        // remove the extra comma at the end
        requestURL = requestURL.substring(0, requestURL.length() - 1);
        Log.d("Whattosearch", requestURL);

        try {
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.GET,
                    requestURL,
                    null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(final JSONObject response) {
                            Log.d("Recipe", response.toString());
                            writeQuote(response);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(final VolleyError error) {
                    Log.w("Recipe", error.toString());
                }
            });
            requestQueue.add(jsonObjectRequest);
        } catch (Exception e) {
            Log.e("JsonObjectRequest", e.getMessage());
        }
    }

    public void writeQuote(final JSONObject response) {
        final LinearLayout f2fCards = findViewById(R.id.F2FCards);
        f2fCards.removeAllViews();

        try {
            final TextView readout = findViewById(R.id.jsonResult);

            String toDisplay = "";
            JSONArray recipes = (JSONArray) response.get("recipes");
            if (recipes.length() == 0) {
                final CardView f2fCard = new CardView(mContext);
                final TextView text = new TextView(mContext);
                String msg = "No results found :(";
                text.setText(msg);
                f2fCard.addView(text);
                f2fCard.setRadius(5);
                f2fCard.setPadding(20, 20, 20, 20);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                );
                layoutParams.setMargins(20, 20, 20, 20);
                f2fCards.addView(f2fCard, layoutParams);
            }
            for (int i = 0; i < recipes.length(); i++) {
                final CardView f2fCard = new CardView(mContext);
                final ImageView foodImage = new ImageView(mContext);
                final TextView foodTitle = new TextView(mContext);
                final TextView foodLink = new TextView(mContext);
                final TextView publisherLink = new TextView(mContext);

                final JSONObject recipe = recipes.getJSONObject(i);

                String imageURL = (String) recipe.get("image_url");
                new AsyncGettingBitmapFromUrl(foodImage).execute(imageURL);

                double socialRank = (double) recipe.get("social_rank");
                String recipeTitle = (String) recipe.get("title");
                String title = "<a href='food2fork.com'>Food2Fork</a> " + String.format("Score: %.1f out of 100.0", socialRank);
                foodTitle.setText(Html.fromHtml(title));

                // create an clickable html string, convert it to actual html, then put onto textview
                final String recipeURL = "<p><a href='" + recipe.get("source_url") + "'>" + recipeTitle + "</a></p></br></br>";
                foodLink.setText(Html.fromHtml(recipeURL));
                foodLink.setMovementMethod(LinkMovementMethod.getInstance());
                foodLink.setAutoLinkMask(Linkify.WEB_URLS);

                // create an clickable html string, convert it to actual html, then put onto textview
                final String publisherURL = "<a href='" + recipe.get("publisher_url") + "'>Publisher Link</a>";
                publisherLink.setText(Html.fromHtml(publisherURL));
                publisherLink.setMovementMethod(LinkMovementMethod.getInstance());
                publisherLink.setAutoLinkMask(Linkify.WEB_URLS);

                LinearLayout horizontalLayout = new LinearLayout(mContext);
                horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
                horizontalLayout.addView(foodImage);

                LinearLayout verticalLayout = new LinearLayout(mContext);
                verticalLayout.setOrientation(LinearLayout.VERTICAL);
                verticalLayout.addView(foodTitle);
                verticalLayout.addView(foodLink);
                verticalLayout.addView(publisherLink);

                horizontalLayout.addView(verticalLayout);
                horizontalLayout.setWeightSum(1f);

                // to specify weight (the text gets 70% of width)
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.MATCH_PARENT,
                        .6f
                );
                layoutParams.setMargins(30, 10, 10, 10);
                verticalLayout.setLayoutParams(layoutParams);

                layoutParams = new LinearLayout.LayoutParams(450, 450, .4f);
                layoutParams.setMargins(20, 20, 20, 20);
                foodImage.setLayoutParams(layoutParams);
                foodImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

                // special cardview properties
                f2fCard.setRadius(5);
                f2fCard.setPadding(20, 20, 20, 20);
                layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                );
                layoutParams.setMargins(20, 20, 20, 20);

                f2fCard.addView(horizontalLayout, layoutParams);
                f2fCards.addView(f2fCard, layoutParams);
            }

            readout.setText(toDisplay);
        } catch (JSONException exception) {
            Log.e("JSONException", exception.getMessage());
        } catch (ClassCastException exception) {
            Log.e("ClassCastException", exception.getMessage());
        }
    }

    private class AsyncGettingBitmapFromUrl extends AsyncTask<String, Void, Bitmap> {
        private ImageView toEdit;
        AsyncGettingBitmapFromUrl(ImageView setEdit) {
            toEdit = setEdit;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bm = null;
            try {
                URL aURL = new URL(params[0].replace("http", "https"));
                URLConnection conn = aURL.openConnection();
                conn.connect();
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                bm = BitmapFactory.decodeStream(bis);
                bis.close();
                is.close();
            } catch (IOException e) {
                Log.e(TAG, "Error getting bitmap", e);
            }
            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                toEdit.setImageBitmap(result);
                Log.d("Bitmap/onPostExecute", "result posted");
            }
        }
    }


    private class IdentifyImageConnection extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... arg0) {
            List<ClarifaiOutput<Concept>> response = client.getDefaultModels()
                    .foodModel()
                    .predict()
                    .withInputs(ClarifaiInput.forImage(new File(getRealPathFromURI(selectedImage))))
                    .withMinValue(0.95) // minimum prediction value
                    .executeSync()
                    .get();
            Log.d("ClarifaiOutput", "output from API: " + response.toString());

            String display = "Clarifai Prediction Rankings: \n";
            for (ClarifaiOutput<Concept> output : response) {
                int maxFoodNameLen = 0;
                for (Concept concept : output.data()) {
                    if (concept.name().length() > maxFoodNameLen) {
                        maxFoodNameLen = concept.name().length();
                    }
                }
                foodNames.clear();
                for (int i = 0; i < output.data().size(); i++) {
                    String foodName = output.data().get(i).name();
                    foodNames.add(foodName);
                    double predictionScore = output.data().get(i).value();
                    display += String.format("%d) foodName: %-"
                                    + maxFoodNameLen + "s predictionScore: %.2f%%\n",
                            i+1, foodName, predictionScore * 100);
                }
            }
            return display;
        }

        @Override
        protected void onPostExecute(String result) {
            outputTextBox.setText(result);
            Log.d("Identify/onPostExecute", result);
        }
    }
}