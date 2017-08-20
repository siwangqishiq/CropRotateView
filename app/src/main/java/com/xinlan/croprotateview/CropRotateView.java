package com.xinlan.croprotateview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by panyi on 2017/8/19.
 */

public class CropRotateView extends View {
    private static final int CONTROL_BTN_DEFAULT_SIZE = 25;
    private static final int CONTROL_BTN_DEFAULT_COLOR = Color.rgb(171,175,178);
    private static final String DEFAULT_BACKGROUND_COLOR_ID = "#B0000000";

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
        SCALE,
        /**
         * 不做缩放操作
         */
        ORIGIN
    }

    private RotateCropType mCropType = RotateCropType.CROP;

    private final Rect mSrcRect = new Rect();
    private final Rect mDstRect = new Rect();
    private final Rect mCropBoundRect = new Rect();//剪裁边界
    private final Rect mCropRect = new Rect();

    private Matrix tempMatrix = new Matrix();
    private RectF tempRect = new RectF();

    private int mControlBtnSize = CONTROL_BTN_DEFAULT_SIZE;
    private int mControlBtnColor = CONTROL_BTN_DEFAULT_COLOR;
    private Paint mCropBgPaint;
    private Paint mCropControlPaint;

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

        mCropBgPaint = new Paint();
        mCropBgPaint.setColor(Color.parseColor(DEFAULT_BACKGROUND_COLOR_ID));

        mCropControlPaint = new Paint();
        mCropControlPaint.setColor(mControlBtnColor);
    }

    public void setBitmap(Bitmap bit){
        if(bit == null)
            return;

        mAngle = 0;
        mSrcBit = bit;
        mSrcRect.set(0,0,mSrcBit.getWidth(),mSrcBit.getHeight());
        invalidate();
    }

    /**
     * 设置旋转
     *
     * @param degree
     */
    public void setRotate(int degree){
        mAngle = degree;
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

        //do draw
        canvas.save();
        updateScaleByRotate(mSrcRect , mAngle,canvas);
        canvas.rotate(mAngle,getWidth()>>1,getHeight()>>1);
        canvas.drawBitmap(mSrcBit,mSrcRect,mDstRect,null);
        canvas.restore();
        drawCropView(canvas);
        //matirx.mapRect()
    }

    private void drawCropView(Canvas canvas){
        if(canvas == null)
            return;

        mCropRect.set(mCropBoundRect);

        //draw bg
        canvas.drawRect(0,0,getWidth(), mCropRect.top , mCropBgPaint);
        canvas.drawRect(0,mCropRect.top,mCropRect.left, mCropRect.bottom , mCropBgPaint);
        canvas.drawRect(mCropRect.right,mCropRect.top,getWidth(), mCropRect.bottom , mCropBgPaint);
        canvas.drawRect(0,mCropRect.bottom,getWidth(), getHeight() , mCropBgPaint);

        //draw control btn
        canvas.drawCircle(mCropBoundRect.left, mCropBoundRect.top , mControlBtnSize , mCropControlPaint);
        canvas.drawCircle(mCropBoundRect.right, mCropBoundRect.top , mControlBtnSize , mCropControlPaint);
        canvas.drawCircle(mCropBoundRect.left, mCropBoundRect.bottom , mControlBtnSize , mCropControlPaint);
        canvas.drawCircle(mCropBoundRect.right, mCropBoundRect.bottom , mControlBtnSize , mCropControlPaint);
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
        mCropBoundRect.set(mDstRect);
    }

    /**
     *  依据旋转角度  计算出缩放比例
     *  originRect 原始图片矩形
     *  degree 旋转角度
     *
     *  此缩放比可以保证图片被完整显示 或 裁剪
     * @return
     */
    private float updateScaleByRotate(Rect originRect , int degree,Canvas canvas){
        float retScale = 1.0f;

        tempMatrix.reset();
        tempRect.set(originRect);

        tempMatrix.postRotate(degree , getWidth()>>1,getHeight() >>1);
        tempMatrix.mapRect(tempRect);

        if(mCropType == RotateCropType.CROP){//裁剪策略
            float scaleWidth = tempRect.width() / originRect.width();
            float scaleHeight = tempRect.height() / originRect.height();

            if(originRect.width() >= originRect.height()){
                retScale = scaleHeight;
            }else{
                retScale = scaleWidth;
            }
            canvas.scale(retScale,retScale , getWidth()>>1,getHeight() >>1);
            System.out.println("scaleWidth = "+scaleWidth+"   scaleHeight = "+scaleHeight);
        }else if(mCropType == RotateCropType.SCALE){
            tempMatrix.reset();
            tempRect.set(originRect);

            tempMatrix.postRotate(degree , getWidth()>>1,getHeight() >>1);
            tempMatrix.mapRect(tempRect);

            float scaleWidth = originRect.width() /  tempRect.width();
            float scaleHeight = originRect.height() /  tempRect.height();

            if(originRect.width() >= originRect.height()){
                retScale = scaleHeight;
            }else{
                retScale = scaleWidth;
            }
            canvas.scale(retScale,retScale , getWidth()>>1,getHeight() >>1);
            System.out.println("scaleWidth = "+scaleWidth+"   scaleHeight = "+scaleHeight);
        }

        //mCropBoundRect.set((int)tempRect.left,tempRect.top , tempRect.right , tempRect.bottom);
        return retScale;
    }
}//end class
