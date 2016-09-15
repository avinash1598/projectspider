package com.example.avinash.screen;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;

/**
 * Created by AVINASH on 9/4/2016.
 */
public interface Stream {
    public void configure() throws IllegalStateException, IOException;
    public void start() throws IllegalStateException, IOException;
    public void stop();
    public void setTimeToLive(int ttl) throws IOException;
    public void setDestinationAddress(InetAddress dest);
    public void setDestinationPorts(int dport);
    public void setDestinationPorts(int rtpPort, int rtcpPort);
    public void setOutputStream(OutputStream stream, byte channelIdentifier);
    public int[] getLocalPorts();
    public int[] getDestinationPorts();
    public int getSSRC();
    public long getBitrate();
    public String getSessionDescription() throws IllegalStateException;
    public boolean isStreaming();

}
