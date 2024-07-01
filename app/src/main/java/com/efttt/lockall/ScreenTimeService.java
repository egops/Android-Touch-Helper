package com.efttt.lockall;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Notification;
import androidx.core.app.NotificationCompat;

import java.lang.ref.WeakReference;

public class ScreenTimeService extends Service {

    private static long CHECK_INTERVAL = 1000; // 检查间隔1秒
    public static long MAX_SCREEN_ON_TIME = 10 * 1000;
    public static long REST_TIME = 10 * 1000;
    private ScreenTimeReceiver screenTimeReceiver;
    public static WeakReference<ScreenTimeService> sServiceRef;

    public static TimerHelper Helper = new TimerHelper();
    private Handler handler;
    private Runnable restCheckRunnable;
    private Runnable timeoutCheckRunnable;

    private static String TAG = "ScreenTimeService";
    private String CHANNEL_ID = "LockAll_CHNNEL1";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Started");
        Toast.makeText(this, "ScreenTimeService Started", Toast.LENGTH_SHORT).show();

        // Create notification channel for foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Screen Time Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Time Service")
                .setContentText("Tracking screen on/off events")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        // Return START_STICKY to make the service restart if it gets terminated
        return START_STICKY;
    }

    public void showUnlockDialog() {
        // Create and display the dialog
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_countdown_dialog);
        dialog.setCancelable(false); // 设置是否可以通过点击空白区域或返回键关闭弹窗

        // 初始化弹窗中的控件
        TextView titleTextView = dialog.findViewById(R.id.dialog_title);
        titleTextView.setText("Countdown Dialog");

        final ProgressBar progressBar = dialog.findViewById(R.id.progress_bar);
        final TextView timeRemainingTextView = dialog.findViewById(R.id.time_remaining);

        // 设置倒计时时间和间隔
        long totalTime = REST_TIME + Helper.getBeginTime() - System.currentTimeMillis(); // 总时间60秒
        long interval = 1000; // 每秒更新一次

        // 创建倒计时器
        CountDownTimer countDownTimer = new CountDownTimer(totalTime, interval) {
            @Override
            public void onTick(long millisUntilFinished) {
                // 更新进度条
                int progress = (int) (millisUntilFinished / (float) totalTime * 100);
                progressBar.setProgress(progress);

                // 更新剩余时间显示
                long secondsRemaining = millisUntilFinished / 1000;
                long minutes = secondsRemaining / 60;
                long seconds = secondsRemaining % 60;
                timeRemainingTextView.setText(String.format("%d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                // 倒计时结束时的处理
                progressBar.setProgress(0);
                timeRemainingTextView.setText("0:00");
                // 在这里可以添加倒计时结束后的操作
                dialog.dismiss(); // 关闭弹窗
            }
        };

        // 开始倒计时
        countDownTimer.start();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }

        // 拦截返回键事件，防止关闭弹窗
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_UP) {
                    // 拦截返回键事件，不做任何操作
                    return true;
                }
                // 其他按键事件继续处理
                return false;
            }
        });

        dialog.show();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("ScreenTimeService","creating ScreenTimeService.....");
        sServiceRef = new WeakReference<>(this);
        screenTimeReceiver = new ScreenTimeReceiver();
        handler = new Handler();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        registerReceiver(screenTimeReceiver, filter);

        Log.d("ScreenTimeService", "ScreenTimeReceiver registered");

        startHandler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenTimeReceiver);
        Log.d("ScreenTimeService", "ScreenTimeReceiver unregistered");
        stopHandler();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    public void startHandler() {
        restCheckRunnable = new Runnable() {
            @Override
            public void run() {
                long currentM = System.currentTimeMillis();
                if (ScreenTimeReceiver.isScreenOn() && !Helper.isShowing()) {
                    // 亮屏处理
                    if (Helper.getLastOn() == 0 || (currentM - Helper.getLastOn() > 20 * 60 * 1000)) {
                        Helper.setLastOn(currentM);
                    } else {
                        if (currentM - Helper.getLastOn() > MAX_SCREEN_ON_TIME) {
                            Log.d("ScreenTimeService", "Screen has been on for too long!");
                            // 这里可以添加屏幕亮屏超时的处理逻辑
                            Helper.setShowing(true);
                            Helper.setBeginTime(currentM);
                            showUnlockDialog();
                        }
                    }
                }
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };

        timeoutCheckRunnable = new Runnable() {
            @Override
            public void run() {
                long currentM = System.currentTimeMillis();
                if (Helper.isShowing()) {
                    if (currentM - Helper.getBeginTime() < REST_TIME) {
                        // 正在休息倒计时

                    } else {
                        Helper.setBeginTime(0);
                        Helper.setShowing(false);
                        Helper.setLastOn(0);
                    }
                }
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };

        handler.post(restCheckRunnable);
        handler.post(timeoutCheckRunnable);
    }

    public void stopHandler() {
        handler.removeCallbacks(restCheckRunnable);
        handler.removeCallbacks(timeoutCheckRunnable);
    }
}