package service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.UserRepository;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private Algorithm testAlgorithm;

    private UserService userService;

    @BeforeEach
    public void setUp() {
        testAlgorithm = Algorithm.HMAC256("test-secret-key");
        userService = new UserService(userRepository, testAlgorithm);
    }

    @Test
    public void shouldLoginSuccessfullyAndReturnToken() {
        String password = "password123";
        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        User testUser = new User(1, "username", hashedPassword);

        when(userRepository.getByUsername("username")).thenReturn(testUser);

        String token = userService.login("username", password);

        assertNotNull(token);
        assertEquals("username", JWT.decode(token).getSubject());
    }

    @Test
    public void shouldReturnNullOnLoginWithWrongPassword() {
        String password = "password123";
        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        User testUser = new User(1, "username", hashedPassword);

        when(userRepository.getByUsername("username")).thenReturn(testUser);

        String token = userService.login("username", "wrong_password");

        assertNull(token);
    }

    @Test
    public void shouldReturnNullOnLoginWithUnknownUsername() {
        when(userRepository.getByUsername("unknown")).thenReturn(null);

        String token = userService.login("unknown", "any_password");

        assertNull(token);
    }

    @Test
    public void shouldVerifyValidTokenSuccessfully() {
        String token = JWT.create().withSubject("username").sign(testAlgorithm);

        boolean result = userService.verify(token);

        assertTrue(result);
    }

    @Test
    public void shouldFailToVerifyInvalidToken() {
        Algorithm wrongAlgorithm = Algorithm.HMAC256("wrong-secret-key");
        String invalidToken = JWT.create().withSubject("username").sign(wrongAlgorithm);

        boolean result = userService.verify(invalidToken);

        assertFalse(result);
    }

    @Test
    public void shouldVerifyAndGetUsernameSuccessfully() {
        String token = JWT.create().withSubject("username").sign(testAlgorithm);

        String username = userService.verifyAndGetUsername(token);

        assertEquals("username", username);
    }

    @Test
    public void shouldReturnNullWhenVerifyAndGetUsernameWithInvalidToken() {
        Algorithm wrongAlgorithm = Algorithm.HMAC256("wrong-secret-key");
        String invalidToken = JWT.create().withSubject("username").sign(wrongAlgorithm);

        String username = userService.verifyAndGetUsername(invalidToken);

        assertNull(username);
    }

    @Test
    public void shouldFailToVerifyMalformedToken() {
        boolean result = userService.verify("this.is.not.a.valid.token");

        assertFalse(result);
    }

    @Test
    public void shouldRegisterUserSuccessfully() {
        when(userRepository.create(any(User.class))).thenReturn(10);

        int id = userService.register("new_user", "password");

        assertEquals(10, id);
        verify(userRepository, times(1)).create(argThat(user -> 
                user.username().equals("new_user") && 
                BCrypt.verifyer().verify("password".toCharArray(), user.password().toCharArray()).verified
        ));
    }

    @Test
    public void shouldThrowExceptionWhenRegisteringDuplicateUser() {
        SQLException sqlException = new SQLException("Duplicate key", "23505");
        when(userRepository.create(any(User.class))).thenThrow(new RuntimeException(sqlException));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> userService.register("existing_user", "password"));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    public void shouldGetUserSuccessfully() {
        User expectedUser = new User(1, "user1", "hash");
        when(userRepository.get(1)).thenReturn(expectedUser);

        User user = userService.getUser(1);

        assertEquals(expectedUser, user);
    }

    @Test
    public void shouldThrowExceptionWhenGettingNonExistentUser() {
        when(userRepository.get(99)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> userService.getUser(99));

        assertTrue(exception.getMessage().contains("was not found"));
    }

    @Test
    public void shouldGetByUsernameSuccessfully() {
        User expectedUser = new User(1, "user1", "hash");
        when(userRepository.getByUsername("user1")).thenReturn(expectedUser);

        User user = userService.getByUsername("user1");

        assertEquals(expectedUser, user);
    }

    @Test
    public void shouldThrowExceptionWhenGettingNonExistentUsername() {
        when(userRepository.getByUsername("unknown")).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> userService.getByUsername("unknown"));

        assertTrue(exception.getMessage().contains("was not found"));
    }

    @Test
    public void shouldUpdateUserSuccessfully() {
        userService.updateUser(1, "updated_user", "new_password");

        verify(userRepository, times(1)).update(eq(1), argThat(user -> 
                user.id() == 1 &&
                user.username().equals("updated_user") &&
                BCrypt.verifyer().verify("new_password".toCharArray(), user.password().toCharArray()).verified
        ));
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingToDuplicateUsername() {
        SQLException sqlException = new SQLException("Duplicate key", "23505");
        doThrow(new RuntimeException(sqlException)).when(userRepository).update(anyInt(), any(User.class));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> userService.updateUser(1, "existing_user", "password"));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    public void shouldDeleteUserSuccessfully() {
        when(userRepository.delete(1)).thenReturn(true);

        boolean result = userService.deleteUser(1);

        assertTrue(result);
        verify(userRepository, times(1)).delete(1);
    }
}
