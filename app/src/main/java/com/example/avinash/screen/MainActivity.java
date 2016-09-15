package com.example.avinash.screen;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.example.avinash.screen.SessionBuilder.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements RtspClient.Callback,
        Session.Callback {

    private MediaProjectionManager mMediaProjectionManager;
    private static final int REQUEST_CODE_CAPTURE_PERM = 1234;
    // …


    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1000;
    public static int mScreenDensity;
    public static MediaProjectionManager mProjectionManager;
    public static final int DISPLAY_WIDTH = 320;
    public static final int DISPLAY_HEIGHT = 480;
    public static final int framerate = 20;
    public  static  int BIT_RATE= 4000000;
    public static MediaProjection mMediaProjection;
    public static VirtualDisplay mVirtualDisplay;
    public static MediaProjectionCallback mMediaProjectionCallback;
    private ToggleButton mToggleButton;
    public static MediaRecorder mMediaRecorder2;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();


    public static final String STREAM_URL = "rtsp://10.1.75.232:1935/live/Screen";
    public static final String PUBLISHER_USERNAME = "madrupam";
    public static final String PUBLISHER_PASSWORD = "Brainlara";

    public  static int screenWidth ,screenHeight;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    public static  String VIDEO_MIME_TYPE = "video/avc";
   // public static MediaCodec mVideoEncoder2;
    public static MediaCodec.BufferInfo mVideoBufferInfo;
    public static Surface mInputSurface2;
    //private EncodedFrameListener frameListener;
    //private MediaCodec mediaCodec;
    //private ParameterSetsListener parameterSetsListener;


    private Session mSession;
    private static RtspClient mClient;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
         screenWidth = metrics.widthPixels;
         screenHeight = metrics.heightPixels;


        mMediaProjectionManager =(MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);

        Intent permissionIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(permissionIntent, REQUEST_CODE_CAPTURE_PERM);



         handler = new Handler(Looper.getMainLooper());


///////////////////////////////////////////main program//////////////////////////////////////////////////////////////////////////////////
        mToggleButton = (ToggleButton) findViewById(R.id.toggle);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleScreenShare(v);
            }
        });
        initRtspClient();

    }

    public void onToggleScreenShare(View view) {
        if (((ToggleButton) view).isChecked()) {
            toggleStreaming();

        } else {
            toggleStreaming();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (REQUEST_CODE_CAPTURE_PERM == requestCode) {
            if (resultCode == RESULT_OK) {
                mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, intent);
               Encoder e=new Encoder();e.startRecording(); // defined below
            } else {
                // user did not grant permissions
            }
        }
    }


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onBitrateUpdate(long bitrate) {

    }

    @Override
    public void onSessionError(int reason, int streamType, Exception e) {

    }

    @Override
    public void onSessionConfigured() {

    }

    @Override
    public void onSessionStarted() {

    }

    @Override
    public void onSessionStopped() {

    }

    @Override
    public void onRtspUpdate(int message, Exception exception) {
        switch (message) {
            case RtspClient.ERROR_CONNECTION_FAILED:
            case RtspClient.ERROR_WRONG_CREDENTIALS:
                //alertError(exception.getMessage());
                exception.printStackTrace();
                break;
        }
    }
    /////////////////////////////////////////////h264////////////////////////////////////////////////////////

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (mToggleButton.isChecked()) {
                mToggleButton.setChecked(false);
                mMediaRecorder2.stop();
                mMediaRecorder2.reset();
                Log.v(TAG, "Recording Stopped");
            }
            mMediaProjection = null;
            stopScreenSharing();
        }
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mMediaRecorder2.release(); //If used: mMediaRecorder object cannot
        // be reused again
        destroyMediaProjection();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // mClient.release();
        // mSession.release();
        mClient.release();
        mSession.release();

        destroyMediaProjection();
    }

    public void destroyMediaProjection() {

        if(mVirtualDisplay!=null)mVirtualDisplay.release();
      /*  if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }*/
        Log.i(TAG, "MediaProjection Stopped");
    }
  //////////////////////////////////////////////////////calling rtsp//////////////////////////////////////////////////////////////////
  private void initRtspClient() {
      // Configures the SessionBuilder
      mSession = SessionBuilder.getInstance()
              .setContext(getApplicationContext())
              .setVideoEncoder(SessionBuilder.VIDEO_H264).
                      setContext(MainActivity.this)
              .setCallback(this).build();

      // Configures the RTSP client
      mClient = new RtspClient();
      mClient.setSession(mSession);
      mClient.setCallback(this);
      String ip, port, path;

      // We parse the URI written in the Editext
      Pattern uri = Pattern.compile("rtsp://(.+):(\\d+)/(.+)");
      Matcher m = uri.matcher(STREAM_URL);
      m.find();
      ip = m.group(1);
      port = m.group(2);
      path = m.group(3);

      mClient.setCredentials(PUBLISHER_USERNAME,
              PUBLISHER_PASSWORD);
      mClient.setServerAddress(ip, Integer.parseInt(port));
      mClient.setStreamPath("/" + path);
  }

    private void toggleStreaming() {
        if (!mClient.isStreaming()) {
            // Start camera previe
            mSession.startPreview();
            // Start video stream
            mClient.startStream();
        } else {
            // already streaming, stop streaming
            // stop camera preview
            mSession.stopPreview();
            // stop streaming
            mClient.stopStream();
        }
    }

    public void connect(View v){

    }

}
