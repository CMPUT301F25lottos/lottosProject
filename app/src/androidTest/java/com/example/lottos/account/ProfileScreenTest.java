package com.example.lottos.account;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.lottos.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@RunWith(AndroidJUnit4.class)
public class ProfileScreenTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private UserProfileManager mockProfileManager;

    @Test
    public void testUserProfile_DisplaysCorrectly() {
        // ARRANGE: Define the behavior of our mock.
        // When `loadUserProfile` is called, instantly succeed with test data.
        doAnswer(invocation -> {
            UserProfileManager.ProfileLoadListener listener = invocation.getArgument(1);
            listener.onProfileLoaded("John Doe", "john.doe@example.com", "123-456-7890");
            return null;
        }).when(mockProfileManager).loadUserProfile(anyString(), any(UserProfileManager.ProfileLoadListener.class));

        // ARRANGE: Create a factory that knows how to build ProfileScreen with our mock.
        FragmentFactory factory = new FragmentFactory() {
            @NonNull
            @Override
            public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
                if (className.equals(ProfileScreen.class.getName())) {
                    return new ProfileScreen(mockProfileManager);
                }
                return super.instantiate(classLoader, className);
            }
        };

        // ACT: Launch the fragment using the factory.
        Bundle args = new Bundle();
        args.putString("userName", "johndoe");
        FragmentScenario.launchInContainer(ProfileScreen.class, args, R.style.Theme_Lottos, factory);

        // ASSERT: Espresso will wait for the UI to settle and then check the views.
        onView(withId(R.id.tvUsername)).check(matches(withText("Username: johndoe")));
        onView(withId(R.id.tvName)).check(matches(withText("Name: John Doe")));
        onView(withId(R.id.tvEmail)).check(matches(withText("Email: john.doe@example.com")));
        onView(withId(R.id.tvPhoneNumber)).check(matches(withText("Phone Number: 123-456-7890")));
        onView(withId(R.id.btnEdit)).check(matches(isDisplayed()));
        onView(withId(R.id.btnDelete)).check(matches(isDisplayed()));
    }

    @Test
    public void testDeleteButton_ShowsConfirmationDialog() {
        // ARRANGE: Set up the fragment to be in a valid user state first.
        doAnswer(invocation -> {
            UserProfileManager.ProfileLoadListener listener = invocation.getArgument(1);
            listener.onProfileLoaded("John Doe", "john.doe@example.com", "123-456-7890");
            return null;
        }).when(mockProfileManager).loadUserProfile(anyString(), any(UserProfileManager.ProfileLoadListener.class));

        FragmentFactory factory = new FragmentFactory() {
            @NonNull
            @Override
            public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
                if (className.equals(ProfileScreen.class.getName())) {
                    return new ProfileScreen(mockProfileManager);
                }
                return super.instantiate(classLoader, className);
            }
        };

        Bundle args = new Bundle();
        args.putString("userName", "johndoe");
        FragmentScenario.launchInContainer(ProfileScreen.class, args, R.style.Theme_Lottos, factory);

        // ACT: Click the delete button.
        onView(withId(R.id.btnDelete)).perform(click());

        // ASSERT: Check that the confirmation dialog appears with the correct text.
        onView(withText("Delete Profile")).check(matches(isDisplayed()));
        onView(withText("Are you sure you want to permanently delete your account?")).check(matches(isDisplayed()));
        onView(withText("Yes")).check(matches(isDisplayed()));
        onView(withText("Cancel")).check(matches(isDisplayed()));
    }
}
