package org.jacorb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Properties;
import java.util.logging.LogManager;

import javax.swing.JOptionPane;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.osgi.service.datalocation.Location;
import org.omg.CORBA.ORB;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class JacorbActivator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jacorb.eclipse";
	private static JacorbActivator instance;
	private boolean init;
	private boolean postInit;
	private String jacorbConfigDir;
	private String jacorbHome;

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

		Throwable exception = null;
		try {
			ORB.init();
			return;
		} catch (Throwable e) {
			exception = e;
		}

		try {
			initViaReflection();
			return;
		} catch (Throwable e) {
			e.printStackTrace();
			exception = e;
		}

		autoConfigure(exception);
	}

	private void initViaReflection() throws Exception {
		Field[] fields = ORB.class.getDeclaredFields();
		for (Field f : fields) {
			if ("singleton".equalsIgnoreCase(f.getName())) {
				f.setAccessible(true);
				try {
					f.set(null, new org.jacorb.orb.ORBSingleton());
				} finally {
					f.setAccessible(false);
				}
				return;
			}
		}
	}


	private void autoConfigure(Throwable exception) {
		Location installLocation = Platform.getInstallLocation();
		boolean autoConfigured = false;
		boolean shouldConfigure = false;
		IStatus configureStatus = Status.OK_STATUS;

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
							configureStatus = new Status(Status.ERROR, PLUGIN_ID, "Failed to auto configure " + iniFile, e);
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
				shutdown(-1, msg, configureStatus);
			} else {
				String msg = "REDHAWK initial setup has completed. " + "\n\nYou MUST restart the application for these new settings to take effect.";
				shutdown(IApplication.EXIT_OK, msg, null);
			}
		} else {
			String msg = "Please add the following to your vm args:\n\t" + jacorbLine;
			shutdown(-1, msg, new Status(Status.ERROR, PLUGIN_ID, "Failed to find configuration file.", null));
		}
	}


	private void shutdown(int errorCode, String msg, IStatus status) {
		if (status == null || status.isOK()) {
			JOptionPane.showMessageDialog(null, msg, "Setup", JOptionPane.INFORMATION_MESSAGE);
			System.out.println(msg);
		} else {
			JOptionPane.showMessageDialog(null, msg, "Configuration Error", JOptionPane.ERROR_MESSAGE);
			System.err.println(msg);
		}

		if (status != null && !status.isOK() && status.getException() != null) {
			status.getException().printStackTrace();
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
		configureLogback(getBundle().getBundleContext());
		configureJavaLogger(getBundle().getBundleContext());
		configureJacorb(getBundle().getBundleContext());
		postInit();
	}

	private void configureLogback(BundleContext bundleContext) {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			// Call context.reset() to clear any previous configuration, e.g. default
			// configuration. For multi-step configuration, omit calling context.reset().
			context.reset();
			InputStream stream;
			try {
				URL logbackUrl = Platform.getConfigurationLocation().getDataArea("logback.xml");
				stream = logbackUrl.openStream();
			} catch (Exception e) {
				stream = FileLocator.find(bundleContext.getBundle(), new Path("etc/logback.xml"), null).openStream();
			}
			try {
				configurator.doConfigure(stream);
			} finally {
				stream.close();
			}
		} catch (JoranException je) {
			// StatusPrinter will handle this
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}

	private void configureJacorb(BundleContext context) {
		URL propUrl = null;
		String currentProperty = System.getProperty("jacorb.config.dir");
		if (currentProperty != null) {
			File f = new File(currentProperty);
			File propFile = new File(f, "jacorb.properties");
			if (propFile.exists()) {
				this.jacorbConfigDir = propFile.getParentFile().getAbsolutePath();
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
				this.jacorbConfigDir = file.getParentFile().getAbsolutePath();
				System.setProperty("jacorb.config.dir", file.getParentFile().getAbsolutePath());
			} else {
				// TODO Log this to the trace logger

			}
		}
		try {
			jacorbHome = FileLocator.toFileURL(FileLocator.find(context.getBundle(), new Path(""), null)).getPath();
		} catch (IOException e) {
			// PASS
		} 
		setProperties(context);
	}

	private void setProperties(BundleContext context) {
		if (jacorbHome != null) {
			System.setProperty("jacorb.home", jacorbHome);
		}
		if (jacorbConfigDir != null) {
			System.setProperty("jacorb.config.dir", getDefault().jacorbConfigDir);
		}
		System.setProperty("com.sun.CORBA.transport.ORBUseNIOSelectToWait", "false");
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
		System.setProperty("org.omg.CORBA.ORBSingletonClass", "org.jacorb.orb.ORBSingleton");
		System.setProperty("org.omg.PortableInterceptor.ORBInitializerClass.standard_init", "org.jacorb.orb.standardInterceptors.IORInterceptorInitializer");
	}

	public static void setupProperties(Properties properties) {
		if (getDefault() != null) {
			if (getDefault().jacorbHome != null) {
				properties.setProperty("jacorb.home", getDefault().jacorbHome);
			}
			if (getDefault().jacorbConfigDir != null) {
				properties.setProperty("jacorb.config.dir", getDefault().jacorbConfigDir);
			}
		}
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
