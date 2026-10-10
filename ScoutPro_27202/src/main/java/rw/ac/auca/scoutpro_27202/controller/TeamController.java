package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.domain.Team;
import rw.ac.auca.scoutpro_27202.dto.PageResponse;
import rw.ac.auca.scoutpro_27202.service.TeamService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @PostMapping("/sports/{sportId}/teams")
    public ResponseEntity<Team> createTeam(@PathVariable UUID sportId, @RequestBody Team team) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.saveTeam(sportId, team));
    }

    @GetMapping("/sports/{sportId}/teams")
    public PageResponse<Team> getTeamsBySport(@PathVariable UUID sportId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return teamService.getTeamsBySport(sportId, page, size);
    }

    @GetMapping("/teams/{id}")
    public Team getTeamById(@PathVariable UUID id) {
        return teamService.getTeamById(id);
    }

    @PutMapping("/teams/{id}")
    public Team updateTeam(@PathVariable UUID id, @RequestBody Team team) {
        return teamService.updateTeam(id, team);
    }

    @DeleteMapping("/teams/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable UUID id) {
        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }
}