package com.example.lottos.notifications;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class NotificationManagerTest {

    private NotificationManager notificationManager;

    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockCollection;
    @Mock private DocumentReference mockDocRef;
    @Mock private Query mockQuery;
    @Mock private Task<QuerySnapshot> mockGetTask;
    @Mock private QuerySnapshot mockQuerySnapshot;
    @Mock private Task<Void> mockDeleteTask;

    @Mock private NotificationManager.NotificationCallback mockCallback;
    @Mock private Runnable mockOnDone;

    @Captor private ArgumentCaptor<List<NotificationManager.NotificationModel>> listCaptor;
    @Captor private ArgumentCaptor<Exception> exceptionCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        notificationManager = new NotificationManager(mockDb);

        when(mockDb.collection("notification")).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocRef);
        when(mockCollection.orderBy(anyString(), any(Query.Direction.class))).thenReturn(mockQuery);
        when(mockCollection.whereEqualTo(anyString(), anyString())).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockGetTask);
        when(mockDocRef.delete()).thenReturn(mockDeleteTask);
    }

    private void simulateGetSuccess(List<QueryDocumentSnapshot> docs) {
        when(mockQuerySnapshot.iterator()).thenReturn(docs.iterator());
        when(mockQuerySnapshot.size()).thenReturn(docs.size());
        when(mockGetTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            ((com.google.android.gms.tasks.OnSuccessListener<QuerySnapshot>) invocation.getArgument(0))
                    .onSuccess(mockQuerySnapshot);
            return mockGetTask;
        });
        when(mockGetTask.addOnFailureListener(any())).thenReturn(mockGetTask);
    }

    private void simulateGetFailure(Exception e) {
        when(mockGetTask.addOnFailureListener(any())).thenAnswer(invocation -> {
            ((com.google.android.gms.tasks.OnFailureListener) invocation.getArgument(0)).onFailure(e);
            return mockGetTask;
        });
        when(mockGetTask.addOnSuccessListener(any())).thenReturn(mockGetTask);
    }

    @Test
    public void loadAllNotifications_onSuccess_returnsCorrectlyMappedList() {
        QueryDocumentSnapshot mockDoc = mock(QueryDocumentSnapshot.class);
        when(mockDoc.getId()).thenReturn("doc-1");
        when(mockDoc.getString("content")).thenReturn("Test Content");
        when(mockDoc.getString("eventName")).thenReturn("Test Event");
        when(mockDoc.getString("receiver")).thenReturn("receiver-1");
        when(mockDoc.getString("sender")).thenReturn("sender-1");
        when(mockDoc.getTimestamp("timestamp")).thenReturn(new Timestamp(new Date()));
        simulateGetSuccess(Collections.singletonList(mockDoc));

        notificationManager.loadAllNotifications(mockCallback);

        verify(mockCollection).orderBy("timestamp", Query.Direction.ASCENDING);
        verify(mockCallback).onSuccess(listCaptor.capture());
        verify(mockCallback, never()).onError(any());

        List<NotificationManager.NotificationModel> capturedList = listCaptor.getValue();
        assertEquals(1, capturedList.size());
        assertEquals("Test Content", capturedList.get(0).content);
        assertEquals("Test Event", capturedList.get(0).eventName);
    }

    @Test
    public void loadNotificationForUser_onSuccess_buildsCorrectQueryAndReturnsList() {
        String userName = "test-user";
        QueryDocumentSnapshot mockDoc = mock(QueryDocumentSnapshot.class);
        when(mockDoc.getId()).thenReturn("doc-2");
        when(mockDoc.getString("content")).thenReturn("User Content");
        when(mockDoc.getString("eventName")).thenReturn("User Event");
        when(mockDoc.getTimestamp("timestamp")).thenReturn(new Timestamp(new Date()));
        simulateGetSuccess(Collections.singletonList(mockDoc));

        notificationManager.loadNotificationForUser(userName, mockCallback);

        verify(mockCollection).whereEqualTo("receiver", userName);
        verify(mockCallback).onSuccess(listCaptor.capture());
        verify(mockCallback, never()).onError(any());

        List<NotificationManager.NotificationModel> capturedList = listCaptor.getValue();
        assertEquals(1, capturedList.size());
        assertEquals("User Content", capturedList.get(0).content);
    }

    @Test
    public void loadNotificationForUser_onFailure_callsOnError() {
        String userName = "test-user";
        Exception testException = new Exception("Firestore unavailable");
        simulateGetFailure(testException);

        notificationManager.loadNotificationForUser(userName, mockCallback);

        verify(mockCallback).onError(exceptionCaptor.capture());
        verify(mockCallback, never()).onSuccess(any());
        assertEquals(testException, exceptionCaptor.getValue());
    }



    @Test
    public void deleteNotificationById_callsDeleteOnCorrectDocument() {
        String notificationId = "notif-to-delete";
        when(mockDeleteTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            ((com.google.android.gms.tasks.OnSuccessListener<Void>) invocation.getArgument(0)).onSuccess(null);
            return mockDeleteTask;
        });

        notificationManager.deleteNotificationById(notificationId, mockOnDone);

        verify(mockCollection).document(notificationId);
        verify(mockDocRef).delete();
        verify(mockOnDone).run();
    }
}
