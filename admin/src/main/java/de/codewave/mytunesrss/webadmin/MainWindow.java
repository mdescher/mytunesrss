/*
 * Copyright (c) 2010. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.webadmin;

import com.vaadin.terminal.Sizeable;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Window;
import de.codewave.mytunesrss.MessageWithParameters;
import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.vaadin.component.MessageWindow;

import java.util.NoSuchElementException;

public class MainWindow extends Window {

    public MainWindow(String caption, Panel panel) {
        super(caption);
        getContent().setWidth(100, Sizeable.UNITS_PERCENTAGE);
        addComponent(panel);
    }

    public void showComponent(Component component) {
        removeAllComponents();
        addComponent(component);
    }

    private MyTunesRssWebAdmin getMyTunesRssWebAdmin() {
        return (MyTunesRssWebAdmin) super.getApplication();
    }

    public String getBundleString(String key, Object... parameters) {
        return getMyTunesRssWebAdmin().getBundleString(key, parameters);
    }

    @Deprecated
    public void showDebugMessage(String message) {
        new MessageWindow(50, Sizeable.UNITS_EM, null, null, message, new Button(getBundleString("button.ok"))) {
            @Override
            protected void onClick(Button button) {
                // intentionally left blank
            }
        }.show(this);
    }

    public void showError(String messageKey, Object... parameters) {
        new MessageWindow(50, Sizeable.UNITS_EM, null, null, getBundleString(messageKey, parameters), new Button(getBundleString("button.ok"))) {
            @Override
            protected void onClick(Button button) {
                // intentionally left blank
            }
        }.show(this);
    }

    public void showWarning(String messageKey, Object... parameters) {
        new MessageWindow(50, Sizeable.UNITS_EM, null, null, getBundleString(messageKey, parameters), new Button(getBundleString("button.ok"))) {
            @Override
            protected void onClick(Button button) {
                // intentionally left blank
            }
        }.show(this);
    }

    public void showInfo(String messageKey, Object... parameters) {
        new MessageWindow(50, Sizeable.UNITS_EM, null, null, getBundleString(messageKey, parameters), new Button(getBundleString("button.ok"))) {
            @Override
            protected void onClick(Button button) {
                // intentionally left blank
            }
        }.show(this);
    }

    public void checkUnhandledException() {
        if (MyTunesRss.UNHANDLED_EXCEPTION.getAndSet(false)) {
            new MessageWindow(50, Sizeable.UNITS_EM, null, getBundleString("unhandledException.header"), getBundleString("unhandledException.detail"), new Button(getBundleString("button.ok"))) {
                @Override
                protected void onClick(Button button) {
                    // intentionally left blank
                }
            }.show(this);
        }
    }

    public void checkImportantMessage() {
        MessageWithParameters messageWithParameters = MyTunesRss.getImportantAdminMessage();
        if (messageWithParameters != null) {
            new MessageWindow(50, Sizeable.UNITS_EM, null, getBundleString("importantAdminMessage.header"), getBundleString(messageWithParameters.getMessage(), messageWithParameters.getParameters()), new Button(getBundleString("button.ok"))) {
                @Override
                protected void onClick(Button button) {
                    // intentionally left blank
                }
            }.show(this);
        }
    }
}
