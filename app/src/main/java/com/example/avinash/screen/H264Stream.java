package com.example.avinash.screen;

import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by AVINASH on 9/4/2016.
 */
public class H264Stream extends VideoStream {
    public final static String TAG = "H264Stream";

    private Semaphore mLock = new Semaphore(0);
    private MP4Config mConfig;
    //public static MediaRecorder mMediaRecorder;
    public static MediaProjection mMediaProjection=null;
    public static final int DISPLAY_WIDTH = 320;
    public static final int DISPLAY_HEIGHT = 480;
    public static final int framerate = 30;
    public static MP4Config config;
    public static String profileid=null,sps=null,pps=null;
    protected SharedPreferences mSettings = null;
    protected int mVideoEncoder;

    public static String TESTFILE;

    public H264Stream() {
        mMimeType = "video/avc";
        mVideoEncoder = MediaRecorder.VideoEncoder.H264;
        mPacketizer = new H264Packetizer();
    }

    public synchronized String getSessionDescription() throws IllegalStateException {
        if (mConfig == null) throw new IllegalStateException("You need to call configure() first !");
        return "m=video "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
                "a=rtpmap:96 H264/90000\r\n" +
                "a=fmtp:96 packetization-mode=1;profile-level-id="+mConfig.getProfileLevel()+";sprop-parameter-sets="+mConfig.getB64SPS()+","+mConfig.getB64PPS()+";\r\n";

    }

    public synchronized void start() throws IllegalStateException, IOException {
        if (!mStreaming) {
            configure();
            byte[] pps = Base64.decode(mConfig.getB64PPS(), Base64.NO_WRAP);
            byte[] sps = Base64.decode(mConfig.getB64SPS(), Base64.NO_WRAP);
            ((H264Packetizer)mPacketizer).setStreamParameters(pps, sps);
            super.start();
        }
    }

    public synchronized void configure() throws IllegalStateException, IOException {
        super.configure();
       // mMode = mRequestedMode;
        //mQuality = mRequestedQuality.clone();
        mConfig = testH264();
    }
    private MP4Config testH264() throws IllegalStateException, IOException {
        return testMediaCodecAPI();
        // return testMediaRecorderAPI();//////////////////****changes1/////////////////////////////////////////////////////////
    }

    private MP4Config testMediaCodecAPI() throws RuntimeException, IOException {
        try {
            //EncoderDebugger debugger = EncoderDebugger.debug(mSettings, DISPLAY_WIDTH, DISPLAY_HEIGHT);/////prob///////
            Encoder e=new Encoder();
            //e.startRecording();
            Log.d("pps and sps me aaya","yessssssssssssss");
            return new MP4Config(Encoder.mSPS, Encoder.mPPS);
            //return null;
            //return new MP4Config("J0IAFKaCxMQ", "KM48gA");
        } catch (Exception e) {

            Log.e(TAG,"Resolution not supported with the MediaCodec API, we fallback on the old streamign method.");
            return testH264();
        }
    }

    private MP4Config testMediaRecorderAPI() throws RuntimeException, IOException {

         config = new MP4Config(TESTFILE);

        return config;

    }

}
