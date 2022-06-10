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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import javax.imageio.stream.ImageInputStream;

import org.apache.pdfbox.jbig2.err.JBIG2Exception;
import org.apache.pdfbox.jbig2.io.DefaultInputStreamFactory;

/**
 * Arithmetic Decoder performance tests.
 * <p>
 * Run with {@code -XX:-TieredCompilation}, {@code -XX:TieredStopAtLevel=1}, or {@code -Djava.compiler=NONE}.
 * 
 * <p>
 * For in depth optimization debugging use JITWatch with<br>
 * {@code -XX:+UnlockDiagnosticVMOptions -XX:+TraceClassLoading -XX:+LogCompilation -XX:+PrintAssembly -XX:+DebugNonSafepoints}<br>
 * {@code -XX:+UnlockDiagnosticVMOptions -Xlog:class+load=info -XX:+LogCompilation -XX:+PrintAssembly -XX:+DebugNonSafepoints}<br>
 */
public class ArithmeticDecoderPerformanceTest
{
    public static void main(String[] args)
        throws InterruptedException, InvocationTargetException, IOException, JBIG2Exception
    {
        int limit = 20;
        //System.in.read();  // pause for jvisualvm if necessary 

        final ImageInputStream iis;
        InputStream in = ArithmeticDecoderPerformanceTest.class.getResourceAsStream("/images/002.jb2");
        try {
            byte[] raw = new byte[in.available()];
            in.read(raw);
            iis = new DefaultInputStreamFactory().getInputStream(new ByteArrayInputStream(raw));
        } finally {
            in.close();
        }
        
        final JBIG2Document doc = new JBIG2Document(iis);
        final JBIG2Page page = doc.getPage(1);
        
        if ( !System.getProperty("java.vm.info", "").contains("interpret") ) {
            System.out.println("warming up for " + (limit / 10) + " seconds");
            for ( long end = System.currentTimeMillis() + limit * 1000 / 10; System.currentTimeMillis() < end; ) {
                for ( int i = 0; i<10; i++ ) {
                    page.getBitmap();
                    page.clearPageData();
                }
            }
            Thread.sleep(1000);
        }
        
        System.out.println("running for " + limit + " seconds");
        final long now = System.currentTimeMillis();
        
        int loops = 0;
        for ( long end = now + limit * 1000; System.currentTimeMillis() < end; loops++ ) {
            page.getBitmap();
            page.clearPageData();
        }
        long time = System.currentTimeMillis() - now;
        System.out.println("used " + time + " ms for " + loops + " iterations: " + ((double)time / loops) + " ms/loop");
    }

}
