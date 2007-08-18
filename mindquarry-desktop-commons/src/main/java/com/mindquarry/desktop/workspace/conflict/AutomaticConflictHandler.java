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
package com.mindquarry.desktop.workspace.conflict;

import java.io.File;

import com.mindquarry.desktop.workspace.exception.CancelException;

/**
 * An {@link ConflictHandler} that automatically chooses the best solution for
 * all conflicts that ensure no data is lost.
 * 
 * @author <a href="mailto:alexander(dot)klimetschek(at)mindquarry(dot)com">
 *         Alexander Klimetschek</a>
 *
 */
public class AutomaticConflictHandler implements ConflictHandler {
    
    private ConflictPrinter printer;

    public AutomaticConflictHandler(String workingCopyPath) {
        this.printer = new ConflictPrinter(workingCopyPath);
    }

    private static int uniqueCounter = 0;
    
    public void handle(AddConflict conflict) throws CancelException {
        printer.printConflict(conflict);
        
        String oldName = new File(conflict.getStatus().getPath()).getName();
        String newName;
        do {
            uniqueCounter++;
            newName = oldName + "_renamed_" + uniqueCounter;
        } while (!conflict.isRenamePossible(newName));

        conflict.doRename(newName);
    }
    
    public void handle(ModificationInDeletedConflict conflict)
            throws CancelException {
        printer.printConflict(conflict);
        
        conflict.doReAdd();
    }

    public void handle(DeleteWithModificationConflict conflict)
            throws CancelException {
        printer.printConflict(conflict);

        conflict.doOnlyKeepModified();
    }

}
