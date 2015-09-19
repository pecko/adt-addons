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

import java.util.Map;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

/**
 * @author Sara
 *
 */
public abstract class AndroidRenameParticipant extends RenameParticipant {
	
	protected IFile androidManifest;
	protected ITextFileBufferManager manager;
	protected String oldName;
	protected String newName;
	protected IDocument document;
	protected String javaPackage;
	protected Map<String, String> androidElements;

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#checkConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext)
	 */
	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm,
			CheckConditionsContext context) throws OperationCanceledException {
		return new RefactoringStatus();
	}

	public IDocument getDocument() throws CoreException {
		if (document == null) {
			manager = FileBuffers.getTextFileBufferManager();
			manager.connect(androidManifest.getFullPath(), LocationKind.NORMALIZE, new NullProgressMonitor());
			ITextFileBuffer buffer = manager.getTextFileBuffer(androidManifest.getFullPath(), LocationKind.NORMALIZE);
			document = buffer.getDocument();
		}
		return document;
	}

	public IFile getAndroidManifest() {
		return androidManifest;
	}

}
