package com.molian.web.utils;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.TreeSet;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiDal {
    static String BASE_URL = "";
    static final String ACCESS_SECRET = "mochain_access";

    public static void init(String url) {
        BASE_URL = url;
//        http://47.91.173.119:8899/api
    }

    /**
     * mei登录
     *
     * @return
     */
    public static JSONObject loginForMei(String imei, String fingerId) throws IOException {
        String mei = md5(imei);
        String method = BASE_URL + "/user/userlogins";
        JSONObject body = new JSONObject();
        body.put("timestamp", System.currentTimeMillis() / 1000 + "");
        body.put("imei", mei);
        body.put("fingerprint", fingerId);
        JSONObject result = postJsonBody(body.toJSONString(), method);
        if (result == null)
            return null;
        return result;
    }

    /**
     * post jsonBody
     *
     * @param json 提交的Json内容
     * @param url  请求url
     * @return 具体的内容
     * @throws IOException
     */
    private static JSONObject postJsonBody(String json, String url) throws IOException {
        String mediaJson = completeSign(json);
        return post(url, mediaJson, "application/json; charset=utf-8");
    }

    /**
     * 通过提交的json 生成sign （服务器加密验证用）
     *
     * @param json 提交内容
     * @return 返回加入sing字段的json
     */
    private static String completeSign(String json) {
        JSONObject mcJson = JSON.parseObject(json);
        Set<String> keys = new TreeSet<>();
        keys.addAll(mcJson.keySet());
        StringBuilder temp = new StringBuilder();
        for (String str : keys) {
            temp.append("&").append(str).append("=").append(String.valueOf(mcJson.get(str)));
        }
        temp.append("&").append("AccessKey").append("=").append(ACCESS_SECRET);
        String sign = md5(temp.toString()).toUpperCase();
        mcJson.put("sign", sign);
        return mcJson.toJSONString();
    }

    private static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * post 公用方法
     *
     * @param url          请求url
     * @param mediaContent 提交内容
     * @param mediaType    提交类型
     * @return 具体的内容
     * @throws IOException
     */
    private static JSONObject post(String url, String mediaContent, String mediaType) throws IOException {
        MediaType MEDIA_TYPE_NORAML_FORM = MediaType.parse(mediaType);
        RequestBody requestBody = RequestBody.create(MEDIA_TYPE_NORAML_FORM, mediaContent);
        Request requestPost = new Request.Builder().url(url).post(requestBody).build();
        Response response = OkHttpUtil.getOkHttpClient().newCall(requestPost).execute();
        JSONObject result = null;
        if (response.isSuccessful() && response.body() != null) {
            String bodyString = response.body().string();
            result = JSON.parseObject(bodyString);
        }
        return result;
    }
}
