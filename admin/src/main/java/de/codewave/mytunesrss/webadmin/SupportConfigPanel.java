/*
 * Copyright (c) 2010. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.webadmin;

import com.vaadin.terminal.Sizeable;
import com.vaadin.ui.*;
import de.codewave.mytunesrss.*;
import de.codewave.mytunesrss.task.SendSupportRequestCallable;
import de.codewave.vaadin.SmartTextField;
import de.codewave.vaadin.component.SinglePanelWindow;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;

import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class SupportConfigPanel extends MyTunesRssConfigPanel implements Upload.Receiver, Upload.SucceededListener, Upload.FailedListener {

    private Form mySupportForm;
    private Form myRegistrationForm;
    private Form mySysInfoForm;
    private Select myLogLevel;
    private Button myShowLog;
    private SmartTextField myName;
    private SmartTextField myEmail;
    private SmartTextField myDescription;
    private CheckBox myIncludeItunesXml;
    private Button mySendSupport;
    private SmartTextField myRegName;
    private DateField myExpirationDate;
    private Upload myUploadLicense;
    private SmartTextField mySysInfo;
    private File myUploadDir;

    public void attach() {
        init(getBundleString("supportConfigPanel.caption"), getComponentFactory().createGridLayout(1, 4, true, true));
        mySupportForm = getComponentFactory().createForm(null, true);
        myName = getComponentFactory().createTextField("supportConfigPanel.name");
        myEmail = getComponentFactory().createTextField("supportConfigPanel.email");
        myDescription = getComponentFactory().createTextField("supportConfigPanel.description");
        myDescription.setRows(10);
        myIncludeItunesXml = getComponentFactory().createCheckBox("supportConfigPanel.includeItunesXml");
        mySendSupport = getComponentFactory().createButton("supportConfigPanel.sendSupport", this);
        mySupportForm.addField("name", myName);
        mySupportForm.addField("email", myEmail);
        mySupportForm.addField("description", myDescription);
        mySupportForm.addField("includeItunesXml", myIncludeItunesXml);
        mySupportForm.addField("sendSupport", mySendSupport);
        addComponent(getComponentFactory().surroundWithPanel(mySupportForm, FORM_PANEL_MARGIN_INFO, getBundleString("supportConfigPanel.caption.support")));
        myRegistrationForm = getComponentFactory().createForm(null, true);
        myRegName = getComponentFactory().createTextField("supportConfigPanel.regName");
        myRegName.setEnabled(false);
        myExpirationDate = new DateField(getBundleString("supportConfigPanel.expirationDate"));
        myExpirationDate.setDateFormat(MyTunesRssUtils.getBundleString(Locale.getDefault(), "common.dateFormat"));
        myExpirationDate.setResolution(DateField.RESOLUTION_DAY);
        myExpirationDate.setEnabled(false);
        myRegistrationForm.addField("regName", myRegName);
        myRegistrationForm.addField("expirationDate", myExpirationDate);
        Panel registrationPanel = getComponentFactory().surroundWithPanel(myRegistrationForm, new Layout.MarginInfo(false, true, true, true), getBundleString("supportConfigPanel.caption.registration"));
        if (!MyTunesRss.REGISTRATION.isReleaseVersion()) {
            Label label = new Label(getBundleString("supportConfigPanel.prereleaseCannotBeRegistered"));
            registrationPanel.addComponent(label);
        } else {
            myUploadLicense = new Upload(null, this);
            myUploadLicense.setButtonCaption(getBundleString("supportConfigPanel.uploadLicense"));
            myUploadLicense.setImmediate(true);
            myUploadLicense.addListener((Upload.SucceededListener) this);
            myUploadLicense.addListener((Upload.FailedListener) this);
            registrationPanel.addComponent(myUploadLicense);
        }
        addComponent(registrationPanel);
        mySysInfoForm = getComponentFactory().createForm(null, true);
        myLogLevel = getComponentFactory().createSelect("supportConfigPanel.logLevel", Arrays.asList(Level.OFF, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG));
        myShowLog = getComponentFactory().createButton("supportConfigPanel.showLog", this);
        mySysInfo = getComponentFactory().createTextField("supportConfigPanel.sysInfo");
        mySysInfo.setEnabled(false);
        mySysInfo.setRows(5);
        mySysInfoForm.addField("logLevel", myLogLevel);
        mySysInfoForm.addField("showLog", myShowLog);
        mySysInfoForm.addField("sysInfo", mySysInfo);
        addComponent(getComponentFactory().surroundWithPanel(mySysInfoForm, FORM_PANEL_MARGIN_INFO, getBundleString("supportConfigPanel.caption.sysInfo")));

        attach(0, 3, 0, 3);

        initFromConfig();
    }

    protected void initFromConfig() {
        myLogLevel.setValue(MyTunesRss.CONFIG.getCodewaveLogLevel());
        myName.setValue(MyTunesRss.CONFIG.getSupportName());
        myEmail.setValue(MyTunesRss.CONFIG.getSupportEmail());
        myRegName.setValue(MyTunesRss.REGISTRATION.getName());
        if (MyTunesRss.REGISTRATION.isExpirationDate()) {
            myExpirationDate.setValue(new Date(MyTunesRss.REGISTRATION.getExpiration()));
        }
        mySysInfo.setValue(MyTunesRssUtils.getSystemInfo());
    }

    protected void writeToConfig() {
        MyTunesRss.CONFIG.setCodewaveLogLevel((Level) myLogLevel.getValue());
        MyTunesRss.CONFIG.setSupportName((String) myName.getValue());
        MyTunesRss.CONFIG.setSupportEmail((String) myEmail.getValue());
        MyTunesRss.CONFIG.save();
    }

    public void buttonClick(Button.ClickEvent clickEvent) {
        if (clickEvent.getSource() == mySendSupport) {
            if (StringUtils.isNotBlank((String) myName.getValue()) && StringUtils.isNotBlank((String) myEmail.getValue()) && StringUtils.isNotBlank((String) myDescription.getValue())) {
                SendSupportRequestCallable requestCallable = new SendSupportRequestCallable((String) myName.getValue(), (String) myEmail.getValue(), (String) myDescription.getValue() + "\n\n\n", (Boolean) myIncludeItunesXml.getValue());
                if (requestCallable.call()) {
                    getApplication().showInfo("supportConfigPanel.info.supportRequestSent");
                } else {
                    getApplication().showError("supportConfigPanel.error.supportRequestFailed");
                }
            } else {
                getApplication().showError("supportConfigPanel.error.allFieldsMandatoryForSupport");
            }
        } else if (clickEvent.getSource() == myShowLog) {
            LogPanel logPanel = new LogPanel();
            SinglePanelWindow logWindow = new SinglePanelWindow(80, Sizeable.UNITS_PERCENTAGE, null, getBundleString("supportConfigPanel.logWindowTitle"), logPanel);
            logWindow.setHeight(80, Sizeable.UNITS_PERCENTAGE);
            logWindow.setClosable(true);
            logWindow.setResizable(true);
            logWindow.setDraggable(true);
            logWindow.show(getWindow());
        } else {
            super.buttonClick(clickEvent);
        }
    }

    public OutputStream receiveUpload(String filename, String MIMEType) {
        try {
            myUploadDir = new File(MyTunesRssUtils.getCacheDataPath() + "/license-upload");
            if (!myUploadDir.isDirectory()) {
                myUploadDir.mkdir();
            }
            return new FileOutputStream(new File(myUploadDir, filename));
        } catch (IOException e) {
            throw new RuntimeException("Could not receive upload.", e);
        }
    }

    public void uploadFailed(Upload.FailedEvent event) {
        FileUtils.deleteQuietly(myUploadDir);
        getApplication().showError("supportConfigPanel.error.licenseUploadFailed");
    }

    public void uploadSucceeded(Upload.SucceededEvent event) {
        try {
            MyTunesRssRegistration registration = MyTunesRssRegistration.register(new File(myUploadDir, event.getFilename()));
            getApplication().showInfo("supportConfigPanel.info.licenseOk", registration.getName());
        } catch (MyTunesRssRegistrationException e) {
            switch (e.getErrror()) {
                case InvalidFile:
                    getApplication().showError("supportConfigPanel.error.invalidLicenseFile");
                    break;
                case LicenseExpired:
                    getApplication().showError("supportConfigPanel.error.licenseExpired");
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected error code \"" + e.getErrror() + "\".");
            }
        } finally {
            FileUtils.deleteQuietly(myUploadDir);
        }
    }
}