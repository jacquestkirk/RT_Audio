package com.example.summer.rt_audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

/**
 * Created by Summer on 1/31/2016.
 *
 * With help from ... http://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android
 */
public class AudioTx {

    private final float DEFAULT_CENTER_FREQ_HZ = 10500; //300;
    private final int DEFAULT_SAMPRATE_HZ = 44100; //8000;
    private final int DEFAULT_CHANNELCONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private final int DEFAULT_AUDIOENCODEING = AudioFormat.ENCODING_PCM_16BIT;
    private final int DEFAULT_BUFFERSIZE_WORDS = 2048;
    private final int DEFAULT_WORDSIZE_BYTES = 2;

    private final double DEFAULT_BAUD_RATE_Hz = 1000;//2;
    private final double DEFAULT_FREQ_DEV_Hz = 500;//10;
    private final byte[] DEFAULT_TRAINING_SEQUENCE ={0x55, 0x55};




    private float centerFreq_Hz;
    private int sampleRate_Hz;
    private int channelConfig;
    private int audioEncoding;//16 bit encoding because of 2 byte words
    private int wordSize_bytes;
    private int bufferSize_words;
    private int bufferSize_bytes;
    private double baud_rate_hz;
    private double freq_dev_hz;
    private byte[] training_sequence;

    private AudioTrack generator;
    private byte packet_byte[];
    private double packet[];


    public AudioTx()
    {

        sampleRate_Hz =DEFAULT_SAMPRATE_HZ;
        channelConfig = DEFAULT_CHANNELCONFIG;
        audioEncoding = DEFAULT_AUDIOENCODEING;
        centerFreq_Hz = DEFAULT_CENTER_FREQ_HZ;

        wordSize_bytes = DEFAULT_WORDSIZE_BYTES; //Todo: Make this a funcition of audioEncoding instead of making this hard coded.
        bufferSize_words = DEFAULT_BUFFERSIZE_WORDS;
        bufferSize_bytes = wordSize_bytes * bufferSize_words;

        baud_rate_hz = DEFAULT_BAUD_RATE_Hz;
        freq_dev_hz = DEFAULT_FREQ_DEV_Hz;


        training_sequence = DEFAULT_TRAINING_SEQUENCE;

        //initialize the packet
        //generateCwTone();


    }

    private void generateCwTone()
    {
        packet = new double[bufferSize_words];

        for( int i = 0; i<bufferSize_words; i++)
        {
            packet[i]= Math.sin(2*Math.PI*centerFreq_Hz/sampleRate_Hz * i);
        }
    }




    private void encodePacket()
    {
        bufferSize_bytes = bufferSize_words* wordSize_bytes;
        packet_byte = new byte[bufferSize_bytes];
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        for (int i = 0; i<bufferSize_words; i++)
        {
            short val = (short) (packet[i]*32767);
            packet_byte[2*i] = (byte) (val & 0x00ff);
            packet_byte[2*i+1] = (byte) ((val & 0xff00) >>> 8);
        }


        /*int idx = 0;
        for (double dVal : packet) {
            // scale to maximum amplitude
            short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            packet_byte[idx++] = (byte) (val & 0x00ff);
            packet_byte[idx++] = (byte) ((val & 0xff00) >>> 8);

        }*/

    }

    public void modulatePacket(String packet)
    {
        //todo: Add checks to make sure that Packets are the correct length.

        //Add training sequence to the beginning of the packet
        String training_sequence_str = new String(training_sequence);
        packet =  training_sequence_str + packet;

        int packet_len_bytes = packet.length();
        byte[] byte_packet = packet.getBytes();

        byte[] bit_packet = new byte[packet_len_bytes*8];



        for (int i=0; i < packet_len_bytes; i++)
        {
            //convert each bit into a new entry
            bit_packet[8*i+7]= (byte)(((int)byte_packet[i] & 0b00000001)>>0);
            bit_packet[8*i+6]= (byte)(((int)byte_packet[i] & 0b00000010)>>1);
            bit_packet[8*i+5]= (byte)(((int)byte_packet[i] & 0b00000100)>>2);
            bit_packet[8*i+4]= (byte)(((int)byte_packet[i] & 0b00001000)>>3);
            bit_packet[8*i+3]= (byte)(((int)byte_packet[i] & 0b00010000)>>4);
            bit_packet[8*i+2]= (byte)(((int)byte_packet[i] & 0b00100000)>>5);
            bit_packet[8*i+1]= (byte)(((int)byte_packet[i] & 0b01000000)>>6);
            bit_packet[8*i+0]= (byte)(((int)byte_packet[i] & 0b10000000)>>7);
        }

        modulatePacket(bit_packet, 8*packet_len_bytes);
    }


    public void modulatePacket(byte[] binary_data, int packet_len)
    {
        int samples_per_bit = (int)Math.round(sampleRate_Hz/baud_rate_hz);
        bufferSize_words= packet_len * samples_per_bit;

        double freq0_hz = centerFreq_Hz-freq_dev_hz;
        double freq1_hz = centerFreq_Hz+freq_dev_hz;


        packet = new double[bufferSize_words];

        //fill in the packet with generated sin waves.
        int current_index = 0;

        for(int i = 0; i<packet_len; i++)  //loop over each bit
        {//Todo: Add some error checking here to make sure that all the dimensions are correct

            //check value of the bit
            if(binary_data[i]==0)
            {
                appendSin(packet, freq0_hz, current_index, samples_per_bit);
            }
            else if (binary_data[i]==1)
            {
                appendSin(packet, freq1_hz, current_index, samples_per_bit);
            }
            else
            {
                //Todo: throw exception here
            }
            current_index += samples_per_bit;
        }
    }

    private void appendSin(double[] thePacket, double frequency_hz, int start_index, int samplesToWrite)
    {
        for( int i = 0; i< samplesToWrite; i++)
        {
            thePacket[i + start_index] = Math.sin(2*Math.PI*frequency_hz/sampleRate_Hz * i);
        }
    }

    public void playPacket()
    {
        encodePacket();
        //Todo: double check what bufferSize_bytes actually does
        generator = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate_Hz, channelConfig, audioEncoding, bufferSize_bytes, AudioTrack.MODE_STATIC);

        int generatorState = generator.getState();
        if(generatorState== AudioTrack.STATE_UNINITIALIZED)
        {
            //Todo: Add an exception here showing that we were not able to initialize properly.
        }


        generator.write(packet_byte, 0,bufferSize_bytes);
        generator.play();
        //generator.stop();
    }

    /// Setters and getters
    public void setCenterFreq_Hz(float newVal)
    {
        centerFreq_Hz = newVal;
    }
    public float getCenterFreq_Hz()
    {
        return centerFreq_Hz;
    }

    public void setBaudRate_Hz(double newVal)
    {
        baud_rate_hz = newVal;
    }
    public double getBaudRate_Hz()
    {
        return baud_rate_hz;
    }

    public void setFreqDev_Hz(double newVal)
    {
        freq_dev_hz = newVal;
    }
    public double getFreqDev_Hz()
    {
        return freq_dev_hz;
    }
}
