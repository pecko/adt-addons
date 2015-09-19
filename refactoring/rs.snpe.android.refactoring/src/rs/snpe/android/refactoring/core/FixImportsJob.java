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
package rs.snpe.android.refactoring.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import rs.snpe.android.refactoring.Activator;

/**
 * @author Sara
 * 
 */
public class FixImportsJob extends WorkspaceJob {

	private IFile androidManifest;
	private String javaPackage;

	public FixImportsJob(String name, IFile androidManifest, String javaPackage) {
		super(name);
		this.androidManifest = androidManifest;
		this.javaPackage = javaPackage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core
	 * .runtime.IProgressMonitor)
	 */
	@Override
	public IStatus runInWorkspace(final IProgressMonitor monitor)
			throws CoreException {
		if (javaPackage == null || androidManifest == null || !androidManifest.exists()) {
			return Status.CANCEL_STATUS;
		}
		IProject project = androidManifest.getProject();
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject == null || !javaProject.isOpen()) {
			return Status.CANCEL_STATUS;
		}
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true,
				IResource.DEPTH_INFINITE);
		for (int i = 0; i < markers.length; i++) {
			IMarker marker = markers[i];
			IResource resource = marker.getResource();
			try {
				IJavaElement element = JavaCore.create(resource);
				if (element != null && (element instanceof ICompilationUnit)) {
					final ICompilationUnit cu = (ICompilationUnit) element;
					IPackageFragment packageFragment = (IPackageFragment) cu.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
					if (packageFragment != null && packageFragment.exists()) {
						String packageName = packageFragment.getElementName();
						if (packageName != null && packageName.startsWith(javaPackage)) {
							CompilationUnit astRoot = SharedASTProvider.getAST(cu, SharedASTProvider.WAIT_ACTIVE_ONLY, null);
							CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings(cu.getJavaProject());
							final boolean hasAmbiguity[]= new boolean[] { false };
							IChooseImportQuery query= new IChooseImportQuery() {
								public TypeNameMatch[] chooseImports(TypeNameMatch[][] openChoices, ISourceRange[] ranges) {
									hasAmbiguity[0]= true;
									return new TypeNameMatch[0];
								}
							};
							final OrganizeImportsOperation op = new OrganizeImportsOperation(cu, astRoot, settings.importIgnoreLowercase, !cu.isWorkingCopy(), true, query);
							Display.getDefault().asyncExec(new Runnable() {

								public void run() {
									try {
										IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
										progressService.run(true, true, new WorkbenchRunnableAdapter(op, op.getScheduleRule()));
										IEditorPart openEditor= EditorUtility.isOpenInEditor(cu);
										if (openEditor != null) {
											openEditor.doSave(monitor);
										}
									} catch (Throwable e) {
										Activator.log(e);
									}
								}
							});

						}
					}
				}
			} catch (Throwable e) {
				Activator.log(e);
			} 
		}
		return Status.OK_STATUS;
	}

}
