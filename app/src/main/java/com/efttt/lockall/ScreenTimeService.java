package com.efttt.lockall;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.lang.ref.WeakReference;

public class ScreenTimeService extends Service {
    public static boolean running = false;
    public static boolean ENABLED = true;
    public static long CHECK_INTERVAL = 1000; // 检查间隔1秒
    public static long MAX_USE_TIME = 40 * 60 * 1000;
    public static long REST_TIME = 20 * 60 * 1000;

    private ScreenTimeReceiver screenTimeReceiver;
    public static WeakReference<ScreenTimeService> sServiceRef;

    public static TimerHelper Helper = new TimerHelper();
    private Handler handler;
    private Runnable restCheckRunnable;
    private Runnable timeoutCheckRunnable;

    private NotificationCompat.Builder notificationBuilder;
    private Runnable updateRunnable;

    private View countdownView;
    private WindowManager windowManager;
    private long totalTime = 0;
    private CountDownTimer countDownTimer;
    private View passwordInputView;
    private static ASettings settings;

    private static String TAG = "ScreenTimeService";
    private String CHANNEL_ID = "LockAll_CHNNEL1";

    public static void init() {
        running = true;
        settings = ASettings.getInstance();
        ENABLED = settings.isHelperEnabled();
        MAX_USE_TIME = settings.getMaxUseTime() * 60 * 1000;
        REST_TIME = settings.getRestTime() * 60 * 1000;
        long currentM = System.currentTimeMillis();
        Helper.setLastOn(settings.getLastOnTime());
        // 还在休息中, 以休息时间为准
        if(settings.getLastLockTime() != 0 && currentM - settings.getLastLockTime() < REST_TIME) {
//            Helper.setShowing(false);
            Helper.setLastOff(currentM);
            Helper.setRestStart(settings.getLastLockTime());
        } else {
            settings.setLastLockTime(0);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Started");
        init();
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

//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this,
//                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Time Service")
                .setContentText("Tracking screen on")
                .setSmallIcon(R.drawable.ic_notification);
//                .setContentIntent(pendingIntent)

        startForeground(1, notificationBuilder.build());

        // Start the update task
        notiticationUpdateTask();

        // Return START_STICKY to make the service restart if it gets terminated
        return START_STICKY;
    }

    private void notiticationUpdateTask() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                String content;
                long timeRemain = MAX_USE_TIME - (System.currentTimeMillis() - Helper.getLastOn());
                if(Helper.isShowing()) {
                    content = "@_@";
                } else {
                    long hour = timeRemain / 1000 / 60 / 60 % 24;
                    long min = timeRemain / 1000 / 60 % 60;
                    long sec = timeRemain / 1000 % 60;
                    content = String.format("剩余时间: %02d:%02d:%02d", hour, min, sec);
                }
                notificationBuilder.setContentText(content);
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.notify(1, notificationBuilder.build());
                }

                // Schedule the next update after 1 second
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };

        // Start the first update
        handler.post(updateRunnable);
    }

    public void showUnlockDialog() {
        if(!ENABLED) return;
        handler.post(() -> {
            removeCountdownView();
            if(!Helper.isShowing()) return;
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            LayoutInflater inflater = LayoutInflater.from(ScreenTimeService.this);

            countdownView = inflater.inflate(R.layout.custom_countdown_dialog, null);

            final TextView titleTextView = countdownView.findViewById(R.id.dialog_title);
            final ProgressBar progressBar = countdownView.findViewById(R.id.progress_bar);
            final TextView timeRemainingTextView = countdownView.findViewById(R.id.time_remaining);
            final Button btnClose = countdownView.findViewById(R.id.btn_close);

            titleTextView.setText("Countdown Dialog");

            // 设置 WindowManager.LayoutParams
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                params.type = WindowManager.LayoutParams.TYPE_PHONE;
            }

            // 添加 View 到 WindowManager
            windowManager.addView(countdownView, params);
            totalTime = REST_TIME - (System.currentTimeMillis() - Helper.getRestStart());

            if(totalTime <= 0) return;

            // 创建倒计时器
            countDownTimer = new CountDownTimer(totalTime, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // 更新进度条
                    int progress = (int) (millisUntilFinished / (float) totalTime * 100);
                    progressBar.setProgress(progress);

                    // 更新剩余时间显示
                    long secondsRemaining = millisUntilFinished / 1000;
                    long minutes = secondsRemaining / 60;
                    long seconds = secondsRemaining % 60;
                    long hours = secondsRemaining / 60 / 60;
                    timeRemainingTextView.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                }

                @Override
                public void onFinish() {
                    // 倒计时结束时的处理
                    progressBar.setProgress(0);
                    timeRemainingTextView.setText("00:00:00");
                    // 移除 View
                    resetRestTime();
                }
            };

            // 开始倒计时
            countDownTimer.start();

            btnClose.setOnClickListener((a)->{
                if(!ENABLED || timeRemainingTextView.getText().equals("00:00:00")) {
                    resetRestTime();
                } else {
                    // 启动透明Activity进行指纹验证
                    Intent biometricIntent = new Intent(this, BiometricPromptActivity.class);
                    biometricIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(biometricIntent);
                    // 打开密码弹窗
//                    showPasswordInputDialog();
                }
            });
        });
    }

    private void showPasswordInputDialog() {
        removePassView();
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        passwordInputView = inflater.inflate(R.layout.password_input_dialog, null);

        final EditText passwordEditText = passwordInputView.findViewById(R.id.passwordEditText);
        Button submitButton = passwordInputView.findViewById(R.id.submitButton);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputPassword = passwordEditText.getText().toString();
                if (inputPassword.equals("tty0")) { // Replace "your_password" with the actual password
                    Toast.makeText(ScreenTimeService.this, "Password correct", Toast.LENGTH_SHORT).show();
                    resetRestTime();
                } else {
                    Toast.makeText(ScreenTimeService.this, "Password incorrect", Toast.LENGTH_SHORT).show();
                    removePassView();
                }
            }
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // Note: Use TYPE_APPLICATION_OVERLAY for API level 26 and above
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        params.gravity = Gravity.CENTER;

        windowManager.addView(passwordInputView, params);

        // 手动请求软键盘显示
        passwordEditText.post(new Runnable() {
            @Override
            public void run() {
                passwordEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            }
        });
    }

    private void removePassView() {
        if (windowManager != null && passwordInputView != null) {
            windowManager.removeView(passwordInputView);
            passwordInputView = null;
        }
    }

    public void resetRestTime() {
        Helper.setLastOn(0);
        Helper.setLastOff(0);
        Helper.setShowing(false);
        settings.setLastOnTime(0);
        settings.setLastLockTime(0);
        settings.setLastOffTime(0);
        removeCountdownView();
    }

    private void removeCountdownView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        removePassView();
        if (countdownView != null && windowManager != null) {
            try {
                if(countDownTimer != null) {
                    countDownTimer.cancel();
                }

                windowManager.removeView(countdownView);
                countdownView = null;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "View not attached to window manager", e);
            }
        }
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
        running = false;
        unregisterReceiver(screenTimeReceiver);
        Log.d("ScreenTimeService", "ScreenTimeReceiver unregistered");
        stopHandler();
        handler.removeCallbacks(updateRunnable);
        removeCountdownView();
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
                if(!ENABLED) {
                    resetRestTime();
                }
                if (ScreenTimeReceiver.isScreenOn()) {
                    // 在屏幕打开且不再休息的情况下, 才计数
                    if (!Helper.isShowing()) {
                        // 屏幕在正常状态息屏很久重新亮起的时候检查一下, 如果已经休息过了, 就重置
                        if (Helper.getLastOff() != 0 && currentM - Helper.getLastOff() > REST_TIME) {
                            // 重置
                            Helper.setLastOn(0);
                            Helper.setLastOff(currentM);
                        }
                        if (Helper.getLastOn() == 0) {
                            Helper.setLastOn(currentM);
                            settings.setLastOnTime(currentM);
                        } else {
                            // 正常处理
                            long timeRemain = MAX_USE_TIME - (currentM - Helper.getLastOn());
                            if (timeRemain <= 0) {
                                Log.d("ScreenTimeService", "Screen has been on for too long!");
                                // 这里可以添加屏幕亮屏超时的处理逻辑
                                Helper.setShowing(true);
                                Helper.setRestStart(currentM);
                                settings.setLastLockTime(currentM);
                                showUnlockDialog();
                            }
                            Helper.setLastOff(currentM);
                            if(currentM - settings.getLastOffTime() > 60 * 1000) {
                                settings.setLastOffTime(currentM);
                            }
                        }
                    } else {
                        // 正在休息, 检查休息时间是不是到了
                        if (Helper.getRestStart() == 0 || currentM - Helper.getRestStart() > REST_TIME) {
                            // 重置
                            resetRestTime();
                        }
                    }
                } else {
                    // 息屏下判断, 息屏的时间是不是比休息时间长, 如果长, 就算休息了
                    if (!Helper.isShowing()) {
                        if (Helper.getLastOff() == 0 || currentM - Helper.getLastOff() > REST_TIME) {
                            // 重置
                            Helper.setLastOn(0);
                            Helper.setLastOff(currentM);
                        }
                        if(Helper.getLastOn() != 0)
                            Helper.setLastOn(Helper.getLastOn() + 1000);
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
                    if (currentM - Helper.getRestStart() < REST_TIME) {
                        // 正在休息倒计时, 这里可能要开启检测, 避免卸载

                    } else {
                        resetRestTime();
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