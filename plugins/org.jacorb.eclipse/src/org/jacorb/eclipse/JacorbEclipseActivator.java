package org.jacorb.eclipse;

import org.eclipse.core.runtime.Plugin;
import org.jacorb.JacorbActivator;
import org.osgi.framework.BundleContext;

public class JacorbEclipseActivator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jacorb.eclipse";
	private static JacorbEclipseActivator instance;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		JacorbActivator.getDefault().init();
		JacorbEclipseActivator.instance = this;
	}
	
	public void init() {
		JacorbActivator.getDefault().init();
	}
	
	public static JacorbEclipseActivator getDefault() {
		return instance;
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
