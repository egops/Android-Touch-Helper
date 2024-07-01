package com.efttt.lockall.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    private MutableLiveData<Boolean> mAppPermission;
    private MutableLiveData<Boolean> mAccessibilityPermission;
    private MutableLiveData<Boolean> mPowerOptimization;
    private MutableLiveData<Boolean> mUsagePermission;
    private MutableLiveData<Boolean> mBackPermission;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mAppPermission = new MutableLiveData<>();
        mAccessibilityPermission = new MutableLiveData<>();
        mPowerOptimization = new MutableLiveData<>();
        mUsagePermission = new MutableLiveData<>();
        mBackPermission = new MutableLiveData<>();
    }

    public LiveData<String> getText() {
        return mText;
    }

    public MutableLiveData<Boolean> getAppPermission() {
        return mAppPermission;
    }

    public MutableLiveData<Boolean> getAccessibilityPermission() {
        return mAccessibilityPermission;
    }

    public MutableLiveData<Boolean> getPowerOptimization() {
        return mPowerOptimization;
    }

    public MutableLiveData<Boolean> getUsagePermission() {
        return mUsagePermission;
    }

    public MutableLiveData<Boolean> getBackPermission() {
        return mBackPermission;
    }
}