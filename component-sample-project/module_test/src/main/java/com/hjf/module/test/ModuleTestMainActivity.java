package com.hjf.module.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.hjf.router.facade.annotation.Route;

@Route(path = "/test/main")
public class ModuleTestMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_test_main);
    }
}
