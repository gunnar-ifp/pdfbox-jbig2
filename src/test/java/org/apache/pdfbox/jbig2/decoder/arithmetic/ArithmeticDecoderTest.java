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

import java.io.InputStream;
import javax.imageio.stream.ImageInputStream;

import org.apache.pdfbox.jbig2.io.DefaultInputStreamFactory;
import org.junit.Assert;
import org.junit.Test;

public class ArithmeticDecoderTest
{

    int[][] tracedata = { //
            { 0, 0x8000, 0x42638000, 0x3D9C0000 }, { 0, 0xAC02, 0x84C70000, 0x273A0000 },
            { 0, 0xF002, 0xA18C7600, 0x4E758800 }, { 0, 0xD801, 0x898B7600, 0x4E758800 },
            { 0, 0xC000, 0x718A7600, 0x4E758800 }, { 0, 0xA7FF, 0x59897600, 0x4E758800 },
            { 0, 0x8FFE, 0x41887600, 0x4E758800 }, { 0, 0xEFFA, 0x530EEC00, 0x9CEB1000 },
            { 0, 0xE539, 0x484DEC00, 0x9CEB1000 }, { 0, 0xDA78, 0x3D8CEC00, 0x9CEB1000 },
            { 0, 0xCFB7, 0x32CBEC00, 0x9CEB1000 }, { 0, 0xC4F6, 0x280AEC00, 0x9CEB1000 },
            { 0, 0xBA35, 0x1D49EC00, 0x9CEB1000 }, { 0, 0xAF74, 0x1288EC00, 0x9CEB1000 },
            { 1, 0xA4B3, 0x07C7EC00, 0x9CEB1000 }, { 0, 0xAC10, 0x7C7EC000, 0x2F910000 },
            { 0, 0x900F, 0x607DC000, 0x2F910000 }, { 0, 0xE81C, 0x88F98000, 0x5F220000 },
            { 0, 0xD21B, 0x72F88000, 0x5F220000 }, { 0, 0xBC1A, 0x5CF78000, 0x5F220000 },
            { 0, 0xA619, 0x46F68000, 0x5F220000 }, { 0, 0x9018, 0x30F58000, 0x5F220000 },
            { 0, 0xF42E, 0x35E90000, 0xBE440000 }, { 0, 0xE32D, 0x24E80000, 0xBE440000 },
            { 0, 0xD22C, 0x13E70000, 0xBE440000 }, { 1, 0xC12B, 0x02E60000, 0xBE440000 },
            { 0, 0x8808, 0x1737E000, 0x70D01800 }, { 1, 0xE80E, 0x066DC000, 0xE1A03000 },
            { 0, 0x9008, 0x336E0000, 0x5C998000 }, { 0, 0xF40E, 0x3ADA0000, 0xB9330000 },
            { 0, 0xE00D, 0x26D90000, 0xB9330000 }, { 1, 0xCC0C, 0x12D80000, 0xB9330000 },
            { 0, 0xA008, 0x96C70800, 0x0940F000 }, { 0, 0x8807, 0x7EC60800, 0x0940F000 },
            { 0, 0xE00C, 0xCD8A1000, 0x1281E000 }, { 0, 0xCA0B, 0xB7891000, 0x1281E000 },
            { 0, 0xB40A, 0xA1881000, 0x1281E000 }, { 0, 0x9E09, 0x8B871000, 0x1281E000 },
            { 0, 0x8808, 0x75861000, 0x1281E000 }, { 0, 0xE40E, 0xBF0A2000, 0x2503C000 },
            { 0, 0xD00D, 0xAB092000, 0x2503C000 }, { 0, 0xBC0C, 0x97082000, 0x2503C000 },
            { 0, 0xA80B, 0x83072000, 0x2503C000 }, { 0, 0x940A, 0x6F062000, 0x2503C000 },
            { 0, 0x8009, 0x5B052000, 0x2503C000 }, { 0, 0xD810, 0x8E084000, 0x4A078000 },
            { 0, 0xC60F, 0x7C074000, 0x4A078000 }, { 0, 0xB40E, 0x6A064000, 0x4A078000 },
            { 0, 0xA20D, 0x58054000, 0x4A078000 }, { 0, 0x900C, 0x46044000, 0x4A078000 },
            { 0, 0xFC16, 0x68068000, 0x940F0000 }, { 0, 0xEB15, 0x57058000, 0x940F0000 },
            { 0, 0xDA14, 0x46048000, 0x940F0000 }, { 0, 0xC913, 0x35038000, 0x940F0000 },
            { 0, 0xB812, 0x24028000, 0x940F0000 }, { 0, 0xA711, 0x13018000, 0x940F0000 },
            { 1, 0x9610, 0x02008000, 0x940F0000 }, { 1, 0x8808, 0x10068400, 0x78017800 },
            { 0, 0xA008, 0x80342000, 0x1FD3C000 }, { 0, 0x8807, 0x68332000, 0x1FD3C000 },
            { 0, 0xE00C, 0xA0644000, 0x3FA78000 }, { 0, 0xCA0B, 0x8A634000, 0x3FA78000 },
            { 0, 0xB40A, 0x74624000, 0x3FA78000 }, { 0, 0x9E09, 0x5E614000, 0x3FA78000 },
            { 0, 0x8808, 0x48604000, 0x3FA78000 }, { 0, 0xE40E, 0x64BE8000, 0x7F4F0000 },
            { 0, 0xD00D, 0x50BD8000, 0x7F4F0000 }, { 0, 0xBC0C, 0x3CBC8000, 0x7F4F0000 },
            { 0, 0xA80B, 0x28BB8000, 0x7F4F0000 }, { 0, 0x940A, 0x14BA8000, 0x7F4F0000 },
            { 1, 0x8009, 0x00B98000, 0x7F4F0000 }, { 1, 0xA008, 0x05CD0C00, 0x9A3AF000 },
            { 0, 0xC008, 0x2E686000, 0x919F8000 }, { 1, 0x9E07, 0x0C676000, 0x919F8000 },
            { 0, 0x8804, 0x319D8000, 0x56660000 }, { 1, 0xC006, 0x13390000, 0xACCC0000 },
            { 0, 0x9004, 0x4CE41000, 0x431FEC00 }, { 0, 0xC006, 0x39C62000, 0x863FD800 },
            { 1, 0x9805, 0x11C52000, 0x863FD800 }, { 0, 0xA004, 0x47148000, 0x58EF6000 },
            { 1, 0xD806, 0x26270000, 0xB1DEC000 }, { 0, 0xC004, 0x989C0000, 0x27670000 },
            { 0, 0x8C03, 0x649B0000, 0x27670000 }, { 0, 0xB004, 0x61340400, 0x4ECFFA00 },
            { 0, 0x8003, 0x31330400, 0x4ECFFA00 }, { 1, 0xA004, 0x02640800, 0x9D9FF400 },
            { 1, 0xA004, 0x09902000, 0x9673D000 }, { 1, 0xD004, 0x26408000, 0xA9C34000 },
            { 0, 0xE004, 0x99020000, 0x47010000 }, { 0, 0x9803, 0x51010000, 0x47010000 },
            { 1, 0xA004, 0x12004000, 0x8E03BE00 }, { 0, 0xE004, 0x48010000, 0x9802F800 },
            { 1, 0x9803, 0x00000000, 0x9802F800 }, { 0, 0x9002, 0x00000000, 0x9001F000 },
            { 1, 0xA202, 0x00000000, 0xA201E000 }, { 0, 0x9002, 0x00000000, 0x9001C000 },
            { 1, 0xA202, 0x00000000, 0xA2018000 }, { 0, 0x9002, 0x00000000, 0x90010000 },
            { 1, 0xA202, 0x00000000, 0xA201FE00 }, { 0, 0x9002, 0x00000000, 0x9001FC00 },
            { 1, 0xA202, 0x00000000, 0xA201F800 }, { 0, 0x9002, 0x00000000, 0x9001F000 },
            { 1, 0xA202, 0x00000000, 0xA201E000 }, { 0, 0x9002, 0x00000000, 0x9001C000 },
            { 1, 0xA202, 0x00000000, 0xA2018000 }, { 0, 0x9002, 0x00000000, 0x90010000 },
            { 1, 0xA202, 0x00000000, 0xA201FE00 }, { 0, 0x9002, 0x00000000, 0x9001FC00 },
            { 1, 0xA202, 0x00000000, 0xA201F800 }, { 0, 0x9002, 0x00000000, 0x9001F000 },
            { 1, 0xA202, 0x00000000, 0xA201E000 }, { 0, 0x9002, 0x00000000, 0x9001C000 },
            { 1, 0xA202, 0x00000000, 0xA2018000 }, { 0, 0x9002, 0x00000000, 0x90010000 },
            { 1, 0xA202, 0x00008200, 0xA2017C00 }, { 0, 0x9002, 0x00010400, 0x9000F800 },
            { 1, 0xA202, 0x00020800, 0xA1FFF000 }, { 0, 0x9002, 0x00041000, 0x8FFDE000 },
            { 1, 0xA202, 0x00082000, 0xA1F9C000 }, { 0, 0x9002, 0x00104000, 0x8FF18000 },
            { 1, 0xA202, 0x00208000, 0xA1E10000 }, { 0, 0x9002, 0x00410000, 0x8FC00000 },
            { 1, 0xA202, 0x00821A00, 0xA17FE400 }, { 0, 0x9002, 0x01043400, 0x8EFDC800 },
            { 1, 0xA202, 0x02086800, 0x9FF99000 }, { 0, 0x9002, 0x0410D000, 0x8BF12000 },
            { 1, 0xA202, 0x0821A000, 0x99E04000 }, { 0, 0x9002, 0x10434000, 0x7FBE8000 },
            { 1, 0xA202, 0x20868000, 0x817B0000 }, { 0, 0x9002, 0x410D0000, 0x4EF40000 },
            { 0, 0xA202, 0x821B7600, 0x1FE68800 }, { 0, 0xB402, 0x7434EC00, 0x3FCD1000 },
            { 0, 0xF802, 0x7867D800, 0x7F9A2000 }, { 0, 0xC401, 0x4466D800, 0x7F9A2000 },
            { 1, 0x9000, 0x1065D800, 0x7F9A2000 }, { 0, 0xD004, 0x41976000, 0x8E6C8000 },
            { 1, 0x9803, 0x09966000, 0x8E6C8000 }, { 1, 0xE004, 0x26598000, 0xB9AA0000 },
            { 0, 0x9002, 0x4CB30000, 0x434E0000 }, { 0, 0xA202, 0x99670C00, 0x089AF200 },
            { 0, 0xB402, 0xA2CC1800, 0x1135E400 }, { 0, 0xF802, 0xD5963000, 0x226BC800 },
            { 0, 0xC401, 0xA1953000, 0x226BC800 }, { 0, 0x9000, 0x6D943000, 0x226BC800 },
            { 0, 0xB7FE, 0x73266000, 0x44D79000 }, { 0, 0x87FD, 0x43256000, 0x44D79000 },
            { 1, 0xAFF8, 0x2648C000, 0x89AF2000 }, { 0, 0xA004, 0x99230000, 0x06E08000 },
            { 0, 0xD806, 0xCA440000, 0x0DC10000 }, { 0, 0xA805, 0x9A430000, 0x0DC10000 },
            { 0, 0xF008, 0xD485E800, 0x1B821600 }, { 0, 0xC807, 0xAC84E800, 0x1B821600 },
            { 0, 0xA006, 0x8483E800, 0x1B821600 }, { 0, 0xF00A, 0xB905D000, 0x37042C00 },
            { 0, 0xCC09, 0x9504D000, 0x37042C00 }, { 0, 0xA808, 0x7103D000, 0x37042C00 },
            { 0, 0x8407, 0x4D02D000, 0x37042C00 }, { 0, 0xC00C, 0x5203A000, 0x6E085800 },
            { 0, 0x9E0B, 0x3002A000, 0x6E085800 }, { 0, 0xF814, 0x1C034000, 0xDC10B000 },
            { 1, 0xDC13, 0x00024000, 0xDC10B000 }, { 1, 0xE008, 0x00120000, 0xDFF58000 },
            { 1, 0x9004, 0x00486200, 0x8FBB9C00 }, { 1, 0xC004, 0x01218800, 0xBEE27000 },
            { 1, 0xD004, 0x04862000, 0xCB7DC000 }, { 1, 0xE004, 0x12188000, 0xCDEB0000 },
            { 0, 0x9002, 0x24310000, 0x6BD00000 }, { 0, 0xA202, 0x4862FE00, 0x599F0000 },
            { 1, 0xB402, 0x00C3FC00, 0xB33E0000 }, { 1, 0xE004, 0x030FF000, 0xDCF40000 },
            { 0, 0x9002, 0x061FE000, 0x89E20000 }, { 1, 0xA202, 0x0C3FC000, 0x95C20000 },
            { 0, 0x9002, 0x187F8000, 0x77820000 }, { 1, 0xA202, 0x30FF0000, 0x71020000 },
            { 1, 0x9002, 0x61FFFE00, 0x2E020000 }, { 1, 0xFC04, 0x43FBF800, 0xB8080000 },
            { 1, 0xA802, 0x87F7F000, 0x200A0000 }, { 0, 0xA402, 0x63EDE000, 0x40140000 },
            { 0, 0x9C02, 0x1BD9C000, 0x80280000 }, { 1, 0xAC02, 0x37B38000, 0x744E0000 },
            { 1, 0xA802, 0x6F670000, 0x389A0000 }, { 1, 0xA402, 0x32CE2000, 0x7133DC00 },
            { 1, 0xAC02, 0x659C4000, 0x4665B800 }, { 0, 0xB002, 0x23368000, 0x8CCB7000 },
            { 1, 0xA202, 0x466D0000, 0x5B94E000 }, { 1, 0xA802, 0x8CDA0000, 0x1B27C000 },
            { 1, 0xAE02, 0x77B20000, 0x364F8000 }, { 1, 0xCC02, 0x5F620000, 0x6C9F0000 },
            { 0, 0x9401, 0x27610000, 0x6C9F0000 }, { 1, 0xE004, 0x9D87FC00, 0x427C0000 },
            { 1, 0x9803, 0x5586FC00, 0x427C0000 }, { 0, 0xA004, 0x1B0BF800, 0x84F80000 },
            { 1, 0xE004, 0x6C2FE000, 0x73D40000 }, { 0, 0x9803, 0x242EE000, 0x73D40000 },
            { 1, 0x9002, 0x485DC000, 0x47A40000 }, { 1, 0xA202, 0x90BB8000, 0x11460000 },
            { 1, 0xB402, 0x91750000, 0x228C0000 }, { 1, 0xF802, 0xB2E8DC00, 0x45192000 },
            { 1, 0xC401, 0x7EE7DC00, 0x45192000 }, { 1, 0x9000, 0x4AE6DC00, 0x45192000 },
            { 0, 0xB7FE, 0x2DCBB800, 0x8A324000 }, { 1, 0xC004, 0xB72EE000, 0x08D50000 },
            { 1, 0x8C03, 0x832DE000, 0x08D50000 }, { 1, 0xB004, 0x9E59C000, 0x11AA0000 },
            { 1, 0x8003, 0x6E58C000, 0x11AA0000 }, { 1, 0xA004, 0x7CAF8000, 0x23540000 },
            { 1, 0xF006, 0xA95D0000, 0x46A80000 }, { 1, 0xCC05, 0x855C0000, 0x46A80000 },
            { 1, 0xA804, 0x615B0000, 0x46A80000 }, { 1, 0x8403, 0x3D5A0000, 0x46A80000 },
            { 1, 0xC004, 0x32B28E00, 0x8D517000 }, { 0, 0x9E03, 0x10B18E00, 0x8D517000 },
            { 1, 0x8804, 0x42C63800, 0x453DC000 }, { 1, 0xC006, 0x358A7000, 0x8A7B8000 },
            { 0, 0x9C05, 0x11897000, 0x8A7B8000 }, { 1, 0x9004, 0x4625C000, 0x49DE0000 },
            { 1, 0xC006, 0x2C498000, 0x93BC0000 }, { 0, 0x9805, 0x04488000, 0x93BC0000 },
            { 0, 0xA004, 0x11223400, 0x8EE1CA00 }, { 1, 0xD004, 0x4488D000, 0x8B7B2800 },
            { 0, 0x9803, 0x0C87D000, 0x8B7B2800 }, { 0, 0xE004, 0x321F4000, 0xADE4A000 },
            { 0, 0x9002, 0x643E8000, 0x2BC34000 }, { 0, 0xFC04, 0x4CF60000, 0xAF0D0000 },
            { 0, 0xA802, 0x99EDB600, 0x0E144800 }, { 1, 0xA402, 0x87D96C00, 0x1C289000 },
            { 0, 0x9C02, 0x63B0D800, 0x38512000 }, { 0, 0x8C02, 0x1B5FB000, 0x70A24000 },
            { 1, 0xAC02, 0x36BF6000, 0x75428000 }, { 1, 0xA802, 0x6D7EC000, 0x3A830000 },
            { 1, 0xA402, 0x2EFB8000, 0x75060000 }, { 1, 0xAC02, 0x5DF70000, 0x4E0A0000 },
            { 0, 0xB002, 0x13ECD400, 0x9C152A00 }, { 1, 0xA202, 0x27D9A800, 0x7A285400 },
            { 0, 0xA802, 0x4FB35000, 0x584EA800 }, { 0, 0xA202, 0x9F66A000, 0x029B5000 },
            { 0, 0x9C02, 0x96CB4000, 0x0536A000 }, { 1, 0x8C02, 0x81948000, 0x0A6D4000 },
            { 1, 0xD804, 0xAE4E0000, 0x29B50000 }, { 0, 0x8203, 0x584D0000, 0x29B50000 },
            { 1, 0xB008, 0x09337C00, 0xA6D48000 }, { 0, 0xAC02, 0x1266F800, 0x999B0000 },
            { 1, 0xAC02, 0x24CDF000, 0x87340000 }, { 0, 0xAC02, 0x499BE000, 0x62660000 },
            { 0, 0xAC02, 0x9337C000, 0x18CA0000 }, { 0, 0xAC02, 0x7A6D8000, 0x31940000 },
            { 1, 0xB002, 0x4CD90000, 0x63280000 }, { 1, 0xA202, 0x99B3FE00, 0x084E0000 },
            { 1, 0x9C02, 0x8B65FC00, 0x109C0000 }, { 0, 0x8C02, 0x6AC9F800, 0x21380000 },
            { 1, 0xD804, 0x5323E000, 0x84E00000 }, { 1, 0xAC02, 0xA647C000, 0x05BA0000 },
            { 1, 0xAC02, 0xA08D8000, 0x0B740000 }, { 1, 0xB002, 0x99190000, 0x16E80000 },
            { 1, 0xBE02, 0x9031FE00, 0x2DD00000 }, { 1, 0xEC02, 0x9061FC00, 0x5BA00000 } };

    @Test
    public void decodeTest() throws Throwable
    {
        InputStream is = getClass().getResourceAsStream("/images/arith/encoded testsequence");
        DefaultInputStreamFactory factory = new DefaultInputStreamFactory();
        ImageInputStream iis = factory.getInputStream(is);

        ArithmeticDecoder decoder = new ArithmeticDecoder(iis);

        CX cx = new CX(1);
        for (int i = 0; i < 257; i++)
        {
            decoder.decode(cx, 0);
        }

    }

    @Test
    public void decodeTestWithTracadataComparison() throws Throwable
    {
        InputStream is = getClass().getResourceAsStream("/images/arith/encoded testsequence");
        DefaultInputStreamFactory factory = new DefaultInputStreamFactory();
        ImageInputStream iis = factory.getInputStream(is);

        ArithmeticDecoder decoder = new ArithmeticDecoder(iis);
        CX cx = new CX(1);

        for (int i = 0; i < 255; i++)
        {
            Assert.assertEquals(tracedata[i][0], decoder.decode(cx, 0));
            Assert.assertEquals(tracedata[i + 1][1], decoder.getA());
            Assert.assertEquals(tracedata[i + 1][2], decoder.getC());

        }
    }
}
