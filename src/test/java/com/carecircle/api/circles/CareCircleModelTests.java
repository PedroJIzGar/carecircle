package com.carecircle.api.circles;

import com.carecircle.api.circles.entity.CareCircle;
import com.carecircle.api.circles.entity.CareCircleStatus;
import com.carecircle.api.circles.repository.CareCircleRepository;
import com.carecircle.api.elderprofiles.entity.ElderProfile;
import com.carecircle.api.elderprofiles.repository.ElderProfileRepository;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleMemberStatus;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CareCircleModelTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CareCircleRepository careCircleRepository;

    @Autowired
    private ElderProfileRepository elderProfileRepository;

    @Autowired
    private CircleMemberRepository circleMemberRepository;

    @Test
    void persistsCareCircleWithElderProfileAndMainCaregiver() {
        User user = userRepository.save(new User(
                UUID.randomUUID().toString(),
                "carecircle-" + UUID.randomUUID() + "@example.com"
        ));

        CareCircle circle = careCircleRepository.save(new CareCircle("Family care circle", user));
        ElderProfile elderProfile = elderProfileRepository.save(new ElderProfile(circle, "Maria Garcia"));
        CircleMember member = circleMemberRepository.save(new CircleMember(circle, user, CircleRole.MAIN_CAREGIVER));

        assertThat(circle.getId()).isNotNull();
        assertThat(circle.getStatus()).isEqualTo(CareCircleStatus.ACTIVE);
        assertThat(circle.getCreatedAt()).isNotNull();
        assertThat(elderProfile.getId()).isNotNull();
        assertThat(elderProfileRepository.findByCareCircle_Id(circle.getId())).contains(elderProfile);
        assertThat(member.getId()).isNotNull();
        assertThat(member.getStatus()).isEqualTo(CircleMemberStatus.ACTIVE);
        assertThat(member.getJoinedAt()).isNotNull();
        assertThat(circleMemberRepository.findByCareCircle_IdAndUser_Id(circle.getId(), user.getId())).contains(member);
    }
}
