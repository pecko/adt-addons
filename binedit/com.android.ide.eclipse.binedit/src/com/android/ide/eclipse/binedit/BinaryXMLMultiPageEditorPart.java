package com.android.ide.eclipse.binedit;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JarEntryFile;
import org.eclipse.jdt.internal.ui.javaeditor.JarEntryEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.xml.ui.internal.tabletree.XMLMultiPageEditorPart;

import java.io.File;
import java.net.URL;

public class BinaryXMLMultiPageEditorPart extends XMLMultiPageEditorPart {

	@Override
	protected void setInput(IEditorInput input) {
		if (input instanceof JarEntryEditorInput) {
			JarEntryEditorInput jarInput = (JarEntryEditorInput) input;
			IStorage storage = jarInput.getStorage();
			if (storage instanceof JarEntryFile) {
				JarEntryFile jarEntryFile = (JarEntryFile) storage;
				IPackageFragmentRoot fragmentRoot = jarEntryFile.getPackageFragmentRoot();
				if (fragmentRoot == null) {
					super.setInput(input);
					return;
				}
				IPath path = fragmentRoot.getPath();
				if (path == null) {
					super.setInput(input);
					return;
				}
				path = path.removeLastSegments(1);
				IPath filePath = path.append("data").append(jarEntryFile.getFullPath().toPortableString());
				File file = new File(filePath.toOSString());
				if ( !(file.isFile()) ) {
					super.setInput(input);
					return;
				}
				try {
					XmlStorageEditorInput newInput = new XmlStorageEditorInput(new FileStorage(file));
					super.setInput(newInput);
					return;
				} catch (Exception e) {
					// TODO log
				}
			}
		}
		super.setInput(input);
	}

}
