package com.hjf.module.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hjf.module.core.aop.SingleClick;
import com.hjf.router.facade.annotation.Autowired;
import com.hjf.router.facade.annotation.Route;
import com.hjf.router.router.Router;

@Route(path = "/test/main")
public class ModuleTestMainActivity extends AppCompatActivity {

    @Autowired()
    public int intabc;
    public int num;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Router.getInstance().inject(this);

        setContentView(R.layout.activity_module_test_main);

        TextView textView = findViewById(R.id.textView);
        textView.setText(textView.getText().toString() + "\n param intabc = " + intabc);
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
