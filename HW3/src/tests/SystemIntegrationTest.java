package tests;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import application.User;
import databasePart1.DatabaseHelper;
import java.sql.SQLException;

/**
 * Integration tests for the User Question and Answer System.
 * <p>
 * This class tests the core functionalities of the system, including user registration,
 * login, role management, and deletion. It ensures that the database operations are
 * working correctly and validates role-based access control.
 * </p>
 *
 * @author John Ramsey
 * @version 1.0
 * @since 2025-03-24
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SystemIntegrationTest {

    private static DatabaseHelper databaseHelper;
    private static String inviteCode;
    private static final String TEST_USER = "TestUser";
    private static final String TEST_PASSWORD = "TestPass123!";
    
    /**
     * Set up the database for use in all of the tests.
     *
     * @throws SQLException if there is an error during database initialization.
     */
    @BeforeAll
    static void setUp() throws SQLException {
        databaseHelper = new DatabaseHelper();
        databaseHelper.initializeTestDatabase();
    }

    /**
     * Creates a new admin user and generates an invitation code for a student user.
     *
     * @throws SQLException if there is an error during database operations.
     */
    @Test
    @Order(1)
    void testCreateAdminAndInviteUser() throws SQLException {
        // Register an admin user
        databaseHelper.register(new User("AdminUser", "AdminPass123!", "admin"));
        System.out.println("Test #1a Complete: New user 'AdminUser' created.");
        
        // Generate invitation code
        inviteCode = databaseHelper.generateInvitationCode(false, true, false, false, false);
        assertNotNull(inviteCode);
        System.out.println("Test #1b Complete: Invite code generated.");
    }

    /**
     * Registers a student user using an invitation code generated by an admin.
     *
     * @throws SQLException if there is an error during database operations.
     */
    @Test
    @Order(2)
    void testRegisterStudentWithInviteCode() throws SQLException {
        assertTrue(databaseHelper.validateInvitationCode(inviteCode));
        databaseHelper.register(new User(TEST_USER, TEST_PASSWORD, "student"));
        databaseHelper.assignRolesFromCodeToUser(TEST_USER, inviteCode);
        assertEquals("student", databaseHelper.getUserRole(TEST_USER));
        System.out.println("Test #2 Complete: New user 'TestUser' created with role 'student'.");
    }

    /**
     * Tests logging in as a student user.
     *
     * @throws SQLException if there is an error during login validation.
     */
    @Test
    @Order(3)
    void testLoginAsStudent() throws SQLException {
        boolean loginSuccess = databaseHelper.login(new User(TEST_USER, TEST_PASSWORD, "student"));
        assertTrue(loginSuccess, "Student should be able to login");
        System.out.println("Test #3 Complete: Student user 'TestUser' logged in.");
    }

    /**
     * Changes the role of a student user to include multiple roles (student and reviewer).
     *
     * @throws SQLException if there is an error during role update operations.
     */
    @Test
    @Order(4)
    void testChangeUserRole() throws SQLException {
        databaseHelper.changeRoles(TEST_USER, "student, reviewer");
        String userRole = databaseHelper.getUserRole(TEST_USER);
        assertTrue(userRole.contains("student") && userRole.contains("reviewer"));
        System.out.println("Test #4 Complete: Student user 'TestUser' changed roles to 'student' and 'reviewer'.");
    }

    /**
     * Deletes a student user from the database.
     *
     * @throws SQLException if there is an error during deletion operations.
     */
    @Test
    @Order(5)
    void testDeleteUser() throws SQLException {
        String result = databaseHelper.tryDeleteUser(TEST_USER);
        assertEquals(TEST_USER, result);
        assertFalse(databaseHelper.doesUserExist(TEST_USER));
        System.out.println("Test #5 Complete.");
    }

    /**
     * Cleans up after all tests by closing the database connection.
     *
     * @throws SQLException if there is an error during connection closure.
     */
    @AfterAll
    static void tearDown() throws SQLException {
    	System.out.println("Tests are done! Closing connection...");
        databaseHelper.closeConnection();
    }
}

