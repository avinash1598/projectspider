package com.example.avinash.screen;

import android.util.Base64;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by AVINASH on 9/4/2016.
 */
public class MP4Config {
    public final static String TAG = "MP4Config";

    private MP4Parser mp4Parser;
    private String mProfilLevel, mPPS, mSPS;

    public MP4Config(String profil, String sps, String pps) {
        mProfilLevel = profil;
        mPPS = pps;
        mSPS = sps;
    }

    public MP4Config(String sps, String pps) {
        mPPS = pps;
        mSPS = sps;
        mProfilLevel = MP4Parser.toHexString(Base64.decode(sps, Base64.NO_WRAP),1,3);
    }

    public MP4Config(byte[] sps, byte[] pps) {
        mPPS = Base64.encodeToString(pps, 0, pps.length, Base64.NO_WRAP);
        mSPS = Base64.encodeToString(sps, 0, sps.length, Base64.NO_WRAP);
        mProfilLevel = MP4Parser.toHexString(sps,1,3);
    }

    public MP4Config (String path) throws IOException, FileNotFoundException {

        StsdBox stsdBox;
        try {
            mp4Parser = MP4Parser.parse(path);
        } catch (IOException ignore) {
        }

        stsdBox = mp4Parser.getStsdBox();
        mPPS = stsdBox.getB64PPS();
        mSPS = stsdBox.getB64SPS();
        mProfilLevel = stsdBox.getProfileLevel();

        mp4Parser.close();

    }

    public String getProfileLevel() {
        return mProfilLevel;
    }

    public String getB64PPS() {
        Log.d(TAG, "PPS: " + mPPS);
        return mPPS;
    }

    public String getB64SPS() {
        Log.d(TAG, "SPS: "+mSPS);
        return mSPS;
    }

}
