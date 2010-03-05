/*
 * Copyright (c) 2010. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.webadmin;

import com.vaadin.data.Property;
import com.vaadin.terminal.Sizeable;
import com.vaadin.ui.*;
import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.job.MyTunesRssJobUtils;
import de.codewave.mytunesrss.settings.Database;
import de.codewave.vaadin.ComponentFactory;
import de.codewave.vaadin.OptionWindow;
import de.codewave.vaadin.VaadinUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class DatabaseConfigPanel extends MyTunesRssConfigPanel implements Property.ValueChangeListener {

    private AtomicLong myTriggerIdGenerator;
    private Form myDatabaseTypeForm;
    private Form myMiscOptionsForm;
    private Panel mySchedulesPanel;
    private Select myDatabaseType;
    private TextField myDatabaseDriver;
    private TextField myDatabaseConnection;
    private TextField myDatabaseUser;
    private TextField myDatabasePassword;
    private CheckBox myUpdateDatabaseOnServerStart;
    private CheckBox myItunesDeleteMissingFiles;
    private Table myCronTriggers;
    private Button myAddSchedule;

    public DatabaseConfigPanel(ComponentFactory componentFactory) {
        super(MyTunesRssWebAdminUtils.getBundleString("databaseConfigPanel.caption"), componentFactory.createGridLayout(1, 4, true, true), componentFactory);
    }

    protected void init(ComponentFactory componentFactory) {
        myTriggerIdGenerator = new AtomicLong();
        myDatabaseType = componentFactory.createSelect("databaseConfigPanel.databaseType", Arrays.asList(Database.DatabaseType.h2, Database.DatabaseType.h2custom, Database.DatabaseType.postgres, Database.DatabaseType.mysql));
        myDatabaseType.addListener(this);
        myDatabaseDriver = componentFactory.createTextField("databaseConfigPanel.databaseDriver");
        myDatabaseConnection = componentFactory.createTextField("databaseConfigPanel.databaseConnection");
        myDatabaseUser = componentFactory.createTextField("databaseConfigPanel.databaseUser");
        myDatabasePassword = componentFactory.createPasswordTextField("databaseConfigPanel.databasePassword");
        myUpdateDatabaseOnServerStart = componentFactory.createCheckBox("databaseConfigPanel.updateDatabaseOnServerStart");
        myItunesDeleteMissingFiles = componentFactory.createCheckBox("databaseConfigPanel.itunesDeleteMissingFiles");
        myCronTriggers = new Table();
        myCronTriggers.addContainerProperty("day", Select.class, null, MyTunesRssWebAdminUtils.getBundleString("databaseConfigPanel.cronTriggers.day"), null, null);
        myCronTriggers.addContainerProperty("hour", Select.class, null, MyTunesRssWebAdminUtils.getBundleString("databaseConfigPanel.cronTriggers.hour"), null, null);
        myCronTriggers.addContainerProperty("minute", Select.class, null, MyTunesRssWebAdminUtils.getBundleString("databaseConfigPanel.cronTriggers.minute"), null, null);
        myCronTriggers.addContainerProperty("delete", Button.class, null, "", null, null);
        myCronTriggers.setEditable(true);
        myAddSchedule = componentFactory.createButton("databaseConfigPanel.addSchedule", this);

        myDatabaseTypeForm = componentFactory.createForm(null, true);
        myDatabaseTypeForm.addField(myDatabaseType, myDatabaseType);
        myDatabaseTypeForm.addField(myDatabaseDriver, myDatabaseDriver);
        myDatabaseTypeForm.addField(myDatabaseConnection, myDatabaseConnection);
        myDatabaseTypeForm.addField(myDatabaseUser, myDatabaseUser);
        myDatabaseTypeForm.addField(myDatabasePassword, myDatabasePassword);
        addComponent(componentFactory.surroundWithPanel(myDatabaseTypeForm, FORM_PANEL_MARGIN_INFO, MyTunesRssWebAdminUtils.getBundleString("databaseConfigPanel.caption.database")));

        myMiscOptionsForm = componentFactory.createForm(null, true);
        myMiscOptionsForm.addField(myUpdateDatabaseOnServerStart, myUpdateDatabaseOnServerStart);
        myMiscOptionsForm.addField(myItunesDeleteMissingFiles, myItunesDeleteMissingFiles);
        addComponent(componentFactory.surroundWithPanel(myMiscOptionsForm, FORM_PANEL_MARGIN_INFO, MyTunesRssWebAdminUtils.getBundleString("databaseConfigPanel.caption.misc")));

        mySchedulesPanel = new Panel(MyTunesRssWebAdminUtils.getBundleString("databaseConfigPanel.caption.cronTriggers"), componentFactory.createVerticalLayout(true, true));
        mySchedulesPanel.addComponent(myCronTriggers);
        mySchedulesPanel.addComponent(myAddSchedule);
        addComponent(mySchedulesPanel);

        addMainButtons(0, 3, 0, 3);
    }

    protected void initFromConfig() {
        myDatabaseType.select(Database.DatabaseType.valueOf(MyTunesRss.CONFIG.getDatabaseType()));
        myDatabaseDriver.setValue(MyTunesRss.CONFIG.getDatabaseDriver());
        myDatabaseConnection.setValue(MyTunesRss.CONFIG.getDatabaseConnection());
        myDatabaseUser.setValue(MyTunesRss.CONFIG.getDatabaseUser());
        myDatabasePassword.setValue(MyTunesRss.CONFIG.getDatabasePassword());
        myUpdateDatabaseOnServerStart.setValue(MyTunesRss.CONFIG.isUpdateDatabaseOnServerStart());
        myItunesDeleteMissingFiles.setValue(MyTunesRss.CONFIG.isItunesDeleteMissingFiles());
        showHideDatabaseDetails(Database.DatabaseType.valueOf(MyTunesRss.CONFIG.getDatabaseType()));
        refreshCronTriggers();
    }

    private void refreshCronTriggers() {
        myCronTriggers.removeAllItems();
        for (String cronTrigger : MyTunesRss.CONFIG.getDatabaseCronTriggers()) {
            addCronTrigger(cronTrigger);
        }
        setCronTriggersPageLength();
    }

    private void addCronTrigger(String cronTrigger) {
        String[] cronTriggerParts = cronTrigger.split(" ");
        Select daySelect = getComponentFactory().createSelect(null, Arrays.asList(MyTunesRssJobUtils.getDays()));
        daySelect.select(new MyTunesRssJobUtils.TriggerItem(cronTriggerParts[5], null));
        Select hourSelect = getComponentFactory().createSelect(null, Arrays.asList(MyTunesRssJobUtils.getHours()));
        hourSelect.select(new MyTunesRssJobUtils.TriggerItem(cronTriggerParts[2], null));
        Select minuteSelect = getComponentFactory().createSelect(null, Arrays.asList(MyTunesRssJobUtils.getMinutes()));
        minuteSelect.select(new MyTunesRssJobUtils.TriggerItem(cronTriggerParts[1], null));
        Button deleteButton = new Button(MyTunesRssWebAdminUtils.getBundleString("databaseConfigPanel.cronTriggers.delete"), this);
        myCronTriggers.addItem(new Object[]{daySelect, hourSelect, minuteSelect, deleteButton}, myTriggerIdGenerator.getAndIncrement());
        setCronTriggersPageLength();
    }

    private void setCronTriggersPageLength() {
        myCronTriggers.setPageLength(Math.min(myCronTriggers.getItemIds().size(), 10));
    }

    private void showHideDatabaseDetails(Database.DatabaseType type) {
        myDatabaseDriver.setEnabled(type != Database.DatabaseType.h2);
        myDatabaseConnection.setEnabled(type != Database.DatabaseType.h2);
        myDatabaseUser.setEnabled(type != Database.DatabaseType.h2);
        myDatabasePassword.setEnabled(type != Database.DatabaseType.h2);
        if (type == Database.DatabaseType.h2) {
            MyTunesRssWebAdminUtils.setOptional(myDatabaseDriver);
            MyTunesRssWebAdminUtils.setOptional(myDatabaseConnection);
            MyTunesRssWebAdminUtils.setOptional(myDatabaseUser);
            MyTunesRssWebAdminUtils.setOptional(myDatabasePassword);
        } else {
            MyTunesRssWebAdminUtils.setRequired(myDatabaseDriver);
            MyTunesRssWebAdminUtils.setRequired(myDatabaseConnection);
            MyTunesRssWebAdminUtils.setRequired(myDatabaseUser);
            MyTunesRssWebAdminUtils.setRequired(myDatabasePassword);
        }
    }

    protected void writeToConfig() {
        MyTunesRss.CONFIG.setDatabaseType(((Database.DatabaseType) myDatabaseType.getValue()).name());
        MyTunesRss.CONFIG.setDatabaseDriver((String) myDatabaseDriver.getValue());
        MyTunesRss.CONFIG.setDatabaseConnection((String) myDatabaseConnection.getValue());
        MyTunesRss.CONFIG.setDatabaseUser((String) myDatabaseUser.getValue());
        MyTunesRss.CONFIG.setDatabasePassword((String) myDatabasePassword.getValue());
        MyTunesRss.CONFIG.setUpdateDatabaseOnServerStart(myUpdateDatabaseOnServerStart.booleanValue());
        MyTunesRss.CONFIG.setItunesDeleteMissingFiles(myItunesDeleteMissingFiles.booleanValue());
        List<String> databaseCronTriggers = new ArrayList<String>();
        for (Object itemId : myCronTriggers.getItemIds()) {
            databaseCronTriggers.add("0 " + getTableCellString(itemId, "minute") + " " + getTableCellString(itemId, "hour") + " ? * " + getTableCellString(itemId, "day"));
        }
        MyTunesRss.CONFIG.setDatabaseCronTriggers(databaseCronTriggers);
        MyTunesRssJobUtils.scheduleDatabaseJob();
    }

    private String getTableCellString(Object itemId, String property) {
        Select select = (Select) myCronTriggers.getItem(itemId).getItemProperty(property).getValue();
        MyTunesRssJobUtils.TriggerItem triggerItem = (MyTunesRssJobUtils.TriggerItem) select.getValue();
        return triggerItem.getKey();
    }

    public void buttonClick(Button.ClickEvent clickEvent) {
        if (clickEvent.getSource() == myAddSchedule) {
            addCronTrigger("0 0 0 ? * SUN-SAT");
        } else {
            final Long cronTriggerToDelete = getCronTriggerToDelete(clickEvent.getSource());
            if (cronTriggerToDelete != null) {
                final Button yes = new Button(getBundleString("button.yes"));
                Button no = new Button(getBundleString("button.no"));
                new OptionWindow(30, Sizeable.UNITS_EM, null, getBundleString("databaseConfigDialog.optionWindowDeleteCronTrigger.caption"), getBundleString("databaseConfigDialog.optionWindowDeleteCronTrigger.message"), yes, no) {
                    public void clicked(Button button) {
                        if (button == yes) {
                            myCronTriggers.removeItem(cronTriggerToDelete);
                            setCronTriggersPageLength();
                        }
                    }
                }.show(getApplication().getMainWindow());
            } else {
                super.buttonClick(clickEvent);
            }
        }
    }

    protected boolean beforeSave() {
        if (VaadinUtils.isValid(myDatabaseTypeForm, myMiscOptionsForm)) {
            if (isDatabaseChanged()) {
                getApplication().showWarning("databaseConfigPanel.warning.databaseChanged");
            }
            return true;
        } else {
            getApplication().showError("error.formInvalid");
        }
        return false;
    }

    private Long getCronTriggerToDelete(Object source) {
        for (Long itemId : (Collection<Long>) myCronTriggers.getItemIds()) {
            if (source == myCronTriggers.getItem(itemId).getItemProperty("delete").getValue()) {
                return itemId;
            }
        }
        return null;
    }

    public void valueChange(Property.ValueChangeEvent valueChangeEvent) {
        if (((EventObject) valueChangeEvent).getSource() == myDatabaseType) {
            showHideDatabaseDetails((Database.DatabaseType) valueChangeEvent.getProperty().getValue());
        }
    }

    private boolean isDatabaseChanged() {
        Database.DatabaseType databaseType = (Database.DatabaseType) myDatabaseType.getValue();
        String newType = StringUtils.trimToEmpty(databaseType.name());
        String newDriver = StringUtils.trimToEmpty((String) myDatabaseDriver.getValue());
        String newConnect = StringUtils.trimToEmpty((String) myDatabaseConnection.getValue());
        String newUser = StringUtils.trimToEmpty((String) myDatabaseUser.getValue());
        String newPass = StringUtils.trimToEmpty((String) myDatabasePassword.getValue());
        if (!newType.equals(MyTunesRss.CONFIG.getDatabaseType())) {
            return true;
        }
        if (!newDriver.equals(MyTunesRss.CONFIG.getDatabaseDriver())) {
            return true;
        }
        if (!newConnect.equals(MyTunesRss.CONFIG.getDatabaseConnection())) {
            return true;
        }
        if (!newUser.equals(MyTunesRss.CONFIG.getDatabaseUser())) {
            return true;
        }
        if (!newPass.equals(MyTunesRss.CONFIG.getDatabasePassword())) {
            return true;
        }
        return false;
    }
}