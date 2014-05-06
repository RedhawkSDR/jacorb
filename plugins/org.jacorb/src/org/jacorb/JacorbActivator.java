package org.jacorb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.LogManager;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.osgi.service.datalocation.Location;
import org.omg.CORBA.ORB;
import org.osgi.framework.BundleContext;

public class JacorbActivator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jacorb.eclipse";
	private static JacorbActivator instance;
	private boolean init;
	private boolean postInit;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		JacorbActivator.instance = this;
		init();
	}

	private void postInit() {
		if (postInit) {
			return;
		}
		postInit = true;

		String version = System.getProperty("java.version");
		try {
			if (version.startsWith("1.7.0_")) {
				int minorVersion = Integer.parseInt(version.substring(6));
				if (minorVersion < 55) {
					return;
				}
			} else if (version.startsWith("1.8.0_")) {
				// PASS
				// Always do this for 1.8
			} else if (version.startsWith("1.6.0_")) {
				int minorVersion = Integer.parseInt(version.substring(6));
				if (minorVersion < 30) {
					return;
				}
			}
		} catch (NumberFormatException e) {
			// PASS
		}
		try {
			ORB.init();
		} catch (Throwable exception) {
			Location installLocation = Platform.getInstallLocation();
			boolean autoConfigured = false;
			boolean shouldConfigure = false;
			Exception configureException = null;
			
			String jacorbLine = "-Djava.endorsed.dirs=<Eclipse Dir>/plugins/org.jacorb/lib";
			File iniFile = new File("eclipse.ini");
			if (installLocation != null) {
				URL homeUrl = installLocation.getURL();
				File homeFile = new File(homeUrl.getPath());
				if (homeFile.exists()) {
					File jacorbDir = null;
					try {
						jacorbDir = new File(FileLocator.toFileURL(FileLocator.find(Platform.getBundle("org.jacorb"), new Path("lib"), null)).getPath());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					if (jacorbDir != null && jacorbDir.exists()) {
						jacorbLine = "-Djava.endorsed.dirs=" + jacorbDir.getAbsolutePath();
						iniFile = new File(homeFile, "eclipse.ini");
						if (!iniFile.exists()) {
							File[] files = homeFile.listFiles(new FilenameFilter() {

								@Override
								public boolean accept(File dir, String name) {
									if ("launch.ini".equals(name)) {
										return false;
									}
									return name.endsWith(".ini");
								}
							});
							if (files != null && files.length > 0) {
								iniFile = files[0];
							}
						}
						if (iniFile.exists()) {
							shouldConfigure = true;
							StringBuilder buffer = new StringBuilder();
							BufferedReader reader = null;
							BufferedWriter writer = null;
							boolean foundConfig = false;
							try {
								reader = new BufferedReader(new FileReader(iniFile));
								for (String line = reader.readLine(); line != null; line = reader.readLine()) {
									if (line.trim().startsWith("-Djava.endorsed.dirs")) {
										if (jacorbLine.equals(line)) {
											exception.printStackTrace();
											System.exit(-1);
										}
										buffer.append(jacorbLine);
										foundConfig = true;
									} else if (!line.trim().isEmpty()) {
										buffer.append(line);
									} else {
										continue;
									}
									buffer.append("\n");
								}
								if (!foundConfig) {
									buffer.append(jacorbLine);
									buffer.append("\n");
								}
								writer = new BufferedWriter(new FileWriter(iniFile));
								writer.write(buffer.toString());
								writer.flush();
								autoConfigured = true;
							} catch (IOException e) {
								configureException = e;
							} finally {
								if (reader != null) {
									IOUtils.closeQuietly(reader);
								}
								if (writer != null) {
									IOUtils.closeQuietly(writer);
								}
							}
						}
					}
				}
			}

			if (shouldConfigure) {
				if (!autoConfigured) {
					String msg = "Please add the following to " + iniFile + ":\n\t" + jacorbLine;
					shutdown(-1, msg, configureException);
				} else {
					String msg = iniFile + " Jacorb configuration has been automatically updated. "
						+ "\n\nYou MUST restart the application for these new settings to take effect.";
					shutdown(IApplication.EXIT_OK, msg, null);
				}
			} else {
				String msg = "Please add the following to your vm args:\n\t" + jacorbLine;
				shutdown(-1, msg, null);
			}
		}

	}
	
	private void shutdown(int errorCode, String msg, Throwable exception) {
		System.err.println(msg);
		if (exception != null) {
			exception.printStackTrace();
		}
		Platform.endSplash();
		System.exit(errorCode);
	}

	public static JacorbActivator getDefault() {
		return JacorbActivator.instance;
	}

	public synchronized void init() {
		if (init) {
			return;
		}
		init = true;
		// Ensure Jacorb and Java Logger are configured correctly
		configureJavaLogger(getBundle().getBundleContext());
		configureJacorb(getBundle().getBundleContext());
		postInit();
	}

	private void configureJacorb(BundleContext context) {
		setProperties(context);
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
			// TODO Log this to the trace logger
			try {
				propUrl = FileLocator.toFileURL(FileLocator.find(context.getBundle(), new Path("etc/jacorb.properties"), null));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} finally {
			if (propUrl != null) {
				String fileName = propUrl.getFile();
				File file = new File(fileName);
				System.setProperty("jacorb.config.dir", file.getParentFile().getAbsolutePath());
			} else {
				// TODO Log this to the trace logger

			}
		}
	}

	private void setProperties(BundleContext context) {
		String jacorbHome;
		try {
			jacorbHome = FileLocator.toFileURL(FileLocator.find(context.getBundle(), new Path(""), null)).getPath();
			System.setProperty("jacorb.home", jacorbHome);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		System.setProperty("com.sun.CORBA.transport.ORBUseNIOSelectToWait", "false");
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
		System.setProperty("org.omg.CORBA.ORBSingletonClass", "org.jacorb.orb.ORBSingleton");
		System.setProperty("org.omg.PortableInterceptor.ORBInitializerClass.standard_init", "org.jacorb.orb.standardInterceptors.IORInterceptorInitializer");
	}
	
	public static void setupProperties(Properties properties) {
		properties.setProperty("com.sun.CORBA.transport.ORBUseNIOSelectToWait", "false");
		properties.setProperty("java.net.preferIPv4Stack", "true");
		properties.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
		properties.setProperty("org.omg.CORBA.ORBSingletonClass", "org.jacorb.orb.ORBSingleton");
		properties.setProperty("org.omg.PortableInterceptor.ORBInitializerClass.standard_init", "org.jacorb.orb.standardInterceptors.IORInterceptorInitializer");
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
				try {
					LogManager.getLogManager().readConfiguration();
					return;
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		InputStream test = null;
		InputStream logInputStream = null;
		try {
			test = Platform.getConfigurationLocation().getDataArea("javalogger.properties").openStream();
			LogManager.getLogManager().readConfiguration(test);
		} catch (IOException e) {
			URL javaloggerURL = FileLocator.find(context.getBundle(), new Path("etc/javalogger.properties"), null);
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
		JacorbActivator.instance = null;
	}

}
