package com.kevinguanchedarias.owgejava.business.rule;

import com.kevinguanchedarias.owgejava.business.rule.itemtype.RuleItemTypeProvider;
import com.kevinguanchedarias.owgejava.business.rule.type.RuleTypeProvider;
import com.kevinguanchedarias.owgejava.converter.rule.RuleDtoToEntityConverter;
import com.kevinguanchedarias.owgejava.converter.rule.RuleEntityToDtoConverter;
import com.kevinguanchedarias.owgejava.entity.Rule;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.RuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collection;
import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.RuleMock.FIRST_EXTRA_ARG;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.ORIGIN_ID;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.ORIGIN_TYPE;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.SECOND_EXTRA_ARG;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.TYPE;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRule;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRuleDto;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRuleItemTypeDescriptor;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRuleTypeDescriptorDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                RuleBo.class,
                RuleEntityToDtoConverter.class,
                RuleDtoToEntityConverter.class,
                DefaultConversionService.class
        }
)
@MockBean({
        RuleRepository.class,
        RuleItemTypeProvider.class,
        RuleTypeProvider.class

})
class RuleBoTest {
    private final RuleRepository ruleRepository;
    private final RuleBo ruleBo;
    private final RuleItemTypeProvider ruleItemTypeProvider;
    private final RuleTypeProvider ruleTypeProvider;

    @Autowired
    public RuleBoTest(
            RuleRepository ruleRepository,
            RuleBo ruleBo,
            DefaultConversionService conversionService,
            Collection<Converter<?, ?>> converters,
            RuleItemTypeProvider ruleItemTypeProvider,
            RuleTypeProvider ruleTypeProvider
    ) {
        this.ruleRepository = ruleRepository;
        this.ruleBo = ruleBo;
        this.ruleItemTypeProvider = ruleItemTypeProvider;
        this.ruleTypeProvider = ruleTypeProvider;
        converters.forEach(conversionService::addConverter);
    }

    @Test
    void findByOriginTypeAndOriginId_should_work() {
        var rule = givenRule();
        when(ruleRepository.findByOriginTypeAndOriginId(ORIGIN_TYPE, ORIGIN_ID)).thenReturn(List.of(rule));

        var result = ruleBo.findByOriginTypeAndOriginId(ORIGIN_TYPE, ORIGIN_ID);

        verify(ruleRepository, times(1)).findByOriginTypeAndOriginId(ORIGIN_TYPE, ORIGIN_ID);
        assertThat(result).hasSize(1);
    }

    @Test
    void findByType_should_return_types_by_id() {
        var rule = givenRule();
        when(ruleRepository.findByType(TYPE)).thenReturn(List.of(rule));

        var result = ruleBo.findByType(TYPE);

        verify(ruleRepository, times(1)).findByType(TYPE);
        assertThat(result).hasSize(1);
        var resultDto = result.get(0);
        assertEquals(rule.getId(), resultDto.getId());
        assertEquals(rule.getType(), resultDto.getType());
        assertEquals(rule.getOriginType(), resultDto.getOriginType());
        assertEquals(rule.getOriginId(), resultDto.getOriginId());
        assertEquals(rule.getDestinationType(), resultDto.getDestinationType());
        assertEquals(rule.getDestinationId(), resultDto.getDestinationId());
        assertEquals(List.of(FIRST_EXTRA_ARG, SECOND_EXTRA_ARG), resultDto.getExtraArgs());
    }

    @Test
    void deleteById_should_work() {
        int id = 4;

        ruleBo.deleteById(id);

        verify(ruleRepository, times(1)).deleteById(id);
    }

    @Test
    void save_should_work() {
        var dto = givenRuleDto();
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = ruleBo.save(dto);

        assertEquals(dto, saved);
        var captor = ArgumentCaptor.forClass(Rule.class);
        verify(ruleRepository, times(1)).save(captor.capture());
        var entityToBeSaved = captor.getValue();
        assertEquals(dto.getId(), entityToBeSaved.getId());
        assertEquals(dto.getType(), entityToBeSaved.getType());
        assertEquals(dto.getOriginType(), entityToBeSaved.getOriginType());
        assertEquals(dto.getOriginId(), entityToBeSaved.getOriginId());
        assertEquals(dto.getDestinationType(), entityToBeSaved.getDestinationType());
        assertEquals(dto.getDestinationId(), entityToBeSaved.getDestinationId());
        assertEquals(FIRST_EXTRA_ARG + "#" + SECOND_EXTRA_ARG, entityToBeSaved.getExtraArgs());
    }

    @Test
    void findItemTypeDescriptor_should_work() {
        var itemType = "UNIT";
        var expectedResult = givenRuleItemTypeDescriptor();
        when(ruleItemTypeProvider.getRuleItemTypeId()).thenReturn(itemType);
        when(ruleItemTypeProvider.findRuleItemTypeDescriptor()).thenReturn(expectedResult);

        var result = this.ruleBo.findItemTypeDescriptor(itemType);

        verify(ruleItemTypeProvider, times(1)).getRuleItemTypeId();
        verify(ruleItemTypeProvider, times(1)).findRuleItemTypeDescriptor();
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void findItemTypeDescriptor_should_throw_when_no_provider_for_given_type() {
        when(ruleItemTypeProvider.getRuleItemTypeId()).thenReturn("OTHER");

        assertThatThrownBy(() -> ruleBo.findItemTypeDescriptor("UNIT"))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageStartingWith("No item type");
        verify(ruleItemTypeProvider, never()).findRuleItemTypeDescriptor();
    }

    @Test
    void findTypeDescriptor_should_work() {
        var type = "UNIT_CAPTURE";
        var expectedResult = givenRuleTypeDescriptorDto();
        when(ruleTypeProvider.getRuleTypeId()).thenReturn(type);
        when(ruleTypeProvider.findRuleTypeDescriptor()).thenReturn(expectedResult);

        var result = ruleBo.findTypeDescriptor(type);

        verify(ruleTypeProvider, times(1)).getRuleTypeId();
        verify(ruleTypeProvider, times(1)).findRuleTypeDescriptor();
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void findTypeDescriptor_should_throw_when_no_provider_for_given_type() {
        when(ruleTypeProvider.getRuleTypeId()).thenReturn("OTHER");

        assertThatThrownBy(() -> ruleBo.findTypeDescriptor("CAPTURE_UNIT"))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageStartingWith("No type");
        verify(ruleTypeProvider, never()).findRuleTypeDescriptor();
    }

    @Test
    void findExtraArg_should_work_when_arg_exists() {
        var rule = givenRule();

        var result = ruleBo.findExtraArg(rule, 1);

        assertThat(result)
                .isPresent()
                .contains(SECOND_EXTRA_ARG);
    }

    @Test
    void findExtraArg_should_return_empty_when_no_such_arg() {
        var result = ruleBo.findExtraArg(givenRule(), 30);

        assertThat(result).isNotPresent();
    }

    @Test
    void findExtraArgs_should_work() {
        assertThat(ruleBo.findExtraArgs(givenRule()))
                .hasSize(2)
                .containsAll(List.of(FIRST_EXTRA_ARG, SECOND_EXTRA_ARG));
    }

    @Test
    void hasExtraArg_should_work() {
        var rule = givenRule();

        assertThat(ruleBo.hasExtraArg(rule, 0)).isTrue();
        assertThat(ruleBo.hasExtraArg(rule, 1)).isTrue();
        assertThat(ruleBo.hasExtraArg(rule, 2)).isFalse();
    }
}
