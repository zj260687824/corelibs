package com.core.upgrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.core.upgrade.entity.VersionUpdateConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UpgradeApi {
    private static final String TAG = "UpgradeApi";
    //更新URL
    public static boolean upgraded = false;

    Activity mActivity = null;
    Context mContext = null;

    String appId;
    int versionCode;
    String versionName;
    boolean force = false;
    int iconRes = -1;
    int smallIconRes = -1;
    String channel = "";

    private UpgradeApi(Activity activity) {
        mActivity = activity;
    }

    private UpgradeApi(Context context) {
        this.mContext = context;
    }

    public static UpgradeApi create(Activity activity) {
        return new UpgradeApi(activity);
    }

    public static UpgradeApi create(Context context) {
        return new UpgradeApi(context);
    }

    public UpgradeApi setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public UpgradeApi setVersionCode(int versionCode) {
        this.versionCode = versionCode;
        return this;
    }

    public UpgradeApi setVersionName(String versionName) {
        this.versionName = versionName;
        return this;
    }

    public UpgradeApi setIconRes(int iconRes) {
        this.iconRes = iconRes;
        return this;
    }

    public UpgradeApi setSmallIconRes(int smallIconRes) {
        this.smallIconRes = smallIconRes;
        return this;
    }

    public UpgradeApi setForce(boolean force) {
        this.force = force;
        return this;
    }

    public UpgradeApi setChannel(String channel) {
        this.channel = channel;
        return this;
    }

    /**
     * 统一更新api
     */
    public void checkUpgrade() {
        /* if (UpgradeApi.upgraded) return;*/
        /*String[] parmas = new String[]{appId, String.valueOf(versionCode), versionName};*/
        Object[] parmas = new Object[]{appId, versionCode, channel};
        UpgradeTask upgradeTask = new UpgradeTask(mActivity != null ? mActivity : mContext);
        upgradeTask.execute(parmas);

    }

    private static JSONObject post(String mdediaContent, String url) throws IOException {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        MediaType MEDIA_TYPE_NORAML_FORM = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(MEDIA_TYPE_NORAML_FORM, mdediaContent);
        Request requestPost = new Request.Builder().url(url).post(requestBody).build();
        Response response = httpClient.newCall(requestPost).execute();
        JSONObject result = null;
        if (response.isSuccessful() && response.body() != null) {
            String bodyString = response.body().string();
            result = JSON.parseObject(bodyString);
        }
        return result;
    }

    //异步更新请求
    class UpgradeTask extends AsyncTask<Object, Integer, JSONObject> {
        private Context context;

        public UpgradeTask(Context context) {
            this.context = context;
        }

        @Override
        protected JSONObject doInBackground(Object... params) {
            //请求api 获取更新
            //?applicationId=[APPLICATION_ID]&versionCode=[VERSION_CODE]&versionName=[VERSION_NAME]";
            JSONObject media = new JSONObject();
            media.put("applicationId", params[0]);
            media.put("versionCode", params[1]);
            media.put("channelId", params[2]);
            /* media.put("versionName", params[2]);*/
            JSONObject result = null;
            try {
                result = post(media.toJSONString(), Constant.UPDATE_VERSION);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            if (jsonObject == null) return;
            final String uversionName = jsonObject.getString("versionName");
            final String remark = jsonObject.getString("remark");
            final String downloadUrl = jsonObject.getString("address");
            final int uversionCode = jsonObject.getIntValue("versionCode");
            if (uversionCode <= versionCode || TextUtils.isEmpty(downloadUrl)) {
                return;
            }
            //主活动已关闭，退出更新
            if (context instanceof Activity) {
                if (((Activity) context).isFinishing() || jsonObject == null) return;
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setIcon(R.mipmap.ic_launcher)
                        .setTitle("更新提示")
                        .setMessage(remark)
                        .setNegativeButton("下载更新", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.e(TAG, "onClick: 开始下载...");
                                startDownloa(context, downloadUrl, uversionName);
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                if (force) {
                                    ((Activity) context).finish();
                                }
                            }
                        }).create().show();
            } else {
                startDownloa(context, downloadUrl, uversionName);
            }
        }

        private void startDownloa(Context context, String downloadUrl, String uversionName) {
            VersionUpdateConfig.getInstance()//获取配置实例
                    .setContext(context)//设置上下文
                    .setDownLoadURL(downloadUrl)//设置文件下载链接
                    .setNewVersion(uversionName)//设置即将下载的APK的版本号,避免重复下载
                    //.setFileSavePath(savePath)//设置文件保存路径（可不设置）
                    .setNotificationIconRes(iconRes)//设置通知图标
                    .setNotificationSmallIconRes(smallIconRes)//设置通知小图标
                    .setNotificationTitle("更新下载")//设置通知标题
                    .startDownLoad();//开始下载
            UpgradeApi.upgraded = true;
            report(downloadUrl);
        }

    }


    private static void report(final String apkurl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject media = new JSONObject();
                    media.put("address", apkurl);
                    try {
                        post(media.toJSONString(), Constant.UPDATE_REPORT);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
