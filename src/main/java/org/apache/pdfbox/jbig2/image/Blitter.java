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


/**
 * Binary image blitter with full support for partially overlapping source
 * and destination bitmaps.
 * Also supports optional source and target regions for virtual
 * sub-bitmaps (can be used for clipping or extracting parts of a bitmap).
 * 
 * @author <a href="mailto:gunnar.brand@interface-projects.de">Gunnar Brand</a>
 * @since 14.07.2022
 */
public final class Blitter
{
    /**
     * Bit combination operator.
     * <p>
     * This class is a stand-alone class, so it has it's own
     * operator enum, which is intentionally kept compatible
     * to the JBIG 2 codes and CombinationOperator.
     */
    public enum BitCombinationOperator {
        OR, AND, XOR, XNOR, REPLACE, NOT;
    }
    
    
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
        int dstX, int dstY, BitCombinationOperator operator)
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
        
        // the inner loop only ever shifts right, so if we need to shift left,
        // we offset it by 8 (loading an additional source byte if necessary). 
        int shiftDelta = shiftRight - shiftLeft;
        int preShift   = 0;
        if ( shiftLeft>shiftRight ) {
            shiftDelta += 8;
            preShift = 8 - shiftLeft<headBits ? -1 : 1; 
        }

        switch (operator) {
            case OR:
                blitOr(src, srcOffset, srcRowStride, dst, dstOffset, dstRowStride, srcHeight,
                    shiftDelta, preShift, headBits, fullBytes, tailBits, headMask, tailMask);
                break;
                
            case AND:
                blitAnd(src, srcOffset, srcRowStride, dst, dstOffset, dstRowStride, srcHeight,
                    shiftDelta, preShift, headBits, fullBytes, tailBits, headMask, tailMask);
                break;
                
            case XOR:
                blitXor(src, srcOffset, srcRowStride, dst, dstOffset, dstRowStride, srcHeight,
                    shiftDelta, preShift, headBits, fullBytes, tailBits, headMask, tailMask);
                break;
                
            case XNOR:
                blitXnor(src, srcOffset, srcRowStride, dst, dstOffset, dstRowStride, srcHeight,
                    shiftDelta, preShift, headBits, fullBytes, tailBits, headMask, tailMask);
                break;
                
            case REPLACE:
                blitReplace(src, srcOffset, srcRowStride, dst, dstOffset, dstRowStride, srcHeight,
                    shiftDelta, preShift, headBits, fullBytes, tailBits, headMask, tailMask);
                break;
                
            case NOT:
                blitNot(src, srcOffset, srcRowStride, dst, dstOffset, dstRowStride, srcHeight,
                    shiftDelta, preShift, headBits, fullBytes, tailBits, headMask, tailMask);
                break;
        }
    }
    
    
    private static void blitOr(
        byte[] src, int srcOffset, int srcRowStride, byte[] dst, int dstOffset, int dstRowStride, int height,
        int shiftDelta, int preShift, int headBits, int fullBytes, int tailBits, int headMask, int tailMask)
    {
        int in = srcOffset, out = dstOffset;
        // speeds up halftone rendering quite a bit but otherwise slows things down for C2
        /*
        if ( preShift==0 && fullBytes==0 && shiftDelta>=tailBits ) {
            if ( tailMask==0 ) {
                while ( --height>=0 ) {
                    dst[out] |= headMask & src[in] >> shiftDelta;
                    in  = srcOffset += srcRowStride;
                    out = dstOffset += dstRowStride;
                }
            } else {
                final int tailShift = 8 - shiftDelta;
                while ( --height>=0 ) {
                    int reg = src[in];
                    dst[out    ] |= headMask & reg >> shiftDelta;
                    dst[out + 1] |= tailMask & reg << tailShift;
                    in  = srcOffset += srcRowStride;
                    out = dstOffset += dstRowStride;
                }
            }
        }
        else
        //*/
        while ( --height>=0 ) {
            int reg = src[in++] & 0xff;
            if ( preShift!=0 ) {
                reg <<= 8;
                if ( preShift<0 ) reg |= src[in++] & 0xff;
            }
    
            dst[out] |= headMask & reg >> shiftDelta;
    
            if ( fullBytes==0 ) {
                // do nothing
            }
            else if ( shiftDelta==0 ) {
                for ( int c = fullBytes; --c>=0; ) dst[++out] |= src[in++];
            }
            else {
                for ( int c = fullBytes; --c>=0; ) dst[++out] |= (reg = reg << 8 | src[in++] & 0xff) >> shiftDelta;
            }
    
            if ( tailBits!=0 ) {
                reg = reg << 8 | (shiftDelta>=tailBits ? 0 : src[in++] & 0xff);
                dst[++out] |= tailMask & reg >> shiftDelta;
            }
            
            in  = srcOffset += srcRowStride;
            out = dstOffset += dstRowStride;
        }
    }

    
    private static void blitAnd(
        byte[] src, int srcOffset, int srcRowStride, byte[] dst, int dstOffset, int dstRowStride, int height,
        int shiftDelta, int preShift, int headBits, int fullBytes, int tailBits, int headMask, int tailMask)
    {
        headMask = ~headMask;
        tailMask = ~tailMask;
            
        int in = srcOffset, out = dstOffset;
        while ( --height>=0 ) {
            int reg = src[in++] & 0xff;
            if ( preShift!=0 ) {
                reg <<= 8;
                if ( preShift<0 ) reg |= src[in++] & 0xff;
            }
    
            dst[out] &= headMask | reg >> shiftDelta;
    
            if ( fullBytes==0 ) {
                // do nothing
            }
            else if ( shiftDelta==0 ) {
                for ( int c = fullBytes; --c>=0; ) dst[++out] &= src[in++];
            }
            else {
                for ( int c = fullBytes; --c>=0; ) dst[++out] &= (reg = reg << 8 | src[in++] & 0xff) >> shiftDelta;
            }
    
            if ( tailBits!=0 ) {
                reg = reg << 8 | (shiftDelta>=tailBits ? 0 : src[in++] & 0xff);
                dst[++out] &= tailMask | reg >> shiftDelta;
            }
            
            in  = srcOffset += srcRowStride;
            out = dstOffset += dstRowStride;
        }
    }
    
    
    private static void blitXor(
        byte[] src, int srcOffset, int srcRowStride, byte[] dst, int dstOffset, int dstRowStride, int height,
        int shiftDelta, int preShift, int headBits, int fullBytes, int tailBits, int headMask, int tailMask)
    {
        int in = srcOffset, out = dstOffset;
        while ( --height>=0 ) {
            int reg = src[in++] & 0xff;
            if ( preShift!=0 ) {
                reg <<= 8;
                if ( preShift<0 ) reg |= src[in++] & 0xff;
            }
    
            dst[out] ^= headMask & reg >> shiftDelta;

            if ( fullBytes==0 ) {
                // do nothing
            }
            else if ( shiftDelta==0 ) {
                for ( int c = fullBytes; --c>=0; ) dst[++out] ^= src[in++];
            }
            else {
                for ( int c = fullBytes; --c>=0; ) dst[++out] ^= (reg = reg << 8 | src[in++] & 0xff) >> shiftDelta;
            }
    
            if ( tailBits!=0 ) {
                reg = reg << 8 | (shiftDelta>=tailBits ? 0 : src[in++] & 0xff);
                dst[++out] ^= tailMask & reg >> shiftDelta;
            }
            
            in  = srcOffset += srcRowStride;
            out = dstOffset += dstRowStride;
        }
    }
    
    
    private static void blitXnor(
        byte[] src, int srcOffset, int srcRowStride, byte[] dst, int dstOffset, int dstRowStride, int height,
        int shiftDelta, int preShift, int headBits, int fullBytes, int tailBits, int headMask, int tailMask)
    {
        // note: be aware that out is incremented according to java expression left-to-right syntax
        // to make combination operations easy to write.   
        int in = srcOffset, out = dstOffset;
        while ( --height>=0 ) {
            int reg = src[in++] & 0xff;
            if ( preShift!=0 ) {
                reg <<= 8;
                if ( preShift<0 ) reg |= src[in++] & 0xff;
            }
    
            dst[out] = (byte)(~headMask & dst[out] | headMask & ~(dst[out] ^ reg >> shiftDelta));
    
            if ( fullBytes==0 ) {
                // do nothing
            }
            else if ( shiftDelta==0 ) {
                for ( int c = fullBytes; --c>=0; ) dst[++out] = (byte)~(dst[out] ^ src[in++]);
            }
            else {
                for ( int c = fullBytes; --c>=0; ) dst[++out] = (byte)~(dst[out] ^ (reg = reg << 8 | src[in++] & 0xff) >> shiftDelta);
            }
    
            if ( tailBits!=0 ) {
                reg = reg << 8 | (shiftDelta>=tailBits ? 0 : src[in++] & 0xff);
                dst[++out] = (byte)(~tailMask & dst[out] | tailMask & ~(dst[out] ^ reg >> shiftDelta));
            }
            
            in  = srcOffset += srcRowStride;
            out = dstOffset += dstRowStride;
        }
    }
    
    
    private static void blitReplace(
        byte[] src, int srcOffset, int srcRowStride, byte[] dst, int dstOffset, int dstRowStride, int height,
        int shiftDelta, int preShift, int headBits, int fullBytes, int tailBits, int headMask, int tailMask)
    {
        int in = srcOffset, out = dstOffset;
        while ( --height>=0 ) {
            int reg = src[in++] & 0xff;
            if ( preShift!=0 ) {
                reg <<= 8;
                if ( preShift<0 ) reg |= src[in++] & 0xff;
            }
    
            dst[out] = (byte)(~headMask & dst[out] | headMask & reg >> shiftDelta);
    
            if ( fullBytes==0 ) {
                // do nothing
            }
            else if ( shiftDelta==0 ) {
                System.arraycopy(src, in, dst, out + 1, fullBytes);
                in  += fullBytes;
                out += fullBytes;
            }
            else {
                for ( int c = fullBytes; --c>=0; ) dst[++out] = (byte)((reg = reg << 8 | src[in++] & 0xff) >> shiftDelta);
            }
    
            if ( tailBits!=0 ) {
                reg = reg << 8 | (shiftDelta>=tailBits ? 0 : src[in++] & 0xff);
                dst[++out] = (byte)(~tailMask & dst[out] | tailMask & reg >> shiftDelta);
            }
            
            in  = srcOffset += srcRowStride;
            out = dstOffset += dstRowStride;
        }
    }
    

    /**
     * Unofficial extension
     */
    private static void blitNot(
        byte[] src, int srcOffset, int srcRowStride, byte[] dst, int dstOffset, int dstRowStride, int height,
        int shiftDelta, int preShift, int headBits, int fullBytes, int tailBits, int headMask, int tailMask)
    {
        int in = srcOffset, out = dstOffset;
        while ( --height>=0 ) {
            int reg = src[in++] & 0xff;
            if ( preShift!=0 ) {
                reg <<= 8;
                if ( preShift<0 ) reg |= src[in++] & 0xff;
            }
    
            dst[out] = (byte)(~headMask & dst[out] | headMask & ~(reg >> shiftDelta));
    
            if ( fullBytes==0 ) {
                // do nothing
            }
            else if ( shiftDelta==0 ) {
                for ( int c = fullBytes; --c>=0; ) dst[++out] = (byte)~(src[in++]);
            }
            else {
                for ( int c = fullBytes; --c>=0; ) dst[++out] = (byte)~((reg = reg << 8 | src[in++] & 0xff) >> shiftDelta);
            }
    
            if ( tailBits!=0 ) {
                reg = reg << 8 | (shiftDelta>=tailBits ? 0 : src[in++] & 0xff);
                dst[++out] = (byte)(~tailMask & dst[out] | tailMask & ~(reg >> shiftDelta));
            }
            
            in  = srcOffset += srcRowStride;
            out = dstOffset += dstRowStride;
        }
    }


    static int trimByte(int value, int left, int right)
    {
        return (0xff >> left) & (0xff << right) & value;
    }
    
}
