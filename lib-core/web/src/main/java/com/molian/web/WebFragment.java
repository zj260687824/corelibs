package com.molian.web;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.molian.web.utils.FileManager;
import com.molian.web.utils.PhotoUtils;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.common.Constant;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;

public class WebFragment extends Fragment implements View.OnClickListener {

    FragmentActivity mActivity;
    private WebView webView;
    private ProgressBar progressBar;
    private TextView top_title;
    private FrameLayout mErrorView; //加载错误的视图
    private FrameLayout mContentView;
    private View mTopBar;
    private boolean enadbleTopBar = false;
    private boolean loadError = false;
    private static final int REQUEST_CODE_SCAN = 10001;

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mUploadCallbackAboveL;
    public static final int SELECT_PIC_BY_TACK_PHOTO = 100;

    private Uri fileUri;
    public static final int TYPE_REQUEST_PERMISSION = 3;
    public static final int TYPE_CAMERA = 1;
    public static final int TYPE_GALLERY = 2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void bindActivity(FragmentActivity activity) {
        this.mActivity = activity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_web, null);
        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
        init();
        loadingUrl();
    }

    public static WebFragment create(FragmentActivity activity, String url, String user) {
        WebFragment fragment = new WebFragment();
        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        bundle.putString("userjson", user);
        fragment.setArguments(bundle);
        fragment.bindActivity(activity);
        return fragment;
    }

    private void initView() {
        webView = getView().findViewById(R.id.webView);
        progressBar = getView().findViewById(R.id.progressBar);
        top_title = getView().findViewById(R.id.top_title);
        mTopBar = getView().findViewById(R.id.topBar);
        mErrorView = getView().findViewById(R.id.error_view);
        mContentView = getView().findViewById(R.id.content_view);
        TextView top_close = getView().findViewById(R.id.top_close);
        TextView top_back = getView().findViewById(R.id.top_back);
        top_back.setOnClickListener(this);
        top_close.setOnClickListener(this);
    }

    public void enableTopBar(boolean enable) {
        enadbleTopBar = enable;
        if (enadbleTopBar && mTopBar.getVisibility() != View.VISIBLE) {
            mTopBar.setVisibility(View.VISIBLE);
        } else if (!enadbleTopBar && mTopBar.getVisibility() != View.GONE) {
            mTopBar.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        webView.setDrawingCacheEnabled(true);
        webView.setWillNotCacheDrawing(false);
        webView.setSaveEnabled(true);

        webView.setBackground(null);
        webView.getRootView().setBackground(null);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        webView.setScrollbarFadingEnabled(true);

        webView.setWebViewClient(webViewClient);
        webView.setWebChromeClient(webChromeClient);

        webView.addJavascriptInterface(new JavascriptInterface(), "javaListener");
        webView.addJavascriptInterface(new JavascriptInterface2(), "BSL");
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                WebView.HitTestResult result = ((WebView) v).getHitTestResult();
                if (result == null) return false;
                int type = result.getType();
                if (type == WebView.HitTestResult.IMAGE_TYPE) {
                    final String imageUrl = result.getExtra();
                    new AlertDialog.Builder(mActivity)
                            .setTitle("提示")
                            .setMessage("下载该图片")
                            .setPositiveButton("下载", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    new SaveImage(imageUrl).execute();
                                }
                            }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();

                }
                return false;
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        initWebSettings();
    }

    private synchronized void initWebSettings() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(webView.getContext().getCacheDir().toString());
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setGeolocationDatabasePath(webView.getContext().getFilesDir().toString());

        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSettings.setLoadsImagesAutomatically(true);
        } else {
            webSettings.setLoadsImagesAutomatically(false);
        }
    }

    //WebViewClient主要帮助WebView处理各种通知、请求事件
    private WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {//页面加载完成
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            if ((url.startsWith("http://") || url.startsWith("https://"))
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //加载完成
                webView.evaluateJavascript("javascript:getColor()", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (!TextUtils.isEmpty(value) && !"null".equals(value)) {
                            try {
                                int color = Color.parseColor(value.replaceAll("\"", ""));
                                Window window = mActivity.getWindow();
                                window.setStatusBarColor(color);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {//页面开始加载
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String
                failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            //6.0以下执行
            //网络未连接
            showErrorPage();
        }

        //处理网页加载失败时
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError
                error) {
            super.onReceivedError(view, request, error);
            //6.0以上执行
            showErrorPage();//显示错误页面
        }
    };

    //WebChromeClient主要辅助WebView处理Javascript的对话框、网站图标、网站title、加载进度等
    private WebChromeClient webChromeClient = new WebChromeClient() {
        //获取网页标题
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (title.contains("404")) {
                showErrorPage();
            } else if (title.length() > 20) {
                title = title.substring(0, 20) + "...";
            }
            top_title.setText(title);
        }

        //加载进度回调
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            mUploadCallbackAboveL = filePathCallback;
            showOptions();
            return true;
        }
    };

    /**
     * 包含拍照和相册选择
     */
    public void showOptions() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mActivity);
        alertDialog.setOnCancelListener(new ReOnCancelListener());
        alertDialog.setTitle("选择");
        alertDialog.setItems(R.array.options,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            if (ContextCompat.checkSelfPermission(mActivity,
                                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                // 申请WRITE_EXTERNAL_STORAGE权限
                                ActivityCompat
                                        .requestPermissions(
                                                mActivity,
                                                new String[]{Manifest.permission.CAMERA},
                                                TYPE_REQUEST_PERMISSION);
                            } else {
                                toCamera();
                            }
                        } else {
                            Intent i = new Intent(
                                    Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);// 调用android的图库
                            startActivityForResult(i,
                                    TYPE_GALLERY);
                        }
                    }
                });
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == TYPE_REQUEST_PERMISSION) {
            toCamera();// 到相机
        }
    }

    private class ReOnCancelListener implements
            DialogInterface.OnCancelListener {

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;
            }
            if (mUploadCallbackAboveL != null) {
                mUploadCallbackAboveL.onReceiveValue(null);
                mUploadCallbackAboveL = null;
            }
        }
    }

    // 请求拍照
    public void toCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);// 调用android的相机
        // 创建一个文件保存图片
        fileUri = getOutputMediaFileUri();
        Log.d("MainActivity", "fileUri=" + fileUri);
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(mActivity, mActivity.getPackageName() + ".fileprovider", new File(fileUri.getPath())));
        startActivityForResult(intent, TYPE_CAMERA);
    }

    private Uri getOutputMediaFileUri() {
        return Uri.fromFile(FileManager.getImgFile(mActivity.getApplicationContext()));
    }

    private void loadingUrl() {
        String url = getArguments().getString("url", "");
        webView.loadUrl(url);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webView.canGoBack() && keyCode == KeyEvent.KEYCODE_BACK) {//点击返回按钮的时候判断有没有上一页
            webView.goBack(); // goBack()表示返回webView的上一页面
            return true;
        }
        return false;
    }

    /**
     * 显示自定义错误提示页面，用一个View覆盖在WebView
     */
    private void showErrorPage() {
        mContentView.setVisibility(View.GONE);
        mErrorView.setVisibility(View.VISIBLE);
        mErrorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mErrorView.setVisibility(View.GONE);
                mContentView.setVisibility(View.VISIBLE);
                webView.reload();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //释放资源
        webView.destroy();
        webView = null;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.top_back) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                mActivity.finish();
            }
        } else if (id == R.id.top_close) {
            mActivity.finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String content = data.getStringExtra(Constant.CODED_CONTENT);
                String callback = data.getStringExtra("callback");
                if (!TextUtils.isEmpty(callback)) {
                    webView.loadUrl("javascript:" + callback + "('" + content + "')");
                }
            }
        } else if (requestCode == TYPE_CAMERA) { // 相册选择
            if (resultCode == Activity.RESULT_OK) {
                onActivityCallBack(true, null);
            } else {
                onActivityCallBacknull();
            }
        } else if (requestCode == TYPE_GALLERY) {// 相册选择
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        onActivityCallBack(false, uri);
                    } else {
                        Toast.makeText(mActivity, "获取数据为空", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                onActivityCallBacknull();
            }
        }
    }

    public void onActivityCallBack(boolean isCamera, Uri uri) {
        if (isCamera) {
            uri = fileUri;
        }
        if (mUploadCallbackAboveL != null) {
            Uri[] uris = new Uri[]{uri};
            mUploadCallbackAboveL.onReceiveValue(uris);
            mUploadCallbackAboveL = null;
        } else if (mUploadMessage != null) {
            mUploadMessage.onReceiveValue(uri);
            mUploadMessage = null;
        } else {
            Toast.makeText(mActivity, "无法获取数据", Toast.LENGTH_LONG).show();
        }
    }

    public void onActivityCallBacknull() {
        if (mUploadCallbackAboveL != null) {
            mUploadCallbackAboveL.onReceiveValue(null);
            mUploadCallbackAboveL = null;
        } else if (mUploadMessage != null) {
            mUploadMessage.onReceiveValue(null);
            mUploadMessage = null;
        }
    }

    private class SaveImage extends AsyncTask<String, Void, String> {
        String imgurl = "";

        public SaveImage(String imageurl) {
            this.imgurl = imageurl;
        }

        @Override
        protected String doInBackground(String... params) {
            String result = "";
            try {
                String sdcard = Environment.getExternalStorageDirectory().toString();
                File file = new File(sdcard + "/Download");
                if (!file.exists()) {
                    file.mkdirs();
                }
                int idx = imgurl.lastIndexOf(".");
                String ext = imgurl.substring(idx);
                file = new File(sdcard + "/Download/" + new Date().getTime() + ext);
                InputStream inputStream = null;
                URL url = new URL(imgurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(20000);
                if (conn.getResponseCode() == 200) {
                    inputStream = conn.getInputStream();
                }
                byte[] buffer = new byte[4096];
                int len = 0;
                FileOutputStream outStream = new FileOutputStream(file);
                while ((len = inputStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len);
                }
                outStream.close();
                if (PhotoUtils.displayToGallery(mActivity, file)) {
                    file.delete();
                    result = "图片已保存至相冊";
                } else {

                    result = "图片已保存至：" + file.getAbsolutePath();
                }
            } catch (FileNotFoundException e) {
                result = "保存失败，缺少文件访问权限";
            } catch (Exception e) {
                result = "保存失败！";
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(mActivity, result, Toast.LENGTH_SHORT).show();
        }
    }


    final class JavascriptInterface {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @android.webkit.JavascriptInterface
        public void setStatuBarColor(String color) {
            Log.d("background-color", color);
            Window window = mActivity.getWindow();
            window.setStatusBarColor(Color.parseColor(color));
        }
    }

    //BSL.Qcode('0','qrcodeCallback')
    final class JavascriptInterface2 {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @android.webkit.JavascriptInterface
        public void Qcode(String num, final String callback) {
            if (num.equals("0")) {
                AndPermission.with(mActivity)
                        .permission(Permission.CAMERA, Permission.READ_EXTERNAL_STORAGE)
                        .onGranted(new Action() {
                            @Override
                            public void onAction(List<String> permissions) {
                                Intent intent = new Intent(mActivity, CaptureActivity.class);
                                intent.putExtra("callback", callback);
                                startActivityForResult(intent, REQUEST_CODE_SCAN);
                            }
                        })
                        .onDenied(new Action() {
                            @Override
                            public void onAction(List<String> permissions) {
                                Uri packageURI = Uri.parse("package:" + mActivity.getPackageName());
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                Toast.makeText(mActivity, "没有权限无法扫描呦", Toast.LENGTH_LONG).show();
                            }
                        }).start();
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @android.webkit.JavascriptInterface
        public void copyText(String text) {
            ClipboardManager cm = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData myClip;
            myClip = ClipData.newPlainText("text", text);
            cm.setPrimaryClip(myClip);
            Toast.makeText(mActivity, "复制成功", Toast.LENGTH_LONG).show();
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @android.webkit.JavascriptInterface
        public String getUserJson() {
            String userString = getArguments().getString("userjson", "");
            TelephonyManager tm = (TelephonyManager) mActivity.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            @SuppressLint({"MissingPermission", "HardwareIds"}) String imei = tm.getDeviceId();
            JSONObject json = new JSONObject();
            json.put("loginkey", userString);
            json.put("imei", imei);
            return json.toJSONString();
        }
    }
}
