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
package com.mindquarry.desktop.model.team;

import org.dom4j.Element;
import org.dom4j.Node;

import com.mindquarry.desktop.model.ModelBase;
import com.mindquarry.desktop.model.TransformerBase;

import dax.Path;

/**
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class TeamListTransformer extends TransformerBase {
    private TeamList teamList = null;

    private String baseURL;

    private String login;

    private String password;

    public TeamListTransformer(String login, String password) {
        this.login = login;
        this.password = password;
    }

    @Override
    protected void handleModelPart(ModelBase model) {
        teamList = (TeamList) model;
    }

    @Path("/teamspaces")
    public void teamspaces(Node node) {
        if (node instanceof Element) {
            Element element = (Element) node;
            baseURL = element.attribute("base").getStringValue(); //$NON-NLS-1$
        }
        applyTemplates();
    }

    @Path("teamspace")
    public void teamspace(Node node) {
        if (node instanceof Element) {
            Element element = (Element) node;
            teamList.add(baseURL + element.attribute("href").getStringValue()//$NON-NLS-1$ 
                    + "/", //$NON-NLS-1$
                    login, password);
        }
    }

    public TeamList getTeamList() {
        return teamList;
    }
}
