/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */
package tzuyu.plugin.action.testgen;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import tzuyu.plugin.action.TzuyuAction;
import tzuyu.plugin.core.dto.WorkObject;
import tzuyu.plugin.reporter.PluginLogger;

/**
 * @author LLT
 */
public class GenerateTestcasesAction extends TzuyuAction<GenTestConfiguration> {

	@Override
	protected void run(WorkObject workObject, GenTestConfiguration config) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell();

		IJavaProject project = workObject.getFirstProject();
		config.setJavaProject(project);
		GenTestWizard wizard = new GenTestWizard(project, config);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.create();
		dialog.open();
	}

	private void launch(GenTestConfiguration config){
		DebugUITools.launch(config.getLaunchConfig(), ILaunchManager.RUN_MODE);
	}

	@Override
	protected GenTestConfiguration initConfiguration() {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager
				.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		ILaunchConfiguration[] configurations;
		try {
			configurations = manager.getLaunchConfigurations(type);
			for (int i = 0; i < configurations.length; i++) {
				ILaunchConfiguration configuration = configurations[i];
				if (configuration.getName().equals(GenTestConfiguration.CONFIG_NAME)) {
					configuration.delete();
					break;
				}
			}
			ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(
					null, GenTestConfiguration.CONFIG_NAME);
			return new GenTestConfiguration(workingCopy);
		} catch (CoreException e) {
			PluginLogger.logEx(e);
		}
		return null;
	}

}
