/*******************************************************************************
 * This file is protected by Copyright. 
 * Please refer to the COPYRIGHT file distributed with this source distribution.
 *
 * This file is part of REDHAWK IDE.
 *
 * All rights reserved.  This program and the accompanying materials are made available under 
 * the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jacorb.eclipse.tests;

import org.jacorb.config.Configuration;
import org.jacorb.eclipse.JacorbEclipseActivator;
import org.jacorb.orb.ORBSingleton;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

/**
 * 
 */
public class JacorbEclipseTest {

	@BeforeClass
	public static void init() {
		JacorbEclipseActivator.getDefault().init();
	}

	@Test
	public void testConfiguration() {
		Configuration configuration = ((ORBSingleton) ORB.init()).getConfiguration();
		Assert.assertTrue("jacorb.interop.lax_boolean_encoding should be true",
			configuration.getAttributeAsBoolean("jacorb.interop.lax_boolean_encoding", false));
	}

}
