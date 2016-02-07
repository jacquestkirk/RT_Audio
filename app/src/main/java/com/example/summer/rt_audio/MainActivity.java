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

import android.os.Handler;
import java.util.logging.LogRecord;

// thanks to http://stackoverflow.com/questions/8499042/android-audiorecord-example

public class MainActivity extends AppCompatActivity {
    private AudioRx receiver;
    private AudioTx transmitter;

    private EditText sumSqEditText;
    private EditText logEditText;

    private static long updateTextBoxInterval_ms = 100;
    private Handler editTextUpdateHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Set up EditTexts
        sumSqEditText = (EditText) findViewById(R.id.sumSqEditText);
        logEditText = (EditText) findViewById(R.id.logEditText);

        sumSqEditText.setText("Hello");//String.format("%.2f", sound_level));
        logEditText.setText("world");

        //Set up handler to update text boxes
        editTextUpdateHandler = new Handler();


        //Set up Rx and Tx
        receiver = new AudioRx();
        transmitter = new AudioTx();


    }

    private void sandbox()
    {
        //processData();
    }


    public void goButtonClick(View view)
    {
        if(receiver.getRecordingState())
        {
            receiver.stopRx();
        }
        else{
            receiver.startRx();
            editTextUpdateHandler.postDelayed(updateTextBoxes, updateTextBoxInterval_ms);
        }

    }

    public void txButtonClick (View view)
    {
        byte[] txPacket = {1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,1,1,0,0,1,1,0,1,1};

        transmitter.setBaudRate_Hz(10);
        transmitter.modulatePacket("hi");
        transmitter.playPacket();

    }


    //repetative task that updates text boxes

    private Runnable updateTextBoxes = new Runnable() {
        @Override
        public void run() {
            //Get sound level from receiver
            float current_sound_level = receiver.getSoundLevel();
            //update textboxes
            sumSqEditText.setText(String.format("%f2", current_sound_level));
            logEditText.setText(String.format("%f2", Math.log10(current_sound_level)));

            //if we're still recording, schedule this task again
            if (receiver.getRecordingState())
            {
                editTextUpdateHandler.postDelayed(updateTextBoxes,updateTextBoxInterval_ms);
            }
        }
    };





}

