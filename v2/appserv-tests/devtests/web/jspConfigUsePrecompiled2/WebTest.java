/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

import java.io.*;
import java.net.*;
import com.sun.ejte.ccl.reporter.*;

/*
 * Unit test for 6181923 ("Add support for 'use-precompiled' JspServlet param
 * introduced by WS 6.0"):
 *
 * Ensure that a JSP that was precompiled and whose servlet class file has
 * been bundled in a JAR in WEB-INF/lib, may still be accessed even if the JSP
 * file itself is not present in the webapp, provided that usePrecompiled
 * has been set to TRUE.
 */
public class WebTest {

    private static SimpleReporterAdapter stat
        = new SimpleReporterAdapter("appserv-tests");

    private static final String TEST_NAME = "jsp-precompiled-bundled-in-jar";
    private static final String EXPECTED_RESPONSE = "this is a test";

    private String host;
    private String port;
    private String contextRoot;

    public WebTest(String[] args) {
        host = args[0];
        port = args[1];
        contextRoot = args[2];
    }
    
    public static void main(String[] args) {
        stat.addDescription("Unit test for 6273340");
        WebTest webTest = new WebTest(args);
        webTest.doTest();
        stat.printSummary(TEST_NAME);
    }

    public void doTest() {     
        try { 
            invokeJsp();
        } catch (Exception ex) {
            stat.addStatus(TEST_NAME, stat.FAIL);
            ex.printStackTrace();
        }
    }

    public void invokeJsp() throws Exception {

        InputStream is = null;
        BufferedReader input = null;
        try {
            String url = "http://" + host + ":" + port + contextRoot
                         + "/jsps/test.jsp";
            HttpURLConnection conn = (HttpURLConnection)
                (new URL(url)).openConnection();

            int code = conn.getResponseCode();
            if (code != 200) {
                System.err.println("Unexpected return code: " + code);
                stat.addStatus(TEST_NAME, stat.FAIL);
            } else {
                is = conn.getInputStream();
                input = new BufferedReader(new InputStreamReader(is));
                String line = input.readLine();
                if (!EXPECTED_RESPONSE.equals(line)) {
                    System.err.println("Wrong response. "
                                       + "Expected: " + EXPECTED_RESPONSE
                                       + ", received: " + line);
                    stat.addStatus(TEST_NAME, stat.FAIL);
                } else {
                    stat.addStatus(TEST_NAME, stat.PASS);
                }
            }
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException ex) {}
            try {
                if (input != null) input.close();
            } catch (IOException ex) {}
        }
    }
}