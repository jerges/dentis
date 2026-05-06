package com.adakadavra.dentis.patient.infrastructure.persistence.mapper;

import com.adakadavra.dentis.patient.domain.model.Address;
import com.adakadavra.dentis.patient.domain.model.ContactInfo;
import com.adakadavra.dentis.patient.domain.model.Patient;
import com.adakadavra.dentis.patient.domain.model.Representative;
import com.adakadavra.dentis.patient.infrastructure.persistence.entity.PatientEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-06T12:05:09+0200",
    comments = "version: 1.6.2, compiler: javac, environment: Java 25.0.2 (JetBrains s.r.o.)"
)
@Component
public class PatientEntityMapperImpl implements PatientEntityMapper {

    @Override
    public PatientEntity toEntity(Patient patient) {
        if ( patient == null ) {
            return null;
        }

        PatientEntity.PatientEntityBuilder patientEntity = PatientEntity.builder();

        patientEntity.email( patientContactInfoEmail( patient ) );
        patientEntity.phoneNumber( patientContactInfoPhoneNumber( patient ) );
        patientEntity.alternativePhone( patientContactInfoAlternativePhone( patient ) );
        patientEntity.street( patientAddressStreet( patient ) );
        patientEntity.city( patientAddressCity( patient ) );
        patientEntity.state( patientAddressState( patient ) );
        patientEntity.zipCode( patientAddressZipCode( patient ) );
        patientEntity.representativeFullName( patientRepresentativeFullName( patient ) );
        patientEntity.representativeIdDocument( patientRepresentativeIdDocument( patient ) );
        patientEntity.representativeRelationship( patientRepresentativeRelationship( patient ) );
        patientEntity.representativePhone( patientRepresentativePhoneNumber( patient ) );
        patientEntity.id( patient.getId() );
        patientEntity.firstName( patient.getFirstName() );
        patientEntity.lastName( patient.getLastName() );
        patientEntity.idDocument( patient.getIdDocument() );
        patientEntity.birthDate( patient.getBirthDate() );
        patientEntity.sex( patient.getSex() );
        patientEntity.gender( patient.getGender() );
        patientEntity.socialName( patient.getSocialName() );
        patientEntity.active( patient.isActive() );
        patientEntity.clinicId( patient.getClinicId() );

        return patientEntity.build();
    }

    @Override
    public Patient toDomain(PatientEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Patient.PatientBuilder patient = Patient.builder();

        patient.id( entity.getId() );
        patient.firstName( entity.getFirstName() );
        patient.lastName( entity.getLastName() );
        patient.idDocument( entity.getIdDocument() );
        patient.birthDate( entity.getBirthDate() );
        patient.sex( entity.getSex() );
        patient.gender( entity.getGender() );
        patient.socialName( entity.getSocialName() );
        patient.active( entity.isActive() );
        patient.clinicId( entity.getClinicId() );

        patient.contactInfo( buildContactInfo(entity) );
        patient.address( buildAddress(entity) );
        patient.representative( buildRepresentative(entity) );

        return patient.build();
    }

    private String patientContactInfoEmail(Patient patient) {
        ContactInfo contactInfo = patient.getContactInfo();
        if ( contactInfo == null ) {
            return null;
        }
        return contactInfo.getEmail();
    }

    private String patientContactInfoPhoneNumber(Patient patient) {
        ContactInfo contactInfo = patient.getContactInfo();
        if ( contactInfo == null ) {
            return null;
        }
        return contactInfo.getPhoneNumber();
    }

    private String patientContactInfoAlternativePhone(Patient patient) {
        ContactInfo contactInfo = patient.getContactInfo();
        if ( contactInfo == null ) {
            return null;
        }
        return contactInfo.getAlternativePhone();
    }

    private String patientAddressStreet(Patient patient) {
        Address address = patient.getAddress();
        if ( address == null ) {
            return null;
        }
        return address.getStreet();
    }

    private String patientAddressCity(Patient patient) {
        Address address = patient.getAddress();
        if ( address == null ) {
            return null;
        }
        return address.getCity();
    }

    private String patientAddressState(Patient patient) {
        Address address = patient.getAddress();
        if ( address == null ) {
            return null;
        }
        return address.getState();
    }

    private String patientAddressZipCode(Patient patient) {
        Address address = patient.getAddress();
        if ( address == null ) {
            return null;
        }
        return address.getZipCode();
    }

    private String patientRepresentativeFullName(Patient patient) {
        Representative representative = patient.getRepresentative();
        if ( representative == null ) {
            return null;
        }
        return representative.getFullName();
    }

    private String patientRepresentativeIdDocument(Patient patient) {
        Representative representative = patient.getRepresentative();
        if ( representative == null ) {
            return null;
        }
        return representative.getIdDocument();
    }

    private String patientRepresentativeRelationship(Patient patient) {
        Representative representative = patient.getRepresentative();
        if ( representative == null ) {
            return null;
        }
        return representative.getRelationship();
    }

    private String patientRepresentativePhoneNumber(Patient patient) {
        Representative representative = patient.getRepresentative();
        if ( representative == null ) {
            return null;
        }
        return representative.getPhoneNumber();
    }
}
