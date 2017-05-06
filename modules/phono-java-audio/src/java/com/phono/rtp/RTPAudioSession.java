/*
 * Copyright 2011 Voxeo Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.phono.rtp;

import com.phono.api.CodecList;
import com.phono.audio.AudioException;
import com.phono.audio.AudioFace;
import com.phono.audio.AudioReceiver;
import com.phono.audio.StampedAudio;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Properties;
import com.phono.srtplight.*;

public class RTPAudioSession implements RTPDataSink, AudioReceiver {

    RTPProtocolFace _sps;
    protected int _id = 1;
    AudioFace _audio;
    private boolean _first = true;
    int _ptype;
    private long _then;
    private boolean _doNTP;
    public RTPAudioSession(DatagramSocket near, InetSocketAddress far, int type, AudioFace a, Properties lsrtpProps, Properties rsrtpProps,long csrc) {
        this(near,far,type,a,lsrtpProps,rsrtpProps);
        if (_sps instanceof SRTPProtocolImpl){
            ((SRTPProtocolImpl)_sps).setSSRC(csrc);
        }
    }
    public RTPAudioSession(DatagramSocket near, InetSocketAddress far, int type, AudioFace a, Properties lsrtpProps, Properties rsrtpProps) {
        _sps = mkSps(near, far, type, a, lsrtpProps, rsrtpProps);
        _sps.setRTPDataSink(this);
        _ptype = type;
        makePhonoAudioSrc(a);

    }
    public void setDpRealloc(boolean v){
        if (_sps != null){
            _sps.setRealloc(v);
        }
    }
    protected RTPProtocolFace mkSps(DatagramSocket near, InetSocketAddress far, int type, AudioFace a, Properties lsrtpProps, Properties rsrtpProps) {
        RTPProtocolFace sps = null;
        if ((lsrtpProps != null) && (rsrtpProps != null)) {
            sps = new SRTPProtocolImpl(_id++, near, far, type, lsrtpProps, rsrtpProps);

        } else {
            sps = new RTPProtocolImpl(_id++, near, far, type);
        }
        return sps;
    }

    public void halt() {
        _sps.terminate();
    }

    private void makePhonoAudioSrc(AudioFace a) {
        // send side.
        _audio = a;
        try {
            _audio.addAudioReceiver(this);
        } catch (AudioException ex) {
            Log.error(ex.toString());
        }
        // receive side

        //_audio.startPlay(); // actually don't - let some packets build up first
        _audio.startRec();
    }

    public void digit(String value, int duration, boolean audible) throws SocketException, IOException {

        int fac = (int) (_audio.getSampleRate() / 1000.0); // assume duration is in ms.
        int dur = fac * duration;
        Log.debug("RAS sending digit " + value + " dur =" + duration + " " + (audible ? "Audible" : "InAudible"));
        int stamp = fac * _audio.getOutboundTimestamp();
        char c = value.toUpperCase().charAt(0);

        if (audible) {
            _audio.playDigit(c);
        }

        _sps.sendDigit(value, stamp, dur, duration);

        if (audible) {
            c = 0;
            _audio.playDigit(c);
        }


    }

    public void dataPacketReceived(byte[] data, long stamp, long index) {
        // Log.debug("stamp: " + stamp);
        StampedAudio sa = _audio.getCleanStampedAudio();
        /* broken timestamps so make up stamp from index */

        //stamp = stamp / CodecList.getFac(_audio.getCodec());
        stamp = index * _audio.getFrameInterval();
        //Log.debug("rcv fake stamp =" + stamp);
        sa.setStampAndBytes(data, 0, data.length, (int) stamp);
        try {
            _audio.writeStampedAudio(sa);
        } catch (AudioException ex) {
            Log.error(ex.toString());
        }
    }
// lifted from org.apache.commons.net.ntp.TimeStamp - with apache 2.0 license
    /**
     * baseline NTP time if bit-0=0 -> 7-Feb-2036 @ 06:28:16 UTC
     */
    protected static final long msb0baseTime = 2085978496000L;

    /**
     * baseline NTP time if bit-0=1 -> 1-Jan-1900 @ 01:00:00 UTC
     */
    protected static final long msb1baseTime = -2208988800000L;

    protected static long toNtpTime(long t) {
        boolean useBase1 = t < msb0baseTime;    // time < Feb-2036
        long baseTime;
        if (useBase1) {
            baseTime = t - msb1baseTime; // dates <= Feb-2036
        } else {
            // if base0 needed for dates >= Feb-2036
            baseTime = t - msb0baseTime;
        }

        long seconds = baseTime / 1000;
        long fraction = ((baseTime % 1000) * 0x100000000L) / 1000;

        if (useBase1) {
            seconds |= 0x80000000L; // set high-order bit if msb1baseTime 1900 used
        }

        long time = seconds << 32 | fraction;
        return time;
    }
// end apache 2.0 
    
    public void newAudioDataReady(AudioFace af, int i) {

        if (_first) {
            _sps.startrecv();
            _first = false;
            _then = System.currentTimeMillis();
        }
        try {
            StampedAudio sa = af.readStampedAudio();
            while (sa != null) {
                int fac = CodecList.getFac(af.getCodec());
                long dur = sa.getStamp();
                long ts = _doNTP?toNtpTime(_then+dur):dur*fac;
                _sps.sendPacket(sa.getData(),ts , _ptype);
                //Log.debug("send "+ sa.getStamp() * fac);
                sa = af.readStampedAudio();
            }
        } catch (Exception ex) {
            //Log.error(ex.toString());
        }
        if (_sps.finished()) {
            af.stopRec();
            af.stopPlay();
        }

    }

    public String getSent() {
        String ret = "0";
        if ((_sps != null) && (_sps instanceof RTPProtocolImpl)) {
            ret = "" + ((RTPProtocolImpl) _sps).getIndex();
        }
        return ret;
    }

    public String getRcvd() {
        String ret = "0";
        if ((_sps != null) && (_sps instanceof RTPProtocolImpl)) {
            ret = "" + ((RTPProtocolImpl) _sps).getSeqno();
        }
        return ret;
    }

    public String getLastError() {
        String ret = "";
        if ((_sps != null) && (_sps instanceof RTPProtocolImpl)) {
            Exception x = ((RTPProtocolImpl) _sps).getNClearLastX();
            if (x != null) {
                ret = x.getMessage();
            }
        }
        return ret;
    }
    public void setNTP(boolean d){
        _doNTP = d;
    }
}
