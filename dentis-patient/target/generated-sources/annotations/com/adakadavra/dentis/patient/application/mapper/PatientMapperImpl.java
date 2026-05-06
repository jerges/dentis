package com.adakadavra.dentis.patient.application.mapper;

import com.adakadavra.dentis.patient.application.dto.AddressDto;
import com.adakadavra.dentis.patient.application.dto.ContactInfoDto;
import com.adakadavra.dentis.patient.application.dto.CreatePatientRequest;
import com.adakadavra.dentis.patient.application.dto.PatientResponse;
import com.adakadavra.dentis.patient.application.dto.RepresentativeDto;
import com.adakadavra.dentis.patient.domain.model.Address;
import com.adakadavra.dentis.patient.domain.model.ContactInfo;
import com.adakadavra.dentis.patient.domain.model.Patient;
import com.adakadavra.dentis.patient.domain.model.Representative;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-06T12:05:09+0200",
    comments = "version: 1.6.2, compiler: javac, environment: Java 25.0.2 (JetBrains s.r.o.)"
)
@Component
public class PatientMapperImpl implements PatientMapper {

    @Override
    public Patient toDomain(CreatePatientRequest request) {
        if ( request == null ) {
            return null;
        }

        Patient.PatientBuilder patient = Patient.builder();

        patient.firstName( request.getFirstName() );
        patient.lastName( request.getLastName() );
        patient.idDocument( request.getIdDocument() );
        patient.birthDate( request.getBirthDate() );
        patient.sex( request.getSex() );
        patient.gender( request.getGender() );
        patient.socialName( request.getSocialName() );
        patient.contactInfo( toDomain( request.getContactInfo() ) );
        patient.address( toDomain( request.getAddress() ) );
        patient.representative( toDomain( request.getRepresentative() ) );

        patient.id( java.util.UUID.randomUUID() );
        patient.active( true );

        return patient.build();
    }

    @Override
    public PatientResponse toResponse(Patient patient) {
        if ( patient == null ) {
            return null;
        }

        PatientResponse.PatientResponseBuilder patientResponse = PatientResponse.builder();

        patientResponse.id( patient.getId() );
        patientResponse.firstName( patient.getFirstName() );
        patientResponse.lastName( patient.getLastName() );
        patientResponse.idDocument( patient.getIdDocument() );
        patientResponse.birthDate( patient.getBirthDate() );
        patientResponse.sex( patient.getSex() );
        patientResponse.gender( patient.getGender() );
        patientResponse.socialName( patient.getSocialName() );
        patientResponse.contactInfo( toDto( patient.getContactInfo() ) );
        patientResponse.address( toDto( patient.getAddress() ) );
        patientResponse.representative( toDto( patient.getRepresentative() ) );
        patientResponse.active( patient.isActive() );

        patientResponse.fullName( patient.fullName() );
        patientResponse.age( calculateAge(patient.getBirthDate()) );

        return patientResponse.build();
    }

    @Override
    public ContactInfo toDomain(ContactInfoDto dto) {
        if ( dto == null ) {
            return null;
        }

        ContactInfo.ContactInfoBuilder contactInfo = ContactInfo.builder();

        contactInfo.email( dto.getEmail() );
        contactInfo.phoneNumber( dto.getPhoneNumber() );
        contactInfo.alternativePhone( dto.getAlternativePhone() );

        return contactInfo.build();
    }

    @Override
    public ContactInfoDto toDto(ContactInfo contactInfo) {
        if ( contactInfo == null ) {
            return null;
        }

        ContactInfoDto.ContactInfoDtoBuilder contactInfoDto = ContactInfoDto.builder();

        contactInfoDto.email( contactInfo.getEmail() );
        contactInfoDto.phoneNumber( contactInfo.getPhoneNumber() );
        contactInfoDto.alternativePhone( contactInfo.getAlternativePhone() );

        return contactInfoDto.build();
    }

    @Override
    public Address toDomain(AddressDto dto) {
        if ( dto == null ) {
            return null;
        }

        Address.AddressBuilder address = Address.builder();

        address.street( dto.getStreet() );
        address.city( dto.getCity() );
        address.state( dto.getState() );
        address.zipCode( dto.getZipCode() );

        return address.build();
    }

    @Override
    public AddressDto toDto(Address address) {
        if ( address == null ) {
            return null;
        }

        AddressDto.AddressDtoBuilder addressDto = AddressDto.builder();

        addressDto.street( address.getStreet() );
        addressDto.city( address.getCity() );
        addressDto.state( address.getState() );
        addressDto.zipCode( address.getZipCode() );

        return addressDto.build();
    }

    @Override
    public Representative toDomain(RepresentativeDto dto) {
        if ( dto == null ) {
            return null;
        }

        Representative.RepresentativeBuilder representative = Representative.builder();

        representative.fullName( dto.getFullName() );
        representative.idDocument( dto.getIdDocument() );
        representative.relationship( dto.getRelationship() );
        representative.phoneNumber( dto.getPhoneNumber() );

        return representative.build();
    }

    @Override
    public RepresentativeDto toDto(Representative representative) {
        if ( representative == null ) {
            return null;
        }

        RepresentativeDto.RepresentativeDtoBuilder representativeDto = RepresentativeDto.builder();

        representativeDto.fullName( representative.getFullName() );
        representativeDto.idDocument( representative.getIdDocument() );
        representativeDto.relationship( representative.getRelationship() );
        representativeDto.phoneNumber( representative.getPhoneNumber() );

        return representativeDto.build();
    }
}
