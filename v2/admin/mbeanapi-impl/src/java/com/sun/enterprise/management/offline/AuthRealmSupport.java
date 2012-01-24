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
package com.sun.enterprise.management.offline;

import com.sun.enterprise.admin.config.MBeanConfigException;

import com.sun.appserv.management.util.misc.StringUtil;

/**
    Support for working with our default file realm.
 */
final class AuthRealmSupport
{
    private final AuthRealmMBeanX            mAuthRealmMBean;
    private final AuthRealmConfigBeanHelper  mHelper;
    
    public final String    PREFIX  = "${com.sun.aas.instanceRoot}";
        
        public
    AuthRealmSupport( final AuthRealmConfigBeanHelper  helper )
    {
        mHelper = helper;
        
        String  file    = helper.getFile();
        
        if ( file.startsWith( PREFIX ) )
        {
            throw new IllegalArgumentException(
                "AuthRealm does not yet support ${...} values for the filename" );
        }
        
        mAuthRealmMBean = new AuthRealmMBeanX( file );
    }
	    protected void
	sdebug( Object o )
	{
	    System.out.println( "" + o );
	}
	
	    private String
	getRealmName()
	{
	    try
	    {
	        return (String)mHelper.getAttribute( "Name" );
	    }
	    catch( Exception e )
	    {
	        return null;
	    }
	}
	
        public String[]
    getGroupNames()
    {
        try
        {
            return mAuthRealmMBean.getGroupNames();
        }
        catch( MBeanConfigException e )
        {
            throw new RuntimeException( "" + e );
        }
    }
    
        public String[]
    getUserNames()
    {
        //sdebug( "AuthRealmSupport.getUserNames(): " + getRealmName() );
        
        try
        {
            final String[]  userNames   = mAuthRealmMBean.getUserNames();
            
            //sdebug( "AuthRealmSupport.getUserNames(): " + 
                //getRealmName() + ": " + StringUtil.toString( userNames ) );
            return userNames;
        }
        catch( MBeanConfigException e )
        {
            //sdebug( e.getMessage() );
            //e.printStackTrace();
            throw new RuntimeException( "" + e );
        }
    }
    
        public String[]
    getUserGroupNames( final String user )
    {
        try
        {
            return mAuthRealmMBean.getUserGroupNames( user );
        }
        catch( MBeanConfigException e )
        {
            throw new RuntimeException( "" + e );
        }
    }
    
        public void
    addUser(
        final String    user,
        final String    password,
        final String[]  groupList)
    {
        try
        {
            mAuthRealmMBean.addUser( user, password, groupList );
        }
        catch( MBeanConfigException e )
        {
            throw new RuntimeException( "" + e );
        }
    }
    
        public void
    updateUser(
        final String    user,
        final String    password,
        final String[]  groupList)
    {
        try
        {
            mAuthRealmMBean.updateUser( user, password, groupList );
        }
        catch( MBeanConfigException e )
        {
            throw new RuntimeException( "" + e );
        }
    }
    
        public void
    removeUser( final String    user)
    {
        try
        {
            mAuthRealmMBean.removeUser( user );
        }
        catch( MBeanConfigException e )
        {
            throw new RuntimeException( "" + e );
        }
    }
}
















