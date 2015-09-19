package com.android.ide.eclipse.binedit;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class FileStorage implements IStorage {

	private File mFile = null;

	public FileStorage(File file) {
		mFile = file;
	}

	public boolean equals(Object obj) {
		if (obj instanceof FileStorage) {
			return mFile.equals(((FileStorage) obj).mFile);
		}
		return super.equals(obj);
	}

	public InputStream getContents() throws CoreException {
		InputStream stream = null;
		try {
			stream = new FileInputStream(mFile);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator
					.getDefault().getBundle().getSymbolicName(), IStatus.ERROR,
					mFile.getAbsolutePath(), e));
		}
		return stream;
	}

	public IPath getFullPath() {
		return new Path(mFile.getAbsolutePath());
	}

	public String getName() {
		return mFile.getName();
	}

	public boolean isReadOnly() {
		return true;
	}

	public Object getAdapter(Class adapter) {
		return null;
	}
}
