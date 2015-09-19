package rs.snpe.android.refactoring;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "rs.snpe.android.refactoring";

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * @param message
	 */
	public static void logError(String message) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message));
	}
	
	public static void logInfo(String message) {
		getDefault().getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
	}
	
	public static void log(Throwable e) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	/**
	 *  ** copied from Android SDK library ** 
	 * 
     * Combines a java package, with a class value from the manifest to make a fully qualified
     * class name
     * @param javaPackage the java package from the manifest.
     * @param className the class name from the manifest.
     * @return the fully qualified class name.
     */
    public static String combinePackageAndClassName(String javaPackage, String className) {
        if (className == null || className.length() == 0) {
            return javaPackage;
        }
        if (javaPackage == null || javaPackage.length() == 0) {
            return className;
        }

        // the class name can be a subpackage (starts with a '.'
        // char), a simple class name (no dot), or a full java package
        boolean startWithDot = (className.charAt(0) == '.');
        boolean hasDot = (className.indexOf('.') != -1);
        if (startWithDot || hasDot == false) {

            // add the concatenation of the package and class name
            if (startWithDot) {
                return javaPackage + className;
            } else {
                return javaPackage + '.' + className;
            }
        } else {
            // just add the class as it should be a fully qualified java name.
            return className;
        }
    }

	/**
	 * @param javaPackage
	 * @param name
	 * @return
	 */
	public static String getNewValue(String javaPackage, String oldName, String newName) {
		if (oldName == null || oldName.length() == 0) {
            return null;
        }
        if (javaPackage == null || javaPackage.length() == 0) {
            return null;
        }
        if (newName == null || newName.length() == 0) {
            return null;
        }
        if (!newName.startsWith(javaPackage)) {
        	return newName;
        } else if (newName.length() > (javaPackage.length() + 1)) {
        	String value = newName.substring(javaPackage.length() + 1);
        	return value;
        }
        boolean startWithDot = (oldName.charAt(0) == '.');
        boolean hasDot = (oldName.indexOf('.') != -1);
        if (startWithDot || !hasDot) {

            if (startWithDot) {
                return "." + newName;
            } else {
            	int lastPeriod = newName.lastIndexOf(".");
                return newName.substring(lastPeriod + 1);
            }
        } else {
            return newName;
        }
	}
	
	public static void fixModel(IStructuredModel model, IDocument document) {
		if (model != null) {
			model.releaseFromRead();
		}
		if (document == null) {
			return;
		}
		model = StructuredModelManager.getModelManager()
				.getExistingModelForRead(document);
		if (model != null) {
			try {
				model.save();
			} catch (Exception e) {
				Activator.log(e);
			} finally {
				model.releaseFromEdit();
			}
		}
	}

}
