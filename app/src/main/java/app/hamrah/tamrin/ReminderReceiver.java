package app.hamrah.tamrin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

public class ReminderReceiver extends BroadcastReceiver {

    static final String CHANNEL = "med_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra("id", -1);
        if (id < 0) {
            return;
        }
        JSONObject med = ReminderScheduler.find(context, id);
        if (med == null) {
            return; // دارو حذف شده است
        }
        // اول آلارم فردا را تنظیم می‌کنیم تا حتی با خطا زنجیره قطع نشود
        ReminderScheduler.scheduleOne(context, id, med.optString("time"));

        if (ReminderScheduler.today().equals(med.optString("doneDate"))) {
            return; // امروز «مصرف شد» ثبت شده است
        }
        String text = "زمان ثبت‌شده برای " + med.optString("name")
                + " رسیده است. دستور نسخه را بررسی کن.";
        show(context, "یادآوری دارو", text, Long.hashCode(id));
    }

    static void show(Context c, String title, String text, int notificationId) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL, "یادآور دارو", NotificationManager.IMPORTANCE_HIGH));

        PendingIntent open = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build();
        nm.notify(notificationId, n);
    }
}
