package com.example.anne.testtravel;

import android.graphics.Bitmap;

/**
 * Created by yunqingjiang on 5/11/17.
 */

public class Friend {
    private boolean isFriend;
    private final Bitmap imageResource;
    //private final String imageUrl;

    public Friend(boolean isFriend, Bitmap imageResource) {
        this.isFriend = isFriend;
        this.imageResource = imageResource;
        //this.imageUrl = imageUrl;
    }

    public Bitmap getImageResource() {
        return imageResource;
    }

    public boolean getIsFriend() {
        return isFriend;
    }

    public void setIsFriend(boolean isFriend) {
        this.isFriend = isFriend;
    }



}
