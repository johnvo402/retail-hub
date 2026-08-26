package com.johnvo.retailhub.application.features.auth.query.me;

import com.johnvo.retailhub.application.common.ResourceNotFoundException;
import com.johnvo.retailhub.application.common.cqrs.QueryHandler;
import com.johnvo.retailhub.application.features.auth.common.UserView;
import com.johnvo.retailhub.domain.identity.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCurrentUserQueryHandler implements QueryHandler<GetCurrentUserQuery, UserView> {
    private final UserRepository users;

    public GetCurrentUserQueryHandler(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserView handle(GetCurrentUserQuery query) {
        return users.findById(query.userId()).map(UserView::from)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found"));
    }
}

