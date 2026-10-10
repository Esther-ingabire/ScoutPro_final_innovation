package rw.ac.auca.scoutpro_27202.dto;

import java.util.UUID;

// userId null unlinks the login account from the athlete.
public record LinkAccountRequest(UUID userId) {}
