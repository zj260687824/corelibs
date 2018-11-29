package com.molian.web;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
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

public class WebActivity extends AppCompatActivity implements View.OnClickListener {
    private WebView webView;
    private ProgressBar progressBar;
    private TextView top_title;
    private View mErrorView; //加载错误的视图
    private FrameLayout webParentView;
    private View mTopBar;
    private boolean enadbleTopBar = false;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        initView();
        init();
        loadingUrl();
    }

    private void initView() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        top_title = findViewById(R.id.top_title);
        mTopBar = findViewById(R.id.topBar);
        TextView top_close = findViewById(R.id.top_close);
        TextView top_back = findViewById(R.id.top_back);
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

    private void loadingUrl() {
        String url = getIntent().getStringExtra("url");
        webView.loadUrl(url);
    }

    /**
     * 供外部使用
     *
     * @param url      加载的地址
     * @param activity activity
     */
    public static void startActivity(String url, Activity activity) {
        Intent intent = new Intent();
        intent.putExtra("url", url);
        intent.setClass(activity.getApplicationContext(), WebActivity.class);
        activity.startActivity(intent);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        webParentView = (FrameLayout) webView.getParent(); //获取父容器
        initErrorPage();//初始化自定义页面
        webView.setWebChromeClient(webChromeClient);
        webView.setWebViewClient(webViewClient);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);//允许使用js
        /**
         * LOAD_CACHE_ONLY: 不使用网络，只读取本地缓存数据
         * LOAD_DEFAULT: （默认）根据cache-control决定是否从网络上取数据。
         * LOAD_NO_CACHE: 不使用缓存，只从网络获取数据.
         * LOAD_CACHE_ELSE_NETWORK，只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据。
         */
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);//不使用缓存，只从网络获取数据.
        //支持屏幕缩放
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webView.addJavascriptInterface(new JavascriptInterface(), "javaListener");
    }

    //WebViewClient主要帮助WebView处理各种通知、请求事件
    private WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {//页面加载完成
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
//            String js = "javascript:function hideTitle(){" +
//                    "document.getElementsByTagName('body')[0].firstElementChild.firstElementChild.style.display='none'" +
//                    "}";
//            //隐藏h5中顶部返回样式
//            view.loadUrl(js);
//            view.loadUrl("javascript:hideTitle();");


        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {//页面开始加载
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.equals("http://www.google.com/")) {
                Toast.makeText(WebActivity.this, "国内不能访问google,拦截该url", Toast.LENGTH_LONG).show();
                return true;//表示我已经处理过了
            }
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            //6.0以下执行
            //网络未连接
            showErrorPage();
        }

        //处理网页加载失败时
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
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
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webView.canGoBack() && keyCode == KeyEvent.KEYCODE_BACK) {//点击返回按钮的时候判断有没有上一页
            webView.goBack(); // goBack()表示返回webView的上一页面
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 显示自定义错误提示页面，用一个View覆盖在WebView
     */
    private void showErrorPage() {
        webParentView.removeAllViews(); //移除加载网页错误时，默认的提示信息
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        webParentView.addView(mErrorView, 0, layoutParams); //添加自定义的错误提示的View
        TextView reload_tv = mErrorView.findViewById(R.id.reload_tv);
        reload_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingUrl();
            }
        });
    }

    /***
     * 显示加载失败时自定义的网页
     */
    private void initErrorPage() {
        if (mErrorView == null) {
            mErrorView = View.inflate(this, R.layout.layout_load_error, null);
        }
    }

    @Override
    protected void onDestroy() {
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
                finish();
            }
        } else if (id == R.id.top_close) {
            finish();
        }
    }

    final class JavascriptInterface {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @android.webkit.JavascriptInterface
        public void setStatuBarColor(String color) {
            Log.d("background-color", color);
            Window window = getWindow();
            window.setStatusBarColor(Color.parseColor(color));
        }
    }

}
