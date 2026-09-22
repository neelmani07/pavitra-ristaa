package com.pavitraristaa.relationship.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.relationship.dto.RelationshipModeResponse;
import com.pavitraristaa.relationship.dto.ReplaceRelationshipModesRequest;
import com.pavitraristaa.relationship.entity.RelationshipMode;
import com.pavitraristaa.relationship.entity.UserRelationshipMode;
import com.pavitraristaa.relationship.repository.RelationshipModeRepository;
import com.pavitraristaa.relationship.repository.UserRelationshipModeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelationshipModeService {

    private final RelationshipModeRepository relationshipModeRepository;
    private final UserRelationshipModeRepository userRelationshipModeRepository;
    private final AuthService authService;

    public RelationshipModeService(
            RelationshipModeRepository relationshipModeRepository,
            UserRelationshipModeRepository userRelationshipModeRepository,
            AuthService authService
    ) {
        this.relationshipModeRepository = relationshipModeRepository;
        this.userRelationshipModeRepository = userRelationshipModeRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<RelationshipModeResponse> listActive() {
        return relationshipModeRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RelationshipModeResponse> listMine(AuthenticatedUser principal) {
        UserAccount user = authService.requireUsable(principal);
        return userRelationshipModeRepository.findByUser(user).stream()
                .map(UserRelationshipMode::getRelationshipMode)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<RelationshipModeResponse> replaceMine(AuthenticatedUser principal, ReplaceRelationshipModesRequest request) {
        UserAccount user = authService.requireUsable(principal);
        List<String> codes = request.modeCodes() == null ? List.of() : request.modeCodes();
        List<String> distinct = new ArrayList<>(new LinkedHashSet<>(
                codes.stream().map(code -> code == null ? "" : code.trim().toUpperCase()).filter(code -> !code.isBlank()).toList()
        ));
        Map<String, RelationshipMode> byCode = relationshipModeRepository.findByCodeInAndActiveTrue(distinct).stream()
                .collect(Collectors.toMap(mode -> mode.getCode().toUpperCase(), Function.identity()));
        if (byCode.size() != distinct.size()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "One or more relationship modes are invalid",
                    Map.of("modeCodes", distinct)
            );
        }
        userRelationshipModeRepository.deleteByUser(user);
        userRelationshipModeRepository.flush();
        Instant now = Instant.now();
        List<UserRelationshipMode> saved = new ArrayList<>();
        for (String code : distinct) {
            UserRelationshipMode assignment = new UserRelationshipMode();
            assignment.setUser(user);
            assignment.setRelationshipMode(byCode.get(code));
            assignment.setCreatedAt(now);
            saved.add(userRelationshipModeRepository.save(assignment));
        }
        return saved.stream()
                .map(UserRelationshipMode::getRelationshipMode)
                .map(this::toResponse)
                .toList();
    }

    private RelationshipModeResponse toResponse(RelationshipMode mode) {
        return new RelationshipModeResponse(mode.getCode(), mode.getName(), mode.getDescription(), mode.getDisplayOrder());
    }
}
