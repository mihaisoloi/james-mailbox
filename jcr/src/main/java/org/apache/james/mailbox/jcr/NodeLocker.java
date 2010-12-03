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

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * The NodeLocker takes care of locking a {@link Node} and execute the {@link NodeLockedExecution} on it. The implementations need to take care of prevent a {@link InvalidItemStateException} 
 * in all cases
 *
 */
public interface NodeLocker {

    /**
     * Execute the given {@link NodeLockedExecution} after locking the {@link Node}
     * @param <T> the return type of the method
     * @param exection
     * @param node
     * @param returnType
     * @return result
     * @throws RepositoryException
     * @throws InterruptedException
     */
    public <T>T execute(NodeLockedExecution<T> exection, Node node, Class<T> returnType) throws RepositoryException, InterruptedException;
    
    /**
     * Execute some code on a Node
     *
     * @param <T>
     */
    public interface NodeLockedExecution<T> {
        
        /**
         * Execute some code on the locked {@link Node}
         * @param node
         * @return result
         * @throws RepositoryException
         */
        public T execute(Node node) throws RepositoryException;
        
        /**
         * Return true if the node needs to be deep locked (all childs too)
         * 
         * @return deepLocked
         */
        public boolean isDeepLocked();
    }
}
