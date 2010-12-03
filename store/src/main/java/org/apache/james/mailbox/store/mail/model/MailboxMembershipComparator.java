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

import java.util.Comparator;


/**
 * UID comparator for mailbox membership.
 */
public final class MailboxMembershipComparator implements Comparator<MailboxMembership<?>> {
    
    public static final MailboxMembershipComparator INSTANCE = new MailboxMembershipComparator();
    
    private MailboxMembershipComparator() {}
    
    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(MailboxMembership<?> o1, MailboxMembership<?> o2) {
        final long uid = o1.getUid();
        final long otherUid = o2.getUid();
        return uid < otherUid ? -1 : uid == otherUid ? 0 : 1;
    }
}