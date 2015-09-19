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
package rs.snpe.android.refactoring.changes;

import java.util.Map;

import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;

/**
 * @author Sara
 *
 */
public class AndroidTypeMoveChange extends AndroidTypeRenameChange {

	/**
	 * @param androidManifest
	 * @param manager
	 * @param document
	 * @param elements
	 * @param newName
	 * @param oldName
	 */
	public AndroidTypeMoveChange(IFile androidManifest,
			ITextFileBufferManager manager, IDocument document,
			Map<String, String> elements, String newName, String oldName) {
		super(androidManifest, manager, document, elements, newName, oldName);
	}

}
