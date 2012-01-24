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

package com.sun.enterprise.webservice.codegen;

import com.sun.appserv.ClassLoaderUtil;
import java.net.URLClassLoader;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.io.*;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.enterprise.deploy.shared.ModuleType;

import com.sun.enterprise.util.LocalStringManagerImpl;

import com.sun.ejb.codegen.EjbcContext;
import com.sun.ejb.codegen.GeneratorException;

// DOL imports
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.backend.Deployer;
import com.sun.enterprise.deployment.backend.OptionalPkgDependency;
import com.sun.enterprise.deployment.deploy.shared.FileArchive;
import com.sun.enterprise.deployment.Descriptor;
import com.sun.enterprise.deployment.io.JaxrpcMappingDeploymentDescriptorFile;
import com.sun.enterprise.deployment.JaxrpcMappingDescriptor;
import com.sun.enterprise.deployment.JaxrpcMappingDescriptor.Mapping;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.util.ApplicationVisitor;
import com.sun.enterprise.deployment.util.DefaultDOLVisitor;
import com.sun.enterprise.deployment.util.ModuleContentLinker;
import com.sun.enterprise.deployment.util.WebServerInfo;
import com.sun.enterprise.deployment.WebService;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.WebServicesDescriptor;
import com.sun.enterprise.deployment.backend.IASDeploymentException;
import com.sun.enterprise.loader.EJBClassLoader;
import com.sun.enterprise.loader.EJBClassPathUtils;

import com.sun.enterprise.deployment.backend.OptionalPkgDependency;
import com.sun.enterprise.deployment.backend.Deployer;

import javax.xml.namespace.QName;
import javax.xml.rpc.Stub;

// web service impl imports
import com.sun.enterprise.webservice.WsUtil;
import com.sun.enterprise.webservice.WsCompile;

//JAX-RPC SPI
import com.sun.xml.rpc.spi.JaxRpcObjectFactory;
import com.sun.xml.rpc.spi.tools.CompileTool;
import com.sun.xml.rpc.spi.tools.GeneratedFileInfo;
import com.sun.xml.rpc.spi.tools.GeneratorConstants;
import com.sun.xml.rpc.spi.tools.J2EEModelInfo;
import com.sun.xml.rpc.spi.tools.ModelFileModelInfo;
import com.sun.xml.rpc.spi.tools.ModelInfo;
import com.sun.xml.rpc.spi.tools.NamespaceMappingInfo;
import com.sun.xml.rpc.spi.tools.NamespaceMappingRegistryInfo;
import com.sun.xml.rpc.spi.tools.NoMetadataModelInfo;

/**
 * This class is responsible for generating all non portable 
 * jax-rpc artifacts for a single .ear or standalone module.
 *
 * @author  Jerome Dochez
 */
public class JaxRpcRICodegen extends ModuleContentLinker 
    implements JaxRpcCodegenAdapter
{
    
    // our code generation context
    protected EjbcContext context = null;
    
    // list of generated files
    Vector files = new Vector();

    private JaxRpcObjectFactory rpcFactory;
    
    private Logger logger = WsUtil.getDefaultLogger();

    // total number of times wscompile is invoked for the .ear or the
    // standalone module.
    private int wscompileInvocationCount = 0;

    // resources...
    private static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(JaxRpcRICodegen.class);        
    
    private CompileTool wscompileForAccept = null;
    private CompileTool wscompileForWebServices = null;
    
    /** Creates a new instance of JaxRpcRICodegen */
    public JaxRpcRICodegen() {
        rpcFactory = JaxRpcObjectFactory.newInstance();
    }
    
    public void run(EjbcContext context) throws Exception {
        rootLocation_ = new FileArchive();
        rootLocation_.open(context.getSrcDir().getAbsolutePath());
        this.context = context;
        Application application = context.getDescriptor();
        application.visit((ApplicationVisitor) this);
    }
    
    /** 
     * Visits a webs service reference
     */
    public void accept(ServiceReferenceDescriptor serviceRef)  {
        boolean codegenRequired = false;

        URL wsdlOverride = null;
        boolean wsdlOverriden = false;
        boolean jaxwsClient = false;
        super.accept(serviceRef);
        try {
            ClassLoader clr = serviceRef.getBundleDescriptor().getClassLoader();
            Class serviceInterface = clr.loadClass(serviceRef.getServiceInterface());
            if (javax.xml.ws.Service.class.isAssignableFrom(serviceInterface)) {
                jaxwsClient = true;
            }
            
            // Resolve port component links to target endpoint address.
            // We can't assume web service client is running in same VM
            // as endpoint in the intra-app case because of app clients.
            //
            // Also set port-qname based on linked port's qname if not
            // already set.
            for(Iterator ports = serviceRef.getPortsInfo().iterator(); ports.hasNext();) {
                ServiceRefPortInfo portInfo = (ServiceRefPortInfo) ports.next();
                
                if( portInfo.isLinkedToPortComponent() ) {
                    WebServiceEndpoint linkedPortComponent = portInfo.getPortComponentLink();
                    
                    if (linkedPortComponent==null) {
                        throw new GeneratorException(localStrings.getLocalString(
		    	   "enterprise.webservice.componentlinkunresolved",
                           "The port-component-link {0} cannot be resolved", 
                           new Object[] {portInfo.getPortComponentLinkName()}));
                    }
                    WsUtil wsUtil = new WsUtil();
                    WebServerInfo wsi = wsUtil.getWebServerInfo(context.getDeploymentRequest());
                    URL rootURL = wsi.getWebServerRootURL(linkedPortComponent.isSecure());
                    URL actualAddress = linkedPortComponent.composeEndpointAddress(rootURL);
                    if(jaxwsClient) {
                        portInfo.addStubProperty(javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                                actualAddress.toExternalForm());
                    } else {
                        portInfo.addStubProperty(Stub.ENDPOINT_ADDRESS_PROPERTY, actualAddress.toExternalForm());
                    }
                    if (serviceRef.getBundleDescriptor().getModuleType().equals(ModuleType.CAR)) { 
                        wsdlOverride = serviceRef.getWsdlOverride();
			if (wsdlOverride!=null) {
                            wsdlOverriden = true;
                            serviceRef.setWsdlOverride(linkedPortComponent.getWebService().getWsdlFileUrl());
			}
                    }
                }
            }

            // If this is a post JAXRPC-1.1 based web service, then no need for code gen etc etc
            if(jaxwsClient) {
                return;
            }
            
            if( serviceRef.hasGeneratedServiceInterface() ) {
                
                if( serviceRef.hasWsdlFile() && serviceRef.hasMappingFile() ) {
                    codegenRequired = true;
                } else {
                    throw new GeneratorException
			("Deployment error for service-ref " + serviceRef.getName()
			 + ".\nService references with generated service " +
			 "interface must include WSDL and mapping information.");
                }
                
            } else {
                
                if( serviceRef.hasWsdlFile() ) {
                    if( serviceRef.hasMappingFile() ) {
                        codegenRequired = true;
                    } else {
                        throw new GeneratorException
			    ("Deployment error for service-ref " + serviceRef.getName()
			     + ".\nService references with wsdl must also have " +
			     "mapping information.");
                    }
                }
            }
            
            if( codegenRequired ) {
                ModelInfo modelInfo = createModelInfo(serviceRef);
                String args[] = createJaxrpcCompileArgs(false);

                CompileTool wscompile =
		    rpcFactory.createCompileTool(System.out, "wscompile");
                wscompileForAccept = wscompile;
                WsCompile delegate =
                    new WsCompile(wscompile, serviceRef);
                delegate.setModelInfo(modelInfo);
                wscompile.setDelegate(delegate);

                jaxrpc(args, delegate, serviceRef, files);
            }
            if (wsdlOverriden) {
                serviceRef.setWsdlOverride(wsdlOverride);
            }
        } catch(Exception e) {
            RuntimeException re = new RuntimeException(e.getMessage());
            re.initCause(e);
            throw re;
        }
    }
    
    /**
     * visits a web service definition
     * @param web service
     */
    public void accept(WebService webService) {
        super.accept(webService);
        try {
             if("1.1".compareTo(webService.getWebServicesDescriptor().getSpecVersion())<0) {
                WsUtil wsUtil = new WsUtil();
                Collection<WebServiceEndpoint> endpoints = webService.getEndpoints();
                for(WebServiceEndpoint ep : endpoints) {
                    if( ep.implementedByWebComponent() ) {
                        wsUtil.updateServletEndpointRuntime(ep);
                    } else {
                        wsUtil.validateEjbEndpoint(ep);
                    }
                }
                //wsImport(webService,  files);
             } else {
                jaxrpcWebService(webService, files);
            }
        } catch(Exception e) {
            RuntimeException ge =new RuntimeException(e.getMessage());
            ge.initCause(e);
            throw ge;
        }
    }
    
    public Iterator getListOfBinaryFiles() {
        return files.iterator();
    }
    
    public Iterator getListOfSourceFiles() {
        // for now I do not maintain those
        return null;
    }
    
    /**
     *Releases resources used during the code gen and compilation.
     */
    public void done() {
//        done(CompileTool) is now invoked after each compilation is complete
//        from inside the jaxrpc method.  Otherwise, multiple uses of jaxrpc could
//        cause continued file locking on Windows since only the last one was 
//        recorded in the wscompileForxxx variables.
//        
//        done(wscompileForAccept);
//        done(wscompileForWebServices);
    }
    
    /**
     *Navigates to the URLClassLoader used by the jaxrpc compilation and 
     *releases it.
     *@param wscompile the CompileTool whose loader is to be released
     */
    private void done(CompileTool wscompile) {
        /*
         *Follow the object graph to the loader: 
         *basically CompileTool -> ProcessorEnvironment -> the URLClassLoader.
         */
        if (wscompile != null && wscompile instanceof com.sun.xml.rpc.tools.wscompile.CompileTool) {
            com.sun.xml.rpc.tools.wscompile.CompileTool compileTool = (com.sun.xml.rpc.tools.wscompile.CompileTool) wscompile;
            com.sun.xml.rpc.spi.tools.ProcessorEnvironment env = compileTool.getEnvironment();
            if (env != null && env instanceof com.sun.xml.rpc.processor.util.ProcessorEnvironment) {
                com.sun.xml.rpc.processor.util.ProcessorEnvironment typedEnv = (com.sun.xml.rpc.processor.util.ProcessorEnvironment) env;
                java.net.URLClassLoader urlCL = typedEnv.getClassLoader();
                ClassLoaderUtil.releaseLoader(urlCL);
            }
        }
    }
    
    private JaxrpcMappingDescriptor getJaxrpcMappingInfo(URL mappingFileUrl,
                                                         Descriptor desc) 
        throws Exception {
        JaxrpcMappingDescriptor mappingDesc = null;

        InputStream is = null;
        try {
            is = mappingFileUrl.openStream();
            JaxrpcMappingDeploymentDescriptorFile jaxrpcDD = 
                new JaxrpcMappingDeploymentDescriptorFile();
            
            // useful for validation errors...
            if (desc instanceof ServiceReferenceDescriptor) {
                ServiceReferenceDescriptor srd = (ServiceReferenceDescriptor) desc;
                jaxrpcDD.setDeploymentDescriptorPath(srd.getMappingFileUri());
                jaxrpcDD.setErrorReportingString(srd.getBundleDescriptor().getModuleDescriptor().getArchiveUri());
            } 
            if (desc instanceof WebService) {
                WebService ws = (WebService) desc;
                jaxrpcDD.setDeploymentDescriptorPath(ws.getMappingFileUri());
                jaxrpcDD.setErrorReportingString(ws.getBundleDescriptor().getModuleDescriptor().getArchiveUri());
            }   
            jaxrpcDD.setXMLValidationLevel(Deployer.getValidationLevel());
            mappingDesc =  (JaxrpcMappingDescriptor) jaxrpcDD.read(desc, is);
        } finally {
            if( is != null ) {
                is.close();
            }
        } 

        return mappingDesc;
    }    
    
    private boolean isJaxrpcRIModelFile(URL mappingFileUrl) {
        boolean isModel = false;
        InputStream is  = null;
        try {
            is = mappingFileUrl.openStream();
            isModel = rpcFactory.createXMLModelFileFilter().isModelFile(is);
        } catch(Throwable t) {
        } finally {
            if( is != null ) {
                try {
                    is.close();
                } catch(Exception e) {}
            }
        }
        return isModel;
    }
    
    private ModelInfo createModelInfo(WebService webService) 
        throws Exception {

        ModelInfo modelInfo = null;
        URL mappingFileUrl = webService.getMappingFile().toURL();
        modelInfo = createModelFileModelInfo(mappingFileUrl);        
        if( isJaxrpcRIModelFile(mappingFileUrl) ) {
            debug("000. JaxrpcRIModelFile.");
            modelInfo = createModelFileModelInfo(mappingFileUrl);
        } else {
            JaxrpcMappingDescriptor mappingDesc = 
                getJaxrpcMappingInfo(mappingFileUrl, webService);
            if( mappingDesc.isSimpleMapping() ) {
                debug("111. SimpleMapping.");
                modelInfo = createNoMetadataModelInfo(webService, mappingDesc);
            } else {
                debug("222. FullMapping .");
                modelInfo = createFullMappingModelInfo(webService);
            }
        } 

        return modelInfo;
    }

    private ModelInfo createModelInfo(ServiceReferenceDescriptor serviceRef) 
        throws Exception {

        ModelInfo modelInfo = null;
        URL mappingFileUrl = serviceRef.getMappingFile().toURL();
        if( isJaxrpcRIModelFile(mappingFileUrl) ) {
            modelInfo = createModelFileModelInfo(mappingFileUrl);
        } else {
            JaxrpcMappingDescriptor mappingDesc = 
                getJaxrpcMappingInfo(mappingFileUrl, serviceRef);
            if( mappingDesc.isSimpleMapping() && 
                serviceRef.hasGeneratedServiceInterface() ) {
                // model info for this modeler requires generated service 
                // interface name.
                modelInfo = createNoMetadataModelInfo(serviceRef, mappingDesc);
            } else {
                modelInfo = createFullMappingModelInfo(serviceRef);
            }
        } 

        return modelInfo;
    }

    private ModelFileModelInfo createModelFileModelInfo(URL modelFileUrl) 
        throws Exception {
        
        ModelFileModelInfo modelInfo = rpcFactory.createModelFileModelInfo();
        modelInfo.setLocation(modelFileUrl.toExternalForm());

        return modelInfo;
    }

    private J2EEModelInfo createFullMappingModelInfo(WebService webService)
        throws Exception {

        URL mappingFileUrl = webService.getMappingFile().toURL();        
        URL wsdlFileUrl = webService.getWsdlFileUrl();

        return createFullMappingModelInfo(mappingFileUrl, wsdlFileUrl);
    }

    private J2EEModelInfo createFullMappingModelInfo
        (ServiceReferenceDescriptor serviceRef) throws Exception {

        URL mappingFileUrl = serviceRef.getMappingFile().toURL();        
        URL wsdlFileUrl = serviceRef.hasWsdlOverride() ?
            serviceRef.getWsdlOverride() : serviceRef.getWsdlFileUrl();
        return createFullMappingModelInfo(mappingFileUrl, wsdlFileUrl);
    }

    private J2EEModelInfo createFullMappingModelInfo
        (URL mappingFile, URL wsdlFile) throws Exception {

        J2EEModelInfo modelInfo = rpcFactory.createJ2EEModelInfo(mappingFile);
        modelInfo.setLocation(wsdlFile.toExternalForm());
        // java package name not used
        modelInfo.setJavaPackageName("package_ignored");
        return modelInfo;
        
    }

    private NoMetadataModelInfo createNoMetadataModelInfo
        (WebService webService, JaxrpcMappingDescriptor mappingDesc) 
        throws Exception {

        NoMetadataModelInfo modelInfo = rpcFactory.createNoMetadataModelInfo();
        URL wsdlFileUrl = webService.getWsdlFileUrl();

        Collection endpoints = webService.getEndpoints();
        if( endpoints.size() != 1 ) {
            throw new GeneratorException
                ("Deployment code generation error for webservice " + 
                 webService.getName() + ". " + 
                 " jaxrpc-mapping-file is required if web service has " +
                 "multiple endpoints");
        }

        WebServiceEndpoint endpoint = (WebServiceEndpoint) 
            endpoints.iterator().next();

        modelInfo.setLocation(wsdlFileUrl.toExternalForm());
        modelInfo.setInterfaceName(endpoint.getServiceEndpointInterface());
        modelInfo.setPortName(endpoint.getWsdlPort());

        addNamespaceMappingRegistry(modelInfo, mappingDesc);

        return modelInfo;
    }

    private void addNamespaceMappingRegistry
        (NoMetadataModelInfo modelInfo, JaxrpcMappingDescriptor mappingDesc) {
                                             
        NamespaceMappingRegistryInfo namespaceRegistry =
            rpcFactory.createNamespaceMappingRegistryInfo();
        
        modelInfo.setNamespaceMappingRegistry(namespaceRegistry);

        Collection mappings = mappingDesc.getMappings();
        for(Iterator iter = mappings.iterator(); iter.hasNext();) {
            Mapping next = (Mapping) iter.next();
            NamespaceMappingInfo namespaceInfo = 
                rpcFactory.createNamespaceMappingInfo(next.getNamespaceUri(), 
                                                      next.getPackage());
            namespaceRegistry.addMapping(namespaceInfo);
        }
    }

    private NoMetadataModelInfo createNoMetadataModelInfo
        (ServiceReferenceDescriptor serviceRef,
         JaxrpcMappingDescriptor mappingDesc) throws Exception {

        NoMetadataModelInfo modelInfo = rpcFactory.createNoMetadataModelInfo();
        URL wsdlFile = serviceRef.hasWsdlOverride() ?
            serviceRef.getWsdlOverride() : serviceRef.getWsdlFileUrl();
        modelInfo.setLocation(wsdlFile.toExternalForm());

        // Service endpoint interface is required.  Parse generated
        // service interface for it since we can't count on SEI
        // having been listed in standard deployment information.
        WsUtil wsUtil = new WsUtil();
        String serviceInterfaceName = serviceRef.getServiceInterface();
        
        ClassLoader cl = context.getDescriptor().getClassLoader();
        if (cl instanceof EJBClassLoader) {
            List moduleList = EJBClassPathUtils.getApplicationClassPath((Application) context.getDescriptor(), context.getSrcDir().getAbsolutePath());
            for (Iterator itr=moduleList.iterator();itr.hasNext();) {                
                ((EJBClassLoader) cl).appendURL((new File((String) itr.next())));
            }
        }
        
        Class serviceInterface = cl.loadClass(serviceInterfaceName);
        Collection seis = wsUtil.getSEIsFromGeneratedService(serviceInterface);

        if( seis.size() == 0 ) {
            throw new GeneratorException("Invalid Generated Service Interface "
                                         + serviceInterfaceName + " . ");
        } else if( seis.size() > 1 ) {
            throw new GeneratorException("Deployment error : If no " +
                                         "jaxrpc-mapping file is provided, " +
                                         "Generated Service Interface must have"
                                         +" only 1 Service Endpoint Interface");
        }

        String serviceEndpointInterface = (String) seis.iterator().next();
        modelInfo.setInterfaceName(serviceEndpointInterface);

        addNamespaceMappingRegistry(modelInfo, mappingDesc);

        return modelInfo;
    }    
    
    private boolean keepJaxrpcGeneratedFile(String fileType, Descriptor desc) {
        boolean keep = true;
        if( (fileType.equals(GeneratorConstants.FILE_TYPE_WSDL) ||
             fileType.equals(GeneratorConstants.FILE_TYPE_REMOTE_INTERFACE)) ) {
            keep = false;
        } else if( fileType.equals(GeneratorConstants.FILE_TYPE_SERVICE ) ) {
            // Only keep the service interface if this is a service reference
            // with generic service interface.  In this case, the interface
            // is generated during deployment instead of being packaged in
            // the module.
            keep = (desc instanceof ServiceReferenceDescriptor) &&
                ((ServiceReferenceDescriptor)desc).hasGenericServiceInterface();
        }

        return keep;
    }    
    
    // dummy file for jax-rpc wscompile bug
    File dummyConfigFile=null;
    
    private String[] createJaxrpcCompileArgs(boolean generateTies) 
        throws IOException 
    {
        int numJaxrpcArgs = 0;
        if (logger.isLoggable(Level.FINE) ) {
	    numJaxrpcArgs = 16;
	} else {
	    numJaxrpcArgs = 11;
	}

        // If we need to run wscompile more than once per .ear or
        // standalone module, use the -infix option to reduce the
        // chances that generated non-portable jaxrpc artifacts will clash
        // with generated artifacts from other service-refs and endpoints
        // loaded by the same classloader at runtime.   
        wscompileInvocationCount++;
        String infix = null;

        if( wscompileInvocationCount > 1 ) {
            numJaxrpcArgs++;
            infix = wscompileInvocationCount + "";
        }

	String[] jaxrpcArgs = new String[numJaxrpcArgs];
	int jaxrpcCnt = 0;

        if( dummyConfigFile == null ) {
            dummyConfigFile = File.createTempFile("dummy_wscompile_config",
                                                  "config");
            dummyConfigFile.deleteOnExit();
        }

        String classPath=null;
        String[] urls = context.getClasspathUrls();
        
        // we need to add all web and appclient classpath
        Application app = (Application) context.getDescriptor();
        List moduleList = EJBClassPathUtils.getApplicationClassPath(app, context.getSrcDir().getAbsolutePath());
        
        moduleList.addAll(java.util.Arrays.asList(urls));
        for (int i=0;i<moduleList.size();i++) {
            if (classPath==null) {
                classPath = (String) moduleList.get(i);
            } else {
                classPath = classPath + File.pathSeparatorChar + (String) moduleList.get(i);
            }
        }
        
        // wscompile doesn't support the -extdirs option, so the best we
        // can do is prepend the ext dir jar files to the classpath.
        String optionalDependencyClassPath = 
            OptionalPkgDependency.getExtDirFilesAsClasspath();
        if(optionalDependencyClassPath.length() > 0) {
            classPath = optionalDependencyClassPath +
                File.pathSeparatorChar + classPath;
        }

	jaxrpcArgs[jaxrpcCnt++] = generateTies ? "-gen:server" : "-gen:client";

        // Prevent wscompile from regenerating portable classes that are
        // already packaged within the deployed application. 
        jaxrpcArgs[jaxrpcCnt++] = "-f:donotoverride";

        if( infix != null ) {
            jaxrpcArgs[jaxrpcCnt++] = "-f:infix:" + infix;
        }

	jaxrpcArgs[jaxrpcCnt++] = "-classpath";
	jaxrpcArgs[jaxrpcCnt++] = classPath;

	if (logger.isLoggable(Level.FINE)) {
            long timeStamp = System.currentTimeMillis();
	    jaxrpcArgs[jaxrpcCnt++] = "-Xdebugmodel:" +
                context.getStubsDir() + File.separator + "debugModel.txt." +
                timeStamp;
	    jaxrpcArgs[jaxrpcCnt++] = "-Xprintstacktrace";
	    jaxrpcArgs[jaxrpcCnt++] = "-model";
	    jaxrpcArgs[jaxrpcCnt++] = 
                context.getStubsDir() + File.separator + "debugModel.model" +
                timeStamp;
            jaxrpcArgs[jaxrpcCnt++] = "-verbose";
	}

        jaxrpcArgs[jaxrpcCnt++] = "-s";
        jaxrpcArgs[jaxrpcCnt++] = context.getStubsDir().toString();
        jaxrpcArgs[jaxrpcCnt++] = "-d";
        jaxrpcArgs[jaxrpcCnt++] = context.getStubsDir().toString();
        jaxrpcArgs[jaxrpcCnt++] = "-keep";
        jaxrpcArgs[jaxrpcCnt++] = "-g";

        // config file is not used, but it must be an existing file or it
        // will not pass CompileTool argument validation.
        jaxrpcArgs[jaxrpcCnt++] = dummyConfigFile.getPath();

	if ( logger.isLoggable(Level.FINE)) {
	    for ( int i = 0; i < jaxrpcArgs.length; i++ ) {
		logger.fine(jaxrpcArgs[i]);
	    }
	}
        
        return jaxrpcArgs;
    }
    
    private void jaxrpc(String[] args, WsCompile wsCompile, Descriptor desc,
                        Vector files)
        throws Exception {

	try {
	    if (logger.isLoggable(Level.FINE)) {
		debug("---> ARGS = ");
		for (int i = 0; i < args.length; i++) {
		    System.err.print(args[i] + "; ");
		}
	    }
            boolean compiled = wsCompile.getCompileTool().run(args);
            done(wsCompile.getCompileTool());
            if( compiled ) {
                Iterator generatedFiles = 
                    wsCompile.getGeneratedFiles().iterator();

                while(generatedFiles.hasNext()) {
                    GeneratedFileInfo next = (GeneratedFileInfo) 
                        generatedFiles.next();
                    String fileType = next.getType();
                    File file = next.getFile();
                    String origPath = file.getPath();
                    if( origPath.endsWith(".java") ) {
                        int javaIndex = origPath.lastIndexOf(".java");
                        String newPath = origPath.substring(0, javaIndex) +
                            ".class";
                        if( keepJaxrpcGeneratedFile(fileType, desc) ) {
                            files.add(newPath);
                        } 
                    }
                }
            } else {
                throw new GeneratorException("jaxrpc compilation exception");
            }
        } catch (Throwable t) {
            GeneratorException ge = 
                new GeneratorException(t.getMessage());
            ge.initCause(t);
            throw ge;
	}
    }
    
    private void jaxrpcWebService(WebService webService, Vector files) 
        throws Exception {

        if((webService.getWsdlFileUrl() == null) ||
           (webService.getMappingFileUri() == null)) {
                throw new IASDeploymentException(localStrings.getLocalString(
               "enterprise.webservice.jaxrpcFilesNotFound",
               "Service {0} seems to be a JAXRPC based web service but without "+
               "the mandatory WSDL and Mapping file. Deployment cannot proceed", 
               new Object[] {webService.getName()}));            
        }
        ModelInfo modelInfo = createModelInfo(webService);
        String args[] = createJaxrpcCompileArgs(true);

        CompileTool wscompile =
            rpcFactory.createCompileTool(System.out, "wscompile");
        wscompileForWebServices = wscompile;
        WsCompile delegate = new WsCompile(wscompile, webService);
        delegate.setModelInfo(modelInfo);
        wscompile.setDelegate(delegate);

        jaxrpc(args, delegate, webService, files);
    }        

    private void debug(String msg) {
        if (logger.isLoggable(Level.FINE) ) {
	    System.out.println("[JaxRpcRICodegen] --> " + msg);
        }
    }
    
}