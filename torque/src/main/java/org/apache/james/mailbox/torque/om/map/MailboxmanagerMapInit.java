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
package org.apache.james.mailbox.torque.om.map;

import org.apache.torque.TorqueException;

/**
 * This is a Torque Generated class that is used to load all database map
 * information at once. This is useful because Torque's default behaviour is to
 * do a "lazy" load of mapping information, e.g. loading it only when it is
 * needed.
 * <p>
 * 
 * @see org.apache.torque.map.DatabaseMap#initialize() DatabaseMap.initialize()
 *
 * @deprecated Torque implementation will get removed in the next release
 */
@Deprecated()
public class MailboxmanagerMapInit {
    public static final void init() throws TorqueException {
        org.apache.james.mailbox.torque.om.MailboxRowPeer
                .getMapBuilder();
        org.apache.james.mailbox.torque.om.MessageRowPeer
                .getMapBuilder();
        org.apache.james.mailbox.torque.om.MessageFlagsPeer
                .getMapBuilder();
        org.apache.james.mailbox.torque.om.MessageHeaderPeer
                .getMapBuilder();
        org.apache.james.mailbox.torque.om.MessageBodyPeer
                .getMapBuilder();
    }
}
