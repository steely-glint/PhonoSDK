
/*
 * Copyright 2020 pi.pe gmbh.
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
package com.phono.applet.audio.phone;

import com.phono.audio.AudioException;
import com.phono.audio.AudioFace;
import com.phono.audio.AudioReceiver;
import com.phono.audio.StampedAudio;
import com.phono.audio.codec.CodecFace;
import com.phono.audio.codec.DecoderFace;
import com.phono.audio.codec.EncoderFace;
import com.phono.audio.codec.OpusCodec;
import com.phono.audio.codec.opus.PureOpusCodec;
import com.phono.audio.phone.PhonoAudioPropNames;
import com.phono.srtplight.Log;

/**
 *
 * @author tim Loops audio through a codec. Use a headset.
 */
public class EchoAudioTester implements AudioReceiver {

    private PhonoAudioShim aud;
    private final int step;

    class FrankenCodec implements CodecFace {

        CodecFace base;
        EncoderFace enc;
        DecoderFace dec;

        FrankenCodec(EncoderFace enc, DecoderFace dec) {
            Log.info("Making Frankencodec - to test compatibility between native and pure");
            if (enc instanceof CodecFace) {
                base = (CodecFace) enc;
            }
            this.enc = enc;
            this.dec = dec;
        }

        @Override
        public int getFrameSize() {
            return base.getFrameSize();
        }

        @Override
        public int getFrameInterval() {
            return base.getFrameInterval();
        }

        @Override
        public long getCodec() {
            return base.getCodec();
        }

        @Override
        public DecoderFace getDecoder() {
            return dec;
        }

        @Override
        public EncoderFace getEncoder() {
            return enc;
        }

        @Override
        public String getName() {
            return base.getName();
        }

        @Override
        public float getSampleRate() {
            return base.getSampleRate();
        }

    };

    EchoAudioTester(String codec) throws AudioException {
        aud = new PhonoAudioShim() {
            protected void fillCodecMap() {
                super.fillCodecMap();
                CodecFace nat = _codecMap.get(PureOpusCodec.AUDIO_OPUS);
                Log.info("opus codec is "+nat.getClass().getSimpleName());
                if ((nat != null) && (nat instanceof OpusCodec)) {
                    PureOpusCodec dec = new PureOpusCodec();
                    FrankenCodec mixed = new FrankenCodec((PureOpusCodec) dec, (DecoderFace)nat);
                    this._codecMap.put(mixed.getCodec(), mixed);
                    Log.info("using FrankenCodec");
                } else {
                    Log.info("No native opus codec found");
                }
            }
        };
        aud.addAudioReceiver(this);
        aud.setAudioProperty(PhonoAudioPropNames.DOEC, "false");

        Long codecL = CodecFace.AUDIO_ULAW;
        long[] codecs = aud.getCodecs();
        for (long c : codecs) {
            CodecFace co = aud.getCodec(c);
            if (co.getName().equals(codec)) {
                codecL = c;
                break;
            }
        }
        aud.init(codecL, 60);
        this.step = aud.getFrameInterval();
        Log.debug("step =" + step);
    }

    public void start() {
        aud.startRec();
        //aud.startPlay();
    }

    public void stop() {
        aud.stopPlay();
        aud.stopRec();
    }
    int count = 0;

    @Override
    public void newAudioDataReady(AudioFace a, int bytesAvailable) {
        if (count <=5) {
            count++;
        }
        if (count == 5) {
            aud.startPlay();
        }
        try {
            StampedAudio sa = a.readStampedAudio();
            while (sa != null) {
                mayBeWriteAudio(a, sa);
                Log.debug("read " + sa.getStamp());
                sa = a.readStampedAudio();
            }
        } catch (AudioException ex) {
            Log.warn("newAudioData problem " + ex.getMessage());
        }
    }

    public void mayBeWriteAudio(AudioFace a, StampedAudio sa) throws AudioException {
        int num = sa.getStamp() / step;
        if ((num % 50) == 17) {
            Log.debug("skip stamp " + sa.getStamp());
        } else {
            a.writeStampedAudio(sa);
        }
    }

    public static void main(String argv[]) {
        String codecName = "OPUS";
        PureOpusCodec.PHONOSAMPLERATE = OpusCodec.SampleRate.FM;
        PureOpusCodec.PHONOAPPLICATION = OpusCodec.Application.VOIP;
        PureOpusCodec.FRAMEINTERVAL = 20;
        Log.setLevel(Log.VERB);
        if (argv.length > 0) {
            codecName = argv[0];
        }
        try {
            EchoAudioTester t = new EchoAudioTester(codecName);
            t.start();
            Thread.sleep(100000);
            t.stop();

        } catch (AudioException ex) {
            Log.error("failed to load " + ex.getMessage());
        } catch (InterruptedException ex) {
            ;
        }

    }

}
