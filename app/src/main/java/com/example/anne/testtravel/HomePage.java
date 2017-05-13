package com.example.anne.testtravel;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;


public class HomePage extends AppCompatActivity {

    Button SignUp;
    Button SignIn;

    AmazonS3 s3;
    ImageView ProfileThumbnail;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "Sign In";

    boolean flag;
    String Event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),    //* get the context for the application *//*
                Constants.COGNITO_POOL_ID,    //* Identity Pool ID *//*
                Regions.US_WEST_2          //* Region for your identity pool--US_EAST_1 or EU_WEST_1*//*
        );

        s3 = new AmazonS3Client(credentialsProvider);
        ImageView Logo = (ImageView) findViewById(R.id.imageView2);
        Logo.setImageResource(R.drawable.friendtrip);
        //ProfileThumbnail = (ImageView) findViewById(R.id.ProfileThumbnail);

        SignIn = (Button) findViewById(R.id.SignIn);

        SignUp = (Button) findViewById(R.id.SignUp);

        //Button Test = (Button) findViewById(R.id.button);

        SignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = true;
                goToUsername();
            }
        });

        SignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = false;
                goToUsername();
            }
        });

        /*Test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //new SendLambda().execute("username");
                goToStart();

            }
        });*/
    }

    private void goToUsername() {

        Intent intent = new Intent(this, EnterUsername.class);
        if (flag) {
            Event = "login";
        }else{
            Event = "signup";
        }
        intent.putExtra("Event",Event);
        startActivity(intent);
    }

    private void goToStart() {
        Intent i = new Intent(this, Start.class);
        i.putExtra("username","test");
        startActivity(i);
    }

    private class SendLambda extends AsyncTask<String, Void, Bitmap> {
        protected Bitmap doInBackground(String... params) {
            String Gateway_url = "https://c5fctb7p49.execute-api.us-west-2.amazonaws.com/lambdatest/lambda-test";
            String Result = null;
            Bitmap decodedBitmap = null;
            try {
                URL url = new URL(Gateway_url);
                HttpURLConnection GatewayHttp = (HttpURLConnection) url.openConnection();
                GatewayHttp.setConnectTimeout(20000);
                GatewayHttp.setDoOutput(true);
                GatewayHttp.setDoInput(true);
                GatewayHttp.setRequestMethod("POST");
                GatewayHttp.setRequestProperty("Content-Type", "application/json");

                String username = params[0];

                String Body_Data = "{" + '"' + "event" + '"' + ":" + '"' + "login" + '"' + ","+
                        '"' + "image" + '"' + ":" + '"' + "user3.jpg" + '"'+"}";

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
                byte[] decodedBytes = Base64.decode(Result,Base64.DEFAULT);
                decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return decodedBitmap;
        }

        protected void onPostExecute(Bitmap b) {
            ProfileThumbnail.setImageBitmap(b);
        }
    }


    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                if (mCurrentPhotoPath != null) {
                    Bitmap bb = BitmapFactory.decodeFile(mCurrentPhotoPath);
                    bb = ScaleRotateBitmap(bb);
                    ProfileThumbnail.setImageBitmap(bb);
                    new S3PutObjectTask().execute(mCurrentPhotoPath);

                }
            }
        }

    }*/


    /*public Bitmap ScaleRotateBitmap(Bitmap b) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        int nh = (int) (b.getHeight() * (512.0 / b.getWidth()));
        //Log.i(TAG, "Resize : " + nh);
        Bitmap scaled = Bitmap.createScaledBitmap(b, 512, nh, true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaled, 0, 0, scaled.getWidth(), scaled.getHeight(), matrix, true);
        return rotatedBitmap;
    }*/

    /*private void TakeSignInPhoto() {
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

    }*/

    /*private File createImageFile() throws IOException {
        // Create an image file name
        Date date = new Date();
        String timeStamp = DateFormat.getDateTimeInstance().format(date);
        String imageFileName = "PNG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  *//* prefix *//*
                ".png",         *//* suffix *//*
                storageDir      *//* directory *//*
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.i(TAG, mCurrentPhotoPath);
        return image;
    }*/

    /*private class S3PutObjectTask extends AsyncTask<String, Void, S3TaskResult> {

        ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = new ProgressDialog(HomePage.this);
            //dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            //    @Override
            //    public void onDismiss(DialogInterface dialogInterface) {
            //        goToUsername();
            //    }
            //});
            dialog.setMessage(HomePage.this
                    .getString(R.string.sign_in));
            dialog.setCancelable(false);
            dialog.show();
        }

        protected S3TaskResult doInBackground(String... paths) {

            //if (paths == null || paths.length != 1) {
            //  Toast.makeText(getBaseContext(), "error in Uri", Toast.LENGTH_LONG).show();
            // return null;
            //}

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
                //String timeStamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, Locale.ENGLISH).format(date);
                String pictureName = "picture_" + timeStamp;

                Log.i(TAG, "User_Profile : " + pictureName);

                //File OutputPath = new File(Environment.getExternalStorageDirectory()+"/Android/datatest");
                File OutputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File UploadImg = new File(OutputPath, pictureName + ".png");

                if (! OutputPath.exists()){
                    if (! OutputPath.mkdirs()){
                        return null;
                    }
                }
                //OutputPath.mkdirs();

                Log.i(TAG,"Output path"+OutputPath.toString());
                Log.i(TAG,"UploadImg"+UploadImg.toString());

                //UploadImg.createNewFile();

                Bitmap b = BitmapFactory.decodeFile(path);
                int nh = (int) (b.getHeight() * (1024.0 / b.getWidth()));
                //Log.i(TAG, "Resize : " + nh);
                Bitmap scaled = Bitmap.createScaledBitmap(b, 1024, nh, true);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.PNG, 0 *//*ignored for PNG*//*, bos);
                byte[] bitmapdata = bos.toByteArray();
                FileOutputStream fos = new FileOutputStream(UploadImg);
                fos.write(bitmapdata);
                fos.flush();
                fos.close();

                PutObjectRequest por = new PutObjectRequest(
                        Constants.BUCKET_NAME_PROFILE,
                        pictureName,
                        UploadImg);

                por.withBucketName(Constants.BUCKET_NAME_PROFILE);
                s3.putObject(por);

                GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(
                        Constants.BUCKET_NAME_PROFILE, pictureName);

//
                //URL url = s3.generatePresignedUrl(urlRequest);

            } catch (Exception exception) {
                //Toast.makeText(getBaseContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG,exception.getMessage());
                result.setErrorMessage(exception.getMessage());
            }

            return result;

        }


        protected void onPostExecute(S3TaskResult result) {

            dialog.dismiss();
            Log.i(TAG, "Uploaded");

            if (result.getErrorMessage() != null) {

                displayErrorAlert(
                        HomePage.this.getString(R.string.upload_failure_title),
                        result.getErrorMessage());
            }
        }
    }*/

    /*protected void displayErrorAlert(String title, String message) {

        Log.e(TAG,"error");

        AlertDialog.Builder confirm = new AlertDialog.Builder(this);
        confirm.setTitle(title);
        confirm.setMessage(message);

        confirm.setNegativeButton(
                HomePage.this.getString(R.string.ok),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        HomePage.this.finish();
                    }
                });

        confirm.show().show();
    }*/


}

