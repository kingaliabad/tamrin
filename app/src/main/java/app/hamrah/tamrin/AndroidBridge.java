package app.hamrah.tamrin;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** پل بین جاوااسکریپت صفحه و قابلیت‌های بومی اندروید. */
public class AndroidBridge {

    private final Activity activity;

    AndroidBridge(Activity activity) {
        this.activity = activity;
    }

    /** فهرست داروها را ذخیره و آلارم‌های بومی را دوباره تنظیم می‌کند. */
    @JavascriptInterface
    public void syncMeds(String json) {
        ReminderScheduler.saveAndSchedule(activity.getApplicationContext(), json);
    }

    @JavascriptInterface
    public boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        NotificationManager nm = activity.getSystemService(NotificationManager.class);
        return nm.areNotificationsEnabled();
    }

    @JavascriptInterface
    public void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            activity.runOnUiThread(() -> activity.requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, MainActivity.REQ_NOTIF));
        }
    }

    @JavascriptInterface
    public void openNotificationSettings() {
        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        i.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
        activity.runOnUiThread(() -> {
            try {
                activity.startActivity(i);
            } catch (Exception ignored) {
            }
        });
    }

    @JavascriptInterface
    public void testNotification() {
        ReminderReceiver.show(activity.getApplicationContext(), "همراه تمرین من",
                "اعلان آزمایشی فعال است.", 1);
    }

    /** فایل پشتیبان را در پوشه Downloads گوشی ذخیره می‌کند. */
    @JavascriptInterface
    public boolean saveBackup(String filename, String content) {
        try {
            String safe = filename.replaceAll("[^A-Za-z0-9._-]", "_");
            ContentResolver resolver = activity.getContentResolver();
            ContentValues v = new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME, safe);
            v.put(MediaStore.Downloads.MIME_TYPE, "application/json");
            v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) {
                throw new IllegalStateException("insert failed");
            }
            try (OutputStream os = resolver.openOutputStream(uri)) {
                if (os == null) {
                    throw new IllegalStateException("stream null");
                }
                os.write(content.getBytes(StandardCharsets.UTF_8));
            }
            toast("فایل پشتیبان در پوشه Downloads ذخیره شد.");
            return true;
        } catch (Exception e) {
            toast("ذخیره فایل پشتیبان ناموفق بود.");
            return false;
        }
    }

    private void toast(String msg) {
        activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_LONG).show());
    }
}
