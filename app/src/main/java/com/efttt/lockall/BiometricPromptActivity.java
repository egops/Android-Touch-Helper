package com.efttt.lockall;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executor;

public class BiometricPromptActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "该设备没有生物识别功能", Toast.LENGTH_SHORT).show();
                finish();
                return;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "生物识别功能当前不可用", Toast.LENGTH_SHORT).show();
                finish();
                return;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "没有配置任何生物识别信息", Toast.LENGTH_SHORT).show();
                finish();
                return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(BiometricPromptActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                ScreenTimeService.sServiceRef.get().resetRestTime();
//                Toast.makeText(BiometricPromptActivity.this, "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                // 在此处处理验证成功的情况
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(BiometricPromptActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("生物识别验证")
                .setSubtitle("使用您的生物识别信息进行验证")
                .setNegativeButtonText("取消")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}