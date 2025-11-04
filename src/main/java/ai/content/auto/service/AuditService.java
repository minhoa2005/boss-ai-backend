package ai.content.auto.service;

import ai.content.auto.entity.AuditLog;
import ai.content.auto.entity.User;
import ai.content.auto.repository.AuditLogRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for comprehensive audit logging.
 * Handles creation and management of audit log entries for compliance and
 * debugging.
 * 
 * Requirements: 7.1, 7.2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityUtil securityUtil;

    /**
     * Log a content version revert action.
     * 
     * @param contentId         Content ID that was reverted
     * @param fromVersionNumber Version number reverted from
     * @param toVersionNumber   Version number reverted to
     * @param newVersionNumber  New version number created
     * @param user              User who performed the revert
     */
    @Transactional
    public void logContentVersionRevert(Long contentId, Integer fromVersionNumber,
            Integer toVersionNumber, Integer newVersionNumber, User user) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .eventId(generateEventId())
                    .eventType("CONTENT_VERSION_REVERT")
                    .eventCategory("CONTENT_MANAGEMENT")
                    .eventDescription(
                            String.format("Content %d reverted from version %d to version %d, creating new version %d",
                                    contentId, fromVersionNumber, toVersionNumber, newVersionNumber))
                    .userId(user.getId())
                    .username(user.getUsername())
                    .userRole(getUserRole(user))
                    .resourceType("CONTENT")
                    .resourceId(contentId.toString())
                    .resourceName("Content Generation")
                    .actionPerformed("REVERT_VERSION")
                    .actionResult("SUCCESS")
                    .oldValues(Map.of(
                            "currentVersion", fromVersionNumber,
                            "contentId", contentId))
                    .newValues(Map.of(
                            "revertedToVersion", toVersionNumber,
                            "newVersionCreated", newVersionNumber,
                            "contentId", contentId))
                    .changedFields(List.of("version", "content"))
                    .dataSensitivityLevel("INTERNAL")
                    .riskLevel("MEDIUM")
                    .impactLevel("MEDIUM")
                    .businessImpact("Content version reverted - may affect published content")
                    .gdprRelevant(true)
                    .piiInvolved(false)
                    .regulatoryRequirements(List.of("AUDIT_TRAIL", "DATA_INTEGRITY"))
                    .dataClassification("INTERNAL")
                    .build();

            // Add request context if available
            enrichWithRequestContext(auditLog);

            auditLogRepository.save(auditLog);

            log.info("Audit log created for content version revert: contentId={}, user={}, eventId={}",
                    contentId, user.getUsername(), auditLog.getEventId());

        } catch (Exception e) {
            log.error("Failed to create audit log for content version revert: contentId={}, user={}",
                    contentId, user.getUsername(), e);
            // Don't throw - audit logging failure shouldn't break the main operation
        }
    }

    /**
     * Log a general content management action.
     * 
     * @param actionType  Type of action performed
     * @param contentId   Content ID affected
     * @param description Description of the action
     * @param oldValues   Previous values (optional)
     * @param newValues   New values (optional)
     * @param user        User who performed the action
     */
    @Transactional
    public void logContentAction(String actionType, Long contentId, String description,
            Map<String, Object> oldValues, Map<String, Object> newValues, User user) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .eventId(generateEventId())
                    .eventType("CONTENT_ACTION")
                    .eventCategory("CONTENT_MANAGEMENT")
                    .eventDescription(description)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .userRole(getUserRole(user))
                    .resourceType("CONTENT")
                    .resourceId(contentId.toString())
                    .resourceName("Content Generation")
                    .actionPerformed(actionType)
                    .actionResult("SUCCESS")
                    .oldValues(oldValues != null ? oldValues : Map.of())
                    .newValues(newValues != null ? newValues : Map.of())
                    .dataSensitivityLevel("INTERNAL")
                    .riskLevel("LOW")
                    .impactLevel("LOW")
                    .gdprRelevant(true)
                    .piiInvolved(false)
                    .dataClassification("INTERNAL")
                    .build();

            // Add request context if available
            enrichWithRequestContext(auditLog);

            auditLogRepository.save(auditLog);

            log.debug("Audit log created for content action: contentId={}, action={}, user={}, eventId={}",
                    contentId, actionType, user.getUsername(), auditLog.getEventId());

        } catch (Exception e) {
            log.error("Failed to create audit log for content action: contentId={}, action={}, user={}",
                    contentId, actionType, user.getUsername(), e);
            // Don't throw - audit logging failure shouldn't break the main operation
        }
    }

    /**
     * Log a failed action with error details.
     * 
     * @param actionType   Type of action that failed
     * @param resourceType Type of resource affected
     * @param resourceId   ID of the resource
     * @param errorMessage Error message
     * @param exception    Exception that occurred (optional)
     * @param user         User who attempted the action
     */
    @Transactional
    public void logFailedAction(String actionType, String resourceType, String resourceId,
            String errorMessage, Exception exception, User user) {
        try {
            AuditLog.AuditLogBuilder auditLogBuilder = AuditLog.builder()
                    .eventId(generateEventId())
                    .eventType("ACTION_FAILED")
                    .eventCategory("ERROR")
                    .eventDescription(String.format("Failed to perform %s on %s %s: %s",
                            actionType, resourceType, resourceId, errorMessage))
                    .userId(user != null ? user.getId() : null)
                    .username(user != null ? user.getUsername() : "SYSTEM")
                    .userRole(user != null ? getUserRole(user) : "SYSTEM")
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .actionPerformed(actionType)
                    .actionResult("FAILURE")
                    .errorMessage(errorMessage)
                    .dataSensitivityLevel("INTERNAL")
                    .riskLevel("HIGH")
                    .impactLevel("MEDIUM")
                    .alertTriggered(true)
                    .gdprRelevant(false)
                    .piiInvolved(false)
                    .dataClassification("INTERNAL");

            if (exception != null) {
                auditLogBuilder
                        .exceptionType(exception.getClass().getSimpleName())
                        .stackTrace(getStackTrace(exception));
            }

            AuditLog auditLog = auditLogBuilder.build();

            // Add request context if available
            enrichWithRequestContext(auditLog);

            auditLogRepository.save(auditLog);

            log.warn("Audit log created for failed action: action={}, resource={}:{}, user={}, eventId={}",
                    actionType, resourceType, resourceId,
                    user != null ? user.getUsername() : "SYSTEM", auditLog.getEventId());

        } catch (Exception e) {
            log.error("Failed to create audit log for failed action: action={}, resource={}:{}, user={}",
                    actionType, resourceType, resourceId,
                    user != null ? user.getUsername() : "SYSTEM", e);
            // Don't throw - audit logging failure shouldn't break error handling
        }
    }

    /**
     * Log a security-related event.
     * 
     * @param eventType   Type of security event
     * @param description Description of the event
     * @param riskLevel   Risk level (LOW, MEDIUM, HIGH, CRITICAL)
     * @param user        User involved in the event (optional)
     */
    @Transactional
    public void logSecurityEvent(String eventType, String description, String riskLevel, User user) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .eventId(generateEventId())
                    .eventType(eventType)
                    .eventCategory("SECURITY")
                    .eventDescription(description)
                    .userId(user != null ? user.getId() : null)
                    .username(user != null ? user.getUsername() : "SYSTEM")
                    .userRole(user != null ? getUserRole(user) : "SYSTEM")
                    .actionPerformed("SECURITY_EVENT")
                    .actionResult("DETECTED")
                    .dataSensitivityLevel("CONFIDENTIAL")
                    .riskLevel(riskLevel)
                    .impactLevel(riskLevel)
                    .securityImplications(description)
                    .alertTriggered(true)
                    .escalationLevel(getRiskEscalationLevel(riskLevel))
                    .gdprRelevant(false)
                    .piiInvolved(false)
                    .regulatoryRequirements(List.of("SECURITY_MONITORING", "INCIDENT_RESPONSE"))
                    .dataClassification("CONFIDENTIAL")
                    .build();

            // Add request context if available
            enrichWithRequestContext(auditLog);

            auditLogRepository.save(auditLog);

            log.warn("Security audit log created: eventType={}, riskLevel={}, user={}, eventId={}",
                    eventType, riskLevel, user != null ? user.getUsername() : "SYSTEM", auditLog.getEventId());

        } catch (Exception e) {
            log.error("Failed to create security audit log: eventType={}, user={}",
                    eventType, user != null ? user.getUsername() : "SYSTEM", e);
            // Don't throw - audit logging failure shouldn't break security handling
        }
    }

    // Private helper methods

    private String generateEventId() {
        return "AUDIT_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private String getUserRole(User user) {
        // For simplicity, return a default role
        // In a full implementation, this would query the UserRoleRepository
        return "USER";
    }

    private void enrichWithRequestContext(AuditLog auditLog) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                auditLog.setRequestMethod(request.getMethod());
                auditLog.setRequestUrl(request.getRequestURL().toString());
                auditLog.setUserAgent(request.getHeader("User-Agent"));

                // Set IP address
                String ipAddress = getClientIpAddress(request);
                if (ipAddress != null) {
                    try {
                        auditLog.setIpAddress(InetAddress.getByName(ipAddress));
                    } catch (UnknownHostException e) {
                        log.debug("Invalid IP address format: {}", ipAddress);
                    }
                }

                // Set session ID if available
                if (request.getSession(false) != null) {
                    auditLog.setSessionId(request.getSession().getId());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to enrich audit log with request context", e);
            // Continue without request context
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle comma-separated IPs (X-Forwarded-For can contain multiple IPs)
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    private String getStackTrace(Exception exception) {
        if (exception == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(exception.getClass().getName()).append(": ").append(exception.getMessage()).append("\n");

        StackTraceElement[] stackTrace = exception.getStackTrace();
        for (int i = 0; i < Math.min(stackTrace.length, 10); i++) { // Limit to first 10 stack trace elements
            sb.append("\tat ").append(stackTrace[i].toString()).append("\n");
        }

        if (stackTrace.length > 10) {
            sb.append("\t... ").append(stackTrace.length - 10).append(" more\n");
        }

        return sb.toString();
    }

    private Integer getRiskEscalationLevel(String riskLevel) {
        return switch (riskLevel.toUpperCase()) {
            case "CRITICAL" -> 3;
            case "HIGH" -> 2;
            case "MEDIUM" -> 1;
            case "LOW" -> 0;
            default -> 0;
        };
    }
}