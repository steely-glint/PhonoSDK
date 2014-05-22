/*!
 * Copyright 2013, 2014 Tropo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * Includes third party software from various sources. Portions of this
 * software are copyright their respective owners. See
 * http://phono.com/license for copyright statements from Adobe Systems
 * Incorporated, Kyle Simpson, Getify Solutions, Inc., Paul Johnston, and
 * Flowplayer.
 *
 */


var flensed;

(function($) {

    //@Include=../../../../../build/phono.config.js
    //@Include=$phono-core
   
   $.phono = function(config) {
      return new Phono(config);
   }
   
})(jQuery);
