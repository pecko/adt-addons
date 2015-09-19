package com.android.ide.eclipse.api.analysis.compiler;

import com.android.ide.eclipse.adt.AdtConstants;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestHelper;
import com.android.ide.eclipse.adt.internal.project.XmlErrorHandler.BasicXmlErrorListener;
import com.android.ide.eclipse.adt.internal.sdk.ProjectState;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.io.IFileWrapper;
import com.android.ide.eclipse.api.analysis.Activator;
import com.android.ide.eclipse.api.analysis.properties.ApiAnalysisPropertyPage;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.xml.ManifestData;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.compiler.ReconcileContext;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.core.builder.AbstractImageBuilder;
import org.eclipse.jdt.internal.core.builder.BuildNotifier;
import org.eclipse.jdt.internal.core.builder.JavaBuilder;
import org.eclipse.jdt.internal.core.builder.ProblemFactory;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ApiAnalysisCompilationParticipant extends CompilationParticipant implements ICompilerRequestor {

    private static final String MARKER_ID = "com.android.ide.eclipse.api.analysis.compatibility.problem";  //$NON-NLS-1$

    private ApiAnalysisNameEnvironment nameEnvironment;
    private ClasspathMultiDirectory[] sourceLocations;
    private BuildNotifier notifier;
    private List problemSourceFiles;
    private Compiler compiler;

    @Override
    public void buildFinished(IJavaProject project) {
        // skip if any error exist
        try {
            IMarker[] markers = project.getProject().findMarkers(IMarker.PROBLEM, true,
                    IResource.DEPTH_INFINITE);
            for (IMarker marker:markers) {
                Integer severity = (Integer) marker.getAttribute(IMarker.SEVERITY);
                if (severity != null && severity.intValue() == IMarker.SEVERITY_ERROR) {
                    return;
                }
            }
        } catch (CoreException e1) {
            // ignore
        }
        ArrayList sourceFiles = new ArrayList(33);
        this.problemSourceFiles = new ArrayList(3);
        this.notifier = new BuildNotifier(new NullProgressMonitor(), project.getProject());
        this.notifier.begin();
        compiler = newCompiler(project);
        this.notifier.updateProgressDelta(0.05f);

        this.notifier.subTask(Messages.build_analyzingSources);
        sourceLocations = nameEnvironment.sourceLocations;
        try {
            addAllSourceFiles(sourceFiles, project);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.notifier.updateProgressDelta(0.10f);

        if (sourceFiles.size() > 0) {
            SourceFile[] allSourceFiles = new SourceFile[sourceFiles.size()];
            sourceFiles.toArray(allSourceFiles);

            this.notifier.setProgressPerCompilationUnit(0.75f / allSourceFiles.length);
            //this.workQueue.addAll(allSourceFiles);
            compile(allSourceFiles);

//            if (this.typeLocatorsWithUndefinedTypes != null)
//                if (this.secondaryTypes != null && !this.secondaryTypes.isEmpty())
//                    rebuildTypesAffectedBySecondaryTypes();
//            if (this.incrementalBuilder != null)
//                this.incrementalBuilder.buildAfterBatchBuild();
        }
    }

    private void compile(SourceFile[] units) {
        if (units.length == 0) return;
        SourceFile[] additionalUnits = null;
        String message = Messages.bind(Messages.build_compiling, units[0].resource.getFullPath().removeLastSegments(1).makeRelative().toString());
        this.notifier.subTask(message);

        // extend additionalFilenames with all hierarchical problem types found during this entire build
        if (!this.problemSourceFiles.isEmpty()) {
            int toAdd = this.problemSourceFiles.size();
            int length = additionalUnits == null ? 0 : additionalUnits.length;
            if (length == 0)
                additionalUnits = new SourceFile[toAdd];
            else
                System.arraycopy(additionalUnits, 0, additionalUnits = new SourceFile[length + toAdd], 0, length);
            for (int i = 0; i < toAdd; i++)
                additionalUnits[length + i] = (SourceFile) this.problemSourceFiles.get(i);
        }
        String[] initialTypeNames = new String[units.length];
        for (int i = 0, l = units.length; i < l; i++)
            initialTypeNames[i] = units[i].initialTypeName;
        this.nameEnvironment.setNames(initialTypeNames, additionalUnits);
        this.notifier.checkCancel();
        try {
            this.compiler.compile(units);
        } catch (AbortCompilation ignored) {
            // ignore the AbortCompilcation coming from BuildNotifier.checkCancelWithinCompiler()
            // the Compiler failed after the user has chose to cancel... likely due to an OutOfMemory error
        } finally {

        }
        // Check for cancel immediately after a compile, because the compiler may
        // have been cancelled but without propagating the correct exception
        this.notifier.checkCancel();
    }

    protected void addAllSourceFiles(final ArrayList sourceFiles, final IJavaProject project) throws CoreException {
        for (int i = 0, l = this.sourceLocations.length; i < l; i++) {
            final ClasspathMultiDirectory sourceLocation = this.sourceLocations[i];
            final char[][] exclusionPatterns = sourceLocation.exclusionPatterns;
            final char[][] inclusionPatterns = sourceLocation.inclusionPatterns;
            final boolean isAlsoProject = sourceLocation.sourceFolder.equals(project.getProject());
            final int segmentCount = sourceLocation.sourceFolder.getFullPath().segmentCount();
            final IContainer outputFolder = sourceLocation.binaryFolder;
            final boolean isOutputFolder = sourceLocation.sourceFolder.equals(outputFolder);
            sourceLocation.sourceFolder.accept(
                new IResourceProxyVisitor() {
                    public boolean visit(IResourceProxy proxy) throws CoreException {
                        switch(proxy.getType()) {
                            case IResource.FILE :
                                if (org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(proxy.getName())) {
                                    IResource resource = proxy.requestResource();
                                    if (exclusionPatterns != null || inclusionPatterns != null)
                                        if (Util.isExcluded(resource.getFullPath(), inclusionPatterns, exclusionPatterns, false))
                                            return false;
                                    sourceFiles.add(new SourceFile((IFile) resource, sourceLocation));
                                }
                                return false;
                            case IResource.FOLDER :
                                IPath folderPath = null;
                                if (isAlsoProject)
                                    if (isExcludedFromProject(folderPath = proxy.requestFullPath(),project))
                                        return false;
                                if (exclusionPatterns != null) {
                                    if (folderPath == null)
                                        folderPath = proxy.requestFullPath();
                                    if (Util.isExcluded(folderPath, inclusionPatterns, exclusionPatterns, true)) {
                                        // must walk children if inclusionPatterns != null, can skip them if == null
                                        // but folder is excluded so do not create it in the output folder
                                        return inclusionPatterns != null;
                                    }
                                }
                                if (!isOutputFolder) {
                                    if (folderPath == null)
                                        folderPath = proxy.requestFullPath();
                                    String packageName = folderPath.lastSegment();
                                    if (packageName.length() > 0) {
                                        String sourceLevel = project.getOption(JavaCore.COMPILER_SOURCE, true);
                                        String complianceLevel = project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
                                        if (JavaConventions.validatePackageName(packageName, sourceLevel, complianceLevel).getSeverity() != IStatus.ERROR)
                                            createFolder(folderPath.removeFirstSegments(segmentCount), outputFolder);
                                    }
                                }
                        }
                        return true;
                    }
                },
                IResource.NONE
            );
            this.notifier.checkCancel();
        }
    }

    protected IContainer createFolder(IPath packagePath, IContainer outputFolder) throws CoreException {
        if (packagePath.isEmpty()) return outputFolder;
        IFolder folder = outputFolder.getFolder(packagePath);
        if (!folder.exists()) {
            createFolder(packagePath.removeLastSegments(1), outputFolder);
            folder.create(IResource.FORCE | IResource.DERIVED, true, null);
        }
        return folder;
    }

    protected boolean isExcludedFromProject(IPath childPath, IJavaProject project) throws JavaModelException {
        // answer whether the folder should be ignored when walking the project as a source folder
        if (childPath.segmentCount() > 2) return false; // is a subfolder of a package

        for (int j = 0, k = this.sourceLocations.length; j < k; j++) {
            if (childPath.equals(this.sourceLocations[j].binaryFolder.getFullPath())) return true;
            if (childPath.equals(this.sourceLocations[j].sourceFolder.getFullPath())) return true;
        }
        // skip default output folder which may not be used by any source folder
        return childPath.equals(project.getOutputLocation());
    }

    @Override
    public void reconcile(ReconcileContext context) {
        // TODO Auto-generated method stub
        super.reconcile(context);
    }

    @Override
    public boolean isActive(IJavaProject project) {
        if (project == null || project.getProject() == null) {
            return false;
        }
        String apiSeverity = Activator.getDefault().getPreferenceStore()
                .getString(ApiAnalysisPropertyPage.API_SEVERITY_LEVEL);
        if (ApiAnalysisPropertyPage.IGNORE.equals(apiSeverity)) {
            return false;
        }

        try {
            if (!project.getProject().hasNature(AdtConstants.NATURE_DEFAULT)) {
                return false;
            }
        } catch (CoreException e) {
            return false;
        }
        IFile manifestFile = project.getProject().getFile(SdkConstants.FN_ANDROID_MANIFEST_XML);
        if (manifestFile == null || !manifestFile.exists()) {
            return false;
        }
        BasicXmlErrorListener errorListener = new BasicXmlErrorListener();
        ManifestData parser = AndroidManifestHelper.parse(new IFileWrapper(manifestFile),
                true /*gather data*/,
                errorListener);

        if (errorListener.mHasXmlError == true) {
            return false;
        }
        String minSdkVersion = getMinSdkVersion(project.getProject());
        if (minSdkVersion == null) {
            return false;
        }
        int minSdkValue = -1;
        try {
            minSdkValue = Integer.parseInt(minSdkVersion);
        } catch (NumberFormatException e) {
            return false;
        }
        if (minSdkValue == -1) {
            return false;
        }
        ProjectState projectState = Sdk.getProjectState(project.getProject());
        if (projectState == null) {
            return false;
        }
        IAndroidTarget projectTarget = projectState.getTarget();
		if (projectTarget == null) {
			return false;
		}
        AndroidVersion projectVersion = projectTarget.getVersion();
        if (minSdkValue < projectVersion.getApiLevel()) {
            if (projectState != null && minSdkValue > 0) {
                IAndroidTarget[] targets = Sdk.getCurrent().getTargets();
                for (IAndroidTarget androidTarget : targets) {
                    if (androidTarget.getVersion().getApiLevel() == minSdkValue) {
                        String targetPath = androidTarget.getPath(IAndroidTarget.ANDROID_JAR);
                        File targetJar = new File(targetPath);
                        if (targetJar != null && targetJar.isFile()) {
                            return true;
                        }
                    }

                }
            }
        }
        return false;
    }

    public static String getMinSdkVersion(IProject project) {
        IFile manifestFile = project.getFile(SdkConstants.FN_ANDROID_MANIFEST_XML);
        if (manifestFile == null || !manifestFile.exists()) {
            return null;
        }
        BasicXmlErrorListener errorListener = new BasicXmlErrorListener();
        ManifestData parser = AndroidManifestHelper.parse(new IFileWrapper(manifestFile),
                true /*gather data*/,
                errorListener);

        if (errorListener.mHasXmlError == true) {
            return null;
        }
        return parser.getMinSdkVersionString();
    }

    protected Compiler newCompiler(IJavaProject javaProject) {
        Map projectOptions = javaProject.getOptions(true);
        String option = (String) projectOptions.get(JavaCore.COMPILER_PB_INVALID_JAVADOC);
        if (option == null || option.equals(JavaCore.IGNORE)) {
            option = (String) projectOptions.get(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS);
            if (option == null || option.equals(JavaCore.IGNORE)) {
                option = (String) projectOptions.get(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS);
                if (option == null || option.equals(JavaCore.IGNORE)) {
                    option = (String) projectOptions.get(JavaCore.COMPILER_PB_UNUSED_IMPORT);
                    if (option == null || option.equals(JavaCore.IGNORE)) {
                        projectOptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.DISABLED);
                    }
                }
            }
        }

        CompilerOptions compilerOptions = new CompilerOptions(projectOptions);
        compilerOptions.performMethodsFullRecovery = true;
        compilerOptions.performStatementsRecovery = true;
        nameEnvironment = new ApiAnalysisNameEnvironment(javaProject);
        Compiler newCompiler = new Compiler(
            nameEnvironment,
            DefaultErrorHandlingPolicies.proceedWithAllProblems(),
            compilerOptions,
            this,
            ProblemFactory.getProblemFactory(Locale.getDefault()));
        CompilerOptions options = newCompiler.options;
        // temporary code to allow the compiler to revert to a single thread
        String setting = System.getProperty("jdt.compiler.useSingleThread"); //$NON-NLS-1$
        newCompiler.useSingleThread = setting != null && setting.equals("true"); //$NON-NLS-1$

        // enable the compiler reference info support
        options.produceReferenceInfo = true;

        return newCompiler;
    }

    public void acceptResult(CompilationResult result) {
        CategorizedProblem[] problems = result.getErrors();
        if (problems == null)
            return;
        SourceFile sourceFile = (SourceFile) result.compilationUnit;
        IFile resource = sourceFile.resource;
        try {
            resource.deleteMarkers(MARKER_ID, false, IResource.DEPTH_INFINITE);
            for (CategorizedProblem problem : problems) {
                storeProblem(problem, resource);
            }
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void storeProblem(CategorizedProblem problem, IFile resource)
            throws CoreException {
        IMarker marker = resource.createMarker(MARKER_ID);
        String[] attributeNames = AbstractImageBuilder.JAVA_PROBLEM_MARKER_ATTRIBUTE_NAMES;
        int standardLength = attributeNames.length;
        String[] allNames = attributeNames;
        int managedLength = 1;
        String[] extraAttributeNames = problem.getExtraMarkerAttributeNames();
        int extraLength = extraAttributeNames == null ? 0 : extraAttributeNames.length;
        if (managedLength > 0 || extraLength > 0) {
            allNames = new String[standardLength + managedLength + extraLength];
            System.arraycopy(attributeNames, 0, allNames, 0, standardLength);
            if (managedLength > 0)
                allNames[standardLength] = IMarker.SOURCE_ID;
            System.arraycopy(extraAttributeNames, 0, allNames, standardLength + managedLength, extraLength);
        }

        Object[] allValues = new Object[allNames.length];
        // standard attributes
        int index = 0;
        StringBuffer sb = new StringBuffer();
        sb.append("API Level ");
        sb.append(nameEnvironment.minSdkValue);
        sb.append(" compatibily problem (");
        sb.append(problem.getMessage());
        sb.append(")");
        allValues[index++] = sb.toString(); // message
        String apiSeverity = Activator.getDefault().getPreferenceStore()
        .getString(ApiAnalysisPropertyPage.API_SEVERITY_LEVEL);
        if (ApiAnalysisPropertyPage.ERROR.equals(apiSeverity)) {
            allValues[index++] = new Integer(IMarker.SEVERITY_ERROR);
        } else {
            allValues[index++] = new Integer(IMarker.SEVERITY_WARNING);
        }
        allValues[index++] = new Integer(problem.getID()); // ID
        allValues[index++] = new Integer(problem.getSourceStart()); // start
        int end = problem.getSourceEnd();
        allValues[index++] = new Integer(end > 0 ? end + 1 : end); // end
        allValues[index++] = new Integer(problem.getSourceLineNumber()); // line
        allValues[index++] = Util.getProblemArgumentsForMarker(problem.getArguments()); // arguments
        allValues[index++] = new Integer(problem.getCategoryID()); // category ID
        // SOURCE_ID attribute for JDT problems
        allValues[index++] = JavaBuilder.SOURCE_ID;
        // optional extra attributes
        if (extraLength > 0)
            System.arraycopy(problem.getExtraMarkerAttributeValues(), 0, allValues, index, extraLength);

        marker.setAttributes(allNames, allValues);
    }

    private int searchColumnNumber(int[] startLineIndexes, int lineNumber, int position) {
        switch(lineNumber) {
            case 1 :
                return position + 1;
            case 2:
                return position - startLineIndexes[0];
            default:
                int line = lineNumber - 2;
                int length = startLineIndexes.length;
                if (line >= length) {
                    return position - startLineIndexes[length - 1];
                }
                return position - startLineIndexes[line];
        }
    }
}
