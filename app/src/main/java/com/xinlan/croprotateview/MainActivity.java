package com.xinlan.croprotateview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private CropRotateView mCropView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCropView = (CropRotateView) findViewById(R.id.crop_view);
        //Bitmap srcBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.demo);
        Bitmap srcBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.demo);
        mCropView.setBitmap(srcBitmap);
    }
}//end class
