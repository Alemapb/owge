package com.kevinguanchedarias.sgtjava.pojo;

import java.util.List;

import com.kevinguanchedarias.sgtjava.dto.UnitDto;

public class UnitWithRequirementInformation {
	private UnitDto unit;
	private List<UnitUpgradeRequirements> requirements;

	public UnitDto getUnit() {
		return unit;
	}

	public void setUnit(UnitDto unit) {
		this.unit = unit;
	}

	public List<UnitUpgradeRequirements> getRequirements() {
		return requirements;
	}

	public void setRequirements(List<UnitUpgradeRequirements> requirements) {
		this.requirements = requirements;
	}

}
