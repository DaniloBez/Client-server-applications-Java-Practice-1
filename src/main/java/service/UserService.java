package service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import entity.User;
import lombok.extern.slf4j.Slf4j;
import repository.UserRepository;

import java.sql.SQLException;

@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final Algorithm algorithm;

    public UserService(UserRepository userRepository, Algorithm algorithm) {
        this.userRepository = userRepository;
        this.algorithm = algorithm;
    }

    public String login(String username, String password) {
        User user = userRepository.getByUsername(username);

        if (user == null)
            return null;

        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.password().toCharArray());

        if (!result.verified)
            return null;

        try {
            return JWT.create()
                    .withSubject(username)
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            log.error("JWT creation error", e);
            return null;
        }
    }

    public boolean verify(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(token);
            return true;
        }
        catch (JWTVerificationException e) {
            log.error("JWT verification error", e);
            return false;
        }
    }

    public String verifyAndGetUsername(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token).getSubject();
        }
        catch (JWTVerificationException e) {
            log.error("JWT verification error", e);
            return null;
        }
    }

    public int register(String username, String password) {
        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        try {
            return userRepository.create(new User(0, username, hashedPassword));
        } catch (RuntimeException e) {
            if (isUniqueConstraintViolation(e))
                throw new IllegalArgumentException("Unable to create user: the username '" + username + "' already exists");

            throw e;
        }
    }

    public User getUser(int userId) {
        User user = userRepository.get(userId);
        if (user == null)
            throw new IllegalArgumentException("The user with ID " + userId + " was not found");

        return user;
    }

        public User getByUsername(String username) {
        User user = userRepository.getByUsername(username);
        if (user == null)
            throw new IllegalArgumentException("The user with username '" + username + "' was not found");

        return user;
    }

    public void updateUser(int userId, String newUsername, String newPassword) {
        String hashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
        try {
            userRepository.update(userId, new User(userId, newUsername, hashedPassword));
        } catch (RuntimeException e) {
            if (isUniqueConstraintViolation(e))
                throw new IllegalArgumentException("Unable to update user: the username '" + newUsername + "' already exists");

            throw e;
        }
    }

    public boolean deleteUser(int userId) {
        return userRepository.delete(userId);
    }

    private boolean isUniqueConstraintViolation(RuntimeException e) {
        Throwable cause = e.getCause();
        if (cause instanceof SQLException sqlException) {
            return "23505".equals(sqlException.getSQLState());
        }
        return false;
    }
}
