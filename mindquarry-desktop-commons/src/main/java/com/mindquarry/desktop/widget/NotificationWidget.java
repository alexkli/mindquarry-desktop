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
package com.mindquarry.desktop.widget;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A widget for displaying notification messages.
 * 
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class NotificationWidget {
    private static final int WIDTH = 200;

    private static final int HEIGHT = 100;

    private final Display display;

    private Shell shell;

    private Label titleLabel;

    private Label msgLabel;

    private Image img;

    private int currentHeight;

    public NotificationWidget(Display display) {
        this.display = display;
        this.display.syncExec(new Runnable() {
            public void run() {
                createContents();
            }
        });
    }

    private void createContents() {
        shell = new Shell(SWT.ON_TOP);
        shell.setLocation(display.getBounds().width - WIDTH, display
                .getBounds().height);
        shell.setLocation(400, 400);
        shell.setSize(WIDTH, HEIGHT);

        img = new Image(null, getClass().getResourceAsStream(
                "/com/mindquarry/icons/16x16/logo/mindquarry-icon.png")); //$NON-NLS-1$

        Label icon = new Label(shell, SWT.CENTER);
        icon.setSize(20, 20);
        icon.setImage(img);

        titleLabel = new Label(shell, SWT.LEFT);
        titleLabel.setLocation(22, 2);
        titleLabel.setSize(WIDTH - 26, 20);
        titleLabel.setFont(new Font(display, "Arial", 9, SWT.BOLD)); //$NON-NLS-1$

        msgLabel = new Label(shell, SWT.WRAP);
        msgLabel.setBackground(display.getSystemColor(SWT.COLOR_CYAN));
        msgLabel.setLocation(2, titleLabel.getSize().y + 2);
        msgLabel.setSize(WIDTH - 6, HEIGHT - titleLabel.getSize().y - 6);
        
        shell.open();
    }

    public void show(final String title, final String message,
            final long duration) {
        display.syncExec(new Runnable() {
            public void run() {
                titleLabel.setText(title);
                msgLabel.setText(message);

                titleLabel.redraw();
                msgLabel.redraw();

                currentHeight = shell.getSize().y;
            }
        });
        moveUp();
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            // nothing to do here
        }
        moveDown();
    }

    private void moveUp() {
        for (int i = 0; i <= currentHeight; i++) {
            updatePosition(true);
            redrawAndSleep();
        }
    }

    private void moveDown() {
        for (int i = 0; i <= currentHeight; i++) {
            updatePosition(false);
            redrawAndSleep();
        }
    }

    private void updatePosition(final boolean up) {
        display.syncExec(new Runnable() {
            public void run() {
                int x = shell.getLocation().x;
                int y = shell.getLocation().y;
                if (up) {
                    shell.setLocation(x, --y);
                } else {
                    shell.setLocation(x, ++y);
                }
            }
        });
    }

    private void redrawAndSleep() {
        display.syncExec(new Runnable() {
            public void run() {
                shell.redraw(shell.getLocation().x, shell.getLocation().y,
                        shell.getSize().x, shell.getSize().y, true);
            }
        });
        // short sleep for better movement look&feel
        try {
            Thread.sleep(6);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void dispose() {
        display.syncExec(new Runnable() {
            public void run() {
                shell.dispose();
                img.dispose();
            }
        });
    }

    public boolean isDisposed() {
        if (shell.isDisposed()) {
            if (!img.isDisposed()) {
                img.dispose();
            }
        }
        return shell.isDisposed();
    }
}
