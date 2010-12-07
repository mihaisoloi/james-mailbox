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
package org.apache.james.mailbox.jpa.mail;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;

import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.store.mail.CachingUidProvider;
import org.apache.james.mailbox.store.mail.model.Mailbox;

/**
 * Lazy-lookup last used uid via JPA 
 * 
 *
 */
public class JPACachingUidProvider extends CachingUidProvider<Long>{

    private final EntityManagerFactory factory;
    
    public JPACachingUidProvider(EntityManagerFactory factory) {
        this.factory = factory;
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.CachingUidProvider#getLastUid(org.apache.james.mailbox.MailboxSession, org.apache.james.mailbox.store.mail.model.Mailbox)
     */
    protected long getLastUid(MailboxSession session, Mailbox<Long> mailbox) throws MailboxException{
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            final long uid = (Long) em.createNamedQuery("findLastUidInMailbox").setParameter("idParam", mailbox.getMailboxId()).setMaxResults(1).getSingleResult();
            em.getTransaction().commit();
            return uid;
        } catch (NoResultException e) {
            em.getTransaction().rollback();
            return 0;
        } catch (PersistenceException e) {
            em.getTransaction().rollback();
            throw new MailboxException("Unable to retrieve last uid for mailbox " + mailbox);
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

}
