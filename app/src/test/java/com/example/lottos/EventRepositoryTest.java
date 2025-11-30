package com.example.lottos;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class EventRepositoryTest {

    private EventRepository eventRepository;

    @Mock
    private FirebaseFirestore mockDb;
    @Mock
    private CollectionReference mockCollection;
    @Mock
    private DocumentReference mockDocRef;
    @Mock
    private Query mockQuery;
    @Mock
    private Task<Void> mockVoidTask;
    @Mock
    private Task<DocumentSnapshot> mockGetTask;
    @Mock
    private DocumentSnapshot mockDocSnapshot;

    @Mock
    private Runnable mockOnSuccess;
    @Mock
    private EventRepository.OnError mockOnError;
    @Mock
    private EventRepository.OnNameResult mockOnNameResult;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;
    @Captor
    private ArgumentCaptor<String> stringCaptor;


    @Before
    public void setUp() {

        MockitoAnnotations.openMocks(this);


        eventRepository = new EventRepository(mockDb);

        when(mockDb.collection("open events")).thenReturn(mockCollection);
        when(mockCollection.document(any(String.class))).thenReturn(mockDocRef);
    }


    @Test
    public void getEvent_returnsCorrectDocumentReference() {
        String eventId = "testEvent123";
        when(mockDocRef.getPath()).thenReturn("open events/testEvent123");
        when(eventRepository.getEvent(eventId)).thenReturn(mockDocRef);

        DocumentReference ref = eventRepository.getEvent(eventId);

        assertEquals("open events/testEvent123", ref.getPath());
    }


    @Test
    public void getEventsByOrganizer_buildsCorrectQuery() {
        String organizerId = "organizer-abc";

        Query query = eventRepository.getEventsByOrganizer(organizerId);

    }


    @Test
    public void createEvent_callsSetWithCorrectData() {
        String eventId = "newEventId";
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("name", "New Event");
        eventData.put("capacity", 100);

        when(mockDocRef.set(any(Map.class))).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            ((com.google.android.gms.tasks.OnSuccessListener<Void>) invocation.getArgument(0)).onSuccess(null);
            return mockVoidTask;
        });

        eventRepository.createEvent(eventId, eventData, mockOnSuccess, mockOnError);

        verify(mockDocRef).set(mapCaptor.capture());
        assertEquals(eventData, mapCaptor.getValue());
        verify(mockOnSuccess).run();
    }


    @Test
    public void updateEvent_callsUpdateWithCorrectData() {
        String eventId = "existingEventId";
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "Updated Event Name");

        when(mockDocRef.update(any(Map.class))).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            ((com.google.android.gms.tasks.OnSuccessListener<Void>) invocation.getArgument(0)).onSuccess(null);
            return mockVoidTask;
        });

        eventRepository.updateEvent(eventId, updates, mockOnSuccess, mockOnError);

        verify(mockDocRef).update(mapCaptor.capture());
        assertEquals(updates, mapCaptor.getValue());
        verify(mockOnSuccess).run();
    }


    @Test
    public void deleteEvent_callsDeleteAndTriggersSuccess() {

        String eventId = "eventToDelete";
        when(mockDocRef.delete()).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            ((com.google.android.gms.tasks.OnSuccessListener<Void>) invocation.getArgument(0)).onSuccess(null);
            return mockVoidTask;
        });

        eventRepository.deleteEvent(eventId, mockOnSuccess, mockOnError);

        verify(mockDocRef).delete();
        verify(mockOnSuccess).run();
    }

    @Test
    public void getEventName_onSuccess_returnsCorrectName() {

        String eventId = "eventWithAName";
        String eventName = "The Grand Gala";
        when(mockDocRef.get()).thenReturn(mockGetTask);
        when(mockDocSnapshot.exists()).thenReturn(true);
        when(mockDocSnapshot.getString("eventName")).thenReturn(eventName);

        when(mockGetTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            ((com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>) invocation.getArgument(0)).onSuccess(mockDocSnapshot);
            return mockGetTask;
        });

        eventRepository.getEventName(eventId, mockOnNameResult, mockOnError);

        verify(mockOnNameResult).run(stringCaptor.capture());
        assertEquals(eventName, stringCaptor.getValue());
    }

    @Test
    public void getEventName_whenNameIsEmpty_fallsBackToEventId() {
        String eventId = "eventWithoutAName";
        when(mockDocRef.get()).thenReturn(mockGetTask);
        when(mockDocSnapshot.exists()).thenReturn(true);
        when(mockDocSnapshot.getString("eventName")).thenReturn("");

        when(mockGetTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            ((com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>) invocation.getArgument(0)).onSuccess(mockDocSnapshot);
            return mockGetTask;
        });

        eventRepository.getEventName(eventId, mockOnNameResult, mockOnError);

        verify(mockOnNameResult).run(stringCaptor.capture());
        assertEquals(eventId, stringCaptor.getValue());
    }
}
