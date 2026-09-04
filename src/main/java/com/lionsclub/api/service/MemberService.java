package com.lionsclub.api.service;

import com.lionsclub.api.domain.member.Member;
import com.lionsclub.api.domain.user.DisplayNames;
import com.lionsclub.api.infrastructure.persistence.MemberRepository;
import com.lionsclub.api.web.dto.MemberResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;

    public List<MemberResponse> listAll() {
        return memberRepository.findAllByOrderByJoinedAtDesc().stream()
                .map(MemberService::toResponse)
                .toList();
    }

    public MemberResponse findById(UUID id) {
        return memberRepository.findById(id).map(MemberService::toResponse).orElse(null);
    }

    @Transactional
    public MemberResponse create(MemberInput input, MultipartFile avatarFile) {
        var member = new Member();
        apply(member, input, resolveAvatar(null, input.avatarUrl(), avatarFile));
        return toResponse(memberRepository.save(member));
    }

    @Transactional
    public MemberResponse update(UUID id, MemberInput input, MultipartFile avatarFile) {
        var member = memberRepository.findById(id).orElse(null);
        if (member == null) {
            return null;
        }
        apply(member, input, resolveAvatar(member.getAvatarUrl(), input.avatarUrl(), avatarFile));
        return toResponse(memberRepository.save(member));
    }

    @Transactional
    public boolean delete(UUID id) {
        if (!memberRepository.existsById(id)) {
            return false;
        }
        memberRepository.deleteById(id);
        return true;
    }

    private void apply(Member member, MemberInput input, String avatarUrl) {
        member.setFirstName(DisplayNames.firstNameOf(input.name()));
        member.setLastName(DisplayNames.lastNameOf(input.name()));
        member.setRole(input.role());
        member.setBio(emptyToNull(input.bio()));
        member.setEmail(emptyToNull(input.email()));
        member.setPhone(emptyToNull(input.phone()));
        member.setSocialLinkedin(emptyToNull(input.linkedin()));
        member.setSocialFacebook(emptyToNull(input.facebook()));
        member.setSocialInstagram(emptyToNull(input.instagram()));
        member.setAvatarUrl(avatarUrl);
    }

    private String resolveAvatar(String current, String avatarUrl, MultipartFile avatarFile) {
        if (avatarFile != null && !avatarFile.isEmpty()) {
            return fileStorageService.store("members", avatarFile);
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            return avatarUrl;
        }
        return current;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public static MemberResponse toResponse(Member member) {
        MemberResponse.MemberSocials socials = null;
        if (member.getSocialLinkedin() != null
                || member.getSocialFacebook() != null
                || member.getSocialInstagram() != null) {
            socials = new MemberResponse.MemberSocials(
                    member.getSocialLinkedin(), member.getSocialFacebook(), member.getSocialInstagram());
        }
        return new MemberResponse(
                member.getId(),
                DisplayNames.displayName(member.getFirstName(), member.getLastName()),
                member.getRole(),
                member.getAvatarUrl(),
                member.getBio(),
                member.getJoinedAt(),
                member.getEmail(),
                member.getPhone(),
                socials);
    }

    public record MemberInput(
            String name,
            String role,
            String bio,
            String email,
            String phone,
            String linkedin,
            String facebook,
            String instagram,
            String avatarUrl
    ) {}
}
