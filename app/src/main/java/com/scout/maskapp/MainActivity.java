package com.scout.maskapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.scout.maskapp.Mask.MaskedEditText;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final MaskedEditText edit = (MaskedEditText)findViewById(R.id.editor);

        findViewById(R.id.mask1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit.setMask("++\\d\\d--\\d\\d\\d-\\d\\d");
            }
        });
        findViewById(R.id.mask2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit.setMask("+3\\d\\d-\\d\\c\\c-\\c\\c");
            }
        });
        findViewById(R.id.value1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit.setText("1234567890");
            }
        });
        findViewById(R.id.value2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit.setText("987654321");
            }
        });
    }
}
