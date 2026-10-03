package rw.ac.auca.scoutpro_27202.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// what the scout sends; assessmentDate is optional (defaults to today)
public record AssessmentRequest(
        UUID athleteId,
        LocalDate assessmentDate,
        String remarks,
        List<ScoreRequest> scores
) {}