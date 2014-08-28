var acnumber = "";
var call;
var phono;


function numbersonly(e) {
    if (!e) {
        e = event;
    }
    var unicode = e.charCode ? e.charCode : e.keyCode;
    //alert('numbersonly, unicode=' + unicode);

    // backspace or delete
    if (unicode == 8 || unicode == 46) {
        redButtonPressed();
    }

    // return
    if (unicode == 13) {
        greenButtonPressed();
    }
    if ((unicode == 42) || (unicode == 35) || (unicode >= 48 && unicode <= 57)) {
        var but = String.fromCharCode(unicode);
        buttonPressed(but);
    }
    // return false;
}

function areyousure(e) {
    var retval;
    if (call) {
        retval = "You are in a call at the moment. If you leave this page, your call will be cut off.";
    }
    return retval;
}

function hangup() {
    acnumber = "";
    document.getElementById('NumberBar').innerHTML = acnumber;
    if (call) {
        try {
            call.hangup();
        } catch (err) {
            // alert('hangup(): ' + err);
        }
    }
}

function dial() {
    if (phono) {
        var no = acnumber;
        phono.messaging.send("asterisk@lookafar.westhawk.co.uk/asterisk", no);
        var mbar = document.getElementById('MessageBar');
        mbar.innerHTML = "Dialling";
    }
    // return false;
}

function buttonPressed(but) {
    if (call) {
        call.sendDTMF(but.charCodeAt(0));
    } else {
        acnumber += but;
        document.getElementById('NumberBar').innerHTML = acnumber;
    }
}




$(document).ready(function() {
    var connectionUrl = "https://lookafar.westhawk.co.uk:7443/http-bind/";
    var miceServers = [
        {url: "stun:71.6.135.115:3478"},
        {url: "turn:71.6.135.115:3478", username: "test", credential: "tester"},
        {url: "turn:71.6.135.115:3479", username: "test", credential: "tester"}
    ];
    var xmlSerializer = {};
    if (navigator.appName.indexOf('Internet Explorer') > 0) {
        xmlSerializer.serializeToString = function(body) {
            return body.xml;
        };
    } else {
        xmlSerializer = new XMLSerializer();
    }
    Strophe.log = function(level, msg) {
        if (level == 4) {
            console.log("STROPHE ERROR! " + level + " : " + msg);
        }
        console.log("STROPHE " + level + " : " + msg);
    };
    connection = new Strophe.Connection(connectionUrl);
    connection.xmlInput = function(body) {
        console.log("[WIRE] (i) " + xmlSerializer.serializeToString(body));
    };
    connection.xmlOutput = function(body) {
        console.log("[WIRE] (o) " + xmlSerializer.serializeToString(body));
    };
    connection.connect("lookafar.westhawk.co.uk", null,
            function(status) {
                if (status == Strophe.Status.CONNECTED) {
                    connection.send($pres().tree());
                    phono = $.phono(
                            {
                                connectionUrl: connectionUrl,
                                gateway: "lookafar.westhawk.co.uk",
                                connection: connection,
                                audio: {media: {audio: true, video: false},
                                    iceServers: miceServers//,
                                            //gatewayType: "xep-0320"
                                },
                                phone: {
                                    ringTone: "ringtones/DialTone.mp3",
                                    ringbackTone: "ringtones/ringback-us.mp3",
                                    onIncomingCall: function(evt) {
                                        call = evt.call;
                                        call.bind({onHangup: function(ev) {
                                                console.log("hungup");
                                                hungUp();
                                            }});
                                        call.bind({onAnswer: function(ev) {
                                                console.log("Connected");
                                            }});
                                        call.answer();

                                    }
                                },
                                onReady: function() {
                                    console.log("jid = " + phono.sessionId);
                                },
                                onUnready: function(event) {
                                    console.log("[" + connectionUrl + "] Phono disconnected");
                                },
                                onError: function(event) {
                                    console.log(event.reason);
                                },
                                messaging: {
                                    onMessage: function(event) {
                                        var message = event.message;
                                        alert("Message from: " + message.from + "\n" + message.body);
                                    }
                                }
                            }
                    );
                    phono.connect();
                }
                ;
            });
});




// =======================================================================
// The rest of the functions (applet unrelated)
// =======================================================================
function greenButtonPressed() {
    if (!call) {
        dial();
    }
}

function redButtonPressed() {
    if (call) {
        hangup();
    } else {
        // remove last digit
        acnumber = acnumber.substring(0, acnumber.length - 1);
        document.getElementById('NumberBar').innerHTML = acnumber;
    }
}

function ringing() {
    var mbar = document.getElementById('MessageBar');
    mbar.innerHTML = "Ringing";
}
function answered() {
    var mbar = document.getElementById('MessageBar');
    mbar.innerHTML = "Answered";
}
function hungUp() {
    var mbar = document.getElementById('MessageBar');
    mbar.innerHTML = "Ready to Dial";
    call = null;
}
