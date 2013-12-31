/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */
package tzuyu.plugin.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import tzuyu.plugin.core.dto.RunConfiguration;
import tzuyu.plugin.core.dto.WorkObject;

/**
 * @author LLT
 */
public abstract class TzuyuAction<C extends RunConfiguration> implements IObjectActionDelegate {
	// current selection
	protected ISelection selection;
	private IWorkbenchPart targetPart;

	protected void openDialog(String msg) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell();
		TitleAreaDialog dialog = new TitleAreaDialog(shell);
		dialog.create();
		dialog.setMessage(msg);
		dialog.open();
	}

	@Override
	public void run(IAction action) {
		if (selection == null || selection.isEmpty()) {
			openDialog("selection is null or empty");
		} else if (selection instanceof IStructuredSelection) {
			WorkObject workObject = WorkObject.getResourcesPerProject((IStructuredSelection) selection);
			run(workObject, initConfiguration());
		}
	}
	
	protected abstract void run(WorkObject workObject, C config);

	protected abstract C initConfiguration();

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}
}
