package com.xinlan.croprotateview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity {
    private CropRotateView mCropView;
    private SeekBar mRotateSeek;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRotateSeek = (SeekBar) findViewById(R.id.seekBar);
        mCropView = (CropRotateView) findViewById(R.id.crop_view);
        //Bitmap srcBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.demo);
        Bitmap srcBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.demo3);

        mCropView.setBitmap(srcBitmap);

        mRotateSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCropView.setRotate(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}//end class
