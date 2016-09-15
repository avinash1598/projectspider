package com.example.avinash.screen;

import android.content.Context;

/**
 * Created by AVINASH on 9/4/2016.
 */
public class SessionBuilder {
    public final static String TAG = "SessionBuilder";
    public final static int VIDEO_H264 = 1;
    public final static int AUDIO_AAC = 5;
    public static int mVideoEncoder = VIDEO_H264;
    public int mTimeToLive = 64;
    public String mOrigin = null;
    public String mDestination = null;
    public Session.Callback mCallback = null;
    private Context mContext;
    private SessionBuilder() {}

    private static volatile SessionBuilder sInstance = null;

    public final static SessionBuilder getInstance() {
        if (sInstance == null) {
            synchronized (SessionBuilder.class) {
                if (sInstance == null) {
                    SessionBuilder.sInstance = new SessionBuilder();
                }
            }
        }
        return sInstance;
    }
    public Session build() {
        Session session;

        session = new Session();
        session.setOrigin(mOrigin);
        session.setDestination(mDestination);
        session.setTimeToLive(mTimeToLive);
        session.setCallback(mCallback);
        H264Stream stream = new H264Stream();
        session.addVideoTrack(stream);

        if (session.getVideoTrack()!=null) {
            VideoStream video = session.getVideoTrack();
            //video.setFlashState(mFlash);
            //video.setVideoQuality(mVideoQuality);
            //video.setSurfaceView(mSurfaceView);/////////////u can remove it it is just to show on the surface//////////////////////////////////
            //video.setPreviewOrientation(mOrientation);
            video.setDestinationPorts(5006);
        }
        return session;

    }

    public SessionBuilder setContext(Context context) {
        mContext = context;
        return this;
    }

    public SessionBuilder setDestination(String destination) {
        mDestination = destination;
        return this;
    }

    public SessionBuilder setOrigin(String origin) {
        mOrigin = origin;
        return this;
    }
    public SessionBuilder setVideoEncoder(int encoder) {
        mVideoEncoder = encoder;
        return this;
    }
    public SessionBuilder setTimeToLive(int ttl) {
        mTimeToLive = ttl;
        return this;
    }
    public SessionBuilder setCallback(Session.Callback callback) {
        mCallback = callback;
        return this;
    }

    public Context getContext() {
        return mContext;
    }

    public String getDestination() {
        return mDestination;
    }

    public String getOrigin() {
        return mOrigin;
    }

    public int getTimeToLive() {
        return mTimeToLive;
    }

    public SessionBuilder clone() {
        return new SessionBuilder()
                .setDestination(mDestination)
                .setOrigin(mOrigin)
                .setVideoEncoder(mVideoEncoder)
                .setTimeToLive(mTimeToLive)
                .setContext(mContext)
                .setCallback(mCallback);
    }
}
