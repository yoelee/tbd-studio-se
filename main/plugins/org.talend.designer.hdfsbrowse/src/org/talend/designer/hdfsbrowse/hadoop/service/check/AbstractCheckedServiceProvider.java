// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.hdfsbrowse.hadoop.service.check;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.classloader.DynamicClassLoader;
import org.talend.core.utils.ReflectionUtils;
import org.talend.designer.hdfsbrowse.exceptions.HadoopServerException;
import org.talend.designer.hdfsbrowse.hadoop.service.HadoopServiceProperties;
import org.talend.designer.hdfsbrowse.manager.HadoopParameterUtil;

/**
 * created by ycbai on Aug 14, 2014 Detailled comment
 *
 */
public abstract class AbstractCheckedServiceProvider implements ICheckedServiceProvider {

    @Override
    public boolean checkService(final HadoopServiceProperties serviceProperties, final int timeout) throws HadoopServerException {
        boolean checkedOK = true;

        ClassLoader classLoader = getClassLoader(serviceProperties);
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            ICheckedWorkUnit workUnit = new CheckedWorkUnit() {

                @Override
                protected Object run(ClassLoader cl) throws Exception {
                    return check(serviceProperties, cl);
                }
            };
            workUnit.setTimeout(timeout);
            workUnit.setClassLoader(classLoader);
            workUnit.execute();
        } catch (Exception e) {
            checkedOK = false;
            throw new HadoopServerException(e);
        }

        return checkedOK;
    }

    protected abstract Object check(final HadoopServiceProperties serviceProperties, final ClassLoader classLoader)
            throws Exception;

    protected abstract ClassLoader getClassLoader(HadoopServiceProperties serviceProperties);

    protected void setHadoopProperties(Object conf, HadoopServiceProperties serviceProperties) throws Exception {
        List<Map<String, Object>> hadoopProperties = serviceProperties.getHadoopProperties();
        if (hadoopProperties != null) {
            for (Map<String, Object> propMap : hadoopProperties) {
                String key = String.valueOf(propMap.get("PROPERTY")); //$NON-NLS-1$
                String value = String.valueOf(propMap.get("VALUE")); //$NON-NLS-1$
                ReflectionUtils.invokeMethod(conf, "set", new Object[] { key, value }); //$NON-NLS-1$
            }
        }
    }

    protected ClassLoader addCustomConfsJarIfNeeded(ClassLoader baseLoader, HadoopServiceProperties serviceProperties) {
        ClassLoader classLoader = baseLoader;
        if (serviceProperties.isUseCustomConfs()) {
            String clusterLabel = serviceProperties.getRelativeHadoopClusterLabel();
            if (classLoader instanceof DynamicClassLoader && clusterLabel != null) {
                String customConfsJarName = HadoopParameterUtil.getConfsJarDefaultName(clusterLabel);
                boolean confFileExist = false;
                Set<String> libraries = ((DynamicClassLoader) classLoader).getLibraries();
                for (String lib : libraries) {
                    if (customConfsJarName.equals(lib)) {
                        confFileExist = true;
                        break;
                    }
                }
                if (!confFileExist) {
                    try {
                        classLoader = DynamicClassLoader.createNewOneBaseLoader((DynamicClassLoader) baseLoader,
                                new String[] { customConfsJarName }, null);
                    } catch (MalformedURLException e) {
                        ExceptionHandler.process(e);
                    }
                }
            }
        }
        return classLoader;
    }

    protected void setMaprTicketConfig(HadoopServiceProperties serviceProperties, ClassLoader classLoader, boolean useKerberos)
            throws Exception {
        String mapRTicketUsername = serviceProperties.getUserName();
        String mapRTicketPassword = serviceProperties.getMaprTPassword();
        String mapRTicketCluster = serviceProperties.getMaprTCluster();
        String mapRTicketDuration = serviceProperties.getMaprTDuration();
        boolean setMapRHomeDir = serviceProperties.isSetMaprTHomeDir();
        String mapRHomeDir = serviceProperties.getMaprTHomeDir();
        boolean setMapRHadoopLogin = serviceProperties.isSetHadoopLogin();

        String mapRHadoopLogin = serviceProperties.getMaprTHomeDir();
        System.setProperty("pname", "MapRLogin");//$NON-NLS-1$ //$NON-NLS-2$
        System.setProperty("https.protocols", "TLSv1.2");//$NON-NLS-1$ //$NON-NLS-2$
        System.setProperty("mapr.home.dir", setMapRHomeDir ? mapRHomeDir : "/opt/mapr");//$NON-NLS-1$ //$NON-NLS-2$

        Long desiredTicketDurInSecs = 86400L;
        if (mapRTicketDuration != null && StringUtils.isNotBlank(mapRTicketDuration)) {
            if (mapRTicketDuration.endsWith("L")) {//$NON-NLS-1$ 
                mapRTicketDuration = mapRTicketDuration.substring(0, mapRTicketDuration.length() - 1);
                desiredTicketDurInSecs = Long.valueOf(mapRTicketDuration) + 'L';
            } else if (StringUtils.isNumeric(mapRTicketDuration)) {
                desiredTicketDurInSecs = Long.valueOf(mapRTicketDuration) + 'L';
            }
        }
        Object mapRClientConfig = ReflectionUtils.newInstance(
                "com.mapr.login.client.MapRLoginHttpsClient", classLoader, new Object[] {}); //$NON-NLS-1$
        if (useKerberos) {
            System.setProperty("hadoop.login", "kerberos");//$NON-NLS-1$ //$NON-NLS-2$  
            ReflectionUtils.invokeMethod(mapRClientConfig,
                    "getMapRCredentialsViaKerberos", new Object[] { mapRTicketCluster, desiredTicketDurInSecs }); //$NON-NLS-1$
        } else {
            if (setMapRHadoopLogin) {
                System.setProperty("hadoop.login", mapRHadoopLogin);//$NON-NLS-1$
            } else {
                ReflectionUtils.invokeMethod(mapRClientConfig, "setCheckUGI", new Object[] { false }, boolean.class);//$NON-NLS-1$
            }
            ReflectionUtils
                    .invokeMethod(
                            mapRClientConfig,
                            "getMapRCredentialsViaPassword", new Object[] { mapRTicketCluster, mapRTicketUsername, mapRTicketPassword, desiredTicketDurInSecs }); //$NON-NLS-1$
        }
    }
}
