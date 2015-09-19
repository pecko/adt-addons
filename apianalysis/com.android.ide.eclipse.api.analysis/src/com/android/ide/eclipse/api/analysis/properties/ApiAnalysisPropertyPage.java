package com.android.ide.eclipse.api.analysis.properties;

import com.android.ide.eclipse.api.analysis.Activator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

public class ApiAnalysisPropertyPage extends PropertyPage {

    public static final String API_SEVERITY_LEVEL = "ApiSeverityLevel";

    public static final String IGNORE = "Ignore";
    public static final String WARNING = "Warning";
    public static final String ERROR = "Error";

    private Combo combo;

    private IJavaProject mProject;

    @Override
    protected Control createContents(Composite parent) {
        mProject = getJavaProject();
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout(2, false));

        Label label = new Label(composite, SWT.NONE);
        if (mProject != null) {
        	label.setText("Select the severity level for the Android API Analysis");
        	combo = new Combo(composite, SWT.READ_ONLY);
        	combo.setItems(new String[] { IGNORE, WARNING, ERROR });
        	String apiSeverity = Activator.getDefault().getPreferenceStore().getString(API_SEVERITY_LEVEL);
        	combo.setText(apiSeverity);
        } else {
        	label.setText("Cannot adapt the project to a Java project");
        	noDefaultAndApplyButton();
        }
        return composite;
    }

	private IJavaProject getJavaProject() {
		IAdaptable element = getElement();
		if (element instanceof IJavaProject) {
			return (IJavaProject)getElement();
		}
		if (element instanceof IProject) {
			IJavaProject project = JavaCore.create((IProject)element);
			if (project != null && project.exists()) {
				return project;
			}
		}
		IJavaProject project = (IJavaProject) element.getAdapter(IJavaProject.class);
		return project;
	}

    @Override
    protected void performApply() {
        saveValue();
        super.performApply();
    }

    private void saveValue() {
    	if (mProject == null) {
    		return;
    	}
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        prefs.setValue(API_SEVERITY_LEVEL, combo.getText());
        try {
            mProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
        } catch (CoreException e) {
            // ignore
        }
    }

    @Override
    protected void performDefaults() {
    	if (mProject == null) {
    		return;
    	}
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        prefs.setValue(API_SEVERITY_LEVEL, WARNING);
        combo.setText(WARNING);
        try {
            mProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
        } catch (CoreException e) {
            // ignore
        }
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        saveValue();
        return super.performOk();
    }

}
