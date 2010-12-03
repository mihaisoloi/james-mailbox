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
package org.apache.james.mailbox.store.mail.model;

import java.util.Date;

import javax.mail.Flags;

/**
 * Links mailbox to member messages.
 */
public interface MailboxMembership<Id> {

    public abstract Date getInternalDate();

    /**
     * Return the mailbox id of the linked mailbox
     * 
     * @return mailboxId
     */
    public abstract Id getMailboxId();

    /**
     * Return the uid
     * 
     * @return uid
     */
    public abstract long getUid();
    
    /**
     * Return the linked Document
     * 
     * @return document
     */
    public abstract Message getMessage();

    /**
     * Return if it was marked as answered
     * 
     * @return answered
     */
    public abstract boolean isAnswered();

    /**
     * Return if it was mark as deleted
     * 
     * @return deleted
     */
    public abstract boolean isDeleted();

    /**
     * Return if it was mark as draft
     * 
     * @return draft
     */
    public abstract boolean isDraft();

    /**
     * Return if it was flagged
     * 
     * @return flagged
     */
    public abstract boolean isFlagged();

    /**
     * Return if it was marked as recent
     * 
     * @return recent
     */
    public abstract boolean isRecent();

    /**
     * Return if it was marked as seen
     * 
     * @return seen
     */
    public abstract boolean isSeen();

    /**
     * Sets {@link #isRecent()} to false.
     * A message can only be recent once.
     */
    public abstract void unsetRecent();

    /**
     * Set the Flags 
     * 
     * @param flags
     */
    public abstract void setFlags(Flags flags);

    /**
     * Creates a new flags instance populated
     * with the current flag data.
     * 
     * @return new instance, not null
     */
    public abstract Flags createFlags();

}
