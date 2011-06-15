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

/**
 * 
 */
package org.apache.james.mailbox.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.apache.james.mailbox.InputStreamContent;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MessageResult;

public final class ResultHeader implements MessageResult.Header, InputStreamContent {
    private final String name;

    private final String value;

    private final long size;


    public ResultHeader(String name, String value) {
        this.name = name;
        this.value = value;
        size = name.length() + value.length() + 2;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MessageResult.Header#getName()
     */
    public String getName() throws MailboxException {
        return name;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MessageResult.Header#getValue()
     */
    public String getValue() throws MailboxException {
        return value;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Content#size()
     */
    public long size() {
        return size;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Content#writeTo(java.nio.channels.WritableByteChannel)
     */
    public void writeTo(WritableByteChannel channel) throws IOException {
        writeAll(channel, ResultUtils.US_ASCII.encode(name));
        ByteBuffer buffer = ByteBuffer
                .wrap(ResultUtils.BYTES_HEADER_FIELD_VALUE_SEP);
        writeAll(channel, buffer);
        writeAll(channel, ResultUtils.US_ASCII.encode(value));
    }

    private void writeAll(WritableByteChannel channel, ByteBuffer buffer)
            throws IOException {
        while (channel.write(buffer) > 0) {
            // write more
        }
    }

    public String toString() {
        return "[HEADER " + name + ": " + value + "]";
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.InputStreamContent#getInputStream()
     */
    public InputStream getInputStream() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTo(Channels.newChannel(out));
        return new ByteArrayInputStream(out.toByteArray());
    }
}