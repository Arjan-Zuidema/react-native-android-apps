// ReactNativeAndroidAppsModule.java

package com.reactlibrary;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ReactNativeAndroidAppsModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public ReactNativeAndroidAppsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "ReactNativeAndroidApps";
    }

    @ReactMethod
    public void getApps(ReadableArray apps, final Callback callback) {
        class GetAppsTask implements Runnable {
            private final ReactApplicationContext reactContext;
            private final ArrayList<Object> appList;

            GetAppsTask(final ReactApplicationContext reactContext, ReadableArray appList) {
                this.reactContext = reactContext;
                this.appList = appList.toArrayList();
            }

            public void run() {
                WritableArray list = Arguments.createArray();
                
                try {
                    PackageManager packageManager = this.reactContext.getPackageManager();
                    List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(0);
                    for (int i = 0; i < packageInfoList.size(); i++) {
                        PackageInfo packageInfo = packageInfoList.get(i);

                        if(!shouldFilterApp(appList, packageInfo.packageName)) {
                            WritableMap applicationInfo = Arguments.createMap();
                            
                            applicationInfo.putString("applicationName", packageInfo.applicationInfo.loadLabel(packageManager).toString());
                            applicationInfo.putString("packageName", packageInfo.packageName);
                            applicationInfo.putString("versionName", packageInfo.versionName);
                            applicationInfo.putDouble("versionCode", packageInfo.versionCode);

                            try {
                                Drawable appIcon = packageManager.getApplicationIcon(packageInfo.applicationInfo);
                                applicationInfo.putString("iconData", base64FromBitmap(bitmapFromDrawable(appIcon)));
                            } catch (Exception e) {
                                applicationInfo.putString("iconError", e.getMessage());
                            }

                            list.pushMap(applicationInfo);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                callback.invoke(list);
            }

            private boolean shouldFilterApp(ArrayList<Object> apps, String app) {
                if(apps.size() == 0) {
                    return false;
                }
        
                for(int i = 0; i < apps.size(); i++) {
                    String appName = apps.get(i).toString();
                    if(appName.equals(app)) {
                        return false;
                    }
                }
        
                return true;
            }
        
            private String base64FromBitmap(Bitmap bitmap) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        
                return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
            }
        
            private Bitmap bitmapFromDrawable(Drawable drawable) throws Exception {
                if (drawable instanceof BitmapDrawable) {
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                    if (bitmapDrawable.getBitmap() != null) {
                        return bitmapDrawable.getBitmap();
                    }
                }
        
                if (drawable instanceof AdaptiveIconDrawable) {
                    Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);
                    return bitmap;
                }
        
                throw new Exception("Invalid class " + drawable.getClass().getSimpleName());
            }
        }

        Thread thread = new Thread(new GetAppsTask(this.reactContext, apps));
        thread.start();
    }
}
