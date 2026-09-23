package com.pavitraristaa.trust.service;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.auth.repository.UserAccountRepository;
import com.pavitraristaa.auth.service.AuthService;
import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.common.security.AuthenticatedUser;
import com.pavitraristaa.common.util.PaginationSupport;
import com.pavitraristaa.profile.dto.UserSummaryResponse;
import com.pavitraristaa.profile.mapper.UserSummaryMapper;
import com.pavitraristaa.profile.repository.UserProfileRepository;
import com.pavitraristaa.trust.entity.Block;
import com.pavitraristaa.trust.repository.BlockRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlockService {

    private final AuthService authService;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final BlockRepository blockRepository;
    private final UserSummaryMapper userSummaryMapper;

    public BlockService(
            AuthService authService,
            UserAccountRepository userAccountRepository,
            UserProfileRepository userProfileRepository,
            BlockRepository blockRepository,
            UserSummaryMapper userSummaryMapper
    ) {
        this.authService = authService;
        this.userAccountRepository = userAccountRepository;
        this.userProfileRepository = userProfileRepository;
        this.blockRepository = blockRepository;
        this.userSummaryMapper = userSummaryMapper;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listMine(AuthenticatedUser principal, Integer page, Integer size) {
        UserAccount self = authService.requireUsable(principal);
        return blockRepository.findByBlockerOrderByCreatedAtDesc(self, PaginationSupport.pageable(page, size))
                .map(Block::getBlocked)
                .map(blocked -> userProfileRepository.findByUserAndDeletedFalse(blocked)
                        .map(userSummaryMapper::toSummary)
                        .orElse(null))
                .filter(summary -> summary != null)
                .toList();
    }

    @Transactional
    public void block(AuthenticatedUser principal, UUID userId) {
        UserAccount self = authService.requireUsable(principal);
        if (self.getUuid().equals(userId)) {
            throw new ApiException(ErrorCode.CANNOT_INTERACT_WITH_SELF, "You cannot block yourself");
        }
        UserAccount target = userAccountRepository.findByUuid(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        if (blockRepository.findByBlockerAndBlocked(self, target).isPresent()) {
            return;
        }
        Block block = new Block();
        block.setBlocker(self);
        block.setBlocked(target);
        block.setCreatedAt(Instant.now());
        blockRepository.save(block);
    }

    @Transactional
    public void unblock(AuthenticatedUser principal, UUID userId) {
        UserAccount self = authService.requireUsable(principal);
        UserAccount target = userAccountRepository.findByUuid(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        blockRepository.findByBlockerAndBlocked(self, target).ifPresent(blockRepository::delete);
    }
}
