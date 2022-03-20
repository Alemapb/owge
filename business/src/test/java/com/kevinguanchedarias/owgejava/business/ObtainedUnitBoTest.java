package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.MissionFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.test.abstracts.AbstractBaseBoTest;
import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import javax.persistence.EntityManager;
import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.EXPLORE_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenDeployedMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.OBTAINED_UNIT_1_COUNT;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.TARGET_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_NAME;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = ObtainedUnitBo.class
)
@MockBean({
        ObtainedUnitRepository.class,
        UserStorageBo.class,
        PlanetBo.class,
        UnitTypeBo.class,
        ImprovementBo.class,
        SocketIoService.class,
        AsyncRunnerBo.class,
        EntityManager.class,
        RequirementBo.class,
        MissionFinderBo.class,
        TaggableCacheManager.class,
        HiddenUnitBo.class
})
class ObtainedUnitBoTest extends AbstractBaseBoTest {
    private final ObtainedUnitBo obtainedUnitBo;
    private final EntityManager entityManager;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final PlanetBo planetBo;
    private final MissionFinderBo missionFinderBo;
    private final TaggableCacheManager taggableCacheManager;
    private final HiddenUnitBo hiddenUnitBo;

    @Autowired
    ObtainedUnitBoTest(
            ObtainedUnitBo obtainedUnitBo,
            EntityManager entityManager,
            ObtainedUnitRepository obtainedUnitRepository,
            PlanetBo planetBo,
            MissionFinderBo missionFinderBo,
            TaggableCacheManager taggableCacheManager,
            HiddenUnitBo hiddenUnitBo
    ) {
        // Some methods has not all branches covered, only touched lines
        this.obtainedUnitBo = obtainedUnitBo;
        this.entityManager = entityManager;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.planetBo = planetBo;
        this.missionFinderBo = missionFinderBo;
        this.taggableCacheManager = taggableCacheManager;
        this.hiddenUnitBo = hiddenUnitBo;
    }

    @Test
    void trySave_should_work_and_refresh_entity_if_missing_from_orm(CapturedOutput capturedOutput) {
        var ou = givenObtainedUnit1();
        long sumValue = 19;
        given(entityManager.contains(ou)).willReturn(true);
        given(obtainedUnitRepository.saveAndFlush(ou)).willAnswer(returnsFirstArg());

        var saved = obtainedUnitBo.trySave(ou, sumValue);

        verify(obtainedUnitRepository, times(1)).saveAndFlush(ou);
        assertThat(saved).isSameAs(ou);
        assertThat(capturedOutput.getOut()).doesNotContain("OMG THE PROGRAMMER IS GOING CRAZY");
    }

    @Test
    void trySave_should_work_but_cry_because_not_able_to_refresh_entity(CapturedOutput capturedOutput) {
        var ou = givenObtainedUnit1();
        long sumValue = 19;
        given(obtainedUnitRepository.saveAndFlush(ou)).willAnswer(returnsFirstArg());

        var saved = obtainedUnitBo.trySave(ou, sumValue);

        verify(obtainedUnitRepository, times(1)).saveAndFlush(ou);
        assertThat(saved).isSameAs(ou);
        assertThat(capturedOutput.getOut()).contains("OMG THE PROGRAMMER IS GOING CRAZY");
    }

    @Test
    void moveUnit_should_do_only_save_when_unit_is_part_of_DEPLOYED_mission() {
        var ou = givenObtainedUnit1();
        ou.setTargetPlanet(null);
        ou.setMission(givenDeployedMission());
        given(obtainedUnitRepository.save(ou)).willAnswer(returnsFirstArg());
        given(planetBo.findById(TARGET_PLANET_ID)).willReturn(givenTargetPlanet());

        var result = obtainedUnitBo.moveUnit(ou, USER_ID_1, TARGET_PLANET_ID);

        verify(planetBo, times(1)).isOfUserProperty(anyInt(), anyLong());
        verify(obtainedUnitRepository, times(1)).save(ou);
        assertThat(ou.getTargetPlanet()).isEqualTo(givenTargetPlanet());
        assertThat(result).isSameAs(ou);
    }

    @Test
    void moveUnit_should_assign_a_target_planet() {
        var ou = givenObtainedUnit1();
        var deployedMission = givenDeployedMission();
        ou.setTargetPlanet(null);
        ou.setMission(null);
        given(obtainedUnitRepository.save(ou)).willAnswer(returnsFirstArg());
        given(planetBo.findById(TARGET_PLANET_ID)).willReturn(givenTargetPlanet());
        given(missionFinderBo.findDeployedMissionOrCreate(ou)).willReturn(deployedMission);

        var result = obtainedUnitBo.moveUnit(ou, USER_ID_1, TARGET_PLANET_ID);

        verify(planetBo, times(2)).isOfUserProperty(USER_ID_1, TARGET_PLANET_ID);
        verify(obtainedUnitRepository, times(2)).save(ou);
        assertThat(ou.getTargetPlanet()).isEqualTo(givenTargetPlanet());
        assertThat(result.getMission()).isEqualTo(deployedMission);
        assertThat(result).isSameAs(ou);
    }

    @Test
    void explorePlanetUnits_should_work() {
        var ou = givenObtainedUnit1();
        var invisibleOu = givenObtainedUnit2();
        invisibleOu.setUser(ou.getUser());
        invisibleOu.getUnit().setIsInvisible(true);
        var mission = givenExploreMission();
        var planet = mission.getTargetPlanet();
        given(obtainedUnitRepository.findByExplorePlanet(EXPLORE_MISSION_ID, TARGET_PLANET_ID)).willReturn(List.of(ou, invisibleOu));

        var result = obtainedUnitBo.explorePlanetUnits(mission, planet);
        verify(hiddenUnitBo, times(1)).defineHidden(List.of(ou, invisibleOu), result);
        assertThat(result).hasSize(2);
        var visibleResult = result.get(0);
        var invisibleResult = result.get(1);
        assertThat(visibleResult.getUnit().getName()).isEqualTo(UNIT_NAME);
        assertThat(visibleResult.getCount()).isEqualTo(OBTAINED_UNIT_1_COUNT);
        assertThat(invisibleResult).isNotNull();
        assertThat(invisibleResult.getCount()).isNull();
        assertThat(invisibleResult.getUnit()).isNull();

    }

    @Override
    public CacheTagTestModel findCacheTagInfo() {
        return CacheTagTestModel.builder()
                .tag(ObtainedUnitBo.OBTAINED_UNIT_CACHE_TAG)
                .targetBo(obtainedUnitBo)
                .taggableCacheManager(taggableCacheManager)
                .build();
    }
}
