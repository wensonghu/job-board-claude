package com.example.jobboard.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "chat_schedule")
public class ChatSchedule {

    /** ISO day-of-week: 1=Monday … 7=Sunday */
    @Id
    @Column(name = "day_of_week")
    private Short dayOfWeek;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    public Short getDayOfWeek()             { return dayOfWeek; }
    public void  setDayOfWeek(Short v)      { this.dayOfWeek = v; }

    public Boolean getEnabled()             { return enabled; }
    public void    setEnabled(Boolean v)    { this.enabled = v; }

    public LocalTime getOpenTime()          { return openTime; }
    public void      setOpenTime(LocalTime v) { this.openTime = v; }

    public LocalTime getCloseTime()         { return closeTime; }
    public void      setCloseTime(LocalTime v) { this.closeTime = v; }
}
