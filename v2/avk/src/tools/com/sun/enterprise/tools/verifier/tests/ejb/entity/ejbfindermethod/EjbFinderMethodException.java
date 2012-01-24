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
package com.sun.enterprise.tools.verifier.tests.ejb.entity.ejbfindermethod;

import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import java.lang.reflect.*;
import java.util.*;
import com.sun.enterprise.deployment.EjbEntityDescriptor;
import com.sun.enterprise.deployment.EjbCMPEntityDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbUtils;
import com.sun.enterprise.tools.verifier.*;
import java.lang.ClassLoader;
import com.sun.enterprise.tools.verifier.tests.*;

/** 
 * ejbFind<METHOD>(...) methods test.  
 *
 *   EJB class contains all ejbFind<METHOD>(...) methods declared in the bean 
 *   class.  
 *
 *   The signatures of the finder methods must follow the following rules: 
 *
 *     A finder method name must start with the prefix ``ejbFind'' 
 *     (e.g. ejbFindByPrimaryKey, ejbFindLargeAccounts, ejbFindLateShipments). 
 * 
 *     Compatibility Note: EJB 1.0 allowed the finder methods to throw the 
 *     java.rmi.RemoteException to indicate a non-application exception. This 
 *     practice is deprecated in EJB 1.1---an EJB 1.1 compliant enterprise 
 *     bean should throw the javax.ejb.EJBException or another 
 *     java.lang.RuntimeException to indicate non-application exceptions to 
 *     the Container. 
 *
 */
public class EjbFinderMethodException extends EjbTest implements EjbCheck { 


    /** 
     * ejbFind<METHOD>(...) methods test.  
     *
     *   EJB class contains all ejbFind<METHOD>(...) methods declared in the bean 
     *   class.  
     *
     *   The signatures of the finder methods must follow the following rules: 
     *
     *     A finder method name must start with the prefix ``ejbFind'' 
     *     (e.g. ejbFindByPrimaryKey, ejbFindLargeAccounts, ejbFindLateShipments). 
     * 
     *     Compatibility Note: EJB 1.0 allowed the finder methods to throw the 
     *     java.rmi.RemoteException to indicate a non-application exception. This 
     *     practice is deprecated in EJB 1.1---an EJB 1.1 compliant enterprise 
     *     bean should throw the javax.ejb.EJBException or another 
     *     java.lang.RuntimeException to indicate non-application exceptions to 
     *     the Container.
     *  
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbEntityDescriptor) {
	    String persistence =
		((EjbEntityDescriptor)descriptor).getPersistenceType();
	    if (EjbEntityDescriptor.BEAN_PERSISTENCE.equals(persistence)) {

		boolean ejbFindMethodFound = false;
		boolean throwsRemoteException = false;
		boolean throwsFinderException = false;
		boolean isValidFinderException = false;
		boolean oneFailed = false;
		int foundWarning = 0;
		int foundAtLeastOne = 0;
		try {
		    // retrieve the EJB Class Methods
		    Context context = getVerifierContext();
		    ClassLoader jcl = context.getClassLoader();
		    Class EJBClass = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());
                    // start do while loop here....
                    do {
			Method [] ejbFinderMethods = EJBClass.getDeclaredMethods();
    	  
			for (int j = 0; j < ejbFinderMethods.length; ++j) {
			    throwsRemoteException = false;
			    throwsFinderException = false;
			    ejbFindMethodFound = false;
  
			    if (ejbFinderMethods[j].getName().startsWith("ejbFind")) {
				ejbFindMethodFound = true;
				foundAtLeastOne++;
  
				// Compatibility Note: EJB 1.0 allowed the ejbFind<METHOD> to 
				// throw the java.rmi.RemoteException to indicate a non-application
				// exception. This practice is deprecated in EJB 1.1---an EJB 1.1
				// compliant enterprise bean should throw the javax.ejb.EJBException
				// or another RuntimeException to indicate non-application
				// exceptions to the Container (see Section 12.2.2).
				// Note: Treat as a warning to user in this instance.
				Class [] exceptions = ejbFinderMethods[j].getExceptionTypes();
				if (EjbUtils.isValidRemoteException(exceptions)) {
				    throwsRemoteException = true;
				}

				if (EjbUtils.isValidFinderException(exceptions)) {
				    throwsFinderException = true;
				}
  
				if (ejbFindMethodFound && throwsRemoteException == false && throwsFinderException == true) {
				    result.addGoodDetails(smh.getLocalString
							  ("tests.componentNameConstructor",
							   "For [ {0} ]",
							   new Object[] {compName.toString()}));
				    result.addGoodDetails(smh.getLocalString
							  (getClass().getName() + ".debug1",
							   "For EJB Class [ {0} ] Finder Method [ {1} ]",
							   new Object[] {EJBClass.getName(),ejbFinderMethods[j].getName()}));
				    result.addGoodDetails(smh.getLocalString
							  (getClass().getName() + ".passed",
							   "[ {0} ] declares [ {1} ] method, which properly does not throw java.rmi.RemoteException, but throws FinderException",
							   new Object[] {EJBClass.getName(),ejbFinderMethods[j].getName()}));
				} else if (ejbFindMethodFound  && throwsRemoteException == true) {
                                    if (descriptor instanceof EjbCMPEntityDescriptor){
                                        if(((EjbCMPEntityDescriptor) descriptor).getCMPVersion()==EjbCMPEntityDescriptor.CMP_2_x){
                                        
                                            result.addErrorDetails(smh.getLocalString
							       ("tests.componentNameConstructor",
								"For [ {0} ]",
								new Object[] {compName.toString()}));
                                            result.addErrorDetails(smh.getLocalString
							       (getClass().getName() + ".debug1",
								"For EJB Class [ {0} ] Finder method [ {1} ]",
								new Object[] {descriptor.getEjbClassName(),ejbFinderMethods[j].getName()}));
                                            result.addErrorDetails(smh.getLocalString
							       (getClass().getName() + ".error",
								"Error: Compatibility Note:" +
								"\n An [ {0} ] method was found, but" +
								"\n EJB 1.0 allowed the ejbFind<METHOD> method to throw the " +
								"\n java.rmi.RemoteException to indicate a non-application" +
								"\n exception. This practice was deprecated in EJB 1.1" +
								"\n ---an EJB 2.0 compliant enterprise bean must" +
								"\n throw the javax.ejb.EJBException or another " +
								"\n RuntimeException to indicate non-application exceptions" +
								"\n to the Container. ",
								new Object[] {ejbFinderMethods[j].getName()}));
                                            oneFailed = true;
                                        } else {
                                            // warning for 1.1 
                                            foundWarning++;
                                            result.addWarningDetails(smh.getLocalString
								 ("tests.componentNameConstructor",
								  "For [ {0} ]",
								  new Object[] {compName.toString()}));
                                            result.addWarningDetails(smh.getLocalString
								 (getClass().getName() + ".debug1",
								  "For EJB Class [ {0} ] Finder method [ {1} ]",
								  new Object[] {descriptor.getEjbClassName(),ejbFinderMethods[j].getName()}));
                                            result.addWarningDetails(smh.getLocalString
								 (getClass().getName() + ".warning",
								  "Error: Compatibility Note:" +
								  "\n An [ {0} ] method was found, but" +
								  "\n EJB 1.0 allowed the ejbFind<METHOD> method to throw the " +
								  "\n java.rmi.RemoteException to indicate a non-application" +
								  "\n exception. This practice is deprecated in EJB 1.1" +
								  "\n ---an EJB 1.1 compliant enterprise bean should" +
								  "\n throw the javax.ejb.EJBException or another " +
								  "\n RuntimeException to indicate non-application exceptions" +
								  "\n to the Container. ",
								  new Object[] {ejbFinderMethods[j].getName()}));
                                        }
                                    }else{
                                        result.addNaDetails(smh.getLocalString
                                            ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));
			                result.notApplicable(smh.getLocalString
					    (getClass().getName() + ".notApplicable1",
					    "[ {0} ] does not declare any ejbFind<METHOD>(...) methods.",
					    new Object[] {descriptor.getEjbClassName()}));
                                    }
				} else if (ejbFindMethodFound  && throwsFinderException == false) {
				    result.addErrorDetails(smh.getLocalString
							   ("tests.componentNameConstructor",
							    "For [ {0} ]",
							    new Object[] {compName.toString()}));
				    result.addErrorDetails(smh.getLocalString
							   (getClass().getName() + ".debug1",
							    "For EJB Class [ {0} ] Finder method [ {1} ]",
							    new Object[] {descriptor.getEjbClassName(),ejbFinderMethods[j].getName()}));
				    result.addErrorDetails(smh.getLocalString
							   (getClass().getName() + ".error1",
							    "Error: ejbFind[Method] [ {0} ] does not throw FinderException",
							    new Object[] {ejbFinderMethods[j].getName()}));
				    oneFailed = true;
				}			 
			    }
			}
                    } while (((EJBClass = EJBClass.getSuperclass()) != null) && (foundAtLeastOne == 0));
  
		    if (foundAtLeastOne == 0) {
			result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			result.notApplicable(smh.getLocalString
					     (getClass().getName() + ".notApplicable1",
					      "[ {0} ] does not declare any ejbFind<METHOD>(...) methods.",
					      new Object[] {descriptor.getEjbClassName()}));
		    }
  
		} catch (ClassNotFoundException e) {
		    Verifier.debug(e);
		    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.failed(smh.getLocalString
				  (getClass().getName() + ".failedException",
				   "Error: EJB Class [ {1} ] does not exist or is not loadable.",
				   new Object[] {descriptor.getEjbClassName()}));
		    oneFailed = true;
		}
    
		if (oneFailed) {
		    result.setStatus(result.FAILED);
                } else if (foundAtLeastOne == 0) {
                    result.setStatus(result.NOT_APPLICABLE);
		} else if (foundWarning > 0) {
		    result.setStatus(result.WARNING); 
		} else { 
		    result.setStatus(result.PASSED);
		}
  
		return result;

	    } else { //if (CONTAINER_PERSISTENCE.equals(persistence))
		result.addNaDetails(smh.getLocalString
				    ("tests.componentNameConstructor",
				     "For [ {0} ]",
				     new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable2",
				      "Expected [ {0} ] managed persistence, but [ {1} ] bean has [ {2} ] managed persistence.",
				      new Object[] {EjbEntityDescriptor.BEAN_PERSISTENCE,descriptor.getName(),persistence}));
		return result;
	    }  

	} else {
	    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "[ {0} ] expected {1} bean, but called with {2} bean.",
				  new Object[] {getClass(),"Entity","Session"}));
	    return result;
	}
    }
}