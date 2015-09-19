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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.manipulation.JavaManipulation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenamePackageChange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.SearchUtils;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
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
import rs.snpe.android.refactoring.changes.AndroidPackageRenameChange;

import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestParser;

/**
 * @author Sara
 *
 */
public class AndroidPackageRenameParticipant extends AndroidRenameParticipant {

	private IPackageFragment packageFragment;
	private boolean isPackage;
	private Set<AndroidLayoutFileChanges> fileChanges = new HashSet<AndroidLayoutFileChanges>();

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
		IPath pkgPath = packageFragment.getPath();
		IJavaProject javaProject = (IJavaProject) packageFragment.getAncestor(IJavaElement.JAVA_PROJECT);
		IProject project = javaProject.getProject();
		IPath genPath = project.getFullPath().append(IConstants.FD_GEN_SOURCES);
		if (genPath.isPrefixOf(pkgPath)) {
			Activator.logInfo(getName() + ": Cannot rename generated package.");
			return null;
		}
		CompositeChange result = new CompositeChange(getName());
		if (androidManifest.exists()) {
			if (androidElements.size() > 0 || isPackage) {
				getDocument();
				Change change = new AndroidPackageRenameChange(androidManifest, manager, document, androidElements, newName, oldName, isPackage);
				if (change != null) {
					result.add(change);
				}
			}
			if (isPackage) {
				Change genChange = getGenPackageChange(pm);
				if (genChange != null) {
					result.add(genChange);
				}
			}
			// add layoutChange
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

	public Change getGenPackageChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		if (isPackage) {
			IPackageFragment genJavaPackageFragment = getGenPackageFragment();
			if (genJavaPackageFragment != null && genJavaPackageFragment.exists()) {
				return new RenamePackageChange(genJavaPackageFragment, newName, true);
			}
		}
		return null;
	}

	private IPackageFragment getGenPackageFragment() throws JavaModelException {
		IJavaProject javaProject = (IJavaProject) packageFragment.getAncestor(IJavaElement.JAVA_PROJECT);
		if (javaProject != null && javaProject.isOpen()) {
			IProject project = javaProject.getProject();
			IFolder genFolder = project.getFolder(IConstants.FD_GEN_SOURCES);
			if (genFolder.exists()) {
				String javaPackagePath = javaPackage.replace(".", "/");
				IPath genJavaPackagePath = genFolder.getFullPath().append(javaPackagePath);
				IPackageFragment genPackageFragment = javaProject.findPackageFragment(genJavaPackagePath);
				return genPackageFragment;
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#getName()
	 */
	@Override
	public String getName() {
		return "Android Package Rename";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#initialize(java.lang.Object)
	 */
	@Override
	protected boolean initialize(final Object element) {
		isPackage = false;
		try {
			if (element instanceof IPackageFragment) {
				packageFragment = (IPackageFragment) element;
				if (!packageFragment.containsJavaResources())
					return false;
				IJavaProject javaProject = (IJavaProject) packageFragment.getAncestor(IJavaElement.JAVA_PROJECT);
				IProject project = javaProject.getProject();
				IResource manifestResource = project .findMember(
	                    AndroidConstants.WS_SEP + AndroidConstants.FN_ANDROID_MANIFEST);

	            if (manifestResource == null || !manifestResource.exists() || !(manifestResource instanceof IFile)) {
	                Activator.logInfo("Invalid or missing the " + AndroidConstants.FN_ANDROID_MANIFEST + " in the " + project.getName() + " project.");
	                return false;
	            }
	            androidManifest = (IFile) manifestResource;
	            String packageName = packageFragment.getElementName();
	            AndroidManifestParser parser;
	            try {
					parser = AndroidManifestParser.parseForData(androidManifest);
				} catch (CoreException e) {
					Activator.logInfo("Invalid or missing the " + AndroidConstants.FN_ANDROID_MANIFEST + " in the " + project.getName() + " project.");
	                return false;
				}
				javaPackage = parser.getPackage();
				oldName = packageName;
				newName = getArguments().getNewName();
				if (oldName == null || newName == null) {
					return false;
				}
				
				if (javaPackage != null && javaPackage.equals(packageName)) {
					isPackage = true;
				}
				androidElements = addAndroidElements();
				try {
					final IType type = javaProject.findType(AndroidConstants.CLASS_VIEW);
					SearchPattern pattern = SearchPattern.createPattern("*", IJavaSearchConstants.TYPE, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_REGEXP_MATCH);
					IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {packageFragment});
					final HashSet<IType> elements = new HashSet<IType>();
					SearchRequestor requestor= new SearchRequestor() {
						public void acceptSearchMatch(SearchMatch match) throws CoreException {
							Object elem = match.getElement();
							if (elem instanceof IType) {
								IType eType = (IType) elem;
								IType[] superTypes = JavaModelUtil.getAllSuperTypes(eType, new NullProgressMonitor());
								for (int i = 0; i < superTypes.length; i++) {
									if (superTypes[i].equals(type)) {
										elements.add(eType);
										break;
									}
								}
							}
							
						}
					};
					SearchEngine searchEngine = new SearchEngine();
					searchEngine.search(pattern, new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, scope, requestor, null);
					List<String> views = new ArrayList<String>();
					for (IType elem:elements) {
						views.add(elem.getFullyQualifiedName());
					}
					if (views.size() > 0) {
						String[] classNames = views.toArray(new String[0]);
						addLayoutChanges(project, classNames);
					}
				} catch (CoreException e) {
					Activator.log(e);
				}

			    
				return isPackage || androidElements.size() > 0 || fileChanges.size() > 0;
			}
		} catch (JavaModelException ignore) {
		}
		return false;
	}

	/**
	 * @param project 
	 * @param className
	 * @return 
	 */
	private void addLayoutChanges(IProject project, String[] classNames) {
		try {
			IFolder resFolder = project.getFolder(IConstants.FD_RESOURCES);
			IFolder layoutFolder = resFolder.getFolder(IConstants.FD_LAYOUT);
			IResource[] members = layoutFolder.members();
			for (int i = 0; i < members.length; i++) {
				IResource member = members[i];
				if ( (member instanceof IFile) && member.exists()) {
					IFile file = (IFile) member;
					Set<AndroidLayoutChangeDescription> changes = parse(file, classNames);
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
	private Set<AndroidLayoutChangeDescription> parse(IFile file, String[] classNames) {
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
								if (value != null) {
									for (int j = 0; j < classNames.length; j++) {
										String className = classNames[j];
										if (value.equals(className)) {
											String newClassName = getNewClassName(className);
											AndroidLayoutChangeDescription layoutChange = new AndroidLayoutChangeDescription(className, newClassName, AndroidLayoutChangeDescription.VIEW_TYPE);
											changes.add(layoutChange);
										}
									}
								}
							}
						}
					}
					for (int j = 0; j < classNames.length; j++) {
						String className = classNames[j];
						nodes = xmlDoc.getElementsByTagName(className);
						for (int i = 0; i < nodes.getLength(); i++) {
							String newClassName = getNewClassName(className);
							AndroidLayoutChangeDescription layoutChange = new AndroidLayoutChangeDescription(className, newClassName, AndroidLayoutChangeDescription.STANDALONE_TYPE);
							changes.add(layoutChange);
						}
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

	private String getNewClassName(String className) {
		int lastDot = className.lastIndexOf(".");
		if (lastDot < 0) {
			return newName;
		}
		String name = className.substring(lastDot,className.length());
		String newClassName = newName + name;
		return newClassName;
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
						if (fullName != null && fullName.startsWith(javaPackage)) {
							boolean startWithDot = (value.charAt(0) == '.');
					        boolean hasDot = (value.indexOf('.') != -1);
					        if (!startWithDot && hasDot) {
					        	androidElements.put(element, value);
					        }
						}
					}
				}
			}
		}	
	}

}
