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

import com.android.ide.eclipse.cheatsheets.Activator;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;


public class SetBreakpoint extends Action implements ICheatSheetAction {

	private static final String HANDLE_ID = JDIDebugUIPlugin.getUniqueIdentifier() + ".JAVA_ELEMENT_HANDLE_ID"; //$NON-NLS-1$

	private static final Object EMPTY_STRING = "";

	private ITextEditor editor;
	private IResource resource;
	
	public void run(final String[] params, ICheatSheetManager manager) {
		// param1 - project
		// param2 - path
		// param3 - type name
		// param4 - line number
		if(params == null || params[0] == null || params[1] == null || params[2] == null || params[3] == null) {
			return;
		}
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

		IProject project = workspaceRoot.getProject(params[0]);
		if (project == null || !project.isOpen()) {
			Activator.log("Invalid the project " + params[0] + ".");
			return;
		}
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject == null || !javaProject.isOpen()) {
			Activator.log("Invalid the project: " + params[0] + ".");
			return;
		}
		IJavaElement element;
		try {
			element = javaProject.findElement(new Path(params[1]));
			
			if (element == null) {
				Activator.log("Invalid the path: " + params[1] + ".");
				return;
			}
			resource = element.getCorrespondingResource();
			if (resource == null || resource.getType() != IResource.FILE) {
				Activator.log("Invalid the path: " + params[1] + ".");
				return;
			}
		} catch (JavaModelException e1) {
			Activator.log("Invalid the " + params[1] + " path.");
			return;
		}
		String tname = params[2];
		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				editor = openEditor(params, (IFile) resource);
			}
		});
		if (editor == null) {
			Activator.log("Cannot open the " + " " + params[0] + "file.");
			return;
		}
		try {
			//String markerType = "org.eclipse.jdt.debug.javaLineBreakpointMarker";
			int lnumber;
			try {
				lnumber = new Integer(params[3]).intValue();
			} catch (NumberFormatException e) {
				Activator.log("Invalid line number " + params[1]);
				return;
			}
			Map attributes = new HashMap(10);
			IDocumentProvider documentProvider = editor.getDocumentProvider();
			if (documentProvider == null) {
			    return;
			}
			IDocument document = documentProvider.getDocument(editor.getEditorInput());
			int charstart = -1, charend = -1;
			try {
				IRegion line = document.getLineInformation(lnumber - 1);
				charstart = line.getOffset();
				charend = charstart + line.getLength();
			} 	
			catch (BadLocationException e) {
				Activator.log(e);
			}
			//BreakpointUtils.addJavaBreakpointAttributes(attributes, type);
			
			String handleId = element.getHandleIdentifier();
			attributes.put(HANDLE_ID, handleId);
			JavaCore.addJavaElementMarkerAttributes(attributes, element);	
			IJavaLineBreakpoint breakpoint = JDIDebugModel.createLineBreakpoint(resource, tname, lnumber, charstart, charend, 0, true, attributes);
			
			IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
			breakpointManager.addBreakpoint(breakpoint);
		} catch (CoreException e) {
			Activator.log(e);
		}
	}

	/**
     * Returns the package qualified name, while accounting for the fact that a source file might
     * not have a project
     * @param type the type to ensure the package qualified name is created for
     * @return the package qualified name
     * @since 3.3
     */
    private String createQualifiedTypeName(IType type) {
    	String tname = pruneAnonymous(type);
    	try {
    		String packName = null;
    		if (type.isBinary()) {
    			packName = type.getPackageFragment().getElementName();
    		} else {
    			IPackageDeclaration[] pd = type.getCompilationUnit().getPackageDeclarations();
				if(pd.length > 0) {
					packName = pd[0].getElementName();
				}
    		}
			if(packName != null && !packName.equals(EMPTY_STRING)) {
				tname =  packName+"."+tname; //$NON-NLS-1$
			}
    	} 
    	catch (JavaModelException e) {}
    	return tname;
    }
    
    /**
     * Prunes out all naming occurrences of anonymous inner types, since these types have no names
     * and cannot be derived visiting an AST (no positive type name matching while visiting ASTs)
     * @param type
     * @return the compiled type name from the given {@link IType} with all occurrences of anonymous inner types removed
     * @since 3.4
     */
    private String pruneAnonymous(IType type) {
    	StringBuffer buffer = new StringBuffer();
    	IJavaElement parent = type;
    	while(parent != null) {
    		if(parent.getElementType() == IJavaElement.TYPE){
    			IType atype = (IType) parent;
    			try {
	    			if(!atype.isAnonymous()) {
	    				if(buffer.length() > 0) {
	    					buffer.insert(0, '$');
	    				}
	    				buffer.insert(0, atype.getElementName());
	    			}
    			}
    			catch(JavaModelException jme) {}
    		}
    		parent = parent.getParent();
    	}
    	return buffer.toString();
    }
    

	private ITextEditor openEditor(String[] params, IFile file) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor = null;
		try {
			if (params.length >= 4 && params[4] != null && params[4].trim().length() > 0) {
				String editorID = params[4];
				try {
					editor = IDE.openEditor(page, file, editorID, true);
				} catch (Exception e) {
					Activator.log(e);
				}
			}
			if (editor == null) {
				editor = IDE.openEditor(page, file, true);
			}
		} catch (PartInitException e) {
			setStatusMessage(page,"Cannot open the " + " " + params[0] + "file." );
		}
		ITextEditor textEditor = getTextEditor(editor);
		return textEditor;
	}

	private ITextEditor getTextEditor(IEditorPart editor) {
		if (editor instanceof ITextEditor) {
			return (ITextEditor) editor;
		}
		if (editor instanceof MultiPageEditorPart) {
			MultiPageEditorPart multiPageEditor = (MultiPageEditorPart) editor;
			IEditorPart[] editors = multiPageEditor.findEditors(editor.getEditorInput());
			for (int i = 0; i < editors.length; i++) {
				if (editors[i] instanceof ITextEditor) {
					ITextEditor textEditor = (ITextEditor) editors[i];
					if (textEditor.getDocumentProvider() != null) {
						return (ITextEditor) editors[i];
					}
				}
			}
		}
		return null;
	}
	
	private void setStatusMessage(IWorkbenchPage page,String message) {
		IWorkbenchPart activePart = page.getActivePart();
		IWorkbenchPartSite site = activePart.getSite();
		IActionBars actionBar = null;
		if (site instanceof IViewSite) {
			IViewSite viewSite = (IViewSite) site;
			actionBar = viewSite.getActionBars();
		} else if (site instanceof IEditorSite) {
			IEditorSite editorSite = (IEditorSite) site;
			actionBar = editorSite.getActionBars();
		}
		if (actionBar == null) {
			return;
		}
		IStatusLineManager lineManager = actionBar.getStatusLineManager();
		if (lineManager == null) {
			return;
		}
		lineManager.setMessage(message);
	}

}
