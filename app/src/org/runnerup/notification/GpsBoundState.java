package org.runnerup.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import org.runnerup.R;
import org.runnerup.util.Constants;
import org.runnerup.view.MainLayout;

@TargetApi(Build.VERSION_CODES.FROYO)
public class GpsBoundState implements NotificationState {
    private final Notification notification;

    public GpsBoundState(Context context) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        Intent i = new Intent(context, MainLayout.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        i.putExtra(Constants.Intents.FROM_NOTIFICATION, true);
        Intent intent = new Intent(Constants.Intents.START_STOP);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.drawable.ic_tab_main,"Start",pendingIntent);
        PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);

        builder.setContentIntent(pi);
        builder.setContentTitle(context.getString(R.string.activity_ready));
        builder.setContentText(context.getString(R.string.ready_to_start_running));
        builder.setSmallIcon(R.drawable.icon);
        builder.setOnlyAlertOnce(true);
        builder.setAutoCancel(true);
        notification = builder.build();
    }

    @Override
    public Notification createNotification() {
        return notification;
    }
}
