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
package org.apache.james.mailbox.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.Property;

public class SimpleMessage implements Message {
    
    public static final char[] NEW_LINE = { 0x0D, 0x0A };
    
    public byte[] body;
    public byte[] fullContent;
    public List<SimpleHeader> headers;
    public List<SimpleProperty> properties;
    public String subType = null;
    public String mediaType = null;
    public Long textualLineCount = null;

	private int size;

    public SimpleMessage(byte[] body, int size, final List<SimpleHeader> headers) throws Exception {
        super();
        this.body = body;
        this.headers = new ArrayList<SimpleHeader>(headers);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Writer writer = new OutputStreamWriter(baos, "us-ascii");
        for (SimpleHeader header:headers) {
            writer.write(header.getFieldName());
            writer.write(": ");
            writer.write(header.getValue());
            writer.write(NEW_LINE);
        }
        writer.write(NEW_LINE);
        writer.flush();
        baos.write(body);
        baos.flush();
        fullContent = baos.toByteArray();
        this.size = size;
    }
    
    /**
     * Constructs a copy of the given message.
     * All properties are cloned except mailbox and UID.
     * @param mailboxId new mailbox ID
     * @param uid new UID
     * @param original message to be copied, not null
     */
    public SimpleMessage(SimpleMessage original) {
        super();
        this.body = original.body;
        this.fullContent = original.fullContent;
        final List<SimpleHeader> originalHeaders = original.headers;
        if (originalHeaders == null) {
            this.headers = new ArrayList<SimpleHeader>();
        } else {
            this.headers = new ArrayList<SimpleHeader>(originalHeaders.size());
            for (SimpleHeader header:originalHeaders) {
                this.headers.add(new SimpleHeader(header));
            }
        }
    }

    /**
     * @throws IOException 
     * @see org.apache.james.imap.Message.mail.model.Document#getBodyContent()
     */
    public InputStream getBodyContent() throws IOException {
        return new ByteArrayInputStream(body);
    }

    /**
     * Gets the full content (including headers) of the document.
     * @return read only buffer, not null
     * @throws IOException 
     */
    public InputStream getFullContent() throws IOException {
        return new ByteArrayInputStream(fullContent);
    }
    
    /**
     * @see org.apache.james.imap.Message.mail.model.Document#getHeaders()
     */
    public List<Header> getHeaders() {
        return new ArrayList<Header>(headers);
    }

    public long getBodyOctets() {
        return body.length;
    }

    public String getSubType() {
        return subType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public List<Property> getProperties() {
        return new ArrayList<Property>(properties);
    }

    public Long getTextualLineCount() {
        return textualLineCount;
    }

    public long getFullContentOctets() {
        return size;
    }
}
