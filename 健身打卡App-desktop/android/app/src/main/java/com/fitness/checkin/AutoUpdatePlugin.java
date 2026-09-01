package com.fitness.checkin;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@CapacitorPlugin(name = "AutoUpdate")
public class AutoUpdatePlugin extends Plugin {

    /**
     * 下载传入的 APK 并在应用内唤起系统安装界面。
     * 首次使用时若未授权“安装未知应用”，会跳转到系统设置引导用户开启。
     */
    @PluginMethod
    public void installApk(PluginCall call) {
        String url = call.getString("url");
        if (url == null || url.isEmpty()) {
            call.reject("缺少下载地址");
            return;
        }
        new Thread(() -> {
            try {
                final File apk = download(url);
                getActivity().runOnUiThread(() -> install(call, apk));
            } catch (Exception e) {
                call.reject("下载失败：" + e.getMessage(), e);
            }
        }).start();
    }

    private File download(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        c.setInstanceFollowRedirects(true);
        c.connect();
        if (c.getResponseCode() >= 400) {
            throw new Exception("HTTP " + c.getResponseCode());
        }
        long total = c.getContentLengthLong();
        File dir = new File(getContext().getCacheDir(), "update");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("无法创建缓存目录");
        }
        File out = new File(dir, "update.apk");
        try (InputStream is = c.getInputStream();
             FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int n;
            long received = 0;
            long lastEmit = 0;
            while ((n = is.read(buf)) != -1) {
                fos.write(buf, 0, n);
                received += n;
                long now = System.currentTimeMillis();
                // 节流：每 150ms 上报一次，避免过度打断主线程
                if (now - lastEmit > 150 || received == total) {
                    lastEmit = now;
                    final long r = received;
                    getActivity().runOnUiThread(() -> emitProgress(r, total));
                }
            }
        }
        emitProgress(total <= 0 ? -1 : total, total);
        return out;
    }

    private void emitProgress(long received, long total) {
        int percent;
        if (total > 0) {
            percent = (int) (received * 100 / total);
            if (percent > 100) percent = 100;
        } else {
            percent = -1; // 未知总大小，仅提示已下载字节数
        }
        JSObject data = new JSObject();
        data.put("received", received);
        data.put("total", total);
        data.put("percent", percent);
        notifyListeners("downloadProgress", data, true);
    }

    private void install(PluginCall call, File apk) {
        try {
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Context ctx = getContext();
                uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", apk);
            } else {
                uri = Uri.fromFile(apk);
            }
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/vnd.android.package-archive");
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(i);
            call.resolve(new JSObject().put("started", true));
        } catch (ActivityNotFoundException e) {
            guideUnknownSources();
            call.reject("未开启未知来源安装", e);
        } catch (Exception e) {
            call.reject("安装启动失败：" + e.getMessage(), e);
        }
    }

    private void guideUnknownSources() {
        try {
            Intent s;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                s = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getContext().getPackageName()));
            } else {
                s = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            }
            s.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(s);
        } catch (ActivityNotFoundException ignored) {
        }
    }
}