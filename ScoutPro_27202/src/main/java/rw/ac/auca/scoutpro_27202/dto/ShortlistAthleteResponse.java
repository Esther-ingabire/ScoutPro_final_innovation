package rw.ac.auca.scoutpro_27202.dto;

import java.util.UUID;

// a short summary of each athlete on a shortlist
public record ShortlistAthleteResponse(
        UUID id,
        String athleteCode,
        String fullName,
        String position,
        String sportName
) {}