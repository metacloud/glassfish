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
package com.sun.enterprise.management.base;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;

import javax.management.ObjectName;
import javax.management.MBeanServerConnection;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.InstanceNotFoundException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.AttributeChangeNotification;

import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.base.NotificationServiceMgr;
import com.sun.appserv.management.base.NotificationService;
import com.sun.appserv.management.helper.NotificationServiceHelper;
import com.sun.appserv.management.base.QueryMgr;
import com.sun.appserv.management.client.ProxyFactory;


import com.sun.enterprise.management.AMXTestBase;
import com.sun.enterprise.management.Capabilities;


/**
 */
public final class NotificationServiceTest extends AMXTestBase
{
		public
	NotificationServiceTest( )
	{
	}
	    public static Capabilities
	getCapabilities()
	{
	    return getOfflineCapableCapabilities( true );
	}

		public NotificationService
	create()
	{
		final NotificationServiceMgr	proxy	= getNotificationServiceMgr();
		
		return( proxy.createNotificationService( "test", 512 ) );
	}

		public void
	testCreate()
		throws Exception
	{
		final NotificationService	proxy	= create();
		
		removeNotificationService( proxy );
	}

		public void
	testGetFromEmpty()
		throws Exception
	{
		final NotificationService	proxy	= create();
		
		assert( proxy.getListeneeSet().size() == 0 );
		final Object	id	= proxy.createBuffer( 10, null);
		final Map<String,Object>	result	= proxy.getBufferNotifications( id, 0 );
		final Notification[]	notifs	= (Notification[])result.get( proxy.NOTIFICATIONS_KEY );
		assertEquals( 0, notifs.length );
	}
	
		private  void
	removeNotificationService( final NotificationService service )
		throws InstanceNotFoundException
	{
		getNotificationServiceMgr().removeNotificationService( service.getName() );
	}


	private static final class MyListener implements NotificationListener
	{
		private final List<Notification>	mReceived;
		
		public MyListener()
		{
			mReceived	= Collections.synchronizedList( new ArrayList<Notification>() );
		}
		
			public void
		handleNotification( final Notification notif, final Object handback )
		{
			mReceived.add( notif );
		}
		
			public int
		getCount()
		{
			return( mReceived.size() );
		}
	}
	
		private static void
	sleep( int duration )
	{
		try
		{
			Thread.sleep( duration );
		}
		catch( InterruptedException e )
		{
		}
	}
	
		public void
	testListen()
		throws Exception
	{
		final NotificationService	proxy	= create();
	
		final QueryMgr	queryMgr	= getQueryMgr();
		final ObjectName	objectName	= Util.getObjectName( queryMgr );
		
		final Object	id	= proxy.createBuffer( 10, null);
		final NotificationServiceHelper	helper	= new NotificationServiceHelper( proxy, id);
		proxy.listenTo( objectName, null );
		assert( proxy.getListeneeSet().size() == 1 );
		assert( Util.getObjectName( (Util.asAMX(proxy.getListeneeSet().iterator().next())) ).equals( objectName ) );
		
		final MyListener	myListener	= new MyListener();
		proxy.addNotificationListener( myListener, null, null );
		 
		final Level	saveLevel = queryMgr.getMBeanLogLevel();
		queryMgr.setMBeanLogLevel( Level.INFO );
		queryMgr.setMBeanLogLevel( saveLevel );
		
		// delivery may be asynchronous; wait until done
		while ( myListener.getCount() < 2 )
		{
			sleep( 20 );
		}
		assert( myListener.getCount() == 2 );
		
		Notification[]	notifs	= helper.getNotifications();
		
		assertEquals( 2, notifs.length );
		assert( notifs[ 0 ].getType().equals( AttributeChangeNotification.ATTRIBUTE_CHANGE ) );
		assert( notifs[ 1 ].getType().equals( AttributeChangeNotification.ATTRIBUTE_CHANGE ) );
		notifs	= helper.getNotifications();
		assert( notifs.length == 0 );
		
		
		proxy.dontListenTo( objectName );
		assert( proxy.getListeneeSet().size() == 0 );
		
		removeNotificationService( proxy );
	}

}

