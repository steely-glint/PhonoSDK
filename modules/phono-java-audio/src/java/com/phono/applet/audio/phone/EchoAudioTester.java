
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
import com.phono.audio.codec.OpusCodec;
import com.phono.audio.codec.opus.PureOpusCodec;
import com.phono.audio.phone.PhonoAudioPropNames;
import com.phono.srtplight.Log;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tim Loops audio through a codec. Use a headset.
 */
public class EchoAudioTester implements AudioReceiver {

    private PhonoAudioShim aud;
    private final int step;

    EchoAudioTester(String codec) throws AudioException {
        aud = new PhonoAudioShim();
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
        Log.debug("step =" +step);
    }

    public void start() {
        aud.startRec();
        aud.startPlay();
    }

    public void stop() {
        aud.stopPlay();
        aud.stopRec();
    }

    @Override
    public void newAudioDataReady(AudioFace a, int bytesAvailable) {
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
        int num = sa.getStamp()/step;
        if ((num % 50) == 17) {
            Log.debug("skip stamp " + sa.getStamp());
        } else {
            a.writeStampedAudio(sa);
        }
    }

    public static void main(String argv[]) {
        String codecName = "GSM";
        PureOpusCodec.PHONOSAMPLERATE = OpusCodec.SampleRate.HD;
        PureOpusCodec.PHONOAPPLICATION = OpusCodec.Application.VOIP;
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
