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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.store.mail.model.MailboxMembership;
import org.apache.james.mailbox.store.mail.model.Message;

public class SimpleMailboxMembership implements MailboxMembership<Long> {
    
    private static final String TOSTRING_SEPARATOR = " ";
    
    public long mailboxId;
    public long uid;
    public Date internalDate;
    public int size = 0;
    public boolean recent = false;
    public boolean answered = false;
    public boolean deleted = false;
    public boolean draft = false;
    public boolean flagged = false;
    public boolean seen = false;
    public SimpleMessage message;

    public SimpleMailboxMembership(long mailboxId, long uid, Date internalDate, int size, 
            Flags flags, byte[] body, final List<SimpleHeader> headers) throws Exception {
        super();
        this.mailboxId = mailboxId;
        this.uid = uid;
        this.internalDate = internalDate;
        this.size = size;
        this.message = new SimpleMessage(body, size, new ArrayList<SimpleHeader>(headers));
        setFlags(flags);
    }

    /**
     * Constructs a copy of the given message.
     * All properties are cloned except mailbox and UID.
     * @param mailboxId new mailbox ID
     * @param uid new UID
     * @param original message to be copied, not null
     */
    public SimpleMailboxMembership(long mailboxId, long uid, SimpleMailboxMembership original) {
        super();
        this.mailboxId = mailboxId;
        this.uid = uid;
        this.internalDate = original.getInternalDate();
        this.answered = original.isAnswered();
        this.deleted = original.isDeleted();
        this.draft = original.isDraft();
        this.flagged = original.isFlagged();
        this.recent = original.isRecent();
        this.seen = original.isSeen();
        this.message = new SimpleMessage(original.message);
    }

    /**
     * @see org.apache.james.imap.Message.mail.model.Document#getInternalDate()
     */
    public Date getInternalDate() {
        return internalDate;
    }

    /**
     * @see org.apache.james.imap.Message.mail.model.Document#getMailboxId()
     */
    public Long getMailboxId() {
        return mailboxId;
    }
    
    /**
     * @see org.apache.james.imap.Message.mail.model.Document#getUid()
     */
    public long getUid() {
        return uid;
    }

    /**
     * @see org.apache.james.imap.Message.mail.model.Document#isAnswered()
     */
    public boolean isAnswered() {
        return answered;
    }

    /**
     * @see org.apache.james.imap.Message.mail.model.Document#isDeleted()
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @see org.apache.james.imap.Message.mail.model.Document#isDraft()
     */
    public boolean isDraft() {
        return draft;
    }

    /**
     * @see org.apache.james.imap.Message.mail.model.Document#isFlagged()
     */
    public boolean isFlagged() {
        return flagged;
    }

    /**
     * @see org.apache.james.imap.Message.mail.model.Document#isRecent()
     */
    public boolean isRecent() {
        return recent;
    }

    /**
     * @see org.apache.james.imap.Message.mail.model.Document#isSeen()
     */
    public boolean isSeen() {
        return seen;
    }

    /**
     * @see org.apache.james.imap.Message.mail.model.Document#unsetRecent()
     */
    public void unsetRecent() {
        recent = false;
    }
    
    /**
     * @see org.apache.james.imap.Message.mail.model.Document#setFlags(javax.mail.Flags)
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
     * @see org.apache.james.imap.Message.mail.model.Document#createFlags()
     */
    public Flags createFlags() {
        final Flags flags = new Flags();

        if (isAnswered()) {
            flags.add(Flags.Flag.ANSWERED);
        }
        if (isDeleted()) {
            flags.add(Flags.Flag.DELETED);
        }
        if (isDraft()) {
            flags.add(Flags.Flag.DRAFT);
        }
        if (isFlagged()) {
            flags.add(Flags.Flag.FLAGGED);
        }
        if (isRecent()) {
            flags.add(Flags.Flag.RECENT);
        }
        if (isSeen()) {
            flags.add(Flags.Flag.SEEN);
        }
        return flags;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (mailboxId ^ (mailboxId >>> 32));
        result = PRIME * result + (int) (uid ^ (uid >>> 32));
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MailboxMembership<Long> other = (MailboxMembership<Long>) obj;
        if (mailboxId != other.getMailboxId())
            return false;
        if (uid != other.getUid())
            return false;
        return true;
    }

    public String toString()
    {
        final String retValue = 
            "mailbox("
            + "mailboxId = " + this.mailboxId + TOSTRING_SEPARATOR
            + "uid = " + this.uid + TOSTRING_SEPARATOR
            + "internalDate = " + this.internalDate + TOSTRING_SEPARATOR
            + "size = " + this.size + TOSTRING_SEPARATOR
            + "answered = " + this.answered + TOSTRING_SEPARATOR
            + "deleted = " + this.deleted + TOSTRING_SEPARATOR
            + "draft = " + this.draft + TOSTRING_SEPARATOR
            + "flagged = " + this.flagged + TOSTRING_SEPARATOR
            + "recent = " + this.recent + TOSTRING_SEPARATOR
            + "seen = " + this.seen + TOSTRING_SEPARATOR
            + " )";

        return retValue;
    }

    public Message getMessage() {
        return message;
    }

}
