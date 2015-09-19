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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;

/**
 * @author Sara
 *
 */
public class AndroidLayoutFileChanges {
	private IFile file;
	private Set<AndroidLayoutChangeDescription> changes = new HashSet<AndroidLayoutChangeDescription>();
	

	public AndroidLayoutFileChanges(IFile file) {
		this.file = file;
	}
	public IFile getFile() {
		return file;
	}
	public Set<AndroidLayoutChangeDescription> getChanges() {
		return changes;
	}
	
}
