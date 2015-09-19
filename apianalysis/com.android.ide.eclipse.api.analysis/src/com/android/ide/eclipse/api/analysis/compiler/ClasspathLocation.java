package com.android.ide.eclipse.api.analysis.compiler;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

public abstract class ClasspathLocation {

static ClasspathLocation forSourceFolder(IContainer sourceFolder, IContainer outputFolder, char[][] inclusionPatterns, char[][] exclusionPatterns) {
    return new ClasspathMultiDirectory(sourceFolder, outputFolder, inclusionPatterns, exclusionPatterns);
}

public static ClasspathLocation forBinaryFolder(IContainer binaryFolder, boolean isOutputFolder, AccessRuleSet accessRuleSet) {
    return new ClasspathDirectory(binaryFolder, isOutputFolder, accessRuleSet);
}

static ClasspathLocation forLibrary(String libraryPathname, long lastModified, AccessRuleSet accessRuleSet) {
    return new ClasspathJar(libraryPathname, lastModified, accessRuleSet);
}

static ClasspathLocation forLibrary(String libraryPathname, AccessRuleSet accessRuleSet) {
    return forLibrary(libraryPathname, 0, accessRuleSet);
}

static ClasspathLocation forLibrary(IFile library, AccessRuleSet accessRuleSet) {
    return new ClasspathJar(library, accessRuleSet);
}

public abstract NameEnvironmentAnswer findClass(String binaryFileName, String qualifiedPackageName, String qualifiedBinaryFileName);

public abstract IPath getProjectRelativePath();

public boolean isOutputFolder() {
    return false;
}

public abstract boolean isPackage(String qualifiedPackageName);

public void cleanup() {
    // free anything which is not required when the state is saved
}
public void reset() {
    // reset any internal caches before another compile loop starts
}

public abstract String debugPathString();

}
