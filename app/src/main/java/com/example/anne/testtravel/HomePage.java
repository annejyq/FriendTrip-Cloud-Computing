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


}

