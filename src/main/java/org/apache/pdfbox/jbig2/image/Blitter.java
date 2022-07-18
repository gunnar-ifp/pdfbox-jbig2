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

package org.apache.pdfbox.jbig2.image;

import java.awt.Rectangle;
import java.util.Arrays;

import org.apache.pdfbox.jbig2.util.CombinationOperator;

/**
 * Binary image blitter with full support for partially overlapping source
 * and destination bitmaps.
 * Also supports optional source and target regions for virtual
 * sub-bitmaps (can be used for clipping or extracting parts of a bitmap).
 * 
 * @author <a href="mailto:gunnar.brand@interface-projects.de">Gunnar Brand</a>
 * @since 14.07.2022
 */
final class Blitter
{
    private Blitter()
    {
    }
    
    
    public static void fill(byte[] src, int width, int height, int rowStride, int pixel)
    {
        final byte full = (byte)(pixel==0 ? 0 : -1);
        if ( width==rowStride * 8) {
            Arrays.fill(src, full);
        } else {
            final byte bits = (byte)trimByte(full, 0, 8 - width % 8);
            final int bytes = width / 8;
            for ( int idx = 0; height>0; height-- ) {
                for ( int cols = bytes; cols>0; cols-- ) src[idx++] = full;
                src[idx++] = bits;
            }
        }
    }
    
    
    public static void blit(
        byte[] src, int srcWidth, int srcHeight, int srcRowStride, Rectangle srcRegion,
        byte[] dst, int dstWidth, int dstHeight, int dstRowStride, Rectangle dstRegion,
        int dstX, int dstY, CombinationOperator operator)
    {
        // coordinate normalization:
        // 1) adjust source rectangle with source region (optional)
        // 2) adjust source rectangle with target coordinates if negative
        // 3) adjust target rectangle with target region (optional)
        
        int srcX = 0, srcY = 0;
        if ( srcRegion!=null ) {
            srcX      = srcRegion.x;
            srcY      = srcRegion.y;
            srcWidth  = Math.min(Math.min(srcWidth,  srcWidth  - srcX), srcRegion.width);
            srcHeight = Math.min(Math.min(srcHeight, srcHeight - srcY), srcRegion.height);
        }

        if ( dstX<0 ) {
            srcWidth += dstX;
            srcX -= dstX;
            dstX = 0;
        }

        if ( dstY<0 ) {
            srcHeight += dstY;
            srcY -= dstY;
            dstY = 0;
        }
        
        if ( dstRegion!=null ) {
            dstX     += dstRegion.x;
            srcY     += dstRegion.y;
            dstWidth  = Math.min(Math.min(dstWidth,  dstWidth  - dstX), dstRegion.width);
            dstHeight = Math.min(Math.min(dstHeight, dstHeight - dstY), dstRegion.height);
        }
        
        srcWidth = Math.min(srcWidth, dstWidth - dstX);
        if ( srcWidth<=0 ) return;

        srcHeight = Math.min(srcHeight, dstHeight - dstY);
        if ( srcHeight<=0 ) return;

        // shift left / right calculations,
        // with pre-shift for first byte and tail bits for last byte
        int shiftLeft  = srcX % 8;
        int shiftRight = dstX % 8;
        int srcOffset  = srcX / 8 + srcY * srcRowStride;
        int dstOffset  = dstX / 8 + dstY * dstRowStride;
        
        // For X there are 8 pixels per byte, with first pixel in MSB.
        // We need two src bytes to fill a shift register so we always have
        // 8 bits to combine with the destination byte.
        // Special care must be taken for the first and the last bytes, which must be 
        // filled with virtual blank pixels.
        // Also source and dest width are not always a multiple of 8 and can have padding at the end
        int headBits   = Math.min(8 - shiftRight, srcWidth);
        int fullBytes  = (srcWidth - headBits) / 8;
        int tailBits   = (srcWidth - headBits) % 8;

        // First byte might be the only byte and takes special care of being limited left and right.
        // Last byte might be cut on the right side only.
        int headMask   = trimByte(0xff, shiftRight, 8 - headBits - shiftRight);
        int tailMask   = trimByte(0xff, 0,          8 - tailBits);
        
        int shiftDelta = shiftRight - shiftLeft;
        int preShift   = 0;
        int firstBits  = 8 - shiftLeft;
        if ( shiftLeft>shiftRight ) {
            shiftDelta += 8;
            preShift = firstBits<headBits ? -1 : 1; // firstBits - headBits; 
        }

        // note: be aware that out is incremented according to java expression left-to-right syntax
        // to make combination operations easy to write.   
        int in = srcOffset, out = dstOffset;
        while ( --srcHeight>=0 ) {
            // Unshifted: first and last byte might be partial but in between it is byte to byte.
            // Shifted to the right: first src byte split over up to two dst bytes.
            // Shifted to the left:  first or first two src bytes make up a dst byte.
            int reg = src[in++] & 0xff;
            if ( preShift!=0 ) {
                reg <<= 8;
                if ( preShift<0 ) reg |= src[in++] & 0xff;
            }
    
            dst[out] = (byte)
                (~headMask & dst[out] 
                | headMask & combineByte(reg >> shiftDelta, dst[out], operator));

            
            if ( fullBytes==0 ) {
                // do nothing
            }
            else if ( shiftDelta==0 ) {
                switch (operator) {
                    case OR:
                        for ( int c = fullBytes; --c>=0; ) dst[++out] |= src[in++];
                        break;
                        
                    case AND:
                        for ( int c = fullBytes; --c>=0; ) dst[++out] &= src[in++];
                        break;
                        
                    case XOR:
                        for ( int c = fullBytes; --c>=0; ) dst[++out] ^= src[in++];
                        break;
                        
                    case XNOR:
                        for ( int c = fullBytes; --c>=0; ) dst[++out] = (byte)~(dst[out] ^ src[in++]);
                        break;
                        
                    case REPLACE:
                        System.arraycopy(src, in, dst, out + 1, fullBytes);
                        in  += fullBytes;
                        out += fullBytes;
                        break;
                        
                    case NOT:
                        for ( int c = fullBytes; --c>=0; ) dst[++out] = (byte)~(src[in++]);
                        break;
                }
            }
            else {
                switch (operator) {
                    case OR:
                        for ( int c = fullBytes; --c>=0; ) dst[++out] |= (reg = reg << 8 | src[in++] & 0xff) >> shiftDelta;
                        break;
                        
                    case AND:
                        for ( int c = fullBytes; --c>=0; ) dst[++out] &= (reg = reg << 8 | src[in++] & 0xff) >> shiftDelta;
                        break;
                        
                    case XOR:
                        for ( int c = fullBytes; --c>=0; ) dst[++out] ^= (reg = reg << 8 | src[in++] & 0xff) >> shiftDelta;
                        break;
                        
                    case XNOR:
                        for ( int c = fullBytes; --c>=0; ) dst[++out] = (byte)~(dst[out] ^ (reg = reg << 8 | src[in++] & 0xff) >> shiftDelta);
                        break;
                    
                    case REPLACE:
                        for ( int c = fullBytes; --c>=0; ) dst[++out] = (byte)((reg = reg << 8 | src[in++] & 0xff) >> shiftDelta);
                        break;
                        
                    case NOT:
                        for ( int c = fullBytes; --c>=0; ) dst[++out] = (byte)~((reg = reg << 8 | src[in++] & 0xff) >> shiftDelta);
                        break;
                }
            }
    
            if ( tailBits!=0 ) {
                reg = reg << 8 | (shiftDelta>=tailBits ? 0 : src[in++] & 0xff);
                dst[++out] = (byte)
                    (~tailMask & dst[out]
                    | tailMask & combineByte(reg >> shiftDelta, dst[out], operator));
            }
            
            in  = srcOffset += srcRowStride;
            out = dstOffset += dstRowStride;
        }
    }

    
    /**
     * Combines a source byte with the destination byte according
     * to JBIG2 combination rules and returns the new destination byte.
     */
    private static int combineByte(int src, int dst, CombinationOperator operator)
    {
        switch (operator)
        {
            case OR:      return   dst | src;
            case AND:     return   dst & src;
            case XOR:     return   dst ^ src;
            case XNOR:    return ~(dst ^ src);
            case REPLACE: return         src;
            case NOT:     return        ~src;
        }
        return src;
    }
    
    
    static int trimByte(int value, int left, int right)
    {
        return (0xff >> left) & (0xff << right) & value;
    }
    
}
