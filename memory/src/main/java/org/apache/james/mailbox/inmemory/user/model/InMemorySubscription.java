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
package org.apache.james.mailbox.inmemory.user.model;

import org.apache.james.mailbox.MailboxSession.User;
import org.apache.james.mailbox.store.user.model.Subscription;

public class InMemorySubscription implements Subscription {

    private final String mailbox;
    private final String user;
    
    public InMemorySubscription(final String mailbox, final User user) {
        super();
        this.mailbox = mailbox;
        this.user = user.getUserName();
    }

    public String getMailbox() {
        return mailbox;
    }

    public String getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((mailbox == null) ? 0 : mailbox.hashCode());
        result = PRIME * result + ((user == null) ? 0 : user.hashCode());
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
        final InMemorySubscription other = (InMemorySubscription) obj;
        if (mailbox == null) {
            if (other.mailbox != null)
                return false;
        } else if (!mailbox.equals(other.mailbox))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        return true;
    }

    /**
     * Representation suitable for logging and debugging.
     * @return a <code>String</code> representation 
     * of this object.
     */
    public String toString()
    {
        return "InMemorySubscription[ "
            + "mailbox = " + this.mailbox + " "
            + "user = " + this.user + " "
            + " ]";
    }
    
}
