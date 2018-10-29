package com.example.android.architecture.blueprints.todoapp.my.base;

import android.support.v7.app.AppCompatActivity;

import com.example.android.architecture.blueprints.todoapp.my.constant.DebugConst;
import com.example.android.architecture.blueprints.todoapp.my.util.ToastUtils;


public class BaseActivity extends AppCompatActivity {
    protected String TAG;

    @Override
    protected void onResume() {
        super.onResume();
        TAG = getClass().getSimpleName();
        if (DebugConst.DEBUG_SHOW_UI_NAME) {
            ToastUtils.showShort(TAG);
        }
    }
}