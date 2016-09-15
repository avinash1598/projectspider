package com.example.avinash.screen;

import android.os.SystemClock;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import static com.example.avinash.screen.RtpSocket.TRANSPORT_TCP;
import static com.example.avinash.screen.RtpSocket.TRANSPORT_UDP;

/**
 * Created by AVINASH on 9/4/2016.
 */
public class SenderReport {
    public static final int MTU = 1500;

    private static final int PACKET_LENGTH = 28;

    private MulticastSocket usock;
    private DatagramPacket upack;

    private int mTransport;
    private OutputStream mOutputStream = null;
    private byte[] mBuffer = new byte[MTU];
    private int mSSRC, mPort = -1;
    private int mOctetCount = 0, mPacketCount = 0;
    private long interval, delta, now, oldnow;
    private byte mTcpHeader[];

    public SenderReport(int ssrc) throws IOException {
        super();
        this.mSSRC = ssrc;
    }

    public SenderReport() {

        mTransport = TRANSPORT_UDP;
        mTcpHeader = new byte[] {'$',0,0,PACKET_LENGTH};

        mBuffer[0] = (byte) Integer.parseInt("10000000",2);

        mBuffer[1] = (byte) 200;
        setLong(PACKET_LENGTH/4-1, 2, 4);

        try {
            usock = new MulticastSocket();
        } catch (IOException e) {

            throw new RuntimeException(e.getMessage());
        }
        upack = new DatagramPacket(mBuffer, 1);
        interval = 3000;

    }

    public void close() {
        usock.close();
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public void update(int length, long rtpts) throws IOException {
        mPacketCount += 1;
        mOctetCount += length;
        setLong(mPacketCount, 20, 24);
        setLong(mOctetCount, 24, 28);

        now = SystemClock.elapsedRealtime();
        delta += oldnow != 0 ? now-oldnow : 0;
        oldnow = now;
        if (interval>0) {
            if (delta>=interval) {
                // We send a Sender Report
                send(System.nanoTime(), rtpts);
                delta = 0;
            }
        }

    }

    public void setSSRC(int ssrc) {
        this.mSSRC = ssrc;
        setLong(ssrc,4,8);
        mPacketCount = 0;
        mOctetCount = 0;
        setLong(mPacketCount, 20, 24);
        setLong(mOctetCount, 24, 28);
    }

    public void setDestination(InetAddress dest, int dport) {
        mTransport = TRANSPORT_UDP;
        mPort = dport;
        upack.setPort(dport);
        upack.setAddress(dest);
    }

    /**
     * If a TCP is used as the transport protocol for the RTP session,
     * the output stream to which RTP packets will be written to must
     * be specified with this method.
     */
    public void setOutputStream(OutputStream os, byte channelIdentifier) {
        mTransport = TRANSPORT_TCP;
        mOutputStream = os;
        mTcpHeader[1] = channelIdentifier;
    }

    public int getPort() {
        return mPort;
    }

    public int getLocalPort() {
        return usock.getLocalPort();
    }

    public int getSSRC() {
        return mSSRC;
    }

    /**
     * Resets the reports (total number of bytes sent, number of packets sent, etc.)
     */
    public void reset() {
        mPacketCount = 0;
        mOctetCount = 0;
        setLong(mPacketCount, 20, 24);
        setLong(mOctetCount, 24, 28);
        delta = now = oldnow = 0;
    }

    private void setLong(long n, int begin, int end) {
        for (end--; end >= begin; end--) {
            mBuffer[end] = (byte) (n % 256);
            n >>= 8;
        }
    }

    /**
     * Sends the RTCP packet over the network.
     *
     * @param ntpts
     *            the NTP timestamp.
     * @param rtpts
     *            the RTP timestamp.
     */
    private void send(long ntpts, long rtpts) throws IOException {
        long hb = ntpts/1000000000;
        long lb = ( ( ntpts - hb*1000000000 ) * 4294967296L )/1000000000;
        setLong(hb, 8, 12);
        setLong(lb, 12, 16);
        setLong(rtpts, 16, 20);
        if (mTransport == TRANSPORT_UDP) {
            upack.setLength(PACKET_LENGTH);
            usock.send(upack);
        } else {
            synchronized (mOutputStream) {
                try {
                    mOutputStream.write(mTcpHeader);
                    mOutputStream.write(mBuffer, 0, PACKET_LENGTH);
                } catch (Exception e) {}
            }
        }
    }

}
