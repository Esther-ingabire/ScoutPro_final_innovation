package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.domain.PhysicalProfile;
import rw.ac.auca.scoutpro_27202.service.PhysicalProfileService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/athletes/{athleteId}/physical-profile")
public class PhysicalProfileController {

    @Autowired
    private PhysicalProfileService profileService;

    @PutMapping
    public PhysicalProfile saveOrUpdateProfile(@PathVariable UUID athleteId,
                                               @RequestBody PhysicalProfile profile) {
        return profileService.saveOrUpdateProfile(athleteId, profile);
    }

    @GetMapping
    public PhysicalProfile getProfile(@PathVariable UUID athleteId) {
        return profileService.getProfileByAthlete(athleteId);
    }
}