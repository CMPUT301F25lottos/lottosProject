package com.example.lottos.account;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class UserProfileManagerTest {

    private static final String TEST_USER = "testUser";
    private static final String TEST_NAME = "John Doe";
    private static final String TEST_EMAIL = "john.doe@example.com";
    private static final String TEST_PHONE = "123-456-7890";
    private static final String ERROR_MESSAGE = "Database error";

    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockCollection;
    @Mock private DocumentReference mockDocRef;
    @Mock private Task<DocumentSnapshot> mockGetTask;
    @Mock private Task<Void> mockUpdateTask;
    @Mock private Task<Void> mockDeleteTask;
    @Mock private DocumentSnapshot mockSnapshot;

    @Mock private UserProfileManager.ProfileLoadListener mockLoadListener;
    @Mock private UserProfileManager.ProfileUpdateListener mockUpdateListener;
    @Mock private UserProfileManager.DeleteListener mockDeleteListener;

    @Captor private ArgumentCaptor<OnSuccessListener<DocumentSnapshot>> getSuccessCaptor;
    @Captor private ArgumentCaptor<OnFailureListener> getFailureCaptor;
    @Captor private ArgumentCaptor<OnSuccessListener<Void>> updateSuccessCaptor;
    @Captor private ArgumentCaptor<OnFailureListener> updateFailureCaptor;
    @Captor private ArgumentCaptor<OnSuccessListener<Void>> deleteSuccessCaptor;
    @Captor private ArgumentCaptor<OnFailureListener> deleteFailureCaptor;

    private UserProfileManager manager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        manager = new UserProfileManager(mockDb);

        when(mockDb.collection("users")).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocRef);

        when(mockGetTask.addOnSuccessListener(any())).thenReturn(mockGetTask);
        when(mockUpdateTask.addOnSuccessListener(any())).thenReturn(mockUpdateTask);
        when(mockDeleteTask.addOnSuccessListener(any())).thenReturn(mockDeleteTask);

        when(mockDocRef.get()).thenReturn(mockGetTask);
        when(mockDocRef.update(anyString(), any(), anyString(), any(), anyString(), any())).thenReturn(mockUpdateTask);
        when(mockDocRef.delete()).thenReturn(mockDeleteTask);
    }

    @Test
    public void loadUserProfile_Success() {

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", TEST_NAME);
        userInfo.put("email", TEST_EMAIL);
        userInfo.put("phoneNumber", TEST_PHONE);

        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.get("userInfo")).thenReturn(userInfo);

        manager.loadUserProfile(TEST_USER, mockLoadListener);
        verify(mockGetTask).addOnSuccessListener(getSuccessCaptor.capture());
        getSuccessCaptor.getValue().onSuccess(mockSnapshot);

        verify(mockLoadListener).onProfileLoaded(TEST_NAME, TEST_EMAIL, TEST_PHONE);
        verify(mockLoadListener, never()).onProfileNotFound();
        verify(mockLoadListener, never()).onError(anyString());
    }

    @Test
    public void loadUserProfile_NotFound() {

        when(mockSnapshot.exists()).thenReturn(false);

        manager.loadUserProfile(TEST_USER, mockLoadListener);
        verify(mockGetTask).addOnSuccessListener(getSuccessCaptor.capture());
        getSuccessCaptor.getValue().onSuccess(mockSnapshot);

        verify(mockLoadListener).onProfileNotFound();
        verify(mockLoadListener, never()).onProfileLoaded(any(), any(), any());
        verify(mockLoadListener, never()).onError(anyString());
    }

    @Test
    public void loadUserProfile_Failure() {

        Exception exception = new Exception(ERROR_MESSAGE);

        manager.loadUserProfile(TEST_USER, mockLoadListener);
        verify(mockGetTask).addOnFailureListener(getFailureCaptor.capture());
        getFailureCaptor.getValue().onFailure(exception);

        verify(mockLoadListener).onError("Failed to load profile info.");
        verify(mockLoadListener, never()).onProfileLoaded(any(), any(), any());
        verify(mockLoadListener, never()).onProfileNotFound();
    }

    @Test
    public void updateUserProfile_Success() {

        manager.updateUserProfile(TEST_USER, TEST_NAME, TEST_EMAIL, TEST_PHONE, mockUpdateListener);
        verify(mockUpdateTask).addOnSuccessListener(updateSuccessCaptor.capture());
        updateSuccessCaptor.getValue().onSuccess(null);

        verify(mockUpdateListener).onUpdateSuccess();
        verify(mockUpdateListener, never()).onUpdateFailure(anyString());
    }

    @Test
    public void updateUserProfile_Failure() {
        Exception exception = new Exception(ERROR_MESSAGE);

        manager.updateUserProfile(TEST_USER, TEST_NAME, TEST_EMAIL, TEST_PHONE, mockUpdateListener);
        verify(mockUpdateTask).addOnFailureListener(updateFailureCaptor.capture());
        updateFailureCaptor.getValue().onFailure(exception);

        verify(mockUpdateListener).onUpdateFailure("Failed to update profile.");
        verify(mockUpdateListener, never()).onUpdateSuccess();
    }

    @Test
    public void deleteUser_Success() {
        manager.deleteUser(TEST_USER, mockDeleteListener);
        verify(mockDeleteTask).addOnSuccessListener(deleteSuccessCaptor.capture());
        deleteSuccessCaptor.getValue().onSuccess(null);

        verify(mockDeleteListener).onDeleteSuccess();
        verify(mockDeleteListener, never()).onDeleteFailure(anyString());
    }

    @Test
    public void deleteUser_Failure() {
        Exception exception = new Exception(ERROR_MESSAGE);

        manager.deleteUser(TEST_USER, mockDeleteListener);
        verify(mockDeleteTask).addOnFailureListener(deleteFailureCaptor.capture());
        deleteFailureCaptor.getValue().onFailure(exception);

        verify(mockDeleteListener).onDeleteFailure("Failed to delete profile.");
        verify(mockDeleteListener, never()).onDeleteSuccess();
    }
}
