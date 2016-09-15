package com.example.avinash.screen;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by AVINASH on 9/6/2016.
 */
public class Encoder extends AppCompatActivity{

    public static boolean mMuxerStarted = false;
    public static Surface mInputSurface;
    public static MediaMuxer mMuxer;
    public static MediaCodec mVideoEncoder;
    public static int mTrackIndex = -1;
    public static byte[] mSPS, mPPS;
    private static String mB64PPS, mB64SPS;
    public static MediaProjection mMediaProjection2;

    public static final String VIDEO_MIME_TYPE = "video/avc";
    public static MediaCodec mVideoEncoder2;
    public static MediaCodec.BufferInfo mVideoBufferInfo;
    public static Surface mInputSurface2;
    //private EncodedFrameListener frameListener;
    //private MediaCodec mediaCodec;

    public static byte[] sps;
    public static byte[] pps;

    private final Handler mDrainHandler = new Handler(Looper.getMainLooper());
    private Runnable mDrainEncoderRunnable = new Runnable() {
        @Override
        public void run() {
            drainEncoder();
        }
    };

    public void startRecording() {
        Log.d("startrecording","ke andar");
        mMediaProjection2=MainActivity.mMediaProjection;
       mInputSurface2= prepareVideoEncoder();

        try {
            mMuxer = new MediaMuxer("/sdcard/video.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException ioe) {
            throw new RuntimeException("MediaMuxer creation failed", ioe);
        }

        mMediaProjection2.createVirtualDisplay("Recording Display",MainActivity.DISPLAY_WIDTH,
                MainActivity.DISPLAY_HEIGHT, MainActivity.mScreenDensity, 0 /* flags */, mInputSurface2,
                null /* callback */, null /* handler */);

        // Start the encoders
        drainEncoder();
    }

    public  Surface prepareVideoEncoder() {
        mVideoBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, 320, 480);
        int frameRate = 20; // 30 fps
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, MainActivity.BIT_RATE); // 6Mbps
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, frameRate);
        //format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
        try {
            mVideoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
            mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mVideoEncoder.createInputSurface();
            mVideoEncoder.start();
        } catch (IOException e) {
            releaseEncoders();
        }
        return mInputSurface;
    }

    public  boolean drainEncoder() {
        mDrainHandler.removeCallbacks(mDrainEncoderRunnable);
        while (true) {
            Log.d("count", "aaya");
            int bufferIndex = mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, 1000000/20);

            if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                try {
                    MediaFormat format = mVideoEncoder.getOutputFormat();//
                    ByteBuffer spsb = format.getByteBuffer("csd-0");//
                    ByteBuffer ppsb = format.getByteBuffer("csd-1");//
                    mSPS = new byte[spsb.capacity() - 4];             //
                    spsb.position(4);                               //
                    spsb.get(mSPS, 0, mSPS.length);                   //
                    mPPS = new byte[ppsb.capacity() - 4];             //
                    ppsb.position(4);                               //
                    ppsb.get(mPPS, 0, mPPS.length);
                    Log.d("sps and pps", mSPS.toString() + " and " + mPPS.toString());
                    mB64PPS = Base64.encodeToString(mPPS, 0, mPPS.length, Base64.NO_WRAP);
                    mB64SPS = Base64.encodeToString(mSPS, 0, mSPS.length, Base64.NO_WRAP);
                    Log.d("sps and pps", mB64SPS + " and " + mB64PPS.toString());
                    //
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // nothing available yet
                break;
            } else if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mTrackIndex >= 0) {
                    throw new RuntimeException("format changed twice");
                }
                mTrackIndex = mMuxer.addTrack(mVideoEncoder.getOutputFormat());
                if (!mMuxerStarted && mTrackIndex >= 0) {
                    mMuxer.start();
                    mMuxerStarted = true;
                    Log.d("muxer","started");
                }
            } else if (bufferIndex < 0) {
                // not sure what's going on, ignore it
            } else {
                ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(bufferIndex);
                Log.d("buffer size",encodedData.toString());
                if (encodedData == null) {
                    throw new RuntimeException("couldn't fetch buffer at index " + bufferIndex);
                }

                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mVideoBufferInfo.size = 0;
                }

                if (mVideoBufferInfo.size != 0) {
                    if (mMuxerStarted) {
                        encodedData.position(mVideoBufferInfo.offset);
                        encodedData.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);
                        mMuxer.writeSampleData(mTrackIndex, encodedData, mVideoBufferInfo);
                    } else {
                        // muxer not started
                    }
                }

                mVideoEncoder.releaseOutputBuffer(bufferIndex, false);

                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }
        if(mSPS!=null&&mPPS!=null){releaseEncoders();}
        mDrainHandler.postDelayed(mDrainEncoderRunnable, 10);
        return false;
    }
    public  void releaseEncoders() {
        mDrainHandler.removeCallbacks(mDrainEncoderRunnable);
        if (mMuxer != null) {
            if (mMuxerStarted) {
                mMuxer.stop();
            }
            mMuxer.release();
            mMuxer = null;
            mMuxerStarted = false;
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
            //mVideoEncoder = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
           // mInputSurface = null;
        }
        if (mMediaProjection2 != null) {
            //mMediaProjection2.stop();
            //mMediaProjection2 = null;
        }
        mVideoBufferInfo = null;
        mDrainEncoderRunnable = null;
        mTrackIndex = -1;
        Log.d("stopped", "encoder");
    }

}
