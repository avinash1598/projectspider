package com.example.avinash.screen;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.Buffer;
import java.util.Random;

/**
 * Created by AVINASH on 9/4/2016.
 */
abstract public class AbstractPacketizer {
    protected static final int rtphl = RtpSocket.RTP_HEADER_LENGTH;

    // Maximum size of RTP packets
    protected final static int MAXPACKETSIZE = RtpSocket.MTU-28;

    protected RtpSocket socket = null;
    protected InputStream is = null;
    public static byte[] buffer;

    protected long ts = 0;

    public AbstractPacketizer() {
        int ssrc = new Random().nextInt();
        ts = new Random().nextInt();
        socket = new RtpSocket();//////////////////////rtp socket creation////////////////////////////
        socket.setSSRC(ssrc);
    }

    public RtpSocket getRtpSocket() {
        return socket;
    }

    public void setSSRC(int ssrc) {
        socket.setSSRC(ssrc);
    }

    public int getSSRC() {
        return socket.getSSRC();
    }

    public void setInputStream(InputStream is) {
        this.is = is;
    }////////////////sets input stream from media codec/////////////////////

    public void setTimeToLive(int ttl) throws IOException {
        socket.setTimeToLive(ttl);
    }

    public void setDestination(InetAddress dest, int rtpPort, int rtcpPort) {
        socket.setDestination(dest, rtpPort, rtcpPort);/////////////////setting destination for socket//////////////////////////////////
    }

    public abstract void start();
    public abstract void stop();
    protected void send(int length) throws IOException {
        Log.d("sending byte length", "" + length);
        socket.commitBuffer(length);/////////////////////////////////////////////////////////////
    }

    protected static String printBuffer(byte[] buffer, int start,int end) {
        String str = "";
        for (int i=start;i<end;i++) str+=","+Integer.toHexString(buffer[i]&0xFF);
        return str;
    }

    protected static class Statistics {

        public final static String TAG = "Statistics";

        private int count=700, c = 0;
        private float m = 0, q = 0;
        private long elapsed = 0;
        private long start = 0;
        private long duration = 0;
        private long period = 10000000000L;
        private boolean initoffset = false;

        public Statistics() {}

        public Statistics(int count, int period) {
            this.count = count;
            this.period = period;
        }

        public void reset() {
            initoffset = false;
            q = 0; m = 0; c = 0;
            elapsed = 0;
            start = 0;
            duration = 0;
        }

        public void push(long value) {
            elapsed += value;
            if (elapsed>period) {
                elapsed = 0;
                long now = System.nanoTime();
                if (!initoffset || (now - start < 0)) {
                    start = now;
                    duration = 0;
                    initoffset = true;
                }
                value += (now - start) - duration;
                //Log.d(TAG, "sum1: "+duration/1000000+" sum2: "+(now-start)/1000000+" drift: "+((now-start)-duration)/1000000+" v: "+value/1000000);
            }
            if (c<5) {
                // We ignore the first 20 measured values because they may not be accurate
                c++;
                m = value;
            } else {
                m = (m*q+value)/(q+1);
                if (q<count) q++;
            }
        }

        public long average() {
            long l = (long)m;
            duration += l;
            return l;
        }

    }

}
