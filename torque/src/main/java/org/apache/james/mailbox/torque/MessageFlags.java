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

package org.apache.james.mailbox.torque;

import java.util.Collection;
import java.util.Iterator;

import javax.mail.Flags;
import javax.mail.MessagingException;

import org.apache.james.mailbox.MessageResult;

/**
 * Represents the flags for a message.
 * 
 * @deprecated Torque implementation will get removed in the next release
 */
@Deprecated()
public class MessageFlags {

    /**
     * Converts given message results into {@link MessageFlags}.
     * 
     * @param messageResults
     *            <code>Collection</code> of {@link MessageResult}, not null
     * @return <code>MessageFlags</code> array, not null
     * @throws MessagingException
     */
    public static final MessageFlags[] toMessageFlags(Collection messageResults)
            throws MessagingException {
        final int size = messageResults.size();
        final MessageFlags[] results = new MessageFlags[size];
        int i = 0;
        for (final Iterator it = messageResults.iterator(); it.hasNext();) {
            final MessageResult result = (MessageResult) it.next();
            results[i++] = new MessageFlags(result);
        }
        return results;
    }

    private final long uid;

    private Flags flags;

    public MessageFlags(final MessageResult result) throws MessagingException {
        this(result.getUid(), result.getFlags());
    }

    public MessageFlags(final long uid, Flags flags) {
        this.uid = uid;
        this.flags = flags;
    }

    /**
     * Gets the message flags.
     * 
     * @return <code>Flags</code>, not null
     */
    public final Flags getFlags() {
        return flags;
    }

    /**
     * Sets the message flags
     * 
     * @param flags
     *            <code>Flags</code>, not null
     */
    public final void setFlags(Flags flags) {
        this.flags = flags;
    }

    /**
     * Gets the UID for the message.
     * 
     * @return the message UID
     */
    public final long getUid() {
        return uid;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    // @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (uid ^ (uid >>> 32));
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    // @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MessageFlags other = (MessageFlags) obj;
        if (uid != other.uid)
            return false;
        return true;
    }

    /**
     * Represents this object suitable for logging.
     * 
     * @return a <code>String</code> representation of this object.
     */
    // @Override
    public String toString() {
        final String TAB = " ";

        final String retValue = "MessageFlags ( " + "uid = " + this.uid + TAB
                + "flags = " + this.flags + TAB + " )";

        return retValue;
    }

}
