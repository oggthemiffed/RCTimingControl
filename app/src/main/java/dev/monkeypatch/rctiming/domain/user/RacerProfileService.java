package dev.monkeypatch.rctiming.domain.user;

import dev.monkeypatch.rctiming.api.racer.dto.ClassRatingDto;
import dev.monkeypatch.rctiming.api.racer.dto.MembershipDto;
import dev.monkeypatch.rctiming.api.racer.dto.RacerProfileDto;
import dev.monkeypatch.rctiming.api.racer.dto.UpdateRacerProfileRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class RacerProfileService {

    private final UserRepository userRepository;
    private final UserGoverningBodyMembershipRepository membershipRepository;
    private final UserClassRatingRepository classRatingRepository;

    public RacerProfileService(UserRepository userRepository,
                                UserGoverningBodyMembershipRepository membershipRepository,
                                UserClassRatingRepository classRatingRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.classRatingRepository = classRatingRepository;
    }

    @Transactional(readOnly = true)
    public RacerProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        List<MembershipDto> memberships = membershipRepository.findByUserId(userId).stream()
                .map(MembershipDto::from)
                .toList();
        List<ClassRatingDto> classRatings = classRatingRepository.findByUserId(userId).stream()
                .map(ClassRatingDto::from)
                .toList();
        return toDto(user, memberships, classRatings);
    }

    public RacerProfileDto updateProfile(Long userId, UpdateRacerProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        if (req.firstName() != null) user.setFirstName(req.firstName());
        if (req.lastName() != null) user.setLastName(req.lastName());
        if (req.phoneNumber() != null) user.setPhoneNumber(req.phoneNumber());
        if (req.emergencyContactName() != null) user.setEmergencyContactName(req.emergencyContactName());
        if (req.emergencyContactPhone() != null) user.setEmergencyContactPhone(req.emergencyContactPhone());
        if (req.phoneticName() != null) user.setPhoneticName(req.phoneticName());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        List<MembershipDto> memberships = membershipRepository.findByUserId(userId).stream()
                .map(MembershipDto::from)
                .toList();
        List<ClassRatingDto> classRatings = classRatingRepository.findByUserId(userId).stream()
                .map(ClassRatingDto::from)
                .toList();
        return toDto(user, memberships, classRatings);
    }

    @Transactional(readOnly = true)
    public List<MembershipDto> listMemberships(Long userId) {
        return membershipRepository.findByUserId(userId).stream()
                .map(MembershipDto::from)
                .toList();
    }

    public MembershipDto addMembership(Long userId, String code, String number) {
        UserGoverningBodyMembership membership = new UserGoverningBodyMembership();
        membership.setUserId(userId);
        membership.setGoverningBodyCode(code);
        membership.setMembershipNumber(number);
        Instant now = Instant.now();
        membership.setCreatedAt(now);
        membership.setUpdatedAt(now);
        // DataIntegrityViolationException propagates on duplicate (user_id, governing_body_code)
        return MembershipDto.from(membershipRepository.save(membership));
    }

    public MembershipDto updateMembership(Long userId, String code, String newNumber) {
        UserGoverningBodyMembership membership = membershipRepository
                .findByUserIdAndGoverningBodyCode(userId, code)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Membership not found for user " + userId + " and code " + code));
        membership.setMembershipNumber(newNumber);
        membership.setUpdatedAt(Instant.now());
        return MembershipDto.from(membershipRepository.save(membership));
    }

    public void removeMembership(Long userId, String code) {
        membershipRepository.findByUserIdAndGoverningBodyCode(userId, code)
                .ifPresent(membershipRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<ClassRatingDto> getClassRatings(Long userId) {
        return classRatingRepository.findByUserId(userId).stream()
                .map(ClassRatingDto::from)
                .toList();
    }

    private RacerProfileDto toDto(User user, List<MembershipDto> memberships, List<ClassRatingDto> classRatings) {
        return new RacerProfileDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getEmergencyContactName(),
                user.getEmergencyContactPhone(),
                user.getPhoneticName(),
                memberships,
                classRatings
        );
    }
}
