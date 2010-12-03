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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.List;

import org.apache.james.mailbox.Content;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MessageResult.Header;
import org.apache.james.mailbox.store.ResultUtils;

/**
 * Abstract base class for {@link Content} implementations which hold the headers and 
 * the body a email
 *
 */
public abstract class AbstractFullContent implements Content {


    private List<Header> headers;
    
    public AbstractFullContent(final List<MessageResult.Header> headers) throws IOException {
        this.headers = headers;
    }
    
    protected long caculateSize() throws IOException{
        long result = getBodySize();
        result += 2;
        for (final Iterator<MessageResult.Header> it = headers.iterator(); it.hasNext();) {
            final MessageResult.Header header = it.next();
            if (header != null) {
                result += header.size();
                result += 2;
            }
        }
        return result;
    }
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Content#writeTo(java.nio.channels.WritableByteChannel)
     */
    public final void writeTo(WritableByteChannel channel) throws IOException {
        ByteBuffer newLine = ByteBuffer.wrap(ResultUtils.BYTES_NEW_LINE);
        for (final Iterator<MessageResult.Header> it = headers.iterator(); it.hasNext();) {
            final MessageResult.Header header = it.next();
            if (header != null) {
                header.writeTo(channel);
            }
            newLine.rewind();
            writeAll(channel, newLine);
        }
        newLine.rewind();
        writeAll(channel, newLine);
        bodyWriteTo(channel);
    }

    
    /**
     * Write all 
     * 
     * @param channel
     * @param buffer
     * @throws IOException
     */
    protected void writeAll(WritableByteChannel channel, ByteBuffer buffer)
            throws IOException {
        while (channel.write(buffer) > 0) {
            // write more
        }
    }
    
    /**
     * Return the size of the body
     * 
     * @return size
     * @throws IOException
     */
    protected abstract long getBodySize() throws IOException;
    
    /**
     * Write the body to the channel
     * 
     * @param channel
     * @throws IOException
     */
    protected abstract void bodyWriteTo(WritableByteChannel channel) throws IOException;

    
}
