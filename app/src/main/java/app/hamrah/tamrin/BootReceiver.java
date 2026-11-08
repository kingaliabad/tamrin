package app.hamrah.tamrin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** بعد از ریبوت، تغییر ساعت/منطقه زمانی یا به‌روزرسانی برنامه، آلارم‌ها را دوباره می‌سازد. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ReminderScheduler.cancelAll(context);
        ReminderScheduler.scheduleAll(context);
    }
}
