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
package com.mindquarry.desktop.model.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.Node;

import com.mindquarry.desktop.model.ModelBase;
import com.mindquarry.desktop.model.TransformerBase;

import dax.Path;

/**
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class TaskTransformer extends TransformerBase {
    private Log log;

    private Task task = null;

    public TaskTransformer() {
        log = LogFactory.getLog(TaskTransformer.class);
    }

    @Override
    protected void handleModelPart(ModelBase model) {
        task = (Task) model;
    }

    @Path("/task")
    public void task(Node node) {
        if (node instanceof Element) {
            Element element = (Element) node;

            log.info("Retrieved new task description from " //$NON-NLS-1$
                    + element.attribute("base").getStringValue()); //$NON-NLS-1$
            task.setId(element.attribute("base").getStringValue()); //$NON-NLS-1$
        }
        applyTemplates(node);
    }

    @Path("title")
    public void title(Node node) {
        task.setTitle(node.getStringValue().trim());
    }

    @Path("status")
    public void status(Node node) {
        task.setStatus(node.getStringValue().trim());
    }

    @Path("priority")
    public void priority(Node node) {
        task.setPriority(node.getStringValue().trim());
    }

    @Path("summary")
    public void summary(Node node) {
        task.setSummary(node.getStringValue().trim());
    }

    @Path("description")
    public void description(Node node) {
        task.setDescription(node.getStringValue().trim());
    }

    @Path("date")
    public void date(Node node) {
        task.setDate(node.getStringValue().trim());
    }
    
    @Path("targettime")
    public void targettime(Node node) {
    	task.setTargetTime(node.getStringValue().trim());
	}

    @Path("actualtime")
    public void actualtime(Node node) {
    	task.setActualTime(node.getStringValue().trim());
    }

    @Path("people/item")
    public void people(Node node) {
        Node personID = node.selectSingleNode("./person"); //$NON-NLS-1$
        Node role = node.selectSingleNode("./role"); //$NON-NLS-1$
        task.addPerson(personID.getText(), role.getText());
    }

    @Path("dependencies/item")
    public void dependencies(Node node) {
        Node taskID = node.selectSingleNode("./task"); //$NON-NLS-1$
        Node role = node.selectSingleNode("./role"); //$NON-NLS-1$
        task.addDependency(taskID.getText(), role.getText());
    }

    public Task getTask() {
        return task;
    }
}
