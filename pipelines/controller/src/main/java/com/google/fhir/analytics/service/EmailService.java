package com.google.fhir.analytics.service;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;


@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class.getName());

    @Autowired
    private JavaMailSender emailSender;

    private final AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();

    public boolean sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("keerthiga.thurvassurendran@gmail.com"); // Set your "from" address

            emailSender.send(message);
            logger.info("Received request to start the pipeline - email service send-email try..." + message);
            return true;
        } catch (MailException e) {
//            return "Email not sending";
            logger.info("Received request to start the pipeline - email service send-email catch...");
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    public boolean sendMailWithAttachment(Map<String, String> EmailInfo, String filePath, String source) {
        MimeMessage mimeMessage
                = emailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper;

        try {
            mimeMessageHelper
                    = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setFrom(EmailInfo.get("FromUser"));
            mimeMessageHelper.setTo(EmailInfo.get("ToUser"));
            mimeMessageHelper.setText(EmailInfo.get("Body"));
            mimeMessageHelper.setSubject(
                    EmailInfo.get("Subject"));

            // Adding the attachment
            FileSystemResource file
                    = new FileSystemResource(new File(filePath));

            if (file.exists()) {
                try {
                    mimeMessageHelper.addAttachment(Objects.requireNonNull(file.getFilename()), file);
                } catch (MessagingException e) {
                    throw new RuntimeException(e);
                }
            } else {
                logger.info("File not exists" + file.getPath() + file);
            }

            // Sending the mail
            emailSender.send(mimeMessage);
            return true;

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean sendEmailWithAttachmentS3Api(Map<String, String> EmailInfo, InputStream stream) {
        try {
            // Convert InputStream to ByteArrayResource
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            ByteArrayResource byteArrayResource = new ByteArrayResource(byteArrayOutputStream.toByteArray());

            // Create email
            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(EmailInfo.get("FromUser"));
            helper.setTo(EmailInfo.get("ToUser"));
            helper.setSubject(EmailInfo.get("Subject"));
            helper.setText(EmailInfo.get("Body"));
            helper.addAttachment("attachment.txt", byteArrayResource); // Specify the attachment name

            emailSender.send(mimeMessage);
            System.out.println("Email sent successfully with attachment.");
            return true;
        } catch (MessagingException e) {
            logger.info("sendEmailWithAttachmentS3Api line no: 145 catch method");
            return false;
        } catch (IOException e) {
            logger.info("sendEmailWithAttachmentS3Api line no: 148 catch method");
            throw new RuntimeException(e);
        }
    }
}