/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailbox.torque;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility methods for messages.
 * 
 * @deprecated Torque implementation will get removed in the next release
 */
@Deprecated()
public class MessageUtils {

    private static final int BYTE_STREAM_CAPACITY = 8182;

    private static final int BYTE_BUFFER_SIZE = 4096;

    public static final byte CR = 0x0D;

    public static final byte LF = 0x0A;

    /**
     * Counts the number of <code>CR</code>'s and <code>LF</code>'s which
     * do are part of <code>CRLF</code>s.
     * 
     * @param contents
     *            bytes, not null
     * @return the number of lines which are not normal <code>CRLF</code>
     */
    public static long countUnnormalLines(final byte[] contents) {
        int count = 0;
        if (contents != null) {
            final int length = contents.length;
            for (int i = 0; i < length; i++) {
                byte current = contents[i];
                if (current == CR) {
                    final int next = i + 1;
                    if (next < length) {
                        if (contents[next] != LF) {
                            count++;
                        }
                    } else {
                        count++;
                    }
                } else if (current == LF) {
                    final int last = i - 1;
                    if (last >= 0) {
                        if (contents[last] != CR) {
                            count++;
                        }
                    } else {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Writes bytes into the buffer using naive encoding and converts isolated
     * LF and CR to CRLF.
     * 
     * @param contents
     *            bytes to write, not null
     * @param buffer
     *            <code>StringBuffer</code> sink, not null
     */
    public static void normalisedWriteTo(final byte[] contents,
            final StringBuffer buffer) {
        char last = 0;
        for (int i = 0; i < contents.length; i++) {
            final char current = (char) contents[i];
            if (current == '\n') {
                if (last == '\r') {
                    buffer.append('\n');
                } else {
                    buffer.append('\r');
                    buffer.append('\n');
                }
            } else {
                if (last == '\r') {
                    buffer.append('\n');
                }
                buffer.append(current);
            }
            last = current;
        }
        if (last == '\r') {
            buffer.append('\n');
        }
    }

    public static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(
                BYTE_STREAM_CAPACITY);
        byte[] buf = new byte[BYTE_BUFFER_SIZE];
        int read;
        while ((read = is.read(buf)) > 0) {
            baos.write(buf, 0, read);
        }

        final byte[] bytes = baos.toByteArray();
        return bytes;
    }
}
