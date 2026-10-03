package rw.ac.auca.scoutpro_27202.dto;

import java.util.List;

// body for PUT /api/v1/users/{id}/roles, e.g. {"roles": ["SCOUT"]}
public record RoleUpdateRequest(List<String> roles) {}