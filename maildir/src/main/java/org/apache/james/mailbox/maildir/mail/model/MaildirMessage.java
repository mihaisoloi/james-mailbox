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
package org.apache.james.mailbox.maildir.mail.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.store.ResultUtils;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Property;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.james.mailbox.store.streaming.LazySkippingInputStream;

public class MaildirMessage extends AbstractMaildirMessage {

    // Document
    private int bodyStartOctet;
    private InputStream rawFullContent;
    private String mediaType;
    private List<MaildirProperty> properties;
    private String subType;
    private Long textualLineCount;
    
    // MailboxMembership
    private Date internalDate;
    private long size;
    
    private boolean modified = false;
    
    /**
     * This constructor is called when appending a new message.
     * @param mailbox
     * @param internalDate
     * @param size
     * @param flags
     * @param documentIn
     * @param bodyStartOctet
     * @param maildirHeaders
     * @param propertyBuilder
     */
    public MaildirMessage(Mailbox<Integer> mailbox, Date internalDate,
            int size, Flags flags, InputStream header, InputStream body, int bodyStartOctet, PropertyBuilder propertyBuilder) {
        super(mailbox);
        // Document
        this.rawFullContent = ResultUtils.toInput(header, body);
        this.bodyStartOctet = bodyStartOctet;
        this.textualLineCount = propertyBuilder.getTextualLineCount();
        this.mediaType = propertyBuilder.getMediaType();
        this.subType = propertyBuilder.getSubType();
        final List<Property> properties = propertyBuilder.toProperties();
        this.properties = new ArrayList<MaildirProperty>(properties.size());
        int order = 0;
        for (final Property property:properties) {
            this.properties.add(new MaildirProperty(property, order++));
        }
        // MailboxMembership
        this.internalDate = internalDate;
        this.size = size;
        setFlags(flags);
        // this message is new (this constructor is only used for such)
        this.newMessage = true;
    }
    

    
    /**
     * Create a copy of the given message
     * 
     * @param mailbox
     * @param message The message to copy
     * @throws IOException 
     */
    public MaildirMessage(Mailbox<Integer> mailbox, AbstractMaildirMessage message) throws MailboxException {
        super(mailbox);
        this.internalDate = message.getInternalDate();
        this.size = message.getFullContentOctets();
        this.answered = message.isAnswered();
        this.deleted = message.isDeleted();
        this.draft = message.isDraft();
        this.flagged = message.isFlagged();

        this.seen = message.isSeen();
        
        try {
            this.rawFullContent = new ByteArrayInputStream(IOUtils.toByteArray(ResultUtils.toInput(message)));
        } catch (IOException e) {
            throw new MailboxException("Parsing of message failed",e);
        }
       
        this.bodyStartOctet = (int) (message.getFullContentOctets() - message.getBodyOctets());
      
        PropertyBuilder pBuilder = new PropertyBuilder(message.getProperties());
        this.textualLineCount = message.getTextualLineCount();
        this.mediaType = message.getMediaType();
        this.subType = message.getSubType();
        final List<Property> properties = pBuilder.toProperties();
        this.properties = new ArrayList<MaildirProperty>(properties.size());
        int order = 0;
        for (final Property property:properties) {
            this.properties.add(new MaildirProperty(property, order++));
        }
        // this is a copy and thus new
        newMessage = true;
        // A copy of a message is recent 
        // See MAILBOX-85
        this.recent = true;
    }

  

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Document#getFullContentOctets()
     */
    public long getFullContentOctets() {
        return size;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Message#getHeaderContent()
     */
    public InputStream  getHeaderContent() {
        return new BoundedInputStream(rawFullContent, bodyStartOctet - 2);
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Document#getMediaType()
     */
    public String getMediaType() {
        return mediaType;
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Document#getProperties()
     */
    public List<Property> getProperties() {
        return new ArrayList<Property>(properties);
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Document#getSubType()
     */
    public String getSubType() {
        return subType;
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Document#getTextualLineCount()
     */
    public Long getTextualLineCount() {
        return textualLineCount;
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#getInternalDate()
     */
    public Date getInternalDate() {
        return internalDate;
    }
    
    /**
     * Set the internal date
     * @param date
     */
    public void setInternalDate(Date date) {
        this.internalDate = date;
    }

    @Override
    public void setUid(long uid) {
        modified = true;
        super.setUid(uid);
    }

    @Override
    public void setModSeq(long modSeq) {
        modified = true;
        super.setModSeq(modSeq);
    }

    @Override
    public void setFlags(Flags flags) {
        if (flags != null) {
            modified = true;
       
        }
        super.setFlags(flags);
    }

    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#unsetRecent()
     */
    public void unsetRecent() {
        modified = true;
        super.unsetRecent();
    }
    
    /**
     * Indicates whether this MaildirMessage has been modified since its creation.
     * @return true if modified (flags, recent or uid changed), false otherwise
     */
    public boolean isModified() {
        return modified;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Message#getBodyContent()
     */
    public InputStream getBodyContent() throws IOException {
        return new LazySkippingInputStream(rawFullContent, bodyStartOctet);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.AbstractMessage#getBodyStartOctet()
     */
    protected int getBodyStartOctet() {
        return bodyStartOctet;
    }
   
}
