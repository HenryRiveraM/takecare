package com.takecare.backend.user.security;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.takecare.backend.user.model.User;
import com.takecare.backend.user.repository.PatientRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ClinicalDocAccessInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ClinicalDocAccessInterceptor.class);

    private static final boolean TESTING_MODE = true;

    private static final byte ROLE_PATIENT    = 1;
    private static final byte ROLE_SPECIALIST = 2;

    private final PatientRepository patientRepository;

    public ClinicalDocAccessInterceptor(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (TESTING_MODE) {
            logger.warn("[TESTING_MODE] Interceptor bypassed — cambiar TESTING_MODE=false al integrar JWT");
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User requestingUser)) {
            logger.warn("Unauthenticated access attempt to clinical docs");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> pathVars =
                (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (pathVars == null || !pathVars.containsKey("patient_id")) {
            return true;
        }

        Integer patientId;
        try {
            patientId = Integer.parseInt(pathVars.get("patient_id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid patient_id");
            return false;
        }

        byte role = requestingUser.getRole();

        if (role == ROLE_PATIENT) {
            if (!requestingUser.getId().equals(patientId)) {
                logger.warn("Patient {} tried to access docs of patient {}", requestingUser.getId(), patientId);
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "You can only access your own documents");
                return false;
            }
            return true;
        }

        if (role == ROLE_SPECIALIST) {
            if (!hasConfirmedAppointment(requestingUser.getId(), patientId)) {
                logger.warn("Specialist {} has no confirmed appointment with patient {}",
                        requestingUser.getId(), patientId);
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "No confirmed appointment with this patient");
                return false;
            }
            return true;
        }

        logger.warn("Role {} denied access to clinical docs", role);
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        return false;
    }

    private boolean hasConfirmedAppointment(Integer specialistId, Integer patientId) {

        logger.warn("[STUB] hasConfirmedAppointment called for specialist={} patient={} " +
                    "— returning false until Appointment module is integrated", specialistId, patientId);
        return false;
    }
}