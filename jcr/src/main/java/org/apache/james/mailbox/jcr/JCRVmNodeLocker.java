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

import org.apache.jackrabbit.util.Locked;

/**
 * {@link NodeLocker} implementation which use {@link Locked} utility to make sure the given node will not get modified while
 * excecute the {@link NodeLockedExecution}. 
 * 
 * This implementation will only work with NON-CLUSTERED JCR.
 *
 */
public class JCRVmNodeLocker implements NodeLocker{
    
    @SuppressWarnings("unchecked")
    public <T> T execute(final NodeLockedExecution<T> exection, Node node, Class<T> returnType) throws RepositoryException, InterruptedException {
        T result = (T) new Locked() {

            @Override
            protected Object run(Node node) throws RepositoryException {
                return exection.execute(node);
            }
            
        }.with(node, exection.isDeepLocked());
        return result;
    }



}
