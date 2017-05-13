package com.example.anne.testtravel;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EnterUsername extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "Take Face Photo";
    EditText mUsername;
    Button TakeFacePhoto;
    String mCurrentPhotoPath;
    AmazonS3 s3;
    String Username;
    String PictoLoad = null;
    ProgressDialog dialog;
    String Event;
    String BucketName;
    String DialogString;
    boolean DismissFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_username);

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),    /* get the context for the application */
                Constants.COGNITO_POOL_ID,    /* Identity Pool ID */
                Regions.US_WEST_2        /* Region for your identity pool--US_EAST_1 or EU_WEST_1*/
        );

        s3 = new AmazonS3Client(credentialsProvider);

        mUsername = (EditText) findViewById(R.id.username);

        TakeFacePhoto = (Button) findViewById(R.id.takephoto);

        TakeFacePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Username = mUsername.getText().toString();
                Log.i("username",Username);
                TakeSignInPhoto();
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Event = extras.getString("Event");
            Log.i(TAG, "Event Type: " + Event);
            if (Event.equals("login")){
                BucketName = Constants.DYNAMIC_BUCKET;
                DialogString = getString(R.string.sign_in);

            }else if(Event.equals("signup")){
                BucketName = Constants.BASIC_BUCKET;
                DialogString = getString(R.string.sign_up);
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                if (mCurrentPhotoPath != null) {
                    new S3PutObjectTask().execute(mCurrentPhotoPath);

                }
            }
        }

    }
    private void TakeSignInPhoto() {
        Uri photoURI;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }

    }

    private File createImageFile() throws IOException {
        // Create an image file name
        Date date = new Date();
        String timeStamp = DateFormat.getDateTimeInstance().format(date);
        String imageFileName = "PNG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.i(TAG, mCurrentPhotoPath);
        return image;
    }

    private class S3PutObjectTask extends AsyncTask<String, Void, S3TaskResult> {
        protected void onPreExecute() {
            dialog = new ProgressDialog(EnterUsername.this);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    if (DismissFlag){
                        goToStart();
                    }
                }
            });
            dialog.setMessage(DialogString);
            dialog.setCancelable(false);
            dialog.show();
        }

        protected S3TaskResult doInBackground(String... paths) {
            // The file location of the image selected.
            if (paths == null || paths.length != 1) {
                return null;
            }

            // The file location of the image selected.
            String path = paths[0];
            S3TaskResult result = new S3TaskResult();

            // Put the image data into S3.
            try {
                String pictureName = Username + ".jpg";

                Log.i(TAG, "Picture Name : " + pictureName);

                File OutputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File UploadImg = new File(OutputPath, pictureName + ".png");

                // Make sure output path exists
                if (! OutputPath.exists()){
                    if (! OutputPath.mkdirs()){
                        return null;
                    }
                }
                Log.i(TAG,"Output path"+OutputPath.toString());

                // Convert Bitmap to File
                Bitmap b = BitmapFactory.decodeFile(path);
                int nh = (int) (b.getHeight() * (1024.0 / b.getWidth()));
                Bitmap scaled = Bitmap.createScaledBitmap(b, 1024, nh, true);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                byte[] bitmapdata = bos.toByteArray();
                FileOutputStream fos = new FileOutputStream(UploadImg);
                fos.write(bitmapdata);
                fos.flush();
                fos.close();

                // Upload file to S3
                PutObjectRequest por = new PutObjectRequest(
                        BucketName,
                        pictureName,
                        UploadImg);
                por.withBucketName(BucketName);
                s3.putObject(por);

            } catch (Exception exception) {
                Log.e(TAG,exception.getMessage());
                result.setErrorMessage(exception.getMessage());
            }
            return result;
        }

        protected void onPostExecute(S3TaskResult result) {
            Log.i(TAG, "Image Uploaded");
            if (result.getErrorMessage() != null) {
                displayErrorAlert(
                        EnterUsername.this.getString(R.string.upload_failure_title),
                        result.getErrorMessage());
            }
            // Send Request to API gateway
            new SendLambda().execute(Username);
            //dialog.dismiss();
        }
    }



    private class SendLambda extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params) {
            String Gateway_url = "https://c5fctb7p49.execute-api.us-west-2.amazonaws.com/lambdatest/lambda-test";
            String Result = null;
            try {
                URL url = new URL(Gateway_url);
                HttpURLConnection GatewayHttp = (HttpURLConnection) url.openConnection();
                GatewayHttp.setConnectTimeout(20000);
                GatewayHttp.setDoOutput(true);
                GatewayHttp.setDoInput(true);
                GatewayHttp.setRequestMethod("POST");
                GatewayHttp.setRequestProperty("Content-Type", "application/json");

                String username = params[0];

                String Body_Data = "{" + '"' + "event" + '"' + ":" + '"' + Event + '"' +","+
                        '"' + "file" + '"' + ":" + '"' + username + ".jpg" + '"'+"}";
                //username +

                GatewayHttp.setFixedLengthStreamingMode(Body_Data.getBytes().length);
                OutputStream SendtoCloud = GatewayHttp.getOutputStream();

                PrintWriter out = new PrintWriter(SendtoCloud);
                out.print(Body_Data);
                out.close();

                int responseCode = GatewayHttp.getResponseCode();
                System.out.println("Response Code of API Gateway: " + responseCode);
                StringBuilder response = new StringBuilder();

                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(GatewayHttp.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    Result = response.toString();
                    Log.i("Response from Lambda",Result);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return Result;
        }

        protected void onPostExecute(String Result) {
            //status.setText(Result);
            //ProfileThumbnail.setImageBitmap(b);
            if (Result.equals("0")){
                DismissFlag = false;
                displayError(EnterUsername.this.getString(R.string.error),
                        "Wrong User. Please try again.");
            }else{
                DismissFlag = true;
                PictoLoad = Result;
            }
            dialog.dismiss();
        }
    }

    // Display Error Alert for Upload
    protected void displayErrorAlert(String title, String message) {

        Log.e(TAG,"error");
        AlertDialog.Builder confirm = new AlertDialog.Builder(this);
        confirm.setTitle(title);
        confirm.setMessage(message);
        confirm.setNegativeButton(
                EnterUsername.this.getString(R.string.ok),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        EnterUsername.this.finish();
                    }
                });
        confirm.show().show();
    }

    // Display Error Information from Lambda
    protected void displayError(String title, String message) {

        Log.e(TAG,"Error");
        AlertDialog.Builder confirm = new AlertDialog.Builder(this);
        confirm.setTitle(title);
        confirm.setMessage(message);

        confirm.setNegativeButton(
                EnterUsername.this.getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        d.dismiss();
                    }
                });

        confirm.show().show();
    }


    private void goToStart() {
        Intent intent = new Intent(this, Start.class);
        intent.putExtra("username", Username);
        intent.putExtra("picture",PictoLoad); // Save response from lambda as string
        startActivity(intent);
    }
}
