package com.example.jobboard.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.example.jobboard.dto.ExtractedCardDraft;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

@Service
public class AiCardExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(AiCardExtractionService.class);

    private static final int MAX_INPUT_CHARS = 8000;
    private static final Set<String> VALID_STAGES = Set.of("early", "recruiter", "hm", "other", "final");
    private static final Set<String> VALID_STATUSES = Set.of(
            "in-progress", "interview-schedule-pending", "interview-date-confirmed", "interview-completed",
            "offer-pending", "offer-received", "offer-accepted", "declined", "rejected");
    private static final Set<String> VALID_TIMEZONES = Set.of("PT", "MT", "CT", "ET", "GMT", "UTC");

    @Value("${anthropic.api.key:}")
    private String apiKey;

    private AnthropicClient client;

    @PostConstruct
    private void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        }
    }

    public ExtractedCardDraft extract(String pastedText) {
        if (client == null) {
            throw new IllegalStateException("AI extraction not configured");
        }

        String text = pastedText.length() > MAX_INPUT_CHARS
                ? pastedText.substring(0, MAX_INPUT_CHARS)
                : pastedText;

        StructuredMessageCreateParams<ExtractedCardDraft> params = MessageCreateParams.builder()
                .model("claude-opus-5")
                .maxTokens(2048L)
                .system(buildSystemPrompt())
                .outputConfig(ExtractedCardDraft.class)
                .addUserMessage(text)
                .build();

        ExtractedCardDraft draft = client.messages().create(params).content().stream()
                .flatMap(cb -> cb.text().stream())
                .findFirst()
                .map(typed -> (ExtractedCardDraft) typed.text())
                .orElseThrow(() -> new IllegalStateException("Empty extraction response"));

        return sanitize(draft);
    }

    private String buildSystemPrompt() {
        return "You extract structured job-application details from pasted text (a job posting, "
                + "recruiter email, or LinkedIn message) so a user can review and save it as a card. "
                + "Today's date is " + LocalDate.now() + " — resolve relative dates (\"next Tuesday\", "
                + "\"in two weeks\") against it. "
                + "If a field isn't clearly present in the text, leave it null — never guess or invent "
                + "a value. Only these exact slugs are valid for 'stage': early, recruiter, hm, other, final. "
                + "Only these exact slugs are valid for 'status': in-progress, interview-schedule-pending, "
                + "interview-date-confirmed, interview-completed, offer-pending, offer-received, "
                + "offer-accepted, declined, rejected. Only these exact abbreviations are valid for "
                + "'interviewTimezone': PT, MT, CT, ET, GMT, UTC.";
    }

    /** Defense in depth: null out anything that didn't land on the closed vocabularies above. */
    private ExtractedCardDraft sanitize(ExtractedCardDraft draft) {
        return new ExtractedCardDraft(
                draft.company(),
                draft.position(),
                whitelist(draft.stage(), VALID_STAGES),
                whitelist(draft.status(), VALID_STATUSES),
                draft.interviewDate(),
                draft.interviewTime(),
                whitelist(draft.interviewTimezone(), VALID_TIMEZONES),
                draft.appliedDate(),
                draft.referredBy(),
                draft.details()
        );
    }

    private String whitelist(String value, Set<String> allowed) {
        return (value != null && allowed.contains(value)) ? value : null;
    }
}
