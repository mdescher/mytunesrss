package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.User;
import de.codewave.mytunesrss.jsp.BundleError;
import de.codewave.mytunesrss.jsp.LocalizedError;
import de.codewave.mytunesrss.jsp.MyTunesRssResource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;

public class DoSelfRegistrationCommandHandler extends MyTunesRssCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DoSelfRegistrationCommandHandler.class);

    @Override
    public void execute() throws Exception {
        String username = StringUtils.trimToEmpty(getRequestParameter("username", null));
        String password = StringUtils.trimToEmpty(getRequestParameter("password", null));
        String retypePassword = StringUtils.trimToEmpty(getRequestParameter("retypepassword", null));
        if (StringUtils.isNotBlank(username)) {
            addError(new BundleError("error.registration.emptyUsername"));
        }
        if (StringUtils.isBlank(password)) {
            addError(new BundleError("error.registration.emptyPassword"));
        } else if (!StringUtils.equals(password, retypePassword)) {
            addError(new BundleError("error.registration.retypeFailure"));
        }
        if (!isError()) {
            User user = (User) MyTunesRss.CONFIG.getUser(MyTunesRss.CONFIG.getSelfRegisterTemplateUser()).clone();
            user.setName(username);
            user.setPasswordHash(MyTunesRss.SHA1_DIGEST.digest(password.getBytes("UTF-8")));
            if (!MyTunesRss.CONFIG.addUser(user)) {
                addError(new BundleError("error.registration.duplicateUsername"));
            } else if (MyTunesRss.CONFIG.isSelfRegAdminEmail() && MyTunesRss.CONFIG.isValidMailConfig() && StringUtils.isNotBlank(MyTunesRss.CONFIG.getAdminEmail())) {
                sendAdminMail("New user account registration", "Someone has create a new account with the name \"" + username + "\" on your MyTunesRSS server.\nPlease review the registration and activate the account if necessary.");
                addMessage(new BundleError("info.registration." + (user.isActive() ? "done" : "needsActivation")));
                forward(MyTunesRssResource.Login);
            }
        }
        forward(MyTunesRssResource.SelfRegistration);
    }

    private void sendAdminMail(String subject, String body) {
        try {
            MyTunesRss.MAILER.sendMail(MyTunesRss.CONFIG.getAdminEmail(), "MyTunesRSS: " + subject, body);
        } catch (MailException e) {
            LOGGER.error("Could not send admin email for user registration.", e);
        }
    }
}
