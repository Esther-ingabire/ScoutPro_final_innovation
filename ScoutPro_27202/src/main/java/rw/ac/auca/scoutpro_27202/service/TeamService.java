package rw.ac.auca.scoutpro_27202.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rw.ac.auca.scoutpro_27202.domain.Sport;
import rw.ac.auca.scoutpro_27202.domain.Team;
import rw.ac.auca.scoutpro_27202.repository.AthleteRepository;
import rw.ac.auca.scoutpro_27202.repository.TeamRepository;

import java.util.List;
import java.util.UUID;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepo;

    @Autowired
    private AthleteRepository athleteRepo;

    @Autowired
    private SportService sportService;

    // CREATE
    @PreAuthorize("hasRole('ADMIN')")   // RBAC: only admins manage teams
    public Team saveTeam(UUID sportId, Team team) {
        Sport sport = sportService.getSportById(sportId);
        validate(team);

        String name = team.getName().trim();
        if (teamRepo.findBySportIdAndNameIgnoreCase(sportId, name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Team '" + name + "' already exists for this sport");
        }

        team.setName(name);
        team.setSport(sport);
        return teamRepo.save(team);
    }

    // READ all teams of one sport (any logged-in user)
    public List<Team> getTeamsBySport(UUID sportId) {
        sportService.getSportById(sportId);
        return teamRepo.findBySportId(sportId);
    }

    // READ one (any logged-in user)
    public Team getTeamById(UUID id) {
        return teamRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
    }

    // UPDATE
    @PreAuthorize("hasRole('ADMIN')")   // RBAC
    public Team updateTeam(UUID id, Team newData) {
        Team existing = getTeamById(id);
        validate(newData);

        String name = newData.getName().trim();
        Team sameName = teamRepo
                .findBySportIdAndNameIgnoreCase(existing.getSport().getId(), name)
                .orElse(null);
        if (sameName != null && !sameName.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Team '" + name + "' already exists for this sport");
        }

        existing.setName(name);
        existing.setCity(newData.getCity());
        existing.setLevel(newData.getLevel());
        return teamRepo.save(existing);
    }

    // DELETE
    @PreAuthorize("hasRole('ADMIN')")   // RBAC
    public void deleteTeam(UUID id) {
        if (!teamRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found");
        }
        if (athleteRepo.existsByTeamId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Team still has athletes and cannot be deleted");
        }
        teamRepo.deleteById(id);
    }

    private void validate(Team team) {
        if (team.getName() == null || team.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team name is required");
        }
        if (team.getLevel() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Team level is required (CLUB or ACADEMY)");
        }
    }
}