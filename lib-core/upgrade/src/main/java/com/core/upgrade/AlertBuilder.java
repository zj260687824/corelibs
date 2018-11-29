package com.core.upgrade;

import android.app.Activity;
import android.support.v7.app.AlertDialog;

import com.alibaba.fastjson.JSONObject;

import com.core.upgrade.entity.VersionUpdateConfig;


/**
 * 更新提示窗口
 */
public class AlertBuilder {
    private AlertDialog.Builder builder = null;
    private AlertDialog dialog = null;
    private Activity mActivity = null;
    //通知栏图标
    private int iconRes = 0;
    //通知栏小图标
    private int smallIconRes = 0;
    //通知栏标题
    private String title = "下载更新";
    private JSONObject updateJson;

    /**
     * 创建一个dialog buider
     *
     * @param activity
     * @return
     */
    public static AlertBuilder create(Activity activity) {
        return new AlertBuilder(activity);
    }

    private AlertBuilder(Activity activity) {
        builder = new AlertDialog.Builder(activity);
    }

    /**
     * 设置更新信息
     *
     * @param jsonObject
     * @return
     */
    public AlertBuilder setUpdateJSON(JSONObject jsonObject) {
        updateJson = jsonObject;
        return this;
    }

    /**
     * 设置通知栏大图标
     *
     * @param resId
     * @return
     */
    public AlertBuilder setIconRes(int resId) {
        this.iconRes = resId;
        return this;
    }

    /**
     * 设置通知栏大图标
     *
     * @param resId
     * @return
     */
    public AlertBuilder setSmallIconRes(int resId) {
        this.smallIconRes = resId;
        return this;
    }

    /**
     * 通過 builder创建dialog并显示
     */
    public void show() {
        if (builder == null) {
            return;
        }
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.setTitle("应用更新");
        String remark = updateJson.getString("remark");
        dialog.setMessage(remark);
        dialog.show();
        dialog.dismiss();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        builder = null;
        dialog = null;
    }

    private void downloadUpdate() {
        VersionUpdateConfig.getInstance()//获取配置实例
                .setContext(mActivity)//设置上下文
                .setDownLoadURL("")//设置文件下载链接
                .setNewVersion("1.0.1")//设置即将下载的APK的版本号,避免重复下载
                //.setFileSavePath(savePath)//设置文件保存路径（可不设置）
                .setNotificationIconRes(iconRes)//设置通知图标
                .setNotificationSmallIconRes(smallIconRes)//设置通知小图标
                .setNotificationTitle(title)//设置通知标题
                .startDownLoad();//开始下载

    }

}
