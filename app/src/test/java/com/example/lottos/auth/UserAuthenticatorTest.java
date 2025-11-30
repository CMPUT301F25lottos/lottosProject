package com.example.lottos.auth;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the UserAuthenticator class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class UserAuthenticatorTest {
    @Mock
    private FirebaseFirestore mockFirestore;
    @Mock
    private CollectionReference mockCollectionReference;
    @Mock
    private DocumentReference mockDocumentReference;
    @Mock
    private DocumentSnapshot mockDocumentSnapshot;
    @Mock
    private Task<DocumentSnapshot> mockGetTask;
    @Mock
    private Task<Void> mockSetTask;
    @Mock
    private UserAuthenticator.AuthListener mockAuthListener;

    @Captor
    private ArgumentCaptor<OnCompleteListener<DocumentSnapshot>> getCompleteListenerCaptor;
    @Captor
    private ArgumentCaptor<OnSuccessListener<DocumentSnapshot>> getSuccessListenerCaptor;
    @Captor
    private ArgumentCaptor<OnFailureListener> getFailureListenerCaptor;
    @Captor
    private ArgumentCaptor<OnSuccessListener<Void>> setSuccessListenerCaptor;
    @Captor
    private ArgumentCaptor<OnFailureListener> setFailureListenerCaptor;
    @Captor
    private ArgumentCaptor<Map<String, Object>> userDataCaptor;

    private UserAuthenticator userAuthenticator;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(mockFirestore.collection("users")).thenReturn(mockCollectionReference);

        userAuthenticator = new UserAuthenticator(mockFirestore);

        when(mockCollectionReference.document(any(String.class))).thenReturn(mockDocumentReference);

        when(mockGetTask.addOnSuccessListener(any())).thenReturn(mockGetTask);
        when(mockGetTask.addOnFailureListener(any())).thenReturn(mockGetTask);
        when(mockSetTask.addOnSuccessListener(any())).thenReturn(mockSetTask);
        when(mockSetTask.addOnFailureListener(any())).thenReturn(mockSetTask);
    }

    @Test
    public void checkUserLogin_success_whenCredentialsAreCorrect() {
        String userName = "testUser";
        String password = "password123";

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("password", password);

        when(mockGetTask.isSuccessful()).thenReturn(true);
        when(mockGetTask.getResult()).thenReturn(mockDocumentSnapshot);
        when(mockDocumentSnapshot.exists()).thenReturn(true);
        when(mockDocumentSnapshot.get("userInfo")).thenReturn(userInfo);

        when(mockDocumentReference.get()).thenReturn(mockGetTask);

        userAuthenticator.checkUserLogin(userName, password, mockAuthListener);

        verify(mockGetTask).addOnCompleteListener(getCompleteListenerCaptor.capture());
        getCompleteListenerCaptor.getValue().onComplete(mockGetTask);

        verify(mockAuthListener).onSuccess(userName);
    }

    @Test
    public void checkUserLogin_failure_whenPasswordIsIncorrect() {
        String userName = "testUser";
        String correctPassword = "password123";
        String wrongPassword = "wrongPassword";

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("password", correctPassword);

        when(mockGetTask.isSuccessful()).thenReturn(true);
        when(mockGetTask.getResult()).thenReturn(mockDocumentSnapshot);
        when(mockDocumentSnapshot.exists()).thenReturn(true);
        when(mockDocumentSnapshot.get("userInfo")).thenReturn(userInfo);

        when(mockDocumentReference.get()).thenReturn(mockGetTask);

        userAuthenticator.checkUserLogin(userName, wrongPassword, mockAuthListener);
        verify(mockGetTask).addOnCompleteListener(getCompleteListenerCaptor.capture());
        getCompleteListenerCaptor.getValue().onComplete(mockGetTask);

        verify(mockAuthListener).onFailure("Incorrect password");
    }

    @Test
    public void checkUserLogin_failure_whenUsernameNotFound() {
        when(mockGetTask.isSuccessful()).thenReturn(true);
        when(mockGetTask.getResult()).thenReturn(mockDocumentSnapshot);
        when(mockDocumentSnapshot.exists()).thenReturn(false); // User does not exist

        when(mockDocumentReference.get()).thenReturn(mockGetTask);

        userAuthenticator.checkUserLogin("nonexistentUser", "password", mockAuthListener);
        verify(mockGetTask).addOnCompleteListener(getCompleteListenerCaptor.capture());
        getCompleteListenerCaptor.getValue().onComplete(mockGetTask);

        verify(mockAuthListener).onFailure("Username not found");
    }

    @Test
    public void registerUser_success_whenUsernameIsAvailable() {
        // Arrange
        String userName = "newUser";
        String displayName = "New User";
        String password = "newPassword";
        String email = "new@example.com";

        when(mockDocumentReference.get()).thenReturn(mockGetTask);
        when(mockDocumentSnapshot.exists()).thenReturn(false); // Username is available

        when(mockDocumentReference.set(any(Map.class))).thenReturn(mockSetTask);

        userAuthenticator.registerUser(userName, displayName, password, email, null, mockAuthListener);

        verify(mockGetTask).addOnSuccessListener(getSuccessListenerCaptor.capture());
        getSuccessListenerCaptor.getValue().onSuccess(mockDocumentSnapshot);

        verify(mockDocumentReference).set(userDataCaptor.capture());
        verify(mockSetTask).addOnSuccessListener(setSuccessListenerCaptor.capture());
        setSuccessListenerCaptor.getValue().onSuccess(null);

        verify(mockAuthListener).onSuccess(userName);

        Map<String, Object> capturedData = userDataCaptor.getValue();
        assertNotNull(capturedData);
        assertEquals(userName, capturedData.get("userName"));
        Map<String, Object> userInfo = (Map<String, Object>) capturedData.get("userInfo");
        assertNotNull(userInfo);
        assertEquals(displayName, userInfo.get("displayName"));
        assertEquals(password, userInfo.get("password"));
        assertEquals(email, userInfo.get("email"));
    }

    @Test
    public void registerUser_failure_whenUsernameIsTaken() {
        String userName = "existingUser";
        when(mockDocumentReference.get()).thenReturn(mockGetTask);
        when(mockDocumentSnapshot.exists()).thenReturn(true); // Username is taken

        userAuthenticator.registerUser(userName, "name", "pass", "email", null, mockAuthListener);

        verify(mockGetTask).addOnSuccessListener(getSuccessListenerCaptor.capture());
        getSuccessListenerCaptor.getValue().onSuccess(mockDocumentSnapshot);

        verify(mockAuthListener).onFailure("Username already taken. Please choose another.");
    }

    @Test
    public void registerUser_failure_whenUsernameCheckFails() {
        Exception exception = new Exception("Firestore error");
        when(mockDocumentReference.get()).thenReturn(mockGetTask);

        userAuthenticator.registerUser("user", "name", "pass", "email", null, mockAuthListener);

        verify(mockGetTask).addOnFailureListener(getFailureListenerCaptor.capture());
        getFailureListenerCaptor.getValue().onFailure(exception);

        verify(mockAuthListener).onFailure("Error checking username. Try again.");
    }
}
