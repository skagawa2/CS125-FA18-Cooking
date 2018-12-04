package com.example.alex.supagoodcookingapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.util.Output;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.util.Log;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.api.ClarifaiResponse;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.Model;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;

import java.util.Arrays;
import java.util.List;
import java.io.File;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int RESULT_LOAD_IMAGE = 1;

    ImageView imageToUpload;
    Button identifyImage;
    EditText uploadImageName;
    ClarifaiClient client;
    Uri selectedImage;

    public MainActivity() {
        String APIKEY = "d32c8dd9ed844a9a8c06f8d857a54011";
        client = new ClarifaiBuilder(APIKEY).buildSync();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageToUpload = (ImageView) findViewById(R.id.imageToUpload);

        identifyImage = (Button) findViewById(R.id.identifyImage);


        imageToUpload.setOnClickListener(this);
        identifyImage.setOnClickListener(this);
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
                System.out.println(conn.getResponse());
                break;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedImage = data.getData();
            Log.d("SELECT IMAGE", "selected image converted to file");
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

        private void connect() {
            response = client.getDefaultModels()
                    .foodModel()
                    .predict()
                    .withInputs(ClarifaiInput.forImage(new File(getRealPathFromURI(selectedImage))))
                    .executeSync()
                    .get();
        }

        public List<ClarifaiOutput<Concept>> getResponse() {
            return response;
        }
    }
}
