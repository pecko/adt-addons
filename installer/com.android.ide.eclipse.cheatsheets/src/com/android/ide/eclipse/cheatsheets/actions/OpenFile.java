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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
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
import org.eclipse.ui.texteditor.ITextEditor;

public class OpenFile extends Action implements ICheatSheetAction {

	public void run(final String[] params, ICheatSheetManager manager) {
		if(params == null || params[2] == null ) {
			return;
		}
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		
		IPath path = new Path(params[2]);
		
		final IFile file = workspaceRoot.getFile(path);
		Display.getDefault().syncExec(new Runnable() {
			
			public void run() {
				openEditor(params, file);
			}
		});
		
	}

	private void openEditor(String[] params, IFile file) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor = null;
		try {
			if (params.length >= 4 && params[4] != null && params[4].trim().length() > 0) {
				String editorID = params[4];
				try {
					editor = IDE.openEditor(page, file, editorID, true);
				} catch (Exception e) {
				}
			}
			if (editor == null) {
				editor = IDE.openEditor(page, file, true);
			}
		} catch (PartInitException e) {
			setStatusMessage(page,"Cannot open the " + " " + params[0] + "file." );
			return;
		}
		ITextEditor textEditor = getTextEditor(editor);
		if (params[1] != null && textEditor != null) {
			try {
				int lineStart = Integer.parseInt(params[1]);
				int lineEnd = lineStart;
				if (params[2] != null) {
					lineEnd = Integer.parseInt(params[2]);
				}
				IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
				IRegion lineInfoStart = document.getLineInformation(lineStart-1);
				IRegion lineInfoEnd = document.getLineInformation(lineEnd-1);
				textEditor.selectAndReveal(lineInfoStart.getOffset(), lineInfoEnd.getOffset() - lineInfoStart.getOffset() + lineInfoEnd.getLength());
			} catch (Exception e) {
				setStatusMessage(page, e.getLocalizedMessage());
			}
			
		}
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
