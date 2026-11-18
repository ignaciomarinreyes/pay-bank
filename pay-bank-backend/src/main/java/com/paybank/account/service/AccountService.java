package com.paybank.account.service;

import com.paybank.account.mapper.MovementMapper;
import com.paybank.account.repository.MovementRepository;
import com.paybank.account.repository.MovementRow;
import com.paybank.common.web.PageQuery;
import com.paybank.generated.model.MovementPageResponse;
import com.paybank.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final MovementRepository movementRepository;
    private final MovementMapper movementMapper;
    private final CurrentUserProvider currentUserProvider;

    public AccountService(MovementRepository movementRepository, MovementMapper movementMapper,
            CurrentUserProvider currentUserProvider) {
        this.movementRepository = movementRepository;
        this.movementMapper = movementMapper;
        this.currentUserProvider = currentUserProvider;
    }

    /** Movements of the authenticated user's account. A user without account gets an empty page. */
    @Transactional(readOnly = true)
    public MovementPageResponse getMyMovements(PageQuery query) {
        Long userId = currentUserProvider.currentUser().id();
        Page<MovementRow> page = movementRepository.findPageByUserId(userId, query.toPageable());
        return new MovementPageResponse(
                page.getContent().stream().map(movementMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
