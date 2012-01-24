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
 * $Header: /cvs/glassfish/admin/mbeanapi-impl/src/java/com/sun/enterprise/management/support/Delegate.java,v 1.3 2006/08/04 19:40:05 llc Exp $
 * $Revision: 1.3 $
 * $Date: 2006/08/04 19:40:05 $
 */

package com.sun.enterprise.management.support;

import javax.management.ObjectName;
import javax.management.MBeanInfo;
import javax.management.AttributeList;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException;
import javax.management.MBeanException;



/**
	Interface for delegating responsibility for handling Attribute getters/setters
	as well as invoke().
 */
public interface Delegate
{
    /**
        An arbitrary (but hopefully meaningful) identifier
        for this Delegate.
     */
    public String   getID();
    
	public Object getAttribute( String attrName )
		throws	AttributeNotFoundException;

	public AttributeList getAttributes( final String[] attrNames );
	
	public void setAttribute( final Attribute attrName )
		throws	AttributeNotFoundException,
				InvalidAttributeValueException;
	
	public AttributeList	setAttributes( final AttributeList mappedAttrs );
	
	/**
		Return true if the Attribute is supported
	 */
	public boolean	supportsAttribute( String name );
    
    public String getDefaultValue( final String name )
		throws	AttributeNotFoundException;
	
	/**
		Return true if the operation is supported
	 */
	public boolean	supportsOperation( 
		String 		operationName,
		Object[]	args,
		String[]	types  );
	
	/**
		Get the MBeanInfo this delegate wishes to make visible.
	 */
	public MBeanInfo	getMBeanInfo();
	
	/**
		invoke the operation.
	 */
	public Object	invoke(
		String 		operationName,
		Object[]	args,
		String[]	types );
		
	
	public void	setOwner( DelegateOwner owner );
}







