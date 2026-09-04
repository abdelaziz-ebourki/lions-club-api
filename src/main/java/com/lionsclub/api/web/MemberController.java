package com.lionsclub.api.web;

import com.lionsclub.api.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private static final String ERROR_FIELD = "error";

    private final MemberService memberService;

    @Operation(summary = "List members", description = "Returns all members ordered by join date, newest first.")
    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(memberService.listAll());
    }

    @Operation(summary = "Get member by id", description = "Returns a single member by id.")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        var member = memberService.findById(id);
        if (member == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(member);
    }

    @Operation(summary = "Create member",
            description = "Creates a member from multipart form fields (name, role, bio, email, phone, socials, avatar file or URL). Admin only.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(MultipartHttpServletRequest request) {
        var input = parse(request);
        if (input == null) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, "Name (min 2 chars) and role (min 2 chars) are required"));
        }
        try {
            var created = memberService.create(input, request.getFile("avatar"));
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, e.getMessage()));
        }
    }

    @Operation(summary = "Update member",
            description = "Updates a member from multipart form fields. Omitted fields keep their values. Admin only.")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(@PathVariable UUID id, MultipartHttpServletRequest request) {
        var input = parse(request);
        if (input == null) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, "Name (min 2 chars) and role (min 2 chars) are required"));
        }
        try {
            var updated = memberService.update(id, input, request.getFile("avatar"));
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, e.getMessage()));
        }
    }

    @Operation(summary = "Delete member", description = "Deletes a member by id. Admin only.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        if (!memberService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    private MemberService.MemberInput parse(MultipartHttpServletRequest request) {
        String name = trim(request.getParameter("name"));
        String role = trim(request.getParameter("role"));
        String bio = trim(request.getParameter("bio"));
        if (name == null || name.length() < 2 || name.length() > 100
                || role == null || role.length() < 2 || role.length() > 100
                || (bio != null && bio.length() > 500)) {
            return null;
        }
        return new MemberService.MemberInput(
                name,
                role,
                bio,
                trim(request.getParameter("email")),
                trim(request.getParameter("phone")),
                trim(request.getParameter("socials.linkedin")),
                trim(request.getParameter("socials.facebook")),
                trim(request.getParameter("socials.instagram")),
                trim(request.getParameter("avatar")));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
