/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.installer;

import com.android.ide.eclipse.adt.internal.sdk.Sdk;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class InstallerActivator extends AbstractUIPlugin {

	public static final long MAX_DELAY = 60000L;

	public static final long SLEEP_TIME = 200L;

	// The plug-in ID
	public static final String PLUGIN_ID = "com.android.ide.eclipse.installer"; //$NON-NLS-1$

	public final static String SHOW_ME_AGAIN = "showMeAgain"; //$NON-NLS-1$

	// The shared instance
	private static InstallerActivator plugin;

	private static ImageDescriptor mAndroidImageDescriptor;

	private static Image mAndroidImage;
	
	/**
	 * The constructor
	 */
	public InstallerActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static InstallerActivator getDefault() {
		return plugin;
	}

	public static Image getAndroidImage() {
		if (mAndroidImage == null) {
			mAndroidImageDescriptor = 
				imageDescriptorFromPlugin(PLUGIN_ID, "/icons/android.png"); //$NON-NLS-1$
			mAndroidImage = mAndroidImageDescriptor.createImage();
		}
		return mAndroidImage;
	}
	
	public static boolean showMeInstaller() {
		return getDefault().getPreferenceStore().getBoolean(SHOW_ME_AGAIN);
	}
	
	public static void checkForLoad() {
		long start = System.currentTimeMillis();
		while (Sdk.getCurrent() == null) {
			try {
				Thread.sleep(SLEEP_TIME);
			} catch (InterruptedException e) {
				// ignore
			}
			long delay = System.currentTimeMillis() - start;
			if (delay > MAX_DELAY) {
				return;
			}
		}
	}
	
	public void logError(String message) {
		IStatus status = new Status(Status.ERROR, PLUGIN_ID, message);
		getDefault().getLog().log(status);
	}
	
	public void logWarning(String message) {
		IStatus status = new Status(Status.WARNING, PLUGIN_ID, message);
		getDefault().getLog().log(status);
	}
	
	public static void log(Throwable e) {
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, e
				.getLocalizedMessage(), e);
		getDefault().getLog().log(status);
	}
}
