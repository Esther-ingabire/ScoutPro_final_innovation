package rw.ac.auca.scoutpro_27202.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "scouting_reports")
public class ScoutingReport {

    @Id
    private String id;                 // MongoDB generates an ObjectId

    // references to PostgreSQL rows, stored as text
    @Indexed(unique = true)
    private String assessmentId;       // one report per assessment

    @Indexed
    private String athleteId;          // fast "all reports for this athlete"

    private String scoutId;

    @TextIndexed
    private String summary;

    private List<String> strengths = new ArrayList<>();

    private List<String> weaknesses = new ArrayList<>();

    @TextIndexed
    private List<String> tags = new ArrayList<>();

    private List<MediaClip> media = new ArrayList<>();   // nested, no join table needed

    private Instant createdAt;

    private Instant updatedAt;

    public ScoutingReport() {}



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(String assessmentId) {
        this.assessmentId = assessmentId;
    }

    public String getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(String athleteId) {
        this.athleteId = athleteId;
    }

    public String getScoutId() {
        return scoutId;
    }

    public void setScoutId(String scoutId) {
        this.scoutId = scoutId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<MediaClip> getMedia() {
        return media;
    }

    public void setMedia(List<MediaClip> media) {
        this.media = media;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}