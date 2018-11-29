package com.molian.finger;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class FingerActivity extends FragmentActivity {
    private TextView mCancelBtn;
    private ImageView mShakeImage;
    private TextView mTryText;
    private int mCount = 5;
    private FingerPrintUtils mFingerUtils;
    private TranslateAnimation mAnimation;
    public static final int PERMISSIONS_REQUEST_CODE = 1002;
    private String id = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finger);
        String[] noPermission = getNoPermission();
        if (noPermission.length == 0) {
            init();
        } else {
            ActivityCompat.requestPermissions(this, noPermission, PERMISSIONS_REQUEST_CODE);
        }
    }

    public void init() {
        id = FingerStore.getFingerId();
        if (TextUtils.isEmpty(id)) {
            Toast.makeText(this, "无法使用，请在安全中心注册或者修改指纹", Toast.LENGTH_SHORT).show();
            try {
                String pcgName = "com.molian.ucenter";
                Intent intent = new Intent();
                PackageManager packageManager = getPackageManager();
                Intent launchIntentForPackage = packageManager.getLaunchIntentForPackage(pcgName);
                if (launchIntentForPackage != null) {
                    startActivity(launchIntentForPackage);
                }
//                ComponentName componentName = new ComponentName(pcgName, "com.molian.ucenter.UserCenterActivity");
//                intent.setComponent(componentName);
//                startActivity(intent);
            } catch (Exception e) {
            }
            finish();
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("hct.intent.action.FINGERPRINT_ID");
        registerReceiver(mFingerReceiver, intentFilter);

        mCancelBtn = findViewById(R.id.finger_cancel);
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mTryText = findViewById(R.id.try_text);
        mTryText.setText("请轻触感应器验证指纹");
        mShakeImage = findViewById(R.id.image_finger);
        mAnimation = new TranslateAnimation(0, 5, 0, 0);
        mAnimation.setDuration(800);
        mAnimation.setInterpolator(new CycleInterpolator(8));
        mFingerUtils = new FingerPrintUtils(this);
        if (mFingerUtils.canFinger()) {
            mFingerUtils.setFingerPrintListener(new FingerCallBack());
        } else {
            Toast.makeText(this, "无法启用指纹识别", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(mFingerReceiver);
        } catch (Exception e) {
        }
        super.onDestroy();
    }

    private class FingerCallBack extends FingerprintManagerCompat.AuthenticationCallback {
        //多次识别失败,并且，不能短时间内调用指纹验证
        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            super.onAuthenticationError(errMsgId, errString);
            if (errMsgId == 7) {
                mTryText.setText(errString);
                return;
            }
            if (mCount > 1) {
                mCount--;
                mTryText.setText("指纹不匹配，还可以尝试" + mCount + "次");
            } else {
                mTryText.setText("请稍后重试!");
                setResult(RESULT_CANCELED);
                finish();
            }
//            mHandler.sendMessageDelayed(new Message(), 1000 * 60);
        }

        //出错可恢复
        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            super.onAuthenticationHelp(helpMsgId, helpString);
        }

        //识别成功
        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            if (PubConfig.IS_DEMO) {
                fingerId = 1;
                String fi = FingerStore.md5(String.valueOf(fingerId));
                if (fi.equals(id)) {
                    Intent intent1 = new Intent();
                    intent1.putExtra("fingerId", fi);
                    FingerActivity.this.setResult(RESULT_OK, intent1);
                    FingerActivity.this.finish();
                    ZToast.showShortToast(FingerActivity.this, "验证成功");
                }
            }
        }

        //识别失败
        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            if (mCount > 1) {
                mCount--;
                mTryText.setText("指纹不匹配，还可以尝试" + mCount + "次");
            }
            mShakeImage.startAnimation(mAnimation);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            onPermissionsResult();
        }
    }

    private void onPermissionsResult() {
        String[] noPermission = getNoPermission();
        if (noPermission.length == 0) {
            init();
        } else {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                toSettingPage();
            }
            ActivityCompat.requestPermissions(this, noPermission, PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10011) {
            onPermissionsResult();
        }
    }

    private void toSettingPage() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 10011);
    }

    public String[] getNoPermission() {
        String[] needs = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        List<String> nops = new ArrayList<String>();
        for (String str : needs) {
            int i = ActivityCompat.checkSelfPermission(this, str);
            if (i != PackageManager.PERMISSION_GRANTED) {
                nops.add(str);
            }
        }
        String[] result = new String[nops.size()];
        for (int i = 0; i < nops.size(); i++) {
            result[i] = nops.get(i);
        }
        return result;
    }

    @Override
    public void onBackPressed() {
        mFingerUtils.stopsFingerPrintListener();
        super.onBackPressed();
    }

    int fingerId = -1;
    BroadcastReceiver mFingerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String reason = intent.getStringExtra("reason");
            //获取到的fingerId为完成指纹录制后传出的fingerId；
            if (reason.equals("enroll")) {
            }
            //获取到的fingerId为指纹解锁传出的fingerId，解锁失败时fingerId统一为0；
            else if (reason.equals("onAuthenticated")) {
                fingerId = intent.getIntExtra("fingerId", -1);
                String fi = FingerStore.md5(String.valueOf(fingerId));
                if (fi.equals(id)) {
                    Intent intent1 = new Intent();
                    intent1.putExtra("fingerId", fi);
                    FingerActivity.this.setResult(RESULT_OK, intent1);
                    FingerActivity.this.finish();
                    ZToast.showShortToast(FingerActivity.this, "验证成功");
                } else {
                    ZToast.showShortToast(FingerActivity.this, "不是这根手指哦~");
                    mFingerUtils.stopsFingerPrintListener();
                    mFingerUtils = new FingerPrintUtils(FingerActivity.this);
                    mFingerUtils.setFingerPrintListener(new FingerCallBack());
                }
            }
            //获取到的fingerId为删除指纹时的fingerId
            else if (reason.equals("reason")) {

            }
        }
    };
}

