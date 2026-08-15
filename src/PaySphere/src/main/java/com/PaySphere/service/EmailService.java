package com.PaySphere.service;

public interface EmailService {

    /**
     * Emails a payslip attachment. Delivery failures are logged, not thrown — a salary's
     * payment status is the source of truth and must not roll back just because the
     * notification email couldn't be sent (e.g. SMTP not configured in this environment).
     */
    void sendPayslip(String toEmail, String employeeName, byte[] payslipExcel, String fileName);
}
