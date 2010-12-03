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

import javax.mail.Flags;

import org.apache.james.mailbox.maildir.MaildirMessageName;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMembership;
import org.apache.james.mailbox.store.mail.model.Message;

public abstract class AbstractMaildirMessage implements Message, MailboxMembership<Integer>{

    

    protected boolean answered;
    protected boolean deleted;
    protected boolean draft;
    protected boolean flagged;
    protected boolean recent;
    protected boolean seen;
    private Mailbox<Integer> mailbox;
    private long uid;
    protected boolean newMessage;

    public AbstractMaildirMessage(Mailbox<Integer> mailbox) {
        this.mailbox = mailbox;
    }
    
    public Integer getMailboxId() {
        return mailbox.getMailboxId();
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.model.MailboxMembership#setFlags(
     * javax.mail.Flags)
     */
    public void setFlags(Flags flags) {
        if (flags != null) {
            answered = flags.contains(Flags.Flag.ANSWERED);
            deleted = flags.contains(Flags.Flag.DELETED);
            draft = flags.contains(Flags.Flag.DRAFT);
            flagged = flags.contains(Flags.Flag.FLAGGED);
            recent = flags.contains(Flags.Flag.RECENT);
            seen = flags.contains(Flags.Flag.SEEN);
        }
    }
    

    public Message getMessage() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.model.MailboxMembership#isAnswered()
     */
    public boolean isAnswered() {
        return answered;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.model.MailboxMembership#isDeleted()
     */
    public boolean isDeleted() {
        return deleted;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.model.MailboxMembership#isDraft()
     */
    public boolean isDraft() {
        return draft;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.model.MailboxMembership#isFlagged()
     */
    public boolean isFlagged() {
        return flagged;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.model.MailboxMembership#isRecent()
     */
    public boolean isRecent() {
        return recent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#isSeen()
     */
    public boolean isSeen() {
        return seen;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.mailbox.store.mail.model.MailboxMembership#unsetRecent()
     */
    public void unsetRecent() {
        recent = false;
    }

    
    /* 
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.mail.model.MailboxMembership#createFlags()
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
    
    
    /**
     * Indicates whether this MaildirMessage reflects a new message or one that already
     * exists in the file system.
     * @return true if it is new, false if it already exists
     */
    public boolean isNew() {
        return newMessage;
    }
    
    
    @Override
    public String toString() {
        StringBuffer theString = new StringBuffer("MaildirMessage ");
        theString.append(getUid());
        theString.append(" {");
        Flags flags = createFlags();
        if (flags.contains(Flags.Flag.DRAFT))
            theString.append(MaildirMessageName.FLAG_DRAFT);
        if (flags.contains(Flags.Flag.FLAGGED))
            theString.append(MaildirMessageName.FLAG_FLAGGED);
        if (flags.contains(Flags.Flag.ANSWERED))
            theString.append(MaildirMessageName.FLAG_ANSWERD);
        if (flags.contains(Flags.Flag.SEEN))
            theString.append(MaildirMessageName.FLAG_SEEN);
        if (flags.contains(Flags.Flag.DELETED))
            theString.append(MaildirMessageName.FLAG_DELETED);
        theString.append("} ");
        theString.append(getInternalDate());
        return theString.toString();
    }

}
