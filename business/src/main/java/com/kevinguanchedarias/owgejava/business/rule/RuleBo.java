package com.kevinguanchedarias.owgejava.business.rule;

import com.kevinguanchedarias.owgejava.business.rule.itemtype.RuleItemTypeProvider;
import com.kevinguanchedarias.owgejava.business.rule.type.RuleTypeProvider;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleItemTypeDescriptorDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleTypeDescriptorDto;
import com.kevinguanchedarias.owgejava.entity.Rule;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.RuleRepository;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class RuleBo {
    public static final String ARGS_DELIMITER = "#";

    private final RuleRepository ruleRepository;
    private final ConversionService conversionService;
    private final List<RuleItemTypeProvider> ruleItemTypeProviders;
    private final List<RuleTypeProvider> ruleTypeProviders;

    public List<RuleDto> findByOriginTypeAndOriginId(String originType, long id) {
        return ruleRepository.findByOriginTypeAndOriginId(originType, id).stream()
                .map(entity -> conversionService.convert(entity, RuleDto.class))
                .toList();
    }

    public List<RuleDto> findByType(String type) {
        return ruleRepository.findByType(type)
                .stream()
                .map(entity -> conversionService.convert(entity, RuleDto.class))
                .toList();
    }

    public void deleteById(int id) {
        ruleRepository.deleteById(id);
    }

    public RuleDto save(RuleDto ruleDto) {
        var saved = ruleRepository.save(Objects.requireNonNull(conversionService.convert(ruleDto, Rule.class)));
        return conversionService.convert(saved, RuleDto.class);
    }

    public RuleItemTypeDescriptorDto findItemTypeDescriptor(String itemType) {
        return ruleItemTypeProviders.stream()
                .filter(ruleItemTypeProvider -> ruleItemTypeProvider.getRuleItemTypeId().equals(itemType))
                .findFirst()
                .map(RuleItemTypeProvider::findRuleItemTypeDescriptor)
                .orElseThrow(() -> new SgtBackendInvalidInputException("No item type " + itemType + " exists"));
    }

    public RuleTypeDescriptorDto findTypeDescriptor(String type) {
        return ruleTypeProviders.stream()
                .filter(ruleTypeProvider -> ruleTypeProvider.getRuleTypeId().equals(type))
                .findFirst()
                .map(RuleTypeProvider::findRuleTypeDescriptor)
                .orElseThrow(() -> new SgtBackendInvalidInputException("No type " + type + " exists"));
    }
}
