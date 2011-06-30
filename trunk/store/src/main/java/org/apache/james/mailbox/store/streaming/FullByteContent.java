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
package org.apache.james.mailbox.store.streaming;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.List;

import org.apache.james.mailbox.MessageResult;

/**
 * Content which holds the full content, including {@link Header} objets
 *
 */
public final class FullByteContent extends  AbstractFullContent {
    private final ByteBuffer contents;
    private final long size;

    public FullByteContent(final ByteBuffer contents, final List<MessageResult.Header> headers) throws IOException {
        super(headers);
        this.contents = contents;
        this.size = caculateSize();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Content#size()
     */
    public long size() {
        return size;
    }

    @Override
    protected void bodyWriteTo(WritableByteChannel channel) throws IOException {
        contents.rewind();
        writeAll(channel, contents);        
    }

    @Override
    protected long getBodySize() throws IOException {
        return contents.limit();
    }

}
