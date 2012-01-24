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
 * $Header: /cvs/glassfish/admin/mbeans/src/java/com/sun/enterprise/admin/mbeans/DottedNameMBeansIniter.java,v 1.3 2005/12/25 03:42:19 tcfujii Exp $
 * $Revision: 1.3 $
 * $Date: 2005/12/25 03:42:19 $
 */
package com.sun.enterprise.admin.mbeans;
 

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ObjectInstance;

import com.sun.enterprise.admin.common.ObjectNames;
import com.sun.enterprise.admin.util.ClassUtil;

import com.sun.enterprise.admin.dottedname.DottedNameRegistry;
import com.sun.enterprise.admin.dottedname.DottedNameGetSetMBean;
import com.sun.enterprise.admin.server.core.AdminService;
import com.sun.enterprise.admin.AdminContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/*
	A registry for DottedName-to-ObjectName mapping.
 */
public class DottedNameMBeansIniter
{
		public
	DottedNameMBeansIniter( MBeanServer server )
		throws Exception
	{
		init( server );
	}
	
	
		public void
	init( MBeanServer server )
		throws Exception
	{
		// config registry
		final DottedNameRegistry registry	= new DottedNameRegistryMBeanImpl();
		server.registerMBean( registry, ObjectNames.getDottedNameRegistryObjectName() );
		
		// monitoring registry
		final DottedNameRegistry monitoringRegistry	= new DottedNameRegistryMBeanImpl();
		server.registerMBean( monitoringRegistry, ObjectNames.getDottedNameMonitoringRegistryObjectName() );
		
		
		/*	dotted name get-set
			probably would be cleaner to generate proxies for registry & monitoringRegistry, but
			that would only matter if these objects were unregistered an re-registered (unlikely).
			
			It is also more efficient to just call them directly.
		 */
		server.registerMBean(
                getDottedNameGetSetMBean(server, registry, monitoringRegistry),
			    ObjectNames.getDottedNameGetSetObjectName() );
	}

    private DottedNameGetSetMBean getDottedNameGetSetMBean(
                                    final MBeanServer server,
                                    final DottedNameRegistry registry,
                                    final DottedNameRegistry monitoringRegistry)
                                    throws Exception
    {
        final String dnClassName= AdminService.getAdminService().getAdminContext().getDottedNameMBeanImplClassName();
        final Class dnClass = Class.forName(dnClassName);
        final Class[] types = new Class[]{javax.management.MBeanServerConnection.class,
                                          com.sun.enterprise.admin.dottedname.DottedNameRegistry.class,
                                          com.sun.enterprise.admin.dottedname.DottedNameRegistry.class};
        final Constructor dnConstructor = dnClass.getConstructor(types);
        final Object[] params = new Object[]{server, registry, monitoringRegistry}; 
        final DottedNameGetSetMBean dnMbean = (DottedNameGetSetMBean) dnConstructor.newInstance(params);
        return dnMbean;
    }
}
