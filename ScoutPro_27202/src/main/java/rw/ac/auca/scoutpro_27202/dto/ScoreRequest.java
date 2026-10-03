package rw.ac.auca.scoutpro_27202.dto;

import java.util.UUID;

// one criterion's score in a request, e.g. {"criterionId": "...", "score": 80}
public record ScoreRequest(UUID criterionId, Double score) {}