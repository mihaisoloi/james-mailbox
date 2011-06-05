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
package org.apache.james.mailbox.store.search;

import java.io.UnsupportedEncodingException;

import org.apache.james.mime4j.codec.DecoderUtil;
import org.apache.james.mime4j.util.MimeUtil;

public class SearchUtil {

    private final static String FWD_PARENS = "(fwd)";
    
    /**
     * Extract the base subject from the given subject. 
     * 
     * See rfc5256 2.1 Base Subject
     * 
     * TODO: FIX ME 
     * 
     * @param subject
     * @return baseSubject
     */
    public static String getBaseSubject(String subject) {
        try {
            String decodedSubject = new String(MimeUtil.unfold(DecoderUtil.decodeEncodedWords(subject)).getBytes("UTF-8"), "UTF-8");
            // replace all tabs with spaces and replace multiple spaces with one space
            decodedSubject = decodedSubject.replaceAll("\t", " ").replaceAll("( ){2,}", " ");
            
            while (true) {
                boolean changed = false;
                if (decodedSubject.startsWith(FWD_PARENS)) {
                    decodedSubject = decodedSubject.substring(FWD_PARENS.length(), decodedSubject.length());
                    changed = true;
                }
                if (decodedSubject.startsWith(" ")) {
                    // remove all leading spaces
                    decodedSubject = decodedSubject.replaceAll("^( )+", "");
                    changed = true;
                }
                int length = decodedSubject.length();
                if (!changed) {
                    changed = length != decodedSubject.length();
                }
                
                if(!changed) {
                    break;
                }
            }
            return decodedSubject;
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
}
