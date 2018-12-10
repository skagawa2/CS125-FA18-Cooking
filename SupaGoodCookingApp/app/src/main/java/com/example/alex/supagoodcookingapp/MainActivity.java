
package com.example.alex.supagoodcookingapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;
import android.webkit.WebView;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int RESULT_LOAD_IMAGE = 1;
    private static RequestQueue requestQueue;

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

        outputTextBox = findViewById(R.id.outputText);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageToUpload:
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
                break;
            case R.id.identifyImage:
                Connection conn = new Connection();
                conn.execute("");
                try {
                    outputTextBox.setText(waitUntilResponse(conn, 10));
                } catch (Exception e) {
                    Log.e("waitUntilResponse", "exception: " + e.getMessage());
                }
                break;
            case R.id.add:
                Log.d("toAdd", toAdd.getText().toString());
                for (int i = 0; i < foodNames.size(); i++) {
                    Log.d("foodnamesindex", foodNames.get(i));
                }
                String addMe = toAdd.getText().toString();
                if (!foodNames.contains(addMe)) {
                    foodNames.add(addMe);
                }
                String display = new String();
                for (int i = 0; i < foodNames.size(); i++) {
                    display += (i + 1) + ")" + " " + foodNames.get(i) + "\n";
                }
                outputTextBox.setText(display);
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
     * @param uri
     * @return Return the selected ImagePath
     */
    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }


//    private static final int WRITE_EXTERNAL_STORAGE = 0;
//    private static final int READ_EXTERNAL_STORAGE = 1;
//
//    /**
//     * Requests the Camera permission.
//     * If the permission has been denied previously, a SnackBar will prompt the user to grant the
//     * permission, otherwise it is requested directly.
//     */
//    private void requestPermissions() {
//        ActivityCompat.requestPermissions(this, new String[] {
//                        Manifest.permission.READ_EXTERNAL_STORAGE,
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.INTERNET},
//                WRITE_EXTERNAL_STORAGE);
//    }
//
//    /**
//     * From Google Sample: https://github.com/googlesamples/android-RuntimePermissions/blob/master/Application/src/main/java/com/example/android/system/runtimepermissions/MainActivity.java
//     * Callback received when a permissions request has been completed.
//     */
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                                           int[] grantResults) {
//
//        if (requestCode == WRITE_EXTERNAL_STORAGE) {
//            // BEGIN_INCLUDE(permission_result)
//            // Received permission result for camera permission.
//            Log.i(TAG, "Received response for Camera permission request: " + Arrays.toString(permissions) + " \ngrantResults: " + Arrays.toString(grantResults));
//
//            // Check if the only required permission has been granted
//            if (grantResults.length == 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Camera permission has been granted, preview can be displayed
//                Log.i(TAG, "CAMERA permission has now been granted. Showing preview.");
//            } else {
//                Log.i(TAG, "CAMERA permission was NOT granted.");
//            }
//            // END_INCLUDE(permission_result)
//
//        }
//    }

    private class Connection extends AsyncTask {
        @Override
        protected Object doInBackground(Object... arg0) {
            connect();
            return null;
        }

        private List<ClarifaiOutput<Concept>> response;
        private String display = "";

        private void connect() {
            response = client.getDefaultModels()
                    .foodModel()
                    .predict()
                    .withInputs(ClarifaiInput.forImage(new File(getRealPathFromURI(selectedImage))))
                    .withMinValue(0.98) // minimum prediction value
                    .executeSync()
                    .get();
            Log.d("ClarifaiOutput", "output from API: " + response.toString());

            display = "Clarifai Prediction Rankings: \n";
            for (ClarifaiOutput<Concept> output : response) {
                int maxFoodNameLen = 0;
                for (Concept concept : output.data()) {
                    if (concept.name().length() > maxFoodNameLen) {
                        maxFoodNameLen = concept.name().length();
                    }
                }
                foodNames = new ArrayList<>();
                for (int i = 0; i < output.data().size(); i++) {
                    String foodName = output.data().get(i).name();
                    foodNames.add(foodName);
                    double predictionScore = output.data().get(i).value();
                    display += String.format("%d) foodName: %-"
                                    + maxFoodNameLen + "s predictionScore: %.2f%%\n",
                            i+1, foodName, predictionScore * 100);
                }
            }
        }

        public String getResponse() {
            return display;
        }
    }

    // https://sqa.stackexchange.com/questions/29379/how-to-wait-for-an-api-request-to-return-a-response
    public String waitUntilResponse(Connection conn, int TIMEOUT) throws Exception {
        String result = "";
        int i = 0;
        while (i < TIMEOUT) {
            result = conn.getResponse();
            if (!result.equals("")) {
                break;
            } else {
                Log.d("waitUntilResponse", "waiting...");
                TimeUnit.SECONDS.sleep(1);
                ++i;
                if (i == TIMEOUT) {
                    throw new TimeoutException("Timed out after waiting for " + i + " seconds");
                }
            }
        }
        return result;
    }

    private List<String> foodNames = new ArrayList<>();
    void startAPICall(android.view.View view) {
        startAPICall();
    }
    void startAPICall() {

        // create request URL from the food names
        String requestURL = String.format("https://www.food2fork.com/api/search?key=%s&q=", BuildConfig.Food2ForkApiKey);
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
        final LinearLayout f2fCards = (LinearLayout) findViewById(R.id.F2FCards);
        f2fCards.removeAllViews();

        try {
            final TextView readout = findViewById(R.id.jsonResult);

            String toDisplay = "";
            JSONArray recipes = (JSONArray) response.get("recipes");
            for (int i = 0; i < recipes.length(); i++) {

                final CardView f2fCard = new CardView(mContext);
                final ImageView foodImage = new ImageView(mContext);
                final TextView foodTitle = new TextView(mContext);
                final TextView foodLink = new TextView(mContext);
                final TextView publisherLink = new TextView(mContext);

                final JSONObject recipe = recipes.getJSONObject(i);

                String imageURL = (String) recipe.get("image_url");
                foodImage.setImageURI(getImageUri(mContext, getImageBitmap(imageURL.replace("http", "https"))));

                double socialRank = (double) recipe.get("social_rank");
                String recipeTitle = (String) recipe.get("title");
                foodTitle.setText(String.format("Score- %.1f: %s", socialRank, recipeTitle));

                final String recipeURL = (String) recipe.get("source_url");
                foodLink.setText(recipeURL);
                // SO answer: https://stackoverflow.com/questions/24237278/after-clicking-link-in-texview-how-to-open-that-link-in-webview-instead-of-defa
//                foodLink.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        WebView webView = findViewById(R.id.recipeViewer);
//                        webView.getSettings().setJavaScriptEnabled(true);
//                        webView.loadUrl(recipeURL);
//                    }}
//                );

                final String publisherURL = (String) recipe.get("publisher_url");
                publisherLink.setText(publisherURL);
//                publisherLink.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        WebView webView = findViewById(R.id.recipeViewer);
//                        webView.getSettings().setJavaScriptEnabled(true);
//                        webView.loadUrl(publisherURL);
//                    }}
//                );

                LinearLayout horizontalLayout = new LinearLayout(mContext);
                horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
                horizontalLayout.addView(foodImage);

                LinearLayout verticalLayout = new LinearLayout(mContext);
                verticalLayout.setOrientation(LinearLayout.VERTICAL);
                verticalLayout.addView(foodTitle);
                verticalLayout.addView(foodLink);
                verticalLayout.addView(publisherLink);
                horizontalLayout.addView(verticalLayout);

                f2fCard.addView(horizontalLayout);
                f2fCards.addView(f2fCard);
            }

            readout.setText(toDisplay);
        } catch (JSONException exception) {
            Log.e("JSONException", exception.getMessage());
        } catch (ClassCastException exception) {
            Log.e("ClassCastException", exception.getMessage());
        }
    }

    private Bitmap getImageBitmap(String url) {
        Bitmap bm = null;
        try {
            URL aURL = new URL(url);
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

    private Uri getImageUri(Context context, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
}