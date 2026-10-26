package ma.nqiya.auth;

import ma.nqiya.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/**
 * Sends login codes by email. When no SMTP server is configured (local development) the email is only logged.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppProperties props;

    public MailService(ObjectProvider<JavaMailSender> mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    /** @return true when the email was handed to an SMTP server */
    public boolean sendLoginCode(String to, String code) {
        JavaMailSender sender = mailSender.getIfAvailable();
        boolean noHost = sender instanceof JavaMailSenderImpl impl && (impl.getHost() == null || impl.getHost().isBlank());
        if (sender == null || noHost) {
            log.info("No SMTP configured - login code for {} is {}", to, code);
            return false;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(props.mail().from());
        message.setTo(to);
        message.setSubject("Your NQIYA login code: " + code);
        message.setText("""
                Salam!

                Your NQIYA login code is: %s

                It expires in %d minutes. If you didn't ask for it, you can ignore this email.

                Yallah, let's clean it together.
                """.formatted(code, props.auth().codeTtl().toMinutes()));
        try {
            sender.send(message);
            return true;
        } catch (MailException e) {
            log.error("Could not send login code to {}", to, e);
            return false;
        }
    }
}
