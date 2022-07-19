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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import javax.imageio.ImageReadParam;

import org.apache.pdfbox.jbig2.JBIG2ReadParam;
import org.apache.pdfbox.jbig2.image.Blitter.BitCombinationOperator;
import org.apache.pdfbox.jbig2.util.CombinationOperator;

public class Bitmaps
{

    /**
     * Returns the given bitmap as writable raster.
     * 
     * @param bitmap the given bitmap
     * @return the raster representation of the bitmap
     */
    public static WritableRaster asRaster(final Bitmap bitmap)
    {
        return asRaster(bitmap, FilterType.Gaussian);
    }

    /**
     * Returns the given bitmap as writable raster.
     * 
     * @param bitmap the given bitmap
     * @param filterType type of filter which is used when creating the writable raster
     * @return the raster representation of the bitmap
     */
    public static WritableRaster asRaster(final Bitmap bitmap, final FilterType filterType)
    {
        if (bitmap == null) throw new IllegalArgumentException("bitmap must not be null");
        final JBIG2ReadParam param = new JBIG2ReadParam(1, 1, 0, 0, bitmap.getBounds(), bitmap.getDimensions());
        return asRaster(bitmap, param, filterType);
    }

    /**
     * Returns the given bitmap as writable raster.
     * 
     * @param bitmap the given bitmap
     * @param param ImageReadParam to be used when creating the writable raster
     * @param filterType type of filter which is used when creating the writable raster
     * @return the raster representation of the bitmap
     */
    public static WritableRaster asRaster(Bitmap bitmap, final ImageReadParam param,
            final FilterType filterType)
    {
        if (bitmap == null) throw new IllegalArgumentException("bitmap must not be null");
        if (param == null) throw new IllegalArgumentException("param must not be null");

        final Dimension sourceRenderSize = param.getSourceRenderSize();

        double scaleX;
        double scaleY;
        if (sourceRenderSize != null)
        {
            scaleX = sourceRenderSize.getWidth() / bitmap.getWidth();
            scaleY = sourceRenderSize.getHeight() / bitmap.getHeight();
        }
        else
        {
            scaleX = scaleY = 1;
        }

        Rectangle sourceRegion = param.getSourceRegion();
        if (sourceRegion != null && !bitmap.getBounds().equals(sourceRegion))
        {
            // get region of interest
            bitmap = Bitmaps.extract(sourceRegion, bitmap);
        }

        /*
         * Subsampling is the advance of columns/rows for each pixel in the according direction. The resulting image's
         * quality will be bad because we loose information if we step over columns/rows. For example, a thin line (1
         * pixel high) may disappear completely. To avoid this we use resize filters if scaling will be performed
         * anyway. The resize filters use scale factors, one for horizontal and vertical direction. We care about the
         * given subsampling steps by adjusting the scale factors. If scaling is not performed, subsampling is performed
         * in its original manner.
         */

        final boolean requiresScaling = scaleX != 1 || scaleY != 1;

        final boolean requiresXSubsampling = param.getSourceXSubsampling() != 1;
        final boolean requiresYSubsampling = param.getSourceYSubsampling() != 1;

        if (requiresXSubsampling && requiresYSubsampling)
        {
            // Apply vertical and horizontal subsampling
            if (requiresScaling)
            {
                scaleX /= param.getSourceXSubsampling();
                scaleY /= param.getSourceYSubsampling();
            }
            else
            {
                bitmap = subsample(bitmap, param);
            }
        }
        else
        {
            if (requiresXSubsampling)
            {
                // Apply horizontal subsampling only
                if (requiresScaling)
                {
                    scaleX /= param.getSourceXSubsampling();
                }
                else
                {
                    bitmap = Bitmaps.subsampleX(bitmap, param.getSourceXSubsampling(),
                            param.getSubsamplingXOffset());
                }
            }

            if (requiresYSubsampling)
            {
                // Apply vertical subsampling only
                if (requiresScaling)
                {
                    scaleY /= param.getSourceYSubsampling();
                }
                else
                {
                    bitmap = Bitmaps.subsampleY(bitmap, param.getSourceYSubsampling(),
                            param.getSubsamplingYOffset());
                }
            }
        }

        return buildRaster(bitmap, filterType, scaleX, scaleY);
    }

    private static WritableRaster buildRaster(final Bitmap bitmap, final FilterType filterType,
            final double scaleX, final double scaleY)
    {
        final int height = bitmap.getHeight();
        final int width = bitmap.getWidth();
        
        if (scaleX == 1 && scaleY == 1)
        {
            // scaling not required: clone and invert bitmap into packed raster
            // either empty new bitmap with XNOR or filled new bitmap with XOR, or just NOT
            Bitmap result = new Bitmap(width, height);
            blit(bitmap, result, 0, 0, BitCombinationOperator.NOT);
            DataBufferByte buffer = new DataBufferByte(result.bitmap, result.bitmap.length);
            return Raster.createPackedRaster(buffer, width, height, 1, new Point());
        }

        final Rectangle bounds = new Rectangle(0, 0,
            (int) Math.round(width * scaleX),(int) Math.round(height * scaleY));

        final WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
            bounds.width, bounds.height, 1, new Point());

        final Resizer resizer = new Resizer(scaleX, scaleY);
        final Filter filter = Filter.byType(filterType);
        resizer.resize(bitmap, bitmap.getBounds() /* sourceRegion */, raster, bounds, filter, filter);
        return raster;
    }

    /**
     * Returns the given bitmap as buffered image.
     * 
     * @param bitmap the given bitmap
     * @return the image representation of the bitmap
     */
    public static BufferedImage asBufferedImage(Bitmap bitmap)
    {
        return asBufferedImage(bitmap, FilterType.Gaussian);
    }

    /**
     * Returns the given bitmap as buffered image.
     * 
     * @param bitmap the given bitmap
     * @param filterType type of filter which is used when creating the buffered image
     * @return the image representation of the bitmap
     */
    public static BufferedImage asBufferedImage(Bitmap bitmap, FilterType filterType)
    {
        if (bitmap == null) throw new IllegalArgumentException("bitmap must not be null");
        final JBIG2ReadParam param = new JBIG2ReadParam(1, 1, 0, 0, bitmap.getBounds(), bitmap.getDimensions());
        return asBufferedImage(bitmap, param, filterType);
    }

    /**
     * Returns the given bitmap as buffered image.
     * 
     * @param bitmap the given bitmap
     * @param param ImageReadParam to be used when creating the buffered image
     * @param filterType type of filter which is used when creating the buffered image
     * @return the image representation of the bitmap
     */
    public static BufferedImage asBufferedImage(Bitmap bitmap, ImageReadParam param,
            FilterType filterType)
    {
        if (bitmap == null) throw new IllegalArgumentException("bitmap must not be null");
        if (param == null) throw new IllegalArgumentException("param must not be null");

        final WritableRaster raster = asRaster(bitmap, param, filterType);

        final Dimension sourceRenderSize = param.getSourceRenderSize();

        final double scaleX;
        final double scaleY;
        if (sourceRenderSize != null)
        {
            scaleX = sourceRenderSize.getWidth() / bitmap.getWidth();
            scaleY = sourceRenderSize.getHeight() / bitmap.getHeight();
        }
        else
        {
            scaleX = scaleY = 1d;
        }

        ColorModel cm = null;
        final boolean isScaled = scaleX != 1 || scaleY != 1;
        if (isScaled)
        {
            final byte[] gray = new byte[256];
            for (int i = 0; i < 256; i++ ) gray[i] = (byte)i;
            cm = new IndexColorModel(8, 256, gray, gray, gray);
        }
        else
        {
            cm = new IndexColorModel(1, 2, //
                    new byte[] { 0x00, (byte) 0xff }, new byte[] { 0x00, (byte) 0xff },
                    new byte[] { 0x00, (byte) 0xff });
        }

        return new BufferedImage(cm, raster, false, null);
    }

    /**
     * Returns the specified rectangle area of the bitmap.
     * 
     * @param roi - A {@link Rectangle} that specifies the requested image section.
     * @param src the given bitmap
     * @return A {@code Bitmap} that represents the requested image section.
     */
    public static Bitmap extract(Rectangle roi, final Bitmap src)
    {
        roi = roi.intersection(src.getBounds());

        if ( roi.width<0 || roi.height<0 ) {
            throw new IllegalArgumentException("ROI outside src");
        }
        
        if ( roi.width==0 || roi.height==0 ) {
            return new Bitmap(0, 0);
        }
        
        Bitmap dst = new Bitmap(roi.width, roi.height);
        blit(src, roi, dst, null, 0, 0, CombinationOperator.REPLACE);
        return dst;
    }


    public static Bitmap subsample(Bitmap src, ImageReadParam param)
    {
        if (src == null) throw new IllegalArgumentException("src must not be null");
        if (param == null) throw new IllegalArgumentException("param must not be null");

        final int xSubsampling = param.getSourceXSubsampling();
        final int ySubsampling = param.getSourceYSubsampling();
        final int xSubsamplingOffset = param.getSubsamplingXOffset();
        final int ySubsamplingOffset = param.getSubsamplingYOffset();

        final int dstWidth = (src.getWidth() - xSubsamplingOffset) / xSubsampling;
        final int dstHeight = (src.getHeight() - ySubsamplingOffset) / ySubsampling;

        final Bitmap dst = new Bitmap(dstWidth, dstHeight);

        for (int yDst = 0, ySrc = ySubsamplingOffset; yDst < dst
                .getHeight(); yDst++, ySrc += ySubsampling)
        {
            for (int xDst = 0, xSrc = xSubsamplingOffset; xDst < dst
                    .getWidth(); xDst++, xSrc += xSubsampling)
            {
                final byte pixel = src.getPixel(xSrc, ySrc);
                if (pixel != 0) dst.setPixel(xDst, yDst);
            }
        }

        return dst;
    }

    public static Bitmap subsampleX(Bitmap src, final int xSubsampling, final int xSubsamplingOffset)
    {
        if (src == null) throw new IllegalArgumentException("src must not be null");

        final int dstHeight = (src.getWidth() - xSubsamplingOffset) / xSubsampling;
        final Bitmap dst = new Bitmap(src.getWidth(), dstHeight);

        for (int yDst = 0; yDst < dst.getHeight(); yDst++)
        {
            for (int xDst = 0, xSrc = xSubsamplingOffset; xDst < dst
                    .getWidth(); xDst++, xSrc += xSubsampling)
            {
                final byte pixel = src.getPixel(xSrc, yDst);
                if (pixel != 0) dst.setPixel(xDst, yDst);
            }
        }

        return dst;
    }

    public static Bitmap subsampleY(Bitmap src, final int ySubsampling, final int ySubsamplingOffset)
    {
        if (src == null) throw new IllegalArgumentException("src must not be null");

        final int dstWidth = (src.getWidth() - ySubsamplingOffset) / ySubsampling;
        final Bitmap dst = new Bitmap(dstWidth, src.getHeight());

        for (int yDst = 0, ySrc = ySubsamplingOffset; yDst < dst
                .getHeight(); yDst++, ySrc += ySubsampling)
        {
            for (int xDst = 0; xDst < dst.getWidth(); xDst++)
            {
                final byte pixel = src.getPixel(xDst, ySrc);
                if (pixel != 0) dst.setPixel(xDst, yDst);
            }
        }

        return dst;
    }

    
    public static void fill(Bitmap bitmap, int pixel)
    {
        Blitter.fill(bitmap.bitmap, bitmap.getWidth(), bitmap.getHeight(), bitmap.getRowStride(), pixel);
    }

    
    
    
    public static void combineBitmaps(Bitmap src, Bitmap dst, CombinationOperator operator)
    {
        blit(src, dst, 0, 0, operator);
    }
    
    
    /**
     * This method combines a given bitmap with the current instance.
     * <p>
     * Parts of the bitmap to blit that are outside of the target bitmap will be ignored.
     * 
     * @param src - The bitmap that should be combined with the one of the current instance.
     * @param dst - The destination bitmap.
     * @param x - The x coordinate where the upper left corner of the bitmap to blit should be positioned.
     * @param y - The y coordinate where the upper left corner of the bitmap to blit should be positioned.
     * @param operator - The combination operator for combining two pixels.
     */
    public static void blit(Bitmap src, Bitmap dst, int x, int y, CombinationOperator operator)
    {
        Blitter.blit(
            src.bitmap, src.getWidth(), src.getHeight(), src.getRowStride(), null,
            dst.bitmap, dst.getWidth(), dst.getHeight(), dst.getRowStride(), null,
            x, y, toBlitterOp(operator));
    }
    
    
    private static void blit(Bitmap src, Bitmap dst, int x, int y, BitCombinationOperator operator)
    {
        Blitter.blit(
            src.bitmap, src.getWidth(), src.getHeight(), src.getRowStride(), null,
            dst.bitmap, dst.getWidth(), dst.getHeight(), dst.getRowStride(), null,
            x, y, operator);
    }

    
    public static void blit(Bitmap src, Rectangle srcRegion, Bitmap dst, Rectangle dstRegion, int x, int y, CombinationOperator operator)
    {
        Blitter.blit(
            src.bitmap, src.getWidth(), src.getHeight(), src.getRowStride(), srcRegion,
            dst.bitmap, dst.getWidth(), dst.getHeight(), dst.getRowStride(), dstRegion,
            x, y, toBlitterOp(operator));
    }
    
    
    private static BitCombinationOperator toBlitterOp(CombinationOperator operator)
    {
        switch (operator) {
            case OR:   return BitCombinationOperator.OR;
            case AND:  return BitCombinationOperator.AND;
            case XOR:  return BitCombinationOperator.XOR;
            case XNOR: return BitCombinationOperator.XNOR;
            case REPLACE: 
            default:   return BitCombinationOperator.REPLACE;
        }
    }

}
