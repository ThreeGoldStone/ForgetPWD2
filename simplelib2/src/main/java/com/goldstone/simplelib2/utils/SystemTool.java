/*
 * Copyright (c) 2014, KJFrameForAndroid 张涛 (kymjs123@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.goldstone.simplelib2.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 系统信息工具包<br>
 * <p>
 * <b>创建时间</b> 2014-8-14
 *
 * @author kymjs(kymjs123@gmail.com)
 * @version 1.1 <br>
 *          修改时间:2015年6月24日 17:19:03 by DJL
 */
public final class SystemTool {
    /**
     * 指定格式返回当前系统时间
     */
    @SuppressLint("SimpleDateFormat")
    public static String getDataTime(String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(new Date());
    }

    /**
     * 返回当前系统时间(格式以HH:mm形式)
     */
    public static String getDataTime() {
        return getDataTime("HH:mm");
    }

    /**
     * 获取手机IMEI码
     */
    @SuppressLint("MissingPermission")
    public static String getPhoneIMEI(Context cxt) {
        TelephonyManager tm = (TelephonyManager) cxt.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(cxt, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return tm.getDeviceId();
        }
        return null;
    }

    /**
     * 获取手机系统SDK版本
     *
     * @return 如API 17 则返回 17
     */
    public static int getSDKVersion() {
        return android.os.Build.VERSION.SDK_INT;
    }

    /**
     * 获取系统版本
     *
     * @return 形如2.3.3
     */
    public static String getSystemVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * * 复制文件
     *
     * @param sourceFilePath
     * @param targetFilePath
     * @param handler
     * @param baseCode       handler
     *                       返回的code=baseCode*100+{31（开始）/32（进度,handler.obtainMessage
     *                       (baseCode * 100 + 32, totalsize,
     *                       bytesum).sendToTarget()）/33（完成 handler.obtainMessage(baseCode
     *                       * 100 + 33, toFile).sendToTarget()）/34（失败）}
     */
    public static void copyFile(final String sourceFilePath, final String targetFilePath,
                                final Handler handler, final int baseCode) {
        new Thread() {
            public void run() {
                File tf = new File(targetFilePath);
                boolean mkdirs = false;
                File parentFile = tf.getParentFile();
                if (!parentFile.exists()) {
                    mkdirs = parentFile.mkdirs();
                }
                if (mkdirs && !tf.exists()) {
                    try {
                        tf.createNewFile();
                    } catch (IOException e) {
                        handler.sendEmptyMessage(baseCode * 100 + 34);
                    }
                }
                FileOutputStream fs = null;
                InputStream inStream = null;
                try {
                    int bytesum = 0;
                    int byteread = -1;
                    File oldfile = new File(sourceFilePath);
                    if (oldfile.exists()) { // 文件存在时
                        int totalsize = (int) oldfile.length();
                        inStream = new FileInputStream(sourceFilePath); // 读入原文件
                        fs = new FileOutputStream(tf);
                        byte[] buffer = new byte[1024 * 10];
                        handler.sendEmptyMessage(baseCode * 100 + 31);
                        while ((byteread = inStream.read(buffer)) != -1) {
                            bytesum += byteread; // 字节数 文件大小
                            System.out.println(bytesum);
                            fs.write(buffer, 0, byteread);
                            handler.obtainMessage(baseCode * 100 + 32, totalsize, bytesum)
                                    .sendToTarget();
                        }
                        inStream.close();
                        fs.flush();
                        fs.close();
                        handler.obtainMessage(baseCode * 100 + 33, tf).sendToTarget();
                    }
                } catch (Exception e) {
                    handler.sendEmptyMessage(baseCode * 100 + 34);
                    System.out.println("复制单个文件操作出错");
                    e.printStackTrace();

                } finally {
                    try {
                        inStream.close();
                        fs.flush();
                        fs.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }

            ;
        }.start();

    }

    private static ByteArrayOutputStream ObjOuput(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        return bos;
    }

    /**
     * 将一个Serializable对象缓存为一个文件
     *
     * @param obj
     * @param cachePath
     * @return
     */
    public static <T extends Serializable> boolean cacheObject(T obj, String cachePath) {
        try {
            ByteArrayOutputStream objOuput = ObjOuput(obj);
            File cacheFile = new File(cachePath);
            File parentFile = cacheFile.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (!cacheFile.exists()) {
                cacheFile.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
            fileOutputStream.write(objOuput.toByteArray());
            objOuput.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 读取缓存的文件对象
     *
     * @param cachePath
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCacheObject(String cachePath) {
        ObjectInputStream ois = null;
        T result = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(new File(cachePath)));
            result = (T) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 调用系统发送短信
     */
    public static void sendSMS(Context cxt, String smsBody) {
        Uri smsToUri = Uri.parse("smsto:");
        Intent intent = new Intent(Intent.ACTION_SENDTO, smsToUri);
        intent.putExtra("sms_body", smsBody);
        cxt.startActivity(intent);
    }

    /**
     * 判断网络是否连接
     */
    public static boolean checkNet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null;// 网络是否连接
    }

    // /**
    // * 仅wifi联网功能是否开启
    // */
    // public static boolean checkOnlyWifi(Context context) {
    // if (PreferenceHelper.readBoolean(context,
    // KJConfig.SETTING_FILE, KJConfig.ONLY_WIFI)) {
    // return isWiFi(context);
    // } else {
    // return true;
    // }
    // }

    /**
     * 判断是否为wifi联网
     */
    public static boolean isWiFi(Context cxt) {
        ConnectivityManager cm = (ConnectivityManager) cxt
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        // wifi的状态：ConnectivityManager.TYPE_WIFI
        // 3G的状态：ConnectivityManager.TYPE_MOBILE
        @SuppressLint("MissingPermission") State state = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        return State.CONNECTED == state;
    }

    /**
     * 隐藏系统键盘
     * <p>
     * <br>
     * <b>警告</b> 必须是确定键盘显示时才能调用
     */
    public static void hideKeyBoard(Activity aty) {
        ((InputMethodManager) aty.getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(aty.getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * 判断当前应用程序是否后台运行
     */
    public static boolean isBackground(Context context) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                    // 后台运行
                    return true;
                } else {
                    // 前台运行
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 判断手机是否处理睡眠
     */
    public static boolean isSleeping(Context context) {
        KeyguardManager kgMgr = (KeyguardManager) context
                .getSystemService(Context.KEYGUARD_SERVICE);
        boolean isSleeping = kgMgr.inKeyguardRestrictedInputMode();
        return isSleeping;
    }

    /**
     * 安装apk
     *
     * @param context
     * @param file
     */
    public static void installApk(Context context, File file) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setType("application/vnd.android.package-archive");
        intent.setData(Uri.fromFile(file));
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 获取当前应用程序的版本号
     */
    public static String getAppVersion(Context context) {
        String version = "0";
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }

    /**
     * 回到home，后台运行
     */
    public static void goHome(Context context) {
        Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
        mHomeIntent.addCategory(Intent.CATEGORY_HOME);
        mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(mHomeIntent);
    }

    /**
     * 获取应用签名
     *
     * @param context
     * @param pkgName
     */
    public static String getSign(Context context, String pkgName) {
        try {
            PackageInfo pis = context.getPackageManager().getPackageInfo(pkgName,
                    PackageManager.GET_SIGNATURES);
            return hexdigest(pis.signatures[0].toByteArray());
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将签名字符串转换成需要的32位签名
     */
    private static String hexdigest(byte[] paramArrayOfByte) {
        final char[] hexDigits = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 97, 98, 99, 100, 101,
                102};
        try {
            MessageDigest localMessageDigest = MessageDigest.getInstance("MD5");
            localMessageDigest.update(paramArrayOfByte);
            byte[] arrayOfByte = localMessageDigest.digest();
            char[] arrayOfChar = new char[32];
            for (int i = 0, j = 0; ; i++, j++) {
                if (i >= 16) {
                    return new String(arrayOfChar);
                }
                int k = arrayOfByte[i];
                arrayOfChar[j] = hexDigits[(0xF & k >>> 4)];
                arrayOfChar[++j] = hexDigits[(k & 0xF)];
            }
        } catch (Exception e) {
        }
        return "";
    }

    /**
     * 获取设备的可用内存大小
     *
     * @param cxt 应用上下文对象context
     * @return 当前内存大小
     */
    public static int getDeviceUsableMemory(Context cxt) {
        ActivityManager am = (ActivityManager) cxt.getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo mi = new MemoryInfo();
        am.getMemoryInfo(mi);
        // 返回当前系统的可用内存
        return (int) (mi.availMem / (1024 * 1024));
    }

    /**
     * 清理后台进程与服务
     *
     * @param cxt 应用上下文对象context
     * @return 被清理的数量
     */
    @SuppressLint("MissingPermission")
    public static int gc(Context cxt) {
        long i = getDeviceUsableMemory(cxt);
        int count = 0; // 清理掉的进程数
        ActivityManager am = (ActivityManager) cxt.getSystemService(Context.ACTIVITY_SERVICE);
        // 获取正在运行的service列表
        List<RunningServiceInfo> serviceList = am.getRunningServices(100);
        if (serviceList != null)
            for (RunningServiceInfo service : serviceList) {
                if (service.pid == android.os.Process.myPid())
                    continue;
                try {
                    android.os.Process.killProcess(service.pid);
                    count++;
                } catch (Exception e) {
                    e.getStackTrace();
                    continue;
                }
            }

        // 获取正在运行的进程列表
        List<RunningAppProcessInfo> processList = am.getRunningAppProcesses();
        if (processList != null)
            for (RunningAppProcessInfo process : processList) {
                // 一般数值大于RunningAppProcessInfo.IMPORTANCE_SERVICE的进程都长时间没用或者空进程了
                // 一般数值大于RunningAppProcessInfo.IMPORTANCE_VISIBLE的进程都是非可见进程，也就是在后台运行着
                if (process.importance > RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    // pkgList 得到该进程下运行的包名
                    String[] pkgList = process.pkgList;
                    for (String pkgName : pkgList) {
                        try {
                            am.killBackgroundProcesses(pkgName);
                            count++;
                        } catch (Exception e) { // 防止意外发生
                            e.getStackTrace();
                            continue;
                        }
                    }
                }
            }
        return count;
    }

    public static DisplayMetrics getScreenSize(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics;
    }
}