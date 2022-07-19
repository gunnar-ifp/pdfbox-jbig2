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

import java.util.Arrays;
import java.util.Collection;

import org.apache.pdfbox.jbig2.image.Blitter.BitCombinationOperator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class BitmapsByteCombinationTest
{
    private static final byte SRC = 0xD;
    private static final byte DST = 0xA;

    private final int expected;
    private final BitCombinationOperator operator;

    @Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
            { 0xF,  BitCombinationOperator.OR },
            { 0x8,  BitCombinationOperator.AND },
            { 0x7,  BitCombinationOperator.XOR },
            { -8,   BitCombinationOperator.XNOR },
            { SRC,  BitCombinationOperator.REPLACE },
            { ~SRC, BitCombinationOperator.NOT }
        });
    }

    public BitmapsByteCombinationTest(final int expected, final BitCombinationOperator operator)
    {
        this.expected = expected;
        this.operator = operator;
    }

    @Test
    public void test()
    {
        byte[] src = { SRC }, dst = { DST };
        Blitter.blit(src, 8, 1, 1, null, dst, 8, 1, 1, null, 0, 0, operator);
        assertEquals(expected, dst[0]);
    }

}
