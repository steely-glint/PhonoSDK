/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.phono.audio.codec.opus;

import com.phono.audio.codec.CodecFace;
import com.phono.audio.codec.DecoderFace;
import com.phono.audio.codec.DecodesFEC;
import com.phono.audio.codec.EncoderFace;
import com.phono.audio.codec.OpusCodec;
import static com.phono.audio.codec.OpusCodec.OPUS_CODEC;
import com.phono.srtplight.Log;
import org.concentus.OpusApplication;
import org.concentus.OpusDecoder;
import org.concentus.OpusEncoder;
import org.concentus.OpusException;

/**
 *
 * @author tim
 */
public class PureOpusCodec implements CodecFace, EncoderFace ,DecoderFace {

    private OpusEncoder encoder;
    private OpusDecoder decoder;

    public static OpusCodec.SampleRate PHONOSAMPLERATE = OpusCodec.SampleRate.FM;
    public static OpusCodec.Application PHONOAPPLICATION = OpusCodec.Application.VOIP;
    final static int CHANNELS = 1;
    private final int maxpkt;
    private final int maxaudio;
    public static int FRAMEINTERVAL = 40;

    public PureOpusCodec() {
        maxpkt = 1200;
        maxaudio = (PHONOSAMPLERATE.Value * CHANNELS * FRAMEINTERVAL ) / 1000;
        try {
            OpusApplication mode = OpusApplication.OPUS_APPLICATION_UNIMPLEMENTED;
            if (PHONOAPPLICATION == OpusCodec.Application.VOIP) {
                mode = OpusApplication.OPUS_APPLICATION_VOIP;
            } else if (PHONOAPPLICATION == OpusCodec.Application.AUDIO) {
                mode = OpusApplication.OPUS_APPLICATION_AUDIO;
            } else if (PHONOAPPLICATION == OpusCodec.Application.RESTRICTED_LOWDELAY) {
                mode = OpusApplication.OPUS_APPLICATION_RESTRICTED_LOWDELAY;
            }
            encoder = new OpusEncoder(PHONOSAMPLERATE.Value, CHANNELS, mode);
            encoder.setBitrate(32000);
            encoder.setComplexity(1);
            encoder.setPacketLossPercent(10);
            decoder = new OpusDecoder(PHONOSAMPLERATE.Value, CHANNELS);
        } catch (OpusException ex) {
            Log.error("Failed to create pure java opus codec " + ex.getMessage());
        }
    }

    @Override
    public int getFrameSize() {
        return -1;
    }

    @Override
    public int getFrameInterval() {
        return FRAMEINTERVAL;
    }

    @Override
    public long getCodec() {
        return OPUS_CODEC;
    }

    @Override
    public DecoderFace getDecoder() {
        return this;
    }

    @Override
    public EncoderFace getEncoder() {
        return this;
    }

    @Override
    public float getSampleRate() {
        return (float) PHONOSAMPLERATE.Value;
    }

    public String getName() {
        return "OPUS";
    }

    @Override
    public byte[] encode_frame(short[] original_signal) {
        byte[] out_data = new byte[maxpkt];
        byte[] ret = null;
        try {
            int sz = encoder.encode(original_signal, 0, original_signal.length, out_data, 0, maxpkt);
            ret = new byte[sz];
            System.arraycopy(out_data, 0, ret, 0, sz);
        } catch (OpusException ex) {
            Log.debug("Can't encode frame to opus"+ex.getMessage());
        }
        return ret;
    }

    @Override
    public short[] decode_frame(byte[] ebuff, boolean fec) {
        short[] out_data = new short[maxaudio];
        short[] ret = null;
        try {
            int sz = decoder.decode(ebuff,0,ebuff.length,out_data,0,maxaudio,fec);
            ret = new short[sz];
            System.arraycopy(out_data, 0, ret, 0, sz);
        } catch (OpusException ex) {
            Log.error("Can't decode frame from opus"+ ex.getMessage());
        }
        return ret;
    }
    public void decoderReset(){
        decoder.resetState();
        Log.warn("reset opus decoder");
    }

}
