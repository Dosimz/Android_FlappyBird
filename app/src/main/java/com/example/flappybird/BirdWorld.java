package com.example.flappybird;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;

import java.nio.channels.Pipe;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class BirdWorld {
    public static final int DEFAULT_ROLLING_SPEED = 30;
    public static final int CRASH_DETECT_PADDING = 20;
    public static final int CRASH_TYPE_GROUND = 1;
    public static final int CRASH_TYPE_PIPE = 2;
    /**
     * 近景（地面）滚动速度是远景（天空）的8倍
     */

    private static final int SPEED_SCALE = 8;

    public Rect mBound; //游戏背景世界的边界
    private int mGroundTop; //记录地面位置 (设定为底部 1/5 的位置)

    private Bitmap mSkySkin; //天空 (远景) 背景
    private Bitmap mGroundSkin; // 地面 (近景) 背景

    private Bitmap[] mPipesSkin; // 水管皮肤

    private List<PipePair> mTemplatePipeList; //水管的模板列表, 4 种可能的水管位置
    private Queue<PipePair> mPipePairQueue; //当前出现的水管列表

    private int mRollingSpeed; //画面滚动的速度
    private boolean mIsStandby; //是否处于待命状态

    private int mNextPipeFrameCount; //用于计算下一个水管出现的帧数
    private int mFrameCount; // 当前帧数记录

    private boolean mIsQuiet;
    private int mCrashType; // 碰撞

    /*
    水管的描述
    游戏中的水管都是上下成对出现的,由于涉及到碰撞的判断,描述水管时应该关心,中
    间空隙的位置和长度.
     */

    // Java
    private class PipePair implements Cloneable {
        Rect bound; //水管的边界
        private int downBottom; //朝下的水管底部的位置
        private int upTop; //朝上的水管顶部的位置

        PipePair() {
            bound = new Rect(mBound.right, mBound.top,
                    mBound.right + mPipesSkin[0].getWidth(), mGroundTop);
        }

        @Override
        protected Object clone() { // 重写水管类的克隆方法
            PipePair pp = null;
            try {
                pp = (PipePair)super.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            // 如果直接调用 Object.clone 那么水管类中的引用类型成员将不会得到 深复制
            pp.bound = new Rect(bound);
            return pp;
        }
        // 设置朝下的水管及水管长度
        PipePair setDownBottom(int downBottom) {
            this.downBottom = downBottom;
            return this;
        }
        // 设置朝上的水管和水管长度
        PipePair setUpTop(int upTop) {
            this.upTop = upTop;
            return this;
        }
        // 移动水管的位置，速度就是地面滚动的速度
        void roll() {
            bound.offset(-mRollingSpeed, 0);
        }

        // 分别画出上下水管
        void draw(Canvas canvas) {
            canvas.save(); //保持画布当前的状态
            canvas.clipRect(bound); //在画布上剪出水管的区域进行绘制,这样不会影响到其他地方
            canvas.drawBitmap(mPipesSkin[0], bound.left,
                    downBottom - mPipesSkin[0].getHeight(), null);

            canvas.drawBitmap(mPipesSkin[1], bound.left,
                    upTop, null);
            canvas.restore(); //恢复画布之前状态
        }
    }
    // 获取碰撞值的方法
    public int getCrashType() {
        return mCrashType;
    }

    // 检测鸟是否成功过管
    public boolean hasPassPipe(Bird bird) {
        boolean isPass = false;
        for (PipePair pp : mPipePairQueue) {
            // 通过判断     鸟的左边在X轴的位置 大于 水管的右边在X轴的位置 &&
            //                         鸟的左边在X轴的位置 小于等于 水管的右边在X轴的位置加上画面滚动速度
            if (bird.getBound().left > pp.bound.right && bird.getBound().left <= pp.bound.right + mRollingSpeed) {
                isPass = true;
            }
        }
        return isPass;
    }

    public BirdWorld(){
        mRollingSpeed = DEFAULT_ROLLING_SPEED;
        mTemplatePipeList = new ArrayList<>();
        mPipePairQueue = new LinkedList<>();
    }
    // 设置背景的在屏幕上位置
    public BirdWorld setBound(Rect bound) {
        mBound = bound;
        mGroundTop = mBound.top + mBound.height() * 4/5;  // 地面的位置，占整个游戏背景 4/5
        return this;
    }
    // 设置天空的皮肤
    public BirdWorld setSkySkin(Bitmap skySkin) {
        mSkySkin = skySkin;
        return this;
    }
    // 设置地面皮肤
    public BirdWorld setGroundSkin(Bitmap groundSkin){
        mGroundSkin = groundSkin;
        return this;
    }
    // 设置水管皮肤
    public BirdWorld setPipesSkin(Bitmap[] skins) {
        mPipesSkin = skins;
        return this;
    }

    // 设置滚动速度
//    public BirdWorld setRollingSpeed(int rollingSpeed) {
//        mRollingSpeed = rollingSpeed;
//        return this;
//    }
    /*
    水管在游戏中是会不断产生的,如何高效的生成水管呢?
    创建一个水管的模板列表,事先创建所有可能形状的水管,在每次需要新水管时从模板列表中随机拿出一个,
    来设置新水管的形状.
    水管从屏幕消失了,还可以回收利用当作下一个新出现的水管。
     */

    // 生成水管的模板列表
    public void genTemplatePipeList() {
        // 背景的顶部距边框的距离 + 背景的高度 / 10
        int top = mBound.top + mBound.height()/10;
        // 设置上下管道的间距
        int space = mBound.height() * 3/10;
        int step = mBound.height() / 10;
        mTemplatePipeList.clear();
        // 在循环中构建上下水管实例
        while (top + space < mGroundTop) {
            PipePair pp = new PipePair().setDownBottom(top).setUpTop(top + space);
            mTemplatePipeList.add(pp);
            top += step;
        }
    }
    // 产生新的水管
    private void genPipePair() {
        PipePair pp = null;
        if (mTemplatePipeList.isEmpty()) {
            genTemplatePipeList();
        }
        // 获得一个随机的水管样式
        PipePair temp = mTemplatePipeList.get((int) (Math.random() *
                mTemplatePipeList.size()));
        if(!mPipePairQueue.isEmpty()) {
            // 如果水管队列有水管， 从队列头取得一个水管。
            PipePair tmp = mPipePairQueue.peek();
            // 并判断是否已经在屏幕中不显示
            if (tmp.bound.right < 0) {
                // 如果是，则取出这个水管并设置水管样式，并用作新水管
                pp = mPipePairQueue.poll();
                pp.setDownBottom(temp.downBottom).setUpTop(temp.upTop);
                pp.bound.set(temp.bound);
            }
        }
        if (pp == null) { // 否则，克隆从随机得到的水管
            pp = (PipePair) temp.clone();
        }
        mPipePairQueue.offer(pp); // 把新的水管放入队列尾部
    }


    public void makeStandby() {
        mIsStandby = true;
        mFrameCount = 0;
    }
//    public boolean isStandby() {
//        return mIsStandby;
//    }
    public void roll() { //启动游戏时地调用,画面开始滚动
        mIsStandby = false;
        mNextPipeFrameCount = -1;
    }

    // 画面滚动 ---------------------->------------------>---------------
    public void draw(Canvas canvas){
        int skyLeft = mBound.left; //记录远景的左边界
        int groundLeft = mBound.left; //记录近景的左边界
        if (!mIsStandby) { //如果处于游戏状态
            int recycleFrameCount = mBound.width() / mRollingSpeed; //循环一次的帧数
            //地面帧数 （用来确定地面显示位置）
            int groundFrameCount = mFrameCount % recycleFrameCount;
            skyLeft -= mFrameCount * mRollingSpeed / SPEED_SCALE; // 更新天空的位置
            groundLeft -= groundFrameCount * mRollingSpeed; //更新地面左边的位置
            // 绘制背景图，从左边界开始往右移动
            canvas.drawBitmap(mSkySkin, skyLeft + mBound.width(), mBound.top, null);
            canvas.drawBitmap(mGroundSkin, groundLeft + mBound.width(), mGroundTop, null);
            // 绘制背景图，填补背景图中的空缺的部分
            canvas.drawBitmap(mSkySkin, skyLeft, mBound.top, null);
            canvas.drawBitmap(mGroundSkin, groundLeft, mGroundTop, null);
            // 绘制水管图
            for (PipePair pp : mPipePairQueue) {
                pp.draw(canvas);
            }

//            Log.d("yourTag", "mNextPipeFrameCount =" + mNextPipeFrameCount);
            //  游戏启动时为 -1
            //  水管的帧数
            if (mNextPipeFrameCount == -1) {
                // 让水管帧数 = 循环一次的帧数
                mNextPipeFrameCount = recycleFrameCount;
            }

            if (mFrameCount == mNextPipeFrameCount) {
                genPipePair();
                mNextPipeFrameCount += recycleFrameCount / 2;
                if (mNextPipeFrameCount >= (SPEED_SCALE * recycleFrameCount)) {
                    mNextPipeFrameCount -= (SPEED_SCALE * recycleFrameCount);
                }
            }

            mFrameCount++;
            for (PipePair pp : mPipePairQueue) {
                pp.roll();
            }

            if (mFrameCount == (SPEED_SCALE * recycleFrameCount)) {
                mFrameCount = 0;
            }
        } else {
            // 不动的背景
            canvas.drawBitmap(mSkySkin, skyLeft, mBound.top, null);
            canvas.drawBitmap(mGroundSkin, groundLeft, mGroundTop, null);
        }
    }
    // 判断鸟的碰撞
    public boolean isBirdCrash(Bird bird) {
        boolean crashed = false;
        // 如果 鸟的底部距上方背景边界的距离 - 距上方背景边界整个背景 4/5 的距离
        if (bird.getBound().bottom - mGroundTop > CRASH_DETECT_PADDING) {
            crashed = true;
            mCrashType = CRASH_TYPE_GROUND;
            Log.d("yourTag", "bird.getBound().bottom =" + bird.getBound().bottom);
            Log.d("yourTag", "mGroundTop =" + mGroundTop);
            bird.put2Death();  //撞在地面上，鸟就直接挂了
            mIsQuiet = true;    //表示背景不再动了
        } else {
            for (PipePair pp : mPipePairQueue) {
                if (pp.bound.left - bird.getBound().right > -CRASH_DETECT_PADDING ||
                        bird.getBound().left - pp.bound.right > -CRASH_DETECT_PADDING
                ) {
                    Log.d("shuiguan", "bird.getBound().bottom =" + bird.getBound().bottom);
                    Log.d("shuiguan", "mGroundTop =" + mGroundTop);
                    continue;
                }

                //...CRASH_TYPE_PIPE
                if (pp.downBottom - bird.getBound().top > CRASH_DETECT_PADDING ||
                        bird.getBound().bottom - pp.upTop > CRASH_DETECT_PADDING) {
                    crashed = true;
                    mCrashType = CRASH_TYPE_PIPE;
                    mIsQuiet = true;
                    break;
                }
            }
        }
        return crashed;
    }
}

