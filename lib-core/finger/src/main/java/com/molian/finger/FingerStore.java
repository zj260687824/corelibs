package com.molian.finger;

import android.os.Environment;
import android.text.TextUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FingerStore {
    static File fingerDir;
    static String ffName = "/finger.txt";


    static {
        fingerDir = Environment.getExternalStoragePublicDirectory("mldata");
    }

    public static String getFingerId() {
        File file = new File(fingerDir.getPath() + ffName);
        if (!file.exists()) {
            return "";
        }
        try {
            FileInputStream fis = new FileInputStream(file);//打开文件输入流
            StringBuffer sBuffer = new StringBuffer();
            DataInputStream dataIO = new DataInputStream(fis);//读取文件数据流
            String strLine = null;
            while ((strLine = dataIO.readLine()) != null) {//通过readline按行读取
                sBuffer.append(strLine);//strLine就是一行的内容
            }
            String value = sBuffer.toString();
            dataIO.close();
            fis.close();
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static void setFingerId(String fingerId) {
        writeTxtToFile(fingerId, fingerDir.getPath(), ffName);
    }

    // 将字符串写入到文本文件中
    private static void writeTxtToFile(String strcontent, String filePath, String fileName) {
        makeFilePath(filePath, fileName);
        String strFilePath = filePath + fileName;
        // 每次写入时，都换行写
        String strContent = strcontent;
        try {
            File file = new File(strFilePath);
            //先刪除文件再創建
            if (file.exists()) {
                file.delete();
            }
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            if (TextUtils.isEmpty(strContent)) {
                return;
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //生成文件
    private static File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    //生成文件夹
    private static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String md5(String string) {
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
}
