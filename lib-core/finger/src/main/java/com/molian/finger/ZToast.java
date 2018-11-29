package com.molian.finger;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Zhang Jie on 2016/9/8.
 */
public class ZToast {
    private static Toast mToast;

    public static void showShortToast(final Context context, final Object message, final Object... args) {
        cancelToast(mToast);
        if (message == null)
            return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mToast = Toast.makeText(context, String.format(message.toString(), args), Toast.LENGTH_SHORT);
                    mToast.show();
                }
            });
        } else {
            mToast = Toast.makeText(context, String.format(message.toString(), args), Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    public static void showLongToast(final Context context, final Object message, final Object... args) {
        cancelToast(mToast);
        if (message == null)
            return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mToast = Toast.makeText(context, String.format(message.toString(), args), Toast.LENGTH_LONG);
                    mToast.show();
                }
            });
        } else {
            mToast = Toast.makeText(context, String.format(message.toString(), args), Toast.LENGTH_LONG);
            mToast.show();
        }
    }

    public static void cancelToast(final Toast toast) {


        if (toast != null) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    toast.cancel();
                }
            }, 100);

        }
    }

    public static void clean() {
        mToast = null;
    }

    public static void toast(Context context, String str) {
        showShortToast(context, str);
    }

    public static void toast(Context context, int strRes) {
        showShortToast(context, context.getResources().getString(strRes));
    }
}
