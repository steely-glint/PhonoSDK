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
package com.phono.audio.codec;

import com.phono.srtplight.Log;
import java.nio.ByteBuffer;

/**
 *
 * @author tim
 */
public class OpusCodec implements CodecFace, EncoderFace, DecoderFace {

    public enum Application {
        VOIP(2048),
        AUDIO(2049),
        RESTRICTED_LOWDELAY(2051);
        public final int Value;

        private Application(int value) {
            Value = value;
        }
    }

    public enum SampleRate {
        HD(48000),
        FM(16000),
        PSTN(8000);
        public final int Value;

        private SampleRate(int value) {
            Value = value;
        }
    }
    private final static int OPUS_SET_APPLICATION_REQUEST = 4000;
    private final static int OPUS_GET_APPLICATION_REQUEST = 4001;
    private final static int OPUS_SET_BITRATE_REQUEST = 4002;
    private final static int OPUS_GET_BITRATE_REQUEST = 4003;
    private final static int OPUS_SET_MAX_BANDWIDTH_REQUEST = 4004;
    private final static int OPUS_GET_MAX_BANDWIDTH_REQUEST = 4005;
    private final static int OPUS_SET_VBR_REQUEST = 4006;
    private final static int OPUS_GET_VBR_REQUEST = 4007;
    private final static int OPUS_SET_BANDWIDTH_REQUEST = 4008;
    private final static int OPUS_GET_BANDWIDTH_REQUEST = 4009;
    private final static int OPUS_SET_COMPLEXITY_REQUEST = 4010;
    private final static int OPUS_GET_COMPLEXITY_REQUEST = 4011;
    private final static int OPUS_SET_INBAND_FEC_REQUEST = 4012;
    private final static int OPUS_GET_INBAND_FEC_REQUEST = 4013;
    private final static int OPUS_SET_PACKET_LOSS_PERC_REQUEST = 4014;
    private final static int OPUS_GET_PACKET_LOSS_PERC_REQUEST = 4015;
    private final static int OPUS_SET_DTX_REQUEST = 4016;
    private final static int OPUS_GET_DTX_REQUEST = 4017;
    private final static int OPUS_SET_VBR_CONSTRAINT_REQUEST = 4020;
    private final static int OPUS_GET_VBR_CONSTRAINT_REQUEST = 4021;
    private final static int OPUS_SET_FORCE_CHANNELS_REQUEST = 4022;
    private final static int OPUS_GET_FORCE_CHANNELS_REQUEST = 4023;
    private final static int OPUS_SET_SIGNAL_REQUEST = 4024;
    private final static int OPUS_GET_SIGNAL_REQUEST = 4025;
    private final static int OPUS_GET_LOOKAHEAD_REQUEST = 4027;
    /* final static int OPUS_RESET_STATE 4028 */
    ;
private final static int OPUS_GET_SAMPLE_RATE_REQUEST = 4029;
    private final static int OPUS_GET_FINAL_RANGE_REQUEST = 4031;
    private final static int OPUS_GET_PITCH_REQUEST = 4033;
    private final static int OPUS_SET_GAIN_REQUEST = 4034;
    private final static int OPUS_GET_GAIN_REQUEST = 4045;
    private final static int OPUS_SET_LSB_DEPTH_REQUEST = 4036;
    private final static int OPUS_GET_LSB_DEPTH_REQUEST = 4037;

    public final static long OPUS_CODEC = (1L << 34);
    final int ENCODER = 0;
    final int DECODER = 1;
    public static int CHANNELS = 1;
    private volatile ByteBuffer _enc; // C pointers for the encoder
    private volatile ByteBuffer _dec; // C pointers for the encoder

    private native int getDecoderSize(int chans);

    private native int getEncoderSize(int chans);

    private native void initEncoder(int rate, int channels, int application);

    private native void initDecoder(int rate, int channels);

    private native short[] opusDecode(byte[] wire, int doFec);

    private native byte[] opusEncode(short[] audio);

    private native void opusSetCtl(int ctl, int val, int eord);

    private native int opusGetCtl(int ctl, int eord);

    private native void freeCodec();
    private static boolean __loaded = false;

    // these can be set from the application.
    public static SampleRate PHONOSAMPLERATE = SampleRate.FM;
    public static Application PHONOAPPLICATION = Application.VOIP;

    public static boolean loadLib(String fullPathToLib) {
        try {
            if (!__loaded) {
                if (fullPathToLib == null) {
                    System.loadLibrary("phono-opus");
                    __loaded = true;
                } else {
                    System.load(fullPathToLib);
                    __loaded = true;
                }
            }
        } catch (java.lang.UnsatisfiedLinkError ex) {
            Log.warn("no suitable native libphono-opus :" + ex.getMessage());
        }
        return __loaded;
    }

    public OpusCodec() {
        int esz = getEncoderSize(CHANNELS);
        int dsz = getDecoderSize(CHANNELS);
        _enc = ByteBuffer.allocateDirect(esz);
        _dec = ByteBuffer.allocateDirect(dsz);
        Log.debug("initing native opus codec with rate=" + PHONOSAMPLERATE.Value + " ch=" + CHANNELS + " App=" + PHONOAPPLICATION.Value);
        initEncoder(PHONOSAMPLERATE.Value, CHANNELS, PHONOAPPLICATION.Value);
        initDecoder(PHONOSAMPLERATE.Value, CHANNELS);
    }

    @Override
    public int getFrameSize() {
        return -1;
    }

    @Override
    public int getFrameInterval() {
        return 20;
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
    public byte[] encode_frame(short[] audio) {
        byte[] ret = opusEncode(audio);
        return ret;
    }

    @Override
    public short[] decode_frame(byte[] bytes, boolean fec) {
        return opusDecode(bytes, fec ? 1 : 0);
    }


}
