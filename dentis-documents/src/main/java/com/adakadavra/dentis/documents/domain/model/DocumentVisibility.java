package com.adakadavra.dentis.documents.domain.model;

/**
 * PUBLIC  – visible to all clinic staff; KB folders eligible for IA indexing.
 * PRIVATE – visible only to the creator/uploader; excluded from IA indexing.
 */
public enum DocumentVisibility {
    PUBLIC,
    PRIVATE
}
