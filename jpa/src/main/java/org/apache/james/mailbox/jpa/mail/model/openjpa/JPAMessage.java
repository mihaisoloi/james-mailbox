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
package org.apache.james.mailbox.jpa.mail.model.openjpa;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.apache.james.mailbox.jpa.mail.model.JPAHeader;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.james.mailbox.store.streaming.StreamUtils;

@Entity(name="Message")
@Table(name="JAMES_MAIL")
public class JPAMessage extends AbstractJPAMessage {

    /** The value for the body field. Lazy loaded */
    /** We use a max length to represent 1gb data. Thats prolly overkill, but who knows */
    @Basic(optional = false, fetch = FetchType.LAZY)
    @Column(name = "MAIL_BYTES", length = 1048576000)
    @Lob private byte[] content;

    @Deprecated
    public JPAMessage() {}

    public JPAMessage(final InputStream content, final long contentOctets, final int bodyStartOctet, final List<JPAHeader> headers, final PropertyBuilder propertyBuilder) throws IOException {
        super(contentOctets,bodyStartOctet,headers,propertyBuilder);
        this.content = StreamUtils.out(content).toByteArray();
    }

    /**
     * Create a copy of the given message
     * 
     * @param message
     */
    public JPAMessage(Message message) throws IOException{
        super(message);
        this.content = StreamUtils.out(message.getFullContent()).toByteArray();
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Message#getFullContent()
     */
    public InputStream getFullContent() throws IOException {
        return new ByteArrayInputStream(content);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Message#getBodyContent()
     */
    public InputStream getBodyContent() throws IOException {
        return new ByteArrayInputStream(content, getBodyStartOctet(), (int)getFullContentOctets());
    }



}
