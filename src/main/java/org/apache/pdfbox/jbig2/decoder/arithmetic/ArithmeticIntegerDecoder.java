/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pdfbox.jbig2.decoder.arithmetic;

import java.io.IOException;

/**
 * This class represents the arithmetic integer decoder, described in ISO/IEC 14492:2001 (Annex A).
 */
public class ArithmeticIntegerDecoder
{
    /** List of offset, bitsToRead pairs. */
    private final static int[] VALUES = {
        0    << 16 | 2,
        4    << 16 | 4,
        20   << 16 | 6,
        84   << 16 | 8,
        340  << 16 | 12,
        4436 << 16 | 32
    };
    

    private final ArithmeticDecoder decoder;


    public ArithmeticIntegerDecoder(ArithmeticDecoder decoder)
    {
        this.decoder = decoder;
    }

    /**
     * Arithmetic Integer Decoding Procedure, Annex A.2.
     * 
     * @param cxIAx to be decoded value
     * @return Decoded value.
     * @throws IOException if an underlying IO operation fails
     */
    public long decode(CX cxIAx) throws IOException
    {
        if (cxIAx == null)
        {
            cxIAx = new CX(512);
        }

        final int s = decoder.decode(cxIAx, 1);
        int prev = 2 | s;
        
        int offset = 0;
        do
        {
            int d = decoder.decode(cxIAx, prev);
            prev = prev << 1 | d;
            if ( d==0 ) break;
        }
        while ( ++offset < 5 );
        offset = VALUES[offset];
        
        int v = decoder.decode(cxIAx, prev);
        for ( int d = v, count = (short)offset; --count!=0; )
        {
            prev = prev << 1 & 0x1ff | prev & 0x100 | d;
            d = decoder.decode(cxIAx, prev);
            v = (v << 1) | d;
        }
        v += offset >>> 16;
        
        return s == 0 ? v : v > 0 ? -v : Long.MAX_VALUE;
    }

    /**
     * The IAID decoding procedure, Annex A.3.
     * 
     * @param cxIAID - The contexts and statistics for decoding procedure.
     * @param symCodeLen - Symbol code length.
     * 
     * @return The decoded value.
     * 
     * @throws IOException if an underlying IO operation fails
     */
    public int decodeIAID(CX cxIAID, int symCodeLen) throws IOException
    {
        // A.3 1)
        int prev = 1;

        // A.3 2)
        for (int i = 0; i < symCodeLen; i++)
        {
            prev = (prev << 1) | decoder.decode(cxIAID, prev);
        }

        // A.3 3) & 4)
        return (prev - (1 << symCodeLen));
    }
}
