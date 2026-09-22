package com.pavitraristaa.favorites.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.favorites.entity.Favorite;
import com.pavitraristaa.favorites.repository.FavoriteRepository;
import com.pavitraristaa.profile.dto.UserSummaryResponse;
import com.pavitraristaa.profile.entity.ProfileStatus;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.profile.mapper.UserSummaryMapper;
import com.pavitraristaa.profile.repository.UserProfileRepository;
import com.pavitraristaa.trust.repository.BlockRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {

    private final AuthService authService;
    private final UserProfileRepository userProfileRepository;
    private final FavoriteRepository favoriteRepository;
    private final BlockRepository blockRepository;
    private final UserSummaryMapper userSummaryMapper;

    public FavoriteService(
            AuthService authService,
            UserProfileRepository userProfileRepository,
            FavoriteRepository favoriteRepository,
            BlockRepository blockRepository,
            UserSummaryMapper userSummaryMapper
    ) {
        this.authService = authService;
        this.userProfileRepository = userProfileRepository;
        this.favoriteRepository = favoriteRepository;
        this.blockRepository = blockRepository;
        this.userSummaryMapper = userSummaryMapper;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listMine(AuthenticatedUser principal, Integer page, Integer size) {
        UserAccount user = authService.requireUsable(principal);
        return favoriteRepository.findByUserOrderByCreatedAtDesc(user, PaginationSupport.pageable(page, size))
                .map(favorite -> userSummaryMapper.toSummary(favorite.getProfile()))
                .getContent();
    }

    @Transactional
    public void add(AuthenticatedUser principal, UUID profileId) {
        UserAccount user = authService.requireUsable(principal);
        UserProfile target = requireDiscoverable(user, profileId);
        if (favoriteRepository.existsByUserAndProfile(user, target)) {
            return;
        }
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setProfile(target);
        favorite.setCreatedAt(Instant.now());
        favoriteRepository.save(favorite);
    }

    @Transactional
    public void remove(AuthenticatedUser principal, UUID profileId) {
        UserAccount user = authService.requireUsable(principal);
        UserProfile target = userProfileRepository.findByUser_UuidAndDeletedFalse(profileId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found"));
        favoriteRepository.findByUserAndProfile(user, target).ifPresent(favoriteRepository::delete);
    }

    private UserProfile requireDiscoverable(UserAccount user, UUID profileId) {
        if (user.getUuid().equals(profileId)) {
            throw new ApiException(ErrorCode.CANNOT_INTERACT_WITH_SELF, "You cannot favorite your own profile");
        }
        UserProfile target = userProfileRepository.findByUser_UuidAndDeletedFalse(profileId)
                .filter(profile -> profile.getProfileStatus() == ProfileStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found"));
        if (blockRepository.existsEitherDirection(user, target.getUser())) {
            throw new ApiException(ErrorCode.USER_BLOCKED, "You cannot interact with this user");
        }
        return target;
    }

}
