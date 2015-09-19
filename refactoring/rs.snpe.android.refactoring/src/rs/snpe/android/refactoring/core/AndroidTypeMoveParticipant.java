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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import rs.snpe.android.refactoring.Activator;
import rs.snpe.android.refactoring.IConstants;
import rs.snpe.android.refactoring.changes.AndroidLayoutChange;
import rs.snpe.android.refactoring.changes.AndroidLayoutChangeDescription;
import rs.snpe.android.refactoring.changes.AndroidLayoutFileChanges;
import rs.snpe.android.refactoring.changes.AndroidTypeMoveChange;

import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestParser;

/**
 * @author Sara
 *
 */
public class AndroidTypeMoveParticipant extends MoveParticipant {
	
	protected IFile androidManifest;
	protected ITextFileBufferManager manager;
	protected String oldName;
	protected String newName;
	protected IDocument document;
	protected String javaPackage;
	protected Map<String, String> androidElements;
	private Set<AndroidLayoutFileChanges> fileChanges = new HashSet<AndroidLayoutFileChanges>();
	//private String layoutNewName;
	

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#checkConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext)
	 */
	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm,
			CheckConditionsContext context) throws OperationCanceledException {
		return new RefactoringStatus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		if (pm.isCanceled()) {
			return null;
		}
		if (!getArguments().getUpdateReferences())
			return null;
		CompositeChange result = new CompositeChange(getName());
		if (androidManifest.exists()) {
			if (androidElements.size() > 0) {
				getDocument();
				Change change = new AndroidTypeMoveChange(androidManifest, manager, document, androidElements, newName, oldName);
				if (change != null) {
					result.add(change);
				}
			}
			
			for (AndroidLayoutFileChanges fileChange:fileChanges) {
				IFile file = fileChange.getFile();
				ITextFileBufferManager lManager = FileBuffers.getTextFileBufferManager();
				lManager.connect(file.getFullPath(), LocationKind.NORMALIZE, new NullProgressMonitor());
				ITextFileBuffer buffer = lManager.getTextFileBuffer(file.getFullPath(), LocationKind.NORMALIZE);
				IDocument lDocument = buffer.getDocument();
				Change layoutChange = new AndroidLayoutChange(file, lDocument, lManager, fileChange.getChanges());
				if (layoutChange != null) {
					result.add(layoutChange);
				}
			}
		}
		return (result.getChildren().length == 0) ? null : result;

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

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#getName()
	 */
	@Override
	public String getName() {
		return "Android Type Move";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#initialize(java.lang.Object)
	 */
	@Override
	protected boolean initialize(Object element) {

		if (element instanceof IType) {
			IType type = (IType) element;
			IJavaProject javaProject = (IJavaProject) type.getAncestor(IJavaElement.JAVA_PROJECT);
			IProject project = javaProject.getProject();
			IResource manifestResource = project .findMember(
                    AndroidConstants.WS_SEP + AndroidConstants.FN_ANDROID_MANIFEST);

            if (manifestResource == null || !manifestResource.exists() || !(manifestResource instanceof IFile)) {
                Activator.logInfo("Invalid or missing the " + AndroidConstants.FN_ANDROID_MANIFEST + " in the " + project.getName() + " project.");
                return false;
            }
            androidManifest = (IFile) manifestResource;
            AndroidManifestParser parser;
            try {
				parser = AndroidManifestParser.parseForData(androidManifest);
			} catch (CoreException e) {
				Activator.logInfo("Invalid or missing the " + AndroidConstants.FN_ANDROID_MANIFEST + " in the " + project.getName() + " project.");
                return false;
			}
			javaPackage = parser.getPackage();
			oldName = type.getFullyQualifiedName();
			Object destination = getArguments().getDestination();
			if (destination instanceof IPackageFragment) {
				IPackageFragment packageFragment = (IPackageFragment) destination;
				newName = packageFragment.getElementName() + "." + type.getElementName();
			} 
			if (oldName == null || newName == null) {
				return false;
			}
			androidElements = addAndroidElements();
			try {
				ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(null);
				if (typeHierarchy == null) {
					return false;
				}
				IType[] superTypes = typeHierarchy.getAllSuperclasses(type);
				for (int i = 0; i < superTypes.length; i++) {
					IType superType = superTypes[i];
					String className = superType.getFullyQualifiedName();
					if (className.equals(AndroidConstants.CLASS_VIEW)) {
						addLayoutChanges(project, type.getFullyQualifiedName());
						break;
					}
				}
			} catch (JavaModelException ignore) {
			}
			return androidElements.size() > 0 || fileChanges.size() > 0;
		}
		return false;
	}

	/**
	 * @param project 
	 * @param className
	 * @return 
	 */
	private void addLayoutChanges(IProject project, String className) {
		try {
			IFolder resFolder = project.getFolder(IConstants.FD_RESOURCES);
			IFolder layoutFolder = resFolder.getFolder(IConstants.FD_LAYOUT);
			IResource[] members = layoutFolder.members();
			for (int i = 0; i < members.length; i++) {
				IResource member = members[i];
				if ( (member instanceof IFile) && member.exists()) {
					IFile file = (IFile) member;
					Set<AndroidLayoutChangeDescription> changes = parse(file, className);
					if (changes.size() > 0) {
						AndroidLayoutFileChanges fileChange = new AndroidLayoutFileChanges(file);
						fileChange.getChanges().addAll(changes);
						fileChanges.add(fileChange);
					}
				}
			}
		} catch (CoreException e) {
			Activator.log(e);
		}
	}

	/**
	 * @param file
	 * @param className
	 */
	private Set<AndroidLayoutChangeDescription> parse(IFile file, String className) {
		Set<AndroidLayoutChangeDescription> changes = new HashSet<AndroidLayoutChangeDescription>();
		ITextFileBufferManager lManager = null;
		try {
			lManager = FileBuffers.getTextFileBufferManager();
			lManager.connect(file.getFullPath(), LocationKind.NORMALIZE, new NullProgressMonitor());
			ITextFileBuffer buffer = lManager.getTextFileBuffer(file.getFullPath(), LocationKind.NORMALIZE);
			IDocument lDocument = buffer.getDocument();
			IStructuredModel model = null;
			try {
				model = StructuredModelManager.getModelManager().getExistingModelForRead(lDocument);
				if (model == null) {
					if (lDocument instanceof IStructuredDocument) {
						IStructuredDocument structuredDocument = (IStructuredDocument) lDocument;
						model = StructuredModelManager.getModelManager().getModelForRead(structuredDocument);
					}
				}
				if (model != null) {
					IDOMModel xmlModel = (IDOMModel) model;
					IDOMDocument xmlDoc = xmlModel.getDocument();
					NodeList nodes = xmlDoc.getElementsByTagName(IConstants.ANDROID_LAYOUT_VIEW_ELEMENT);
					for (int i = 0; i < nodes.getLength(); i++) {
						Node node = nodes.item(i);
						NamedNodeMap attributes = node.getAttributes();
						if (attributes != null) {
							Node attributeNode = attributes.getNamedItem(IConstants.ANDROID_LAYOUT_CLASS_ARGUMENT);
							if (attributeNode != null ||  attributeNode instanceof Attr) {
								Attr attribute = (Attr) attributeNode;
								String value = attribute.getValue();
								if (value != null && value.equals(className)) {
									AndroidLayoutChangeDescription layoutChange = new AndroidLayoutChangeDescription(className, newName, AndroidLayoutChangeDescription.VIEW_TYPE);
									changes.add(layoutChange);
								}
							}
						}
					}
					nodes = xmlDoc.getElementsByTagName(className);
					for (int i = 0; i < nodes.getLength(); i++) {
						AndroidLayoutChangeDescription layoutChange = new AndroidLayoutChangeDescription(className, newName, AndroidLayoutChangeDescription.STANDALONE_TYPE);
						changes.add(layoutChange);
					}
				}
			} finally {
				if (model != null) {
					model.releaseFromRead();
				}
			}
			
		} catch (CoreException ignore) {
		} finally {
			if (lManager != null) {
				try {
					lManager.disconnect(file.getFullPath(), LocationKind.NORMALIZE, new NullProgressMonitor());
				} catch (CoreException ignore) {
				}
			}
		}
		return changes;
	}


	/**
	 * @return
	 * @throws CoreException 
	 */
	private Map<String, String> addAndroidElements() {
		Map<String, String> androidElements = new HashMap<String, String>();
		
		IDocument document;
		try {
			document = getDocument();
		} catch (CoreException e) {
			Activator.log(e);
			if (manager != null) {
				try {
					manager.disconnect(androidManifest.getFullPath(), LocationKind.NORMALIZE, new NullProgressMonitor());
				} catch (CoreException e1) {
					Activator.log(e1);
				}
			}
			document = null;
			return androidElements;
		}
		
		IStructuredModel model = null;
		try {
			model = StructuredModelManager.getModelManager().getExistingModelForRead(document);
			if (model == null) {
				if (document instanceof IStructuredDocument) {
					IStructuredDocument structuredDocument = (IStructuredDocument) document;
					model = StructuredModelManager.getModelManager().getModelForRead(structuredDocument);
				}
			}
			if (model != null) {
				IDOMModel xmlModel = (IDOMModel) model;
				IDOMDocument xmlDoc = xmlModel.getDocument();
				add(xmlDoc, androidElements, IConstants.ANDROID_ACTIVITY_ELEMENT, IConstants.ANDROID_NAME_ARGUMENT);
				add(xmlDoc, androidElements, IConstants.ANDROID_APPLICATION_ELEMENT, IConstants.ANDROID_NAME_ARGUMENT);
				add(xmlDoc, androidElements, IConstants.ANDROID_PROVIDER_ELEMENT, IConstants.ANDROID_NAME_ARGUMENT);
				add(xmlDoc, androidElements, IConstants.ANDROID_RECEIVER_ELEMENT, IConstants.ANDROID_NAME_ARGUMENT);
				add(xmlDoc, androidElements, IConstants.ANDROID_SERVICE_ELEMENT, IConstants.ANDROID_NAME_ARGUMENT);
			}
		} finally {
			if (model != null) {
				model.releaseFromRead();
			}
		}
		
		return androidElements;
	}

	/**
	 * @param xmlDoc 
	 * @param androidElements
	 * @param androidActivityElement
	 */
	private void add(IDOMDocument xmlDoc, Map<String, String> androidElements, String element, String argument) {
		NodeList nodes = xmlDoc.getElementsByTagName(element);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			NamedNodeMap attributes = node.getAttributes();
			if (attributes != null) {
				Node attributeNode = attributes.getNamedItem(argument);
				if (attributeNode != null ||  attributeNode instanceof Attr) {
					Attr attribute = (Attr) attributeNode;
					String value = attribute.getValue();
					if (value != null) {
						String fullName = Activator.combinePackageAndClassName(javaPackage, value);
						if (fullName != null && fullName.equals(oldName)) {
					        androidElements.put(element, value);
						}
					}
				}
			}
		}	
	}

}
