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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import org.apache.james.mailbox.jpa.mail.model.JPAHeader;
import org.apache.james.mailbox.jpa.mail.model.JPAProperty;
import org.apache.james.mailbox.store.mail.model.AbstractMessage;
import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.Property;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.openjpa.persistence.jdbc.ElementJoinColumn;

/**
 * Abstract base class for JPA based implementations of {@link AbstractMessage}
 *
 */
@MappedSuperclass
public abstract class AbstractJPAMessage extends AbstractMessage {

    @Id
    @GeneratedValue
    @Column(name = "MAIL_ID", nullable = false)
    private long id;
    
    /** The first body octet */
    @Basic(optional=false)
    @Column(name = "MAIL_BODY_START_OCTET", nullable = false)
    private int bodyStartOctet;
    
    /** Number of octets in the full document content */
    @Basic(optional=false)
    @Column(name = "MAIL_CONTENT_OCTETS_COUNT", nullable = false)
    private long contentOctets;
    
    /** MIME media type */
    @Basic(optional=true)
    @Column(name = "MAIL_MIME_TYPE", nullable = true, length = 200)
    private String mediaType;
    
    /** MIME sub type */
    @Basic(optional=true)
    @Column(name = "MAIL_MIME_SUBTYPE", nullable = true, length = 200)
    private String subType;
    
    /** THE CRFL count when this document is textual, null otherwise */
    @Basic(optional=true)
    @Column(name = "MAIL_TEXTUAL_LINE_COUNT", nullable = true)
    private Long textualLineCount;
    
    /** Headers for this message */
    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
    @ElementJoinColumn(name="MAIL_ID")
    private List<JPAHeader> headers;

    /** Meta data for this message */
    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
    @OrderBy("line")
    @ElementJoinColumn(name="MAIL_ID")
    private List<JPAProperty> properties;

    @Deprecated
    public AbstractJPAMessage() {}

    public AbstractJPAMessage(final long contentOctets, final int bodyStartOctet, final List<JPAHeader> headers, final PropertyBuilder propertyBuilder) {
        super();
        this.contentOctets = contentOctets;
        this.bodyStartOctet = bodyStartOctet;
        this.headers = new ArrayList<JPAHeader>(headers);
        this.textualLineCount = propertyBuilder.getTextualLineCount();
        this.mediaType = propertyBuilder.getMediaType();
        this.subType = propertyBuilder.getSubType();
        final List<Property> properties = propertyBuilder.toProperties();
        this.properties = new ArrayList<JPAProperty>(properties.size());
        int order = 0;
        for (final Property property:properties) {
            this.properties.add(new JPAProperty(property, order++));
        }
    }

    /**
     * Create a copy of the given message
     * 
     * @param message
     */
    public AbstractJPAMessage(Message message) {
    	
        this.contentOctets = message.getFullContentOctets();
        this.bodyStartOctet = (int) (message.getFullContentOctets() - message.getBodyOctets());
        this.headers = new ArrayList<JPAHeader>();
        
        List<Header> originalHeaders = message.getHeaders();
        for (int i = 0; i < originalHeaders.size(); i++) {
            headers.add(new JPAHeader(originalHeaders.get(i)));
        }

        PropertyBuilder pBuilder = new PropertyBuilder(message.getProperties());
        this.textualLineCount = message.getTextualLineCount();
        this.mediaType = message.getMediaType();
        this.subType = message.getSubType();
        final List<Property> properties = pBuilder.toProperties();
        this.properties = new ArrayList<JPAProperty>(properties.size());
        int order = 0;
        for (final Property property:properties) {
            this.properties.add(new JPAProperty(property, order++));
        }

    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.Message#getHeaders()
     */
    public List<Header> getHeaders() {
        return new ArrayList<Header>(headers);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (id ^ (id >>> 32));
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
        final AbstractJPAMessage other = (AbstractJPAMessage) obj;
        if (id != other.id)
            return false;
        return true;
    }

    public String toString() {
        final String retValue = 
            "message("
            + "id = " + id
            + " )";
        return retValue;
    }

    /**
     * Gets the top level MIME content media type.
     * 
     * @return top level MIME content media type, or null if default
     */
    public String getMediaType() {
        return mediaType;
    }
    
    /**
     * Gets the MIME content subtype.
     * 
     * @return the MIME content subtype, or null if default
     */
    public String getSubType() {
        return subType;
    }

    /**
     * Gets a read-only list of meta-data properties.
     * For properties with multiple values, this list will contain
     * several enteries with the same namespace and local name.
     * @return unmodifiable list of meta-data, not null
     */
    public List<Property> getProperties() {
        return new ArrayList<Property>(properties);
    }
    
    /**
     * Gets the number of CRLF in a textual document.
     * @return CRLF count when document is textual,
     * null otherwise
     */
    public Long getTextualLineCount() {
        return textualLineCount;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.Document#getFullContentOctets()
     */
    public long getFullContentOctets() {
        return contentOctets;
    }

    @Override
    protected int getBodyStartOctet() {
        return bodyStartOctet;
    }

}
