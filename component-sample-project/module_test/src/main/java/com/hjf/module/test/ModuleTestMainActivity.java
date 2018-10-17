package com.hjf.module.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.hjf.router.facade.annotation.Autowired;
import com.hjf.router.facade.annotation.Route;
import com.hjf.router.router.Router;

@Route(path = "/test/main")
public class ModuleTestMainActivity extends AppCompatActivity {

    @Autowired()
    public int intabc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Router.getInstance().inject(this);

        setContentView(R.layout.activity_module_test_main);

        TextView textView = findViewById(R.id.textView);
        textView.setText(textView.getText().toString() + "\n param intabc = " + intabc);
    }
}
