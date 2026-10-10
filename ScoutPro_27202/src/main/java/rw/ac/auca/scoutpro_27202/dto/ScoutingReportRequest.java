package rw.ac.auca.scoutpro_27202.dto;

import rw.ac.auca.scoutpro_27202.document.MediaClip;

import java.util.List;

public record ScoutingReportRequest(
        String summary,
        List<String> strengths,
        List<String> weaknesses,
        List<String> tags,
        List<MediaClip> media
) {}