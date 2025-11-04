package ai.content.auto.service;

import ai.content.auto.entity.AuditLog;
import ai.content.auto.entity.User;
import ai.content.auto.repository.AuditLogRepository;
import ai.content.auto.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditService.
 * Tests audit logging functionality for content version operations.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private AuditService auditService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    void logContentVersionRevert_Success() {
        // Given
        Long contentId = 123L;
        Integer fromVersion = 5;
        Integer toVersion = 3;
        Integer newVersion = 6;

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // When
        auditService.logContentVersionRevert(contentId, fromVersion, toVersion, newVersion, testUser);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog capturedLog = auditLogCaptor.getValue();
        assertNotNull(capturedLog);
        assertEquals("CONTENT_VERSION_REVERT", capturedLog.getEventType());
        assertEquals("CONTENT_MANAGEMENT", capturedLog.getEventCategory());
        assertEquals(testUser.getId(), capturedLog.getUserId());
        assertEquals(testUser.getUsername(), capturedLog.getUsername());
        assertEquals("CONTENT", capturedLog.getResourceType());
        assertEquals(contentId.toString(), capturedLog.getResourceId());
        assertEquals("REVERT_VERSION", capturedLog.getActionPerformed());
        assertEquals("SUCCESS", capturedLog.getActionResult());
        assertEquals("MEDIUM", capturedLog.getRiskLevel());
        assertEquals("MEDIUM", capturedLog.getImpactLevel());
        assertTrue(capturedLog.getGdprRelevant());
        assertFalse(capturedLog.getPiiInvolved());

        // Verify event description contains relevant information
        String description = capturedLog.getEventDescription();
        assertTrue(description.contains(contentId.toString()));
        assertTrue(description.contains(fromVersion.toString()));
        assertTrue(description.contains(toVersion.toString()));
        assertTrue(description.contains(newVersion.toString()));

        // Verify old and new values
        assertNotNull(capturedLog.getOldValues());
        assertNotNull(capturedLog.getNewValues());
        assertTrue(capturedLog.getOldValues().containsKey("currentVersion"));
        assertTrue(capturedLog.getNewValues().containsKey("revertedToVersion"));
        assertTrue(capturedLog.getNewValues().containsKey("newVersionCreated"));
    }

    @Test
    void logContentAction_Success() {
        // Given
        String actionType = "CREATE_VERSION";
        Long contentId = 456L;
        String description = "Created new content version";

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // When
        auditService.logContentAction(actionType, contentId, description, null, null, testUser);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog capturedLog = auditLogCaptor.getValue();
        assertNotNull(capturedLog);
        assertEquals("CONTENT_ACTION", capturedLog.getEventType());
        assertEquals("CONTENT_MANAGEMENT", capturedLog.getEventCategory());
        assertEquals(actionType, capturedLog.getActionPerformed());
        assertEquals(description, capturedLog.getEventDescription());
        assertEquals("LOW", capturedLog.getRiskLevel());
        assertEquals("LOW", capturedLog.getImpactLevel());
    }

    @Test
    void logFailedAction_Success() {
        // Given
        String actionType = "REVERT_VERSION";
        String resourceType = "CONTENT";
        String resourceId = "789";
        String errorMessage = "Version not found";
        Exception exception = new RuntimeException("Test exception");

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // When
        auditService.logFailedAction(actionType, resourceType, resourceId, errorMessage, exception, testUser);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog capturedLog = auditLogCaptor.getValue();
        assertNotNull(capturedLog);
        assertEquals("ACTION_FAILED", capturedLog.getEventType());
        assertEquals("ERROR", capturedLog.getEventCategory());
        assertEquals(actionType, capturedLog.getActionPerformed());
        assertEquals("FAILURE", capturedLog.getActionResult());
        assertEquals(errorMessage, capturedLog.getErrorMessage());
        assertEquals("RuntimeException", capturedLog.getExceptionType());
        assertEquals("HIGH", capturedLog.getRiskLevel());
        assertTrue(capturedLog.getAlertTriggered());
        assertNotNull(capturedLog.getStackTrace());
    }

    @Test
    void logSecurityEvent_Success() {
        // Given
        String eventType = "UNAUTHORIZED_ACCESS";
        String description = "Attempted access to restricted content";
        String riskLevel = "HIGH";

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // When
        auditService.logSecurityEvent(eventType, description, riskLevel, testUser);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog capturedLog = auditLogCaptor.getValue();
        assertNotNull(capturedLog);
        assertEquals(eventType, capturedLog.getEventType());
        assertEquals("SECURITY", capturedLog.getEventCategory());
        assertEquals("SECURITY_EVENT", capturedLog.getActionPerformed());
        assertEquals("DETECTED", capturedLog.getActionResult());
        assertEquals(riskLevel, capturedLog.getRiskLevel());
        assertEquals(riskLevel, capturedLog.getImpactLevel());
        assertTrue(capturedLog.getAlertTriggered());
        assertEquals("CONFIDENTIAL", capturedLog.getDataSensitivityLevel());
        assertEquals("CONFIDENTIAL", capturedLog.getDataClassification());
    }

    @Test
    void logContentVersionRevert_HandlesExceptionGracefully() {
        // Given
        Long contentId = 123L;
        Integer fromVersion = 5;
        Integer toVersion = 3;
        Integer newVersion = 6;

        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then - Should not throw exception
        assertDoesNotThrow(
                () -> auditService.logContentVersionRevert(contentId, fromVersion, toVersion, newVersion, testUser));

        verify(auditLogRepository).save(any(AuditLog.class));
    }
}