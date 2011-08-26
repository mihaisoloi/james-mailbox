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
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.MessageManager;


/**
 * Describes the current state of a mailbox.
 */
public class MailboxMetaData implements MessageManager.MetaData {

    private final long recentCount;
    private final List<Long> recent;
    private final Flags premanentFlags;
    private final long uidValidity;
    private final long nextUid;
    private final long messageCount;
    private final long unseenCount;
    private final Long firstUnseen;
    private final boolean writeable;
    private final long highestModSeq;
    private final boolean modSeqPermanent;
    
    public MailboxMetaData(final List<Long> recent, final Flags premanentFlags, final long uidValidity, final long nextUid, final long highestModSeq, 
            final long messageCount, final long unseenCount, final Long firstUnseen, final boolean writeable, final boolean modSeqPermanent) {
        super();
        if (recent == null) {
            this.recent = new ArrayList<Long>();
        } else {
            this.recent = recent;

        }
        this.highestModSeq = highestModSeq;
        recentCount = this.recent.size();

        this.premanentFlags = premanentFlags;
        this.uidValidity = uidValidity;
        this.nextUid = nextUid;
        this.messageCount = messageCount;
        this.unseenCount = unseenCount;
        this.firstUnseen = firstUnseen;
        this.writeable = writeable;
        this.modSeqPermanent = modSeqPermanent;
    }

    /**
     * @see {@link Mailbox.MetaData#countRecent()()}
     */
    public long countRecent() {
        return recentCount;
    }

    /**
     * @see {@link Mailbox.MetaData#getPermanentFlags()()}
     */
    public Flags getPermanentFlags() {
        return premanentFlags;
    }

    /**
     * @see {@link MessageManager.MetaData#getRecent()}
     */
    public List<Long> getRecent() {
        return recent;
    }

    /**
     * @see {@link MessageManager.MetaData#getUidValidity()}
     */
    public long getUidValidity() {
        return uidValidity;
    }

    /**
     * @see {@link MessageManager.MetaData#getUidNext()}
     */
    public long getUidNext() {
        return nextUid;
    }

    /**
     * @see {@link MessageManager.MetaData#getMessageCount()}
     */
    public long getMessageCount() {
        return messageCount;
    }

    /**
     * @see {@link MessageManager.MetaData#getUnseenCount()}
     */
    public long getUnseenCount() {
        return unseenCount;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox.MetaData#getFirstUnseen()
     */
    public Long getFirstUnseen() {
        return firstUnseen;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.Mailbox.MetaData#isWriteable()
     */
    public boolean isWriteable() {
        return writeable;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MessageManager.MetaData#getHighestModSeq()
     */
    public long getHighestModSeq() {
        return highestModSeq;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.MessageManager.MetaData#isModSeqPermanent()
     */
    public boolean isModSeqPermanent() {
        return modSeqPermanent;
    }
}
