package com.android.ide.eclipse.binedit;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public class XmlStorageEditorInput implements IStorageEditorInput {

	IStorage mStorage = null;

	public XmlStorageEditorInput(IStorage storage) {
		mStorage = storage;
	}

	public IStorage getStorage() throws CoreException {
		return mStorage;
	}

	public boolean exists() {
		return mStorage != null;
	}

	public boolean equals(Object obj) {
		if (obj instanceof XmlStorageEditorInput) {
			return mStorage.equals(((XmlStorageEditorInput) obj).mStorage);
		}
		return super.equals(obj);
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return mStorage.getName();
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return mStorage.getFullPath() != null ? mStorage.getFullPath().toString() : mStorage.getName();
	}

	public Object getAdapter(Class adapter) {
		return null;
	}

}
