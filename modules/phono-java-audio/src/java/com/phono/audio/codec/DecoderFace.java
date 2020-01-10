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

/**
 * DecoderFace
 *
 * This interface is to be implemented by the decoder instance of each codec.
 *
 * @see CodecFace
 * @see EncoderFace
 *
 */
public interface DecoderFace {

    /**
     * Decodes an (encoded) frame.
     *
     * @param encoded_signal The encoded frame(s)
     * @param fec decoder should attempt to get fec from this frame
     * @return The decoded frame
     * 
     */
    public short[] decode_frame(byte[] ebuff, boolean fec);

}
