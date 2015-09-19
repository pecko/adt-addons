package com.android.ide.eclipse.api.analysis.properties;

import com.android.ide.eclipse.api.analysis.Activator;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class ApiAnalysisPreferencesInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();

        prefs.setDefault(ApiAnalysisPropertyPage.API_SEVERITY_LEVEL,
                ApiAnalysisPropertyPage.WARNING);
    }

}
