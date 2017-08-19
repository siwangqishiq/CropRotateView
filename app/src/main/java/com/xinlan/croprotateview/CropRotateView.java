package com.xinlan.croprotateview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by panyi on 2017/8/19.
 */

public class CropRotateView extends View {
    private int mAngle;
    private Bitmap mSrcBit;

    /**
     * 当旋转发生时图片的缩放方式
     */
    public static enum RotateCropType{
        /**
         * 裁剪以维持原有图片尺寸
         */
        CROP,
        /**
         * 缩小图片以维持原有尺寸
         */
        SCALE
    }

    private RotateCropType mCropType = RotateCropType.CROP;

    private Rect mSrcRect = new Rect();
    private Rect mDstRect = new Rect();

    public CropRotateView(Context context) {
        super(context);
        init(context);
    }

    public CropRotateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CropRotateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CropRotateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context contetxt){
        mAngle = 0;
    }

    public void setBitmap(Bitmap bit){
        if(bit == null)
            return;

        mAngle = 0;
        mSrcBit = bit;
        mSrcRect.set(0,0,mSrcBit.getWidth(),mSrcBit.getHeight());
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(mSrcBit == null)
            return;

        int w = getWidth();
        int h = getHeight();

        if(w == 0 || h == 0)
            return;
        calDstRect(w , h,mSrcRect);//计算出新的目标矩形
        canvas.drawBitmap(mSrcBit,mSrcRect,mDstRect,null);
    }

    /**
     * 计算显示矩形框
     */
    private void calDstRect(int viewWidth,int viewHeight , Rect srcRect){
        if(viewWidth <=0 || viewHeight <=0 || srcRect ==null)
            return;

        float ratio = (float)srcRect.width() / srcRect.height();//原始图片宽高比
        int dstH ,dstW ,dstLeft,dstTop;
        if(srcRect.width() >= srcRect.height()){ // w >= h
            dstW = viewWidth;
            dstH = (int)(viewWidth / ratio);
            dstLeft = 0;
            dstTop = (viewHeight >> 1 ) - (dstH >> 1);
        }else{ // w <h
            dstH = viewHeight;
            dstW = (int)(viewHeight * ratio);
            dstLeft = (viewWidth>>1) - (dstW >> 1);
            dstTop = 0;
        }//end if
        mDstRect.set(dstLeft, dstTop , dstLeft + dstW , dstTop + dstH);
    }
}//end class
