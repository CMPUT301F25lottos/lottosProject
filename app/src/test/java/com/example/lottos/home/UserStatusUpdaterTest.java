package com.example.lottos.home;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class UserStatusUpdaterTest {

    private UserStatusUpdater userStatusUpdater;

    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockEventsRef;
    @Mock private CollectionReference mockNotificationsRef;
    @Mock private Query mockQuery;
    @Mock private Task<QuerySnapshot> mockQueryTask;
    @Mock private WriteBatch mockWriteBatch;
    @Mock private Task<Void> mockCommitTask;
    @Mock private UserStatusUpdater.UpdateListener mockListener;

    @Captor private ArgumentCaptor<Integer> successCountCaptor;
    @Captor private ArgumentCaptor<String> errorStringCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockDb.collection("open events")).thenReturn(mockEventsRef);
        when(mockDb.collection("notification")).thenReturn(mockNotificationsRef);
        when(mockEventsRef.whereLessThan(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockQueryTask);
        when(mockDb.batch()).thenReturn(mockWriteBatch);
        when(mockWriteBatch.commit()).thenReturn(mockCommitTask);
        userStatusUpdater = new UserStatusUpdater(mockDb);
    }

    private void simulateQuerySuccess(Task<QuerySnapshot> task, List<DocumentSnapshot> documents) {
        QuerySnapshot mockSnapshot = mock(QuerySnapshot.class);
        when(mockSnapshot.isEmpty()).thenReturn(documents.isEmpty());
        when(mockSnapshot.size()).thenReturn(documents.size());
        when(mockSnapshot.getDocuments()).thenReturn(documents);

        when(task.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onSuccess(mockSnapshot);
            return task;
        });
        when(task.addOnFailureListener(any())).thenReturn(task);
    }

    private void simulateQueryFailure(Task<QuerySnapshot> task, Exception e) {
        when(task.addOnFailureListener(any())).thenAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(e);
            return task;
        });
        when(task.addOnSuccessListener(any())).thenReturn(task);
    }

    private void simulateCommitSuccess(Task<Void> task) {
        when(task.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return task;
        });
        when(task.addOnFailureListener(any())).thenReturn(task);
    }


    @Test
    public void sweepExpiredSelectedUsers_withUsersToMove_updatesBatchAndSucceeds() {
        DocumentSnapshot mockDoc = mock(DocumentSnapshot.class);
        DocumentReference mockDocRef = mock(DocumentReference.class);
        when(mockDoc.getReference()).thenReturn(mockDocRef);
        when(mockDoc.getString("eventName")).thenReturn("Old Event");
        when(mockDoc.getString("organizer")).thenReturn("TestOrganizer");

        Map<String, Object> selectedList = new HashMap<>();
        List<String> usersToMove = Arrays.asList("user1", "user2");
        selectedList.put("users", usersToMove);
        when(mockDoc.get("selectedList")).thenReturn(selectedList);

        when(mockNotificationsRef.document()).thenReturn(mock(DocumentReference.class));
        simulateQuerySuccess(mockQueryTask, Collections.singletonList(mockDoc));
        simulateCommitSuccess(mockCommitTask);

        userStatusUpdater.sweepExpiredSelectedUsers(mockListener);

        verify(mockWriteBatch).update(eq(mockDocRef), eq("cancelledList.users"), any(FieldValue.class));
        verify(mockWriteBatch).update(eq(mockDocRef), eq("selectedList.users"), any(ArrayList.class));

        verify(mockWriteBatch, times(2)).set(any(DocumentReference.class), any(Map.class));
        verify(mockWriteBatch).commit();
        verify(mockListener).onUpdateSuccess(successCountCaptor.capture());
        assertEquals(2, (int) successCountCaptor.getValue());
        verify(mockListener, never()).onUpdateFailure(anyString());
    }

    @Test
    public void sweepExpiredSelectedUsers_noUsersToMove_succeedsWithZeroCount() {
        DocumentSnapshot mockDoc = mock(DocumentSnapshot.class);
        Map<String, Object> selectedList = new HashMap<>();
        selectedList.put("users", new ArrayList<>());
        when(mockDoc.get("selectedList")).thenReturn(selectedList);
        simulateQuerySuccess(mockQueryTask, Collections.singletonList(mockDoc));

        userStatusUpdater.sweepExpiredSelectedUsers(mockListener);

        verify(mockWriteBatch, never()).commit();
        verify(mockListener).onUpdateSuccess(successCountCaptor.capture());
        assertEquals(0, (int) successCountCaptor.getValue());
        verify(mockListener, never()).onUpdateFailure(anyString());
    }

    @Test
    public void sweepExpiredSelectedUsers_queryFails_callsOnFailure() {
        Exception testException = new Exception("Permission denied");
        simulateQueryFailure(mockQueryTask, testException);

        userStatusUpdater.sweepExpiredSelectedUsers(mockListener);

        verify(mockDb, never()).batch();
        verify(mockWriteBatch, never()).commit();
        verify(mockListener).onUpdateFailure(errorStringCaptor.capture());
        assertEquals("Permission denied", errorStringCaptor.getValue());

        verify(mockListener, never()).onUpdateSuccess(anyInt());
    }
}
