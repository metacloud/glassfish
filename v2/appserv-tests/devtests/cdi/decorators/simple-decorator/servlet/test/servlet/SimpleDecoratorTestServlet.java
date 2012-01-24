/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
package test.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import test.beans.Account;
import test.beans.LargeTransactionDecorator;
import test.beans.Preferred;
import test.beans.RequiresNewTransactionInterceptor;
import test.beans.ShoppingCart;
import test.beans.TestBean;
import test.beans.TransactionInterceptor;

@WebServlet(name = "mytest", urlPatterns = { "/myurl" })
public class SimpleDecoratorTestServlet extends HttpServlet {
    @Inject
    @Preferred
    TestBean tb;
    
    @Inject
    @Preferred
    ShoppingCart sc;
    
    @Inject
    Account testAccount;

    public void service(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {

        PrintWriter writer = res.getWriter();
        writer.write("Hello from Servlet 3.0.");
        String msg = "";

        if (tb == null)
            msg += "Injection of request scoped bean failed";

        tb.m1();
        if (!TransactionInterceptor.aroundInvokeCalled)
            msg += "Business method interceptor aroundInvoke not called";
        tb.m2();
        if (TransactionInterceptor.aroundInvokeInvocationCount != 2)
            msg += "Business method interceptor invocation on method-level "
                    + "interceptor annotation count not expected. "
                    + "expected =2, actual="
                    + TransactionInterceptor.aroundInvokeInvocationCount;
        if (!TransactionInterceptor.errorMessage.trim().equals(""))
            msg += TransactionInterceptor.errorMessage;
        
        if (RequiresNewTransactionInterceptor.aroundInvokeCalled)
            msg += "RequiresNew TransactionInterceptor called when " +
            		"it shouldn't have been called";
        
        TransactionInterceptor.clear();
        //invoke shopping cart bean. This should result in an invocation on
        //the RequiresNewTransactional
        sc.addItem("Test Item");
        if (!RequiresNewTransactionInterceptor.aroundInvokeCalled)
            msg += "Business method interceptor aroundInvoke in requires new " +
            		"transaction interceptor not called";
        if (RequiresNewTransactionInterceptor.aroundInvokeInvocationCount != 1)
            msg += "Business method requires new interceptor invocation on " +
            		"method-level interceptor annotation count not expected. "
                    + "expected =1, actual="
                    + RequiresNewTransactionInterceptor.aroundInvokeInvocationCount;
        if (!RequiresNewTransactionInterceptor.errorMessage.trim().equals(""))
            msg += RequiresNewTransactionInterceptor.errorMessage;
        
        //TransactionInterceptor should not have been called
        if (TransactionInterceptor.aroundInvokeCalled)
            msg += "TranscationInterceptor aroundInvoke called when a requiresnew" +
            		"transaction interceptor should have been called";
        
        //Test decorators
        System.out.println(testAccount.getBalance());
        if (testAccount.getBalance().compareTo(new BigDecimal(100)) != 0)            
                msg += "Decorators:Invalid initial balance";
        
        testAccount.deposit(new BigDecimal(10));
        if (testAccount.getBalance().compareTo(new BigDecimal(115)) != 0) //5 as bonus by the decorator
            msg += "Decorators:Invalid balance after deposit";
            
        
        testAccount.withdraw(new BigDecimal(10));
        if (testAccount.getBalance().compareTo(new BigDecimal(105)) != 0)
            msg += "Decorators:Invalid balance after withdrawal";

        
        if (!LargeTransactionDecorator.depositCalled)
            msg += "deposit method in Decorator not called";
        if (!LargeTransactionDecorator.withDrawCalled)
            msg += "deposit method in Decorator not called";
        
        writer.write(msg + "\n");
    }

}