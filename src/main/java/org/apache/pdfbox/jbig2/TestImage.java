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
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
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
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * This is a utility class. It can be used to show intermediary results.
 */
public class TestImage extends JFrame
{
    private static final long serialVersionUID = 7353175320371957550L;

    
    public TestImage(byte data[], int w, int h, int scanlineStride)
    {
        this(makeImage(data, w, h, scanlineStride));
    }

    
    public TestImage(BufferedImage image)
    {
        super("Demo image");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        final ImageComponent imageComponent = new ImageComponent(image);
        final JScrollPane sp = new JScrollPane(imageComponent);
        sp.setOpaque(true);
        sp.setAutoscrolls(true);
        sp.setWheelScrollingEnabled(false);
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
    
    
    class ImageComponent extends JComponent implements MouseWheelListener, MouseListener, MouseMotionListener
    {
        private static final long serialVersionUID = -5921296548288376287L;
        
        private final BufferedImage image;
        private Point start, origin;
        private int mscale = 0;
        
        public ImageComponent(BufferedImage image)
        {
            this.image = image;
            setAutoscrolls(true);
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }
        
        private float getScale()
        {
            return 1 + mscale * 0.05f;            
        }
        
        @Override
        public Insets getInsets()
        {
            return new Insets(1, 1, 1, 1);
        }

        
        @Override
        public Dimension getPreferredSize()
        {
            float scale = getScale();
            return new Dimension(Math.round(image.getWidth() * scale), Math.round(image.getHeight() * scale));
        }
        
        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g;
            Dimension d = getPreferredSize();
            Object old = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(image, 0, 0, d.width, d.height, this);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old==null ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : old);
        }
        
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            mscale -= e.getWheelRotation();
            getParent().revalidate();
        }
                
        @Override
        public void mousePressed(MouseEvent e) {
            JScrollPane sp = (JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
            start  = new Point(sp.getHorizontalScrollBar().getValue(), sp.getVerticalScrollBar().getValue());
            origin = e.getLocationOnScreen();
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            JScrollPane sp = (JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
            int x = Math.max(0, Math.min(sp.getHorizontalScrollBar().getMaximum(), start.x + origin.x - e.getXOnScreen()));
            int y = Math.max(0, Math.min(sp.getVerticalScrollBar()  .getMaximum(), start.y + origin.y - e.getYOnScreen()));
            sp.getHorizontalScrollBar().setValue(x);
            sp.getVerticalScrollBar()  .setValue(y);
        }
        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        @Override public void mouseMoved(MouseEvent e) {}
        @Override public void mouseReleased(MouseEvent e) {}
    }

    
    
    private static BufferedImage makeImage(byte data[], int w, int h, int scanlineStride)
    {
        // Color-Model sagt: bit = 0 -> schwarz, bit = 1 -> weiss. Ggf. umdrehen.
        ColorModel colorModel = new IndexColorModel(1, 2, new byte[] { (byte) 0xff, 0x00 },
                new byte[] { (byte) 0xff, 0x00 }, new byte[] { (byte) 0xff, 0x00 });

        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        SampleModel sampleModel = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, w, h, 1,
                scanlineStride, 0);
        WritableRaster writableRaster = Raster.createWritableRaster(sampleModel, dataBuffer,
                new Point(0, 0));

        return new BufferedImage(colorModel, writableRaster, false, null);
    }
}
