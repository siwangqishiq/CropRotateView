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
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by panyi on 2017/8/19.
 */

public class CropRotateView extends View {
    private static final int CONTROL_BTN_DEFAULT_SIZE = 25;
    private static final int CONTROL_BTN_DEFAULT_COLOR = Color.rgb(171, 175, 178);
    private static final String DEFAULT_BACKGROUND_COLOR_ID = "#90000000";

    private int mAngle;
    private Bitmap mSrcBit;

    /**
     * 当旋转发生时图片的缩放方式
     */
    public static enum RotateCropType {
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

    private static final int STATUS_IDLE = 1;//
    private static final int STATUS_MOVE = 2;
    private static final int STATUS_RESIZE = 3;

    private int mStatus = STATUS_IDLE;

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

    private int mSelectedControlBtn = -1;

    private int lastX = 0;
    private int lastY = 0;

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

    private void init(Context contetxt) {
        mAngle = 0;

        mCropBgPaint = new Paint();
        mCropBgPaint.setColor(Color.parseColor(DEFAULT_BACKGROUND_COLOR_ID));

        mCropControlPaint = new Paint();
        mCropControlPaint.setColor(mControlBtnColor);
    }

    public void setBitmap(Bitmap bit) {
        if (bit == null)
            return;

        mAngle = 0;
        mSrcBit = bit;
        mSrcRect.set(0, 0, mSrcBit.getWidth(), mSrcBit.getHeight());

        int w = getWidth();
        int h = getHeight();

        if (w == 0 || h == 0)
            return;
        calDstRect(w, h, mSrcRect);//计算出新的目标矩形

        invalidate();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
            calDstRect(getMeasuredWidth(), getMeasuredHeight(), mSrcRect);//计算出新的目标矩形
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mSrcBit == null)
            return;

        //do draw
        canvas.save();
        updateScaleByRotate(mSrcRect, mAngle, canvas);
        canvas.rotate(mAngle, getWidth() >> 1, getHeight() >> 1);
        canvas.drawBitmap(mSrcBit, mSrcRect, mDstRect, null);
        canvas.restore();
        drawCropView(canvas);
        //matirx.mapRect()
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = super.onTouchEvent(event);// 是否向下传递事件标志 true为消耗
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                System.out.println("ACTION_DOWN");
                int controllCheclResult = checkHitControllerBtn(x, y);
                if (controllCheclResult > 0) {// set resize mode
                    ret = true;
                    mSelectedControlBtn = controllCheclResult;
                    mStatus = STATUS_RESIZE;
                } else if (mCropRect.contains((int) x, (int) y)) {//check in crop rect
                    ret = true;
                    mStatus = STATUS_MOVE;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                handleOnMove(x - lastX, y - lastY, x, y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //System.out.println("ACTION_UP");
                mStatus = STATUS_IDLE;
                break;
        }

        lastX = x;
        lastY = y;
        return ret;
    }

    private void handleOnMove(int dx, int dy, int x, int y) {
        switch (mStatus) {
            case STATUS_IDLE:
                break;
            case STATUS_MOVE:
                tranformCropRectWithLimit(dx, dy);
                break;
            case STATUS_RESIZE:
                if (mSelectedControlBtn < 0) {
                    return;
                }

                if (mSelectedControlBtn == 1) {//left point
                    setCropRectWithLimit(x, y, mCropRect.right, mCropRect.bottom);
                } else if (mSelectedControlBtn == 2) {//right point
                    setCropRectWithLimit(mCropRect.left, y, x, mCropRect.bottom);
                } else if (mSelectedControlBtn == 3) {//left and bottom point
                    setCropRectWithLimit(x, mCropRect.top, mCropRect.right, y);
                } else if (mSelectedControlBtn == 4) {
                    setCropRectWithLimit(mCropRect.left, mCropRect.top, x, y);
                }//end if
                invalidate();
                break;
        }

        //System.out.println("ACTION_MOVE");
    }

    private void tranformCropRectWithLimit(int dx, int dy) {

        if (mCropRect.left + dx > mCropBoundRect.left && mCropRect.right + dx < mCropBoundRect.right) {
            mCropRect.left += dx;
            mCropRect.right += dx;
        } else {
            if (mCropRect.left + dx > mCropBoundRect.left && mCropRect.right + dx >= mCropBoundRect.right) {
                mCropRect.left = mCropBoundRect.right - mCropRect.width();
                mCropRect.right = mCropBoundRect.right;
            } else if (mCropRect.left + dx <= mCropBoundRect.left && mCropRect.right + dx < mCropBoundRect.right) {
                mCropRect.right = mCropBoundRect.left + mCropRect.width();
                mCropRect.left = mCropBoundRect.left;
            }
        }


        if (mCropRect.top + dy > mCropBoundRect.top && mCropRect.bottom + dy < mCropBoundRect.bottom) {
            mCropRect.top += dy;
            mCropRect.bottom += dy;
        } else {
            if (mCropRect.top + dy > mCropBoundRect.top && mCropRect.bottom + dy >= mCropBoundRect.bottom) {
                mCropRect.top = mCropBoundRect.bottom - mCropRect.height();
                mCropRect.bottom = mCropBoundRect.bottom;
            } else if (mCropRect.top + dy <= mCropBoundRect.top && mCropRect.bottom + dy < mCropBoundRect.bottom) {
                mCropRect.bottom = mCropBoundRect.top + mCropRect.height();
                mCropRect.top = mCropBoundRect.top;
            }
        }

        limitCropRectInBounds();
        invalidate();
    }

    private void setCropRectWithLimit(int left, int top, int right, int bottom) {
        int twiceCtlBtnWith = mControlBtnSize << 1;
        if (left >= mCropRect.right - twiceCtlBtnWith) {
            left = mCropRect.right - twiceCtlBtnWith;
        }

        if (top >= mCropRect.bottom - twiceCtlBtnWith) {
            top = mCropRect.bottom - twiceCtlBtnWith;
        }

        if (right <= mCropRect.left + twiceCtlBtnWith) {
            right = mCropRect.left + twiceCtlBtnWith;
        }

        if (bottom <= mCropRect.top + twiceCtlBtnWith) {
            bottom = mCropRect.top + twiceCtlBtnWith;
        }

        mCropRect.set(left, top, right, bottom);
        limitCropRectInBounds();
    }

    private boolean checkRectInBounds(Rect rect) {
        if (rect.left <= mCropBoundRect.left) {
            return false;
        }

        if (rect.top <= mCropBoundRect.top) {
            return false;
        }

        if (rect.right >= mCropBoundRect.right) {
            return false;
        }

        if (rect.bottom >= mCropBoundRect.bottom) {
            return false;
        }

        return true;
    }

    private void limitCropRectInBounds() {
        if (mCropRect.left <= mCropBoundRect.left) {
            mCropRect.left = mCropBoundRect.left;
        }

        if (mCropRect.top <= mCropBoundRect.top) {
            mCropRect.top = mCropBoundRect.top;
        }

        if (mCropRect.right >= mCropBoundRect.right) {
            mCropRect.right = mCropBoundRect.right;
        }

        if (mCropRect.bottom >= mCropBoundRect.bottom) {
            mCropRect.bottom = mCropBoundRect.bottom;
        }
    }

    private int checkHitControllerBtn(float x, float y) {
        int left = mCropRect.left;
        int top = mCropRect.top;
        int right = mCropRect.right;
        int bottom = mCropRect.bottom;

        if (x >= left - mControlBtnSize && x <= left + mControlBtnSize &&
                y >= top - mControlBtnSize && y <= top + mControlBtnSize) {
            //System.out.println("selcet one button");
            return 1;
        }

        if (x >= right - mControlBtnSize && x <= right + mControlBtnSize &&
                y >= top - mControlBtnSize && y <= top + mControlBtnSize) {
            //System.out.println("selcet two button");
            return 2;
        }

        if (x >= left - mControlBtnSize && x <= left + mControlBtnSize &&
                y >= bottom - mControlBtnSize && y <= bottom + mControlBtnSize) {
            //System.out.println("selcet three button");
            return 3;
        }

        if (x >= right - mControlBtnSize && x <= right + mControlBtnSize &&
                y >= bottom - mControlBtnSize && y <= bottom + mControlBtnSize) {
            //System.out.println("selcet four button");
            return 4;
        }

        return -1;
    }

    /**
     * 设置旋转
     *
     * @param degree
     */
    public void setRotate(int degree) {
        mAngle = degree;
        invalidate();
    }

    private void drawCropView(Canvas canvas) {
        if (canvas == null)
            return;

        //mCropRect.set(mCropBoundRect);

        //draw bg
        canvas.drawRect(0, 0, getWidth(), mCropRect.top, mCropBgPaint);
        canvas.drawRect(0, mCropRect.top, mCropRect.left, mCropRect.bottom, mCropBgPaint);
        canvas.drawRect(mCropRect.right, mCropRect.top, getWidth(), mCropRect.bottom, mCropBgPaint);
        canvas.drawRect(0, mCropRect.bottom, getWidth(), getHeight(), mCropBgPaint);

        //draw control btn
        canvas.drawCircle(mCropRect.left, mCropRect.top, mControlBtnSize, mCropControlPaint);
        canvas.drawCircle(mCropRect.right, mCropRect.top, mControlBtnSize, mCropControlPaint);
        canvas.drawCircle(mCropRect.left, mCropRect.bottom, mControlBtnSize, mCropControlPaint);
        canvas.drawCircle(mCropRect.right, mCropRect.bottom, mControlBtnSize, mCropControlPaint);
    }

    /**
     * 计算显示矩形框
     */
    private void calDstRect(int viewWidth, int viewHeight, Rect srcRect) {
        if (viewWidth <= 0 || viewHeight <= 0 || srcRect == null)
            return;

        float ratio = (float) srcRect.width() / srcRect.height();//原始图片宽高比
        int dstH, dstW, dstLeft, dstTop;
        if (srcRect.width() >= srcRect.height()) { // w >= h
            dstW = viewWidth;
            dstH = (int) (viewWidth / ratio);
            dstLeft = 0;
            dstTop = (viewHeight >> 1) - (dstH >> 1);
        } else { // w <h
            dstH = viewHeight;
            dstW = (int) (viewHeight * ratio);
            dstLeft = (viewWidth >> 1) - (dstW >> 1);
            dstTop = 0;
        }//end if
        mDstRect.set(dstLeft, dstTop, dstLeft + dstW, dstTop + dstH);
        mCropBoundRect.set(mDstRect);
        mCropRect.set(mCropBoundRect);
    }

    /**
     * 依据旋转角度  计算出缩放比例
     * originRect 原始图片矩形
     * degree 旋转角度
     * 此缩放比可以保证图片被完整显示 或 裁剪
     *
     * @return
     */
    private float updateScaleByRotate(Rect originRect, int degree, Canvas canvas) {
        float retScale = 1.0f;

        tempMatrix.reset();
        tempRect.set(originRect);

        tempMatrix.postRotate(degree, getWidth() >> 1, getHeight() >> 1);
        tempMatrix.mapRect(tempRect);

        if (mCropType == RotateCropType.CROP) {//裁剪策略
            float scaleWidth = tempRect.width() / originRect.width();
            float scaleHeight = tempRect.height() / originRect.height();

            if (originRect.width() >= originRect.height()) {
                retScale = scaleHeight;
            } else {
                retScale = scaleWidth;
            }
            canvas.scale(retScale, retScale, getWidth() >> 1, getHeight() >> 1);
            //System.out.println("scaleWidth = "+scaleWidth+"   scaleHeight = "+scaleHeight);
        } else if (mCropType == RotateCropType.SCALE) {
            tempMatrix.reset();
            tempRect.set(originRect);

            tempMatrix.postRotate(degree, getWidth() >> 1, getHeight() >> 1);
            tempMatrix.mapRect(tempRect);

            float scaleWidth = originRect.width() / tempRect.width();
            float scaleHeight = originRect.height() / tempRect.height();

            if (originRect.width() >= originRect.height()) {
                retScale = scaleHeight;
            } else {
                retScale = scaleWidth;
            }
            canvas.scale(retScale, retScale, getWidth() >> 1, getHeight() >> 1);
            //System.out.println("scaleWidth = "+scaleWidth+"   scaleHeight = "+scaleHeight);
        }
        //mCropBoundRect.set((int)tempRect.left,tempRect.top , tempRect.right , tempRect.bottom);
        return retScale;
    }
}//end class
