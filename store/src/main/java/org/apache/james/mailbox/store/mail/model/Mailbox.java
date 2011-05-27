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

/**
 * Models long term mailbox data.
 */
public interface Mailbox<Id> {

    /**
     * Gets the unique mailbox ID.
     * @return mailbox id
     */
    public abstract Id getMailboxId();

    /**
     * Gets the current namespace for this mailbox.
     * @return not null
     */
    public abstract String getNamespace();
    
    /**
     * Sets the current namespace for this mailbox.
     * @param name not null
     */
    public abstract void setNamespace(String namespace);

    /**
     * Gets the current user for this mailbox.
     * @return not null
     */
    public abstract String getUser();
    
    /**
     * Sets the current user for this mailbox.
     * @param name not null
     */
    public abstract void setUser(String user);

    /**
     * Gets the current name for this mailbox.
     * @return not null
     */
    public abstract String getName();
    
    /**
     * Sets the current name for this mailbox.
     * @param name not null
     */
    public abstract void setName(String name);

    /**
     * Gets the current UID VALIDITY for this mailbox.
     * @return uid validity
     */
    public abstract long getUidValidity();
    
    /**
     * Return the last known uid for this mailbox which 
     * was stored in a persist way. This does not guaranteer
     * that there is no "higher" uid already allocated
     * 
     * @return lastKnownUid
     */
    public abstract long getLastKnownUid();
    
    /**
     * Return the highest known mod-seq for this mailbox which 
     * was stored in a persist way. This does not guaranteer
     * that there is no "higher" mod-seq already allocated
     * 
     * @return highestKnownModSeq
     */
    public abstract long getHighestKnownModSeq();
}