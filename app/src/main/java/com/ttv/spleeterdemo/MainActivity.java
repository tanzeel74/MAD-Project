package com.ttv.spleeterdemo;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import com.ttv.spleeter.SpleeterSDK;
import com.ttv.spleeterdemo.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;

public class  MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    private Context context;
    private ActivityMainBinding binding;
    private File mLastFile;
    private int mProcessing = 0;
    private AudioTrack mAudioTrack = null;
    private int mBufferSize = 0;
    private String wavPath;

    //ProgressBar
    private int maxProgress = 0;
    private Handler progressBarHandler;
    private Runnable updateProgressRunnable;

    Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == 1){
                int progress = msg.arg1;
//                binding.txtProgress.setText(String.format("%d%%", progress));
            } else if(msg.what == 2){
                int progress = msg.arg1;
//                binding.txtPlayProgress.setText(String.format("Play: %d%%", progress));
            } else{
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle extra = getIntent().getExtras();
        wavPath = extra.get("wavPath").toString();
        String outPath = extra.get("outPath").toString();

        maxProgress = getMaxDuration(wavPath);
        if (maxProgress > 0) {
            int mint = maxProgress / 60;
            int sec = maxProgress % 60;
            binding.totalTime.setText(mint + ":" + sec);
        }

        binding.btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                builder.setTitle("Success")
                        .setMessage("Files Exported Successfully")
                        .create()
                        .show();
            }
        });

        int frameRate = 44100;
        int minBufferSize =
                AudioTrack.getMinBufferSize(
                        frameRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mBufferSize = (3 * (minBufferSize / 2)) & ~3;
        Log.e("TestEngine", "Audio minBufferSize = " + minBufferSize + " " + mBufferSize);

        mAudioTrack =
                new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        frameRate,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        mBufferSize,
                        AudioTrack.MODE_STREAM);

        binding.btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.btnOpen.setImageDrawable(AppCompatResources.getDrawable(MainActivity.this, R.drawable.ic_pause));
                mProcessing = 1;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SpleeterSDK.getInstance().process(wavPath, outPath);

                        mProcessing = 0;
                    }
                }).start();
                new ProgressTask().execute("", "", "");
            }
        });

        progressBarHandler = new Handler();
        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    int currentPosition = mAudioTrack.getPlaybackHeadPosition();
                    long elapsedTimeInMillis = (long) (currentPosition * 1000.0 / frameRate);
                    int elapsedTimeInSeconds = (int) (elapsedTimeInMillis / 1000);
                    long remainingTimeInSeconds = (minBufferSize - currentPosition) / frameRate;
                    Log.e(TAG, "run: current " + elapsedTimeInSeconds );

                    int mint = elapsedTimeInSeconds / 60;
                    int sec = elapsedTimeInSeconds % 60;
                    binding.passedTime.setText(mint + ":" + sec);

                    binding.slider.setProgress(elapsedTimeInSeconds);
                }
                progressBarHandler.postDelayed(this, 100); // Delay between updates
            }
        };


        binding.slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mAudioTrack.setPlaybackHeadPosition(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Called when the user starts interacting with the seek bar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Called when the user stops interacting with the seek bar
            }
        });


    }

    public int getMaxDuration(String filePath) {
        MediaMetadataRetriever retriever;
        try {
           retriever = new MediaMetadataRetriever();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
        retriever.setDataSource(filePath);

        String durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int duration = Integer.parseInt(durationString) / 1000;

        try {
            retriever.release();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return duration;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    public class ProgressTask extends AsyncTask<String, String, String> {
        private void setProgress(int progress) {
            Message message = new Message();
            message.what = 1;
            message.arg1 = progress;
            mHandler.sendMessage(message);
        }

        private void setPlayState(int progress) {
            Message message = new Message();
            message.what = 2;
            message.arg1 = progress;
            mHandler.sendMessage(message);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            binding.btnOpen.setEnabled(false);
            Log.e(TAG, "run: Max " + maxProgress );
            binding.slider.setMax(maxProgress);
            mAudioTrack.play();
            progressBarHandler.postDelayed(updateProgressRunnable, 100);
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                setProgress(0);
                setPlayState(0);

                int playSize = 0;
                int offset = 0;
                while(true) {
                    short[] playbuffer = new short[8192];
                    float[] stemRatio = new float[5];
                    stemRatio[0] = 2 * binding.seekBar1.getProgress() / (float)binding.seekBar1.getMax();
                    stemRatio[1] = 2 * binding.seekBar2.getProgress() / (float)binding.seekBar2.getMax();
                    stemRatio[2] = 2 * binding.seekBar3.getProgress() / (float)binding.seekBar3.getMax();
                    stemRatio[3] = 2 * binding.seekBar4.getProgress() / (float)binding.seekBar4.getMax();
                    stemRatio[4] = 2 * binding.seekBar5.getProgress() / (float)binding.seekBar5.getMax();

                    int ret = SpleeterSDK.getInstance().playbuffer(playbuffer, offset, stemRatio);
                    if(ret == 0) {
                        break;
                    } else if(ret < 0) {
                        Thread.sleep(30);
                    } else {
                        if(playSize == 0) {
                            playSize = SpleeterSDK.getInstance().playsize();
                        }

                        offset += ret;
                        mAudioTrack.write(playbuffer, 0, playbuffer.length);
                        Log.e("TestEngine", "write " + playbuffer.length);

                        int progress = SpleeterSDK.getInstance().progress();

                        setProgress(progress);
                        setPlayState((offset / 4) * 100 / playSize);
                    }
                }

                setProgress(100);
                setPlayState(100);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mAudioTrack.stop();

            float[] stemRatio = new float[5];
            stemRatio[0] = 2 * binding.seekBar1.getProgress() / (float)binding.seekBar1.getMax();
            stemRatio[1] = 2 * binding.seekBar2.getProgress() / (float)binding.seekBar2.getMax();
            stemRatio[2] = 2 * binding.seekBar3.getProgress() / (float)binding.seekBar3.getMax();
            stemRatio[3] = 2 * binding.seekBar4.getProgress() / (float)binding.seekBar4.getMax();
            stemRatio[4] = 2 * binding.seekBar5.getProgress() / (float)binding.seekBar5.getMax();

//            SpleeterSDK.getInstance().saveAllStem("/mnt/sdcard/split");
//            SpleeterSDK.getInstance().saveOne("/mnt/sdcard/one.wav", stemRatio);

            binding.btnOpen.setEnabled(true);
            Toast.makeText(context, "Processing done!", Toast.LENGTH_SHORT).show();
        }
    }
}