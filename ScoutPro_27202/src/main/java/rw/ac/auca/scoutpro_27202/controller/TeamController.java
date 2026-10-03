package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.domain.Team;
import rw.ac.auca.scoutpro_27202.service.TeamService;

import java.util.List;
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
    public List<Team> getTeamsBySport(@PathVariable UUID sportId) {
        return teamService.getTeamsBySport(sportId);
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