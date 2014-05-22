;(function() {
    //@Include=phono.jsep-audio.js

    Phono.registerPlugin("audio", {
        
        create: function(phono, config, callback) {
            config = Phono.util.extend({
                type: "auto"
            }, config);
            
            // What are we going to create? Look at the config...
            if (config.type === "jsep") {
                return Phono.util.loggify("JSEPAudio", new JSEPAudio(phono, config, callback));

            } else if (config.type === "none") {
                window.setTimeout(callback,10);
                return null;
                
            } else if (config.type === "auto") {
                
                Phono.log.info("Detecting Audio Plugin");

                if (JSEPAudio.exists()) {
                    Phono.log.info("Detected JSEP browser"); 
                    return Phono.util.loggify("JSEPAudio", new JSEPAudio(phono, config, callback));
                } else {
                    return null;
                }
            }
        }
    });
      
})();
