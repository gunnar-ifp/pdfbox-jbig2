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

/**
 * CX represents the context used by arithmetic decoding and arithmetic integer decoding.
 * It selects the probability estimate and statistics used during decoding procedure.
 */
public final class CX
{
    /** <pre>
     *   bit 6 - 1: CX (6 bit)
     *   bit     0: MPS
     * </pre>
     */
    private final byte[] cxmps;
    

    /**
     * @param size - Amount of context values.
     */
    public CX(int size)
    {
        this.cxmps = new byte[size];
    }

    
    /**
     * Returns the combined CS and MPS value: <pre>
     *   bit 6 - 1: CX (6 bit)
     *   bit     0: MPS
     * </pre>
     */
    int get(int index)
    {
        return cxmps[index];
    }
    
    
    /**
     * Sets the combined CS and MPS value: <pre>
     *   bit 6 - 1: CX (6 bit)
     *   bit     0: MPS
     * </pre>
     * 
     * Argument must be in range 0 - 0x7f.
     */
    void set(int index, int value)
    {
        cxmps[index] = (byte)value;
    }

}
