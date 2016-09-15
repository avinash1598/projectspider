package com.example.avinash.screen;

import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Random;

/**
 * Created by AVINASH on 9/4/2016.
 */
public abstract class MediaStream implements Stream  {
    protected static final String TAG = "MediaStream";
    protected AbstractPacketizer mPacketizer = null;
    protected boolean mStreaming = false, mConfigured = false;
    protected int mRtpPort = 0, mRtcpPort = 0;
    protected byte mChannelIdentifier = 0;
    protected OutputStream mOutputStream = null;
    protected InetAddress mDestination;

    protected ParcelFileDescriptor[] mParcelFileDescriptors;
    protected ParcelFileDescriptor mParcelRead;
    protected ParcelFileDescriptor mParcelWrite;

    protected LocalSocket mReceiver, mSender = null;
    private LocalServerSocket mLss = null;
    private int mSocketId;
    private int mTTL = 150;
    protected byte mMode, mRequestedMode;
    protected static final String PREF_PREFIX = "Avinash-";

    //protected LocalSocket mReceiver, mSender = null;
    //private LocalServerSocket mLss = null;
    //private int mSocketId;
    protected MediaCodec mMediaCodec;

    protected MediaRecorder mMediaRecorder;

    static {
        // We determine whether or not the MediaCodec API should be used
        try {
            Class.forName("android.media.MediaCodec");
            // Will be set to MODE_MEDIACODEC_API at some point...
            Log.i(TAG,"Phone supports the MediaCoded API");
        } catch (ClassNotFoundException e) {
            Log.i(TAG,"Phone does not support the MediaCodec API");
        }

        // Starting lollipop, the LocalSocket API cannot be used anymore to feed
        // a MediaRecorder object for security reasons
    }

    public void setDestinationAddress(InetAddress dest) {
        mDestination = dest;
    }
    public void setDestinationPorts(int dport) {
        if (dport % 2 == 1) {
            mRtpPort = dport-1;
            mRtcpPort = dport;
        } else {
            mRtpPort = dport;
            mRtcpPort = dport+1;
        }
    }
    public void setDestinationPorts(int rtpPort, int rtcpPort) {
        mRtpPort = rtpPort;
        mRtcpPort = rtcpPort;
        mOutputStream = null;
    }
    public void setOutputStream(OutputStream stream, byte channelIdentifier) {
        mOutputStream = stream;
        mChannelIdentifier = channelIdentifier;
    }
    public void setTimeToLive(int ttl) throws IOException {
        mTTL = ttl;
    }


    public int[] getDestinationPorts() {
        return new int[] {
                mRtpPort,
                mRtcpPort
        };
    }


    public int[] getLocalPorts() {
        return mPacketizer.getRtpSocket().getLocalPorts();
    }


    public void setStreamingMethod(byte mode) {
        mRequestedMode = mode;
    }


    public byte getStreamingMethod() {
        return mMode;
    }


    public AbstractPacketizer getPacketizer() {
        return mPacketizer;
    }


    public long getBitrate() {
        return !mStreaming ? 0 : mPacketizer.getRtpSocket().getBitrate();
    }


    public boolean isStreaming() {
        return mStreaming;
    }


    public synchronized void configure() throws IllegalStateException, IOException {
        if (mStreaming) throw new IllegalStateException("Can't be called while streaming.");
        if (mPacketizer != null) {
            mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
            mPacketizer.getRtpSocket().setOutputStream(mOutputStream, mChannelIdentifier);
        }
        mMode = mRequestedMode;
        mConfigured = true;
    }


    public synchronized void start() throws IllegalStateException, IOException {

        if (mDestination==null)
            throw new IllegalStateException("No destination ip address set for the stream !");

        if (mRtpPort<=0 || mRtcpPort<=0)
            throw new IllegalStateException("No destination ports set for the stream !");

        mPacketizer.setTimeToLive(mTTL);
        encodeWithMediaCodec();
        //encodeWithMediaRecorder();

    }


    public synchronized  void stop() {
        if (mStreaming) {
           /* try {
                    mMediaRecorder.stop();
                    mMediaRecorder.release();
                    mMediaRecorder = null;           ///////////change 3....////////////////
                    closeSockets();
                    mPacketizer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }*/

            mPacketizer.stop();
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;

            mStreaming = false;
        }
    }
    protected abstract void encodeWithMediaRecorder() throws IOException;
    public abstract String getSessionDescription();
    public int getSSRC() {
        return getPacketizer().getSSRC();
    }

    protected void createSockets() throws IOException {///////////////////not in use/////////////////////////////

        /*final String LOCAL_ADDR = "com.example.avinash.screen-";

        for (int i=0;i<10;i++) {
            try {
                mSocketId = new Random().nextInt();
                mLss = new LocalServerSocket(LOCAL_ADDR+mSocketId);
                break;
            } catch (IOException e1) {}
        }

        mReceiver = new LocalSocket();
        mReceiver.connect( new LocalSocketAddress(LOCAL_ADDR+mSocketId));
        mReceiver.setReceiveBufferSize(500000);
        mReceiver.setSoTimeout(3000);
        mSender = mLss.accept();
        mSender.setSendBufferSize(500000);*/

        Log.e(TAG, "parcelFileDescriptors createPipe version = Lollipop");
        mParcelFileDescriptors = ParcelFileDescriptor.createPipe();               /////////change 4..../////////////////////
        mParcelRead = new ParcelFileDescriptor(mParcelFileDescriptors[0]);
        mParcelWrite = new ParcelFileDescriptor(mParcelFileDescriptors[1]);
    }

    protected abstract void encodeWithMediaCodec() throws IOException;

    protected void closeSockets() {
       /* try {
            mReceiver.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mSender.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mLss.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mLss = null;
        mSender = null;
        mReceiver = null;*/

        try {
                if (mParcelRead != null) {
                    mParcelRead.close();
                }
            } catch (Exception e) {
                e.printStackTrace();           //////////////////////change 5.....//////////////////////////////////////
            }
            try {
                if (mParcelWrite != null) {
                    mParcelWrite.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


