package rw.ac.auca.scoutpro_27202.dto;

import java.util.List;
import java.util.UUID;

public record ShortlistResponse(
        UUID id,
        String name,
        String notes,
        String ownerEmail,
        int athleteCount,
        List<ShortlistAthleteResponse> athletes
) {}