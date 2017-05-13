package com.example.anne.testtravel;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
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

public class Start extends AppCompatActivity {

    private static final String TAG = "Start Page";
    private static final int PHOTO_SELECTED = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;

    Button UploadFromGallary;
    Button UploadFromCamera;
    Button AddFriend;
    Button ViewGallary;
    ImageView mImageView;
    TextView WelcomeInfo;


    AmazonS3 s3;
    String pictureN = "";
    String mCurrentPhotoPath; //Path of photo from camera
    String mSelectedPhotoPath; //Path of photo from gallery
    File ImageFromCamera;

    String Username;
    String Event; // Event can be "add friend" and "my journal"
    String ImgToShowString;

    Bitmap ImgToShow;
    ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),    /* get the context for the application */
                Constants.COGNITO_POOL_ID,    /* Identity Pool ID */
                Regions.US_WEST_2          /* Region for your identity pool--US_EAST_1 or EU_WEST_1*/
        );

        s3 = new AmazonS3Client(credentialsProvider);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Username = extras.getString("username");
            ImgToShowString = extras.getString("picture");
            Log.i(TAG, "Username: " + Username);
        }
        // Load user profile
        if (ImgToShowString != null) {
            byte[] decodedBytes = Base64.decode(ImgToShowString, Base64.DEFAULT);
            ImgToShow = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            // Scale the profile image to thumbnail 256 to 512
            ImgToShow = ScaleRotateBitmap(ImgToShow, 512);
        }

        UploadFromGallary = (Button) findViewById(R.id.UploadFromGallery);
        UploadFromCamera = (Button) findViewById(R.id.UploadByCamera);
        AddFriend = (Button) findViewById(R.id.AddFriend);
        ViewGallary = (Button) findViewById(R.id.ViewGallary);

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageBitmap(ImgToShow);
        WelcomeInfo = (TextView) findViewById(R.id.welcome);
        WelcomeInfo.append(Username);

        UploadFromGallary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Event = "photo";
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PHOTO_SELECTED);
            }
        });

        UploadFromCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
                Event = "photo";
            }
        });

        AddFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
                Event = "addfriend";
                //goToAddFriend();
            }
        });

        ViewGallary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Event = "gallery";
                goToGallery();
            }
        });

    }
    private void goToAddFriend(){
        Intent i = new Intent(this, AddFriend.class);
        i.putExtra("Event",Event);
        i.putExtra("Picture Name",pictureN);
        startActivity(i);
    }

    private void goToGallery(){
        Intent intent = new Intent(this, MyGallery.class);
        intent.putExtra("Event",Event);
        intent.putExtra("Username",Username);
        startActivity(intent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        if (requestCode == PHOTO_SELECTED) {
            if (resultCode == RESULT_OK && imageReturnedIntent != null && imageReturnedIntent.getData() != null) {
                Uri uri = imageReturnedIntent.getData();
                if (null != uri) {
                    // Get the path from the Uri
                    mSelectedPhotoPath = getPathFromURI(uri);
                    Log.i(TAG, "Selected Image Path : " + mSelectedPhotoPath);
                    //Set the image in ImageView
                    //Bitmap b = BitmapFactory.decodeFile(mSelectedPhotoPath);
                    //b = ScaleRotateBitmap(b, 1024);
                    //mImageView.setImageBitmap(b);
                    new S3PutObjectTask().execute(mSelectedPhotoPath);

                }
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                if (mCurrentPhotoPath != null) {
                    //Bitmap bb = BitmapFactory.decodeFile(mCurrentPhotoPath);
                    //bb = ScaleRotateBitmap(bb, 1024);
                    //mImageView.setImageBitmap(bb);
                    Log.i(TAG, "Current Image Path : " + mCurrentPhotoPath);
                    new S3PutObjectTask().execute(mCurrentPhotoPath);
                }
            }
        }

    }

    public Bitmap ScaleRotateBitmap(Bitmap b, int Size) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        int nh =  (b.getHeight() * (Size / b.getWidth()));
        Bitmap scaled = Bitmap.createScaledBitmap(b, Size, nh, true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaled, 0, 0, scaled.getWidth(), scaled.getHeight(), matrix, true);
        return rotatedBitmap;
    }

    public String getPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        Log.i(TAG, "res " + res);
        return res;
    }

    private void dispatchTakePictureIntent() {
        Uri photoURI;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            //File photoFile = null;
            try {
                //photoFile = createImageFile();
                ImageFromCamera = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("ERROR", ex.getMessage(), ex);
            }
            // Continue only if the File was successfully created
            if (ImageFromCamera != null) {
                photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", ImageFromCamera);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }

    }

    private File createImageFile() throws IOException {
        // Create an image file name
        Date date = new Date();
        String timeStamp = DateFormat.getDateTimeInstance().format(date);
        String imageFileName = "PNG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.i(TAG, "Image created and returned");
        return image;
    }


    private class S3PutObjectTask extends AsyncTask<String, Void, S3TaskResult> {
        protected void onPreExecute() {
            dialog = new ProgressDialog(Start.this);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    if (Event.equals("addfriend"))
                    goToAddFriend();
                }
            });
            dialog.setMessage(Start.this
                    .getString(R.string.uploading));
            dialog.setCancelable(false);
            dialog.show();
        }

        protected S3TaskResult doInBackground(String... paths) {
            // The file location of the image selected.
            if (paths == null || paths.length != 1) {
                Toast.makeText(getBaseContext(), "error in Uri", Toast.LENGTH_LONG).show();
                return null;
            }

            // The file location of the image selected.
            String path = paths[0];

            S3TaskResult result = new S3TaskResult();

            // Put the image data into S3.
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String pictureName = Username + "_" + timeStamp+".jpg"; // e.g. Anne_20170512.jpg
                pictureN = pictureName;
                //File OutputPath = new File(Environment.getExternalStorageDirectory()+"/Android/datatest");
                File OutputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File UploadImg = new File(OutputPath, pictureN + ".png");

                if (!OutputPath.exists()) {
                    if (!OutputPath.mkdirs()) {
                        return null;
                    }
                }

                Log.i(TAG, "Output path" + OutputPath.toString());

                Bitmap b = BitmapFactory.decodeFile(path);

                int nh = (int) (b.getHeight() * (1024.0 / b.getWidth()));
                //Log.i(TAG, "Resize : " + nh);
                Bitmap scaled = Bitmap.createScaledBitmap(b, 1024, nh, true);
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap rotated = Bitmap.createBitmap(scaled, 0, 0, scaled.getWidth(), scaled.getHeight(), matrix, true);
                //Bitmap scaled = ScaleRotateBitmap(b,1024);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                rotated.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                byte[] bitmapdata = bos.toByteArray();
                FileOutputStream fos = new FileOutputStream(UploadImg);
                fos.write(bitmapdata);
                fos.flush();
                fos.close();

                PutObjectRequest por = new PutObjectRequest(
                        Constants.PHOTO_BUCKET,
                        pictureName,
                        UploadImg);

                por.withBucketName(Constants.PHOTO_BUCKET);
                s3.putObject(por);
                Log.i(TAG, "Photo Uploaded : " + pictureName);

            } catch (Exception exception) {
                Log.e(TAG, exception.getMessage());
                result.setErrorMessage(exception.getMessage());
            }
            return result;
        }

        protected void onPostExecute(S3TaskResult result) {
            Log.i(TAG, "Uploaded");
            dialog.dismiss();

            if (result.getErrorMessage() != null) {
                displayErrorAlert(
                        Start.this
                                .getString(R.string.upload_failure_title),
                        result.getErrorMessage());
            }
            //new SendLambda().execute(Username);
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

                String Body_Data = "{" + '"' + "event" + '"' + ":" + Event + "," +
                        '"' + "file" + '"' + ":" + '"' + "user1.jpg" + '"' + "}";
                //"Username.jpg"

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
                    //Log.i("Received Image",Result);
                }
                //byte[] decodedBytes = Base64.decode(Result,Base64.DEFAULT);
                //decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return Result;
        }

        protected void onPostExecute(String Result) {
            dialog.dismiss();
            //status.setText(Result);
            //ProfileThumbnail.setImageBitmap(b);
            //if (Result.equals("0")){
            //    displayError(EnterUsername.this.getString(R.string.error),
            //            "Wrong User. Please try again.");
            //}else{
            //    PictoLoad = Result;
            // }
        }
    }
    protected void displayErrorAlert(String title, String message) {

        Log.e(TAG, "display error");
        AlertDialog.Builder confirm = new AlertDialog.Builder(this);
        confirm.setTitle(title);
        confirm.setMessage(message);

        confirm.setNegativeButton(
                Start.this.getString(R.string.ok),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });

        confirm.show().show();
    }
    protected void displayAlert(String title, String message) {

        AlertDialog.Builder confirm = new AlertDialog.Builder(this);
        confirm.setTitle(title);
        confirm.setMessage(message);

        confirm.setNegativeButton(
                Start.this.getString(R.string.ok),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });

        confirm.show().show();
    }

    private class S3TaskResult {
        String errorMessage = null;
        Uri uri = null;

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public Uri getUri() {
            return uri;
        }

        public void setUri(Uri uri) {
            this.uri = uri;
        }
    }

    private void goToHomePage() {

        Intent intent = new Intent(this, Start.class);

        startActivity(intent);

    }
}
