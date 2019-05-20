package com.example.flappybird;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;

public class Bird {
    private static final int RISING_MAX_ANGLE = -30;
    private static final int FALLING_MAX_ANGLE = 70;
    private static final int MAX_RISE_SPEED_Y_STANDBY = -10; // 待命时刻向上飞的速度
    private static final int FALL_ACCEL_Y_STANDBY = 1; // 待命时刻向下的加速的
    private static final int MAX_RISE_SPEED_Y = -80;
    private static final int FALL_ACCEL_Y = 20;

    /**
     * 记录鸟的位置和大小
     */
    private Rect mBound;
    // 鸟的皮肤，由 3 张位图构成，分别是
    // 对这三张图进行切换，就可以实现飞翔效果
    private Bitmap[] mBirdsSkin;

    // Matrix 是用于图片变换的矩阵，能实现对位图的移动、缩放和旋转变换
    private Matrix mMatrix;
    // 标识是否处于待命状态 （另一个状态是游戏状态）
    private boolean mIsStandby;
    private boolean mIsDead;

    // 标识是否需要调整图片大小
    private boolean mNeedScale;
    private float mScaleX;
    private float mScaleY;

    // Y方向的速度
    private int mSpeedY;
    // Y方向的加速度
    private int mAccelY;
    // 旋转角的速度
    private float mAngularSpeed;

    // 当前变换帧数
    private int mFrameCount;
    // 当前旋转的角度
    private float mRotationAngle;

    // 设置鸟在屏幕中的位置
    public synchronized Bird setBound(Rect bound){
        mBound = bound;
        return this;
    }
    // 返回鸟的位置
    public Rect getBound() {
        return mBound;
    }

    // 设置一个 Matrix 类的实例来控制位图
    public Bird setMatrix(Matrix matrix){
        mMatrix = matrix;
        return this;
    }

//  设置鸟的皮肤
    public Bird setBirdsSkin(Bitmap[] skin) {
        this.mBirdsSkin = skin;
        // getHeight() 和 getWidth() 方法的返回值会根据 dpi 的不同而有所调整
        // 所以同样的图片，在不同机型上的返回值可能不一样
        int bitmapHeight = mBirdsSkin[0].getHeight();
        int bitmapWidth = mBirdsSkin[0].getWidth();
        // 判断是否需要缩放
        if (bitmapHeight != mBound.height() ||
                bitmapWidth != mBound.width()) {
            mScaleX = mBound.width() / bitmapWidth;
            mScaleY = mBound.height() / bitmapHeight;
            mNeedScale = true;
        } else {
            mNeedScale = false;
        }
        return this;
    }

    // 待命状态： 上下飞
    public void makeStandby() {
        mIsStandby = true;
        mIsDead = false;
        mSpeedY = MAX_RISE_SPEED_Y_STANDBY; // 从中间开始向上飞
        mAccelY = FALL_ACCEL_Y_STANDBY; // 加速度向下
    }

    public void put2Death() {
        mIsDead = true;
        mSpeedY = 0;
        mAccelY = FALL_ACCEL_Y;
    }


    // 发射 单击屏幕后小鸟向上飞的过程
    public synchronized void shot(){
        mIsStandby = false;
        mAccelY = FALL_ACCEL_Y; // 向下的加速度
        mSpeedY = MAX_RISE_SPEED_Y; // 速度向上
        calAngularSpeed(RISING_MAX_ANGLE); //计算角速度
    }
    // 角速度计算： 参数是从当前角速度变换至最大角度
    // 观察鸟的飞行效果，当鸟发射时，会有一个向上旋转的过程，这个旋转角度的最大值
    // 在鸟飞行到最高点后，会朝饭方向旋转（向下）达到 70 度
    // 鸟发射至最高点的时间，可以由发射时速度除以加速度计算得到
    private void calAngularSpeed(int toAngle){
        int frameCount = 0;
        // 在往上飞的时候
        if (mSpeedY < 0){
            frameCount = mSpeedY / (-mAccelY);
        } else {
            frameCount = 2 * MAX_RISE_SPEED_Y / (-FALL_ACCEL_Y); // 坠落的时候
        }
        mAngularSpeed = (toAngle - mRotationAngle) / frameCount;
    }

    public void draw(Canvas canvas) {
        // 具体在画布中如何画鸟？
        // 根据帧号 mFrameCount 选择要绘制的鸟皮肤
        Bitmap skin = mBirdsSkin[mFrameCount++ % mBirdsSkin.length];
        // [mFrameCount++ % mBirdsSkin.length] => [0,1,2]
        if (mFrameCount == mBirdsSkin.length)
            mFrameCount = 0;

        mMatrix.reset(); // 清除矩阵中的数据
        if (mNeedScale) {  // 如果皮肤大小和给定的鸟的 bound 不一致，则需要先缩放
            mMatrix.preScale(mScaleX, mScaleY);
        }
        synchronized (this) {
            if (mIsStandby) {
                // 待命状态下，鸟上下来回飞动，并不需要处理旋转变换
                mMatrix.postTranslate(mBound.left, mBound.top); // 平移到鸟现在的位置
                mBound.offset(0, mSpeedY); // 更新鸟的位置
                // 当鸟处于中间位置时，速度最快，这时需要改变加速方向
                if (mSpeedY == MAX_RISE_SPEED_Y_STANDBY) {
                    mAccelY = FALL_ACCEL_Y_STANDBY;
                } else if (mSpeedY == -MAX_RISE_SPEED_Y_STANDBY) {
                    mAccelY = -FALL_ACCEL_Y_STANDBY;
                }
                mSpeedY += mAccelY;  // 更新速度
            } else {
                // 在游戏状态下，需要对鸟的图片进行平移和旋转变换
                // 先在远点，饶鸟图片的中心进行旋转
                mMatrix.preRotate(mRotationAngle, mBound.width() / 2, mBound.height() / 2);
                // 在对图片做平移变换
                mMatrix.postTranslate(mBound.left, mBound.top);
                mBound.offset(0, mSpeedY); // 用速度更新位置
                mSpeedY += mAccelY; //用加速度更新速度
                if (mSpeedY == mAccelY) { // 表示小鸟由上升状态转入下落状态
                    calAngularSpeed(FALLING_MAX_ANGLE);
                }
                float angle = mRotationAngle + mAngularSpeed; // 更新角度
                if (angle >= RISING_MAX_ANGLE && angle <= FALLING_MAX_ANGLE) {
                    mRotationAngle = angle;
                }
            }
        }
        //  画鸟
        canvas.drawBitmap(skin, mMatrix, null);
    }
}
