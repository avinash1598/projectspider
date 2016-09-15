package com.example.avinash.screen;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by AVINASH on 9/4/2016.
 */
public abstract class VideoStream extends MediaStream{
    protected final static String TAG = "VideoStream";
    protected String mMimeType;
    public static FileDescriptor fd = null;
   // public static MediaCodec mcoder;
    protected SharedPreferences mSettings = null;
    public static VirtualDisplay mVirtualDisplay2;

    //MediaRecorder mMediaRecorder;

    public synchronized void configure() throws IllegalStateException, IOException {
        super.configure();
    }
    public synchronized void start() throws IllegalStateException, IOException {
        super.start();
        Log.d(TAG, "Stream configuration: FPS: " + MainActivity.framerate + " Width: " + MainActivity.DISPLAY_WIDTH +
                " Height: " + MainActivity.DISPLAY_HEIGHT);
    }
    public synchronized void stop() {
        super.stop();
    }

    public synchronized void startPreview()
            throws
            RuntimeException {
        Log.d("started", "yobaby");
    }
    public void setPreferences(SharedPreferences prefs) {
        mSettings = prefs;
    }
    /**
     * Stops the preview.
     */
    public synchronized void stopPreview() {
        stop();
    }

    ///////////////////////////////call it/////////////////////////////////////////////////////////////

    protected void encodeWithMediaRecorder() throws IOException {

        Log.d(TAG, "Video encoded using the MediaRecorder API");
        createSockets();

        //  local socket to forward data output by the camera to the packetizer
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(SessionBuilder.mVideoEncoder);
        //mMediaRecorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());
        mMediaRecorder.setVideoSize(MainActivity.DISPLAY_WIDTH,MainActivity.DISPLAY_HEIGHT);
        mMediaRecorder.setVideoFrameRate(MainActivity.framerate);

        // The bandwidth actually consumed is often above what was requested
        mMediaRecorder.setVideoEncodingBitRate((int)(MainActivity.BIT_RATE));

        // We write the output of the camera in a local socket instead of a file !
        // This one little trick makes streaming feasible quiet simply: data from the camera
        // can then be manipulated at the other end of the socket
        FileDescriptor fd = null;
         {
            fd = mParcelWrite.getFileDescriptor();
        }   {
           // fd = mSender.getFileDescriptor();
        }
        mMediaRecorder.setOutputFile(fd);

        mMediaRecorder.prepare();

        if(mVirtualDisplay2!=null){mVirtualDisplay2.release();mVirtualDisplay2=null;}
       mVirtualDisplay2= MainActivity.mMediaProjection.createVirtualDisplay("DISPLAY_AVI", MainActivity.DISPLAY_WIDTH,
                MainActivity.DISPLAY_HEIGHT, MainActivity.mScreenDensity, 0 /* flags */, mMediaRecorder.getSurface(),
                null /* callback */, null /* handler */);
        mMediaRecorder.start();



    InputStream is = null;

    {
        is = new ParcelFileDescriptor.AutoCloseInputStream(mParcelRead);
    }  {
      //  is = mReceiver.getInputStream();
    }

    // This will skip the MPEG4 header if this step fails we can't stream anything :(
    try {
        byte buffer[] = new byte[4];
        // Skip all atoms preceding mdat atom
        while (!Thread.interrupted()) {
            while (is.read() != 'm');
            is.read(buffer,0,3);
            if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') break;
        }
    } catch (IOException e) {
        Log.e(TAG,"Couldn't skip mp4 header :/");
        stop();
        throw e;
    }

    // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
    mPacketizer.setInputStream(is);
    mPacketizer.start();

    mStreaming = true;


}
    public abstract String getSessionDescription() throws IllegalStateException;

    protected void encodeWithMediaCodec() throws RuntimeException, IOException {

            encodeWithMediaCodecMethod1();

    }
    @SuppressLint("NewApi")
    protected void encodeWithMediaCodecMethod1() throws RuntimeException, IOException {
        Log.d("Avinash","aa to raha h encodemediacodec");

if(Encoder.mVideoEncoder!=null)Encoder.mVideoEncoder=null;
        mMediaCodec=Encoder.mVideoEncoder;
        mVirtualDisplay2=MainActivity.mVirtualDisplay;
if(mMediaCodec==null)Log.d("haa bhai","null h");
      // mMediaCodec = MediaCodec.createByCodecName("ävinashcodec");
        mMediaCodec = MediaCodec.createEncoderByType(MainActivity.VIDEO_MIME_TYPE);

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", MainActivity.DISPLAY_WIDTH
                , MainActivity.DISPLAY_HEIGHT);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, MainActivity.BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, MainActivity.framerate);
        mediaFormat.setInteger(MediaFormat.KEY_CAPTURE_RATE, MainActivity.framerate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        //mediaFormat.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / MainActivity.framerate);
        Surface surface=null;
       try{ mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
           surface = mMediaCodec.createInputSurface();
        //((SurfaceView)mSurfaceView).addMediaCodecSurface(surface);
        mMediaCodec.start();}catch(Exception e){e.printStackTrace();}

        if(mVirtualDisplay2!=null){mVirtualDisplay2.release();mVirtualDisplay2=null;}
        MainActivity.mMediaProjection.createVirtualDisplay("DISPLAY_AVI", MainActivity.DISPLAY_WIDTH,
                MainActivity.DISPLAY_HEIGHT, MainActivity.mScreenDensity, 0 /* flags */, surface,
                null /* callback */, null /* handler */);


        Log.d("packetizer", "me ab");
        mPacketizer.setInputStream(new MediaCodecInputStream(mMediaCodec));////////created input stream from media codec/////////////////
        mPacketizer.start();

        mStreaming = true;
        Log.d("streaming", "true");

        mStreaming = true;
    }


    }



