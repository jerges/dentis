package com.adakadavra.dentis.ia.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RelevanceGuardTest {

    private final RelevanceGuard guard = new RelevanceGuard();

    @Test
    void dentalQueryIsRelevant() {
        assertThat(guard.isRelevant("¿Cuáles son los síntomas de una caries?")).isTrue();
    }

    @Test
    void endodonciaIsRelevant() {
        assertThat(guard.isRelevant("Explícame el procedimiento de endodoncia")).isTrue();
    }

    @Test
    void offTopicQueryIsNotRelevant() {
        assertThat(guard.isRelevant("¿Cuál es la capital de Francia?")).isFalse();
    }

    @Test
    void emptyQueryIsNotRelevant() {
        assertThat(guard.isRelevant("")).isFalse();
    }

    @Test
    void caseInsensitiveMatch() {
        assertThat(guard.isRelevant("DIENTE fracturado")).isTrue();
    }

    @Test
    void offTopicResponseIsNonEmpty() {
        assertThat(RelevanceGuard.OFF_TOPIC_RESPONSE).isNotBlank();
    }
}
