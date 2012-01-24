/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
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

/*
 * HADBCreateDBInfoTest.java
 * JUnit based test
 *
 * Created on May 19, 2004, 2:23 PM
 */

package com.sun.enterprise.ee.admin.hadbmgmt;

import java.util.*;
import java.util.logging.*;
import com.sun.enterprise.config.ConfigContext;
import com.sun.enterprise.config.ConfigException;
import com.sun.enterprise.config.serverbeans.ServerHelper;
import com.sun.enterprise.admin.util.JMXConnectorConfig;
import com.sun.enterprise.config.serverbeans.ClusterHelper;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.io.FileUtils;
import java.io.*;
import java.util.Properties;
import javax.management.MBeanServer;
import junit.framework.*;

/**
 *
 * @author bnevins
 */
public class HADBCreateDBInfoTest extends TestCase
{
	
	public HADBCreateDBInfoTest(java.lang.String testName)
	{
		super(testName);
	}
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite(HADBCreateDBInfoTest.class);
		return suite;
	}
	
	/**
	 * Test of setDeviceSize method, of class com.sun.enterprise.ee.admin.hadbmgmt.HADBCreateDBInfo.
	 */
	public void testSetDeviceSize()
	{
		System.out.println("testSetDeviceSize");
		
		// TODO add your test code below by replacing the default call to fail.
		fail("The test case is empty.");
	}
	
	/**
	 * Test of setProperties method, of class com.sun.enterprise.ee.admin.hadbmgmt.HADBCreateDBInfo.
	 */
	public void testSetProperties()
	{
		System.out.println("testSetProperties");
		
		// TODO add your test code below by replacing the default call to fail.
		fail("The test case is empty.");
	}
	
	/**
	 * Test of getCreateCommands method, of class com.sun.enterprise.ee.admin.hadbmgmt.HADBCreateDBInfo.
	 */
	public void testGetCreateCommands()
	{
		System.out.println("testGetCreateCommands");
		
		// TODO add your test code below by replacing the default call to fail.
		fail("The test case is empty.");
	}
	
	/**
	 * Test of getAgentURLArg method, of class com.sun.enterprise.ee.admin.hadbmgmt.HADBCreateDBInfo.
	 */
	public void testGetAgentURLArg()
	{
		System.out.println("testGetAgentURLArg");
		
		// TODO add your test code below by replacing the default call to fail.
		fail("The test case is empty.");
	}
	
	// TODO add test methods here, they have to start with 'test' name.
	// for example:
	// public void testHello() {}
	
	
}