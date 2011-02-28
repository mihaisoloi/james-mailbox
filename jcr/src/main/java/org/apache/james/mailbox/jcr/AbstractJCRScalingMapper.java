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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.james.mailbox.MailboxSession;
import org.slf4j.Logger;

/**
 * Abstract base class for Mapper's which support scaling 
 *
 */
public abstract class AbstractJCRScalingMapper extends AbstractJCRMapper{

    private final int scaling;
    private final static char PAD ='_';
    
    public AbstractJCRScalingMapper(MailboxSessionJCRRepository repository, MailboxSession mSession, int scaling, Logger logger) {
        super(repository, mSession, logger);
        this.scaling = scaling;
    }

    /**
     * Construct the user node path part, using the specified scaling factor.
     * If the username is not long enough it will fill it with {@link #PAD}
     * 
     * So if you use a scaling of 2 it will look like:
     * 
     * foo:
     *     f/fo/foo
     *     
     * fo:
     *     f/fo/fo
     * 
     * f: 
     *    f/f_/f
     * 
     * @param username
     * @return path
     */
    protected String constructUserPathPart(String username) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0 ; i < scaling; i++) {
            if (username.length() > i) {
                sb.append(username.substring(0,i+1));
            } else {
                sb.append(username);
                int a = i - username.length();
                for (int b = 0; b < a; b++) {
                    sb.append(PAD);
                }
            }
            sb.append(NODE_DELIMITER);
        }
        sb.append(username);
        return sb.toString();

    }

    /**
     * Create the needed Node structure for the given username using the given Node as parent.
     * 
     * The method will use {@link #constructUserPathPart(String)} for create the needed node path and split
     * it when a NODE_DELIMITER was found
     * 
     * @param parent
     * @param username
     * @return userNode
     * @throws RepositoryException
     */
    protected Node createUserPathStructure(Node parent, String username)
            throws RepositoryException {
        String userpath = constructUserPathPart(username);

        String[] userPathParts = userpath.split(NODE_DELIMITER);
        for (int a = 0; a < userPathParts.length; a++) {
            parent = JcrUtils.getOrAddNode(parent, userPathParts[a],
                    "nt:unstructured");

            // thats the last user node so add the right mixin type
            if (a + 1 == userPathParts.length)
                parent.addMixin("jamesMailbox:user");
        }

        return parent;

    }
}
