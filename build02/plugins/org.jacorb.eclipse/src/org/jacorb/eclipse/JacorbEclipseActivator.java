package org.jacorb.eclipse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.LogManager;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class JacorbEclipseActivator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jacorb.eclipse";
	private static JacorbEclipseActivator instance;
	private boolean init;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		JacorbEclipseActivator.instance = this;
		init();
	}

	public static JacorbEclipseActivator getDefault() {
		return JacorbEclipseActivator.instance;
	}

	public void init() {
		if (init) {
			return;
		}
		init = true;
		// Ensure Jacorb and Java Logger are configured correctly
		configureJavaLogger(getBundle().getBundleContext());
		configureJacorb(getBundle().getBundleContext());
	}

	private void configureJacorb(BundleContext context) {
		setProperties();
		URL propUrl = null;
		String currentProperty = System.getProperty("jacorb.config.dir");
		if (currentProperty != null) {
			File f = new File(currentProperty);
			File propFile = new File(f, "jacorb.properties");
			if (propFile.exists()) {
				return;
			}
		}
		try {
			propUrl = Platform.getConfigurationLocation().getDataArea("jacorb.properties");
			// Test if the file exists
			InputStream stream = propUrl.openStream();
			IOUtils.closeQuietly(stream);
		} catch (IOException e) {
			propUrl = FileLocator.find(context.getBundle(), new Path("jacorb.properties"), null);
			// TODO Log this to the trace logger
		} finally {
			if (propUrl != null) {
				URL fileUrl;
				try {
					if ("file".equals(propUrl.getProtocol())) {
						fileUrl = propUrl;
					} else {
						fileUrl = FileLocator.toFileURL(propUrl);
					}
					String fileName = fileUrl.getFile();
					File file = new File(fileName);
					System.setProperty("jacorb.config.dir", file.getParentFile().getAbsolutePath());
				} catch (IOException e) {
					// TODO Log this to the trace logger
					e.printStackTrace();
				}
			} else {
				// TODO Log this to the trace logger
			
			}
		}
	}

	private void setProperties() {
		System.setProperty("com.sun.CORBA.transport.ORBUseNIOSelectToWait", "false");
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
		System.setProperty("org.omg.CORBA.ORBSingletonClass", "org.jacorb.orb.ORBSingleton");
		System.setProperty("org.omg.PortableInterceptor.ORBInitializerClass.standard_init", "org.jacorb.orb.standardInterceptors.IORInterceptorInitializer");
	}

	/**
	 * This method ensures that if the Java logging properties weren't properly installed by the IDE's feature
	 * into the configuration directory that we'll load a backup. This is primarily important for debugging the IDE
	 * within Eclipse where this is the situation.
	 * 
	 * @param context
	 */
	private void configureJavaLogger(final BundleContext context) {
		String currentProperty = System.getProperty("java.util.logging.config.file");
		if (currentProperty != null) {
			File propFile = new File(currentProperty);
			if (propFile.exists()) {
				return;
			}
		}

		InputStream test = null;
		InputStream logInputStream = null;
		try {
			test = Platform.getConfigurationLocation().getDataArea("javalogger.properties").openStream();
		} catch (IOException e) {
			URL javaloggerURL = FileLocator.find(context.getBundle(), new Path("javalogger.properties"), null);
			if (javaloggerURL != null) {
				try {
					logInputStream = javaloggerURL.openStream();
					LogManager.getLogManager().readConfiguration(logInputStream);
				} catch (IOException e2) {
					// TODO Log this to the trace logger
					e.printStackTrace();
				} catch (SecurityException e2) {
					// TODO Log this to the trace logger
					e.printStackTrace();
				}
			}
		} finally {
			if (test != null) {
				try {
					test.close();
				} catch (IOException e) {
					// PASS
				}
			}
			if (logInputStream != null) {
				try {
					logInputStream.close();
				} catch (IOException e) {
					// PASS
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		super.stop(bundleContext);
		JacorbEclipseActivator.instance = null;
	}

}
