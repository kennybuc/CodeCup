package com.app.flexivendsymbol.services;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;

import com.app.flexivendsymbol.activities.MainActivity;
import com.app.flexivendsymbol.helpers.Helper;
import com.app.flexivendsymbol.helpers.Recognizer;

public class RecognizerService extends IntentService {

    public static final String KEY_DATA = "KEY_DATA";
    public static final String KEY_RESULT = "KEY_RESULT";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public RecognizerService() {
        super("RecognizerService");
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            Recognizer recognizer = new Recognizer();
            byte[] bytes = intent.getByteArrayExtra(KEY_DATA);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            short[][] pixelLine = Helper.getPixelStripRotated(bitmap);
            long num = recognizer.Recognize(pixelLine, false);
            if (num != -1) {
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MainActivity.ResponseReceiver.ACTION_PROCESSED);
                broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                broadcastIntent.putExtra(KEY_RESULT, String.valueOf(num));
                sendBroadcast(broadcastIntent);
            }
        }
    }

}
