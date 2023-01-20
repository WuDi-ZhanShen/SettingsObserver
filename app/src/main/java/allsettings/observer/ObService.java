package allsettings.observer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ObService extends Service {




    @Override
    public IBinder onBind(Intent intent) {
        return new MsgBinder();
    }

    public class MsgBinder extends Binder {

        public ObService getService() {
            return ObService.this;
        }
    }

    public interface ChangeListener {
        void onChange(String[] data);
    }

    ChangeListener listener;

    Notification.Builder notification;
    NotificationManager systemService;
    List<Uri> uriBlackList = new ArrayList<>();


    public void setChangeListener(ChangeListener changeListener) {
        this.listener = changeListener;
    }

    class SettingsValueChangeContentObserver extends ContentObserver {

        public SettingsValueChangeContentObserver() {
            super(new Handler());
            uriBlackList.add(Uri.parse("content://settings/system/launcher_state"));
            uriBlackList.add(Uri.parse("content://settings/system/count_for_mi_connect"));
            uriBlackList.add(Uri.parse("content://settings/system/screen_brightness"));
            uriBlackList.add(Uri.parse("content://settings/system/contrast_alpha"));
            uriBlackList.add(Uri.parse("content://settings/system/peak_refresh_rate"));
            uriBlackList.add(Uri.parse("content://settings/secure/freeform_timestamps"));
            uriBlackList.add(Uri.parse("content://settings/secure/freeform_window_state"));
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (!uriBlackList.contains(uri)) {
                Log.w("mContentObs",uri.toString());
                String uristr = uri.toString().replace("content://settings/", "");
                String namespace = Pattern.compile("/").split(uristr)[0];
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                while (cursor != null && cursor.moveToNext()) {
                    String name = cursor.getString(1);
                    String value = cursor.getString(2);
                    listener.onChange(new String[]{namespace, name, value, "0"});
                    String inline = "类别：" + namespace + ",名称：" + name + ",值：" + value;
                    Toast.makeText(ObService.this, inline, Toast.LENGTH_SHORT).show();
                    notification.setContentText(inline).setContentTitle("监测到以下变动：");
                    systemService.notify(1, notification.build());
                }
                assert cursor != null;
                cursor.close();
            }

        }
    }

    private SettingsValueChangeContentObserver mContentOb;

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "开始监测", Toast.LENGTH_SHORT).show();


        mContentOb = new SettingsValueChangeContentObserver();
        getContentResolver().registerContentObserver(Uri.parse("content://settings/"), true, mContentOb);


        notification = new Notification.Builder(getApplication()).setAutoCancel(true).
                setContentText("点击查看历史变动").
                setContentTitle("监测中...").
                setWhen(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notification.
                    addAction(android.R.drawable.ic_delete, "复制本次变动", PendingIntent.getBroadcast(this, 0, new Intent("intent.COPY"), PendingIntent.FLAG_IMMUTABLE)).
                    addAction(android.R.drawable.ic_delete, "退出", PendingIntent.getBroadcast(this, 0, new Intent("intent.EXIT"), PendingIntent.FLAG_IMMUTABLE)).
                    setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE)).
                    setSmallIcon(Icon.createWithResource(this, R.drawable.icon));

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("Ob", "Ob", NotificationManager.IMPORTANCE_DEFAULT);

            notificationChannel.enableLights(false);

            notificationChannel.setShowBadge(true);//是否显示角标

            systemService = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            systemService.createNotificationChannel(notificationChannel);
            notification.setChannelId("Ob");
        }
        startForeground(1, notification.build());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mContentOb);

        Toast.makeText(ObService.this, "停止监测", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


}
