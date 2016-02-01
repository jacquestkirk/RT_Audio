package com.example.summer.rt_audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.widget.TextView;

import com.example.summer.rt_audio.R;

/**
 * Created by Summer on 1/31/2016.
 */
public class AudioRx {


    private final int DEFAULT_BUFFERSIZE_WORDS = 2048;
    private final int DEFAULT_WORDSIZE_BYTES = 2;
    private final int DEFAULT_SAMPRATE_HZ = 8000;
    private final int DEFAULT_CHANNELCONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int DEFAULT_AUDIOENCODEING = AudioFormat.ENCODING_PCM_16BIT;

    private int wordSize_bytes;
    private int bufferSize_words;
    private int sampleRate_Hz;
    private int channelConfig;
    private int audioEncoding;//16 bit encoding because of 2 byte words
    private int bufferSize_bytes;


    private AudioRecord recorder;

    private float sound_level;
    private boolean isRecording = false;


    public AudioRx()
    {

        wordSize_bytes = DEFAULT_WORDSIZE_BYTES; //Todo: Make this a funcition of audioEncoding instead of making this hard coded.
        bufferSize_words = DEFAULT_BUFFERSIZE_WORDS;

        sampleRate_Hz =DEFAULT_SAMPRATE_HZ;
        channelConfig = DEFAULT_CHANNELCONFIG;
        audioEncoding = DEFAULT_AUDIOENCODEING;
        bufferSize_bytes = wordSize_bytes * bufferSize_words;

        //Configure AudioRecord Object
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC , sampleRate_Hz, channelConfig, audioEncoding, bufferSize_bytes);
        recorder.setRecordPositionUpdateListener(updateListener);
        recorder.setPositionNotificationPeriod(bufferSize_words); //Todo: Figure out what the units of frames is? That is the unit for the input to this function


        int recorderState = recorder.getState();
        if(recorderState==AudioRecord.STATE_UNINITIALIZED)
        {
            //Todo: Add an exception here showing that we were not able to initialize properly.
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

    private void processData()
    {
        short[] buffDat = new short[bufferSize_words];

        //while (isRecording)
        //{
        recorder.read(buffDat, 0, bufferSize_words);

        float sound_level_calc= 0;

        for ( int i = 0; i <bufferSize_words; i++)
        {
            sound_level_calc += (Math.pow(buffDat[i],2)); //add up squares
        }

        sound_level = sound_level_calc; //push the newly calculated sound level to the class variable

    }

    public float getSoundLevel()
    {
        return sound_level;
    }

    public void getValidSampleRates() {
        for (int rate : new int[] {8000, 11025, 16000, 22050, 44100}) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                // buffer size is valid, Sample rate supported

            }
        }
    }

    public void startRx()
    {
        isRecording = true;
        recorder.startRecording();
    }

    public void stopRx()
    {
        recorder.stop();
        isRecording = false;
    }

    public boolean getRecordingState()
    {
        return isRecording;
    }

}
