package com.kevinguanchedarias.owgejava.business;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.owgejava.entity.AttackRule;
import com.kevinguanchedarias.owgejava.entity.AttackRuleEntry;
import com.kevinguanchedarias.owgejava.entity.EntityWithMissionLimitation;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.listener.ImageStoreListener;
import com.kevinguanchedarias.owgejava.enumerations.AttackableTargetEnum;
import com.kevinguanchedarias.owgejava.enumerations.DeployMissionConfigurationEnum;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionSupportEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtCorruptDatabaseException;
import com.kevinguanchedarias.owgejava.exception.UserNotFoundException;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.pojo.websocket.MissionWebsocketMessage;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;

@Service
public class UnitMissionBo extends AbstractMissionBo {
	private static final String ENEMY_MISSION_CHANGE = "enemy_mission_change";

	private static final long serialVersionUID = 344402831344882216L;

	private static final Logger LOG = Logger.getLogger(UnitMissionBo.class);
	private static final String JOB_GROUP_NAME = "UnitMissions";
	private static final String MAX_PLANETS_MESSAGE = "You already have the max planets, you can have";

	@Autowired
	private ImageStoreBo imageStoreBo;

	/**
	 * Represents an ObtainedUnit, its full attack, and the pending attack is has
	 *
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public class AttackObtainedUnit {
		AttackUserInformation user;
		Double pendingAttack;
		boolean noAttack = false;
		Double availableShield;
		Double availableHealth;
		Long finalCount;
		ObtainedUnit obtainedUnit;

		private Double totalAttack;
		private Long initialCount;
		private Double totalShield;
		private Double totalHealth;

		private Double initialHealth;

		public AttackObtainedUnit() {
			throw new ProgrammingException("Can't use AttackObtainedUnit");
		}

		public AttackObtainedUnit(ObtainedUnit obtainedUnit, GroupedImprovement userImprovement) {
			Unit unit = obtainedUnit.getUnit();
			UnitType unitType = unit.getType();
			initialCount = obtainedUnit.getCount();
			finalCount = initialCount;
			totalAttack = initialCount.doubleValue() * unit.getAttack();
			totalAttack += (totalAttack * improvementBo.findAsRational(
					(double) userImprovement.findUnitTypeImprovement(ImprovementTypeEnum.ATTACK, unitType)));
			pendingAttack = totalAttack;
			totalShield = initialCount.doubleValue() * unit.getShield();
			totalShield += (totalShield * improvementBo.findAsRational(
					(double) userImprovement.findUnitTypeImprovement(ImprovementTypeEnum.SHIELD, unitType)));
			availableShield = totalShield;
			totalHealth = initialCount.doubleValue() * unit.getHealth();
			initialHealth = totalHealth;
			totalHealth += (totalHealth * improvementBo.findAsRational(
					(double) userImprovement.findUnitTypeImprovement(ImprovementTypeEnum.DEFENSE, unitType)));
			availableHealth = totalHealth;
			this.obtainedUnit = obtainedUnit;
		}

		public Double getTotalAttack() {
			return totalAttack;
		}

		public void setTotalAttack(Double totalAttack) {
			this.totalAttack = totalAttack;
		}

		public Long getInitialCount() {
			return initialCount;
		}

		public void setInitialCount(Long initialCount) {
			this.initialCount = initialCount;
		}

		public Double getTotalShield() {
			return totalShield;
		}

		public void setTotalShield(Double totalShield) {
			this.totalShield = totalShield;
		}

		public Double getTotalHealth() {
			return totalHealth;
		}

		public void setTotalHealth(Double totalHealth) {
			this.totalHealth = totalHealth;
		}

		public Long getFinalCount() {
			return finalCount;
		}

		public ObtainedUnit getObtainedUnit() {
			return obtainedUnit;
		}

		public Double getInitialHealth() {
			return initialHealth;
		}

		public void setInitialHealth(Double initialHealth) {
			this.initialHealth = initialHealth;
		}

	}

	public class AttackUserInformation {
		AttackInformation attackInformationRef;
		Double earnedPoints = 0D;
		List<AttackObtainedUnit> units = new ArrayList<>();
		List<AttackObtainedUnit> attackableUnits;
		boolean isDefeated = false;
		boolean canAttack = true;

		private UserStorage user;
		private GroupedImprovement userImprovement;

		public AttackUserInformation(UserStorage user) {
			this.user = user;
			userImprovement = improvementBo.findUserImprovement(user);
		}

		public UserStorage getUser() {
			return user;
		}

		public Double getEarnedPoints() {
			return earnedPoints;
		}

		public GroupedImprovement getUserImprovement() {
			return userImprovement;
		}

		/**
		 * @return the units
		 * @since 0.9.0
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		public List<AttackObtainedUnit> getUnits() {
			return units;
		}

	}

	public class AttackInformation {
		private Mission attackMission;
		private boolean isRemoved = false;
		private Map<Integer, AttackUserInformation> users = new HashMap<>();
		private List<AttackObtainedUnit> units = new ArrayList<>();

		public AttackInformation() {
			throw new ProgrammingException(
					"Can't invoke constructor for " + this.getClass().getName() + " without arguments");
		}

		public AttackInformation(Mission attackMission) {
			this.attackMission = attackMission;
		}

		/**
		 * To have the expected behavior should be invoked after <i>startAttack()</i>
		 *
		 * @return true if the mission has been removed from the database
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		public boolean isMissionRemoved() {
			return isRemoved;
		}

		public void addUnit(ObtainedUnit unitEntity) {
			UserStorage userEntity = unitEntity.getUser();
			AttackUserInformation user;
			if (users.containsKey(userEntity.getId())) {
				user = users.get(userEntity.getId());
			} else {
				user = new AttackUserInformation(userEntity);
				users.put(userEntity.getId(), user);
			}
			AttackObtainedUnit unit = new AttackObtainedUnit(unitEntity, user.userImprovement);
			unit.user = user;
			user.units.add(unit);
			units.add(unit);
		}

		public void startAttack() {
			Collections.shuffle(units);
			users.forEach((userId, user) -> user.attackableUnits = units.stream().filter(
					unit -> !unit.user.user.getId().equals(user.user.getId()) && filterAlliance(user, unit.user))
					.collect(Collectors.toList()));
			doAttack();
			updatePoints();
		}

		/**
		 * @return the users
		 * @since 0.9.0
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		public Map<Integer, AttackUserInformation> getUsers() {
			return users;
		}

		public void setRemoved(boolean isRemoved) {
			this.isRemoved = isRemoved;
		}

		private void doAttack() {
			units.forEach(unit -> {
				List<AttackObtainedUnit> attackableByUnit = unit.user.attackableUnits.stream().filter(target -> {
					Unit unitEntity = unit.obtainedUnit.getUnit();
					AttackRule attackRule = ObjectUtils.firstNonNull(unitEntity.getAttackRule(),
							unitEntity.getType().getAttackRule());
					return canAttack(attackRule, target);
				}).collect(Collectors.toList());
				for (AttackObtainedUnit target : attackableByUnit) {
					attackTarget(unit, target);
					if (unit.noAttack) {
						break;
					}
				}
			});
		}

		private boolean canAttack(AttackRule attackRule, AttackObtainedUnit target) {
			if (attackRule == null || attackRule.getAttackRuleEntries() == null) {
				return true;
			} else {
				for (AttackRuleEntry ruleEntry : attackRule.getAttackRuleEntries()) {
					if (ruleEntry.getTarget() == AttackableTargetEnum.UNIT) {
						if (target.obtainedUnit.getUnit().getId().equals(ruleEntry.getReferenceId())) {
							return ruleEntry.getCanAttack();
						}
					} else if (ruleEntry.getTarget() == AttackableTargetEnum.UNIT_TYPE) {
						UnitType unitType = findUnitTypeMatchingRule(ruleEntry,
								target.obtainedUnit.getUnit().getType());
						if (unitType != null) {
							return ruleEntry.getCanAttack();
						}
					} else {
						throw new ProgrammingException("unexpected code path");
					}
				}
				return true;
			}
		}

		private UnitType findUnitTypeMatchingRule(AttackRuleEntry ruleEntry, UnitType unitType) {
			if (ruleEntry.getReferenceId().equals(unitType.getId())) {
				return unitType;
			} else if (unitType.getParent() != null) {
				return findUnitTypeMatchingRule(ruleEntry, unitType.getParent());
			} else {
				return null;
			}
		}

		private void attackTarget(AttackObtainedUnit source, AttackObtainedUnit target) {
			Double myAttack = source.pendingAttack;
			Double victimShield = target.availableShield;
			if (victimShield > myAttack) {
				source.pendingAttack = 0D;
				source.noAttack = true;
				target.availableShield -= myAttack;
			} else {
				myAttack -= target.availableShield;
				target.availableShield = 0D;
				Double victimHealth = target.availableHealth;
				addPointsAndUpdateCount(myAttack, source, target);
				if (victimHealth > myAttack) {
					source.pendingAttack = 0D;
					source.noAttack = true;
					target.availableHealth -= myAttack;
				} else {
					source.pendingAttack = myAttack - victimHealth;
					target.availableHealth = 0D;
					obtainedUnitBo.delete(target.obtainedUnit);
					deleteMissionIfRequired(target.obtainedUnit);
				}
				improvementBo.clearCacheEntriesIfRequired(target.obtainedUnit.getUnit(), obtainedUnitBo);
			}
		}

		/**
		 * Deletes the mission from the system, when all units involved are death
		 *
		 * Notice, should be invoked after <b>removing the obtained unit</b>
		 *
		 * @param obtainedUnit
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		private void deleteMissionIfRequired(ObtainedUnit obtainedUnit) {
			Mission mission = obtainedUnit.getMission();
			if (mission != null && !obtainedUnitBo.existsByMission(mission)) {
				if (attackMission.getId().equals(mission.getId())) {
					setRemoved(true);
				} else {
					delete(mission);
				}
			}
		}

		private void addPointsAndUpdateCount(double usedAttack, AttackObtainedUnit source,
				AttackObtainedUnit victimUnit) {
			Double healthForEachUnit = victimUnit.totalHealth / victimUnit.initialCount;
			Long killedCount = (long) Math.floor(usedAttack / healthForEachUnit);
			if (killedCount > victimUnit.finalCount) {
				killedCount = victimUnit.finalCount;
				victimUnit.finalCount = 0L;
			} else {
				victimUnit.finalCount -= killedCount;
			}
			source.user.earnedPoints += killedCount * victimUnit.obtainedUnit.getUnit().getPoints();
		}

		private void updatePoints() {
			Set<Integer> alteredUsers = new HashSet<>();
			users.entrySet().forEach(current -> {
				AttackUserInformation attackUserInformation = current.getValue();
				List<AttackObtainedUnit> userUnits = attackUserInformation.units;
				userStorageBo.addPointsToUser(attackUserInformation.getUser(), attackUserInformation.earnedPoints);
				obtainedUnitBo
						.save(userUnits.stream()
								.filter(currentUnit -> !currentUnit.finalCount.equals(0L)
										&& !currentUnit.initialCount.equals(currentUnit.finalCount))
								.map(currentUnit -> {
									currentUnit.obtainedUnit.setCount(currentUnit.finalCount);
									alteredUsers.add(attackUserInformation.getUser().getId());
									return currentUnit.obtainedUnit;
								}).collect(Collectors.toList()));
			});
			TransactionUtil.doAfterCommit(() -> alteredUsers.forEach(current -> {
				socketIoService.sendMessage(current, UNIT_TYPE_CHANGE,
						() -> unitTypeBo.findUnitTypesWithUserInfo(current));
				socketIoService.sendMessage(current, UNIT_OBTAINED_CHANGE,
						() -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(current)));

			}));
		}

		/**
		 * If the user has an alliance, removes all those users that are not in the user
		 * alliance
		 *
		 * @param current
		 * @return
		 * @since 0.7.0
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		private boolean filterAlliance(AttackUserInformation source, AttackUserInformation target) {
			return source.user.getAlliance() == null || target.user.getAlliance() == null
					|| !source.user.getAlliance().getId().equals(target.user.getAlliance().getId());
		}
	}

	@Autowired
	private ConfigurationBo configurationBo;

	@Autowired
	private transient SocketIoService socketIoService;

	@Autowired
	private transient EntityManager entityManager;

	@Override
	public String getGroupName() {
		return JOB_GROUP_NAME;
	}

	@Override
	public Logger getLogger() {
		return LOG;
	}

	/**
	 * Registers a explore mission <b>as logged in user</b>
	 *
	 * @param missionInformation <i>userId</i> is <b>ignored</b> in this method
	 *                           <b>immutable object</b>
	 * @return mission representation DTO
	 * @throws SgtBackendInvalidInputException When input information is not valid
	 * @throws UserNotFoundException           When user doesn't exists <b>(in this
	 *                                         universe)</b>
	 * @throws PlanetNotFoundException         When the planet doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public UnitRunningMissionDto myRegisterExploreMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		return adminRegisterExploreMission(missionInformation);
	}

	/**
	 * Registers a explore mission <b>as a admin</b>
	 *
	 * @param missionInformation
	 * @return mission representation DTO
	 * @throws SgtBackendInvalidInputException When input information is not valid
	 * @throws UserNotFoundException           When user doesn't exists <b>(in this
	 *                                         universe)</b>
	 * @throws PlanetNotFoundException         When the planet doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public UnitRunningMissionDto adminRegisterExploreMission(UnitMissionInformation missionInformation) {
		return commonMissionRegister(missionInformation, MissionType.EXPLORE);
	}

	@Transactional
	public UnitRunningMissionDto myRegisterGatherMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		return adminRegisterGatherMission(missionInformation);
	}

	@Transactional
	public UnitRunningMissionDto adminRegisterGatherMission(UnitMissionInformation missionInformation) {
		return commonMissionRegister(missionInformation, MissionType.GATHER);
	}

	@Transactional
	public UnitRunningMissionDto myRegisterEstablishBaseMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		return adminRegisterEstablishBase(missionInformation);
	}

	@Transactional
	public UnitRunningMissionDto adminRegisterEstablishBase(UnitMissionInformation missionInformation) {
		return commonMissionRegister(missionInformation, MissionType.ESTABLISH_BASE);
	}

	@Transactional
	public UnitRunningMissionDto myRegisterAttackMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		return adminRegisterAttackMission(missionInformation);
	}

	@Transactional
	public UnitRunningMissionDto adminRegisterAttackMission(UnitMissionInformation missionInformation) {
		return commonMissionRegister(missionInformation, MissionType.ATTACK);
	}

	@Transactional
	public UnitRunningMissionDto myRegisterCounterattackMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		return adminRegisterCounterattackMission(missionInformation);
	}

	@Transactional
	public UnitRunningMissionDto adminRegisterCounterattackMission(UnitMissionInformation missionInformation) {
		if (!planetBo.isOfUserProperty(missionInformation.getUserId(), missionInformation.getTargetPlanetId())) {
			throw new SgtBackendInvalidInputException(
					"TargetPlanet doesn't belong to sender user, try again dear Hacker, maybe next time you have some luck");
		}
		return commonMissionRegister(missionInformation, MissionType.COUNTERATTACK);
	}

	@Transactional
	public UnitRunningMissionDto myRegisterConquestMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		return adminRegisterConquestMission(missionInformation);
	}

	@Transactional
	public UnitRunningMissionDto adminRegisterConquestMission(UnitMissionInformation missionInformation) {
		if (planetBo.myIsOfUserProperty(missionInformation.getTargetPlanetId())) {
			throw new SgtBackendInvalidInputException(
					"Doesn't make sense to conquest your own planet... unless your population hates you, and are going to organize a rebelion");
		}
		if (planetBo.isHomePlanet(missionInformation.getTargetPlanetId())) {
			throw new SgtBackendInvalidInputException(
					"Can't steal a home planet to a user, would you like a bandit to steal in your own home??!");
		}
		if (planetBo.hasMaxPlanets(missionInformation.getUserId())) {
			throw new SgtBackendInvalidInputException(MAX_PLANETS_MESSAGE);
		}
		return commonMissionRegister(missionInformation, MissionType.CONQUEST);
	}

	@Transactional
	public UnitRunningMissionDto myRegisterDeploy(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		return adminRegisterDeploy(missionInformation);
	}

	@Transactional
	public UnitRunningMissionDto adminRegisterDeploy(UnitMissionInformation missionInformation) {
		if (missionInformation.getSourcePlanetId().equals(missionInformation.getTargetPlanetId())) {
			throw exceptionUtilService
					.createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_DEPLOY_ITSELF")
					.withDeveloperHintDoc(GameProjectsEnum.BUSINESS, getClass(), DocTypeEnum.EXCEPTIONS).build();
		}
		return commonMissionRegister(missionInformation, MissionType.DEPLOY);
	}

	/**
	 * Parses the exploration of a planet
	 *
	 * @param missionId
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void processExplore(Long missionId) {
		Mission mission = findById(missionId);
		UserStorage user = mission.getUser();
		List<ObtainedUnit> involvedUnits = findUnitsInvolved(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		if (!planetBo.isExplored(user, targetPlanet)) {
			planetBo.defineAsExplored(user, targetPlanet);
		}
		List<ObtainedUnit> unitsInPlanet = obtainedUnitBo.explorePlanetUnits(mission, targetPlanet);
		adminRegisterReturnMission(mission);
		UnitMissionReportBuilder builder = UnitMissionReportBuilder
				.create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
				.withExploredInformation(unitsInPlanet);
		handleMissionReportSave(mission, builder);
		resolveMission(mission);
	}

	@Transactional
	public void processGather(Long missionId) {
		Mission mission = findById(missionId);
		UserStorage user = mission.getUser();
		List<ObtainedUnit> involvedUnits = findUnitsInvolved(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		adminRegisterReturnMission(mission);
		Long gathered = involvedUnits.stream()
				.map(current -> ObjectUtils.firstNonNull(current.getUnit().getCharge(), 0) * current.getCount())
				.reduce(0L, (sum, current) -> sum + current);
		Double withPlanetRichness = gathered * targetPlanet.findRationalRichness();
		GroupedImprovement groupedImprovement = improvementBo.findUserImprovement(user);
		Double withUserImprovement = withPlanetRichness
				+ (withPlanetRichness * improvementBo.findAsRational(groupedImprovement.getMoreChargeCapacity()));
		Double primaryResource = withUserImprovement * 0.7;
		Double secondaryResource = withUserImprovement * 0.3;
		user.addtoPrimary(primaryResource);
		user.addToSecondary(secondaryResource);
		UnitMissionReportBuilder builder = UnitMissionReportBuilder
				.create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
				.withGatherInformation(primaryResource, secondaryResource);
		handleMissionReportSave(mission, builder);
		resolveMission(mission);
	}

	@Transactional
	public void processEstablishBase(Long missionId) {
		Mission mission = findById(missionId);
		UserStorage user = mission.getUser();
		List<ObtainedUnit> involvedUnits = findUnitsInvolved(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		UnitMissionReportBuilder builder = UnitMissionReportBuilder.create(user, mission.getSourcePlanet(),
				targetPlanet, involvedUnits);
		UserStorage planetOwner = targetPlanet.getOwner();
		boolean hasMaxPlanets = planetBo.hasMaxPlanets(user);
		boolean areUnitsHavingtoReturn = false;
		if (planetOwner != null || hasMaxPlanets) {
			adminRegisterReturnMission(mission);
			areUnitsHavingtoReturn = true;
			if (planetOwner != null) {
				builder.withEstablishBaseInformation(false, "The planet already belongs to a user");
			} else {
				builder.withEstablishBaseInformation(false, MAX_PLANETS_MESSAGE);
			}
		} else {
			builder.withEstablishBaseInformation(true);
			definePlanetAsOwnedBy(user, involvedUnits, targetPlanet);
		}
		handleMissionReportSave(mission, builder);
		resolveMission(mission);
		if (!areUnitsHavingtoReturn) {
			emitLocalMissionChange(mission);
		}
	}

	/**
	 * Due to lack of support from Quartz to access spring context from the
	 * EntityListener of {@link ImageStoreListener} we have to invoke the image URL
	 * computation from here
	 *
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 * @param missionId
	 * @return
	 */
	private List<ObtainedUnit> findUnitsInvolved(Long missionId) {
		List<ObtainedUnit> retVal = obtainedUnitBo.findLockedByMissionId(missionId);
		retVal.forEach(current -> imageStoreBo.computeImageUrl(current.getUnit().getImage()));
		return retVal;
	}

	@Transactional
	public void processAttack(Long missionId) {
		Mission mission = findById(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		AttackInformation attackInformation = buildAttackInformation(targetPlanet, mission);
		attackInformation.startAttack();
		if (!attackInformation.isMissionRemoved()) {
			adminRegisterReturnMission(mission);
		}
		resolveMission(mission);
		UnitMissionReportBuilder builder = UnitMissionReportBuilder
				.create(mission.getUser(), mission.getSourcePlanet(), targetPlanet, new ArrayList<>())
				.withAttackInformation(attackInformation);
		UserStorage invoker = mission.getUser();
		handleMissionReportSave(mission, builder, true,
				attackInformation.users.entrySet().stream().map(current -> current.getValue().user)
						.filter(user -> !user.getId().equals(invoker.getId())).collect(Collectors.toList()));
		handleMissionReportSave(mission, builder, false, invoker);
		if (attackInformation.isMissionRemoved()) {
			emitLocalMissionChange(mission);
		}
	}

	/**
	 * Executes the counterattack logic <br>
	 * <b>NOTICE: </b> For now the current implementation just calls the
	 * processAttack()
	 *
	 * @param missionId
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void processCounterattack(Long missionId) {
		processAttack(missionId);
	}

	/**
	 * Creates a return mission from an existing mission
	 *
	 * @param mission Existing mission that will be returned
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void adminRegisterReturnMission(Mission mission) {
		adminRegisterReturnMission(mission, null);
	}

	/**
	 * Creates a return mission from an existing mission
	 *
	 * @param mission            Existing mission that will be returned
	 * @param customRequiredTime If not null will be used as the time for the return
	 *                           mission, else will use source mission time
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void adminRegisterReturnMission(Mission mission, Double customRequiredTime) {
		Mission returnMission = new Mission();
		returnMission.setType(findMissionType(MissionType.RETURN_MISSION));
		returnMission.setRequiredTime(mission.getRequiredTime());
		Double requiredTime = customRequiredTime == null ? mission.getRequiredTime() : customRequiredTime;
		returnMission.setTerminationDate(computeTerminationDate(requiredTime));
		returnMission.setSourcePlanet(mission.getSourcePlanet());
		returnMission.setTargetPlanet(mission.getTargetPlanet());
		returnMission.setUser(mission.getUser());
		returnMission.setRelatedMission(mission);
		List<ObtainedUnit> obtainedUnits = obtainedUnitBo.findLockedByMissionId(mission.getId());
		missionRepository.saveAndFlush(returnMission);
		obtainedUnits.forEach(current -> current.setMission(returnMission));
		obtainedUnitBo.save(obtainedUnits);
		scheduleMission(returnMission);
		emitLocalMissionChange(returnMission);
	}

	@Transactional
	public void proccessReturnMission(Long missionId) {
		Mission mission = missionRepository.findById(missionId).get();
		Integer userId = mission.getUser().getId();
		List<ObtainedUnit> obtainedUnits = obtainedUnitBo.findLockedByMissionId(mission.getId());
		obtainedUnits.forEach(current -> obtainedUnitBo.moveUnit(current, userId, mission.getSourcePlanet().getId()));
		resolveMission(mission);
		emitLocalMissionChange(mission);
		TransactionUtil.doAfterCommit(() -> socketIoService.sendMessage(userId, UNIT_OBTAINED_CHANGE,
				() -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(userId))));
	}

	@Transactional
	public void processConquest(Long missionId) {
		Mission mission = findById(missionId);
		UserStorage user = mission.getUser();
		List<ObtainedUnit> involvedUnits = findUnitsInvolved(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		UnitMissionReportBuilder builder = UnitMissionReportBuilder.create(user, mission.getSourcePlanet(),
				targetPlanet, involvedUnits);
		boolean maxPlanets = planetBo.hasMaxPlanets(user);
		boolean areUnitsHavingToReturn = false;
		if (maxPlanets || planetBo.isHomePlanet(targetPlanet)) {
			adminRegisterReturnMission(mission);
			areUnitsHavingToReturn = true;
			if (maxPlanets) {
				builder.withConquestInformation(false, MAX_PLANETS_MESSAGE);
			} else {
				builder.withConquestInformation(false, "This is a home planet now, can't conquest it");
			}
		} else {
			obtainedUnitBo.deleteBySourcePlanetIdAndMissionIdNull(targetPlanet);
			UserStorage oldOwner = targetPlanet.getOwner();
			definePlanetAsOwnedBy(user, involvedUnits, targetPlanet);
			builder.withConquestInformation(true);
			if (oldOwner != null) {
				planetBo.emitPlanetOwnedChange(oldOwner);
				emitEnemyMissionsChange(oldOwner);
				UnitMissionReportBuilder enemyReportBuilder = UnitMissionReportBuilder
						.create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
						.withConquestInformation(true, "I18N_YOUR_PLANET_WAS_CONQUISTED");
				handleMissionReportSave(mission, enemyReportBuilder, true, oldOwner);
			}
		}
		handleMissionReportSave(mission, builder);
		resolveMission(mission);
		if (!areUnitsHavingToReturn) {
			emitLocalMissionChange(mission);
		}
	}

	@Transactional
	public void proccessDeploy(Long missionId) {
		Mission mission = findById(missionId);
		if (mission != null) {
			Integer userId = mission.getUser().getId();
			List<ObtainedUnit> alteredUnits = new ArrayList<>();
			findUnitsInvolved(missionId).forEach(current -> alteredUnits
					.add(obtainedUnitBo.moveUnit(current, userId, mission.getTargetPlanet().getId())));
			resolveMission(mission);
			emitLocalMissionChange(mission);
			TransactionUtil.doAfterCommit(() -> {
				alteredUnits.forEach(entityManager::refresh);
				socketIoService.sendMessage(userId, UNIT_OBTAINED_CHANGE,
						() -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(userId)));
			});
		}
	}

	@Transactional
	public void myCancelMission(Long missionId) {
		Mission mission = findById(missionId);
		if (mission == null) {
			throw new NotFoundException("No mission with id " + missionId + " was found");
		} else if (!mission.getUser().getId().equals(userStorageBo.findLoggedIn().getId())) {
			throw new SgtBackendInvalidInputException("You can't cancel other player missions");
		} else if (isOfType(mission, MissionType.RETURN_MISSION)) {
			throw new SgtBackendInvalidInputException("can't cancel return missions");
		} else {
			mission.setResolved(true);
			save(mission);
			long nowMillis = new Instant().getMillis();
			long terminationMillis = mission.getTerminationDate().getTime();
			long durationMillis = 0L;
			if (terminationMillis >= nowMillis) {
				Interval interval = new Interval(nowMillis, terminationMillis);
				durationMillis = (long) (interval.toDurationMillis() / 1000D);
			}
			adminRegisterReturnMission(mission, mission.getRequiredTime() - durationMillis);
		}
	}

	public List<ObtainedUnit> findInvolvedInMission(Mission mission) {
		return obtainedUnitBo.findLockedByMissionId(mission.getId());
	}

	/**
	 * finds user <b>not resolved</b> deployed mission, if none exists creates one
	 * <br>
	 * <b>IMPORTANT:</b> Will save the unit, because if the mission exists, has to
	 * remove the firstDeploymentMission
	 *
	 * @param origin
	 * @param unit
	 * @return
	 * @since 0.7.4
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public Mission findDeployedMissionOrCreate(ObtainedUnit unit) {
		UserStorage user = unit.getUser();
		Planet origin = unit.getSourcePlanet();
		Planet target = unit.getTargetPlanet();
		Mission existingMission = findOneByUserIdAndTypeAndTargetPlanet(user.getId(), MissionType.DEPLOYED,
				target.getId());
		if (existingMission != null) {
			unit.setFirstDeploymentMission(null);
			unit.setMission(existingMission);
			obtainedUnitBo.save(unit);
			return existingMission;
		} else {
			Mission deployedMission = new Mission();
			deployedMission.setType(findMissionType(MissionType.DEPLOYED));
			deployedMission.setUser(user);
			if (unit.getFirstDeploymentMission() == null) {
				deployedMission.setSourcePlanet(origin);
				deployedMission.setTargetPlanet(target);
				deployedMission = save(deployedMission);
				unit.setFirstDeploymentMission(deployedMission);
				obtainedUnitBo.save(unit);
			} else {
				Mission firstDeploymentMission = findById(unit.getFirstDeploymentMission().getId());
				deployedMission.setSourcePlanet(firstDeploymentMission.getSourcePlanet());
				deployedMission.setTargetPlanet(firstDeploymentMission.getTargetPlanet());
				deployedMission = save(deployedMission);
			}
			return deployedMission;
		}
	}

	/**
	 * Test if the given entity with mission limitations can do the mission
	 *
	 * @param user
	 * @param targetPlanet
	 * @param entityWithMissionLimitation
	 * @param missionType
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean canDoMission(UserStorage user, Planet targetPlanet,
			EntityWithMissionLimitation<Integer> entityWithMissionLimitation, MissionType missionType) {
		String targetMethod = "getCan" + WordUtils.capitalizeFully(missionType.name(), '_').replaceAll("_", "");
		try {
			MissionSupportEnum missionSupport = ((MissionSupportEnum) entityWithMissionLimitation.getClass()
					.getMethod(targetMethod).invoke(entityWithMissionLimitation));
			switch (missionSupport) {
			case ANY:
				return true;
			case OWNED_ONLY:
				return planetBo.isOfUserProperty(user, targetPlanet);
			case NONE:
				return false;
			default:
				throw new SgtCorruptDatabaseException(
						"unsupported mission support was specified: " + missionSupport.name());
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new SgtBackendInvalidInputException(
					"Could not invoke method " + targetMethod + " maybe it is not supported mission", e);
		}
	}

	/**
	 * Executes modifications to <i>missionInformation</i> to define the logged in
	 * user as the sender user
	 *
	 * @param missionInformation
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void myRegister(UnitMissionInformation missionInformation) {
		if (missionInformation.getUserId() == null) {
			missionInformation.setUserId(userStorageBo.findLoggedIn().getId());
		} else {
			checkInvokerIsTheLoggedUser(missionInformation.getUserId());
		}
	}

	private UnitRunningMissionDto commonMissionRegister(UnitMissionInformation missionInformation,
			MissionType missionType) {
		List<ObtainedUnit> obtainedUnits = new ArrayList<>();
		missionInformation.setMissionType(missionType);
		UserStorage user = userStorageBo.findLoggedIn();
		checkCanDoMisison(user);
		UnitMissionInformation targetMissionInformation = copyMissionInformation(missionInformation);
		Integer userId = user.getId();
		targetMissionInformation.setUserId(userId);
		if (missionType != MissionType.EXPLORE
				&& !planetBo.isExplored(userId, missionInformation.getTargetPlanetId())) {
			throw new SgtBackendInvalidInputException(
					"Can't send this mission, because target planet is not explored ");
		}
		Map<Integer, ObtainedUnit> dbUnits = checkAndLoadObtainedUnits(missionInformation);
		Mission mission = missionRepository.saveAndFlush((prepareMission(targetMissionInformation, missionType)));
		targetMissionInformation.getInvolvedUnits().forEach(current -> {
			ObtainedUnit currentObtainedUnit = new ObtainedUnit();
			currentObtainedUnit.setMission(mission);
			Mission firstDeploymentMission = dbUnits.get(current.getId()).getFirstDeploymentMission();
			currentObtainedUnit.setFirstDeploymentMission(firstDeploymentMission);
			currentObtainedUnit.setCount(current.getCount());
			currentObtainedUnit.setUser(user);
			currentObtainedUnit.setUnit(unitBo.findById(current.getId()));
			currentObtainedUnit.setSourcePlanet(firstDeploymentMission == null ? mission.getSourcePlanet()
					: firstDeploymentMission.getSourcePlanet());
			currentObtainedUnit.setTargetPlanet(mission.getTargetPlanet());
			obtainedUnits.add(currentObtainedUnit);
		});
		List<UnitType> involvedUnitTypes = obtainedUnits.stream().map(current -> current.getUnit().getType())
				.collect(Collectors.toList());
		if (!unitTypeBo.canDoMission(user, mission.getTargetPlanet(), involvedUnitTypes, missionType)) {
			throw new SgtBackendInvalidInputException(
					"At least one unit type doesn't support the specified mission.... don't try it dear hacker, you can't defeat the system, but don't worry nobody can");
		}
		checkCrossGalaxy(missionType, obtainedUnits, mission.getSourcePlanet(), mission.getTargetPlanet());
		obtainedUnitBo.save(obtainedUnits);
		if (obtainedUnits.stream().noneMatch(obtainedUnit -> obtainedUnit.getUnit().getSpeedImpactGroup() != null
				&& obtainedUnit.getUnit().getSpeedImpactGroup().getIsFixed())) {
			Optional<Double> lowestSpeedOptional = obtainedUnits.stream().map(ObtainedUnit::getUnit)
					.filter(unit -> unit.getSpeed() != null && unit.getSpeed() > 0.000D
							&& (unit.getSpeedImpactGroup() == null || unit.getSpeedImpactGroup().getIsFixed() == false))
					.map(Unit::getSpeed).reduce((a, b) -> a > b ? b : a);
			if (lowestSpeedOptional.isPresent()) {
				Double lowestSpeed = lowestSpeedOptional.get() / 70;
				Double missionTypeTime = calculateRequiredTime(missionType);
				long moveCost = calculateMoveCost(mission.getSourcePlanet(), mission.getTargetPlanet());
				Double withMoveCost = missionTypeTime + (missionTypeTime * (moveCost * 0.01));
				mission.setRequiredTime(withMoveCost / lowestSpeed);
				mission.setTerminationDate(computeTerminationDate(mission.getRequiredTime()));
			}
		}
		save(mission);
		scheduleMission(mission);
		UnitRunningMissionDto retVal = new UnitRunningMissionDto(mission, obtainedUnits);
		retVal.setMissionsCount(countUserMissions(userId));
		emitLocalMissionChange(mission);
		TransactionUtil.doAfterCommit(() -> socketIoService.sendMessage(userId, UNIT_OBTAINED_CHANGE,
				() -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(userId))));
		return retVal;
	}

	/**
	 * Will check if the input DTO is valid, the following validations will be done
	 * <br>
	 * <b>IMPORTANT:</b> This method is intended to be use as part of the mission
	 * registration process
	 * <ul>
	 * <li>Check if the user exists</li>
	 * <li>Check if the sourcePlanet exists</li>
	 * <li>Check if the targetPlanet exists</li>
	 * <li>Check for each selected unit if there is an associated obtainedUnit and
	 * if count is valid</li>
	 * <li>removes DEPLOYED mission if required</li>
	 * </ul>
	 *
	 * @param missionInformation
	 * @return Database list of <i>ObtainedUnit</i> with the subtraction <b>already
	 *         applied</b>, whose key is the "unit" id (don't confuse with obtained
	 *         unit id)
	 *
	 * @throws SgtBackendInvalidInputException when validation was not passed
	 * @throws UserNotFoundException           When user doesn't exists <b>(in this
	 *                                         universe)</b>
	 * @throws PlanetNotFoundException         When the planet doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private Map<Integer, ObtainedUnit> checkAndLoadObtainedUnits(UnitMissionInformation missionInformation) {
		Map<Integer, ObtainedUnit> retVal = new HashMap<>();
		Integer userId = missionInformation.getUserId();
		Long sourcePlanetId = missionInformation.getSourcePlanetId();
		checkUserExists(userId);
		checkPlanetExists(sourcePlanetId);
		checkPlanetExists(missionInformation.getTargetPlanetId());
		checkDeployedAllowed(missionInformation.getMissionType());
		Set<Mission> deletedMissions = new HashSet<>();
		if (CollectionUtils.isEmpty(missionInformation.getInvolvedUnits())) {
			throw new SgtBackendInvalidInputException("involvedUnits can't be empty");
		}
		missionInformation.getInvolvedUnits().forEach(current -> {
			if (current.getCount() == null) {
				throw new SgtBackendInvalidInputException("No count was specified for unit " + current.getId());
			}
			ObtainedUnit currentObtainedUnit = findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(
					missionInformation.getUserId(), current.getId(), sourcePlanetId,
					!planetBo.isOfUserProperty(userId, sourcePlanetId));
			checkUnitCanDeploy(currentObtainedUnit, missionInformation);
			ObtainedUnit unitAfterSubstraction = obtainedUnitBo.saveWithSubtraction(currentObtainedUnit,
					current.getCount(), false);
			if (unitAfterSubstraction == null && currentObtainedUnit.getMission() != null
					&& currentObtainedUnit.getMission().getType().getCode().equals(MissionType.DEPLOYED.toString())) {
				deletedMissions.add(currentObtainedUnit.getMission());
			}
			retVal.put(current.getId(), currentObtainedUnit);
		});
		deletedMissions.forEach(this::resolveMission);
		return retVal;
	}

	/**
	 * Checks if the current obtained unit can do deploy (if already deployed in
	 * some cases, cannot)
	 *
	 * @param currentObtainedUnit
	 * @param missionType
	 * @since 0.7.4
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void checkUnitCanDeploy(ObtainedUnit currentObtainedUnit, UnitMissionInformation missionInformation) {
		MissionType unitMissionType = obtainedUnitBo.resolveMissionType(currentObtainedUnit);
		boolean isOfUserProperty = planetBo.isOfUserProperty(missionInformation.getUserId(),
				missionInformation.getTargetPlanetId());
		switch (configurationBo.findDeployMissionConfiguration()) {
		case ONLY_ONCE_RETURN_SOURCE:
		case ONLY_ONCE_RETURN_DEPLOYED:
			if (!isOfUserProperty && unitMissionType == MissionType.DEPLOYED
					&& missionInformation.getMissionType() == MissionType.DEPLOY) {
				throw new SgtBackendInvalidInputException("You can't do a deploy mission after a deploy mission");
			}
			break;
		default:
			break;
		}
	}

	/**
	 * Checks if the DEPLOY mission is allowed
	 *
	 * @param missionType
	 * @throws SgtBackendInvalidInputException If the deployment mission is
	 *                                         <b>globally</b> disabled
	 * @since 0.7.4
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void checkDeployedAllowed(MissionType missionType) {
		if (missionType == MissionType.DEPLOY
				&& configurationBo.findDeployMissionConfiguration().equals(DeployMissionConfigurationEnum.DISALLOWED)) {
			throw new SgtBackendInvalidInputException("The deployment mission is globally disabñed");
		}
	}

	/**
	 * Returns a copy of the object, used to make missionInformation immutable
	 *
	 * @param missionInformation
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private UnitMissionInformation copyMissionInformation(UnitMissionInformation missionInformation) {
		UnitMissionInformation retVal = new UnitMissionInformation();
		BeanUtils.copyProperties(missionInformation, retVal);
		return retVal;
	}

	/**
	 * Checks if the input Unit <i>id</i> exists, and returns the associated
	 * ObtainedUnit
	 *
	 * @param id
	 * @param isDeployedMission If true will search for a deployed obtained unit,
	 *                          else for an obtained unit with a <i>null<i> mission
	 * @return the expected obtained id
	 * @throws NotFoundException If obtainedUnit doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private ObtainedUnit findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(Integer userId, Integer unitId,
			Long planetId, boolean isDeployedMission) {
		ObtainedUnit retVal = isDeployedMission
				? obtainedUnitBo.findOneByUserIdAndUnitIdAndTargetPlanetAndMissionDeployed(userId, unitId, planetId)
				: obtainedUnitBo.findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIsNull(userId, unitId, planetId);

		if (retVal == null) {
			throw new NotFoundException("No obtainedUnit for unit with id " + unitId + " was found in planet "
					+ planetId + ", nice try, dirty hacker!");
		}
		return retVal;
	}

	/**
	 * Checks if the logged in user is the creator of the mission
	 *
	 * @param invoker The creator of the mission
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void checkInvokerIsTheLoggedUser(Integer invoker) {
		if (!invoker.equals(userStorageBo.findLoggedIn().getId())) {
			throw new SgtBackendInvalidInputException("Invoker is not the logged in user");
		}
	}

	/**
	 * Prepares a mission to be scheduled
	 *
	 * @param missionInformation
	 * @param type
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private Mission prepareMission(UnitMissionInformation missionInformation, MissionType type) {
		Mission retVal = new Mission();
		Double requiredTime = calculateRequiredTime(type);
		retVal.setMissionInformation(null);
		retVal.setType(findMissionType(type));
		retVal.setUser(userStorageBo.findById(missionInformation.getUserId()));
		retVal.setRequiredTime(requiredTime);
		Long sourcePlanetId = missionInformation.getSourcePlanetId();
		Long targetPlanetId = missionInformation.getTargetPlanetId();
		if (sourcePlanetId != null) {
			retVal.setSourcePlanet(planetBo.findLockedById(sourcePlanetId));
		}
		if (targetPlanetId != null) {
			retVal.setTargetPlanet(planetBo.findLockedById(targetPlanetId));
		}

		retVal.setTerminationDate(computeTerminationDate(requiredTime));
		return retVal;
	}

	/**
	 * Calculates time required to complete the mission
	 *
	 *
	 * @param type
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private Double calculateRequiredTime(MissionType type) {
		return (double) configurationBo.findMissionBaseTimeByType(type);
	}

	/**
	 * Emits a local mission change to the target user
	 *
	 * @param mission
	 * @param transactionAffected When specified, will search in the result of find
	 *                            user missions one with the same id, and replace it
	 *                            with that <br>
	 *                            This is require because, entity relations may not
	 *                            has been populated, as transaction is not done
	 * @param user
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void emitLocalMissionChange(Mission mission) {
		UserStorage user = mission.getUser();
		TransactionUtil.doAfterCommit(() -> {
			entityManager.refresh(mission);
			emitEnemyMissionsChange(mission);
			socketIoService.sendMessage(user, "unit_mission_change",
					() -> new MissionWebsocketMessage(countUserMissions(user.getId()),
							findUserRunningMissions(user.getId())));
		});
	}

	private void emitEnemyMissionsChange(Mission mission) {
		UserStorage targetPlanetOwner = mission.getTargetPlanet().getOwner();
		if (targetPlanetOwner != null && !targetPlanetOwner.getId().equals(mission.getUser().getId())) {
			emitEnemyMissionsChange(targetPlanetOwner);
		}
	}

	private void emitEnemyMissionsChange(UserStorage user) {
		socketIoService.sendMessage(user, ENEMY_MISSION_CHANGE, () -> findEnemyRunningMissions(user));

	}

	private AttackInformation buildAttackInformation(Planet targetPlanet, Mission attackMission) {
		AttackInformation retVal = new AttackInformation(attackMission);
		obtainedUnitBo.findInvolvedInAttack(targetPlanet).forEach(retVal::addUnit);
		return retVal;
	}

	/**
	 * Defines the new owner for the targetPlanet
	 *
	 * @param owner         The new owner
	 * @param involvedUnits The units used by the owner to conquest the planet
	 * @param targetPlanet
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void definePlanetAsOwnedBy(UserStorage owner, List<ObtainedUnit> involvedUnits, Planet targetPlanet) {
		targetPlanet.setOwner(owner);
		involvedUnits.forEach(current -> {
			current.setSourcePlanet(targetPlanet);
			current.setTargetPlanet(null);
			current.setMission(null);
		});
		if (targetPlanet.getSpecialLocation() != null) {
			requirementBo.triggerSpecialLocation(owner, targetPlanet.getSpecialLocation());
		}

		planetBo.emitPlanetOwnedChange(owner);
		socketIoService.sendMessage(owner, UNIT_OBTAINED_CHANGE,
				() -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(owner.getId())));
	}

	private long calculateMoveCost(Planet sourcePlanet, Planet targetPlanet) {
		long quadrants = Math.abs(sourcePlanet.getQuadrant() - targetPlanet.getQuadrant());
		long sectors = Math.abs(sourcePlanet.getSector() - targetPlanet.getSector());
		boolean isDifferentGalaxy = sourcePlanet.getGalaxy().equals(targetPlanet.getGalaxy());
		return (quadrants + (sectors * 2) + (isDifferentGalaxy ? 10 : 0));
	}

	private void checkCrossGalaxy(MissionType missionType, List<ObtainedUnit> units, Planet sourcePlanet,
			Planet targetPlanet) {
		UserStorage user = units.get(0).getUser();
		if (!sourcePlanet.getGalaxy().getId().equals(targetPlanet.getGalaxy().getId())) {
			units.forEach(unit -> {
				SpeedImpactGroup speedGroup = unit.getUnit().getSpeedImpactGroup();
				speedGroup = speedGroup == null ? unit.getUnit().getType().getSpeedImpactGroup() : speedGroup;
				if (speedGroup != null) {
					if (!canDoMission(user, targetPlanet, speedGroup, missionType)) {
						throw new SgtBackendInvalidInputException(
								"This speed group doesn't support this mission outside of the galaxy");
					}
					ObjectRelation relation = objectRelationBo
							.findOneByObjectTypeAndReferenceId(ObjectEnum.SPEED_IMPACT_GROUP, speedGroup.getId());
					if (relation == null) {
						LOG.warn("Unexpected null objectRelation for SPEED_IMPACT_GROUP with id " + speedGroup.getId());
					} else if (!unlockedRelationBo.isUnlocked(user, relation)) {
						throw new SgtBackendInvalidInputException(
								"Don't try it.... you can't do cross galaxy missions, and you know it");
					}
				}
			});
		}
	}
}
