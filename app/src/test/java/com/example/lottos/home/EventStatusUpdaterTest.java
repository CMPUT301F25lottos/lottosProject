package com.example.lottos.home;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
// *** FIX 1: Import QueryDocumentSnapshot ***
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
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class EventStatusUpdaterTest {

    private EventStatusUpdater eventStatusUpdater;

    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockEventsRef;
    @Mock private Task<QuerySnapshot> mockQueryTask;
    @Mock private EventStatusUpdater.UpdateListener mockListener;

    @Captor private ArgumentCaptor<Integer> countCaptor;
    @Captor private ArgumentCaptor<String> errorCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockDb.collection("open events")).thenReturn(mockEventsRef);
        when(mockEventsRef.get()).thenReturn(mockQueryTask);

        eventStatusUpdater = new EventStatusUpdater(mockDb);
    }

    private void simulateQuerySuccess(List<QueryDocumentSnapshot> documents) {
        QuerySnapshot mockSnapshot = mock(QuerySnapshot.class);
        when(mockSnapshot.isEmpty()).thenReturn(documents.isEmpty());
        when(mockSnapshot.iterator()).thenReturn(documents.iterator());

        when(mockQueryTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onSuccess(mockSnapshot);
            return mockQueryTask;
        });
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);
    }

    private void simulateQueryFailure(Exception e) {
        when(mockQueryTask.addOnFailureListener(any())).thenAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(e);
            return mockQueryTask;
        });
        when(mockQueryTask.addOnSuccessListener(any())).thenReturn(mockQueryTask);
    }

    @Test
    public void updateEventStatuses_withMismatchedEvents_updatesCorrectly() {
        long nowSeconds = Timestamp.now().getSeconds();

        QueryDocumentSnapshot docToClose = mock(QueryDocumentSnapshot.class);
        DocumentReference refToClose = mock(DocumentReference.class);
        when(docToClose.getReference()).thenReturn(refToClose);
        when(docToClose.getTimestamp("registerEndTime")).thenReturn(new Timestamp(nowSeconds - 1000, 0)); // Expired
        when(docToClose.getBoolean("IsOpen")).thenReturn(true);
        when(refToClose.update(anyString(), anyBoolean())).thenReturn(mock(Task.class));

        QueryDocumentSnapshot docToOpen = mock(QueryDocumentSnapshot.class);
        DocumentReference refToOpen = mock(DocumentReference.class);
        when(docToOpen.getReference()).thenReturn(refToOpen);
        when(docToOpen.getTimestamp("registerEndTime")).thenReturn(new Timestamp(nowSeconds + 1000, 0)); // In the future
        when(docToOpen.getBoolean("IsOpen")).thenReturn(false);
        when(refToOpen.update(anyString(), anyBoolean())).thenReturn(mock(Task.class));

        QueryDocumentSnapshot docCorrect = mock(QueryDocumentSnapshot.class);
        when(docCorrect.getTimestamp("registerEndTime")).thenReturn(new Timestamp(nowSeconds + 1000, 0)); // In the future
        when(docCorrect.getBoolean("IsOpen")).thenReturn(true);

        simulateQuerySuccess(Arrays.asList(docToClose, docToOpen, docCorrect));

        eventStatusUpdater.updateEventStatuses(mockListener);

        verify(refToClose).update("IsOpen", false);
        verify(refToOpen).update("IsOpen", true);

        verify(docCorrect, never()).getReference();

        verify(mockListener).onUpdateSuccess(countCaptor.capture());
        assertEquals(2, (int) countCaptor.getValue());
        verify(mockListener, never()).onUpdateFailure(anyString());
    }

    @Test
    public void updateEventStatuses_whenQueryIsEmpty_succeedsWithZeroCount() {
        simulateQuerySuccess(Collections.emptyList());

        eventStatusUpdater.updateEventStatuses(mockListener);

        verify(mockListener).onUpdateSuccess(countCaptor.capture());
        assertEquals(0, (int) countCaptor.getValue());

        verify(mockListener, never()).onUpdateFailure(anyString());
        verify(mockEventsRef, never()).document(anyString());
    }

    @Test
    public void updateEventStatuses_whenQueryFails_callsOnFailure() {
        Exception testException = new Exception("Firestore disconnected");
        simulateQueryFailure(testException);

        eventStatusUpdater.updateEventStatuses(mockListener);

        verify(mockListener).onUpdateFailure(errorCaptor.capture());
        assertEquals("Failed to update event statuses.", errorCaptor.getValue());

        verify(mockListener, never()).onUpdateSuccess(anyInt());
    }
}
