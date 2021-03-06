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
package com.mindquarry.desktop.client.widget.team;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.mindquarry.desktop.client.I18N;
import com.mindquarry.desktop.model.team.Team;

/**
 * Label provider for the Task table.
 * 
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class TeamlistLabelProvider extends ColumnLabelProvider {
	
	public Image getImage(Object element) {
		return null;
	}

	public String getText(Object element) {
		Team team = (Team) element;
		return team.getName();
	}

	public String getToolTipText(Object element) {
		Team team = (Team) element;

		final int maxLength = 100;
		String text = ""; //$NON-NLS-1$
		String name = team.getName();
		if (name != null) {
			name = name.length() > maxLength ? name.substring(0, maxLength)
					+ "..." : name; //$NON-NLS-1$
		} else {
			name = "-"; //$NON-NLS-1$
		}
		text += I18N.getString("Name") //$NON-NLS-1$
				+ ": " + name; //$NON-NLS-1$ 
		return text;
	}

	public int getToolTipTimeDisplayed(Object object) {
		return 2000;
	}

	public int getToolTipDisplayDelayTime(Object object) {
		return 10;
	}
}
