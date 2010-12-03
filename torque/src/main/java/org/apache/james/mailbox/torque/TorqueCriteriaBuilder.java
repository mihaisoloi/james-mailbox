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

import javax.mail.Flags.Flag;

import org.apache.james.mailbox.torque.om.MessageFlagsPeer;
import org.apache.james.mailbox.torque.om.MessageHeaderPeer;
import org.apache.james.mailbox.torque.om.MessageRowPeer;
import org.apache.torque.util.Criteria;

/**
 * 
 * @deprecated Torque implementation will get removed in the next release
 */
@Deprecated()
class TorqueCriteriaBuilder {

    private final Criteria masterCriteria;

    private boolean headersJoin = false;

    private boolean flagsJoin = false;

    public TorqueCriteriaBuilder() {
        masterCriteria = new Criteria();
    }

    public Criteria getCriteria() {
        return masterCriteria;
    }

    public void andHeaderContains(final String header, final String value) {
        joinHeaders();
        final Criteria.Criterion nameCriterion = masterCriteria
                .getNewCriterion(MessageHeaderPeer.FIELD, header,
                        Criteria.EQUAL);
        if (value != null) {
            final Criteria.Criterion valueCriterion = masterCriteria
                    .getNewCriterion(MessageHeaderPeer.VALUE, value,
                            Criteria.LIKE);
            nameCriterion.and(valueCriterion);
        }
        masterCriteria.add(nameCriterion);
    }

    public void andFlag(Flag flag, boolean value) {
        joinFlags();
        if (Flag.ANSWERED.equals(flag)) {
            masterCriteria.add(MessageFlagsPeer.ANSWERED, value);
        } else if (Flag.DELETED.equals(flag)) {
            masterCriteria.add(MessageFlagsPeer.DELETED, value);
        } else if (Flag.DRAFT.equals(flag)) {
            masterCriteria.add(MessageFlagsPeer.DRAFT, value);
        } else if (Flag.FLAGGED.equals(flag)) {
            masterCriteria.add(MessageFlagsPeer.FLAGGED, value);
        } else if (Flag.RECENT.equals(flag)) {
            masterCriteria.add(MessageFlagsPeer.RECENT, value);
        } else if (Flag.SEEN.equals(flag)) {
            masterCriteria.add(MessageFlagsPeer.SEEN, value);
        }
    }

    public void joinFlags() {
        if (!flagsJoin) {
            flagsJoin = true;
            masterCriteria.addJoin(MessageRowPeer.MAILBOX_ID,
                    MessageFlagsPeer.MAILBOX_ID);
            masterCriteria.addJoin(MessageRowPeer.UID, MessageFlagsPeer.UID);
        }
    }

    public void joinHeaders() {
        if (!headersJoin) {
            headersJoin = true;
            masterCriteria.addJoin(MessageRowPeer.MAILBOX_ID,
                    MessageHeaderPeer.MAILBOX_ID);
            masterCriteria.addJoin(MessageRowPeer.UID, MessageHeaderPeer.UID);
        }
    }
}
