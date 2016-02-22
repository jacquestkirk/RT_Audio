package com.example.summer.rt_audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.widget.TextView;


import com.example.summer.rt_audio.R;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Created by Summer on 1/31/2016.
 */
public class AudioRx {


    private final int DEFAULT_BUFFERSIZE_WORDS = 3584;
    private final int DEFAULT_WORDSIZE_BYTES = 2;
    private final int DEFAULT_SAMPRATE_HZ = 44100;
    private final int DEFAULT_CHANNELCONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int DEFAULT_AUDIOENCODEING = AudioFormat.ENCODING_PCM_16BIT;
    private final double DEFAULT_CENTERFREQ_HZ = 10000;
    private final double DEFAULT_DEVFREQ_HZ = 5000;
    private final double DEFAULT_BAUDRATE = 1000;

    private int wordSize_bytes;
    private int bufferSize_words;
    private int sampleRate_Hz;
    private int channelConfig;
    private int audioEncoding;//16 bit encoding because of 2 byte words
    private int bufferSize_bytes;
    private double centerFreq_Hz;
    private double devFreq_Hz;
    private double baudRate_Hz;

    /*private final double[] iqFilt= {                       0,//9kHz filter
                                    -0.0028,
                                    0.0000,
                                    0.0055,
                                    -0.0000,
                                    -0.0105,
                                    0.0000,
                                    0.0185,
                                    -0.0000,
                                    -0.0310,
                                    0.0000,
                                    0.0526,
                                    -0.0000,
                                    -0.0991,
                                    0.0001,
                                    0.3159,
                                    0.5000,
                                    0.3159,
                                    0.0001,
                                    -0.0991,
                                    -0.0000,
                                    0.0526,
                                    0.0000,
                                    -0.0310,
                                    -0.0000,
                                    0.0185,
                                    0.0000,
                                    -0.0105,
                                    -0.0000,
                                    0.0055,
                                    0.0000,
                                    -0.0028
                                    };*/

    private final double[] iqFilt = { //1kHz filter
                    -0.0000,
                    -0.0002,
                    -0.0002,
                    0.0008,
                    0.0021,
                    -0.0005,
                    -0.0069,
                    -0.0060,
                    0.0114,
                    0.0251,
                    -0.0017,
                    -0.0568,
                    -0.0511,
                    0.0888,
                    0.2965,
                    0.3976,
                    0.2965,
                    0.0888,
                    -0.0511,
                    -0.0568,
                    -0.0017,
                    0.0251,
                    0.0114,
                    -0.0060,
                    -0.0069,
                    -0.0005,
                    0.0021,
                    0.0008,
                    -0.0002,
                    -0.0002,
                    -0.0000};

    private final double[] freqFilt = {0.2,0.2,0.2,0.2,0.2};

    private AudioRecord recorder;

    private float sound_level;
    private boolean isRecording = false;
    private boolean firstRun = true;

    short[] buffer1;

    private AudioTrack debugAudio;

    public AudioRx()
    {

        wordSize_bytes = DEFAULT_WORDSIZE_BYTES; //Todo: Make this a funcition of audioEncoding instead of making this hard coded.
        bufferSize_words = DEFAULT_BUFFERSIZE_WORDS;

        sampleRate_Hz =DEFAULT_SAMPRATE_HZ;
        channelConfig = DEFAULT_CHANNELCONFIG;
        audioEncoding = DEFAULT_AUDIOENCODEING;
        bufferSize_bytes = wordSize_bytes * bufferSize_words;

        centerFreq_Hz = DEFAULT_CENTERFREQ_HZ;
        devFreq_Hz = DEFAULT_DEVFREQ_HZ;
        baudRate_Hz = DEFAULT_DEVFREQ_HZ;

        //Configure AudioRecord Object
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC , sampleRate_Hz, channelConfig, audioEncoding, bufferSize_bytes);
        recorder.setRecordPositionUpdateListener(updateListener);
        recorder.setPositionNotificationPeriod(bufferSize_words); //Todo: Figure out what the units of frames is? That is the unit for the input to this function
        //int junk = recorder.getMinBufferSize(sampleRate_Hz, channelConfig, audioEncoding);

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
        //To ensure that we capture the full packet, capture two buffers worth and cycle through

        short [] buffer2 = new short[bufferSize_words];
        recorder.read(buffer2, 0, bufferSize_words);

        if(firstRun) //if this is the first run, we only have one buffer so there is no point in continuing
        {
            buffer1 = buffer2; //store this buffer as buffer1
            firstRun = false;
            return;
        }

        short [] rawData = ArrayUtils.addAll(buffer1, buffer2); //combine buffers together into one set of data

        short [] filtData = rawData; //This line is a placeholder for applying the Rx filter

        short [] demodData = new short[2*bufferSize_words];
        short [] result = new short[2*bufferSize_words];;
        demodulate(demodData,result);
        //Slice()

        buffer1 = buffer2;

    }

    private void demodulate(short[] indat, short[] outdat)
    {
        short [] debugDat = new short[2*bufferSize_words];

        //instantiate iq data
        double [] idat = new double[2*bufferSize_words + iqFilt.length-1];
        double [] qdat = new double[2*bufferSize_words + iqFilt.length-1];
        double [] idat_filt = new double[2*bufferSize_words];
        double [] qdat_filt = new double[2*bufferSize_words];


        //calculate iq data
        for ( int i = 0; i < iqFilt.length-1; i++) //prepend zeros to iq data
        {
            idat[i] = 0;
            qdat[i] = 0;
        }
        for( int i = 0; i< 2*bufferSize_words; i++)
        {
            //generate iq data
            idat[i + iqFilt.length-1] = indat[i] *Math.cos(2*Math.PI*centerFreq_Hz/sampleRate_Hz * i);
            qdat[i + iqFilt.length-1] = indat[i] *-1 *Math.sin(2 * Math.PI * centerFreq_Hz / sampleRate_Hz * i);

            //convolve with filter to get filtered iq data
            idat_filt[i] = 0;
            qdat_filt[i] = 0;

            for( int k = 0; k< iqFilt.length; k++) {
                idat_filt[i] = idat_filt[i] + iqFilt[k] * idat[i + k];
                qdat_filt[i] = qdat_filt[i] + iqFilt[k] * qdat[i + k];
            }
        }

        //calculate phase
        double [] phase = new double[2*bufferSize_words];
        for (int i=0; i< 2*bufferSize_words; i++)
        {
            phase[i] = Math.atan(qdat_filt[i] / idat_filt[i]);

            //using sign of idat, convert to a number between -pi and pi

            if(idat_filt[i] < 0)
            {
                phase[i] += Math.PI;
            }
        }

        //calculate frequency
        double [] freq = new double[2*bufferSize_words];
        freq[0] = 0; //fill out the first value
        for(int i=1; i< 2*bufferSize_words; i++)
        {
            freq[i] = phase[i]-phase[i-1];
            freq[i] = freq[i]* sampleRate_Hz/(2*Math.PI);
        }

        //filter frequency
        double [] freq_filt = new double [2*bufferSize_words];
        for(int i=1; i< 2*bufferSize_words; i++)
        {
            freq_filt[i]=0;

            for (int k = 0; k < freqFilt.length; k++)
            {
                if(i+k >= 2*bufferSize_words-1) //deal with end condition
                {
                    continue;
                }

                freq_filt[i] = freq_filt[i] + freq[i+k] * freqFilt[k];
                debugDat [i] = (short) (10000 * (freq_filt[i] + 2));
            }
        }

        playDebug(debugDat, 3, 1.5);
    }



    private void processDataPower()
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

    public void playDebug(short[]debug_dat, double scale, double offset)
    {

        byte dataToSend[] = new byte[debug_dat.length *2];

        for (int i = 0; i<debug_dat.length; i++)
        {
            short val = (short) ((debug_dat[i]/3 + 1.5)*32767);
            dataToSend[2*i] = (byte) (val & 0x00ff);
            dataToSend[2*i+1] = (byte) ((val & 0xff00) >>> 8);
        }

        //Todo: double check what bufferSize_bytes actually does
        debugAudio = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate_Hz, AudioFormat.CHANNEL_OUT_MONO, audioEncoding, dataToSend.length, AudioTrack.MODE_STATIC);

        int generatorState = debugAudio.getState();
        if(generatorState== AudioTrack.STATE_UNINITIALIZED)
        {
            //Todo: Add an exception here showing that we were not able to initialize properly.
        }


        debugAudio.write(dataToSend, 0,dataToSend.length);
        debugAudio.play();
        //generator.stop();
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
