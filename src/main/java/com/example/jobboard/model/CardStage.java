package com.example.jobboard.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CardStage {
    @JsonProperty("early")
    EARLY,
    @JsonProperty("recruiter")
    RECRUITER,
    @JsonProperty("hm")
    HM,
    @JsonProperty("other")
    OTHER,
    @JsonProperty("final")
    FINAL
}
