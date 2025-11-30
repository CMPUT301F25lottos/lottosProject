package com.example.lottos.organizer;import com.example.lottos.EventRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class OrganizerEventDetailsManagerTest {

    private OrganizerEventDetailsManager manager;
    @Mock private FirebaseFirestore mockDb;
    @Mock private EventRepository mockRepo;
    @Mock private OrganizerEventDetailsManager.LoadCallback mockLoadCallback;
    @Mock private Runnable mockOnSuccess;
    @Mock private Consumer<Exception> mockOnError;

    @Mock private DocumentReference mockEventRef;
    @Mock private DocumentReference mockUserRef;
    @Mock private CollectionReference mockUsersCollection;
    @Mock private CollectionReference mockNotificationsCollection;
    @Mock private Task<DocumentSnapshot> mockGetTask;
    @Mock private Task<Void> mockCommitTask;
    @Mock private WriteBatch mockWriteBatch;

    @Captor private ArgumentCaptor<Exception> exceptionCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        manager = new OrganizerEventDetailsManager(mockDb, mockRepo);

        when(mockRepo.getEvent(anyString())).thenReturn(mockEventRef);
        when(mockEventRef.get()).thenReturn(mockGetTask);
        when(mockDb.batch()).thenReturn(mockWriteBatch);
        when(mockWriteBatch.commit()).thenReturn(mockCommitTask);
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(anyString())).thenReturn(mockUserRef);
        when(mockDb.collection("notification")).thenReturn(mockNotificationsCollection);
        when(mockNotificationsCollection.document()).thenReturn(mock(DocumentReference.class));
    }

    private void simulateGetSuccess(Task<DocumentSnapshot> task, DocumentSnapshot result) {
        when(task.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            listener.onSuccess(result);
            return task;
        });
        when(task.addOnFailureListener(any())).thenReturn(task);
    }

    private void simulateCommitSuccess(Task<Void> task) {
        when(task.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return task;
        });
    }


    @Test
    public void loadEvent_Success_parsesAndCallsOnSuccess() {
        DocumentSnapshot mockSnap = mock(DocumentSnapshot.class);
        when(mockSnap.exists()).thenReturn(true);
        Map<String, Object> eventData = new HashMap<>();
        Map<String, Object> waitlistData = new HashMap<>();
        waitlistData.put("users", Arrays.asList("user1", "user2"));
        eventData.put("waitList", waitlistData);
        when(mockSnap.getData()).thenReturn(eventData);

        simulateGetSuccess(mockGetTask, mockSnap);

        manager.loadEvent("testEvent", mockLoadCallback);

        ArgumentCaptor<List<String>> waitlistCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockLoadCallback).onSuccess(any(), waitlistCaptor.capture(), any(), any(), any(), any());
        assertEquals(2, waitlistCaptor.getValue().size());
        assertEquals("user1", waitlistCaptor.getValue().get(0));
        verify(mockLoadCallback, never()).onError(any());
    }

    @Test
    public void loadEvent_EventNotFound_callsOnError() {
        DocumentSnapshot mockSnap = mock(DocumentSnapshot.class);
        when(mockSnap.exists()).thenReturn(false); // Event does not exist
        simulateGetSuccess(mockGetTask, mockSnap);

        manager.loadEvent("testEvent", mockLoadCallback);

        verify(mockLoadCallback).onError(exceptionCaptor.capture());
        assertEquals("Event not found", exceptionCaptor.getValue().getMessage());
        verify(mockLoadCallback, never()).onSuccess(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void runLottery_Success_buildsBatchAndCommits() {

        DocumentSnapshot mockSnap = mock(DocumentSnapshot.class);
        when(mockSnap.exists()).thenReturn(true);
        when(mockSnap.getLong("selectionCap")).thenReturn(1L); // Select 1 user
        simulateGetSuccess(mockGetTask, mockSnap);
        simulateCommitSuccess(mockCommitTask);

        List<String> waitlistUsers = Arrays.asList("userA", "userB");

        manager.runLottery("testEvent", waitlistUsers, mockOnSuccess, mockOnError);

        verify(mockWriteBatch).update(eq(mockEventRef), any(Map.class));

        verify(mockWriteBatch, times(2)).update(eq(mockUserRef), anyString(), any(FieldValue.class), anyString(), any(FieldValue.class));

        verify(mockWriteBatch, times(2)).set(any(DocumentReference.class), any(Map.class));

        verify(mockWriteBatch).commit();
        verify(mockOnSuccess).run();
        verify(mockOnError, never()).accept(any());
    }

    @Test
    public void runLottery_NoWaitlistUsers_callsOnError() {
        List<String> emptyWaitlist = new ArrayList<>();

        manager.runLottery("testEvent", emptyWaitlist, mockOnSuccess, mockOnError);

        verify(mockOnError).accept(exceptionCaptor.capture());
        assertEquals("No users on waitlist to run lottery.", exceptionCaptor.getValue().getMessage());

        verify(mockEventRef, never()).get();
        verify(mockDb, never()).batch();
        verify(mockOnSuccess, never()).run();
    }
}
