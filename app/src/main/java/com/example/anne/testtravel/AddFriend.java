package com.example.anne.testtravel;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class AddFriend extends AppCompatActivity {

    boolean isfriend;
    Friend friend;
    /*Friend[] friends = {
            new Friend("user1", false, R.drawable.user1),
            new Friend("user2", false, R.drawable.user2),
            new Friend("user3", false, R.drawable.user3),
    };
*/
    //Friend[] friendtest;
    ArrayList<Friend> friendtest = new ArrayList<>();
    ImageAdapter imageAdapter;

    String Event = "addfriend"; //event can be get friend info from pic or become friend
    String PictureName;
    private final static String TAG = "Add Friend Page";
    Context mContext = this;
    GridView gridview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        gridview = (GridView) findViewById(R.id.gridview);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                friend = friendtest.get(position);
                isfriend = friend.getIsFriend();
                if (isfriend) {
                    ConfirmAddFriend("Remove Friend","Remove this user from your friend list?");
                } else{
                    ConfirmAddFriend("Add Friend","Would you like to be friend with this user?");
                }
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            //Event = extras.getString("Event");
            PictureName = extras.getString("Picture Name");
            Log.i(TAG, "Picture Name: " + PictureName);
        }

        new SendLambda().execute(Event);

    }

    public class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<Friend> friends;

        public ImageAdapter(Context c , ArrayList<Friend> friends) {
            mContext = c;
            this.friends = friends;
        }

        public int getCount() {
            return friends.size();
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            final Friend friend = friends.get(position);

            if (convertView == null) {
                final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
                convertView = layoutInflater.inflate(R.layout.linearlayout_friend, null);
            }

            final ImageView imageView = (ImageView) convertView.findViewById(R.id.friend_profile);
            final ImageView imageViewFavorite = (ImageView)convertView.findViewById(R.id.imageview_favorite);

            imageView.setImageBitmap(friend.getImageResource());
            //imageView.setImageResource(friend.getImageResource());
            imageViewFavorite.setImageResource(
                    friend.getIsFriend() ? R.drawable.star_enabled : R.drawable.star_disabled);

            return convertView;
        }
    }

    protected void ConfirmAddFriend (String title, String message) {
        final AlertDialog.Builder confirm = new AlertDialog.Builder(this);
        confirm.setTitle(title);
        confirm.setMessage(message);
        confirm.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                friend.setIsFriend(!isfriend);
                dialog.dismiss();
            }
        });
        confirm.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        confirm.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                // This tells the GridView to redraw itself
                // in turn calling your BooksAdapter's getView method again for each cell
                imageAdapter.notifyDataSetChanged();
            }
        });
        confirm.show();
    }

    private class SendLambda extends AsyncTask<String, Void, String> {
        ProgressDialog dialog;
        protected void onPreExecute(){
            dialog = new ProgressDialog(AddFriend.this);
            dialog.setMessage(AddFriend.this
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

                String Body_Data = "{" + '"' + "event" + '"' + ":" + '"' +event + '"' +"," +
                        '"' + "file" + '"' + ":" + '"' + PictureName + '"' + "}";
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

        protected void onPostExecute(String Response) {
            dialog.dismiss();
            //System.out.println(Response);
            ArrayList<String> imglist = GetFriendImg(Response);
            try {
                for (int i = 0; i < imglist.size(); i++){
                    byte[] decodedBytes = Base64.decode(imglist.get(i), Base64.DEFAULT);
                    Bitmap FriendImg = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    int nh =  (FriendImg.getHeight() * (512 / FriendImg.getWidth()));
                    FriendImg = Bitmap.createScaledBitmap(FriendImg, 512, nh, true);
                    FriendImg = Bitmap.createBitmap(FriendImg, 0, 0, FriendImg.getWidth(), FriendImg.getHeight(), matrix, true);
                    friendtest.add(new Friend(false,FriendImg));
                }

            }catch (Exception e){
                e.printStackTrace();
            }
            System.out.println(friendtest.size());

            imageAdapter = new ImageAdapter(mContext, friendtest);
            gridview.setAdapter(imageAdapter);

        }
    }

    protected ArrayList<String> GetFriendImg(String result) {
        ArrayList<String> list = new ArrayList<>();
        String[] strArr;
        try{
            JSONArray jsonArray = new JSONArray(result);
            strArr = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                strArr[i] = jsonArray.getString(i);
                list.add(strArr[i]);
            }
            //System.out.println(strArr[1]);
        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }

}

