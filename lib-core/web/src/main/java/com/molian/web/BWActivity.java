package com.molian.web;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

import com.alibaba.fastjson.JSONObject;
import com.molian.web.utils.ApiDal;
import com.molian.web.utils.StatusBarUtils;
import com.molian.web.utils.Tasks;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

/**
 * webActivity抽象类
 */

public abstract class BWActivity extends FragmentActivity {
    WebFragment fragment = null;
    public static final int PERMISSIONS_REQUEST_CODE = 1002;
    String fingerId = "";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 23) {
            StatusBarUtils.translucentBar(this);
        }
        setContentView(R.layout.activity_web);

        String[] noPermission = getNoPermission();
        if (noPermission.length == 0) {//添加fragment之前判斷是否需要添加
            locaInit();
        } else {
            ActivityCompat.requestPermissions(this, noPermission, PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * 本地初始化加载
     */
    private void locaInit() {
        if (beforeBindView()) {
            fullInView();
        }
    }

    /**
     * 請求用戶信息
     */
    public void requestUser() {
        Task.call(new Callable<JSONObject>() {
            @Override
            public JSONObject call() {
                TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                @SuppressLint({"MissingPermission", "HardwareIds"}) String imei = tm.getDeviceId();
                try {
                    return ApiDal.loginForMei(imei, fingerId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }, Tasks.NET_EXECUTOR).continueWith(new Continuation<JSONObject, Void>() {
            @Override
            public Void then(Task<JSONObject> task) {
                if (task != null && task.getResult() != null && task.getResult().containsKey("code") && task.getResult().getIntValue("code") == 200) {
                    UserContext.setUser(task.getResult().getString("data"));
                }
                fullInView();
                return null;
            }
        }, Tasks.UI_EXECUTOR);

    }


    /**
     * 添加view之前操作
     *
     * @return false 不添加webFragment true 直接添加
     */
    public boolean beforeBindView() {
        return true;
    }

    /**
     * 添加webView到视图中
     */
    public void fullInView() {
        //创建webFragment并展示到页面上
        fragment = WebFragment.create(this, baseUrl(), UserContext.getUser());
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(R.id.fragmentLayout, fragment);
        transaction.commit();
        onCreated();
    }

    /**
     * frament添加完成后 其它初始化操作
     */
    public void onCreated() {

    }

    //必须实现 提交一级地址
    abstract public String baseUrl();

    //必须实现 返回是否需要登录
    abstract public boolean needLogin();

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (fragment != null && fragment.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public String[] getNoPermission() {
        String[] needs = new String[]{Manifest.permission.READ_PHONE_STATE};
        List<String> nops = new ArrayList<String>();
        for (String str : needs) {
            int i = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            onRequestPermissionsResult();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10011) {
            onRequestPermissionsResult();
        }
        if (requestCode == 10001 && resultCode == RESULT_OK) {
            fingerId = data.getStringExtra("fingerId");
            if (needLogin()) {
                requestUser();
            }
        }
    }

    public void onRequestPermissionsResult() {
        String[] noPermission = getNoPermission();
        if (noPermission.length == 0) {
            locaInit();
        } else {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                toSettingPage();
            } else {
                ActivityCompat.requestPermissions(this, noPermission, PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    private void toSettingPage() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 10011);
    }
}
