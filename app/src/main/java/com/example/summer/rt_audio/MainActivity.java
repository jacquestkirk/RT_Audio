package com.example.summer.rt_audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


// thanks to http://stackoverflow.com/questions/8499042/android-audiorecord-example

public class MainActivity extends AppCompatActivity {
    private AudioRecord recorder;
    private boolean isRecording = false;
    private Thread recordingThread;

    private int BufferSize_words = 2048;
    private int WordSize_bytes = 2;

    private EditText sumSqEditText;
    private EditText logEditText;

    private float sound_level;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int sampleRate_Hz =8000;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;//16 bit encoding because of 2 byte words
        int bufferSize_bytes = WordSize_bytes * BufferSize_words;


        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC , sampleRate_Hz, channelConfig, audioEncoding, bufferSize_bytes);
        recorder.setRecordPositionUpdateListener(updateListener);
        recorder.setPositionNotificationPeriod(BufferSize_words);
        int recorderState = recorder.getState();

        if(recorderState==AudioRecord.STATE_UNINITIALIZED)
        {
            //AudioRecord did not construct properly
            TextView myTextView = (TextView) findViewById(R.id.textView);
            myTextView.setText("Could not start AudioRecord: Do not press the button or you will crash!!!");
        }

        //Set up EditTexts

        sumSqEditText = (EditText) findViewById(R.id.sumSqEditText);
        logEditText = (EditText) findViewById(R.id.logEditText);

        sumSqEditText.setText("Hello");//String.format("%.2f", sound_level));
        logEditText.setText("workd");

        getValidSampleRates();

    }

    public void getValidSampleRates() {
        for (int rate : new int[] {8000, 11025, 16000, 22050, 44100}) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                // buffer size is valid, Sample rate supported

            }
        }
    }

    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener(){

        @Override
        public void onPeriodicNotification(AudioRecord recorder){
            processData();
        }

        @Override
        public void onMarkerReached(AudioRecord recorder)
        {}
    };


    private void sandbox()
    {
        processData();
    }


    public void goButtonClick(View view)
    {
        if(isRecording)
        {
            isRecording = false;
        }
        else{
            isRecording = true;
            startRec();
        }

    }

    private void startRec()
    {
        recorder.startRecording();

        //start a thread to record data
        //try this later, right now we just want to print results.
        /*recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                processData();
            }
        });

        recordingThread.start();
*/
        //sandbox();

        /*while(true)
        {

            try{
                Thread.sleep(100,0);
            }catch(InterruptedException e)
            {
                //I don't really give a shit if this is interrupted;
            }


            sumSqEditText.setText(String.format("%.2f", sound_level));
            sumSqEditText.setText(String.format("%.2f", Math.log10( sound_level)));


        }
        */
    }



    private void processData()
    {
        short[] buffDat = new short[BufferSize_words];

        //while (isRecording)
        //{
            recorder.read(buffDat, 0, BufferSize_words);

            float sound_level_calc= 0;

            for ( int i = 0; i <BufferSize_words; i++)
            {
                sound_level_calc += (Math.pow(buffDat[i],2)); //add up squares
            }

           sound_level = sound_level_calc; //push the newly calculated sound level to the class variable

            sumSqEditText.setText(String.format("%.2f", sound_level));
            logEditText.setText(String.format("%.2f", Math.log10( sound_level)));

            /*try{
                Thread.sleep(1000,0);
            }catch(InterruptedException e)
            {
                //I don't really give a shit if this is interrupted;
            }*/
        //}


    }

}

