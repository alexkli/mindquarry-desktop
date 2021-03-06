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
package com.mindquarry.desktop.client.widget.workspace;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.NodeKind;
import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.javahl.StatusKind;

import com.mindquarry.desktop.client.I18N;
import com.mindquarry.desktop.client.MindClient;
import com.mindquarry.desktop.client.action.workspace.InteractiveConflictHandler;
import com.mindquarry.desktop.client.action.workspace.OpenSelectedFileEvent;
import com.mindquarry.desktop.client.widget.task.TaskContainerWidget;
import com.mindquarry.desktop.client.widget.util.container.ContainerWidget;
import com.mindquarry.desktop.event.EventBus;
import com.mindquarry.desktop.event.EventListener;
import com.mindquarry.desktop.model.team.Team;
import com.mindquarry.desktop.model.team.SVNRepo;
import com.mindquarry.desktop.preferences.profile.Profile;
import com.mindquarry.desktop.preferences.profile.Profile.SVNRepoData;
import com.mindquarry.desktop.workspace.SVNSynchronizer;
import com.mindquarry.desktop.workspace.conflict.Change;
import com.mindquarry.desktop.workspace.conflict.ObstructedConflict;

/**
 * Widget displaying incoming and outgoing file changes.
 * 
 * @author <a href="saar(at)mindquarry(dot)com">Alexander Saar</a>
 * @author <a href="mailto:christian.richardt@mindquarry.com">Christian Richardt</a>
 */
public class WorkspaceBrowserWidget extends ContainerWidget<TreeViewer> implements EventListener {
    private static Log log = LogFactory.getLog(WorkspaceBrowserWidget.class);

    protected File workspaceRoot;

    protected ChangeSets changeSets = new ChangeSets();
    protected ChangeTree changeTree;
    
    // ignore files like "<filename>.r200" that are created in case of conflicts:
    protected Map<File, Integer> toIgnore = new HashMap<File, Integer>();

    private WorkspaceUpdateContainerRunnable containerRunnable;

    public WorkspaceBrowserWidget(Composite parent, MindClient client) {
        super(parent, SWT.NONE, client);
        EventBus.registerListener(this);
        addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				EventBus.unregisterListener(WorkspaceBrowserWidget.this);
			}
        	
        });
    }

    // #########################################################################
    // ### PUBLIC METHODS
    // #########################################################################

    public ChangeSets getChangeSets() {
        return changeSets;
    }

    public ChangeTree getChangeTree() {
        return changeTree;
    }
    
    /**
     * Checks if a refresh of the changes list itself is needed.
     */
    public boolean refreshNeeded(Boolean applyNewChanges) {
        PreferenceStore store = client.getPreferenceStore();
        Profile selectedProfile = Profile.getSelectedProfile(store);
        if (selectedProfile == null) {
            return false;
        }
        boolean refreshNeeded = false;
        ChangeSets newChanges = getAllChanges(selectedProfile);

        // if team selection has changed, a refresh is needed:
        Set<String> teamIds = changeSets.getTeamIds();
        Set<String> newTeamIds = newChanges.getTeamIds();
        if (!teamIds.equals(newTeamIds)) {
            log.debug("Changes list does need update (#1)");
            refreshNeeded = true;
        }

        // no changes in team selection, compare the changes per team:
        if (checkChangeSetUpdateRequired(changeSets, newChanges)) {
            log.debug("Changes list does need update (#2)");
            refreshNeeded = true;
        }
        
        if(applyNewChanges) {
            changeSets = newChanges;

            // create the tree of changes from the list of all changes/conflicts 
            changeTree = new ChangeTree(changeSets.getChanges(), toIgnore);
            workspaceRoot = new File(selectedProfile.getWorkspaceFolder());
        }

        if (refreshNeeded) {
            log.debug("Changes list does not need update");
        }
        return refreshNeeded;
    }
    
    public void onEvent(com.mindquarry.desktop.event.Event event) {
        if (event instanceof OpenSelectedFileEvent && viewer != null) {
            ISelection selection = viewer.getSelection();
            if (selection instanceof StructuredSelection) {
                StructuredSelection structsel = (StructuredSelection) selection;
                Object element = structsel.getFirstElement();
                if (element instanceof File) {
                    if (!selection.isEmpty()) {
                        File file = (File) element;
                        // TODO: open remotely added files from repository
                        if (file.exists()) {
                            log.debug("Launching " + file.getAbsolutePath());
                            // TODO: doesn't work on Linux
                            Program.launch(file.getAbsolutePath());
                        } else {
                            log.warn("onEvent: cannot open remote files: " + file);
                        }
                    }
                } else {
                    log.warn("onEvent: unexpected type: " + element.getClass());
                }
            }
        }
    }
    
    public boolean changesEqual(Map<File, Change> first, Map<File, Change> second, boolean remote) {
        if (first == null && second == null) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        if (first.size() != second.size()) {
            return false;
        }
        Set<File> firstKeys = first.keySet();
        Set<File> secondKeys = second.keySet();
        if (!firstKeys.equals(secondKeys)) {
            return false;
        }
        for (File file : firstKeys) {
            Status firstStatus = first.get(file).getStatus();
            Status secondStatus = second.get(file).getStatus();
            if (remote) {
                if (firstStatus.getRepositoryTextStatus() != secondStatus.getRepositoryTextStatus()) {
                    return false;
                }
            } else {
                if (firstStatus.getTextStatus() != secondStatus.getTextStatus()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * Refreshes the list of incoming and outgoing changes.
     */
    public void refresh() {
        PreferenceStore store = client.getPreferenceStore();
        Profile selectedProfile = Profile.getSelectedProfile(store);
        if (selectedProfile == null) {
            return;
        }
        log.info("Starting workspace changes refresh."); //$NON-NLS-1$
        if (refreshing) {
            log.info("Already refreshing, nothing to do."); //$NON-NLS-1$
            return;
        }
        refreshing = true;
        try {
            // update changes
            changeSets = getAllChanges(selectedProfile);

            // create the tree of changes from the list of all changes/conflicts 
            changeTree = new ChangeTree(changeSets.getChanges(), toIgnore);
            
            workspaceRoot = new File(selectedProfile.getWorkspaceFolder());
        } finally {
            refreshing = false;
        }
    }

    public boolean isRefreshListEmpty() {
        // FIXME: as ContentProvider does some filtering, this does
        // not always reflect the status in the GUI
        return changeSets.getFiles().size() == 0;
    }
    
    public void setMessage(String message) {
        if (containerRunnable != null) {
            containerRunnable.setMessage(message);
        }
    }
    
    public void setUpdateMessage(String message) {
        if (containerRunnable != null) {
            containerRunnable.setUpdateMessage(message);
        }
    }

    /**
     * Return if at least one team workspace of the current profile has
     * been checked out. Return false on e.g. first start-up.
     */
    public boolean hasCheckout() {
        // TODO: check if there are actually team dirs and
        //  .svn/.svnref dirs/files
        PreferenceStore store = client.getPreferenceStore();
        Profile selected = Profile.getSelectedProfile(store);
        if (selected == null) {
            return false;
        }
        if (selected.getType() == Profile.Type.MindquarryServer) {
	        File wsFolder = new File(selected.getWorkspaceFolder());
	        if (!wsFolder.exists() || wsFolder.list() == null || 
	                wsFolder.list().length == 0) {
	            log.debug("folder does not contain a checkout : " + 
	                    wsFolder.getAbsolutePath());
	            return false;
	        }
	        return true;
        } else {
        	for (SVNRepoData svnRepo : selected.getSvnRepos()) {
        		File folder = new File(svnRepo.localPath);
    	        if (folder.exists() && folder.list() != null && 
    	                folder.list().length > 0) {
    	        	// at least one found
    	        	return true;
    	        }
        	}
        	return false;
        }
    }
    
    public void showRefreshMessage(String message) {
        containerRunnable = new WorkspaceUpdateContainerRunnable(client, this,
                true, false, null, message);
        getDisplay().syncExec(containerRunnable);
    }

    public void showEmptyMessage(boolean isEmpty) {
        String emptyMessage = I18N.getString(
                "There are currently no workspace changes to synchronize,\n" +
                "i.e. there are no local changes and there are no changes on the server.\n" +
                "Last refresh: ")
                + new SimpleDateFormat().format(new Date()); //$NON-NLS-1$
        containerRunnable = new WorkspaceUpdateContainerRunnable(client, this,
                false, isEmpty, null, emptyMessage);
        getDisplay().syncExec(containerRunnable);
    }

    public void showMessage(String message, String icon) {
        containerRunnable = new WorkspaceUpdateContainerRunnable(client, this, false, true, icon, message);
            getDisplay().syncExec(containerRunnable);
    }

    // #########################################################################
    // ### PRIVATE METHODS
    // #########################################################################

    private boolean checkChangeSetUpdateRequired(ChangeSets changes1, ChangeSets changes2) {
        for (ChangeSet changes : changes2.getList()) {
            for (ChangeSet newChanges : changes1.getList()) {
                if (newChanges.getTeam().getId().equals(changes.getTeam().getId())) {
                    boolean equal = changesEqual(changes.getChanges(), 
                            newChanges.getChanges(), false);
                    if (!equal) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private ChangeSets getAllChanges(Profile selected) {
        log.debug("Getting all changes");
        ChangeSets changeSets = new ChangeSets();
        
        // getting list of all selected teams
        final List<Team> selectedTeams = new ArrayList<Team>();
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                selectedTeams.addAll(client.getSelectedTeams());
            }
        });
        
        try {
            toIgnore = new HashMap<File, Integer>();
            for (Team team : selectedTeams) {            
                setMessage(I18N.get(
                        "Refreshing workspaces changes for team \"{0}\" (team {1} of {2}) ...", //$NON-NLS-1$
                        team.getName(),
                        Integer.toString(selectedTeams.indexOf(team)+1),
                        Integer.toString(selectedTeams.size())));

                long startTime = System.currentTimeMillis();
        
                // TODO: also consider the case where the team folder exists
                // but is not a SVN checkout
                if (!team.dirExists(selected)) {
                	continue;
                }
                
                SVNSynchronizer sc =
                	team.createSynchronizer(selected, new InteractiveConflictHandler(client.getShell()));

                ChangeSet changeSet = new ChangeSet(team);

                // iterate over local changes first to avoid exceptions
                // later (in rare cases of "obstructed" state only):
                sc.cleanup();
                boolean teamHasObstruction = false;
                List<Status> tmpLocalChanges = sc.getLocalChanges();
                for (Status status : tmpLocalChanges) {
                    if (status.getTextStatus() == StatusKind.obstructed) {
                        changeSet.addChange(new ObstructedConflict(status));
                        teamHasObstruction = true;
                    }
                }
                
                // we need to stop here in case of obstruction,
                // as getChangesAndConflicts() would throw a
                // ClientException:
                if (teamHasObstruction) {
                    log.info("obstructed status, not calling getChangesAndConflicts()");
                    // skip further detection of conflicts, because it would break 
                } else { // no obstruction
                    List<Change> allChanges = sc.getChangesAndConflicts();
                    for (Change change : allChanges) {
                        Status status = change.getStatus();

                        // we don't know why yet, but files may sometimes be
                        // set to "normal". We ignore these files, i.e. treat
                        // them as non-modified:
                        // TODO: check whether that's actually necessary anymore 
                        if ((status.getTextStatus() == StatusKind.none ||
                                status.getTextStatus() == StatusKind.normal) &&
                                (status.getRepositoryTextStatus() == StatusKind.none || 
                                status.getRepositoryTextStatus() == StatusKind.normal)) {
                            log.debug("ignoring " + change + " ...");
                            continue;
                        }
                        if (status.getTextStatus() == StatusKind.external &&
                                (status.getRepositoryTextStatus() == StatusKind.none ||
                                 status.getRepositoryTextStatus() == StatusKind.external )) {
                            log.debug("ignoring " + change + " ...");
                            continue;
                        }

                        changeSet.addChange(change);
                    }
                }
                
                changeSets.add(changeSet);
                
                log.debug("conflicts and changes: " + changeSet);
                log.debug("internal svn files (to be ignored): " + toIgnore);
                log.debug("time required to find changes for team '" +team.getName()+ "': " +
                        (System.currentTimeMillis()-startTime) + "ms");
            }

            workspaceRoot = new File(selected.getWorkspaceFolder());

        } catch (ClientException e) {
            // TODO: handle exception
            // may happen on very first checkout (before checkout, actually)
            // may happen on network timeout
            // may happen on wrong credentials
            // may happen on wrong server url
            
            // list of seen apr errors:
            // 175002: connection refused
            
            log.error(e.toString() + " (apr error " + e.getAprError() + ")", e);
        } catch (IOException e) {
            log.error(e);
        }

        return changeSets;
    }

    // #########################################################################
    // ### INNER CLASSES
    // #########################################################################

    /**
     * Use the filter not as a filter, but to iterate over all files recursively.
     */
    class SetStatusFileFilter extends FileFileFilter {
        List<Status> subFiles = new ArrayList<Status>();

        public boolean accept(File file) {
            // make up a Status mostly with fake values, as only
            // path and StatusKind will be used:
            Status status = new Status(file.getAbsolutePath(), null,
                    NodeKind.file, 0, 0, 0, null, StatusKind.unversioned, 0, 0,
                    0, false, false, null, null, null, null, 0, false, null,
                    null, null, 0, null, 0, 0, 0, null);
            subFiles.add(status);
            return true;
        }

        List<Status> getSubFiles() {
            return subFiles;
        }
    }

    class SetStatusDirFilter extends DirectoryFileFilter {
        List<Status> subFiles = new ArrayList<Status>();

        public boolean accept(File file) {
            Status status = new Status(file.getAbsolutePath(), null,
                    NodeKind.dir, 0, 0, 0, null, StatusKind.unversioned, 0, 0,
                    0, false, false, null, null, null, null, 0, false, null,
                    null, null, 0, null, 0, 0, 0, null);
            subFiles.add(status);
            return true;
        }

        List<Status> getSubFiles() {
            return subFiles;
        }
    }

    public File getWorkspaceRoot() {
        return workspaceRoot;
    }
}
