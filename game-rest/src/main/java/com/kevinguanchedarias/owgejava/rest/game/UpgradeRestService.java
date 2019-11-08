package com.kevinguanchedarias.owgejava.rest.game;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.ObtainedUpgradeBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.ObtainedUpgradeDto;
import com.kevinguanchedarias.owgejava.dto.RunningUpgradeDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;

@RestController
@RequestMapping("game/upgrade")
@ApplicationScope
public class UpgradeRestService {

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private ObtainedUpgradeBo obtainedUpgradeBo;

	@Autowired
	private MissionBo missionBo;

	@RequestMapping(value = "findObtained", method = RequestMethod.GET)
	public Object findObtained() {
		List<ObtainedUpgrade> obtainedUpgradeList = obtainedUpgradeBo.findByUser(userStorageBo.findLoggedIn().getId());
		List<ObtainedUpgradeDto> obtainedUpgradeDtoList = new ArrayList<>();

		for (ObtainedUpgrade current : obtainedUpgradeList) {
			ObtainedUpgradeDto currentDto = new ObtainedUpgradeDto();
			currentDto.dtoFromEntity(current);
			obtainedUpgradeDtoList.add(currentDto);
		}

		return obtainedUpgradeDtoList;
	}

	/**
	 * Finds one single obtained unit by user and upgrade id
	 * 
	 * @param id
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("findObtained/{id}")
	public ObtainedUpgradeDto findObtanedById(@PathVariable Integer id) {
		return obtainedUpgradeBo
				.toDto(obtainedUpgradeBo.findByUserAndUpgrade(userStorageBo.findLoggedIn().getId(), id));
	}

	@RequestMapping(value = "findRunningUpgrade", method = RequestMethod.GET)
	public Object findRunningUpgrade() {
		RunningUpgradeDto retVal = missionBo.findRunningLevelUpMission(userStorageBo.findLoggedIn().getId());

		if (retVal == null) {
			return "";
		}

		return retVal;
	}

	@RequestMapping(value = "registerLevelUp", method = RequestMethod.GET)
	public Object registerLevelUp(@RequestParam("upgradeId") Integer upgradeId) {
		Integer userId = userStorageBo.findLoggedIn().getId();
		missionBo.registerLevelUpAnUpgrade(userId, upgradeId);
		return missionBo.findRunningLevelUpMission(userId);
	}

	@RequestMapping(value = "cancelUpgrade", method = RequestMethod.GET)
	public Object cancelUpgrade() {
		missionBo.cancelUpgradeMission(userStorageBo.findLoggedIn().getId());
		return "{}";
	}
}