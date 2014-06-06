function JSEPAudio(phono, config, callback) {
    this.type = "jsep";

    Phono.log.info("Initialize JSEP");
    if (typeof (webkitAudioContext) !== 'undefined') {
        Phono.log.info("Have webkitAudio def");
        JSEPAudio.webAudioContext = new webkitAudioContext();
    } else if (typeof (AudioContext) !== 'undefined') {
        Phono.log.info("Have AudioContext def");
        JSEPAudio.webAudioContext = new AudioContext();
    } else if (typeof (mozAudioContext) !== 'undefined') {
        Phono.log.info("Have mozAudio def");
        JSEPAudio.webAudioContext = new mozAudioContext();
    } else {
        Phono.log.info("No webAudio available - so no freep");
    }

    if (typeof webkitRTCPeerConnection == "function") {
        JSEPAudio.GUM = function(p, s, f) {
            navigator.webkitGetUserMedia(p, s, f)
        };
        JSEPAudio.mkPeerConnection = function(a, b) {
            return new webkitRTCPeerConnection(a, b);
        };
        JSEPAudio.mkSessionDescription = function(a) {
            return new RTCSessionDescription(a);
        };
        JSEPAudio.createObjectURL = function(s) {
            return webkitURL.createObjectURL(s);
        };
        JSEPAudio.stun = "stun:stun.l.google.com:19302";
        JSEPAudio.attachMediaStream = function(element, stream) {
            element.src = webkitURL.createObjectURL(stream);
        };
        JSEPAudio.stripCrypto = function(sdpObj) {
            return sdpObj;
        };
        JSEPAudio.AudioUrl = function(url) {
            return url.replace(".mp3", ".ogg");
        };
        JSEPAudio.addCreateConstraint = function(constraint) {
            return constraint;
        };
    } else if (typeof mozRTCPeerConnection == "function") {
        JSEPAudio.GUM = function(p, s, f) {
            navigator.mozGetUserMedia(p, s, f)
        };
        JSEPAudio.mkPeerConnection = function(a, b) {
            return new mozRTCPeerConnection(a, b);
        };
        JSEPAudio.mkSessionDescription = function(a) {
            return new mozRTCSessionDescription(a);
        };
        JSEPAudio.createObjectURL = function(s) {
            return URL.createObjectURL(s);
        };
        JSEPAudio.stun = "stun:23.21.150.121";
        JSEPAudio.attachMediaStream = function(element, stream) {
            element.mozSrcObject = stream;
            element.play();
        };
        JSEPAudio.stripCrypto = function(sdpObj) {
            Phono.util.each(sdpObj.contents, function() {
                //if(this.rtcp) {delete this.rtcp;};
                //if(this['rtcp-mux']) {delete this['rtcp-mux'];};
                if (this.crypto) {
                    delete this.crypto;
                }
                ;
                //if(this.ssrc) { delete this.ssrc;};
                //if(this.mid) { delete this.mid;};
            });
            //if (sdpObj.group) {delete sdpObj.group;};
            return sdpObj;
        };
        JSEPAudio.AudioUrl = function(url) {
            return url.replace(".mp3", ".ogg");
        };
        JSEPAudio.addCreateConstraint = function(constraint) {
            constraint.mandatory.MozDontOfferDataChannel = true;
            return constraint;
        };
    }
    JSEPAudio.spk = 0.0;
    JSEPAudio.mic = 0.0;
    this.config = Phono.util.extend({
        gatewayType: "tropo",
        media: {
            audio: true,
            video: false
        }
    }, config);

    var plugin = this;

    var localContainerId = this.config.localContainerId;

    // Create audio continer if user did not specify one
    if (!localContainerId) {
        this.config.localContainerId = this.createContainer(this.config.media['video']);
    }

    JSEPAudio.localVideo = document.getElementById(this.config.localContainerId);

    callback(plugin);
}

JSEPAudio.exists = function() {
    if (typeof webkitRTCPeerConnection == "function")
        return true;
    if (typeof mozRTCPeerConnection == "function") return true;
}

JSEPAudio.prototype.getCaps = function(c) {
    return c.c(this.type).up();
};

JSEPAudio.count = 0;
JSEPAudio.toneMap = {
    '0': [1336, 941],
    '1': [1209, 697],
    '2': [1336, 697],
    '3': [1477, 696],
    '4': [1209, 770],
    '5': [1336, 770],
    '6': [1477, 770],
    '7': [1209, 852],
    '8': [1336, 852],
    '9': [1447, 852],
    '*': [1209, 941],
    '#': [1477, 941]
};

// JSEPAudio Functions
//
// =============================================================================================

// Creates a new Player and will optionally begin playing
JSEPAudio.prototype.play = function(transport, autoPlay) {
    var url = null;
    var audioPlayer = null;
    if (transport.uri) {
        url = JSEPAudio.AudioUrl(transport.uri);
    }
    var player = null;
    var context = JSEPAudio.webAudioContext;
    if (context) {
        player = {
            stopped: false,
            url: function() {
                return url;
            },
            start: function() {
                if (url) {
                    var request = new XMLHttpRequest();
                    request.open('GET', url, true);
                    request.responseType = 'arraybuffer';
                    // Decode asynchronously
                    request.onload = function() {
                        context.decodeAudioData(request.response, function(buffer) {
                            Phono.log.info("Loaded audio from " + url);
                            if (!this.stopped) {
                                audioPlayer = context.createBufferSource(); // creates a sound source
                                audioPlayer.buffer = buffer;                    // tell the source which sound to play
                                audioPlayer.connect(context.destination);       // connect the source to the context's destination (the speakers)
                                audioPlayer.loop = true;
                                audioPlayer.start(0);
                            }
                        }, function() {
                            Phono.log.info("failed to load audio from " + url)
                        });
                    };
                    request.send();
                }
            },
            stop: function() {
                if (audioPlayer)
                    audioPlayer.stop(0);
                this.stopped = true;
                audioPlayer = null;
            },
            volume: function(value) {
                if (arguments.length === 0) {
                    return transport.volume * 100;
                }
                else {
                    transport.volume = (value / 100);
                }
            }
        };
    } else {
        player = {
            url: function() {
                return url;
            },
            start: function() {
                if (url) {
                    audioPlayer = new Audio(url);
                    var loop = function() {
                        audioPlayer = new Audio(url);
                        audioPlayer.play();
                        audioPlayer.addEventListener('ended', loop);
                    }
                    loop();
                }
            },
            stop: function() {
                if (audioPlayer)
                    audioPlayer.pause();
                audioPlayer = null;
            },
            volume: function(value) {
                if (arguments.length === 0) {
                    return transport.volume * 100;
                }
                else {
                    transport.volume = (value / 100);
                }
            }
        };
    }
    return player;
};

// Creates a new audio Share and will optionally begin playing
JSEPAudio.prototype.share = function(transport, autoPlay, codec) {
    var share = {
        // Readonly
        url: function() {
            // No Share URL
            return null;
        },
        codec: function() {
            return codec;
        },
        // Control
        start: function() {
            // Audio started automatically
            return null;
        },
        stop: function() {
            if (JSEPAudio.localStream) {
                JSEPAudio.localStream.stop();
            }
        },
        // Properties
        gain: function(value) {
            // We have no control over this
            return null;
        },
        mute: function(value) {
            var tracks = [];
            if (JSEPAudio.localStream.getAudioTracks) {
                tracks = JSEPAudio.localStream.getAudioTracks();
            }
            if (arguments.length === 0) {
                var muted = true;
                Phono.util.each(tracks, function() {
                    if (this.enabled == true)
                        muted = false;
                });
                return muted;
            }
            if (value == true) {
                Phono.util.each(tracks, function() {
                    this.enabled = false;
                });
            } else {
                Phono.util.each(tracks, function() {
                    this.enabled = true;
                });
            }
        },
        suppress: function(value) {
            // Echo canceller is on always
            return null;
        },
        energy: function() {
            if ((JSEPAudio.pc) && (JSEPAudio.pc.getStats)) {
                JSEPAudio.pc.getStats(function(stats) {
                    var sr = stats.result();
                    for (var i = 0; i < sr.length; i++) {
                        var obj = sr[i].remote;
                        if (obj) {
                            var nspk = 0.0;
                            var nmic = 0.0;
                            if (obj.stat('audioInputLevel')) {
                                nmic = obj.stat('audioInputLevel');
                            }
                            if (nmic > 0.0) {
                                JSEPAudio.mic = Math.floor(Math.max((Math.LOG2E * Math.log(nmic) - 4.0), 0.0));
                            }
                            if (obj.stat('audioOutputLevel')) {
                                nspk = obj.stat('audioOutputLevel');
                            }
                            if (nspk > 0.0) {
                                JSEPAudio.spk = Math.floor(Math.max((Math.LOG2E * Math.log(nspk) - 4.0), 0.0));
                            }
                        }
                    }
                });
            }
            return {
                mic: JSEPAudio.mic,
                spk: JSEPAudio.spk
            };

        },
        secure: function() {
            return true;
        },
        freep: function(value, duration, audible) {
            if (audible) {
                var context = JSEPAudio.webAudioContext;
                if (context) {
                    var note1;
                    var note2;
                    if (duration < 100)
                        duration = 100;// sensible sound
                    note1 = context.createOscillator();
                    note2 = context.createOscillator();
                    note1.connect(context.destination);
                    note2.connect(context.destination);

                    var twoTone = JSEPAudio.toneMap[value];
                    note1.frequency.value = twoTone[0];
                    note2.frequency.value = twoTone[1];
                    note1.start(0.0);
                    note2.start(0.0);
                    window.setTimeout(
                            function() {
                                note1.stop(0.0);
                                note2.stop(0.0);
                            }, duration);
                }
            }
        }
    };
    if (JSEPAudio.pc.createDTMFSender) {
        share.digit = function(values, duration, audible) {
            if (JSEPAudio.dtmfSender) {
                JSEPAudio.dtmfSender.insertDTMF(values);
                this.freep(values, duration, audible);
            }
        };
    }

    return share;
};

JSEPAudio.prototype.showPermissionBox = function(callback) {
    Phono.log.info("Requesting access to local media");

    JSEPAudio.GUM({
        'audio': this.config.media['audio'],
        'video': this.config.media['video']
    },
    function(stream) {
        JSEPAudio.localStream = stream;
        JSEPAudio.localVideo.style.opacity = 1;
        JSEPAudio.attachMediaStream(JSEPAudio.localVideo, stream);
        JSEPAudio.localVideo.muted = "muted";
        if (typeof callback == 'function')
            callback(true);
    },
            function(error) {
                Phono.log.info("Failed to get access to local media. Error code was " + error.code);
                alert("Failed to get access to local media. Error code was " + error.code + ".");
                if (typeof callback == 'function')
                    callback(false);
            });

};

JSEPAudio.prototype.permission = function() {
    return (JSEPAudio.localStream != undefined);
};


// Returns an object containg JINGLE transport information
JSEPAudio.prototype.transport = function(config) {
    var pc;
    var inboundOffer;
    var configuration;
    var offerconstraints;
    var peerconstraints;
    var remoteContainerId;
    var complete = false;
    var audio = this;
    var candidateCount = 0;
    var remoteCandidates = [];
    var haveRemoteDescription = false;
    var dtlns = null ;
    var localmunger = "none";



    offerconstraints = {
        'mandatory': {
            'OfferToReceiveAudio': this.config.media['audio'],
            'OfferToReceiveVideo': this.config.media['video']
        }
    };
    offerconstraints = JSEPAudio.addCreateConstraint(offerconstraints);
    peerconstraints = {
        'optional': [{'DtlsSrtpKeyAgreement': 'true'}]
    };
    if (!this.config || !this.config.iceServers) {
        configuration = {'iceServers': [{url: JSEPAudio.stun}]};
    } else {
        configuration = {'iceServers': this.config.iceServers};
    }

    if (!config || !config.remoteContainerId) {
        if (this.config.remoteContainerId) {
            remoteContainerId = this.config.remoteContainerId;
        } else {
            remoteContainerId = this.createContainer(this.config.media['video']);
        }
    } else {
        remoteContainerId = config.remoteContainerId;
    }
    if (this.config && this.config.gatewayType) {
        if (this.config.gatewayType == "xep-0320"){
            dtlns="urn:xmpp:jingle:apps:dtls:0";
        }
    }
    if (this.config && this.config.localSettings) {
        localmunger = this.config.localSettings;
    }

    var remoteVideo = document.getElementById(remoteContainerId);
    var addRemoteCandidates = function() {
        var candidate;
        while (candidate = remoteCandidates.pop()) {
            var rtice = new RTCIceCandidate({candidate: candidate, sdpMLineIndex:0,sdpMid:"audio"});
            Phono.log.info("adding a remote ice candidate "+JSON.stringify(candidate));
            pc.addIceCandidate(rtice);
        }
    };
    return {
        name: "urn:xmpp:jingle:transports:ice-udp:1",
        buildTransport: function(direction, j, callback, u, updateCallback) {
            Phono.log.info("made a peer connection");
            pc = JSEPAudio.mkPeerConnection(configuration, peerconstraints);
            JSEPAudio.pc = pc;
            var oic = function(evt) {
                if (!complete) {
                    if ((evt.candidate == null)) {//||
                        //(candidateCount >= 1 && !audio.config.media['video'] && direction == "answer")) {
                        Phono.log.info("All Ice candidates in ");
                        complete = true;
                        var sdp = pc.localDescription.sdp;
                        Phono.log.info('SDP ' + JSON.stringify(sdp));
                        var sdpObj = Phono.sdp.parseSDP(sdp);
                        Phono.log.info('SdpObj ' + JSON.stringify(sdpObj));
                        Phono.sdp.buildJingle(j, sdpObj, dtlns);
                        var codecId = 0;
                        if (sdpObj.contents[0].codecs[0].name == "telephone-event")
                            codecId = 1;
                        var codec =
                                {
                                    id: sdpObj.contents[0].codecs[codecId].id,
                                    name: sdpObj.contents[0].codecs[codecId].name,
                                    rate: sdpObj.contents[0].codecs[codecId].clockrate
                                };
                        callback(codec);
                    } else {
                        Phono.log.info("An Ice candidate ");
                        candidateCount += 1;
                    }
                }
            };
            pc.onicecandidate = oic;
            //pc.onconnecting = function(message) {Phono.log.info("onSessionConnecting.");};
            //pc.onopen = function(message) {Phono.log.info("onSessionOpened.");};
            pc.onaddstream = function(event) {
                Phono.log.info("onAddStream. Attaching");
                JSEPAudio.attachMediaStream(remoteVideo, event.stream);
                remoteVideo.style.opacity = 1;
                if ((JSEPAudio.localStream != null) && (JSEPAudio.pc.createDTMFSender)) {
                    var local_audio_track = JSEPAudio.localStream.getAudioTracks()[0];
                    JSEPAudio.dtmfSender = JSEPAudio.pc.createDTMFSender(local_audio_track);
                    Phono.log.debug("Created DTMF Sender");
                    JSEPAudio.dtmfSender.ontonechange = function(tone) {
                        if (tone) {
                            Phono.log.debug("sent Dtmf tone: \t" + tone.tone);
                        }
                    };
                }
            };
            //pc.onremovestream = function (event) {Phono.log.info("onRemoveStream."); };
            //pc.onicechange= function (event) {Phono.log.info("onIceChange: "+pc.iceState); };
            //pc.onnegotiationneeded = function (event) {Phono.log.info("onNegotiationNeeded."); };
            //pc.onstatechange = function (event) {Phono.log.info("onStateChange: "+pc.readyState); };

            Phono.log.debug("Adding localStream");
            var mungeLocal = {
                active: function(ldesc) {
                    var sdpLines = ldesc.sdp.split('\r\n');
                    // remove a=rtcp:
                    var replacement = ["a=setup:active"];
                    for (var i = 0; i < sdpLines.length; i++) {
                        if (sdpLines[i].search("a=setup:") == 0) {
                            sdpLines.splice(i, 1, replacement);
                        }

                    }
                    return {
                        'sdp': sdpLines.join('\r\n'),
                        'type': ldesc.type
                    };
                },
                none: function(ldesc) {
                    return ldesc;
                },
                xlbw: function(ldesc) {
                    // not clear this is correct....
                    var sdpLines = ldesc.sdp.split('\r\n');
                    // set opus to low bw
                    for (var i = 0; i < sdpLines.length; i++) {
                        if (sdpLines[i].search("a=rtpmap:") == 0) {
                            var bits = sdpLines[i].split(" ");
                            if (bits[1].search("opus") == 0) {
                                var num = bits[0].split(":")[1];
                                var line = "a=fmtp:" + num + " minptime=50; maxaveragebitrate=8000;";
                                sdpLines.splice(i, 0, [line]);
                            }
                        }
                        if (sdpLines[i].search("a=ice-options:google-ice") == 0) {
                            sdpLines.splice(i, 1);
                        }
                    }
                    return {
                        'sdp': sdpLines.join('\r\n'),
                        'type': ldesc.type
                    };
                },
                lbv: function(ldesc) {
                    var sdpLines = ldesc.sdp.split('\r\n');
                    for (var i = 0; i < sdpLines.length; i++) {
                        if (sdpLines[i].search("a=mid:video") == 0) {
                            var line = "b=AS:256" ;
                            i++;
                            sdpLines.splice(i, 0, line);
                        }
                    }
                    return {
                        'sdp': sdpLines.join('\r\n'),
                        'type': ldesc.type
                    };
                }
            }

            var cb2 = function() {
                pc.addStream(JSEPAudio.localStream);
                var setlfail = function(e) {
                    Phono.log.error('failed to setlocal ' + +JSON.stringify(e));
                };
                var setlok = function() {
                    Phono.log.info('setlocal ok');
                };

                var cb = function(localDesc) {
                    var nlocalDesc = mungeLocal[localmunger](localDesc);
                    var sd = JSEPAudio.mkSessionDescription(nlocalDesc);
                    pc.setLocalDescription(sd, setlok, setlfail);
                    window.setTimeout(function() {
                        oic({})
                    }, 1000);
                    Phono.log.info('Set local description ' + JSON.stringify(localDesc));
                };
                var offerfail = function(e) {
                    Phono.log.error('failed to create offer ' + JSON.stringify(e));
                };
                var ansfail = function(e) {
                    Phono.log.error('failed to create answer ' + JSON.stringify(e));
                };

                if (direction == "answer") {
                    Phono.log.info('Set remote description ' + JSON.stringify(inboundOffer));
                    pc.setRemoteDescription(inboundOffer,
                            function() {
                                haveRemoteDescription = true;
                                Phono.log.debug("remoteDescription happy");
                                pc.createAnswer(cb, ansfail);
                                addRemoteCandidates();
                            },
                            function(e) {
                                Phono.log.error("remoteDescription sad " + JSON.stringify(e));
                            });
                } else {
                    Phono.log.info('create offer with  ' + JSON.stringify(offerconstraints));
                    pc.createOffer(cb, offerfail, offerconstraints);
                }
            }

            if (audio.permission()) {
                cb2();
            } else {
                audio.showPermissionBox(cb2);
            }
        },
        processTransport: function(t, update, iq) {
            var sdpObj = Phono.sdp.parseJingle(iq);
            Phono.log.info('Made remote sdp Obj' + JSON.stringify(sdpObj));
            sdpObj = JSEPAudio.stripCrypto(sdpObj);
            if (update) {
                Phono.log.info('Rcving transport info for peer connection');
                var candys = sdpObj.contents[0].candidates;
                Phono.util.each(candys, function() {
                    var candidate = Phono.sdp.buildCandidate(this);
                    remoteCandidates.push(candidate);
                    Phono.log.info('pushed candidate ' + candidate);
                });
                if (haveRemoteDescription) {
                    addRemoteCandidates();
                }
            } else {
                var sdp = Phono.sdp.buildSDP(sdpObj);
                Phono.log.info('constructed remote sdp ' + JSON.stringify(sdp));
                var codecId = 0;
                if (sdpObj.contents[0].codecs[0].name == "telephone-event")
                    codecId = 1;
                var codec =
                        {
                            id: sdpObj.contents[0].codecs[codecId].id,
                            name: sdpObj.contents[0].codecs[codecId].name,
                            rate: sdpObj.contents[0].codecs[codecId].clockrate
                        };

                if (pc) {
                    // We are an answer to an outbound call
                    Phono.log.info('Got remote sdp ' + JSON.stringify(sdp));
                    var sd = JSEPAudio.mkSessionDescription({
                        'sdp': sdp,
                        'type': "answer"
                    });
                    Phono.log.info('Set remote description ' + JSON.stringify(sd));
                    pc.setRemoteDescription(sd,
                            function() {
                                haveRemoteDescription = true;
                                addRemoteCandidates();
                                Phono.log.debug("remoteDescription happy");
                            },
                            function(e) {
                                Phono.log.error("remoteDescription sad " + JSON.stringify(e));
                            });

                } else {
                    // We are an offer for an inbound call
                    Phono.log.info('Got remote description ' + JSON.stringify(sdp));
                    var sd = JSEPAudio.mkSessionDescription({
                        'sdp': sdp,
                        'type': "offer"
                    });
                    inboundOffer = sd;
                }
                return {
                    codec: codec,
                    input: remoteVideo
                };
            }
        },
        destroyTransport: function() {
            // Destroy any transport state we have created
            if (pc) {
                pc.close();
                if (($(remoteVideo).attr("id")).indexOf("_phono-audio-webrtc") == 0) {
                    remoteVideo.parentNode.removeChild(remoteVideo);
                }
            }

            if (JSEPAudio.localStream) {
                JSEPAudio.localStream.stop();
                JSEPAudio.localStream = null;
            }
        }
    }
};

// Returns an array of codecs supported by this plugin
// Hack until we get capabilities support
JSEPAudio.prototype.codecs = function() {
    return {};
};

JSEPAudio.prototype.audioInDevices = function() {
    var result = new Array();
    return result;
}

// Creates a DIV to hold the video element if not specified by the user
JSEPAudio.prototype.createContainer = function(haveVideo) {
    var w = "1";
    var h = "1";
    if (haveVideo) {
        w = "640";
        h = "480";
    }
    Phono.log.info('Appending a <video> because we were not passed one ' + w + "x" + h);


    var webRTC = $("<video>")
            .attr("id", "_phono-audio-webrtc" + (JSEPAudio.count++))
            .attr("autoplay", "autoplay")
            .attr("width", w)
            .attr("height", h)
            .appendTo("body");

    var containerId = $(webRTC).attr("id");
    return containerId;
};
