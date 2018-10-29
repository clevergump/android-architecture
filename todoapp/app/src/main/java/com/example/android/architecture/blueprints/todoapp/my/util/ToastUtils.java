package com.example.android.architecture.blueprints.todoapp.my.util;

import android.support.annotation.StringRes;
import android.widget.Toast;

import com.example.android.architecture.blueprints.todoapp.my.App;

public class ToastUtils {
    public static void showShort(@StringRes final int stringResId) {
        doToast(stringResId, true);
    }

    public static void showShort(String msg) {
        doToast(msg, true);
    }

    public static void showLong(@StringRes final int stringResId) {
        doToast(stringResId, false);
    }

    public static void showLong(String msg) {
        doToast(msg, false);
    }

    private static void doToast(@StringRes int stringResId, boolean isToastShort) {
        int duration = isToastShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
        Toast.makeText(App.getInstance(),
                stringResId, duration).show();
    }

    private static void doToast(String msg, boolean isToastShort) {
        int duration = isToastShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
        Toast.makeText(App.getInstance(),
                msg, duration).show();
    }
}