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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Arrays;

/**
 * This class represents a bi-level image that is organized like a bitmap.
 * <p>
 * Each pixel can have the value {@code 0} or {@code 1}. {@code 0} represents white, or background
 * and {@code 1} represents black, or foreground. Pixel outside the bitmap shall have the value {@code 0}.
 * <p>
 * The bitmap is a top-down, left-to-right bitmap, and 8 pixels are stored per byte.
 * Pixels are packed into bytes starting with the most significant bit. The most significant bit
 * is numbered 7, while the least significant bit is numbered 0. Thus a single
 * pixel set to black at {@code (0, 0)} with the following 7 pixels being unset is represented by
 * the byte {@code 0x80}.
 */
public final class Bitmap implements Cloneable
{
    /** The width of the bitmap in pixels. */
    private final int width;

    /** The height of the bitmap in pixels. */
    private final int height;

    /** The amount of bytes used per row. */
    private final int rowStride;

    /** 8 pixels per byte, 0 for white, 1 for black, MSB first. */
    final byte[] bitmap;

    /**
     * Creates an instance of a blank image.<br>
     * The image data is stored in a byte array. Each pixels is stored as one bit, so that each byte contains 8 pixel. A
     * pixel has by default the value {@code 0} for white and {@code 1} for black. <br>
     * Row stride means the amount of bytes per line. It is computed automatically and fills the pad bits with 0.<br>
     * 
     * @param height - The real height of the bitmap in pixels.
     * @param width - The real width of the bitmap in pixels.
     */
    public Bitmap(int width, int height)
    {
        this.width = width;
        this.height = height;
        this.rowStride = (width + 7) >> 3;
        this.bitmap = new byte[rowStride * height];
    }

    
    public Bitmap(int width, int height, int defaultPixelValue)
    {
        this(width, height);
        if ( defaultPixelValue!=0 ) {
            // emulate old behvaiour with padding pixels until side effects are known.
            //Blitter.fill(bitmap, width, height, rowStride, defaultPixelValue);
            Blitter.fill(bitmap, rowStride * 8, height, rowStride, defaultPixelValue);
        }
    }
    
    
    private Bitmap(Bitmap other)
    {
        this.width = other.width;
        this.height = other.height;
        this.rowStride = other.rowStride;
        this.bitmap = other.bitmap.clone();
    }
    

    /**
     * Simply returns the width of this bitmap.
     * 
     * @return The width of this bitmap.
     */
    public int getWidth()
    {
        return width;
    }


    /**
     * Simply returns the height of this bitmap.
     * 
     * @return The height of this bitmap.
     */
    public int getHeight()
    {
        return height;
    }

    
    public Dimension getDimensions()
    {
        return new Dimension(width, height);
    }

    
    public Rectangle getBounds()
    {
        return new Rectangle(0, 0, width, height);
    }


    /**
     * Simply returns the row stride of this bitmap. <br>
     * (Row stride means the amount of bytes per line.)
     * 
     * @return The row stride of this bitmap.
     */
    public int getRowStride()
    {
        return rowStride;
    }

    
    /**
     * Simply returns the byte array of this bitmap.
     * 
     * @return The byte array of this bitmap.
     * 
     * @deprecated don't expose the underlying byte array, will be removed in a future release.
     */
    @Deprecated
    public byte[] getByteArray()
    {
        return bitmap;
    }

    
    /**
     * Returns the length of the underlying byte array.
     * 
     * @return byte array length
     */
    public int getLength()
    {
        return bitmap.length;
    }

    
    /**
     * Returns the value of a pixel specified by the given coordinates.
     * <p>
     * By default, the value is {@code 0} for a white pixel and {@code 1} for a black pixel. The value is placed in the
     * rightmost bit in the byte.
     * 
     * @param x - The x coordinate of the pixel.
     * @param y - The y coordinate of the pixel.
     * @return The value of a pixel.
     */
    public byte getPixel(int x, int y)
    {
        return (byte) ((bitmap[getByteIndex(x, y)] >> getBitOffset(x)) & 1);
    }

    
    /**
     * Returns the value of a pixel specified by the given coordinates
     * or 0 if the pixel is outside the bitmap.
     * <p>
     * By default, the value is {@code 0} for a white pixel and {@code 1} for a black pixel. The value is placed in the
     * rightmost bit in the byte.
     * 
     * @param x - The x coordinate of the pixel.
     * @param y - The y coordinate of the pixel.
     * @return The value of a pixel.
     */
    public byte getSafePixel(int x, int y)
    {
        if ( x<0 || y<0 || x>=width || y>=height ) return 0;
        return (byte) ((bitmap[getByteIndex(x, y)] >> getBitOffset(x)) & 1);
    }
    
    
    /**
     * Sets a pixel.
     */
    public void setPixel(int x, int y)
    {
        bitmap[getByteIndex(x, y)] |= (1 << getBitOffset(x));
    }

    
    /**
     * 
     * <p>
     * Returns the index of the byte that contains the pixel, specified by the pixel's x and y coordinates.
     * 
     * @param x - The pixel's x coordinate.
     * @param y - The pixel's y coordinate.
     * @return The index of the byte that contains the specified pixel.
     */
    public int getByteIndex(int x, int y)
    {
        return y * this.rowStride + (x >> 3);
    }

    
    /**
     * Simply returns a byte from the bitmap byte array. Throws an {@link IndexOutOfBoundsException} if the given index
     * is out of bound.<br>
     * Any pixels outside the bitmap due to the width not being a multiple of 8 will be returned, too.
     * 
     * @param index - The array index that specifies the position of the wanted byte.
     * @return The byte at the {@code index}-position.
     * 
     * @throws IndexOutOfBoundsException if the index is out of bound.
     */
    public byte getByte(int index)
    {
        return this.bitmap[index];
    }

    
    /**
     * Simply sets the given value at the given array index position. Throws an {@link IndexOutOfBoundsException} if the
     * given index is out of bound.<br>
     * Any pixels outside the bitmap due to the width not being a multiple of 8 will be overwritten, too.
     * 
     * @param index - The array index that specifies the position of a byte.
     * @param value - The byte that should be set.
     * 
     * @throws IndexOutOfBoundsException if the index is out of bound.
     */
    public void setByte(int index, byte value)
    {
        this.bitmap[index] = value;
    }

    
    /**
     * Converts the byte at specified index into an integer and returns the value. Throws an
     * {@link IndexOutOfBoundsException} if the given index is out of bound.<br>
     * Any pixels outside the bitmap due to the width not being a multiple of 8 will be returned, too.
     * 
     * @param index - The array index that specifies the position of the wanted byte.
     * @return The converted byte at the {@code index}-position as an integer.
     * 
     * @throws IndexOutOfBoundsException if the index is out of bound.
     */
    public int getByteAsInteger(int index)
    {
        return (this.bitmap[index] & 0xff);
    }
    
    
    @Override
    public boolean equals(Object obj)
    {
        // most likely used for tests
        if (!(obj instanceof Bitmap))
        {
            return false;
        }
        Bitmap other = (Bitmap)obj;
        return Arrays.equals(bitmap, other.bitmap);
    }

    
    @Override
    public Bitmap clone()
    {
        return new Bitmap(this);
    }
    
    
    /**
     * Computes the bit offset of the given x coordinate in its byte.
     * Returns {@code 7 - (x % 8)} since pixels start with the MSB first.
     */
    private static int getBitOffset(int x)
    {
        return (x ^ 7) & 7;
    }
    
}
