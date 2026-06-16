package com.takecare.backend.careplan.service;

import com.takecare.backend.careplan.dto.CarePlanItemResponseDTO;
import com.takecare.backend.careplan.dto.PatientCarePlanDetailDTO;
import com.takecare.backend.careplan.dto.PatientCarePlanListResponseDTO;
import com.takecare.backend.careplan.dto.PatientCarePlanSummaryDTO;
import com.takecare.backend.careplan.model.CarePlan;
import com.takecare.backend.careplan.model.CarePlanItem;
import com.takecare.backend.careplan.model.CarePlanStatus;
import com.takecare.backend.careplan.repository.CarePlanItemRepository;
import com.takecare.backend.careplan.repository.CarePlanRepository;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class PatientCarePlanQueryService {

    private static final Logger logger = LoggerFactory.getLogger(PatientCarePlanQueryService.class);

    private final CarePlanRepository carePlanRepository;
    private final CarePlanItemRepository carePlanItemRepository;
    private final PatientRepository patientRepository;

    public PatientCarePlanQueryService(
            CarePlanRepository carePlanRepository,
            CarePlanItemRepository carePlanItemRepository,
            PatientRepository patientRepository
    ) {
        this.carePlanRepository = carePlanRepository;
        this.carePlanItemRepository = carePlanItemRepository;
        this.patientRepository = patientRepository;
    }

    public PatientCarePlanDetailDTO getActiveCarePlan(Integer patientId, Integer sessionUserId) {
        validateOwnership(patientId, sessionUserId);
        validatePatientExists(patientId);

        CarePlan activePlan = carePlanRepository
                .findFirstByPatientIdAndStatusOrderByCreatedDateDesc(patientId, CarePlanStatus.ACTIVE)
                .orElseThrow(() -> {
                    logger.warn("GET active care plan - no active plan found. patientId={}", patientId);
                    return new NoSuchElementException("No se encontró un plan de cuidado activo para el paciente");
                });

        logger.info("GET active care plan - found. patientId={}, planId={}", patientId, activePlan.getId());
        return toDetailDTO(activePlan);
    }

    public PatientCarePlanListResponseDTO getCarePlanHistory(Integer patientId, Integer sessionUserId) {
        validateOwnership(patientId, sessionUserId);
        validatePatientExists(patientId);

        List<CarePlan> plans = carePlanRepository.findByPatientIdOrderByCreatedDateDesc(patientId);
        logger.info("GET care plan history - found {} plans. patientId={}", plans.size(), patientId);

        return toListResponseDTO(plans);
    }

    private void validateOwnership(Integer patientId, Integer sessionUserId) {
        if (!patientId.equals(sessionUserId)) {
            logger.warn(
                    "Unauthorized care plan access attempt. pathPatientId={}, sessionUserId={}",
                    patientId, sessionUserId
            );
            throw new SecurityException(
                    "Acceso denegado: no puede consultar los planes de cuidado de otro paciente"
            );
        }
    }

    private void validatePatientExists(Integer patientId) {
        if (!patientRepository.existsById(patientId)) {
            logger.warn("Patient not found. patientId={}", patientId);
            throw new NoSuchElementException("Paciente no encontrado");
        }
    }

    private PatientCarePlanDetailDTO toDetailDTO(CarePlan carePlan) {
        PatientCarePlanDetailDTO dto = new PatientCarePlanDetailDTO();
        dto.setId(carePlan.getId());
        dto.setSpecialistId(carePlan.getSpecialist() != null ? carePlan.getSpecialist().getId() : null);
        dto.setSpecialistName(buildSpecialistName(carePlan.getSpecialist()));
        dto.setTitle(carePlan.getTitle());
        dto.setTherapeuticObjectives(carePlan.getTherapeuticObjectives());
        dto.setGeneralRecommendations(carePlan.getGeneralRecommendations());
        dto.setProfessionalObservations(carePlan.getProfessionalObservations());
        dto.setStatus(carePlan.getStatus() != null ? carePlan.getStatus().name() : null);
        dto.setProgressPercentage(carePlan.getProgressPercentage());
        dto.setCreatedDate(carePlan.getCreatedDate());
        dto.setUpdatedDate(carePlan.getUpdatedDate());

        if (carePlan.getReviewSession() != null && carePlan.getReviewSession().getSchedule() != null) {
            dto.setReviewDate(carePlan.getReviewSession().getSchedule().getScheduleDate());
            dto.setReviewStartTime(carePlan.getReviewSession().getSchedule().getStartTime());
            dto.setReviewEndTime(carePlan.getReviewSession().getSchedule().getEndTime());
        } else {
            dto.setReviewDate(carePlan.getReviewDate());
        }

        List<CarePlanItem> items = carePlanItemRepository.findByCarePlanIdOrderByCreatedDateAsc(carePlan.getId());
        dto.setItems(items.stream().map(this::toItemDTO).toList());

        return dto;
    }

    private PatientCarePlanSummaryDTO toSummaryDTO(CarePlan carePlan) {
        PatientCarePlanSummaryDTO dto = new PatientCarePlanSummaryDTO();
        dto.setId(carePlan.getId());
        dto.setSpecialistId(carePlan.getSpecialist() != null ? carePlan.getSpecialist().getId() : null);
        dto.setSpecialistName(buildSpecialistName(carePlan.getSpecialist()));
        dto.setTitle(carePlan.getTitle());
        dto.setTherapeuticObjectives(carePlan.getTherapeuticObjectives());
        dto.setGeneralRecommendations(carePlan.getGeneralRecommendations());
        dto.setProfessionalObservations(carePlan.getProfessionalObservations());
        dto.setStatus(carePlan.getStatus() != null ? carePlan.getStatus().name() : null);
        dto.setProgressPercentage(carePlan.getProgressPercentage());
        dto.setCreatedDate(carePlan.getCreatedDate());
        dto.setUpdatedDate(carePlan.getUpdatedDate());

        if (carePlan.getReviewSession() != null && carePlan.getReviewSession().getSchedule() != null) {
            dto.setReviewDate(carePlan.getReviewSession().getSchedule().getScheduleDate());
            dto.setReviewStartTime(carePlan.getReviewSession().getSchedule().getStartTime());
            dto.setReviewEndTime(carePlan.getReviewSession().getSchedule().getEndTime());
        } else {
            dto.setReviewDate(carePlan.getReviewDate());
        }

        dto.setItems(carePlanItemRepository.findByCarePlanIdOrderByCreatedDateAsc(carePlan.getId())
                .stream()
                .map(this::toItemDTO)
                .toList());
        return dto;
    }

    private PatientCarePlanListResponseDTO toListResponseDTO(List<CarePlan> plans) {
        PatientCarePlanListResponseDTO response = new PatientCarePlanListResponseDTO();
        response.setTotalCarePlans(plans.size());
        response.setCarePlans(plans.stream().map(this::toSummaryDTO).toList());
        return response;
    }

    private CarePlanItemResponseDTO toItemDTO(CarePlanItem item) {
        CarePlanItemResponseDTO dto = new CarePlanItemResponseDTO();
        dto.setId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setItemType(item.getItemType() != null ? item.getItemType().name() : null);
        dto.setStatus(item.getStatus() != null ? item.getStatus().name() : null);
        dto.setDueDate(item.getDueDate());
        dto.setCompletedDate(item.getCompletedDate());
        dto.setCreatedDate(item.getCreatedDate());
        dto.setUpdatedDate(item.getUpdatedDate());
        return dto;
    }

    private String buildSpecialistName(Specialist specialist) {
        if (specialist == null) {
            return null;
        }

        String names = specialist.getNames() != null ? specialist.getNames().trim() : "";
        String firstLastname = specialist.getFirstLastname() != null ? specialist.getFirstLastname().trim() : "";
        String secondLastname = specialist.getSecondLastname() != null ? specialist.getSecondLastname().trim() : "";
        String fullName = (names + " " + firstLastname + " " + secondLastname).trim().replaceAll("\\s+", " ");
        return fullName.isBlank() ? null : fullName;
    }
}
