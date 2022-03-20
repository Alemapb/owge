package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.ObjectRelationDto;
import com.kevinguanchedarias.owgejava.dto.RequirementGroupDto;
import com.kevinguanchedarias.owgejava.entity.ObjectRelationToObjectRelation;
import com.kevinguanchedarias.owgejava.entity.RequirementGroup;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementGroupRepository;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Service
public class RequirementGroupBo implements BaseBo<Integer, RequirementGroup, RequirementGroupDto> {
    public static final String REQUIREMENT_GROUP_CACHE_TAG = "requirement_group";

    @Serial
    private static final long serialVersionUID = 7208249910149543205L;

    @Autowired
    private transient RequirementGroupRepository repository;

    @Autowired
    private ObjectRelationBo objectRelationBo;

    @Autowired
    private RequirementBo requirementBo;

    @Autowired
    private transient ObjectRelationToObjectRelationRepository objectRelationRequirementGroupRepository;

    @Autowired
    private transient TaggableCacheManager taggableCacheManager;

    @Override
    public Class<RequirementGroupDto> getDtoClass() {
        return RequirementGroupDto.class;
    }

    @Override
    public JpaRepository<RequirementGroup, Integer> getRepository() {
        return repository;
    }

    @Override
    public TaggableCacheManager getTaggableCacheManager() {
        return taggableCacheManager;
    }

    @Override
    public String getCacheTag() {
        return REQUIREMENT_GROUP_CACHE_TAG;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Transactional
    public RequirementGroup addRequirementGroup(ObjectEnum targetObject, Integer referenceId,
                                                RequirementGroupDto requirementGroupDto) {
        ObjectRelationToObjectRelation currentGroup = new ObjectRelationToObjectRelation();
        RequirementGroup requirementGroup = new RequirementGroup();
        requirementGroup.setName(requirementGroupDto.getName());
        RequirementGroup saved = save(requirementGroup);
        currentGroup.setMaster(objectRelationBo.findObjectRelationOrCreate(targetObject, referenceId));

        if (requirementGroupDto.getRequirements() != null) {
            requirementGroupDto.getRequirements().forEach(requirementInformationDto -> {
                ObjectRelationDto objectRelationDto = new ObjectRelationDto();
                objectRelationDto.dtoFromEntity(saved.getRelation());
                requirementInformationDto.setRelation(objectRelationDto);
                requirementBo.addRequirementFromDto(requirementInformationDto);
            });
        }
        currentGroup.setSlave(save(requirementGroup).getRelation());
        objectRelationRequirementGroupRepository.save(currentGroup);
        return requirementGroup;
    }

}
