/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.universal.process;

import java.io.*;
import java.lang.management.ManagementFactory;

import com.sun.enterprise.universal.io.*;
import com.sun.enterprise.util.*;

import static com.sun.enterprise.util.StringUtils.ok;

/**
 * Includes a somewhat kludgy way to get the pid for "me".
 * Another casualty of the JDK catering to the LEAST common denominator.
 * Some obscure OS might not have a pid!
 * The name returned from the JMX method is like so:
 * 12345@mycomputername where 12345 is the PID
 * @author bnevins
 */
public final class ProcessUtils {
    private ProcessUtils() {
        // all static class -- no instances allowed!!
    }

    // for informal testing.  Too difficult to make a unit test...
    public static void main(String[] args) {
        debug = true;
        for (String s : args) {
            System.out.println(s + " ===> " + isProcessRunning(Integer.parseInt(s)));
        }
    }

    public static File getExe(String name) {
        for (String path : paths) {
            File f = new File(path + "/" + name);

            if (f.canExecute()) {
                return SmartFile.sanitize(f);
            }
        }
        return null;
    }

    /**
     * Try and find the Process ID of "our" process.
     * @return the process id or -1 if not known
     */
    public static final int getPid() {
        return pid;
    }

    /**
     * Kill the process with the given Process ID.
     * @param pid
     * @return a String if the process was not killed for any reason including if it does not exist.
     *  Return null if it was killed.
     */
    public static String kill(int pid) {
        try {
            String pidString = Integer.toString(pid);
            ProcessManager pm = null;
            String cmdline;

            if (OS.isWindowsForSure()) {
                pm = new ProcessManager("taskkill", "/F", "/T", "/pid", pidString);
                cmdline = "taskkill /F /T /pid " + pidString;
            }
            else {
                pm = new ProcessManager("kill", "-9", "" + pidString);
                cmdline = "kill -9 " + pidString;
            }

            pm.setEcho(false);
            pm.execute();
            int exitValue = pm.getExitValue();

            if (exitValue == 0)
                return null;
            else
                return Strings.get("ProcessUtils.killerror", cmdline,
                        pm.getStderr() + pm.getStdout(), "" + exitValue);
        }
        catch (ProcessManagerException ex) {
            return ex.getMessage();
        }
    }

    /**
     * If we can determine it -- find out if the process that owns the given
     * process id is running.
     * @param aPid
     * @return true if it's running, false if not and null if we don't know.
     * I.e the return value is a true tri-state Boolean.
     */
    public static final Boolean isProcessRunning(int aPid) {
        try {
            if (OS.isWindowsForSure())
                return isProcessRunningWindows(aPid);
            else
                return isProcessRunningUnix(aPid);
        }
        catch (Exception e) {
            return null;
        }
    }
    //////////////////////////////////////////////////////////////////////////
    //////////     all private below     /////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////
    private static final int pid;
    private static final String[] paths;

    private static boolean isProcessRunningWindows(int aPid) throws ProcessManagerException {
        String pidString = Integer.toString(aPid);
        ProcessManager pm = new ProcessManager("tasklist", "/FI", "\"pid eq " + pidString + "\"");
        pm.setEcho(false);
        pm.execute();
        String out = pm.getStdout() + pm.getStderr();

        /* output is either 
        (1) 
        INFO: No tasks running with the specified criteria.
        (2) 
        Image Name                   PID Session Name     Session#    Mem Usage
        ========================= ====== ================ ======== ============
        java.exe                    3760 Console                 0     64,192 K
         */

        if (debug) {
            System.out.println("------------   Output from tasklist   ----------");
            System.out.println(out);
            System.out.println("------------------------------------------------");
        }

        if (ok(out)) {
            if (out.indexOf("" + aPid) >= 0)
                return true;
            else
                return false;
        }

        throw new ProcessManagerException("unknown");
    }

    private static Boolean isProcessRunningUnix(int aPid) throws ProcessManagerException {
        ProcessManager pm = new ProcessManager("kill", "-0", "" + aPid);
        pm.setEcho(false);
        pm.execute();
        int retval = pm.getExitValue();
        return retval == 0 ? Boolean.TRUE : Boolean.FALSE;
    }

    static {
        // variables named with 'temp' are here so that we can legally set the
        // 2 final variables above.

        int tempPid = -1;

        try {
            String pids = ManagementFactory.getRuntimeMXBean().getName();
            int index = -1;

            if (ok(pids) && (index = pids.indexOf('@')) >= 0) {
                tempPid = Integer.parseInt(pids.substring(0, index));
            }
        }
        catch (Exception e) {
            tempPid = -1;
        }
        // final assignment
        pid = tempPid;

        String tempPaths = null;

        if (OS.isWindows()) {
            tempPaths = System.getenv("Path");

            if (!ok(tempPaths))
                tempPaths = System.getenv("PATH"); // give it a try
        }
        else {
            tempPaths = System.getenv("PATH");
        }

        if (ok(tempPaths))
            paths = tempPaths.split(File.pathSeparator);
        else
            paths = new String[0];
    }
    private static boolean debug;
}