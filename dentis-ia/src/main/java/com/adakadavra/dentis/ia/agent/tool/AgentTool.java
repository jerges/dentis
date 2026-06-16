package com.adakadavra.dentis.ia.agent.tool;

import software.amazon.awssdk.core.document.Document;

import java.util.Map;

public interface AgentTool {
    String name();
    String description();
    String label();
    Document inputSchema();
    String execute(Map<String, Object> input);

    /** Returns true if the tool calls an external API (status = "searching"),
     *  false if it performs local calculation (status = "tooling"). */
    default boolean isExternal() { return true; }
}
