// File: app/src/test/java/com/example/lottos/events/EntrantEventManagerTest.java

package com.example.lottos.events;

import com.example.lottos.EventRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class EntrantEventManagerTest {

    @Mock private EventRepository mockRepo;
    @Mock private FirebaseFirestore mockDb;
    @Mock private EntrantEventManager.EventsCallback mockCallback;

    @Mock private Query mockQuery;
    @Mock private Task<QuerySnapshot> mockEventQueryTask;
    @Mock private Task<DocumentSnapshot> mockUserDocTask;
    @Mock private DocumentSnapshot mockUserSnapshot;
    @Mock private DocumentReference mockUserDocRef;
    @Mock private CollectionReference mockUserCollectionRef;

    @Captor private ArgumentCaptor<List<EntrantEventManager.EventModel>> eventsCaptor;
    @Captor private ArgumentCaptor<List<String>> stringListCaptor;
    @Captor private ArgumentCaptor<Exception> errorCaptor;

    private EntrantEventManager entrantEventManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        entrantEventManager = new EntrantEventManager(mockRepo, mockDb);

        when(mockRepo.getAllEvents()).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockEventQueryTask);
        when(mockDb.collection("users")).thenReturn(mockUserCollectionRef);
        when(mockUserCollectionRef.document(anyString())).thenReturn(mockUserDocRef);
        when(mockUserDocRef.get()).thenReturn(mockUserDocTask);
        when(mockEventQueryTask.addOnSuccessListener(any())).thenReturn(mockEventQueryTask);
        when(mockEventQueryTask.addOnFailureListener(any())).thenReturn(mockEventQueryTask);
        when(mockUserDocTask.addOnSuccessListener(any())).thenReturn(mockUserDocTask);
        when(mockUserDocTask.addOnFailureListener(any())).thenReturn(mockUserDocTask);
    }

    @Test
    public void loadAllOpenEvents_Success_SortsAndMapsCorrectly() {
        setupSuccessfulEventTask(createMockEventSnapshots());

        entrantEventManager.loadAllOpenEvents(mockCallback);

        verify(mockCallback).onSuccess(eventsCaptor.capture(), any());
        verify(mockCallback, never()).onError(any());

        List<EntrantEventManager.EventModel> capturedEvents = eventsCaptor.getValue();
        assertNotNull(capturedEvents);
        assertEquals(2, capturedEvents.size());

        EntrantEventManager.EventModel firstEvent = capturedEvents.get(0);
        assertEquals("event2", firstEvent.id);
        assertEquals("Future Event", firstEvent.name);
        assertTrue(firstEvent.isOpen);
    }

    @Test
    public void loadAllOpenEvents_Failure_CallsOnError() {
        Exception testException = new Exception("Firestore query failed!");
        setupFailedTask(mockEventQueryTask, testException);

        entrantEventManager.loadAllOpenEvents(mockCallback);

        verify(mockCallback).onError(errorCaptor.capture());
        verify(mockCallback, never()).onSuccess(any(), any());
        assertEquals(testException, errorCaptor.getValue());
    }


    @Test
    public void loadOpenEventsForUser_Success_FiltersForOpenEvents() {
        setupSuccessfulEventTask(createMockEventSnapshots());
        entrantEventManager.loadOpenEventsForUser("testUser", mockCallback);

        verify(mockCallback).onSuccess(eventsCaptor.capture(), any());
        verify(mockCallback, never()).onError(any());

        List<EntrantEventManager.EventModel> capturedEvents = eventsCaptor.getValue();
        assertEquals(1, capturedEvents.size()); // Only "Future Event" is open
        assertEquals("event2", capturedEvents.get(0).id);
    }

    @Test
    public void loadOpenEventsForUser_Failure_CallsOnError() {
        Exception testException = new Exception("Firestore query failed!");
        setupFailedTask(mockEventQueryTask, testException);

        entrantEventManager.loadOpenEventsForUser("testUser", mockCallback);

        verify(mockCallback).onError(errorCaptor.capture());
        verify(mockCallback, never()).onSuccess(any(), any());
        assertEquals(testException, errorCaptor.getValue());
    }

    @Test
    public void loadEventsHistory_Success_ReturnsCorrectUserEvents() {
        when(mockUserSnapshot.exists()).thenReturn(true);
        when(mockUserSnapshot.get("waitListedEvents.events")).thenReturn(Arrays.asList("event1", "event3"));
        setupSuccessfulUserTask(mockUserSnapshot);
        setupSuccessfulEventTask(createMockEventSnapshots());

        entrantEventManager.loadEventsHistory("testUser", mockCallback);

        verify(mockCallback).onSuccess(eventsCaptor.capture(), stringListCaptor.capture());
        verify(mockCallback, never()).onError(any());

        List<EntrantEventManager.EventModel> capturedEvents = eventsCaptor.getValue();
        assertEquals(1, capturedEvents.size()); // "event1" is valid, "event3" is not
        assertEquals("event1", capturedEvents.get(0).id);

        List<String> capturedIds = stringListCaptor.getValue();
        assertEquals(2, capturedIds.size());
        assertTrue(capturedIds.containsAll(Arrays.asList("event1", "event3")));
    }

    private void setupSuccessfulEventTask(List<QueryDocumentSnapshot> documents) {
        doAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            QuerySnapshot mockSnapshot = createMockQuerySnapshot(documents);
            listener.onSuccess(mockSnapshot);
            return mockEventQueryTask;
        }).when(mockEventQueryTask).addOnSuccessListener(any(OnSuccessListener.class));
    }

    private void setupSuccessfulUserTask(DocumentSnapshot mockSnapshot) {
        doAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            listener.onSuccess(mockSnapshot);
            return mockUserDocTask;
        }).when(mockUserDocTask).addOnSuccessListener(any(OnSuccessListener.class));
    }

    private <T> void setupFailedTask(Task<T> mockTask, Exception exception) {
        doAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(exception);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(OnFailureListener.class));
    }

    private List<QueryDocumentSnapshot> createMockEventSnapshots() {
        List<QueryDocumentSnapshot> mockDocuments = new ArrayList<>();

        QueryDocumentSnapshot doc1 = mock(QueryDocumentSnapshot.class);
        when(doc1.getId()).thenReturn("event1");
        when(doc1.getString("eventName")).thenReturn("Past Event");
        when(doc1.getBoolean("IsOpen")).thenReturn(false);
        when(doc1.getTimestamp("endTime")).thenReturn(new Timestamp(new Date(1672534800000L)));
        mockDocuments.add(doc1);

        QueryDocumentSnapshot doc2 = mock(QueryDocumentSnapshot.class);
        when(doc2.getId()).thenReturn("event2");
        when(doc2.getString("eventName")).thenReturn("Future Event");
        when(doc2.getBoolean("IsOpen")).thenReturn(true);
        when(doc2.getTimestamp("endTime")).thenReturn(new Timestamp(new Date(1766700000000L)));
        mockDocuments.add(doc2);

        QueryDocumentSnapshot doc3 = mock(QueryDocumentSnapshot.class);
        when(doc3.getId()).thenReturn("event3");
        when(doc3.getString("eventName")).thenReturn(null);
        when(doc3.getBoolean("IsOpen")).thenReturn(true);
        mockDocuments.add(doc3);

        return mockDocuments;
    }

    private QuerySnapshot createMockQuerySnapshot(List<QueryDocumentSnapshot> documents) {
        QuerySnapshot mockSnapshot = mock(QuerySnapshot.class);
        when(mockSnapshot.iterator()).thenReturn(documents.iterator());
        doAnswer(invocation -> {
            Consumer<QueryDocumentSnapshot> consumer = invocation.getArgument(0);
            documents.forEach(consumer);
            return null;
        }).when(mockSnapshot).forEach(any(Consumer.class));
        return mockSnapshot;
    }
}
