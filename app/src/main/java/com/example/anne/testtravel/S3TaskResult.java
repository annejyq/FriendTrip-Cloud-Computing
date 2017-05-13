package com.example.anne.testtravel;

import android.net.Uri;

/**
 * Created by yunqingjiang on 5/10/17.
 */

public class S3TaskResult {
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
