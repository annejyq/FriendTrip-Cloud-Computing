package com.example.anne.testtravel;

/**
 * Created by yunqingjiang on 5/5/17.
 */

public class Constants {

    public static final String ACCESS_KEY_ID = "";

    public static final String ACCESS_KEY = "";

    /*
     * You should replace these values with your own. See the README for details
     * on what to fill in.
     */
    public static final String COGNITO_POOL_ID = ""; 

    /*
     * Region of your Cognito identity pool ID.
     */
    public static final String COGNITO_POOL_REGION = "us-east-1";

    /*
     * Note, you must first create a bucket using the S3 console before running
     * the sample (https://console.aws.amazon.com/s3/). After creating a bucket,
     * put it's name in the field below.
     */
    public static final String BASIC_BUCKET = "data-base-for-created-users";

    public static final String DYNAMIC_BUCKET = "sourcebucketforimage";

    public static final String PHOTO_BUCKET = "bucket-for-photos";

    //public static final String BUCKET_NAME_PROFILE = "sourcebucketforimage";

    /*
     * Region of your bucket.
     */
    public static final String BUCKET_REGION = "US_EAST_1";
}
