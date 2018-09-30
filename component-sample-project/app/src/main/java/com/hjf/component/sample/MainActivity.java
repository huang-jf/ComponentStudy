package com.hjf.component.sample;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.alibaba.android.arouter.facade.template.IRouteRoot;
import com.alibaba.android.arouter.facade.template.IRouteGroup;

import com.alibaba.android.arouter.facade.template.IProviderGroup;
import com.alibaba.android.arouter.facade.template.IProvider;

import com.alibaba.android.arouter.facade.template.IInterceptor;
import com.alibaba.android.arouter.facade.template.IInterceptorGroup;
import com.alibaba.android.arouter.facade.template.ISyringe;
import com.alibaba.android.arouter.routes.ARouter$$Providers$$arouterapi;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tv_click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("app://module/a1");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }
}
