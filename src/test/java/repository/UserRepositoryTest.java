package repository;

import entity.User;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryTest extends BaseRepositoryTest {

    @Test
    public void shouldFindById() {
        int id = userRepository.create(new User(0, "TestUser1", "password1"));

        User user = userRepository.get(id);

        assertNotNull(user);
        assertEquals(id, user.id());
        assertEquals("TestUser1", user.username());
        assertEquals("password1", user.password());
    }

    @Test
    public void shouldFindByUsername() {
        userRepository.create(new User(0, "TestUser2", "password2"));

        User user = userRepository.getByUsername("TestUser2");

        assertNotNull(user);
        assertEquals("TestUser2", user.username());
        assertEquals("password2", user.password());
    }

    @Test
    public void shouldUpdateUser() {
        int id = userRepository.create(new User(0, "OldName", "oldpwd"));

        userRepository.update(id, new User(0, "NewName", "newpwd"));

        User updatedUser = userRepository.get(id);
        assertNotNull(updatedUser);
        assertEquals("NewName", updatedUser.username());
        assertEquals("newpwd", updatedUser.password());
    }

    @Test
    public void shouldDeleteById() {
        int id = userRepository.create(new User(0, "UserToDelete", "pwd"));

        boolean isDeleted = userRepository.delete(id);
        assertTrue(isDeleted);

        assertNull(userRepository.get(id));

        boolean isDeletedAgain = userRepository.delete(id);
        assertFalse(isDeletedAgain);
    }

    @Test
    public void shouldFailToCreateDuplicateUsername() {
        userRepository.create(new User(0, "UniqueUser", "pwd"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userRepository.create(new User(0, "UniqueUser", "pwd2")));

        Throwable cause = exception.getCause();
        assertNotNull(cause);
        assertInstanceOf(SQLException.class, cause);

        assertEquals("23505", ((SQLException) cause).getSQLState(),
                "SQL state should match PostgreSQL unique violation code");
    }
}
