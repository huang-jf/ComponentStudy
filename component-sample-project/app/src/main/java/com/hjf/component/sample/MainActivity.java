package com.hjf.component.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.hjf.module.core.aop.SingleClick;
import com.hjf.router.router.Router;

public class MainActivity extends AppCompatActivity {

    public int num;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_jump).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Router.getInstance().build("/test/main")
                        .withInt("intabc", 15)
                        .navigation(MainActivity.this);
            }
        });
        findViewById(R.id.tv_toast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast(v);
            }
        });
    }

    @SingleClick
    private void toast(View v) {
        Toast.makeText(getApplicationContext(), "click -> " + (++num), Toast.LENGTH_SHORT).show();
    }
}
