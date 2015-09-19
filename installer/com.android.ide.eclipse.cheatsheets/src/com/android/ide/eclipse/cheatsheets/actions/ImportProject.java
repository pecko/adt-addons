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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.project.ProjectHelper;
import com.android.ide.eclipse.adt.internal.sdk.ProjectState;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.cheatsheets.Activator;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.internal.project.ProjectPropertiesWorkingCopy;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class ImportProject extends Action implements ICheatSheetAction {

	private static boolean overwrite;

	private static Shell shell;
	
	private static final IOverwriteQuery OVERWRITE_ALL_QUERY = new IOverwriteQuery() {
		public String queryOverwrite(String pathString) {
			return IOverwriteQuery.ALL;
		}
	};
	
	public void run(String[] params, ICheatSheetManager manager) {
		if(params == null || params[0] == null || params[1] == null) {
			return;
		}
		
		importProject(params[0],params, manager);
	}

	private void importProject(final String projectURL, final String[] params, final ICheatSheetManager manager) {
		final String projectName = params[1];
		WorkspaceJob workspaceJob = new WorkspaceJob("Importing projects ...") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IProject project = workspace.getRoot().getProject(projectName);
				if (project.exists()) {
					Display.getDefault().syncExec(new Runnable() {

						public void run() {
							overwrite = MessageDialog.openQuestion(getShell(), "Question", "Overwrite project " + projectName);
						}

					});
					if (!overwrite) {
						return Status.CANCEL_STATUS;
					}
					project.delete(true, true, monitor);
				}
				InputStream in = null;
				OutputStream out = null;
				File file = null;
				try {
					URL url = new URL(projectURL);
					URL entry = FileLocator.resolve(url);
					file = File.createTempFile("HelloWorld", ".zip");  //$NON-NLS-1$//$NON-NLS-2$
					file.deleteOnExit();
					in = entry.openStream();
					out = new FileOutputStream(file);
					copy(in,out);
				} catch (MalformedURLException e) {
					Activator.log(e);
					return Status.CANCEL_STATUS;
				} catch (IOException e) {
					Activator.log(e);
					return Status.CANCEL_STATUS;
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) {
							// ignore
						}
					}
					if (out != null) {
						try {
							out.close();
						} catch (IOException e) {
							// ignore
						}
					}
				}
				project.create(monitor);
				project.open(monitor);
				ZipFile sourceFile;
				try {
					sourceFile = new ZipFile(file);
				} catch (IOException e) {
					Activator.log(e);
					return Status.CANCEL_STATUS;
				}
				ZipFileStructureProvider structureProvider = new ZipFileStructureProvider(
						sourceFile);

				Enumeration<? extends ZipEntry> entries = sourceFile.entries();
				ZipEntry entry = null;
				List<ZipEntry> filesToImport = new ArrayList<ZipEntry>();
				String prefix = projectName + "/"; //$NON-NLS-1$
				while (entries.hasMoreElements()) {
					entry = entries.nextElement();
					if (entry.getName().startsWith(prefix)) {
						filesToImport.add(entry);
					}
				}

				ImportOperation operation = new ImportOperation(workspace
						.getRoot().getFullPath(), structureProvider.getRoot(),
						structureProvider, OVERWRITE_ALL_QUERY, filesToImport);
				operation.setContext(getShell());
				try {
					operation.run(new NullProgressMonitor());
				} catch (InvocationTargetException e) {
					Activator.log(e);
					return Status.CANCEL_STATUS;
				} catch (InterruptedException e) {
					Activator.log(e);
					return Status.CANCEL_STATUS;
				} finally {
					file.delete();
				}
				return Status.OK_STATUS;
			}
			
		};
		workspaceJob.setUser(true);
		
		workspaceJob.addJobChangeListener(new IJobChangeListener() {

			public void aboutToRun(IJobChangeEvent event) {

			}

			public void awake(IJobChangeEvent event) {

			}

			public void done(IJobChangeEvent event) {
				try {
					Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD,
							new NullProgressMonitor());
					IWorkspace workspace = ResourcesPlugin.getWorkspace();
					IProject project = workspace.getRoot().getProject(projectName);
					Sdk currentSdk = Sdk.getCurrent();
					if (currentSdk != null && project.isOpen()) {
						IJavaProject javaProject = JavaCore.create(project);
			            ProjectState state = Sdk.getProjectState(project);
			            IAndroidTarget projectTarget = state.getTarget();
			            if (projectTarget == null) {
			            	IAndroidTarget[] targets = Sdk.getCurrent().getTargets();
			            	if (targets != null && targets.length > 0) {
			            		IAndroidTarget newTarget = targets[0];
			            		ProjectPropertiesWorkingCopy mPropertiesWorkingCopy = 
			            			state.getProperties().makeWorkingCopy();
			            		mPropertiesWorkingCopy.setProperty(ProjectProperties.PROPERTY_TARGET,
			                            newTarget.hashString());
			            		try {
			                        mPropertiesWorkingCopy.save();
			                        IResource defaultProp = project.findMember(SdkConstants.FN_DEFAULT_PROPERTIES);
			                        defaultProp.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
			                        
			                        
			                        ProjectHelper.fixProject(project);
			                    } catch (Exception e) {
			                        String msg = String.format(
			                                "Failed to save %1$s for project %2$s",
			                                SdkConstants.FN_DEFAULT_PROPERTIES, project.getName());
			                        AdtPlugin.log(e, msg);
			                    }
			            	} else {
			            		Display.getDefault().syncExec(new Runnable() {
									
									public void run() {
										MessageDialog.openInformation(getShell(), 
												"Hello World target",
												"'Hello world' cheatsheet requires " +
												"an Android target. You can download " +
												"them using the 'Open SDK and AVD Manager'" +
												" action.");
									}
								});
			            	}
			            }
					}
					Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD,
							new NullProgressMonitor());
					if (params[2] != null) {
						new OpenFile().run(new String[] {params[1],"src", params[2]}, manager);
					}
				} catch (OperationCanceledException e) {
					Activator.log(e);
				} catch (InterruptedException e) {
					Activator.log(e);
				}
			}

			public void running(IJobChangeEvent event) {

			}

			public void scheduled(IJobChangeEvent event) {

			}

			public void sleeping(IJobChangeEvent event) {

			}

		});
		workspaceJob.schedule();
	}

	private void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[16 * 1024];
		int len;
		while ((len = in.read(buffer)) > 0) {
			out.write(buffer, 0, len);
		}
	}

	private static Shell getShell() {
		Display display = Display.getDefault();
		shell = null;
		display.syncExec(new Runnable() {

			public void run() {
				shell = Display.getCurrent().getActiveShell();
			}
			
		});
		return shell;
	}
}
