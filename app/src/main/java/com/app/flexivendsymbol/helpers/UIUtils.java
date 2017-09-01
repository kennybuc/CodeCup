package com.app.flexivendsymbol.helpers;

import android.content.Context;
import android.widget.Toast;

public class UIUtils {
    public static void showToast(Context context, int resId) {
        String message = context.getString(resId);
        showToast(context, message);
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
