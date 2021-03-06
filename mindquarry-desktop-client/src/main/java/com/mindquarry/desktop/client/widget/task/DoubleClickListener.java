/*
 * Copyright (C) 2006-2007 Mindquarry GmbH, All Rights Reserved
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 */
package com.mindquarry.desktop.client.widget.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import com.mindquarry.desktop.client.I18N;
import com.mindquarry.desktop.client.MindClient;
import com.mindquarry.desktop.client.dialog.task.TaskSettingsDialog;
import com.mindquarry.desktop.model.task.Task;
import com.mindquarry.desktop.model.task.TaskList;
import com.mindquarry.desktop.preferences.profile.Profile;
import com.mindquarry.desktop.util.HttpUtilities;

/**
 * Add summary documentation here.
 * 
 * @author <a href="mailto:saar@mindquarry.com">Alexander Saar</a>
 */
public class DoubleClickListener implements IDoubleClickListener {
    private static Log log = LogFactory.getLog(DoubleClickListener.class);

    private final MindClient client;
    private final TableViewer viewer;

    public DoubleClickListener(MindClient client, TableViewer tableViewer) {
        this.client = client;
        this.viewer = tableViewer;
    }

    public void doubleClick(DoubleClickEvent event) {
        Profile prof = Profile.getSelectedProfile(client.getPreferenceStore());

        ISelection selection = event.getSelection();

        if (selection instanceof StructuredSelection) {
            StructuredSelection structsel = (StructuredSelection) selection;
            Object element = structsel.getFirstElement();

            if (element instanceof Task) {
                Task task = (Task) element;

                try {
                    // use a clone of the task so cancel works:
                    TaskSettingsDialog dlg = new TaskSettingsDialog(client.getShell(), task.clone(), false);

                    if (dlg.open() == Window.OK) {
                        Task newTask = dlg.getChangedTask();
                        HttpUtilities.putAsXML(prof.getLogin(), prof
                                .getPassword(), newTask.getId(), newTask
                                .getContentAsXML().asXML().getBytes("utf-8"));
                        updateTask(viewer, task, newTask);
                    }
                } catch (Exception e) {
                    MessageDialog.openError(new Shell(SWT.ON_TOP), I18N
                            .getString("Network error"),//$NON-NLS-1$
                            I18N.getString("Could not update the task")//$NON-NLS-1$
                                    + ": " + e.toString());
                    log.error("Could not update task with id " //$NON-NLS-1$
                            + task.getId(), e);
                }
            }
            if (viewer != null) {
                // avoid crash if last bug got closed but the task dialog
                // was still open and then OK was pressed:
                // (TODO: task is submitted but not visible until manual
                // refresh)
                viewer.refresh();
            }
        }
    }

    private static void updateTask(TableViewer viewer, Task oldTask, Task newTask) {
        if (viewer != null) {
            TaskList content = (TaskList) viewer.getInput();
            content.replace(oldTask, newTask);
            viewer.setInput(content);
            viewer.refresh();
        }
    }
}
