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

package org.apache.pdfbox.jbig2;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

/**
 * This is a utility class. It can be used to show intermediary results.
 */
public class TestImage extends JFrame
{
    private static final long serialVersionUID = 7353175320371957550L;

    static class ImageComponent extends JComponent implements MouseWheelListener, MouseListener, MouseMotionListener
    {
        private static final long serialVersionUID = -5921296548288376287L;
        Image myImage;
        int x, y, mscale = 0, mx, my;
        
        {
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }

        
        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        @Override public void mouseMoved(MouseEvent e) {}
        @Override public void mouseReleased(MouseEvent e) {}
        @Override public void mousePressed(MouseEvent e) {
            x = e.getX();
            y = e.getY();
        }
        @Override public void mouseDragged(MouseEvent e) {
            mx = Math.min(0, mx + e.getX() - x);
            my = Math.min(0, my + e.getY() - y);
            x = e.getX();
            y = e.getY();
            repaint();
        }
        @Override public void mouseWheelMoved(MouseWheelEvent e) {
            mscale -= e.getWheelRotation();
            repaint();
        }
        

        protected ImageComponent()
        {
            super();
        }

        public ImageComponent(Image image)
        {
            super();
            setImage(image);
        }

        /**
         * Sets an image to be shown
         */
        public void setImage(Image image)
        {
            if (myImage != null)
            {
                myImage.flush();
            }

            myImage = image;

            if (myImage != null)
            {
                MediaTracker mt = new MediaTracker(this);

                mt.addImage(myImage, 0);

                try
                {
                    mt.waitForAll();
                }
                catch (Exception ex)
                {
                }
                invalidate();
                validate();
                repaint();
            }
        }

        /**
         * Returns 1px wide border insets.
         */
        @Override
        public Insets getInsets()
        {
            return new Insets(1, 1, 1, 1);
        }

        /**
         * Paints the image with current zoom level.
         */
        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g;
            if (myImage != null)
            {
                double scaleW = (double)getWidth()  / myImage.getWidth(this);
                double scaleH = (double)getHeight() / myImage.getHeight(this);
                double scale = Math.min(scaleW, scaleH) + mscale * 0.05d;

                g2.scale(scale, scale);
                g2.drawImage(myImage, mx + 1, my + 1, this);
        }
        }
    }

    public TestImage(byte data[], int w, int h, int scanlineStride)
    {
        super("Demo image");

        // Color-Model sagt: bit = 0 -> schwarz, bit = 1 -> weiss. Ggf. umdrehen.
        ColorModel colorModel = new IndexColorModel(1, 2, new byte[] { (byte) 0xff, 0x00 },
                new byte[] { (byte) 0xff, 0x00 }, new byte[] { (byte) 0xff, 0x00 });

        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        SampleModel sampleModel = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, w, h, 1,
                scanlineStride, 0);
        WritableRaster writableRaster = Raster.createWritableRaster(sampleModel, dataBuffer,
                new Point(0, 0));

        BufferedImage image = new BufferedImage(colorModel, writableRaster, false, null);

        ImageComponent imageComponent = new ImageComponent(image);
        // imageComponent.setScale(4);

        JScrollPane sp = new JScrollPane(imageComponent);
        sp.setOpaque(true);

        setContentPane(sp);

        pack();
        setSize(new Dimension(1600, 900));
        setVisible(true);

        try
        {
            System.in.read();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public TestImage(BufferedImage bufferedImage)
    {
        super("Demobild");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        ImageComponent imageComponent = new ImageComponent(bufferedImage);

        JScrollPane sp = new JScrollPane(imageComponent);
        sp.setOpaque(true);

        setContentPane(sp);

        pack();
        setSize(1024, 768);
        setVisible(true);

        try
        {
            System.in.read();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
