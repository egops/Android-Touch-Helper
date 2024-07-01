package com.efttt.lockall.ui.home;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.efttt.lockall.R;
import com.efttt.lockall.TouchHelperService;

import java.lang.reflect.Method;

public class HomeFragment extends Fragment {

    private final String TAG = getClass().getName();

    private HomeViewModel homeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        final Drawable drawableYes = ContextCompat.getDrawable(getContext(), R.drawable.ic_right);
        final Drawable drawableNo = ContextCompat.getDrawable(getContext(), R.drawable.ic_wrong);

        // set observers for widget
        final ImageView imageUsagePermission = root.findViewById(R.id.image_usage_permission);
        homeViewModel.getUsagePermission().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean) {
                    imageUsagePermission.setImageDrawable(drawableYes);
                } else {
                    imageUsagePermission.setImageDrawable(drawableNo);
                }
            }
        });

        final ImageView imageBackPermission = root.findViewById(R.id.image_back_permission);
        homeViewModel.getBackPermission().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean) {
                    imageBackPermission.setImageDrawable(drawableYes);
                } else {
                    imageBackPermission.setImageDrawable(drawableNo);
                }
            }
        });

        final ImageView imageAccessibilityPermission = root.findViewById(R.id.image_accessibility_permission);
        homeViewModel.getAccessibilityPermission().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean) {
                    imageAccessibilityPermission.setImageDrawable(drawableYes);
                } else {
                    imageAccessibilityPermission.setImageDrawable(drawableNo);
                }
            }
        });

        final ImageView imagePowerPermission = root.findViewById(R.id.image_power_permission);
        homeViewModel.getPowerOptimization().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean) {
                    imagePowerPermission.setImageDrawable(drawableYes);
                } else {
                    imagePowerPermission.setImageDrawable(drawableNo);
                }
            }
        });


        // set listener for buttons
        final ImageButton btAccessibilityPermission = root.findViewById(R.id.button_accessibility_permission);
        btAccessibilityPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent_abs);
            }
        });

        final ImageButton btPowerPermission = root.findViewById(R.id.button_power_permission);
        btPowerPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  打开电池优化的界面，让用户设置
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    String packageName = getActivity().getPackageName();

                    // open battery optimization setting page
                    Intent intent = new Intent();
                    PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                }
            }
        });

        final ImageButton btUsagePermission = root.findViewById(R.id.button_usage_permission);
        btUsagePermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_abs = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent_abs);
            }
        });

        final ImageButton btBackPermission = root.findViewById(R.id.button_back_permission);
        btBackPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_abs = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent_abs);
            }
        });

        // get the service status
        checkServiceStatus();

        return root;
    }

    @Override
    public void onResume() {
        checkServiceStatus();
        super.onResume();
    }

    private boolean isXiaomiBgStartPermissionAllowed(Context context) {
        return Settings.canDrawOverlays(getContext());
    }

    // Method to check if the app has Usage Stats permission
    private boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public void checkServiceStatus(){

        // detect the app storage permission
        boolean bAppPermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        MutableLiveData<Boolean> liveData = homeViewModel.getAppPermission();
        liveData.setValue(bAppPermission);

        // detect the usage permission
        MutableLiveData<Boolean> usage = homeViewModel.getUsagePermission();
        boolean bUsagePermission= hasUsageStatsPermission(getContext());
        usage.setValue(bUsagePermission);

        // detect the back permission
        MutableLiveData<Boolean> back = homeViewModel.getBackPermission();
        boolean bBackPermission = isXiaomiBgStartPermissionAllowed(getContext());
        back.setValue(bBackPermission);

        // detect the accessibility permission
        MutableLiveData<Boolean> accessibility = homeViewModel.getAccessibilityPermission();
        accessibility.setValue(TouchHelperService.isServiceRunning());

        // detect power optimization
        PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        boolean hasIgnored = pm.isIgnoringBatteryOptimizations(getContext().getPackageName());
        MutableLiveData<Boolean> power = homeViewModel.getPowerOptimization();
        power.setValue(hasIgnored);
    }
}