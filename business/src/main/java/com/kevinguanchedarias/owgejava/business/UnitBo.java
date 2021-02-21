package com.kevinguanchedarias.owgejava.business;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dto.InterceptableSpeedGroupDto;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.entity.InterceptableSpeedGroup;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.ResourceRequirementsPojo;
import com.kevinguanchedarias.owgejava.repository.InterceptableSpeedGroupRepository;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;

@Component
public class UnitBo implements WithNameBo<Integer, Unit, UnitDto>, WithUnlockableBo<Integer, Unit, UnitDto> {
	private static final long serialVersionUID = 8956360591688432113L;

	@Autowired
	private UnitRepository unitRepository;

	@Autowired
	private UnlockedRelationBo unlockedRelationBo;

	@Autowired
	private ObjectRelationBo objectRelationBo;

	@Autowired
	private ObtainedUnitBo obtainedUnitBo;

	@Autowired
	private ImprovementBo improvementBo;

	@Autowired
	private transient InterceptableSpeedGroupRepository interceptableSpeedGroupRepository;

	@Autowired
	private SpeedImpactGroupBo speedImpactGroupBo;

	@Override
	public JpaRepository<Unit, Integer> getRepository() {
		return unitRepository;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<UnitDto> getDtoClass() {
		return UnitDto.class;
	}

	@Override
	public UnlockedRelationBo getUnlockedRelationBo() {
		return unlockedRelationBo;
	}

	@Override
	public ObjectEnum getObject() {
		return ObjectEnum.UNIT;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.kevinguanchedarias.owgejava.business.BaseBo#save(com.kevinguanchedarias.
	 * owgejava.entity.EntityWithId)
	 */
	@Override
	public Unit save(Unit entity) {
		improvementBo.clearCacheEntriesIfRequired(entity, obtainedUnitBo);
		return WithNameBo.super.save(entity);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#save(java.util.List)
	 */
	@Override
	public void save(List<Unit> entities) {
		improvementBo.clearCacheEntries(obtainedUnitBo);
		WithNameBo.super.save(entities);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.kevinguanchedarias.owgejava.business.BaseBo#delete(java.io.Serializable)
	 */
	@Transactional
	@Override
	public void delete(Unit unit) {
		objectRelationBo.delete(objectRelationBo.findOneByObjectTypeAndReferenceId(getObject(), unit.getId()));
		Set<UserStorage> affectedUsers = new HashSet<>();
		obtainedUnitBo.findByUnit(unit).forEach(obtainedUnit -> affectedUsers.add(obtainedUnit.getUser()));
		obtainedUnitBo.deleteByUnit(unit);
		improvementBo.clearCacheEntriesIfRequired(unit, obtainedUnitBo);
		affectedUsers.forEach(user -> {
			obtainedUnitBo.emitObtainedUnitChange(user.getId());
			if (unit.getImprovement() != null) {
				improvementBo.emitUserImprovement(user);
			}
		});
		WithNameBo.super.delete(unit);
	}

	@Transactional
	@Override
	public void delete(Integer id) {
		delete(findByIdOrDie(id));
	}

	/**
	 * Calculates the requirements according to the count to operate!
	 *
	 * @param unitId
	 * @param count
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public ResourceRequirementsPojo calculateRequirements(Integer unitId, Long count) {
		return calculateRequirements(findByIdOrDie(unitId), count);
	}

	/**
	 * Calculates the requirements according to the count to operate!
	 *
	 * @param unit
	 * @param count
	 * @return
	 * @throws SgtBackendInvalidInputException can't be negative
	 * @author Kevin Guanche Darias
	 */
	public ResourceRequirementsPojo calculateRequirements(Unit unit, Long count) {
		if (count < 1) {
			throw new SgtBackendInvalidInputException("Input can't be negative");
		}

		ResourceRequirementsPojo retVal = new ResourceRequirementsPojo();
		retVal.setRequiredPrimary((double) (unit.getPrimaryResource() * count));
		retVal.setRequiredSecondary((double) (unit.getSecondaryResource() * count));
		retVal.setRequiredTime((double) (unit.getTime() * count));
		retVal.setRequiredEnergy((double) (ObjectUtils.firstNonNull(unit.getEnergy(), 0) * count));
		return retVal;
	}

	public boolean isUnique(Unit unit) {
		return unit.getIsUnique();
	}

	/**
	 * Checks if the unique unit has been build by the user
	 *
	 * @param user
	 * @param unit
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void checkIsUniqueBuilt(UserStorage user, Unit unit) {
		if (isUnique(unit) && obtainedUnitBo.countByUserAndUnitId(user, unit.getId()) > 0) {
			throw new SgtBackendInvalidInputException(
					"Unit with id " + unit.getId() + " has been already build by user " + user.getId());
		}

	}

	/**
	 *
	 * @param interceptableSpeedGroupDtos
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void saveSpeedImpactGroupInterceptors(int unitId,
			List<InterceptableSpeedGroupDto> interceptableSpeedGroupDtos) {
		Unit unit = getOne(unitId);
		interceptableSpeedGroupRepository.deleteByUnit(unit);
		interceptableSpeedGroupDtos.forEach(current -> {
			InterceptableSpeedGroup interceptableSpeedGroup = new InterceptableSpeedGroup();
			interceptableSpeedGroup.setUnit(unit);
			interceptableSpeedGroup
					.setSpeedImpactGroup(speedImpactGroupBo.getOne(current.getSpeedImpactGroup().getId()));
			interceptableSpeedGroupRepository.save(interceptableSpeedGroup);
		});
	}
}
