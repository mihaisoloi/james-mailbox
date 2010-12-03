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

package org.apache.james.mailbox.store.streaming;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;

import org.apache.james.mailbox.Content;

/**
 * Utility methods for messages.
 * 
 */
public class StreamUtils {

    private static final int BYTE_STREAM_CAPACITY = 8182;

    private static final int BYTE_BUFFER_SIZE = 4096;

    public static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = out(is);

        final byte[] bytes = baos.toByteArray();
        return bytes;
    }

    public static ByteArrayOutputStream out(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(
                BYTE_STREAM_CAPACITY);
        out(is, baos);
        return baos;
    }

    public static void out(InputStream is, ByteArrayOutputStream baos) throws IOException {
        byte[] buf = new byte[BYTE_BUFFER_SIZE];
        int read;
        while ((read = is.read(buf)) > 0) {
            baos.write(buf, 0, read);
        }
    }
    
    public static InputStream toInputStream(Content content) throws IOException {
        if (content instanceof org.apache.james.mailbox.InputStreamContent) {
            return ((org.apache.james.mailbox.InputStreamContent) content).getInputStream();
        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            content.writeTo(Channels.newChannel(out));
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
