package rw.ac.auca.scoutpro_27202.dto;

import java.util.UUID;

// one criterion's score in a response, with its name and weight for display
public record ScoreResponse(UUID criterionId, String criterionName, Double weight, Double score) {}