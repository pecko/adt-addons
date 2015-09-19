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
import java.util.Map;

import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import rs.snpe.android.refactoring.Activator;
import rs.snpe.android.refactoring.IConstants;

import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestParser;
import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper;
import com.android.ide.eclipse.adt.internal.project.XmlErrorHandler.BasicXmlErrorListener;

/**
 * @author Sara
 * 
 */
public class AndroidDocumentChange extends DocumentChange {

	protected IFile androidManifest;
	protected String javaPackage;
	protected IStructuredModel model;
	protected IDocument document;
	protected Map<String, String> elements;
	protected String newName;
	protected String oldName;
	protected boolean isPackage;
	protected ITextFileBufferManager manager;

	/**
	 * @param name
	 * @param document
	 */
	public AndroidDocumentChange(String name, IDocument document) {
		super(name, document);
	}

	@Override
	public String getName() {
		return AndroidConstants.FN_ANDROID_MANIFEST;
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		RefactoringStatus status = super.isValid(pm);
		if (model == null) {
			status.addFatalError("Invalid the "
					+ AndroidConstants.FN_ANDROID_MANIFEST + " file.");
		} else {
			javaPackage = getJavaPackage();
			if (javaPackage == null) {
				status.addFatalError("Invalid package in the "
						+ AndroidConstants.FN_ANDROID_MANIFEST + " file.");
			}
		}
		BasicXmlErrorListener errorListener = new BasicXmlErrorListener();
		AndroidManifestParser parser = BaseProjectHelper.parseManifestForError(
				androidManifest, errorListener);

		if (errorListener.mHasXmlError == true) {
			status.addFatalError("Invalid the "
					+ AndroidConstants.FN_ANDROID_MANIFEST + " file.");
		}
		return status;
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
					if (value != null) {
						String fullName = Activator.combinePackageAndClassName(
								getJavaPackage(), value);
						if (fullName != null && fullName.equals(oldName)) {
							return attribute;
						}
					}
				}
			}
		}
		return null;
	}

	protected String getElementAttribute(IDOMDocument xmlDoc, String element,
			String argument) {
		NodeList nodes = xmlDoc.getElementsByTagName(element);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			NamedNodeMap attributes = node.getAttributes();
			if (attributes != null) {
				Node attributeNode = attributes.getNamedItem(argument);
				if (attributeNode != null || attributeNode instanceof Attr) {
					Attr attribute = (Attr) attributeNode;
					return attribute.getValue();
				}
			}
		}
		return null;
	}

	protected IStructuredModel getModel(IDocument document) throws IOException,
			CoreException {
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
	public void setTextType(String type) {
		super.setTextType(androidManifest.getFileExtension());
	}

	protected IDOMDocument getDOMDocument() {
		IDOMModel xmlModel = (IDOMModel) model;
		IDOMDocument xmlDoc = xmlModel.getDocument();
		return xmlDoc;
	}

	protected String getJavaPackage() {
		if (javaPackage == null) {
			IDOMDocument xmlDoc = getDOMDocument();
			javaPackage = getElementAttribute(xmlDoc,
					IConstants.ANDROID_MANIFEST_ELEMENT,
					IConstants.ANDROID_PACKAGE_ARGUMENT);
		}
		return javaPackage;
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
		return createTextEdit(elementName, argumentName, oldName, newName, true);
	}
	
	protected TextEdit createTextEdit(String elementName, String argumentName,
			String oldName, String newName, boolean combinePackage) {
		IDOMDocument xmlDoc = getDOMDocument();
		String name = null;
		Attr attr = findAttribute(xmlDoc, elementName, argumentName, oldName);
		if (attr != null) {
			name = attr.getValue();
		}
		if (name != null) {
			String newValue;
			if (combinePackage) {
				newValue = Activator.getNewValue(getJavaPackage(), name,
					newName);
			} else {
				newValue = newName;
			}
			if (newValue != null) {
				TextEdit edit = createTextEdit(attr, newValue);
				return edit;
			}
		}
		return null;
	}

}
