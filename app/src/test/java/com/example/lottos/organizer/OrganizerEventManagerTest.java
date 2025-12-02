package com.example.lottos.organizer;

import com.example.lottos.EventRepository;
import com.example.lottos.entities.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class OrganizerEventManagerTest {

    private OrganizerEventManager manager;

    @Mock private EventRepository mockRepo;
    @Mock private FirebaseFirestore mockDb;
    @Mock private FirebaseAuth mockAuth;
    @Mock private Runnable mockOnSuccess;
    @Mock private EventRepository.OnError mockOnError;
    @Mock private CollectionReference mockUsersCollection;
    @Mock private DocumentReference mockUserDocRef;
    @Mock private Task<Void> mockUpdateTask;

    @Mock private Event mockEvent;

    @Captor private ArgumentCaptor<Map<String, Object>> eventDataCaptor;
    @Captor private ArgumentCaptor<Exception> exceptionCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        manager = new OrganizerEventManager(mockRepo, mockDb, mockAuth);

        when(mockAuth.getUid()).thenReturn("testOrganizerUid");
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(anyString())).thenReturn(mockUserDocRef);
        when(mockUserDocRef.update(anyString(), any())).thenReturn(mockUpdateTask);
    }

    private void simulateTaskSuccess() {
        when(mockUpdateTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            ((com.google.android.gms.tasks.OnSuccessListener<Void>) invocation.getArgument(0)).onSuccess(null);
            return mockUpdateTask;
        });
        when(mockUpdateTask.addOnFailureListener(any())).thenReturn(mockUpdateTask);
    }

    private void simulateTaskFailure(Exception e) {
        when(mockUpdateTask.addOnFailureListener(any())).thenAnswer(invocation -> {
            ((com.google.android.gms.tasks.OnFailureListener) invocation.getArgument(0)).onFailure(e);
            return mockUpdateTask;
        });
        when(mockUpdateTask.addOnSuccessListener(any())).thenReturn(mockUpdateTask);
    }

    @Test
    public void createEvent_Success_bypassesConstructorAndLinksToUser() {
        when(mockEvent.getEventId()).thenReturn("eventId123");
        when(mockEvent.getEventName()).thenReturn("Test Event");
        when(mockEvent.getOrganizer()).thenReturn("organizer1");
        when(mockEvent.getDescription()).thenReturn("A mock description");

        doAnswer(invocation -> {
            Runnable repoOnSuccess = invocation.getArgument(2);
            repoOnSuccess.run();
            return null;
        }).when(mockRepo).createEvent(anyString(), any(Map.class), any(Runnable.class), eq(mockOnError));

        simulateTaskSuccess();

        manager.createEvent(mockEvent, LocalDateTime.now(), 50, null, false, mockOnSuccess, mockOnError);

        verify(mockRepo).createEvent(eq("eventId123"), eventDataCaptor.capture(), any(Runnable.class), eq(mockOnError));
        Map<String, Object> capturedData = eventDataCaptor.getValue();
        assertEquals("eventId123", capturedData.get("eventId"));
        assertEquals("testOrganizerUid", capturedData.get("organizerUid"));
        assertTrue(capturedData.containsKey("waitList"));

        verify(mockUserDocRef).update(eq("organizedEvents.events"), any(FieldValue.class));
        verify(mockOnSuccess).run();
        verify(mockOnError, never()).run(any());
    }

    @Test
    public void createEvent_LinkingFails_bypassesConstructorAndCallsOnError() {

        when(mockEvent.getEventId()).thenReturn("eventId123");
        when(mockEvent.getOrganizer()).thenReturn("organizer1"); // Needed for the update path
        Exception linkException = new Exception("Update failed");

        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return null;
        }).when(mockRepo).createEvent(anyString(), any(Map.class), any(Runnable.class), eq(mockOnError));

        simulateTaskFailure(linkException);


        manager.createEvent(mockEvent, LocalDateTime.now(), 50, null, false, mockOnSuccess, mockOnError);

        verify(mockOnError).run(exceptionCaptor.capture());
        assertEquals(linkException, exceptionCaptor.getValue());
        verify(mockOnSuccess, never()).run();
    }

    @Test
    public void updateEvent_delegatesToRepository() {

        Map<String, Object> updates = Map.of("eventName", "Updated Name");

        manager.updateEvent("eventId123", updates, mockOnSuccess, mockOnError);

        verify(mockRepo).updateEvent("eventId123", updates, mockOnSuccess, mockOnError);
    }

    @Test
    public void deleteEvent_delegatesToRepository() {
        manager.deleteEvent("eventId123", mockOnSuccess, mockOnError);
        verify(mockRepo).deleteEvent("eventId123", mockOnSuccess, mockOnError);
    }
}
