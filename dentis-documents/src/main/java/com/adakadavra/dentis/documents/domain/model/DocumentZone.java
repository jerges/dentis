package com.adakadavra.dentis.documents.domain.model;

public enum DocumentZone {
    /** Documents uploaded by clinic admin, indexed by the IA for RAG. */
    KNOWLEDGE_BASE,
    /** General clinic documents (invoices, contracts, etc.) — not indexed by IA. */
    GENERAL
}
