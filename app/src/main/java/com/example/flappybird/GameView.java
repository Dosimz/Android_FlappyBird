package com.example.flappybird;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {
    public static int STAGE_READY = 1;
    public static int STAGE_PLAY = 2;
    public static int STAGE_BIRD_FALLING = 3;
    public static int STAGE_OVER = 4;

    private Bird mBird;
    private BirdWorld mBirdWorld;
    private List<Bitmap[]> mListBirdsSkin;
    private List<Bitmap[]> mListPipesSkin;
    private List<Bitmap> mListSkySkin;
    private Bitmap mGroundSkin;
    private Matrix mMatrix;
    private boolean mIsRunning;
    private int mState;

    private SoundPool mSoundPool;
    private Map<String,Integer> mSoundMap;
    private Paint mPaint;

    private GestureDetector mGestureDetector;
    public GameView(Context context, AttributeSet attrs){
        super(context, attrs);
    // 第二个参数表示 手势监听器的实例
        mGestureDetector = new GestureDetector(getContext(), new
                GameGestureDetector());
        mPaint = new Paint();
        mPaint.setStrokeWidth(6);

        mMatrix = new Matrix();
        getHolder().addCallback(this);
        loadSoundPool();
    }

    private Rect calcBirdShotBound() {
        Bitmap birdSkin = mListBirdsSkin.get(0)[0];
        Rect bound = new Rect();
        bound.set(getWidth() / 3 - birdSkin.getWidth() / 2, getHeight() / 2 - birdSkin.getHeight() /2 ,
                getWidth() / 3 + birdSkin.getWidth() / 2, getHeight() / 2 + birdSkin.getHeight() / 2);
        return bound;
    }

    private Rect calcBirdInitBound() {
        Bitmap birdSkin = mListBirdsSkin.get(0)[0];
        Rect bound = new Rect();
        bound.set(getWidth() / 2 - birdSkin.getWidth() / 2, getHeight() / 2 - birdSkin.getHeight() /2 ,
                getWidth() / 2 + birdSkin.getWidth() / 2, getHeight() / 2 + birdSkin.getHeight() / 2);
        return bound;
    }

    private void loadSoundPool() {
        mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        AssetManager am = getContext().getAssets();
        mSoundMap = new HashMap<>();
        try {
            mSoundMap.put("Die", mSoundPool.load(am.openFd("sound/Die.wav"), 1));
            mSoundMap.put("Hit", mSoundPool.load(am.openFd("sound/Hit.wav"), 1));
            mSoundMap.put("Point", mSoundPool.load(am.openFd("sound/Point.wav"), 1));
            mSoundMap.put("Swooshing", mSoundPool.load(am.openFd("sound/Swooshing.wav"), 1));
            mSoundMap.put("Wing", mSoundPool.load(am.openFd("sound/Wing.wav"), 1));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadBackgroundSkin() {
        mListSkySkin = new ArrayList<>();
        Bitmap skyOrigin, skyScale, sky;
        skyOrigin = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.bg_day);
        skyScale = Bitmap.createScaledBitmap(skyOrigin, getWidth(), getHeight(), false);
        sky = Bitmap.createBitmap(skyScale, 0, 0, skyScale.getWidth(), getHeight() * 4 / 5);
        mListSkySkin.add(sky);
        skyOrigin.recycle();
        skyScale.recycle();

        skyOrigin = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.bg_night);
        skyScale = Bitmap.createScaledBitmap(skyOrigin, getWidth(), getHeight(), false);
        sky = Bitmap.createBitmap(skyScale, 0, 0, skyScale.getWidth(), getHeight() * 4 / 5);
        mListSkySkin.add(sky);
        skyOrigin.recycle();
        skyScale.recycle();

        Bitmap groundOrigin;
        groundOrigin = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.land);
        mGroundSkin = Bitmap.createScaledBitmap(groundOrigin, getWidth(),
                getHeight() * 1 / 5, false);
        groundOrigin.recycle();
    }

    private void loadBirdsSkin() {
        Bitmap[] birds = null;
        Bitmap bitmap = null;
        int width = getWidth() / 6;
        int height = getHeight() * 3 / 32;

        mListBirdsSkin = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            birds = new Bitmap[3];
            for (int j = 0; j < 3; j++) {
                bitmap = BitmapFactory.decodeResource(getContext().getResources(),
                        R.drawable.bird0_0 + i * 3 + j);
                birds[j] = Bitmap.createScaledBitmap(bitmap, width, height, false);
                bitmap.recycle();
            }
            mListBirdsSkin.add(birds);
        }
    }

    private void loadPipesSkin() {
        Bitmap[] pipes = null;
        Bitmap bitmap = null;
        int width = getWidth() * 13 / 72;
        int height = getHeight() * 5 / 8;

        mListPipesSkin = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            pipes = new Bitmap[2];
            for (int j = 0; j < 2; j++) {
                bitmap = BitmapFactory.decodeResource(getContext().getResources(),
                        R.drawable.pipe2_down + i * 2 + j);
                pipes[j] = Bitmap.createScaledBitmap(bitmap, width, height, false);
                bitmap.recycle();
            }
            mListPipesSkin.add(pipes);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mBirdWorld.draw(canvas);
        mBird.draw(canvas);
    }

    @Override
    public void run() {
        while (mIsRunning) {
            Canvas canvas = getHolder().lockCanvas();
            draw(canvas);
            if (mState == STAGE_PLAY) {
                if (mBirdWorld.isBirdCrash(mBird)) {
                    mSoundPool.play(mSoundMap.get("Hit"), 1f, 1f, 1, 0, 1f);
                    if (mBirdWorld.getCrashType() == BirdWorld.CRASH_TYPE_GROUND) {
                        mState = STAGE_OVER;
                    } else {
                        mState = STAGE_BIRD_FALLING;
                        mSoundPool.play(mSoundMap.get("Die"), 1f, 1f, 1, 0, 1f);
                    }
                } else if (mBirdWorld.hasPassPipe(mBird)) {
                    mSoundPool.play(mSoundMap.get("Point"), 1f, 1f, 1, 0, 1f);
                }
            } else if (mState == STAGE_BIRD_FALLING) {
                if (mBirdWorld.isBirdCrash(mBird) &&
                        mBirdWorld.getCrashType() == BirdWorld.CRASH_TYPE_GROUND) {
                    mState = STAGE_OVER;
                }
            }

            getHolder().unlockCanvasAndPost(canvas);
            sleep(50);
        }
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        loadBirdsSkin();
        loadPipesSkin();
        loadBackgroundSkin();

        Rect bound = calcBirdInitBound();
        mBird = new Bird().setBound(bound).setMatrix(mMatrix)
                .setBirdsSkin(mListBirdsSkin.get(0));
        mBird.makeStandby();

        mBirdWorld = new BirdWorld().setBound(new Rect(0, 0, getWidth(), getHeight()))
                .setSkySkin(mListSkySkin.get(0)).setGroundSkin(mGroundSkin)
                .setPipesSkin(mListPipesSkin.get(1));
        mBirdWorld.makeStandby();

        mIsRunning = true;
        mState = STAGE_READY;
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mIsRunning = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return true;
    }
    private int mBirdSkinIndex = 0;
    private class GameGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mIsRunning) {
                if (mState == STAGE_READY) {
                    mBirdWorld.roll();
                    Rect bound = calcBirdShotBound();
                    synchronized (mBird) {
                        mBird.setBound(bound).shot();
                    }
                    mState = STAGE_PLAY;
                } else if (mState == STAGE_PLAY) {
                    mBird.shot();
                    int id = mSoundMap.get("Wing");
                    mSoundPool.play(id, 1f, 1f, 1, 0, 1f);
                } else if (mState == STAGE_OVER) {
                    Rect bound = calcBirdInitBound();
                    mBird.setBound(bound).setMatrix(mMatrix).setBirdsSkin(mListBirdsSkin.get((++mBirdSkinIndex) % 3));
                    mBird.makeStandby();
                    mBirdWorld.makeStandby();
                    mState = STAGE_READY;
                }
            }
            return true;
        }
    }


}


