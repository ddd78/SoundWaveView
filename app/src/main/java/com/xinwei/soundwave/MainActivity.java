package com.xinwei.soundwave;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.xinwei.soundwave.view.SoundWaveView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private SoundWaveView mSoundWaveView;

    private Button mChangeBtn;

    private boolean mIsStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initView() {
        mChangeBtn = (Button) findViewById(R.id.change_btn);
        mSoundWaveView = (SoundWaveView) findViewById(R.id.sound_wave_view);

        mChangeBtn.setOnClickListener(mClickListener);
    }

    private void initData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (null == mSoundWaveView) {
                        break;
                    }

                    Random random = new Random();
                    mSoundWaveView.addData(random.nextInt(100));

                    try {
                        Thread.sleep(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.change_btn:
                    mIsStop = !mIsStop;
                    if (mIsStop) {
                        mSoundWaveView.stop();
                        mChangeBtn.setText("开始");
                    } else {
                        mSoundWaveView.start();
                        mChangeBtn.setText("暂停");
                    }
                    break;
                default:
                    break;
            }
        }
    };
}
