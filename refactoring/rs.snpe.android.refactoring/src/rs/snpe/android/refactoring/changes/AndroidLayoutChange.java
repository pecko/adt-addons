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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import rs.snpe.android.refactoring.Activator;
import rs.snpe.android.refactoring.IConstants;

/**
 * @author Sara
 *
 */
public class AndroidLayoutChange extends DocumentChange {

	private IDocument document;
	private ITextFileBufferManager manager;
	private IFile file;
	private IStructuredModel model;
	private Set<AndroidLayoutChangeDescription> changes;

	/**
	 * @param file
	 * @param lDocument
	 * @param lManager
	 * @param androidLayoutChangeDescription
	 */
	public AndroidLayoutChange(IFile file, IDocument document,
			ITextFileBufferManager manager,
			Set<AndroidLayoutChangeDescription> changes) {
		super("", document);
		this.file = file;
		this.document = document;
		this.manager = manager;
		this.changes = changes;
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
		for(AndroidLayoutChangeDescription change:changes) {
			if (!change.isStandalone()) {
				TextEdit edit = createTextEdit(IConstants.ANDROID_LAYOUT_VIEW_ELEMENT, IConstants.ANDROID_LAYOUT_CLASS_ARGUMENT, change.getClassName(), change.getNewName());
				if (edit != null) {
					multiEdit.addChild(edit);
				}			
			} else {
				List<TextEdit> edits = createElementTextEdit(change.getClassName(), change.getNewName());
				for (TextEdit edit:edits) {
					multiEdit.addChild(edit);
				}
			}
		}
		setEdit(multiEdit);
	}
	
	/**
	 * @param className
	 * @param newName
	 * @return
	 */
	private List<TextEdit> createElementTextEdit(String className, String newName) {
		IDOMDocument xmlDoc = getDOMDocument();
		List<TextEdit> edits = new ArrayList<TextEdit>();
		NodeList nodes = xmlDoc.getElementsByTagName(className);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node instanceof IDOMElement) {
				IDOMElement domNode = (IDOMElement) node;
				IStructuredDocumentRegion firstRegion = domNode.getFirstStructuredDocumentRegion();
				if (firstRegion != null) {
					int offset = firstRegion.getStartOffset();
					edits.add(new ReplaceEdit(offset + 1 , className.length(), newName));
				}
				IStructuredDocumentRegion endRegion = domNode.getEndStructuredDocumentRegion();
				if (endRegion != null) {
					int offset = endRegion.getStartOffset();
					edits.add(new ReplaceEdit(offset + 2 , className.length(), newName));
				}
			}
			
		}
		return edits;
	}

	protected IDOMDocument getDOMDocument() {
		IDOMModel xmlModel = (IDOMModel) model;
		IDOMDocument xmlDoc = xmlModel.getDocument();
		return xmlDoc;
	}
	
	protected TextEdit createTextEdit(Attr attribute, String newValue) {
		if (attribute == null)
			return null;

		if (attribute instanceof IDOMAttr) {
			IDOMAttr domAttr = (IDOMAttr) attribute;
			String region = domAttr.getValueRegionText();
			int offset = domAttr.getValueRegionStartOffset();
			if (region != null && region.length() >= 2) {
				return new ReplaceEdit(offset + 1, region.length() - 2,
						newValue);
			}
		}
		return null;
	}

	protected TextEdit createTextEdit(String elementName, String argumentName,
			String oldName, String newName) {
		IDOMDocument xmlDoc = getDOMDocument();
		String name = null;
		Attr attr = findAttribute(xmlDoc, elementName, argumentName, oldName);
		if (attr != null) {
			name = attr.getValue();
		}
		if (name != null && newName != null) {
			TextEdit edit = createTextEdit(attr, newName);
			return edit;
		}
		return null;
	}

	public Attr findAttribute(IDOMDocument xmlDoc, String element,
			String attributeName, String oldName) {
		NodeList nodes = xmlDoc.getElementsByTagName(element);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			NamedNodeMap attributes = node.getAttributes();
			if (attributes != null) {
				Node attributeNode = attributes.getNamedItem(attributeName);
				if (attributeNode != null || attributeNode instanceof Attr) {
					Attr attribute = (Attr) attributeNode;
					String value = attribute.getValue();
					if (value != null && value.equals(oldName)) {
						return attribute;
					}
				}
			}
		}
		return null;
	}


	@Override
	public String getName() {
		return file.getName();
	}
	
	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		RefactoringStatus status = super.isValid(pm);
		if (model == null) {
			status.addFatalError("Invalid the "
					+ getName() + " file.");
		}
		return status;
	}

	@Override
	public void setTextType(String type) {
		super.setTextType(file.getFileExtension());
	}
	
	protected IStructuredModel getModel(IDocument document) throws IOException, CoreException {
		if (model != null) {
			return model;
		}
		IStructuredModel model;
		model = StructuredModelManager.getModelManager()
				.getExistingModelForRead(document);
		if (model == null) {
			if (document instanceof IStructuredDocument) {
				IStructuredDocument structuredDocument = (IStructuredDocument) document;
				model = StructuredModelManager.getModelManager()
						.getModelForRead(structuredDocument);
			}
		}
		return model;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		Activator.fixModel(model, document);
		if (manager != null) {
			try {
				manager.disconnect(file.getFullPath(), LocationKind.NORMALIZE, new NullProgressMonitor());
			} catch (CoreException e) {
				Activator.log(e);
			}
		}
	}
}
