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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.imageio.stream.ImageInputStream;

import org.apache.pdfbox.jbig2.JBIG2DocumentFacade;
import org.apache.pdfbox.jbig2.err.JBIG2Exception;
import org.apache.pdfbox.jbig2.util.CombinationOperator;
import org.apache.pdfbox.jbig2.util.DefaultInputStreamFactory;
import org.junit.Test;

public class BitmapsBlitTest
{

    @Test
    public void testCompleteBitmapTransfer() throws IOException, JBIG2Exception
    {

        final File inputFile = new File("target/images/042_1.jb2");
        // skip test if input stream isn't available
        assumeTrue(inputFile.exists());

        final InputStream inputStream = new FileInputStream(inputFile);

        final DefaultInputStreamFactory disf = new DefaultInputStreamFactory();
        final ImageInputStream iis = disf.getInputStream(inputStream);

        final JBIG2DocumentFacade doc = new JBIG2DocumentFacade(iis);

        final Bitmap src = doc.getPageBitmap(1);
        final Bitmap dst = new Bitmap(src.getWidth(), src.getHeight());
        Bitmaps.blit(src, dst, 0, 0, CombinationOperator.REPLACE);

        assertTrue(src.equals(dst));
    }

    @Test
    public void test() throws IOException, JBIG2Exception
    {

        final File inputFile = new File("target/images/042_1.jb2");
        // skip test if input stream isn't available
        assumeTrue(inputFile.exists());

        final InputStream inputStream = new FileInputStream(inputFile);

        final DefaultInputStreamFactory disf = new DefaultInputStreamFactory();
        final ImageInputStream iis = disf.getInputStream(inputStream);

        final JBIG2DocumentFacade doc = new JBIG2DocumentFacade(iis);

        final Bitmap dst = doc.getPageBitmap(1);

        final Rectangle roi = new Rectangle(100, 100, 100, 100);
        final Bitmap src = new Bitmap(roi.width, roi.height);
        Bitmaps.blit(src, dst, roi.x, roi.y, CombinationOperator.REPLACE);

        final Bitmap dstRegionBitmap = Bitmaps.extract(roi, dst);

        assertTrue(src.equals(dstRegionBitmap));
    }


    @Test
    public void testShiftReplace() throws IOException, JBIG2Exception
    {
        final int bytes = 16, pixels = bytes * 8, lines = 1;
        Bitmap src = new Bitmap(pixels, lines);
        Bitmap dst = new Bitmap(pixels + 6, lines + 1);
        fillBitmap(src, 0);

        final int end = bytes - 1;
        for ( int blit = -8; blit <= 8; blit++ ) {
            Arrays.fill(dst.bitmap, (byte)-1);
            Bitmaps.blit(src, dst, blit, 0, CombinationOperator.REPLACE);
            for ( int i = 0, last = (pixels + blit - 1) / 8; i<=last; i++ ) {
                int s =
                    ( i == 0     ? 0xff55aa
                    : i==end     ? 0x55aaff
                    : i> end     ? 0xaaffff
                    : i % 2 == 0 ? 0xaa55aa : 0x55aa55);
                s = (s >> blit + 8) & 0xff;
                // this is because dst is only 6 pixels wider!
                if ( i>end ) s |= 3;
                int b = 0xff & dst.bitmap[i];
                assertEquals(blit + ":[" + i + "]", s, b);
            }
        }
    }

    
    private static void fillBitmap(Bitmap bitmap, int toggle)
    {
    for ( int y = 0, idx = 0; y<bitmap.getHeight(); y++ ) {
        for ( int x = 0; x<bitmap.getRowStride(); x++ ) bitmap.bitmap[idx++] = (byte)((toggle + y + x) % 2 == 0 ? 0x55 : 0xaa);
    }
        
    }
    
}
