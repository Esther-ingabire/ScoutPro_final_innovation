package rw.ac.auca.scoutpro_27202.dto;

// Register does not sign the user in. They must enter the emailed code first.
public record RegisterResponse(String email, String message) {}
