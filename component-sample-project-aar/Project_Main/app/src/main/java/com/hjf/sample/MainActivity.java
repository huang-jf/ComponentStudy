package com.hjf.sample;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.alibaba.android.arouter.launcher.ARouter;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button).setOnClickListener(this);
        findViewById(R.id.button2).setOnClickListener(this);

        ARouter.init(getApplication());
        showModuleBFragment();
    }

    private void showModuleBFragment() {
        Fragment fragment = (Fragment) ARouter.getInstance().build("/moduleB/fragment/main").navigation();
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fl_content, fragment).commit();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                ARouter.getInstance().build("/moduleA/activity/main").navigation(this);
                break;
                // TODO 提取基本依赖库，获取服务进行动作
            case R.id.button2:
//                ARouter.getInstance().build("/moduleA/Activity/main").navigation(this);
                break;
        }
    }
}
