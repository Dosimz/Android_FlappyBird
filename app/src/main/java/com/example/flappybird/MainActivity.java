package com.example.flappybird;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;




public class MainActivity extends Activity {
    private GameView mGameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //      去除 AppBar 和工具栏，获得沉浸式体验,这一部分代码一定要写
        //                        在 setContentView() 前面，不然会闪退
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mGameView = new GameView(this, null);
        setContentView(mGameView);
    }
}
