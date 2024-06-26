package uta.cse3310;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.io.File;

public class UserAuthenticationTest extends TestCase 
{
    private UserAuthentication userA;

    public UserAuthenticationTest(String testName) 
    {
        super(testName);
    }

    public static Test suite() 
    {
        return new TestSuite(UserAuthenticationTest.class);
    }

    @Override
    public void setUp() 
    {
        File file = new File("users.txt");
        if (file.exists()) 
        {
            file.delete();
        }
        // delete existing file if it exists
        userA = new UserAuthentication(); 
        // Initialize UserAuthentication for file recreation
    }

    public void testLogin() 
    {
        // Verify login
        userA.newUser("TestUser"); // add a user
        userA.login("TestUser");

        // Verify invalid login attempt
        userA.login("InvalidUser");
    }

    public void testNewUser() 
    {
        // Test adding a new user
        userA.newUser("NewUser");
        // Check if new user was added successfully
        assertTrue("CheckName should return true for new user", userA.checkName("NewUser"));

        // Test adding a duplicate user
        userA.newUser("NewUser");
    }

    public void testCheckName() 
    {
        // Test checking an existing user
        userA.newUser("ExistingUser");
        assertTrue("CheckName should return true for user that already exists", userA.checkName("ExistingUser"));

        // Test checking a user that does not exist
        assertFalse("CheckName should return false for user that does not exist", userA.checkName("NonExistingUser"));
    }
}