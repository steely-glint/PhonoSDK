/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.phono.applet.rtp;

import com.phono.api.Codec;
import com.phono.api.CodecList;
import com.phono.applet.audio.phone.PhonoAudioShim;
import com.phono.applet.audio.phone.Play;
import com.phono.audio.phone.PhonoAudioPropNames;
import com.phono.rtp.Endpoint;
import com.phono.srtplight.Log;
import java.io.IOException;
import java.io.StringReader;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

/**
 *
 * @author thp
 */
public class Share {

    private PhonoAudioShim _audio;
    final private Hashtable _endpoints = new Hashtable();
    private CodecList _codecList;
    private boolean _userClickedTrust = false;
    private String _deviceList;

    Share() {
        super();
        this.init();
    }

    Share(String uri) {
        this();
        Codec codecs[] = this.codecs();
        Codec codec = codecs[0];

        for (Codec c : codecs) {
            Log.debug("Codec name " + c.name);
            if ("g722".equals(c.name)) {
                codec = c;
                break;
            }

        }

        this.share(uri, codec, true);
    }

    static public void main(String argv[]) {
        Log.setLevel(Log.DEBUG);
        if (argv.length >= 1) {
            Share me = new Share(argv[0]);
            if (argv.length == 2){
                me.setAudioIn(argv[1]);
            }
        } else {
            StringBuffer bret = new StringBuffer("{\n");
            PhonoAudioShim.getMixersJSON(bret);
            bret.append("}\n");
            System.out.print("audio = "+bret);
        }

    }

    public void init() {

        StringBuffer bret = new StringBuffer("{\n");
        PhonoAudioShim.getMixersJSON(bret);
        bret.append("}\n");
        _deviceList = bret.toString();
//	Log.debug("audio list is :"+_deviceList);
        _audio = new PhonoAudioShim();
        _audio.setAudioProperty(PhonoAudioPropNames.DOEC, "false");
        _codecList = new CodecList(_audio);

        // Call the callback that we have been given to say we are ready
    }

    public String allocateEndpoint() {
        String ret = null;
        // strictly we supposedly want to actually allocate a socket here,
        // but there is _really_ no point so we make plausible something up
        synchronized (_endpoints) {

            Endpoint r = null;
            try {
                r = Endpoint.allocate();
                _endpoints.put(r.getLocalURI(), r);
                ret = r.getLocalURI();
            } catch (SocketException ex) {
                Log.error("Problem allocating a socket " + ex.getMessage());
            }
        }
        return ret;
    }

    public void freeEndpoint(String uri) {
        synchronized (_endpoints) {
            Endpoint e = (Endpoint) _endpoints.get(uri);
            if (e != null) {
                e.release();
            }
            _endpoints.remove(e);
        }
    }
    // see http://download.oracle.com/javase/7/docs/api/java/security/AccessController.html
    // for doc on priv escalation

    public Codec[] codecs() {
        return _codecList.getCodecs();
    }

    public Codec mkCodec(Codec c, int ptype) {
        Codec ret = new Codec(ptype, c.name, c.rate, c.ptime, c.iaxcn);
        return ret;
    }

    public com.phono.api.Share share(String uri, final Codec codec, boolean autoStart) {
        return share(uri, codec, autoStart, null, null);
    }

    /**
     *
     * @param uri rtp://localhost:port<:remotehost:remoteport>
     * @param autoPlay start immediatly
     * @param codec Selected codec.
     * @param srtp crypto params
     * @return
     */
    public com.phono.api.Share share(String uri, final Codec codec, boolean autoStart, String srtpPropsl, String srtpPropsr) {
        com.phono.api.Share ret = null;
        com.phono.api.Share s = null;
        Properties spl = null;
        Properties spr = null;

        if ((srtpPropsl != null) && (srtpPropsl.length() > 0)) {
            StringReader reader = new StringReader(srtpPropsl);
            spl = new Properties();
            try {
                spl.load(reader);
            } catch (IOException ex) {
                Log.error("srtp Props invalid format" + ex.toString());
            }
        }
        if ((srtpPropsr != null) && (srtpPropsr.length() > 0)) {
            StringReader reader = new StringReader(srtpPropsr);
            spr = new Properties();
            try {
                spr.load(reader);
            } catch (IOException ex) {
                Log.error("srtp Props invalid format" + ex.toString());
            }
        }
        Log.debug("in share() codec = " + codec.name + " rate =" + codec.rate + " pt = " + codec.pt);
        Log.debug("in share() uri = " + uri);
        try {
            PhonoAudioShim af = getAudio(codec);
            s = new com.phono.api.Share(uri, af, codec.pt, spl, spr);

            _audio.init(codec.iaxcn, 100);

            String luri = s.getLocalURI();
            synchronized (_endpoints) {
                Endpoint e = (Endpoint) _endpoints.get(luri);
                if (e != null) {
                    e.setShare(s);
                } else {
                    e = new Endpoint(luri);
                    e.setShare(s);
                    Log.warn("Unexpected local endpoint used :" + luri);
                    _endpoints.put(luri, e);

                }
            }
            // should check auto start here...
            if (autoStart) {
                s.start();
            }
            ret = s; // only return the share if no exceptions ....
        } catch (Exception ex) {
            if (s != null) {
                s.stop(); // minimal cleanup on errors.
            }
            Log.error(ex.toString());                // do something useful here....
        }
        return ret;
    }

    public Play play(final String uri, boolean autoStart) {
        Play s = null;
        Log.debug("in play() uri = " + uri);
        try {
            s = new Play(uri);
            if (autoStart) {
                s.start();
            }
        } catch (Exception ex) {
            Log.error(ex.toString());                // do something useful here....
        }
        return s;
    }

    public String getJSONStatus() {
        StringBuffer ret = new StringBuffer("{\n");
        ret.append("userTrust").append(" : ");
        ret.append(_userClickedTrust ? "true" : "false").append(",\n");
        Enumeration rat = _endpoints.elements();
        ret.append("endpoints").append(" : ");
        ret.append("[");
        while (rat.hasMoreElements()) {
            Endpoint r = (Endpoint) rat.nextElement();
            r.getJSONStatus(ret);
            if (rat.hasMoreElements()) {
                ret.append(",");
            }
        }
        ret.append("]\n");
        ret.append("}\n");
        //Log.debug("Status is "+ret.toString());
        return ret.toString();
    }

    public String getAudioDeviceList() {
        return _deviceList;
    }

    public void setAudioIn(String ain) {
        if (_audio != null) {
            _audio.setAudioInName(ain);
            Log.debug("Set audio input device preference to " + ain);
        }
    }

    private PhonoAudioShim getAudio(Codec codec) {
        if (_audio != null) {
            try {
                float ofreq = (_audio.getCodec(_audio.getCodec())).getSampleRate();
                float nfreq = (_audio.getCodec(codec.iaxcn)).getSampleRate();
                Log.debug("getting audio is " + ofreq + " = " + nfreq + " ? " + ((nfreq != ofreq) ? "No" : "Yes"));

                if (nfreq != ofreq) {
                    _audio.unInit();
                    return null; // nothing to return
                }
            } catch (IllegalStateException ok) {
                // thats actually legit - it is an uninitialized audio
                // so we haven't _set_ a rate yet
            }
        }
        return _audio;
    }
}
