package com.example.anne.testtravel;

import android.graphics.Bitmap;

/**
 * Created by yunqingjiang on 5/11/17.
 */

public class Journal {
    private final String Landmark;
    private final String Date;
    private final Bitmap JournalImage;
    //private final int testImage;

    public Journal(String landmark, String date, Bitmap journalimage) {
        this.Landmark = landmark;
        this.Date = date;
        this.JournalImage= journalimage;
    }
    public String getLandmark() {
        return Landmark;
    }

    public String getDate(){
        return Date;
    }

    public Bitmap getJournalImage() {
        return JournalImage;
    }
}
