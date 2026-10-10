package rw.ac.auca.scoutpro_27202.document;

// one video/image reference inside a report, e.g. the moment a goal happens
public record MediaClip(String url, ClipType type, Integer startSec, String note) {}
