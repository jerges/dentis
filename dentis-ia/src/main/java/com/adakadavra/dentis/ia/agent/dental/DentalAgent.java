package com.adakadavra.dentis.ia.agent.dental;

import com.adakadavra.dentis.ia.agent.AgentPort;
import com.adakadavra.dentis.ia.agent.AgentType;
import com.adakadavra.dentis.ia.agent.BaseAgent;
import com.adakadavra.dentis.ia.agent.tool.AgentTool;
import com.adakadavra.dentis.ia.domain.service.RelevanceGuard;
import com.adakadavra.dentis.ia.infrastructure.config.IaProperties;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

import java.util.List;
import java.util.function.Consumer;

@Component
public class DentalAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT = """
            Eres un asistente clínico odontológico experto. Tu única función es ayudar a los médicos dentistas en sus tratamientos, diagnósticos y consultas clínicas relacionadas con odontología y estomatología.

            REGLAS ESTRICTAS:
            1. Solo responde preguntas relacionadas con odontología, estomatología, patologías bucales, tratamientos dentales, diagnósticos clínicos, protocolos de atención o documentos clínicos que se te proporcionen.
            2. Si el usuario hace una pregunta no relacionada con odontología o estomatología, responde únicamente con: "Como asistente odontológico clínico, solo puedo ayudarte con temas relacionados con odontología y salud bucal."
            3. No emitas diagnósticos definitivos. Usa siempre lenguaje de apoyo clínico ("puede indicar", "sugiere evaluar", "considera descartar"). La decisión final corresponde al médico tratante.
            4. Cuando respondas basado en documentos clínicos proporcionados, cita la fuente.
            5. Cuando uses herramientas para buscar información, integra los resultados de forma clara y cita las fuentes.
            6. Responde siempre en español.
            7. Mantén un tono profesional y clínico.
            """;

    private final RelevanceGuard relevanceGuard;

    public DentalAgent(BedrockRuntimeAsyncClient asyncClient,
                       VectorStore vectorStore,
                       IaProperties props,
                       RelevanceGuard relevanceGuard,
                       List<AgentTool> tools) {
        super(asyncClient, vectorStore, props, tools);
        this.relevanceGuard = relevanceGuard;
    }

    @Override
    public AgentResponse streamAsk(AgentRequest req, Consumer<AgentEvent> onEvent) {
        if (!relevanceGuard.isRelevant(req.userText())) {
            onEvent.accept(new AgentEvent.Token(RelevanceGuard.OFF_TOPIC_RESPONSE));
            return new AgentResponse(RelevanceGuard.OFF_TOPIC_RESPONSE, 0, 0, null);
        }
        return super.streamAsk(req, onEvent);
    }

    @Override
    protected String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    public AgentType agentType() {
        return AgentType.DENTAL;
    }
}
