package rw.ac.auca.scoutpro_27202.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// what the client receives
public record AssessmentResponse(
        UUID id,
        String assessmentCode,
        UUID athleteId,
        String athleteName,
        UUID scoutId,
        String scoutName,
        LocalDate assessmentDate,
        Double overallScore,
        String remarks,
        List<ScoreResponse> scores
) {}