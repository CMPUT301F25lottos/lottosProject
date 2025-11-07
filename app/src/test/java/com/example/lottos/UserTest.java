package com.example.lottos;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class UserTest {
    @Test
    public void testUserConstructor() {
        UserInfo info = new UserInfo("username", "Some bio", "email@test.com", "1234567890");

        User user = new User("test123", info);

        assertEquals("test123", user.getUserName());
        assertNotNull(user.getUserInfo());
        Assert.assertEquals("username", user.getUserInfo().getDisplayName()); // Assuming UserInfo has getName()
        assertEquals("email@test.com", user.getUserInfo().getEmail()); // Assuming UserInfo has getEmail()
        Assert.assertEquals("1234567890", user.getUserInfo().getPhoneNumber()); // Assuming UserInfo has getPhone()

        assertNotNull(user.getOpenEvents());
        assertNotNull(user.getEnrolledEvents());
    }
}
