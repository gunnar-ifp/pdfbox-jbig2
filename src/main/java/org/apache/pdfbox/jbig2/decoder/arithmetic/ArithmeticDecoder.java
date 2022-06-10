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

import java.io.EOFException;
import java.io.IOException;

import javax.imageio.stream.ImageInputStream;

/**
 * This class represents the arithmetic decoder, described in ISO/IEC 14492:2001 in E.3
 */
public class ArithmeticDecoder
{
    /**
     * Table E.1, optimized for bit operations (shifting and xor).
     *  
     * <pre>
     * bit      31: zero padding
     * bit 30 - 16: QE (15 bit) 
     * 
     * bit      15: zero padding
     * bit 14 -  9: NLPS (6 bit)
     * bit       8: MPS switch flag (XOR with current MPS)
     * 
     * bit       7: zero padding
     * bit  6 -  1: NMPS (6 bit)
     * bit       0: MPS switch flag = 0 (XOR with current MPS)
     * </pre>
     */
    private static final int[] QE = {
        //     QE    |   NLPS  + SWITCH |   NMPS  
        0x5601 << 16 |  1 << 9 | 1 << 8 |  1 << 1,
        0x3401 << 16 |  6 << 9 | 0 << 8 |  2 << 1,
        0x1801 << 16 |  9 << 9 | 0 << 8 |  3 << 1,
        0x0AC1 << 16 | 12 << 9 | 0 << 8 |  4 << 1,
        0x0521 << 16 | 29 << 9 | 0 << 8 |  5 << 1,
        0x0221 << 16 | 33 << 9 | 0 << 8 | 38 << 1,
        0x5601 << 16 |  6 << 9 | 1 << 8 |  7 << 1,
        0x5401 << 16 | 14 << 9 | 0 << 8 |  8 << 1,
        0x4801 << 16 | 14 << 9 | 0 << 8 |  9 << 1,
        0x3801 << 16 | 14 << 9 | 0 << 8 | 10 << 1,
        0x3001 << 16 | 17 << 9 | 0 << 8 | 11 << 1,
        0x2401 << 16 | 18 << 9 | 0 << 8 | 12 << 1,
        0x1C01 << 16 | 20 << 9 | 0 << 8 | 13 << 1,
        0x1601 << 16 | 21 << 9 | 0 << 8 | 29 << 1,
        0x5601 << 16 | 14 << 9 | 1 << 8 | 15 << 1,
        0x5401 << 16 | 14 << 9 | 0 << 8 | 16 << 1,
        0x5101 << 16 | 15 << 9 | 0 << 8 | 17 << 1,
        0x4801 << 16 | 16 << 9 | 0 << 8 | 18 << 1,
        0x3801 << 16 | 17 << 9 | 0 << 8 | 19 << 1,
        0x3401 << 16 | 18 << 9 | 0 << 8 | 20 << 1,
        0x3001 << 16 | 19 << 9 | 0 << 8 | 21 << 1,
        0x2801 << 16 | 19 << 9 | 0 << 8 | 22 << 1,
        0x2401 << 16 | 20 << 9 | 0 << 8 | 23 << 1,
        0x2201 << 16 | 21 << 9 | 0 << 8 | 24 << 1,
        0x1C01 << 16 | 22 << 9 | 0 << 8 | 25 << 1,
        0x1801 << 16 | 23 << 9 | 0 << 8 | 26 << 1,
        0x1601 << 16 | 24 << 9 | 0 << 8 | 27 << 1,
        0x1401 << 16 | 25 << 9 | 0 << 8 | 28 << 1,
        0x1201 << 16 | 26 << 9 | 0 << 8 | 29 << 1,
        0x1101 << 16 | 27 << 9 | 0 << 8 | 30 << 1,
        0x0AC1 << 16 | 28 << 9 | 0 << 8 | 31 << 1,
        0x09C1 << 16 | 29 << 9 | 0 << 8 | 32 << 1,
        0x08A1 << 16 | 30 << 9 | 0 << 8 | 33 << 1,
        0x0521 << 16 | 31 << 9 | 0 << 8 | 34 << 1,
        0x0441 << 16 | 32 << 9 | 0 << 8 | 35 << 1,
        0x02A1 << 16 | 33 << 9 | 0 << 8 | 36 << 1,
        0x0221 << 16 | 34 << 9 | 0 << 8 | 37 << 1,
        0x0141 << 16 | 35 << 9 | 0 << 8 | 38 << 1,
        0x0111 << 16 | 36 << 9 | 0 << 8 | 39 << 1,
        0x0085 << 16 | 37 << 9 | 0 << 8 | 40 << 1,
        0x0049 << 16 | 38 << 9 | 0 << 8 | 41 << 1,
        0x0025 << 16 | 39 << 9 | 0 << 8 | 42 << 1,
        0x0015 << 16 | 40 << 9 | 0 << 8 | 43 << 1,
        0x0009 << 16 | 41 << 9 | 0 << 8 | 44 << 1,
        0x0005 << 16 | 42 << 9 | 0 << 8 | 45 << 1,
        0x0001 << 16 | 43 << 9 | 0 << 8 | 45 << 1,
        0x5601 << 16 | 46 << 9 | 0 << 8 | 46 << 1
    };

    /** Bit 15 - 0: fractional bits. */
    private int a;
    /** Currently read byte */
    private int b;
    /** Bit 31 - 16 = Chigh = x = fractional bits, bit 15 - 8 = b to be shifted up. */ 
    private int c;
    /** Counter of bits available in Clow. */
    private int ct;

    private final ImageInputStream iis;

    public ArithmeticDecoder(ImageInputStream iis) throws IOException
    {
        this.iis = iis;
        
        b = iis.read();
        if ( b==-1 ) throw new EOFException();

        c = b << 16;
        ct = byteIn() - 7;
        c <<= 7;
        a = 0x8000;
    }

    
    public int decode(final CX cx, final int index) throws IOException
    {
        //int mps = cx.cxmps[index];
        int mps = cx.get(index);
        final int qe = QE[mps >> 1];
        final int qeValue = qe >>> 16;

        // d
        mps &= 1;

        // The following code is described in Figures E.15 - E.17
        // but the "a" < "qeValue" comparision in E.16 and E.17
        // has been turned into non-branching code.
        // A subtraction is used to create a 1/0 boolean flag (x) which is
        // then used to select the NMPS or NLPS column and invert "d" (mps).
        // The SWITCH column has been combined with the NMPS and NLPS columns
        // and is applied as XOR to flip the MPS flag in CX while setting
        // the new qe value in one go.
        // Also "a" is only modified once per block.
        if ((c >>> 16) < qeValue)
        {
            // LPS exchange as described in E.17
//          int x = ~(a - (qeValue << 1)) >>> 31;
            int x = ((a - (qeValue << 1))  >> 31) + 1;
            //cx.cxmps[index] = (byte)(mps ^ qe >> (x << 3));
            cx.set(index, mps ^ qe >> (x << 3));
            a = qeValue;
            mps ^= x;
        } else {
            // MPS exchange as described in E.16
            c -= qeValue << 16;
            a -= qeValue;
            if ( a >= 0x8000 ) return mps;
            int x = (a - qeValue) >>> 31;
            //cx.cxmps[index] = (byte)(mps ^ qe >> (x << 3));
            cx.set(index, mps ^ qe >> (x << 3));
            mps ^= x;
        }

        // renormalize
        do {
            if (--ct < 0) ct = byteIn() - 1;
            c <<= 1;
            a <<= 1;
        } while (a < 0x8000);
        
        return mps;
    }

    
    /**
     * Returns new ct value. Alters c and b.
     */
    private int byteIn() throws IOException
    {
        final int b1 = iis.read();
        
        if ( b1<0 ) throw new EOFException();
        
        if (b != 0xFF) {
            b  = b1;
            c += b1 << 8;
            return 8;
        }
        
        if (b1 < 0x90) {
            b  = b1;
            c += b1 << 9;
            return 7;
        }
        
        iis.seek(iis.getStreamPosition() - 1);
        c += 0xff00;
        return 8;
    }

    /*
    private void renormalize() throws IOException
    {
        int lz = Integer.numberOfLeadingZeros(a) - 16, lct = ct;
        if ( lct==0 ) lct = byteIn();
        while ( lz>lct ) {
            c <<= lct;
            a <<= lct;
            lz -= lct;
            lct = byteIn();
        }
        c <<= lz;
        a <<= lz;
        ct = lct - lz;
    }
    */

    int getA()
    {
        return a;
    }

    int getC()
    {
        return c;
    }
}
