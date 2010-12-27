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
package org.apache.james.mailbox.copier;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags.Flag;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mailbox.BadCredentialsException;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.store.streaming.InputStreamContent;
import org.apache.james.mailbox.util.FetchGroupImpl;

/**
 * Implementation of the {@link MailboxCopier} interface.
 * 
 */
public class MailboxCopierImpl implements MailboxCopier {

    /**
     * The logger.
     */
    private Log log = LogFactory.getLog("org.apache.james.mailbox.copier");

    /**
     * The source MailboxManager from which all mailboxes will be read and
     * copied to the destination MailboxManager.
     */
    private MailboxManager srcMailboxManager;

    /**
     * The destination MailboxManager to which all mailboxes read from the
     * source MailboxManager and copied to the destination MailboxManager.
     */
    private MailboxManager dstMailboxManager;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.mailbox.copier.MailboxCopier#copyMailboxes()
     */
    public boolean copyMailboxes() {

        MailboxSession srcMailboxSession;
        MailboxSession dstMailboxSession;

        try {
            srcMailboxSession = srcMailboxManager.createSystemSession("manager", log);
        } catch (BadCredentialsException e) {
            log.error(e.getMessage());
            return false;
        } catch (MailboxException e) {
            log.error(e.getMessage());
            return false;
        }

        srcMailboxManager.startProcessingRequest(srcMailboxSession);

        try {

            List<MailboxPath> mailboxPathList = srcMailboxManager.list(srcMailboxSession);

            for (MailboxPath mailboxPath : mailboxPathList) {

                try {
                    dstMailboxSession = dstMailboxManager.createSystemSession(mailboxPath.getUser(), log);
                } catch (BadCredentialsException e) {
                    log.error(e.getMessage());
                    return false;
                } catch (MailboxException e) {
                    log.error(e.getMessage());
                    return false;
                }

                dstMailboxManager.startProcessingRequest(dstMailboxSession);
                dstMailboxManager.createMailbox(mailboxPath, dstMailboxSession);
                dstMailboxManager.endProcessingRequest(dstMailboxSession);

                MessageManager srcMessageManager = srcMailboxManager.getMailbox(mailboxPath, srcMailboxSession);

                Iterator<MessageResult> messageResultIterator = srcMessageManager.getMessages(MessageRange.all(), FetchGroupImpl.FULL_CONTENT, srcMailboxSession);

                while (messageResultIterator.hasNext()) {

                    MessageResult messageResult = messageResultIterator.next();
                    InputStreamContent content = (InputStreamContent) messageResult.getFullContent();

                    try {
                        dstMailboxSession = dstMailboxManager.createSystemSession(mailboxPath.getUser(), log);
                    } catch (BadCredentialsException e) {
                        log.error(e.getMessage());
                        return false;
                    } catch (MailboxException e) {
                        log.error(e.getMessage());
                        return false;
                    }

                    dstMailboxManager.startProcessingRequest(dstMailboxSession);
                    MessageManager dstMessageManager = dstMailboxManager.getMailbox(mailboxPath, dstMailboxSession);
                    dstMessageManager.appendMessage(content.getInputStream(), messageResult.getInternalDate(), dstMailboxSession, messageResult.getFlags().contains(Flag.RECENT), messageResult.getFlags());
                    dstMailboxManager.endProcessingRequest(dstMailboxSession);

                }

            }

        } catch (MailboxException e) {
            log.error(e.getMessage());
            return false;
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            return false;
        }

        srcMailboxManager.endProcessingRequest(srcMailboxSession);

        try {
            srcMailboxManager.logout(srcMailboxSession, true);
        } catch (MailboxException e) {
            log.error(e.getMessage());
            return false;
        }

        return true;

    }

    /**
     * Setter to inject the srcMailboxManager.
     * 
     * All mailboxes from the srcMailboxManager will be copied to the
     * dstMailboxManager upon copyMaillboxes method call.
     * 
     * @param srcMailboxManager
     */
    public void setSrcMailboxManager(MailboxManager srcMailboxManager) {
        this.srcMailboxManager = srcMailboxManager;
    }

    /**
     * Setter to inject the dstMailboxManager.
     * 
     * All mailboxes from the srcMailboxManager will be copied to the
     * dstMailboxManager upon copyMaillboxes method call.
     * 
     * @param dstMailboxManager
     */
    public void setDstMailboxManager(MailboxManager dstMailboxManager) {
        this.dstMailboxManager = dstMailboxManager;
    }

}
