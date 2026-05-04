package com.dentis.application.service.impl;

import com.dentis.application.dto.request.CreateDentistRequest;
import com.dentis.application.dto.response.DentistResponse;
import com.dentis.application.mapper.DentistMapper;
import com.dentis.application.service.DentistService;
import com.dentis.common.exception.BusinessException;
import com.dentis.common.exception.ResourceNotFoundException;
import com.dentis.domain.entity.Dentist;
import com.dentis.domain.repository.DentistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DentistServiceImpl implements DentistService {

    private final DentistRepository dentistRepository;
    private final DentistMapper dentistMapper;

    @Override
    @Transactional
    public DentistResponse create(CreateDentistRequest request) {
        if (dentistRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("A dentist with email " + request.getEmail() + " already exists",
                    "DUPLICATE_EMAIL");
        }
        Dentist dentist = dentistMapper.toEntity(request);
        return dentistMapper.toResponse(dentistRepository.save(dentist));
    }

    @Override
    public DentistResponse findById(String id) {
        return dentistMapper.toResponse(findDentistOrThrow(id));
    }

    @Override
    public List<DentistResponse> findAllActive() {
        return dentistRepository.findByActiveTrue().stream()
                .map(dentistMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deactivate(String id) {
        Dentist dentist = findDentistOrThrow(id);
        dentist.setActive(false);
        dentistRepository.save(dentist);
    }

    private Dentist findDentistOrThrow(String id) {
        return dentistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dentist", id));
    }
}
