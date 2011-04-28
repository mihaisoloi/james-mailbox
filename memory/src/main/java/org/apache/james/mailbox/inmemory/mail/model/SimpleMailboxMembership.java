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

package org.apache.james.mailbox.inmemory.mail.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.store.mail.model.AbstractMessage;
import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.Property;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;

public class SimpleMailboxMembership extends AbstractMessage<Long> {

    private final long uid;
    private final long mailboxId;
    private int size;
    private boolean answered;
    private boolean deleted;
    private boolean draft;
    private boolean flagged;
    private boolean recent;
    private boolean seen;
    private Date internalDate;
    private final String subType;
    private List<Property> properties;
    private final String mediaType;
    private List<Header> headers;
    private Long lineCount;
    private byte[] document;
    private int bodyStartOctet;
    public SimpleMailboxMembership(long mailboxId, long uid, final SimpleMailboxMembership original) {
        this.uid = uid;
        this.mailboxId = mailboxId;
        this.size = original.size;
        this.answered = original.answered;
        this.deleted = original.deleted;
        this.draft = original.draft;
        this.flagged = original.flagged;
        this.recent = original.recent;
        this.seen = original.seen;
        this.internalDate = original.internalDate;
        this.subType  = original.subType;
        this.mediaType = original.mediaType;
        this.properties = original.properties;
        this.headers = original.headers;
        this.lineCount = original.lineCount;
        this.document = original.document;
        this.bodyStartOctet = original.bodyStartOctet;
    }
    
    public SimpleMailboxMembership(Date internalDate, long uid,  int size, int bodyStartOctet, byte[] document, 
            Flags flags, List<Header> headers, PropertyBuilder propertyBuilder, final long mailboxId) {
        this.uid = uid;
        this.document = document;
        
        this.size = size;
        this.bodyStartOctet = bodyStartOctet;
        setFlags(flags);
        lineCount = propertyBuilder.getTextualLineCount();
        this.headers = headers;
        this.internalDate = internalDate;
        this.mailboxId = mailboxId;
        this.properties = propertyBuilder.toProperties();
        this.mediaType = propertyBuilder.getMediaType();
        this.subType = propertyBuilder.getSubType();
    }


    public Date getInternalDate() {
        return internalDate;
    }

    public Long getMailboxId() {
        return mailboxId;
    }

    public int getSize() {
        return size;
    }

    public long getUid() {
        return uid;
    }

    public boolean isAnswered() {
        return answered;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isDraft() {
        return draft;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public boolean isRecent() {
        return recent;
    }

    public boolean isSeen() {
        return seen;
    }

    public synchronized void setFlags(Flags flags) {
        answered = flags.contains(Flags.Flag.ANSWERED);
        deleted = flags.contains(Flags.Flag.DELETED);
        draft = flags.contains(Flags.Flag.DRAFT);
        flagged = flags.contains(Flags.Flag.FLAGGED);
        recent = flags.contains(Flags.Flag.RECENT);
        seen = flags.contains(Flags.Flag.SEEN);
    }

    public void unsetRecent() {
        recent = false;
    }

    public InputStream getBodyContent() throws IOException {
        return new ByteArrayInputStream(document,bodyStartOctet, (int) getFullContentOctets());      
    }

    public InputStream getFullContent() throws IOException {
        return new ByteArrayInputStream(document);
    }

    public long getFullContentOctets() {
        return document.length;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public String getMediaType() {
        return mediaType;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public String getSubType() {
        return subType;
    }

    public Long getTextualLineCount() {
        return lineCount;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (uid ^ (uid >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SimpleMailboxMembership other = (SimpleMailboxMembership) obj;
        if (uid != other.uid)
            return false;
        return true;
    }

    /**
     * Representation suitable for logging and debugging.
     *
     * @return a <code>String</code> representation 
     * of this object.
     */
    public String toString()
    {
        return super.toString() + "["
            + "uid = " + this.uid + " "
            + "mailboxId = " + this.mailboxId + " "
            + "size = " + this.size + " "
            + "answered = " + this.answered + " "
            + "deleted = " + this.deleted + " "
            + "draft = " + this.draft + " "
            + "flagged = " + this.flagged + " "
            + "recent = " + this.recent + " "
            + "seen = " + this.seen + " "
            + "internalDate = " + this.internalDate + " "
            + "subType = " + this.subType + " "
            + "mediaType = " + this.mediaType + " "
            + " ]";
    }

    @Override
    protected int getBodyStartOctet() {
        return bodyStartOctet;
    }
}
