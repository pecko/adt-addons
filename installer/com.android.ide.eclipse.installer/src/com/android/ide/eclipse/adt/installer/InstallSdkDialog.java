/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.installer;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.preferences.AdtPrefs;
import com.android.ide.eclipse.adt.internal.sdk.AdtConsoleSdkLog;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.internal.wizards.actions.AvdManagerAction;
import com.android.ide.eclipse.ddms.DdmsPlugin;
import com.android.ide.eclipse.installer.InstallAndroidSdk;
import com.android.ide.eclipse.installer.InstallerActivator;
import com.android.sdklib.SdkManager;
import com.android.sdklib.internal.repository.Archive;
import com.android.sdklib.internal.repository.DocPackage;
import com.android.sdklib.internal.repository.ITask;
import com.android.sdklib.internal.repository.ITaskMonitor;
import com.android.sdklib.internal.repository.Package;
import com.android.sdklib.internal.repository.PlatformPackage;
import com.android.sdklib.internal.repository.PlatformToolPackage;
import com.android.sdklib.internal.repository.SdkRepoSource;
import com.android.sdklib.internal.repository.SdkSourceCategory;
import com.android.sdklib.internal.repository.ToolPackage;
import com.android.sdklib.repository.SdkRepoConstants;
import com.android.sdkuilib.internal.tasks.ProgressTaskFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class InstallSdkDialog extends TitleAreaDialog {

	private static final String MESSAGE =
		"Location of Android SDK has not been setup in the preference";
	private static final String INSTALL_ANDROID_SDK = "Install Android SDK";
	private static final String SDK_DIRECTORY = System.getProperty("user.home") //$NON-NLS-1$
			+ File.separator + "AndroidSDK"; //$NON-NLS-1$
	private static final int INSTALL_SDK_ID = 0;
	private static final int EXIT_ID = 1;
	private static final int OPEN_AVD_MANAGER = 2;
	private static final String PROJECT_LOGO_LARGE = "icons/android_large.png"; //$NON-NLS-1$
	private static final long MAX_DELAY = 60000L;
    private static final long SLEEP_TIME = 200L;

	private Text mSdkDirectory;
	private Button mBrowseButton;
	private Button mShowMeButton;
	private Text mDescriptionText;
	private Image androidImage;
    private List<PackageModel> packageModels;
    private Text mTotalText;

	public InstallSdkDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		parent.getShell().setText(INSTALL_ANDROID_SDK);

		//setTitle(INSTALL_ANDROID_SDK);
		setMessage(MESSAGE);
		setTitleImage(getAndroidImage());
		parent.addDisposeListener(new DisposeListener() {

            public void widgetDisposed(DisposeEvent e) {
                if (androidImage != null) {
                    androidImage.dispose();
                }
            }
        });

		Composite dialogComposite = (Composite) super.createDialogArea(parent);

		Composite composite = new Composite(dialogComposite, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gd);
		GridLayout layout = new GridLayout(3, false);
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		layout.horizontalSpacing = 8;
		layout.marginLeft = 3;
		layout.marginRight = 3;
		layout.marginTop = 3;
		layout.marginBottom = 3;
		composite.setLayout(layout);

		createPathComponent(composite);

		packageModels = createModel();

		Group componentGroup = new Group(composite, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 3;
		componentGroup.setLayoutData(gd);
		layout = new GridLayout(1, false);
		componentGroup.setLayout(layout);
		componentGroup.setText("Components:");
		createModelComponents(componentGroup);

		Group descriptionGroup = new Group(composite, SWT.NONE);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.horizontalSpan = 3;
        descriptionGroup.setLayoutData(gd);
        layout = new GridLayout(1, false);
        descriptionGroup.setLayout(layout);
        descriptionGroup.setText("Description:");
		createDescription(descriptionGroup);

		createTotal(composite);
		createShowMeButton(composite);

		return composite;
	}

	private void createTotal(Composite parent) {
	    Composite composite = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.horizontalSpan = 2;
        composite.setLayoutData(gd);
        GridLayout layout = new GridLayout(3, false);
        composite.setLayout(layout);

        Label totalLabel = new Label(composite,SWT.NONE);
        gd = new GridData(SWT.BEGINNING, SWT.FILL, false, false);
        totalLabel.setLayoutData(gd);
        totalLabel.setText("Total:");

        mTotalText = new Text(composite, SWT.READ_ONLY | SWT.BORDER | SWT.RIGHT);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        mTotalText.setLayoutData(gd);
        mTotalText.setText(getTotal());

        Label mibLabel = new Label(composite,SWT.NONE);
        gd = new GridData(SWT.BEGINNING, SWT.FILL, false, false);
        mibLabel.setLayoutData(gd);
        mibLabel.setText("MiB");
    }

    private int getHeightHint(Composite parent, int lines) {
	    GC gc = new GC(parent);
        int heightHint = Dialog.convertHeightInCharsToPixels(gc
                .getFontMetrics(), lines);
        gc.dispose();
        return heightHint;
	}

    private void createModelComponents(Composite composite) {
        CheckboxTableViewer viewer = CheckboxTableViewer.newCheckList(composite,
                SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
        Table table= viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(false);
        GridData gd = new GridData(GridData.FILL, GridData.FILL, true, false);
        gd.heightHint = getHeightHint(composite, 10);
        table.setLayoutData(gd);

        String[] columnNames = {"Name", "Req", "Rec"};
        int[] columnWidths = { 450,50,50 };

        for (int i = 0; i < columnWidths.length; i++) {
            TableColumn tableColumn= new TableColumn(table, SWT.NONE);
            tableColumn.setText(columnNames[i]);
            tableColumn.setResizable(false);
            tableColumn.setWidth(columnWidths[i]);
        }

        viewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                boolean checked= event.getChecked();
                PackageModel element= (PackageModel) event.getElement();
                element.setEnabled(checked);
                mDescriptionText.setText(getDescription());
                mTotalText.setText(getTotal());
                validate();
            }
        });

        viewer.setContentProvider(new PackageModelContentProvider());

        PackageModelLabelProvider labelProvider= new PackageModelLabelProvider();
        viewer.setLabelProvider(labelProvider);
        viewer.setInput(packageModels);
        for (int i = 0; i < packageModels.size(); i++) {
            PackageModel element = (PackageModel) viewer.getElementAt(i);
            viewer.setChecked(element, element.isEnabled());
        }
        for (int i = 0; i < packageModels.size(); i++) {
            TableItem tableItem = table.getItem(i);
            addButtonToTable(table, 1, packageModels.get(i).isRequired(), tableItem);
            addButtonToTable(table, 2, packageModels.get(i).isRecommended(), tableItem);
        }
    }

    private void addButtonToTable(Table table, int column,
            boolean selection, TableItem tableItem) {
        Button button = new Button(table, SWT.CHECK);
        button.pack();
        button.setEnabled(false);
        button.setSelection(selection);
        TableEditor editor = new TableEditor (table);
        editor.minimumWidth = button.getSize ().x;
        editor.horizontalAlignment = SWT.CENTER;
        editor.setEditor(button, tableItem, column);
        editor = new TableEditor (table);
    }

    private List<PackageModel> createModel() {
        final SdkRepoSource source = new SdkRepoSource(
                SdkRepoConstants.URL_GOOGLE_SDK_SITE, SdkSourceCategory.ANDROID_REPO.getUiName());
        ProgressTaskFactory mTaskFactory = new ProgressTaskFactory(getShell());
        mTaskFactory.start("Refresh Sources", new ITask() {
            public void run(ITaskMonitor monitor) {
                source.load(monitor.createSubMonitor(1), false);
                monitor.incProgress(1);
            }
        });
        List<PackageModel> models = new ArrayList<PackageModel>();
        if (source == null || source.getPackages() == null) {
            return models;
        }
        List<Package> compatiblePackages = new ArrayList<Package>();
        for (Package pkg:source.getPackages()) {
            if (pkg.hasCompatibleArchive()) {
                compatiblePackages.add(pkg);
            }
        }
		ArrayList<PlatformPackage> platformPackages = new ArrayList<PlatformPackage>();
		for (Package pkg : compatiblePackages) {
			if (pkg instanceof PlatformPackage) {
				platformPackages.add((PlatformPackage) pkg);
			}
		}

		PlatformPackage mPlatformPackage = null;
		for (PlatformPackage pkg : platformPackages) {
			if (mPlatformPackage == null) {
				mPlatformPackage = pkg;
			} else if (pkg.getVersion().getApiLevel() > mPlatformPackage
					.getVersion().getApiLevel()) {
				mPlatformPackage = pkg;
			} else if (pkg.getVersion().getApiLevel() == mPlatformPackage
					.getVersion().getApiLevel()
					&& pkg.getRevision() > mPlatformPackage.getRevision()) {
				mPlatformPackage = pkg;
			}
		}

		ArrayList<DocPackage> docPackages = new ArrayList<DocPackage>();
        for (Package pkg : compatiblePackages) {
            if (pkg instanceof DocPackage) {
                docPackages.add((DocPackage) pkg);
            }
        }

		DocPackage mDocPackage = null;
        for (DocPackage pkg : docPackages) {
            if (mDocPackage == null) {
                mDocPackage = pkg;
            } else if (pkg.getVersion().getApiLevel() > mDocPackage
                    .getVersion().getApiLevel()) {
                mDocPackage = pkg;
            } else if (pkg.getVersion().getApiLevel() == mDocPackage
                    .getVersion().getApiLevel()
                    && pkg.getRevision() > mDocPackage.getRevision()) {
                mDocPackage = pkg;
            }
        }

		for (Package pkg:compatiblePackages) {
		    PackageModel model;
		    if (pkg == mPlatformPackage) {
		        model = new PackageModel(pkg, false, true);
		    } else if (pkg instanceof ToolPackage || pkg instanceof PlatformToolPackage) {
		        model = new PackageModel(pkg, true, false);
		    } else if (pkg == mDocPackage) {
		        model = new PackageModel(pkg, false, true);
		    } else {
		        model = new PackageModel(pkg, false, false);
		    }
		    models.add(model);
		}
		return models;
    }

    private void createDescription(Composite composite) {

		mDescriptionText = new Text(composite, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.READ_ONLY | SWT.BORDER | SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = getHeightHint(composite, 6);
		mDescriptionText.setLayoutData(gd);

		mDescriptionText.setText(getDescription());
    }

    private void createShowMeButton(Composite composite) {
        GridData gd;
        mShowMeButton = new Button(composite, SWT.CHECK);
		gd = new GridData();
		gd.horizontalSpan = 3;
		mShowMeButton.setLayoutData(gd);
		mShowMeButton.setText("Show me again");
		boolean showMe = InstallerActivator.getDefault().getPreferenceStore().getBoolean(InstallerActivator.SHOW_ME_AGAIN);
		mShowMeButton.setSelection(showMe);
		mShowMeButton.addSelectionListener(new SelectionAdapter() {

			@Override
            public void widgetSelected(SelectionEvent e) {
				IPreferenceStore prefs = InstallerActivator.getDefault()
						.getPreferenceStore();
				prefs.setValue(InstallerActivator.SHOW_ME_AGAIN,
						mShowMeButton.getSelection());
			}

		});
    }

    private void createPathComponent(Composite composite) {
        Field sdkField = createField(composite, SDK_DIRECTORY,
				"SDK destination:", "Browse...");
		mSdkDirectory = sdkField.getText();
		mSdkDirectory.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				validate();
			}
		});
		mBrowseButton = sdkField.getButton();
		mBrowseButton.addSelectionListener(new SelectionAdapter() {

			@Override
            public void widgetSelected(SelectionEvent e) {
				DirectoryDialog fileDialog = new DirectoryDialog(getShell(),
						SWT.OPEN);
				String startingDirectory = mSdkDirectory.getText();
				if (startingDirectory != null && startingDirectory.length() > 0) {
					fileDialog.setFilterPath(startingDirectory);
				}
				String dir = fileDialog.open();
				if (dir != null) {
					dir = dir.trim();
					if (dir.length() > 0) {
						mSdkDirectory.setText(dir);
					}
				}
			}

		});
    }

	private Image getAndroidImage() {
        if (androidImage == null) {
            ImageDescriptor desc = AdtPlugin.getImageDescriptor(PROJECT_LOGO_LARGE);
            androidImage = desc.createImage();
        }
        return androidImage;
    }

    private String getDescription() {
		StringBuilder builder = new StringBuilder();

		for (PackageModel model:packageModels) {
		    if (model.isEnabled()) {
		        addPackage(model.getPackage(),getArchive(model.getPackage()),builder);
		        builder.append("\n");
		    }
		}

		return builder.toString();
	}

    private String getTotal() {
        long total = 0;
        for (PackageModel model:packageModels) {
            if (model.isEnabled()) {
                Archive archive = getArchive(model.getPackage());
                total += archive.getSize();
            }
        }
        return String.format("%d", Math.round(total / (1024*1024))); //$NON-NLS-1$
    }

	private void addPackage(Package pkg, Archive archive, StringBuilder builder) {
		builder.append(pkg.getShortDescription());
		builder.append("\n"); //$NON-NLS-1$
		builder.append(archive.getLongDescription());
		builder.append("\n"); //$NON-NLS-1$
	}

	private Field createField(Composite composite, String textValue,
			String textLabel, String buttonLabel) {
		GridData gd;
		Label label = new Label(composite, SWT.NONE);
		gd = new GridData(SWT.BEGINNING, SWT.FILL, false, false);
        label.setLayoutData(gd);
		label.setText(textLabel);

		Text text = new Text(composite, SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		text.setLayoutData(gd);
		text.setText(textValue);

		Button button = new Button(composite, SWT.PUSH);
		button.setFont(composite.getFont());
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		int widthHint = convertHorizontalDLUsToPixels(button,
				IDialogConstants.BUTTON_WIDTH);
		gd.widthHint = Math.max(widthHint,
				button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);

		button.setLayoutData(gd);
		button.setText(buttonLabel);
		return new Field(button, text);
	}

	private int convertHorizontalDLUsToPixels(Control control, int dlus) {
		GC gc = new GC(control);
		gc.setFont(control.getFont());
		int averageWidth = gc.getFontMetrics().getAverageCharWidth();
		gc.dispose();
		double horizontalDialogUnitSize = averageWidth * 0.25;
		return (int) Math.round(dlus * horizontalDialogUnitSize);
	}

	private void validate() {
		setErrorMessage(null);
		setMessage(null);
		getButton(OPEN_AVD_MANAGER).setEnabled(false);
        getButton(INSTALL_SDK_ID).setEnabled(false);
        mShowMeButton.setEnabled(true);
		String sdkString = mSdkDirectory.getText();
		if (sdkString.length() == 0) {
			setErrorMessage("Location of SDK is required.");
			return;
		}
		File sdkFile = new File(sdkString);
		if (sdkFile.isFile()) {
			setErrorMessage("The SDK path is invalid.");
			return;
		}
		if (!InstallAndroidSdk.isValidSdk(mSdkDirectory.getText())) {
			setMessage(MESSAGE, IMessageProvider.INFORMATION);
			getButton(INSTALL_SDK_ID).setEnabled(true);

			int enabled = 0;
			int count = 0;
			for (PackageModel model:packageModels) {
			    if (!model.isEnabled() && !model.isInstalled() && model.isRequired()) {
			        setErrorMessage(model.getPackage().getShortDescription() + " is required.");
			        getButton(INSTALL_SDK_ID).setEnabled(false);
			        return;
			    }
			    if (!model.isEnabled() && !model.isInstalled() && model.isRecommended()) {
                    setMessage(model.getPackage().getShortDescription() + " is recommended.");
                    return;
                }
			    if (model.isEnabled()) {
			        enabled++;
			    }
			    count++;
			}
			if (count <= 0) {
                setErrorMessage("There is nothing to install. Please check your internet connection.");
                getButton(INSTALL_SDK_ID).setEnabled(false);
                return;
            }
			if (enabled <= 0) {
                setErrorMessage("There is nothing to install. Please enable required packages.");
                getButton(INSTALL_SDK_ID).setEnabled(false);
                return;
            }
			if (getButton(INSTALL_SDK_ID).isEnabled() && sdkFile.isDirectory()) {
			    String[] files = sdkFile.list();
			    if (files != null && files.length > 0) {
			        setMessage("The '" + sdkString + "' directory isn't empty.");
			    }
			}
			return;
		}
		String oldSdkDir = AdtPlugin.getDefault().getPreferenceStore()
				.getString(AdtPrefs.PREFS_SDK_DIR);
		AdtPlugin.getDefault().getPreferenceStore()
				.putValue(AdtPrefs.PREFS_SDK_DIR, sdkString);
		AdtPrefs.getPrefs().loadValues(null /*event*/);
		DdmsPlugin.setToolsLocation(AdtPlugin.getOsAbsoluteAdb(), true /* startAdb */,
                AdtPlugin.getOsAbsoluteHprofConv(), AdtPlugin.getOsAbsoluteTraceview());
		AdtPlugin.getDefault().reparseSdk();
		checkForLoad();
		if (Sdk.getCurrent() != null) {
			getButton(OPEN_AVD_MANAGER).setEnabled(true);
			mShowMeButton.setEnabled(false);
			mDescriptionText.setText(""); //$NON-NLS-1$
			return;
		} else {
			AdtPlugin.getDefault().getPreferenceStore()
					.putValue(AdtPrefs.PREFS_SDK_DIR, oldSdkDir);
		}
	}

	private static class Field {

		private Button button;
		private Text text;

		public Button getButton() {
			return button;
		}

		public Field(Button button, Text text) {
			this.button = button;
			this.text = text;
		}

		public Text getText() {
			return text;
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// Install SDK
		createButton(parent, INSTALL_SDK_ID, "Install", true);
		// Exit
		createButton(parent, EXIT_ID, "Exit", false);
		// open AVD manager
		Button button = createButton(parent, OPEN_AVD_MANAGER,
				"Open AVD manager", false);
		button.setEnabled(false);
		validate();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (INSTALL_SDK_ID == buttonId) {
			installSdk();
			validate();
		}
		if (EXIT_ID == buttonId) {
			super.cancelPressed();
		}
		if (OPEN_AVD_MANAGER == buttonId) {
			new AvdManagerAction().run(null);
		}

	}

	private Archive getArchive(Package pkg) {
		Archive[] archives = pkg.getArchives();
		for (final Archive archive : archives) {
			if (archive.isCompatible()) {
				return archive;
			}
		}
		return null;
	}

	private void installSdk() {
		final String sdkString = mSdkDirectory.getText();
		File sdkDir = new File(sdkString);
		if (!sdkDir.isDirectory()) {
			boolean created = sdkDir.mkdirs();
			if (!created) {
				setErrorMessage("Could not create SDK directory.");
				return;
			}
		}

		final ProgressTaskFactory mTaskFactory = new ProgressTaskFactory(
				getShell());

		mTaskFactory.start("Install SDK Tools", new ITask() {
			public void run(ITaskMonitor monitor) {
				monitor.setProgressMax(10);
				final int progressPerArchive = 2 * Archive.NUM_MONITOR_INC;
				monitor.setProgressMax((int) (2 * progressPerArchive));
				// install required
				boolean installed = false;
				for (PackageModel packageModel:packageModels) {
				    if (!packageModel.isInstalled() && packageModel.isRequired()) {
				        Archive archive = getArchive(packageModel.getPackage());
				        installed = archive.install(sdkString, false,
                               null, monitor);
				        if (!installed) {
				            break;
				        }
				        packageModel.setInstalled(true);
				    }
				}
				if (installed) {
					File sdkFile = new File(sdkString);
					new File(sdkFile, "platforms").mkdir(); //$NON-NLS-1$
					new File(sdkFile, "add-ons").mkdir(); //$NON-NLS-1$
					new File(sdkFile, "docs").mkdir(); //$NON-NLS-1$
					new File(sdkFile, "samples").mkdir(); //$NON-NLS-1$
					AdtPlugin.getDefault().getPreferenceStore()
							.putValue(AdtPrefs.PREFS_SDK_DIR, sdkString);
					monitor.setDescription("Loading SDK ");
					Sdk.loadSdk(sdkString);
					checkForLoad();
					SdkManager manager = SdkManager.createManager(
							sdkString, new AdtConsoleSdkLog());
					for (PackageModel packageModel:packageModels) {
					    if (!packageModel.isInstalled() && packageModel.isEnabled()) {
					        monitor.setDescription(packageModel.getPackage().
					                getShortDescription());
					        Archive archive = getArchive(packageModel.getPackage());
					        installed = archive.install(sdkString, false,
                               manager, monitor);
					        if (!installed) {
					            break;
					        }
					        packageModel.setInstalled(true);
					    }
					}
					Sdk.loadSdk(sdkString);
				} else {
				    // TODO message dialog (!installed)
				}
			}
		});
	}

	private void checkForLoad() {
		long start = System.currentTimeMillis();
		while (Sdk.getCurrent() == null) {
			try {
				Thread.sleep(SLEEP_TIME);
			} catch (InterruptedException e) {
				// ignore
			}
			long delay = System.currentTimeMillis() - start;
			if (delay > MAX_DELAY) {
				return;
			}
		}
	}

}
