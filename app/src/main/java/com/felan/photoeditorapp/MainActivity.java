package com.felan.photoeditorapp;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.felan.photoeditor.utils.ApplicationLoader;
import com.felan.photoeditor.widgets.PhotoEditView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ApplicationLoader.applicationContext = getApplicationContext();

        PhotoEditView editor = findViewById(R.id.photo_edit);
        editor.setImage(BitmapFactory.decodeResource(getResources(), R.drawable.account));
    }

}
