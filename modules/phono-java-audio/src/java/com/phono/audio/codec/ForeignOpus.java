/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.phono.audio.codec;

import com.phono.srtplight.Log;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.ByteOrder;

/**
 *
 * @author thp
 */
class ForeignOpus {

    public static boolean loadLib(String fullPathToLib) {
        return true;
    }

    private MethodHandle opus_encoder_get_size_handle;
    private MethodHandle opus_decoder_get_size_handle;
    private Arena arena;
    private Linker linker;
    private SymbolLookup opusLib;
    private MemorySegment enc;
    private MemorySegment dec;
    private MethodHandle opus_encoder_init_handle;
    private MethodHandle opus_decoder_init_handle;
    private MethodHandle opus_decode_handle;
    private int maxaudio;
    private MemorySegment audio;
    private MemorySegment nwire;
    private int MAX_PKT_SZ = 1200;

    ForeignOpus() {
        try {
            arena = Arena.ofShared();
            linker = Linker.nativeLinker();
            opusLib = SymbolLookup.libraryLookup("libopus.so", arena);

        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(0);
        }
    }

    protected int getDecoderSize(int chans) {
        long sz = 0;
        try {
            if (opus_decoder_get_size_handle == null) {
                var opus_coder_get_size_sig = FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT);

                var found = opusLib.find("opus_decoder_get_size");
                MemorySegment opus_decoder_get_size_addr = found.orElseThrow();
                opus_decoder_get_size_handle = linker.downcallHandle(opus_decoder_get_size_addr, opus_coder_get_size_sig);
            }
            sz = (long) opus_decoder_get_size_handle.invokeExact(chans);
            Log.info("decoder is " + sz);
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(0);
        }
        maxaudio = 48 * chans * 60;
        return (int) sz;
    }

    protected int getEncoderSize(int chans) {

        long sz = 0;
        try {
            if (opus_encoder_get_size_handle == null) {
                var opus_coder_get_size_sig = FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT);

                var found = opusLib.find("opus_encoder_get_size");
                MemorySegment opus_encoder_get_size_addr = found.orElseThrow();
                opus_encoder_get_size_handle = linker.downcallHandle(opus_encoder_get_size_addr, opus_coder_get_size_sig);
            }
            sz = (long) opus_encoder_get_size_handle.invokeExact(chans);
            Log.info("encoder is " + sz);
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(0);
        }
        return (int) sz;
    }

    /*
            int esz = getEncoderSize(CHANNELS);
        int dsz = getDecoderSize(CHANNELS);
        _enc = ByteBuffer.allocateDirect(esz);
        _dec = ByteBuffer.allocateDirect(dsz);
     */
    protected void initEncoder(int rate, int channels, int application) {

        /*
        OPUS_EXPORT int opus_encoder_init(
    OpusEncoder *st,
    opus_int32 Fs,
    int channels,
    int application)
         */
        try {
            int esz = getEncoderSize(channels);
            if (opus_encoder_init_handle == null) {
                var sig = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT);
                var addr = opusLib.find("opus_encoder_init").get();
                opus_encoder_init_handle = linker.downcallHandle(addr, sig);
            }
            enc = arena.allocate(esz);
            var ret = (int) opus_encoder_init_handle.invokeExact(enc, rate, channels, application);
            Log.info("encoder init ret was " + ret);
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(0);
        }
    }

    protected void initDecoder(int rate, int channels) {
        try {
            int sz = getDecoderSize(channels);
            if (opus_decoder_init_handle == null) {
                var sig = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT);
                var addr = opusLib.find("opus_decoder_init").get();
                opus_decoder_init_handle = linker.downcallHandle(addr, sig);
            }
            dec = arena.allocate(sz);
            var ret = (int) opus_decoder_init_handle.invokeExact(dec, rate, channels);
            Log.info("decoder init ret was " + ret);
            audio = arena.allocate(2 * maxaudio);
            nwire = arena.allocate(MAX_PKT_SZ);

        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(0);
        }
    }

    protected short[] opusDecode(byte[] wire, int doFec) {
        /*
        OPUS_EXPORT OPUS_WARN_UNUSED_RESULT int opus_decode(
    OpusDecoder *st,
    const unsigned char *data,
    opus_int32 len,
    opus_int16 *pcm,
    int frame_size,
    int decode_fec
)
         */
        short[] dst;
        try {
            if (opus_decode_handle == null) {
                var sig = FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, //decoder
                        ValueLayout.ADDRESS, //wire data
                        ValueLayout.JAVA_INT, //data length
                        ValueLayout.ADDRESS, // output buffer
                        ValueLayout.JAVA_INT, // size of output buffer
                        ValueLayout.JAVA_INT // decode fec flag
                );
                var addr = opusLib.find("opus_decode").get();
                opus_decode_handle = linker.downcallHandle(addr, sig);
            }
            synchronized (audio) {
                nwire.asByteBuffer().put(wire);
                var al = (int) opus_decode_handle.invokeExact(dec, nwire, wire.length, audio, maxaudio * 2, doFec);
                dst = new short[al];
                var ab = audio.asByteBuffer().order(ByteOrder.nativeOrder()).asShortBuffer().get(dst);
            }
            Log.info("decoded data is " + dst.length);
        } catch (Throwable x) {
            x.printStackTrace();
            System.exit(0);
            dst = new short[0];
        }
        return dst;
    }

    protected byte[] opusEncode(short[] audio) {
        System.exit(0);

        return new byte[0];
    }

    protected void opusSetCtl(int ctl, int val, int eord) {

    }

    protected int opusGetCtl(int ctl, int eord) {
        return 0;
    }

    protected void freeCodec() {

    }

    public static void main(String args[]) {
        var fc = new ForeignOpus();
    }
}
