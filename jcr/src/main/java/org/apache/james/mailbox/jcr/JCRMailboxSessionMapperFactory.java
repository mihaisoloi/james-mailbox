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
package org.apache.james.mailbox.jcr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.SubscriptionException;
import org.apache.james.mailbox.jcr.mail.JCRMailboxMapper;
import org.apache.james.mailbox.jcr.mail.JCRMessageMapper;
import org.apache.james.mailbox.jcr.user.JCRSubscriptionMapper;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.user.SubscriptionMapper;

/**
 * JCR implementation of a {@link MailboxSessionMapperFactory}
 * 
 *
 */
public class JCRMailboxSessionMapperFactory extends MailboxSessionMapperFactory<String> {

    private final MailboxSessionJCRRepository repository;
    private final Log logger;
    private final static int DEFAULT_SCALING = 2;
    private final int scaling;
    private int messageScaling;

    public JCRMailboxSessionMapperFactory(final MailboxSessionJCRRepository repository) {
        this(repository, DEFAULT_SCALING, JCRMessageMapper.MESSAGE_SCALE_DAY);
    }

    public JCRMailboxSessionMapperFactory(final MailboxSessionJCRRepository repository, final int scaling, final int messageScaling) {
        this.repository = repository;
        this.logger = LogFactory.getLog(JCRMailboxSessionMapperFactory.class);
        this.scaling = scaling;
        this.messageScaling = messageScaling;
    }
    
    @Override
    public MailboxMapper<String> createMailboxMapper(MailboxSession session) throws MailboxException {
        JCRMailboxMapper mapper = new JCRMailboxMapper(repository, session, scaling, logger);
        return mapper;
    }

    @Override
    public MessageMapper<String> createMessageMapper(MailboxSession session) throws MailboxException {
        JCRMessageMapper messageMapper = new JCRMessageMapper(repository, session, logger, messageScaling);
        return messageMapper;
    }

    @Override
    public SubscriptionMapper createSubscriptionMapper(MailboxSession session) throws SubscriptionException {
        JCRSubscriptionMapper mapper = new JCRSubscriptionMapper(repository, session, DEFAULT_SCALING, logger);
        return mapper;
    }
    
    public MailboxSessionJCRRepository getRepository() {
        return repository;
    }

}
