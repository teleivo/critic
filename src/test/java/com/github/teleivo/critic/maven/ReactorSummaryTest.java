package com.github.teleivo.critic.maven;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ReactorSummaryTest
{
    @Test
    void parseReactorSummaryEntrySuccess()
    {
        assertArrayEquals( new String[] { "DHIS Node service", "4.543 s" },
            ReactorSummary.parseReactorSummaryEntry(
                "2021-12-01T08:30:34.9304126Z [INFO] DHIS Node service .................................. SUCCESS [  4.543 s]" ) );
        assertArrayEquals( new String[] { "DHIS Core API Implementations", "03:00 min" },
            ReactorSummary.parseReactorSummaryEntry(
                "2021-12-01T08:30:34.9308634Z [INFO] DHIS Core API Implementations ...................... SUCCESS [03:00 min]" ) );
        assertArrayEquals( new String[] { "DHIS ACL service", "0.980 s" },
            ReactorSummary.parseReactorSummaryEntry(
                "[INFO] DHIS ACL service ................................... FAILURE [  0.980 s]" ) );
    }

    @Test
    void parseReactorSummaryEntryGivenMissingDuration()
    {
        assertNull( ReactorSummary.parseReactorSummaryEntry(
            "2021-12-01T08:30:34.9304126Z [INFO] DHIS Node service .................................. SUCCESS" ) );
        assertNull( ReactorSummary.parseReactorSummaryEntry(
            "[INFO] DHIS Support Commons ............................... SKIPPED" ) );
    }
}
