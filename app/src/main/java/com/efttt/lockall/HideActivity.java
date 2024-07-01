package com.efttt.lockall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

public class HideActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 动态创建布局
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // 设置 Activity 的内容视图为动态创建的布局
        setContentView(layout);

        // 启动 SecondActivity
        Intent intent = new Intent(HideActivity.this, MainActivity.class);
        startActivity(intent);

        // 结束当前 Activity
        finish();
    }
}
