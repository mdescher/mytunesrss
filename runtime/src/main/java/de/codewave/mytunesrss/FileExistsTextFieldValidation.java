package de.codewave.mytunesrss;

import org.apache.commons.lang.*;

import javax.swing.*;
import java.io.*;

/**
 * de.codewave.mytunesrss.NotEmptyTextFieldValidation
 */
public class FileExistsTextFieldValidation extends JTextFieldValidation {
    private boolean myAllowEmptyField;
    private boolean myAllowDirectory;

    public FileExistsTextFieldValidation(JTextField textField, boolean allowEmptyField, boolean allowDirectory) {
        super(textField);
        myAllowEmptyField = allowEmptyField;
        myAllowDirectory = allowDirectory;
    }

    public FileExistsTextFieldValidation(JTextField textField, boolean allowEmptyField, boolean allowDirectory, String validationFailedMessage) {
        super(textField, validationFailedMessage);
        myAllowEmptyField = allowEmptyField;
        myAllowDirectory = allowDirectory;
    }

    protected boolean isValid(String text) {
        if (StringUtils.isNotEmpty(text)) {
            File file = new File(text);
            return file.exists() && (myAllowDirectory || !file.isDirectory());
        }
        return myAllowEmptyField;
    }
}