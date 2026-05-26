package com.takecare.backend.report.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takecare.backend.report.dto.AdminReportItemDTO;
import com.takecare.backend.report.dto.UpdateAdminReportStatusRequestDTO;
import com.takecare.backend.report.model.Report;
import com.takecare.backend.report.repository.ReportRepository;
import com.takecare.backend.session.repository.SessionRepository;
import com.takecare.backend.user.model.User;
import com.takecare.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    private ReportService reportService;
    private Report report;
    private User reportedUser;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, sessionRepository, userRepository);

        reportedUser = new User();
        reportedUser.setId(8);
        reportedUser.setNames("Usuario");
        reportedUser.setRole((byte) 1);
        reportedUser.setStrikes((byte) 2);
        reportedUser.setStatus((byte) 1);

        report = new Report();
        report.setId(25);
        report.setStatus("PENDING");
        report.setReported(reportedUser);

        when(reportRepository.findByIdForStatusUpdate(25)).thenReturn(Optional.of(report));
    }

    @Test
    void acceptingReportAddsStrikeAndSuspendsOnThirdStrike() {
        when(reportRepository.save(report)).thenReturn(report);

        AdminReportItemDTO result = reportService.updateAdminReportStatus(25, request("ACCEPTED"));

        assertThat(result.getStatus()).isEqualTo("ACCEPTED");
        assertThat(reportedUser.getStrikes()).isEqualTo((byte) 3);
        assertThat(reportedUser.getStatus()).isEqualTo((byte) 0);
        verify(userRepository).save(reportedUser);
    }

    @Test
    void acceptingReportAfterReactivationKeepsAccountActiveUntilNextThreeStrikes() {
        reportedUser.setStrikes((byte) 3);
        reportedUser.setStatus((byte) 1);
        when(reportRepository.save(report)).thenReturn(report);

        reportService.updateAdminReportStatus(25, request("ACCEPTED"));

        assertThat(reportedUser.getStrikes()).isEqualTo((byte) 4);
        assertThat(reportedUser.getStatus()).isEqualTo((byte) 1);
        verify(userRepository).save(reportedUser);
    }

    @Test
    void acceptingReportSuspendsReactivatedAccountOnSixthStrike() {
        reportedUser.setStrikes((byte) 5);
        reportedUser.setStatus((byte) 1);
        when(reportRepository.save(report)).thenReturn(report);

        reportService.updateAdminReportStatus(25, request("ACCEPTED"));

        assertThat(reportedUser.getStrikes()).isEqualTo((byte) 6);
        assertThat(reportedUser.getStatus()).isEqualTo((byte) 0);
        verify(userRepository).save(reportedUser);
    }

    @Test
    void finishingReportDoesNotApplyStrike() {
        when(reportRepository.save(report)).thenReturn(report);

        AdminReportItemDTO result = reportService.updateAdminReportStatus(25, request("FINISHED"));

        assertThat(result.getStatus()).isEqualTo("FINISHED");
        assertThat(reportedUser.getStrikes()).isEqualTo((byte) 2);
        assertThat(reportedUser.getStatus()).isEqualTo((byte) 1);
        verify(userRepository, never()).save(reportedUser);
    }

    @Test
    void managedReportCannotBePenalizedAgain() {
        report.setStatus("ACCEPTED");

        assertThatThrownBy(() -> reportService.updateAdminReportStatus(25, request("ACCEPTED")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("El reporte ya fue gestionado");

        verify(userRepository, never()).save(reportedUser);
    }

    private UpdateAdminReportStatusRequestDTO request(String status) {
        UpdateAdminReportStatusRequestDTO request = new UpdateAdminReportStatusRequestDTO();
        request.setStatus(status);
        return request;
    }
}
