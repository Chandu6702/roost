package com.chandu.roost.workspace;

import com.chandu.roost.identity.User;
import com.chandu.roost.identity.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentWorkspaceService {

    private final UserRepository userRepository;

    public CurrentWorkspaceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Long getCurrentWorkspaceId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        Workspace workspace = user.getWorkspace();

        if (workspace == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your account does not have a workspace");
        }

        return workspace.getId();
    }
}
