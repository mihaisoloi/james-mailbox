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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.james.mailbox.MailboxException;

/**
 * Indicates that required locks cannot be acquired at this time.
 */
@Deprecated
public class LockException extends MailboxException {

    private static final long serialVersionUID = 698379731076300030L;

    public LockException(String message) {
        super(message);
    }

    public LockException() {
        super();}

    public LockException(Exception cause) {
        super(null, cause);
    }

    public static void tryLock(final Lock lock, final int timeoutInSeconds) throws LockException {
        try {
            if (!lock.tryLock(timeoutInSeconds, TimeUnit.SECONDS)) {
                throw new LockException("Lock failed");
            }
        } catch (InterruptedException e) {
            throw new LockException("Lock failed");
        }
    }

}
