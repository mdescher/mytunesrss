/*
 * Copyright (c) 2012. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.webadmin;

import com.vaadin.terminal.Sizeable;
import com.vaadin.ui.*;
import de.codewave.mytunesrss.config.ApertureDatasourceConfig;
import de.codewave.mytunesrss.config.IphotoDatasourceConfig;
import de.codewave.mytunesrss.config.ReplacementRule;
import de.codewave.vaadin.SmartTextField;
import de.codewave.vaadin.VaadinUtils;
import de.codewave.vaadin.component.OptionWindow;
import de.codewave.vaadin.validation.ValidRegExpValidator;

public class ApertureDatasourceOptionsPanel extends MyTunesRssConfigPanel {

    private Table myPathReplacements;
    private Button myAddPathReplacement;
    private ApertureDatasourceConfig myConfig;

    public ApertureDatasourceOptionsPanel(ApertureDatasourceConfig config) {
        myConfig = config;
    }

    @Override
    public void attach() {
        super.attach();
        init(null, getComponentFactory().createGridLayout(1, 2, true, true));

        Panel replacementsPanel = new Panel(getBundleString("datasourceOptionsPanel.caption.aperture.replacements"), getComponentFactory().createVerticalLayout(true, true));
        addComponent(replacementsPanel);
        myPathReplacements = new Table();
        myPathReplacements.setCacheRate(50);
        myPathReplacements.addContainerProperty("search", TextField.class, null, getBundleString("datasourceOptionsPanel.replaceSearch"), null, null);
        myPathReplacements.addContainerProperty("replace", TextField.class, null, getBundleString("datasourceOptionsPanel.replaceReplace"), null, null);
        myPathReplacements.addContainerProperty("delete", Button.class, null, "", null, null);
        myPathReplacements.setEditable(false);
        replacementsPanel.addComponent(myPathReplacements);
        myAddPathReplacement = getComponentFactory().createButton("datasourceOptionsPanel.addReplacement", this);
        replacementsPanel.addComponent(getComponentFactory().createHorizontalButtons(false, true, myAddPathReplacement));

        addDefaultComponents(0, 1, 0, 1, false);

        initFromConfig();
    }

    @Override
    protected void writeToConfig() {
        myConfig.clearPathReplacements();
        for (Object itemId : myPathReplacements.getItemIds()) {
            myConfig.addPathReplacement(new ReplacementRule((String) getTableCellPropertyValue(myPathReplacements, itemId, "search"), (String) getTableCellPropertyValue(myPathReplacements, itemId, "replace")));
        }
    }

    @Override
    protected void initFromConfig() {
        myPathReplacements.removeAllItems();
        for (ReplacementRule replacement : myConfig.getPathReplacements()) {
            addPathReplacement(replacement);
        }
        setTablePageLengths();
    }

    private void addPathReplacement(ReplacementRule replacement) {
        SmartTextField searchTextField = new SmartTextField();
        searchTextField.setValue(replacement.getSearchPattern());
        searchTextField.addValidator(new ValidRegExpValidator("datasourcesConfigPanel.error.invalidSearchExpression"));
        searchTextField.setImmediate(true);
        myPathReplacements.addItem(new Object[]{searchTextField, new SmartTextField(null, replacement.getReplacement()), getComponentFactory().createButton("button.delete", this)}, myItemIdGenerator.getAndIncrement());
    }

    private void setTablePageLengths() {
        myPathReplacements.setPageLength(Math.min(myPathReplacements.getItemIds().size(), 5));
    }

    protected boolean beforeSave() {
        if (!VaadinUtils.isValid(myPathReplacements)) {
            ((MainWindow) VaadinUtils.getApplicationWindow(this)).showError("error.formInvalid");
        } else {
            writeToConfig();
            closeWindow();
        }
        return false; // make sure the default operation is not used
    }

    @Override
    protected boolean beforeCancel() {
        closeWindow();
        return false; // make sure the default operation is not used
    }

    private void closeWindow() {
        getWindow().getParent().removeWindow(getWindow());
    }

    public void buttonClick(final Button.ClickEvent clickEvent) {
        if (clickEvent.getSource() == myAddPathReplacement) {
            addPathReplacement(new ReplacementRule("^.*$", "\\0"));
            setTablePageLengths();
        } else if (findTableItemWithObject(myPathReplacements, clickEvent.getSource()) != null) {
            final Button yes = new Button(getBundleString("button.yes"));
            Button no = new Button(getBundleString("button.no"));
            new OptionWindow(30, Sizeable.UNITS_EM, null, getBundleString("datasourcesConfigDialog.optionWindowDeletePathReplacement.caption"), getBundleString("datasourcesConfigDialog.optionWindowDeletePathReplacement.message"), yes, no) {
                public void clicked(Button button) {
                    if (button == yes) {
                        myPathReplacements.removeItem(findTableItemWithObject(myPathReplacements, clickEvent.getSource()));
                        setTablePageLengths();
                    }
                }
            }.show(VaadinUtils.getApplicationWindow(this));
        } else {
            super.buttonClick(clickEvent);
        }
    }
}