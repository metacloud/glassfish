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
package com.sun.enterprise.diagnostics;

import java.util.List;
import java.util.zip.ZipFile;
import com.sun.enterprise.diagnostics.DiagnosticException;
import com.sun.enterprise.diagnostics.ReportConfig;
import com.sun.enterprise.diagnostics.Data;
import com.sun.enterprise.diagnostics.collect.Collector;
import com.sun.enterprise.diagnostics.report.html.HTMLReportWriter;

/**
 * Collects data, generates HTML report and archives it if mode is local
 * @author mu125243
 */
public class ReportGenerator {

    protected ReportConfig config;
    protected Collector harvester;
    protected HTMLReportWriter reportWriter;

    /** Creates a new instance of ReportGenerator */
    public ReportGenerator(ReportConfig config, Collector harvester, 
            HTMLReportWriter reportWriter) {
        this.config = config;
        this.harvester = harvester;
        this.reportWriter = reportWriter;
    }
    
    /** Captures data, writes HTML report and archives it in local mode
     * @return Data object
     */
    public java.util.zip.ZipFile generate() 
    throws DiagnosticException {
        Data data = collectData();
        writeReportSummary(data);
        return archive(config.getTarget(), config.getCLIOptions().getReportFile());
    }
    
    private Data collectData() throws DiagnosticException {
        if(harvester != null) {
            ((com.sun.enterprise.diagnostics.collect.Harvester)harvester).initialize();
            return harvester.capture();
        }
        throw new DiagnosticException("Harvester null");
    }
    
    protected void writeReportSummary(Data data) 
    throws DiagnosticException {
        if(reportWriter != null) 
            reportWriter.writeReportSummary(data);
        else
            throw new DiagnosticException("HTML Report writer == null");
    }
    
    protected ZipFile archive(ReportTarget target, String archiveName)
    throws DiagnosticException {
        return new Archiver().archive(config.getTarget(), archiveName);
    }
 }