package com.example.avinash.screen;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by AVINASH on 9/4/2016.
 */
public class Session {

    public final static String TAG = "Session";

    public final static int STREAM_VIDEO = 0x01;

    public final static int STREAM_AUDIO = 0x00;
    public final static int ERROR_UNKNOWN_HOST = 0x05;
    public final static int ERROR_OTHER = 0x06;

    private String mOrigin;
    private String mDestination;
    private int mTimeToLive = 64;
    private long mTimestamp;

   // private AudioStream mAudioStream = null;
    private VideoStream mVideoStream = null;

    private Callback mCallback;
    private Handler mMainHandler;

    private Handler mHandler;

    public Session() {
        long uptime = System.currentTimeMillis();

        HandlerThread thread = new HandlerThread("Session");
        thread.start();

        mHandler = new Handler(thread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());
        mTimestamp = (uptime/1000)<<32 & (((uptime-((uptime/1000)*1000))>>32)/1000); // NTP timestamp
        mOrigin = "127.0.0.1";
    }
    public interface Callback {
        public void onBitrateUpdate(long bitrate);
        public void onSessionError(int reason, int streamType, Exception e);
        public void onSessionConfigured();
        public void onSessionStarted();
        public void onSessionStopped();

    }
    void addVideoTrack(VideoStream track) {
        removeVideoTrack();
        mVideoStream = track;
    }
    void removeVideoTrack() {
        if (mVideoStream != null) {
            //mVideoStream.stopPreview();
            mVideoStream = null;
        }
    }
    public VideoStream getVideoTrack() {
        return mVideoStream;
    }
    public void setCallback(Callback callback) {
        mCallback = callback;
    }
    public void setOrigin(String origin) {
        mOrigin = origin;
    }
    public void setDestination(String destination) {
        mDestination =  destination;
    }
    public void setTimeToLive(int ttl) {
        mTimeToLive = ttl;
    }
    public Callback getCallback() {
        return mCallback;
    }

    public String getSessionDescription() {
        StringBuilder sessionDescription = new StringBuilder();
        if (mDestination==null) {
            throw new IllegalStateException("setDestination() has not been called !");
        }
        sessionDescription.append("v=0\r\n");
        // TODO: Add IPV6 support
        sessionDescription.append("o=- "+mTimestamp+" "+mTimestamp+" IN IP4 "+mOrigin+"\r\n");
        sessionDescription.append("s=Unnamed\r\n");
        sessionDescription.append("i=N/A\r\n");
        sessionDescription.append("c=IN IP4 "+mDestination+"\r\n");
        // t=0 0 means the session is permanent (we don't know when it will stop)
        sessionDescription.append("t=0 0\r\n");
        sessionDescription.append("a=recvonly\r\n");
        // Prevents two different sessions from using the same peripheral at the same time
       /* if (mAudioStream != null) {
            sessionDescription.append(mAudioStream.getSessionDescription());
            sessionDescription.append("a=control:trackID="+0+"\r\n");
        }*/
        if (mVideoStream != null) {
            sessionDescription.append(mVideoStream.getSessionDescription());
            sessionDescription.append("a=control:trackID="+1+"\r\n");
        }
        return sessionDescription.toString();
    }

    public String getDestination() {
        return mDestination;
    }
    public long getBitrate() {
        long sum = 0;
       // if (mAudioStream != null) sum += mAudioStream.getBitrate();
        if (mVideoStream != null) sum += mVideoStream.getBitrate();
        return sum;
    }
    public boolean isStreaming() {
        if (  (mVideoStream!=null && mVideoStream.isStreaming()) )
            return true;
        else
            return false;
    }
    public void configure() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    syncConfigure();
                } catch (Exception e) {};
            }
        });
    }
    public void syncConfigure()
            throws
            RuntimeException,
            IOException {

        for (int id=1;id<2;id++) {
            Stream stream = mVideoStream;////////////////what to stream////////////////////////////////
            if (stream!=null && !stream.isStreaming()) {
                try {
                    stream.configure();//////////////////////Media Stream///////////////////////////////////
                } catch (IOException e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }
        postSessionConfigured();
    }
    public void start() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    syncStart();
                } catch (Exception e) {
                }
            }
        });
    }
    public void syncStart(int id) 		////////////////////////start////////////////////////////////////////
            throws
            UnknownHostException,
            IOException {

        Stream stream = mVideoStream;
        if (stream!=null && !stream.isStreaming()) {
            try {
                InetAddress destination =  InetAddress.getByName(mDestination);
                stream.setTimeToLive(mTimeToLive);
                stream.setDestinationAddress(destination);
                stream.start();/////////////////////////starting/////////////////////////////////////////////////
                if (getTrack(1-id) == null || getTrack(1-id).isStreaming()) {
                    postSessionStarted();
                }
                if (getTrack(1-id) == null || !getTrack(1-id).isStreaming()) {
                    mHandler.post(mUpdateBitrate);
                }
            } catch (UnknownHostException e) {
                //postError(ERROR_UNKNOWN_HOST, id, e);
                e.printStackTrace();
                throw e;
            }
        }}
    public void syncStart()
            throws
            UnknownHostException,
            IOException {

        syncStart(1);
        try {
            syncStart(0);
        } catch (RuntimeException e) {
            syncStop(1);
            throw e;
        } catch (IOException e) {
            syncStop(1);
            throw e;
        }

    }

    public void stop() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                syncStop();
            }
        });
    }

    private void syncStop(final int id) {
        Stream stream = mVideoStream;
        if (stream!=null) {
            stream.stop();
        }
    }
    public void syncStop() {
        syncStop(0);
        syncStop(1);
        postSessionStopped();
    }

    public void startPreview() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mVideoStream != null) {
                    try {
                        mVideoStream.startPreview();
                        mVideoStream.configure();
                    } catch (IOException e) {
                        postError(ERROR_OTHER, STREAM_VIDEO, e);
                    }
                }
            }
        });
    }

    public void stopPreview() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mVideoStream != null) {
                    mVideoStream.stopPreview();
                }
            }
        });
    }

    private void postSessionConfigured() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onSessionConfigured();
                }
            }
        });
    }

    private void postSessionStarted() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onSessionStarted();
                }
            }
        });
    }

    private void postSessionStopped() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onSessionStopped();
                }
            }
        });
    }

    private void postError(final int reason, final int streamType,final Exception e) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onSessionError(reason, streamType, e);
                }
            }
        });
    }

    private void postBitRate(final long bitrate) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onBitrateUpdate(bitrate);
                }
            }
        });
    }

    private Runnable mUpdateBitrate = new Runnable() {
        @Override
        public void run() {
            if (isStreaming()) {
                postBitRate(getBitrate());
                mHandler.postDelayed(mUpdateBitrate, 500);
            } else {
                postBitRate(0);
            }
        }
    };

    public void release() {
        removeVideoTrack();
        mHandler.getLooper().quit();
    }


    public boolean trackExists(int id) {
            return mVideoStream!=null;
    }

    public Stream getTrack(int id) {
            return mVideoStream;
    }

}




