package com.xinwei.soundwave.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.LinkedBlockingQueue;


/**
 * 声波动画
 * Created by xinwei on 2019/2/28
 */
public class SoundWaveSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "SoundWaveSurfaceView";

    private static final int SHORT_MAX = 2000;//用于计算音量大小 max = 32768

    private static final int SLEEP_TIME = 30;

    public static LinkedBlockingQueue<Integer> mVolumeQueue = new LinkedBlockingQueue<>(100);//声波数据

    private final Object mSurfaceLock = new Object();

    private DrawThread mThread;

    private Paint mPaintA, mPaintB;

    private static float WAVE_K;
    private static float WAVE_AMPLITUDE;
    private static float WAVE_OMEGA;
    private float mWaveA;
    private long mBeginTime;
    private int mHeight;

    public SoundWaveSurfaceView(Context context) {
        super(context);
        init();
    }

    public SoundWaveSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SoundWaveSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaintA = new Paint();
        mPaintA.setStrokeWidth(3);
        mPaintA.setAntiAlias(true);
        mPaintA.setColor(Color.parseColor("#ff4285f4"));

        mPaintB = new Paint();
        mPaintB.setStrokeWidth(3);
        mPaintB.setAntiAlias(true);
        mPaintB.setColor(Color.parseColor("#804285f4"));

        getHolder().addCallback(this);
    }

    public void start() {
        Log.d(TAG, "start()");
        synchronized (mSurfaceLock) { //这里需要加锁，否则doDraw中有可能会crash
            if (null != mThread) {
                mThread.setRun(true);
            }
        }
    }

    public void stop() {
        Log.d(TAG, "stop()");
        synchronized (mSurfaceLock) {
            mWaveA = WAVE_AMPLITUDE;
            if (null != mThread) {
                mThread.setRun(false);
            }
        }
    }

    public void addData(int volume) {
        mVolumeQueue.offer(volume);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated()");
        mBeginTime = System.currentTimeMillis();

        mThread = new DrawThread(holder);

        mThread.setRun(true);
        mThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.d(TAG, "surfaceChanged()");
        //这里可以获取SurfaceView的宽高等信息
        WAVE_K = 0.02f;
        WAVE_AMPLITUDE = getHeight() / 2f - 5;
        WAVE_OMEGA = 0.0025f;
        mHeight = height;
        mWaveA = WAVE_AMPLITUDE;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed()");
        synchronized (mSurfaceLock) { //这里需要加锁，否则doDraw中有可能会crash
            mThread.setRun(false);
            mThread.setStop();
        }
    }

    private void doDraw(Canvas canvas) {
        if (null == canvas) {
            return;
        }

        Integer volume = mVolumeQueue.poll();
        //DebugLog.d(TAG, "doDraw() volume = " + volume);
        if (null != volume) {
            if (volume > SHORT_MAX) {
                volume = SHORT_MAX;
            }
            mWaveA = (mWaveA + (float) (WAVE_AMPLITUDE * (0.2 + 0.8 * volume / SHORT_MAX))) / 2f;
        }

        double deltaT = ((System.currentTimeMillis() - mBeginTime)) * WAVE_OMEGA;

        canvas.drawColor(Color.WHITE);

        float lastTopY = 0;
        float lastButtonY = 0;
        for (int x = 0; x < getWidth() + 20; x = x + 20) {
            float topY = (float) (mWaveA * Math.sin(deltaT - WAVE_K * x));
            topY = topY + mHeight / 2f;
            float buttonY = mHeight - topY;

            canvas.drawLine(x - 20, lastTopY, x, topY, mPaintA);
            canvas.drawLine(x - 20, lastButtonY, x, buttonY, mPaintB);

            lastTopY = topY;
            lastButtonY = buttonY;
        }
    }

    private class DrawThread extends Thread {
        private SurfaceHolder mHolder;
        private boolean mIsRun = false;
        private boolean mIsStop = false;

        public DrawThread(SurfaceHolder holder) {
            super(TAG);
            mHolder = holder;
        }

        @Override
        public void run() {
            while (!mIsStop) {
                synchronized (mSurfaceLock) {
                    if (mIsRun) {
                        try {
                            Canvas canvas = mHolder.lockCanvas();
                            if (canvas != null) {
                                doDraw(canvas); //这里做真正绘制的事情
                                mHolder.unlockCanvasAndPost(canvas);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }

        public void setRun(boolean isRun) {
            this.mIsRun = isRun;
        }

        public void setStop() {
            this.mIsStop = true;
        }
    }
}
