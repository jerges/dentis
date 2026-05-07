package com.adakadavra.dentis.billing.domain.service;

import com.adakadavra.dentis.billing.domain.model.Tariff;
import com.adakadavra.dentis.billing.domain.repository.TariffRepository;
import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TariffService {

    private final TariffRepository tariffRepository;

    @Transactional
    public Tariff createTariff(Tariff tariff) {
        if (tariffRepository.existsByCode(tariff.getCode())) {
            throw new BusinessRuleException("A tariff with code '" + tariff.getCode() + "' already exists", "DUPLICATE_TARIFF_CODE");
        }
        return tariffRepository.save(tariff);
    }

    public Page<Tariff> findAll(Pageable pageable) {
        return tariffRepository.findAll(pageable);
    }

    @Transactional
    public void deactivateTariff(UUID id) {
        tariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff", id));
        tariffRepository.deactivate(id);
    }
}

