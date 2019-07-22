package com.xinwei.soundwave;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.xinwei.soundwave.view.SoundWaveView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private SoundWaveView mSoundWaveView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initView() {
        mSoundWaveView = (SoundWaveView) findViewById(R.id.sound_wave_surface_view);
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

                    }
                }
            }
        }).start();
    }
}
