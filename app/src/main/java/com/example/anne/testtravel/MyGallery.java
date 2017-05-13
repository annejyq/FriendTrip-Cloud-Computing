package com.example.anne.testtravel;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import static com.example.anne.testtravel.R.id.gridview;
import static com.example.anne.testtravel.R.id.time;

public class MyGallery extends AppCompatActivity {

    ArrayList<Journal> journaltest = new ArrayList<>();
    /*{{
        add(new Journal("Columbia University","2017-05-10",R.drawable.columbia));
        add(new Journal("Eiffel Tower","2015-04-18",R.drawable.eiffel));
    }};*/

    GalleryAdapter galleryAdapter;
    Context mmContext = this;
    String Event;
    String Username;
    String Label;
    EditText mEditKeyword;
    GridView gallery_gridview;
    private final static String TAG = "My Gallery Page";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_gallery);
        gallery_gridview = (GridView) findViewById(R.id.galleryview);

        mEditKeyword = (EditText) findViewById(R.id.EditKeyword);
        Button Search = (Button) findViewById(R.id.Search);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Event = extras.getString("Event");
            Username = extras.getString("Username");
            //PictureName = extras.getString("Picture Name");
            Log.i(TAG, "Event Name: " + Event);
        }

        Search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Label = mEditKeyword.getText().toString();
                new SendLambda().execute(Event,Label,Username);
            }
        });

        galleryAdapter = new GalleryAdapter(mmContext,journaltest);
        gallery_gridview.setAdapter(galleryAdapter);

    }

    public class GalleryAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<Journal> journals;

        public GalleryAdapter(Context c, ArrayList<Journal> journal) {
            mContext = c;
            this.journals = journal;
        }

        public int getCount() {
            return journals.size();
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            final Journal journal = journals.get(position);

            if (convertView == null) {
                final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
                convertView = layoutInflater.inflate(R.layout.linearlayout_journal, null);
            }

            final ImageView imageView = (ImageView) convertView.findViewById(R.id.journal_image);
            final TextView landmarkTextView = (TextView) convertView.findViewById(R.id.landmark);
            final TextView dateTextView = (TextView) convertView.findViewById(R.id.date);

            imageView.setImageBitmap(journal.getJournalImage());
            //imageView.setImageResource(journal.getJournalImage());
            landmarkTextView.setText(journal.getLandmark());
            dateTextView.setText(journal.getDate());

            return convertView;
        }
    }

    private class SendLambda extends AsyncTask<String, Void, String> {
        ProgressDialog dialog;
        protected void onPreExecute(){
            dialog = new ProgressDialog(MyGallery.this);
            dialog.setMessage(MyGallery.this
                    .getString(R.string.searching));
            dialog.setCancelable(false);
            dialog.show();
        }
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

                String event = params[0];
                String label = params[1];
                String username = params[2];

                String Body_Data = "{" + '"' + "event" + '"' + ":" + '"' +event + '"' +"," +
                        '"' + "username" + '"' + ":" + '"' + username + '"' + ","+
                        '"' + "label" + '"' + ":" + '"' + label + '"'+"}";
                //"Username.jpg"
                System.out.println(Body_Data);

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
                    Log.i("Received Image",Result);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return Result;
        }

        protected void onPostExecute(String result) {

            ArrayList<String> ImgList = new ArrayList<>();
            ArrayList<String> DateList = new ArrayList<>();

            try{
                JSONArray jsonArray = new JSONArray(result);
                Log.i("Array length",Integer.toString(jsonArray.length()));
                for (int i = 0; i < jsonArray.length(); i++) {
                    if ((i%2)==0){
                        ImgList.add(jsonArray.getString(i));
                    }else{
                        DateList.add(jsonArray.getString(i));

                    }
                }
                for (int q = 0; q < ImgList.size();q++){
                    byte[] decodedBytes = Base64.decode(ImgList.get(q), Base64.DEFAULT);
                    Bitmap JournalImg = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    JournalImg = ScaleRotateBitmap(JournalImg, 512);
                    journaltest.add(new Journal(Label,DateList.get(q),JournalImg));
                }
                //System.out.println(strArr[1]);
            }catch (Exception e){
                e.printStackTrace();
            }

            dialog.dismiss();
            System.out.println(journaltest.size());

            galleryAdapter = new GalleryAdapter(mmContext,journaltest);
            gallery_gridview.setAdapter(galleryAdapter);

        }
    }

    public Bitmap ScaleRotateBitmap(Bitmap b, int Size) {
        int nh =  (b.getHeight() * (Size / b.getWidth()));
        Bitmap scaled = Bitmap.createScaledBitmap(b, Size, nh, true);
        return scaled;
    }

}
