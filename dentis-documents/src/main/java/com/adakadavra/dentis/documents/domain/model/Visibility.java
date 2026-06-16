package com.adakadavra.dentis.documents.domain.model;

public enum Visibility {
    PUBLIC,   // cualquier usuario de la clínica puede verlo
    PRIVATE,  // solo el propietario
    SHARED    // propietario + usuarios en document_shares
}