package com.xinwei.soundwave.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.xinwei.soundwave.R;

import java.util.concurrent.LinkedBlockingQueue;


/**
 * 声波动画
 * Created by xinwei on 2019/2/28
 */
public class SoundWaveView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "SoundWaveView";

    private static final int DEFAULT_MAX_VOLUME = 100;  //默认最大声波值
    private static final int DEFAULT_LINE_WIDTH = 3;    //默认线条宽度

    private static final int WAVE_STYLE_DOUBLE = 0;//双线
    private static final int WAVE_STYLE_SINGLE = 1;//单线

    private static final int SLEEP_TIME = 30;

    private final Object mSurfaceLock = new Object();

    private LinkedBlockingQueue<Integer> mVolumeQueue = new LinkedBlockingQueue<>(100);//声波数据

    private int mMaxVolume = DEFAULT_MAX_VOLUME; //最大声波值

    private boolean mIsSingleLine;//是否为单线

    private DrawThread mThread;

    private Paint mPaintA, mPaintB;

    private static float WAVE_K;
    private static float WAVE_AMPLITUDE;
    private static float WAVE_OMEGA;
    private float mWaveA;
    private long mBeginTime;
    private int mHeight;

    public SoundWaveView(Context context) {
        this(context, null);
    }

    public SoundWaveView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SoundWaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.SoundWaveView);
        int lineWidth = typedArray.getDimensionPixelSize(R.styleable.SoundWaveView_lineWidth, DEFAULT_LINE_WIDTH);
        int lineTopColor = typedArray.getColor(R.styleable.SoundWaveView_lineTopColor, Color.BLUE);
        int lineBottomColor = typedArray.getColor(R.styleable.SoundWaveView_lineBottomColor, Color.GREEN);
        int index = typedArray.getInt(R.styleable.SoundWaveView_waveStyle, WAVE_STYLE_DOUBLE);
        mMaxVolume = typedArray.getInteger(R.styleable.SoundWaveView_maxVolume, DEFAULT_MAX_VOLUME);
        mIsSingleLine = (index == WAVE_STYLE_SINGLE);
        typedArray.recycle();

        mPaintA = new Paint();
        mPaintA.setStrokeWidth(lineWidth);
        mPaintA.setAntiAlias(true);
        mPaintA.setColor(lineTopColor);

        mPaintB = new Paint();
        mPaintB.setStrokeWidth(lineWidth);
        mPaintB.setAntiAlias(true);
        mPaintB.setColor(lineBottomColor);

        getHolder().addCallback(this);
    }

    /**
     * 开始动画
     */
    public void start() {
        Log.d(TAG, "start()");
        synchronized (mSurfaceLock) { //这里需要加锁，否则doDraw中有可能会crash
            if (null != mThread) {
                mThread.setRun(true);
            }
        }
    }

    /**
     * 停止动画
     */
    public void stop() {
        Log.d(TAG, "stop()");
        synchronized (mSurfaceLock) {
            mWaveA = WAVE_AMPLITUDE;
            if (null != mThread) {
                mThread.setRun(false);
            }
        }
    }

    /**
     * 更新数据
     * @param volume 声波值
     */
    public void addData(int volume) {
        mVolumeQueue.offer(volume);
    }

    /**
     * 设置声波最大值
     * @param maxVolume 最大声波值
     */
    public void setMaxVolume(int maxVolume) {
        mMaxVolume = maxVolume;
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
        WAVE_K = 0.02f;//控制振幅
        WAVE_OMEGA = 0.0025f;//控制移动速度
        WAVE_AMPLITUDE = getHeight() / 2f - 5;
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
            if (volume > mMaxVolume) {
                volume = mMaxVolume;
            }
            mWaveA = (mWaveA + (float) (WAVE_AMPLITUDE * (0.2 + 0.8 * volume / mMaxVolume))) / 2f;
        }

        double deltaT = ((System.currentTimeMillis() - mBeginTime)) * WAVE_OMEGA;

        canvas.drawColor(Color.WHITE);

        float lastTopY = 0;
        float lastButtonY = 0;
        //为降低绘制曲线时的计算量，由描点改为画线（每20个像素画条直线）
        for (int x = 0; x < getWidth() + 20; x = x + 20) {
            //画线1
            float topY = (float) (mWaveA * Math.sin(deltaT - WAVE_K * x));
            topY = topY + mHeight / 2f;
            canvas.drawLine(x - 20, lastTopY, x, topY, mPaintA);
            lastTopY = topY;

            //画线2
            if (!mIsSingleLine) {
                float buttonY = mHeight - topY;
                canvas.drawLine(x - 20, lastButtonY, x, buttonY, mPaintB);
                lastButtonY = buttonY;
            }
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
