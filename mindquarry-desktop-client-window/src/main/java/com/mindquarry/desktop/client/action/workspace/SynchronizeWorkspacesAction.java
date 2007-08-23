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
package com.mindquarry.desktop.client.action.workspace;

import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import com.mindquarry.desktop.client.Messages;
import com.mindquarry.desktop.client.MindClient;
import com.mindquarry.desktop.client.action.ActionBase;
import com.mindquarry.desktop.client.widget.workspace.WorkspaceBrowserWidget;
import com.mindquarry.desktop.model.team.Team;
import com.mindquarry.desktop.preferences.profile.Profile;
import com.mindquarry.desktop.workspace.SVNSynchronizer;

/**
 * Trigger workspace synchronization, i.e. SVN update + commit.
 * 
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class SynchronizeWorkspacesAction extends ActionBase {
    public static final String ID = SynchronizeWorkspacesAction.class.getSimpleName();

	private static final Image IMAGE = new Image(
			Display.getCurrent(),
			SynchronizeWorkspacesAction.class
					.getResourceAsStream("/com/mindquarry/icons/" + ICON_SIZE + "/actions/synchronize-vertical.png")); //$NON-NLS-1$

	public SynchronizeWorkspacesAction(MindClient client) {
		super(client);

		setId(ID);
		setActionDefinitionId(ID);

		setText(Messages.getString("Synchronize workspaces"));
		setToolTipText(Messages.getString("Synchronize workspaces with your desktop."));
		setAccelerator(SWT.CTRL + +SWT.SHIFT + 'S');
		setImageDescriptor(ImageDescriptor.createFromImage(IMAGE));
	}

	public void run() {
	    try {
	        // TODO: also disable update button
	        setEnabled(false);
	        client.getToolBarManager().getControl().update();
            
	        WorkspaceBrowserWidget workspaceWidget = client.getCategoryWidget().getWorkspaceBrowserWidget();
            if (workspaceWidget.refreshNeeded()) {
                MessageBox messageBox = new MessageBox(client.getShell(), SWT.ICON_QUESTION
                        | SWT.YES | SWT.NO);
                messageBox.setMessage(Messages.getString("There list of changes is not up to date. It needs " +
                		"to be refreshed before you can synchronize changes. Refresh the list of changes now?"));
                int result = messageBox.open();
                if (result == SWT.YES) {
                    workspaceWidget.asyncRefresh();
                }
                return;
            }

            List<Team> teams = client.getSelectedTeams();
	        Profile selectedProfile = Profile.getSelectedProfile(client
	                .getPreferenceStore());
            if (selectedProfile == null) {
                log.debug("No profile selected."); //$NON-NLS-1$
                return;
            }
	        for (Team team : teams) {
	            SVNSynchronizer sc = new SVNSynchronizer(team.getWorkspaceURL(),
	                    selectedProfile.getWorkspaceFolder(),
	                    selectedProfile.getLogin(), selectedProfile.getPassword(),
	                    new InteractiveConflictHandler(client.getShell()));
	            sc.synchronizeOrCheckout();
	        }
        } finally {
            setEnabled(true);
        }
	}
	
	public String getGroup() {
        return ActionBase.WORKSPACE_ACTION_GROUP;
    }
	
	public boolean isToolbarAction() {
        return true;
    }
	
}
