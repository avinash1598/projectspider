package com.example.avinash.screen;

import android.media.MediaCodec;
import android.media.MediaCodec.Callback;
import android.media.MediaFormat;
import android.util.Log;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Created by AVINASH on 9/5/2016.
 */
public class MediaCodecInputStream extends java.io.InputStream
{

    public final String TAG = "MediaCodecInputStream";

    private MediaCodec mMediaCodec = null;
    private MediaCodec.BufferInfo mBufferInfo ;
    private ByteBuffer[] mBuffers = null;
    private ByteBuffer mBuffer = null;
    private int mIndex = -1;
    private boolean mClosed = false;

    public MediaFormat mMediaFormat;

    public MediaCodecInputStream(MediaCodec mediaCodec) {

        mMediaCodec = mediaCodec;

        mBufferInfo = new MediaCodec.BufferInfo();
        mBuffers = mMediaCodec.getOutputBuffers();
    }

    @Override
    public void close() {
        mClosed = true;
    }

    @Override
    public int read() throws IOException {
        return 0;
    }                                                                           /**************************/

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int min = 0;

        try {
            if (mBuffer==null) {
                while (!Thread.interrupted() && !mClosed) {
                    mIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 1000000/20);/////////////change NOTE/////////////////////////
                    if (mIndex>=0 ){
                        Log.d(TAG,"Index: "+mIndex+" Time: "+mBufferInfo.presentationTimeUs+" size: "+mBufferInfo.size);
                        mBuffer = mBuffers[mIndex];
                        mBuffer.position(0);
                        break;
                    } else if (mIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        mBuffers = mMediaCodec.getOutputBuffers();
                    } else if (mIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mMediaFormat = mMediaCodec.getOutputFormat();
                        Log.i(TAG, mMediaFormat.toString());
                    } else if (mIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        //Log.v(TAG,"No buffer available...");
                        //return 0;
                    } else {
                        Log.e(TAG,"Message: "+mIndex);
                        //return 0;
                    }
                }
            }

            if (mClosed) throw new IOException("This InputStream was closed");

            min = length < mBufferInfo.size - mBuffer.position() ? length : mBufferInfo.size - mBuffer.position();
            mBuffer.get(buffer, offset, min);
            if (mBuffer.position()>=mBufferInfo.size) {
                mMediaCodec.releaseOutputBuffer(mIndex, false);
                mBuffer = null;
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return min;
    }

    public int available() {
        if (mBuffer != null)
            return mBufferInfo.size - mBuffer.position();
        else
            return 0;
    }

    public MediaCodec.BufferInfo getLastBufferInfo() {
        return mBufferInfo;
    }

}
