package app.hamrah.tamrin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * آلارم روزانه برای هر دارو. از setAlarmClock استفاده می‌شود تا بدون مجوز ویژه،
 * دقیق و حتی در حالت Doze اجرا شود.
 */
final class ReminderScheduler {

    private static final String PREFS = "reminders";
    private static final String KEY = "meds";

    private ReminderScheduler() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void saveAndSchedule(Context c, String json) {
        JSONArray fresh;
        try {
            fresh = new JSONArray(json);
        } catch (Exception e) {
            return;
        }
        cancelAll(c);
        prefs(c).edit().putString(KEY, fresh.toString()).commit();
        scheduleAll(c);
    }

    static JSONArray load(Context c) {
        try {
            return new JSONArray(prefs(c).getString(KEY, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    static JSONObject find(Context c, long id) {
        JSONArray arr = load(c);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && o.optLong("id") == id) {
                return o;
            }
        }
        return null;
    }

    static void cancelAll(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        JSONArray arr = load(c);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) {
                am.cancel(pendingIntent(c, o.optLong("id")));
            }
        }
    }

    static void scheduleAll(Context c) {
        JSONArray arr = load(c);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) {
                scheduleOne(c, o.optLong("id"), o.optString("time"));
            }
        }
    }

    static void scheduleOne(Context c, long id, String time) {
        long when = nextTrigger(time);
        if (when < 0) {
            return;
        }
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent show = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, show), pendingIntent(c, id));
    }

    private static PendingIntent pendingIntent(Context c, long id) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setData(Uri.parse("hamrah://med/" + id));
        i.putExtra("id", id);
        return PendingIntent.getBroadcast(c, Long.hashCode(id), i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** زمان بعدی وقوع ساعت HH:mm به میلی‌ثانیه؛ در صورت نامعتبر بودن ساعت -1. */
    static long nextTrigger(String time) {
        try {
            LocalTime t = LocalTime.parse(time);
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime next = now.with(t);
            if (!next.isAfter(now)) {
                next = next.plusDays(1);
            }
            return next.toInstant().toEpochMilli();
        } catch (Exception e) {
            return -1;
        }
    }

    /** تاریخ امروز به‌صورت yyyy-MM-dd با ساعت محلی (هم‌فرمت با جاوااسکریپت برنامه). */
    static String today() {
        return LocalDate.now().toString();
    }
}
