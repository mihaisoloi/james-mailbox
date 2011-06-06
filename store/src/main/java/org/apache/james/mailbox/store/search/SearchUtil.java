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

import java.nio.charset.Charset;

import org.apache.james.mime4j.codec.DecoderUtil;
import org.apache.james.mime4j.util.MimeUtil;

public class SearchUtil {

    private final static String FWD_PARENS = "(fwd)";
    private final static String SUBJ_FWD_HDR = "[fwd:";
    private final static String SUBJ_FWD_TRL = "]";
    private final static String RE = "re";
    private final static String FWD = "FWD";
    private final static String FW = "fw";
    private final static char WS = ' ';
    private final static char OPEN_SQUARE_BRACKED = '[';
    private final static char CLOSE_SQUARE_BRACKED = ']';
    private final static char COLON = ':';
    
    private final static Charset UTF8 = Charset.forName("UTF8");

    /**
     * Extract the base subject from the given subject. 
     * 
     * See rfc5256 2.1 Base Subject
     * 
     * Subject sorting and threading use the "base subject", which has
     * specific subject artifacts removed.  Due to the complexity of these
     * artifacts, the formal syntax for the subject extraction rules is
     * ambiguous.  The following procedure is followed to determine the
     * "base subject", using the [ABNF] formal syntax rules described in
     * section 5:
     * <p>
     *    (1) Convert any RFC 2047 encoded-words in the subject to [UTF-8]
     *        as described in "Internationalization Considerations".
     *        Convert all tabs and continuations to space.  Convert all
     *        multiple spaces to a single space.
     * </p>
     * <p>
     *    (2) Remove all trailing text of the subject that matches the
     *        subj-trailer ABNF; repeat until no more matches are possible.
     * </p>
     * <p>
     *    (3) Remove all prefix text of the subject that matches the subj-
     *        leader ABNF.
     * </p>
     * <p>
     *    (4) If there is prefix text of the subject that matches the subj-
     *        blob ABNF, and removing that prefix leaves a non-empty subj-
     *        base, then remove the prefix text.
     * </p>
     * <p>
     *    (5) Repeat (3) and (4) until no matches remain.
     * </p>
     * Note: It is possible to defer step (2) until step (6), but this
     * requires checking for subj-trailer in step (4).
     * <br>
     * <p>
     *    (6) If the resulting text begins with the subj-fwd-hdr ABNF and
     *        ends with the subj-fwd-trl ABNF, remove the subj-fwd-hdr and
     *        subj-fwd-trl and repeat from step (2).
     * </p>
     * <p>
     *    (7) The resulting text is the "base subject" used in the SORT.
     * </p>
     *
     *
     * @param subject
     * @return baseSubject
     */
    public static String getBaseSubject(String subject) {
            
            //   (1) Convert any RFC 2047 encoded-words in the subject to [UTF-8]
            //    as described in "Internationalization Considerations".
            //    Convert all tabs and continuations to space.  Convert all
            //    multiple spaces to a single space.
            String decodedSubject = MimeUtil.unfold(DecoderUtil.decodeEncodedWords(subject));
            decodedSubject = new String(decodedSubject.getBytes(UTF8), UTF8);

            // replace all tabs with spaces and replace multiple spaces with one space
            decodedSubject = decodedSubject.replaceAll("\t", " ").replaceAll("( ){2,}", " ");
            
            
            while (true) {
                int decodedSubjectLength = decodedSubject.length();
                while (true) {
                    //    (2) Remove all trailing text of the subject that matches the
                    //    subj-trailer ABNF; repeat until no more matches are possible.
                    String subj = removeSubTrailers(decodedSubject);
                    if (decodedSubjectLength > subj.length()) {
                        decodedSubject = subj;
                        decodedSubjectLength = decodedSubject.length();
                    } else {
                        break;
                    }

                }
                
                while (true) {
                    boolean matchedInner = false;

                    //    (3) Remove all prefix text of the subject that matches the subj-
                    //    leader ABNF.
                    decodedSubjectLength = decodedSubject.length();
                    decodedSubject = removeSubjLeaders(decodedSubject);
                    if (decodedSubjectLength > decodedSubject.length()) {
                        matchedInner = true;
                        decodedSubjectLength = decodedSubject.length();

                    }

                    //    (4) If there is prefix text of the subject that matches the subj-
                    //    blob ABNF, and removing that prefix leaves a non-empty subj-
                    //    base, then remove the prefix text.
                    decodedSubjectLength = decodedSubject.length();
                    String subj = removeBlob(decodedSubject);
                    
                    // check if it will leave a non-empty subject
                    if (subj.length() > 0) {
                        decodedSubject = subj;
                        if (decodedSubjectLength > decodedSubject.length()) {
                            matchedInner = true;
                            decodedSubjectLength = decodedSubject.length();

                        }

                    }
                    // (5) Repeat (3) and (4) until no matches remain.
                    if (!matchedInner) {
                        // no more matches so break the loop 
                        break;
                    } 
                }
                
                if (decodedSubject.startsWith(SUBJ_FWD_HDR) && decodedSubject.endsWith(SUBJ_FWD_TRL)) {
                    //    (6) If the resulting text begins with the subj-fwd-hdr ABNF and
                    //    ends with the subj-fwd-trl ABNF, remove the subj-fwd-hdr and
                    //    subj-fwd-trl and repeat from step (2).
                    decodedSubject = decodedSubject.substring(SUBJ_FWD_HDR.length(), decodedSubject.length() - SUBJ_FWD_TRL.length());
                    decodedSubjectLength = decodedSubject.length();

                } else {
                    break;
                }
               
            }
            // (7) The resulting text is the "base subject" used in the SORT.
            return decodedSubject;
    }
 
    private static String removeSubjectBlob(String subject) {
        String subj = subject;
        while(subj.charAt(0) == OPEN_SQUARE_BRACKED) {
            int length = subj.length();
            subj = removeBlob(subject);
            int i = 0;
            if (subj.charAt(i) == CLOSE_SQUARE_BRACKED) {
                i++;
            } else {
                return subject;
            }
            while (subj.charAt(i) == WS) {
                i++;
            }
            subj = subj.substring(i);
            System.out.println(subj);

            if (length == subj.length()) {
                return subj;
            }
        }
        return subj;
    }
    private static String removeSubjLeaders(String subject) {
        
        // subj-leader     = (*subj-blob subj-refwd) / WSP
        // subj-blob       = "[" *BLOBCHAR "]" *WSP
        // subj-refwd      = ("re" / ("fw" ["d"])) *WSP [subj-blob] ":"
        //
        // BLOBCHAR        = %x01-5a / %x5c / %x5e-7f
        //                ; any CHAR except '[' and ']' */
        
        String subj = removeSubjectBlob(subject);
        
        
        int subString = 0;
        if (subj.startsWith(RE)) {
            subString = RE.length();
        } else if (subj.startsWith(FWD)) {
            subString = FWD.length();
        } else if (subj.startsWith(FW)) {
            subString  = FW.length();
        } else {
            return subject;
        } 
        while(subj.charAt(subString) == WS) {
            subString++;
        }
        subj = removeSubjectBlob(subj.substring(subString));
        if (subj.endsWith(String.valueOf(CLOSE_SQUARE_BRACKED))) {
            subString = 1;
        } else {
            subString = 0;
        }

        if (subj.charAt(subString) == COLON) {
            subString++;
        } else {
            return subject;
        }
        
        return subj.substring(subString);
    }
    
    private static String removeSubTrailers(String decodedSubject) {
        int subStringStart = 0;
        int subStringEnd = decodedSubject.length();
        
        // remove the remove_subj_trailers
        //
        // subj-trailer    = "(fwd)" / WSP
        int originalSize = decodedSubject.length();
        int curPos = originalSize -1;
        while(true) {
            char c = decodedSubject.charAt(curPos--);
            if (c == WS) {
                subStringEnd--;
            } else {
                if (subStringEnd > FWD_PARENS.length() && decodedSubject.endsWith(FWD_PARENS)) {
                    subStringEnd -= FWD_PARENS.length();
                } 
                break;
            }
        }
        decodedSubject = decodedSubject.substring(subStringStart, subStringEnd);
        return decodedSubject;
    }
    
    private static String removeBlob(String subject) {
        int i = 0;
        char lastChar = Character.UNASSIGNED;
        for (int a = 0; a < subject.length(); a++) {
            char c = subject.charAt(a);
            lastChar = c;
            if (( a != 0  && c == OPEN_SQUARE_BRACKED) || c == CLOSE_SQUARE_BRACKED) {
                break;
            }
            i++;
        }

        if (lastChar != CLOSE_SQUARE_BRACKED) {
            return subject;
        } else {
            return subject.substring(i);
        }

    }


}
