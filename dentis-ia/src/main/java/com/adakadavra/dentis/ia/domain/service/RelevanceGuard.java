package com.adakadavra.dentis.ia.domain.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RelevanceGuard {

    private static final List<String> DENTAL_KEYWORDS = List.of(
            "diente", "teeth", "tooth", "molar", "premolar", "incisivo", "canino",
            "caries", "pulpa", "endodoncia", "periodoncia", "implante", "prótesis",
            "extracción", "ortodoncia", "radiografía", "oclusión", "gingivitis",
            "periodontitis", "bracket", "corona", "puente", "alvéolo", "mandíbula",
            "maxilar", "cemento", "esmalte", "dentina", "raíz", "absceso",
            "odontología", "odontológico", "estomatología", "boca", "oral",
            "dental", "clínico", "paciente", "tratamiento", "diagnóstico",
            "dolor", "anestesia", "procedimiento", "cirugía", "obturación",
            "empaste", "blanqueamiento", "fluoruro", "sarro", "placa",
            "limpieza", "profilaxis", "histología", "rx", "periapical"
    );

    public static final String OFF_TOPIC_RESPONSE =
            "Como asistente odontológico clínico, solo puedo ayudarte con temas relacionados con odontología y salud bucal.";

    public boolean isRelevant(String query) {
        String lower = query.toLowerCase();
        return DENTAL_KEYWORDS.stream().anyMatch(lower::contains);
    }
}
