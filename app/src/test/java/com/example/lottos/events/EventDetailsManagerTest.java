package com.example.lottos.events;

import com.example.lottos.EventRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class EventDetailsManagerTest {

    private EventDetailsManager eventDetailsManager;

    @Mock private FirebaseFirestore mockDb;
    @Mock private EventRepository mockRepo;
    @Mock private DocumentReference mockEventDocRef;
    @Mock private DocumentReference mockUserDocRef;
    @Mock private CollectionReference mockUserCollectionRef;
    @Mock private Transaction mockTransaction;
    @Mock private Runnable mockOnSuccessRunnable;
    @Mock private Consumer<Exception> mockConsumerOnError;
    @Mock private EventRepository.OnError mockRepoOnError;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        eventDetailsManager = new EventDetailsManager(mockDb, mockRepo);

        when(mockRepo.getEvent(anyString())).thenReturn(mockEventDocRef);
        when(mockDb.collection("users")).thenReturn(mockUserCollectionRef);
        when(mockUserCollectionRef.document(anyString())).thenReturn(mockUserDocRef);
    }

    private <T> void simulateSuccess(Task<T> task) {
        when(task.addOnSuccessListener(any(OnSuccessListener.class))).thenAnswer(invocation -> {
            OnSuccessListener<T> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return task;
        });
        when(task.addOnFailureListener(any(OnFailureListener.class))).thenReturn(task);
    }

    private <T> void simulateFailure(Task<T> task, Exception e) {
        when(task.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(task);
        when(task.addOnFailureListener(any(OnFailureListener.class))).thenAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(e);
            return task;
        });
    }

    private <T> void setupTransactionMocks(Task<T> transactionTask) {
        doAnswer(invocation -> {
            Transaction.Function<T> function = invocation.getArgument(0);
            try {
                function.apply(mockTransaction);
                simulateSuccess(transactionTask);
            } catch (Exception ex) {
                simulateFailure(transactionTask, ex);
            }
            return transactionTask;
        }).when(mockDb).runTransaction(any(Transaction.Function.class));
    }

    @Test
    public void deleteEvent_Success_CallsOnSuccess() {
        Task<Void> mockDeleteTask = mock(Task.class);
        when(mockEventDocRef.delete()).thenReturn(mockDeleteTask);
        simulateSuccess(mockDeleteTask);
        eventDetailsManager.deleteEvent("testEvent", mockOnSuccessRunnable, mockConsumerOnError);
        verify(mockEventDocRef).delete();
        verify(mockOnSuccessRunnable).run();
        verify(mockConsumerOnError, never()).accept(any());
    }

    @Test
    public void deleteEvent_Failure_CallsOnError() {
        Task<Void> mockDeleteTask = mock(Task.class);
        when(mockEventDocRef.delete()).thenReturn(mockDeleteTask);
        Exception e = new Exception("Delete failed");
        simulateFailure(mockDeleteTask, e);
        eventDetailsManager.deleteEvent("testEvent", mockOnSuccessRunnable, mockConsumerOnError);
        verify(mockEventDocRef).delete();
        verify(mockConsumerOnError).accept(e);
        verify(mockOnSuccessRunnable, never()).run();
    }

    @Test
    public void joinWaitlist_Success_PerformsCorrectUpdates() throws FirebaseFirestoreException {
        Task<Void> mockTransactionTask = mock(Task.class);
        setupTransactionMocks(mockTransactionTask);
        DocumentSnapshot mockEventSnap = mock(DocumentSnapshot.class);
        when(mockEventSnap.getBoolean("IsOpen")).thenReturn(true);
        when(mockTransaction.get(mockEventDocRef)).thenReturn(mockEventSnap);

        eventDetailsManager.joinWaitlist("testEvent", "testUser", mockOnSuccessRunnable, mockRepoOnError);

        verify(mockTransaction).update(eq(mockEventDocRef), eq("waitList.users"), any(FieldValue.class));
        verify(mockTransaction).update(eq(mockUserDocRef), eq("waitListedEvents.events"), any(FieldValue.class));
        verify(mockOnSuccessRunnable).run();
        verify(mockRepoOnError, never()).run(any());
    }

    @Test
    public void leaveWaitlist_Success_PerformsCorrectUpdates() {
        Task<Void> mockTransactionTask = mock(Task.class);
        setupTransactionMocks(mockTransactionTask);
        eventDetailsManager.leaveWaitlist("testEvent", "testUser", mockOnSuccessRunnable, mockRepoOnError);

        verify(mockTransaction).update(eq(mockEventDocRef), eq("waitList.users"), any(FieldValue.class));
        verify(mockTransaction).update(eq(mockUserDocRef), eq("waitListedEvents.events"), any(FieldValue.class));
        verify(mockOnSuccessRunnable).run();
        verify(mockRepoOnError, never()).run(any());
    }

    @Test
    public void acceptInvite_Success_PerformsCorrectUpdates() {
        Task<Void> mockTransactionTask = mock(Task.class);
        setupTransactionMocks(mockTransactionTask);
        eventDetailsManager.acceptInvite("testEvent", "testUser", mockOnSuccessRunnable, mockRepoOnError);

        verify(mockTransaction).update(eq(mockEventDocRef), eq("enrolledList.users"), any(FieldValue.class));
        verify(mockTransaction).update(eq(mockUserDocRef), eq("selectedEvents.events"), any(FieldValue.class));
        verify(mockTransaction).update(eq(mockUserDocRef), eq("enrolledEvents.events"), any(FieldValue.class));
        verify(mockOnSuccessRunnable).run();
        verify(mockRepoOnError, never()).run(any());
    }

    @Test
    public void declineInvite_Success_PerformsCorrectUpdates() {
        Task<Void> mockTransactionTask = mock(Task.class);
        setupTransactionMocks(mockTransactionTask);
        eventDetailsManager.declineInvite("testEvent", "testUser", mockOnSuccessRunnable, mockRepoOnError);

        verify(mockTransaction).update(eq(mockEventDocRef), eq("cancelledList.users"), any(FieldValue.class));
        verify(mockTransaction).update(eq(mockUserDocRef), eq("selectedEvents.events"), any(FieldValue.class));
        verify(mockTransaction).update(eq(mockUserDocRef), eq("declinedEvents.events"), any(FieldValue.class));
        verify(mockOnSuccessRunnable).run();
        verify(mockRepoOnError, never()).run(any());
    }
}
