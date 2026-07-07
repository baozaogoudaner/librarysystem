package com.library.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.util.Map;

/**
 * 邮件发送服务 - 使用 Thymeleaf 模板渲染 HTML 邮件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 发送 Thymeleaf 模板邮件
     */
    public void sendTemplateMail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            if (from.isEmpty()) {
                log.info("[MAIL MOCK] To: {}, Subject: {}, Template: {}", to, subject, templateName);
                return;
            }
            Context context = new Context();
            variables.forEach(context::setVariable);

            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage());
        }
    }
}
