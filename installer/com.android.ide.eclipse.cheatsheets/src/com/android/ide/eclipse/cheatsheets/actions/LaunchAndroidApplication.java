/*******************************************************************************
 * Copyright (c) 2000, 2010 SnPe Informacioni Sistemi.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SnPe Informacioni sistemi - initial API and implementation
 *******************************************************************************/
package com.android.ide.eclipse.cheatsheets.actions;

import com.android.ide.eclipse.adt.internal.launch.AndroidLaunchController;
import com.android.ide.eclipse.adt.internal.sdk.ProjectState;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.cheatsheets.Activator;
import com.android.ide.eclipse.installer.InstallerActivator;
import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.avd.AvdManager.AvdInfo;
import com.android.sdkuilib.internal.widgets.MessageBoxLog;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class LaunchAndroidApplication extends Action implements ICheatSheetAction {

	private static final String RUN_MODE = "run";
	
	public void run(final String[] params, ICheatSheetManager manager) {
		if (params == null || params[0] == null) {
			Activator.log("Invalid parameters");
			return;
		}
		String mode = RUN_MODE;
		if (params[1] != null) {
			mode = params[1];
		}
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		final IProject project = workspaceRoot.getProject(params[0]);
		if (project == null || !project.isOpen()) {
			Activator.log("The " + params[0] + " project is invalid");
			return;
		}
		IRunnableWithProgress op = new IRunnableWithProgress() {
			
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				monitor.setTaskName("Checking AVD...");
				checkAvd(project);
			}
		};
		try {
			new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, false, op);
		} catch (InvocationTargetException e) {
			InstallerActivator.log(e);
		} catch (InterruptedException e) {
			// ignore
		}
		ILaunchConfiguration config = AndroidLaunchController.getLaunchConfig(project);

        if (config != null) {
            // and launch!
            DebugUITools.launch(config, mode);
        }
	}

	private void checkAvd(IProject project) {
		Sdk sdk = Sdk.getCurrent();
		if (sdk != null) {
			IJavaProject javaProject = JavaCore.create(project);
            ProjectState state = Sdk.getProjectState(project);
            IAndroidTarget projectTarget = state.getTarget();
            if (projectTarget != null) {
            	AvdManager avdManager = sdk.getAvdManager();
				AvdInfo[] avds = avdManager.getAllAvds();
				for (AvdInfo avd:avds) {
					IAndroidTarget avdTarget = avd.getTarget();
					if (projectTarget.compareTo(avdTarget) == 0) {
						return;
					}
				}
				createAvd("HelloWorldAvd", sdk, projectTarget);
            }
		}
	}
	
	private static boolean createAvd(String avdName, Sdk sdk, IAndroidTarget target) {
		String sdName = "1024M"; //$NON-NLS-1$;
		String skinName = null;
		ISdkLog log = new MessageBoxLog("Create default AVD",
				Display.getDefault(), true);
		File avdFolder = getAvdFolder(avdName);
		if (avdFolder == null) {
			return false;
		}
		if (avdFolder.exists()) {
			// AVD is already created
			return true;
		}
		boolean force = false;

		boolean success = false;
		Map<String, String> mProperties = new HashMap<String, String>();
		mProperties.put("hw.lcd.density", "160");  //$NON-NLS-1$//$NON-NLS-2$

		AvdManager mAvdManager = sdk.getAvdManager();
		AvdInfo avdInfo = mAvdManager.createAvd(avdFolder, avdName, target,
				skinName, sdName, mProperties, force, log);

		success = avdInfo != null;

		if (!success && log instanceof MessageBoxLog) {
			((MessageBoxLog) log).displayResult(success);
		}
		return success;
	}

	private static File getAvdFolder(String avdName) {
		File avdFolder;
		try {
			avdFolder = new File(AndroidLocation.getFolder()
					+ AndroidLocation.FOLDER_AVD, avdName
					+ AvdManager.AVD_FOLDER_EXTENSION);
		} catch (AndroidLocationException e) {
			return null;
		}
		return avdFolder;
	}

}
