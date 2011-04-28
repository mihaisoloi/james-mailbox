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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.jpa.mail.model.JPAHeader;
import org.apache.james.mailbox.jpa.mail.model.JPAMailbox;
import org.apache.james.mailbox.jpa.mail.model.JPAProperty;
import org.apache.james.mailbox.store.mail.model.AbstractMessage;
import org.apache.james.mailbox.store.mail.model.Header;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.Property;
import org.apache.james.mailbox.store.mail.model.PropertyBuilder;
import org.apache.openjpa.persistence.jdbc.ElementJoinColumn;
import org.apache.openjpa.persistence.jdbc.ElementJoinColumns;
import org.apache.openjpa.persistence.jdbc.Index;

/**
 * Abstract base class for JPA based implementations of {@link AbstractMessage}
 */
@IdClass(AbstractJPAMessage.MailboxIdUidKey.class)
@NamedQueries({
    @NamedQuery(name="findRecentMessagesInMailbox",
            query="SELECT message FROM Message message WHERE membership.mailbox.mailboxId = :idParam AND message.recent = TRUE"),
    @NamedQuery(name="findUnseenMessagesInMailboxOrderByUid",
            query="SELECT message FROM Message message WHERE message.mailbox.mailboxId = :idParam AND message.seen = FALSE ORDER BY message.uid ASC"),
    @NamedQuery(name="findMessagesInMailbox",
            query="SELECT message FROM Message message WHERE message.mailbox.mailboxId = :idParam"),
    @NamedQuery(name="findMessagesInMailboxBetweenUIDs",
            query="SELECT message FROM Message message WHERE message.mailbox.mailboxId = :idParam AND message.uid BETWEEN :fromParam AND :toParam"),        
    @NamedQuery(name="findMessagesInMailboxWithUID",
            query="SELECT membership FROM Message message WHERE message.mailbox.mailboxId = :idParam AND message.uid=:uidParam"),                    
    @NamedQuery(name="findMessagesInMailboxAfterUID",
            query="SELECT message FROM Message message WHERE message.mailbox.mailboxId = :idParam AND message.uid>=:uidParam"),                    
    @NamedQuery(name="findDeletedMessagesInMailbox",
            query="SELECT message.uid FROM Message message WHERE message.mailbox.mailboxId = :idParam AND message.deleted=TRUE"),        
    @NamedQuery(name="findDeletedMessagesInMailboxBetweenUIDs",
            query="SELECT message.uid FROM Message message WHERE message.mailbox.mailboxId = :idParam AND message.uid BETWEEN :fromParam AND :toParam AND message.deleted=TRUE"),        
    @NamedQuery(name="findDeletedMessagesInMailboxWithUID",
            query="SELECT message.uid FROM Message message WHERE membership.mailbox.mailboxId = :idParam AND message.uid=:uidParam AND message.deleted=TRUE"),                    
    @NamedQuery(name="findDeletedMessagesInMailboxAfterUID",
            query="SELECT message.uid FROM Message message WHERE message.mailbox.mailboxId = :idParam AND message.uid>=:uidParam AND message.deleted=TRUE"),          
            
    @NamedQuery(name="deleteDeletedMessagesInMailbox",
            query="DELETE FROM Message message WHERE message.mailbox.mailboxId = :idParam AND message.deleted=TRUE"),        
    @NamedQuery(name="deleteDeletedMessagesInMailboxBetweenUIDs",
            query="DELETE FROM Message message WHERE message.mailbox.mailboxId = :idParam AND message.uid BETWEEN :fromParam AND :toParam AND message.deleted=TRUE"),        
    @NamedQuery(name="deleteDeletedMessagesInMailboxWithUID",
            query="DELETE FROM Message message WHERE message.mailbox.mailboxId = :idParam AND message.uid=:uidParam AND message.deleted=TRUE"),                    
    @NamedQuery(name="deleteDeletedMessagesInMailboxAfterUID",
            query="DELETE FROM Message message WHERE message.mailbox.mailboxId = :idParam AND message.uid>=:uidParam AND message.deleted=TRUE"),  
                    
    @NamedQuery(name="countUnseenMessagesInMailbox",
            query="SELECT COUNT(message) FROM Message message WHERE message.mailbox.mailboxId = :idParam AND message.seen=FALSE"),                     
    @NamedQuery(name="countMessagesInMailbox",
            query="SELECT COUNT(message) FROM Message message WHERE message.mailbox.mailboxId = :idParam"),                    
    @NamedQuery(name="deleteMessages",
            query="DELETE FROM Message message WHERE message.mailbox.mailboxId = :idParam"),
    @NamedQuery(name="findLastUidInMailbox",
            query="SELECT message.uid FROM Message message WHERE message.mailbox.mailboxId = :idParam ORDER BY message.uid DESC"),
    @NamedQuery(name="deleteAllMemberships",
            query="DELETE FROM Message message")
})
@MappedSuperclass
public abstract class AbstractJPAMessage extends AbstractMessage<Long> {

    private static final String TOSTRING_SEPARATOR = " ";

    /** Identifies composite key */
    public static class MailboxIdUidKey implements Serializable {

        private static final long serialVersionUID = 7847632032426660997L;

        public MailboxIdUidKey() {}

        /** The value for the mailbox field */
        public long mailbox;
        
        /** The value for the uid field */
        public long uid;

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + (int) (mailbox ^ (mailbox >>> 32));
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
            final MailboxIdUidKey other = (MailboxIdUidKey) obj;
            if (mailbox != other.mailbox)
                return false;
            if (uid != other.uid)
                return false;
            return true;
        }

    }

    /** The value for the mailboxId field */
    @Id
    @ManyToOne(
            cascade = {
                    CascadeType.PERSIST, 
                    CascadeType.REFRESH, 
                    CascadeType.MERGE}, 
            fetch=FetchType.LAZY)
    @Column(name = "MAILBOX_ID", nullable = true)
    private JPAMailbox mailbox;

    /** The value for the uid field */
    @Id
    @Column(name = "MAIL_UID")
    private long uid;

    /** The value for the internalDate field */
    @Basic(optional = false)
    @Column(name = "MAIL_DATE")
    private Date internalDate;

    /** The value for the answered field */
    @Basic(optional = false)
    @Column(name = "MAIL_IS_ANSWERED", nullable = false)
    private boolean answered = false;

    /** The value for the deleted field */
    @Basic(optional = false)
    @Column(name = "MAIL_IS_DELETED", nullable = false)
    @Index
    private boolean deleted = false;

    /** The value for the draft field */
    @Basic(optional = false)
    @Column(name = "MAIL_IS_DRAFT", nullable = false)
    private boolean draft = false;

    /** The value for the flagged field */
    @Basic(optional = false)
    @Column(name = "MAIL_IS_FLAGGED", nullable = false)
    private boolean flagged = false;

    /** The value for the recent field */
    @Basic(optional = false)
    @Column(name = "MAIL_IS_RECENT", nullable = false)
    @Index
    private boolean recent = false;

    /** The value for the seen field */
    @Basic(optional = false)
    @Column(name = "MAIL_IS_SEEN", nullable = false)
    @Index
    private boolean seen = false;

    
    /** The first body octet */
    @Basic(optional = false)
    @Column(name = "MAIL_BODY_START_OCTET", nullable = false)
    private int bodyStartOctet;
    
    /** Number of octets in the full document content */
    @Basic(optional = false)
    @Column(name = "MAIL_CONTENT_OCTETS_COUNT", nullable = false)
    private long contentOctets;
    
    /** MIME media type */
    @Basic(optional = true)
    @Column(name = "MAIL_MIME_TYPE", nullable = true, length = 200)
    private String mediaType;
    
    /** MIME sub type */
    @Basic(optional = true)
    @Column(name = "MAIL_MIME_SUBTYPE", nullable = true, length = 200)
    private String subType;
    
    /** THE CRFL count when this document is textual, null otherwise */
    @Basic(optional = true)
    @Column(name = "MAIL_TEXTUAL_LINE_COUNT", nullable = true)
    private Long textualLineCount;
    
    /** Headers for this message */
    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
    @OrderBy("lineNumber")
    @ElementJoinColumns({@ElementJoinColumn(name="MAILBOX_ID", referencedColumnName="MAILBOX_ID"),
    @ElementJoinColumn(name="MAIL_UID", referencedColumnName="MAIL_UID")})
    private List<JPAHeader> headers;

    /** Meta data for this message */
    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
    @OrderBy("line")
    @ElementJoinColumns({@ElementJoinColumn(name="MAILBOX_ID", referencedColumnName="MAILBOX_ID"),
                @ElementJoinColumn(name="MAIL_UID", referencedColumnName="MAIL_UID")})
    private List<JPAProperty> properties;

    @Deprecated
    public AbstractJPAMessage() {}

    public AbstractJPAMessage(JPAMailbox mailbox, long uid, Date internalDate, Flags flags, final long contentOctets, final int bodyStartOctet, final List<JPAHeader> headers, final PropertyBuilder propertyBuilder) {
        super();
        this.mailbox = mailbox;
        this.internalDate = internalDate;
        this.uid = uid;
        setFlags(flags);
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
     * Constructs a copy of the given message.
     * All properties are cloned except mailbox and UID.
     * @param mailboxId new mailbox ID
     * @param uid new UID
     * @param original message to be copied, not null
     * @throws IOException 
     */
    public AbstractJPAMessage(JPAMailbox mailbox, long uid, Message<?> original) throws MailboxException {
        super();
        this.mailbox = mailbox;
        this.uid = uid;
        this.internalDate = original.getInternalDate();
        this.answered = original.isAnswered();
        this.deleted = original.isDeleted();
        this.draft = original.isDraft();
        this.flagged = original.isFlagged();
        this.recent = original.isRecent();
        this.seen = original.isSeen();
        

        this.contentOctets = original.getFullContentOctets();
        this.bodyStartOctet = (int) (original.getFullContentOctets() - original.getBodyOctets());
        this.headers = new ArrayList<JPAHeader>();
        
        List<Header> originalHeaders = original.getHeaders();
        for (int i = 0; i < originalHeaders.size(); i++) {
            headers.add(new JPAHeader(originalHeaders.get(i)));
        }

        PropertyBuilder pBuilder = new PropertyBuilder(original.getProperties());
        this.textualLineCount = original.getTextualLineCount();
        this.mediaType = original.getMediaType();
        this.subType = original.getSubType();
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
        result = PRIME * result + (int) (getMailboxId() ^ (getMailboxId() >>> 32));
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
        final AbstractJPAMessage other = (AbstractJPAMessage) obj;
        if (getMailboxId() != null) {
            if (!getMailboxId().equals(other.getMailboxId()))
            return false;
        } else {
            if (other.getMailboxId() != null)
            return false;
        }
        if (uid != other.uid)
            return false;
        return true;
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
    

    /**
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#getInternalDate()
     */
    public Date getInternalDate() {
        return internalDate;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#getMailboxId()
     */
    public Long getMailboxId() {
        return getMailbox().getMailboxId();
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#getUid()
     */
    public long getUid() {
        return uid;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#isAnswered()
     */
    public boolean isAnswered() {
        return answered;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#isDeleted()
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#isDraft()
     */
    public boolean isDraft() {
        return draft;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#isFlagged()
     */
    public boolean isFlagged() {
        return flagged;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#isRecent()
     */
    public boolean isRecent() {
        return recent;
    }

    /**
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#isSeen()
     */
    public boolean isSeen() {
        return seen;
    }
    
    public void setUid(long uid) {
        this.uid = uid;
    }
    
    /**
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#setFlags(javax.mail.Flags)
     */
    public void setFlags(Flags flags) {
        answered = flags.contains(Flags.Flag.ANSWERED);
        deleted = flags.contains(Flags.Flag.DELETED);
        draft = flags.contains(Flags.Flag.DRAFT);
        flagged = flags.contains(Flags.Flag.FLAGGED);
        recent = flags.contains(Flags.Flag.RECENT);
        seen = flags.contains(Flags.Flag.SEEN);
    }

    /**
     * Utility getter on Mailbox.
     */
    public JPAMailbox getMailbox() {
        return mailbox;
    }

    /**
     * Utility setter on Mailbox.
     */
    public void setMailbox(JPAMailbox mailbox) {
        this.mailbox = mailbox;
    }

    public String toString() {
        final String retValue = 
            "message("
            + "mailboxId = " + this.getMailboxId() + TOSTRING_SEPARATOR
            + "uid = " + this.uid + TOSTRING_SEPARATOR
            + "internalDate = " + this.internalDate + TOSTRING_SEPARATOR
            + "answered = " + this.answered + TOSTRING_SEPARATOR
            + "deleted = " + this.deleted + TOSTRING_SEPARATOR
            + "draft = " + this.draft + TOSTRING_SEPARATOR
            + "flagged = " + this.flagged + TOSTRING_SEPARATOR
            + "recent = " + this.recent + TOSTRING_SEPARATOR
            + "seen = " + this.seen + TOSTRING_SEPARATOR
            + " )";
        return retValue;
    }
    

}
