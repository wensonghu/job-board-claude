package com.example.jobboard.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CardStage {
    @JsonProperty("seeding")
    SEEDING,
    @JsonProperty("recruiter")
    RECRUITER,
    @JsonProperty("hm")
    HM,
    @JsonProperty("next-rounds")
    NEXT_ROUNDS,
    @JsonProperty("final")
    FINAL
}