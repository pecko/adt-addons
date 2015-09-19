package rs.snpe.android.refactoring.changes;

import java.util.Map;
import java.util.Set;

import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import rs.snpe.android.refactoring.Activator;
import rs.snpe.android.refactoring.IConstants;
import rs.snpe.android.refactoring.core.FixImportsJob;


public class AndroidPackageRenameChange extends AndroidDocumentChange {

	public AndroidPackageRenameChange(IFile androidManifest, ITextFileBufferManager manager, IDocument document,
			Map<String, String> elements, 
			String newName, String oldName,
			boolean isPackage) {
		super("", document);
		this.document = document;
		this.isPackage = isPackage;
		this.elements = elements;
		this.newName = newName;
		this.oldName = oldName;
		this.manager = manager;
		this.androidManifest = androidManifest;
		try {
			this.model = getModel(document);
		} catch (Exception ignore) {
		}
		if (model != null) {
			addEdits();
		}
	}

	private void addEdits() {
		MultiTextEdit multiEdit = new MultiTextEdit();
		
		if (isPackage) {
			TextEdit edit = createTextEdit(IConstants.ANDROID_MANIFEST_ELEMENT, 
					IConstants.ANDROID_PACKAGE_ARGUMENT,
					oldName, newName, false);
			if (edit != null) {
				multiEdit.addChild(edit);
			}
		}
		Set<String> keys = elements.keySet();
		for (String key:keys) {
			String value = elements.get(key);
			String oldValue = Activator.combinePackageAndClassName(javaPackage, value);
			String newValue = oldValue.replaceFirst(oldName, newName);
			TextEdit edit = createTextEdit(key, IConstants.ANDROID_NAME_ARGUMENT, oldValue, newValue);
			if (edit != null) {
				multiEdit.addChild(edit);
			}
			if (IConstants.ANDROID_ACTIVITY_ELEMENT.equals(key)) {
				TextEdit alias = createTextEdit(IConstants.ANDROID_ACTIVITY_ALIAS, IConstants.ANDROID_TARGET_ACTIVITY, oldValue, newValue);
				if (alias != null) {
					multiEdit.addChild(alias);
				}
			}
		}
		setEdit(multiEdit);
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		super.perform(pm);
		return new AndroidPackageRenameChange(androidManifest, manager, document,
				elements, oldName, newName, isPackage);
	}

	@Override
	public void dispose() {
		super.dispose();
		Activator.fixModel(model, document);
		if (manager != null) {
			try {
				manager.disconnect(androidManifest.getFullPath(), LocationKind.NORMALIZE, new NullProgressMonitor());
			} catch (CoreException e) {
				Activator.log(e);
			}
		}
		if (isPackage) {
			new FixImportsJob("Fix Rename Package", androidManifest, newName).schedule(500);
		}
	}
	
}
