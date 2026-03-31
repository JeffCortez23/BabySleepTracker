package com.elyefris.khalessleeptracker.data.repository

import android.util.Log
import com.elyefris.khalessleeptracker.data.model.DiaperChange
import com.elyefris.khalessleeptracker.data.model.DiaperType
import com.elyefris.khalessleeptracker.data.model.Interruption
import com.elyefris.khalessleeptracker.data.model.SleepSession
import com.elyefris.khalessleeptracker.data.model.SleepStatus
import com.elyefris.khalessleeptracker.data.model.SleepType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseSleepRepository : SleepRepository {

    private val db = FirebaseFirestore.getInstance()
    private val sessionsCollection = db.collection("sleep_sessions")
    private val diapersCollection = db.collection("diaper_changes")

    override fun getLastSession(): Flow<SleepSession?> = callbackFlow {
        val query = sessionsCollection.orderBy("startTime", Query.Direction.DESCENDING).limit(1)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null && !snapshot.isEmpty) {
                trySend(snapshot.documents[0].toObject(SleepSession::class.java))
            } else trySend(null)
        }
        awaitClose { registration.remove() }
    }

    override fun getHistory(): Flow<List<SleepSession>> = callbackFlow {
        val query = sessionsCollection.orderBy("startTime", Query.Direction.DESCENDING)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null) trySend(snapshot.toObjects(SleepSession::class.java))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun startSleep(typeString: String) {
        val type = if (typeString == "SIESTA") SleepType.SIESTA else SleepType.NOCHE
        sessionsCollection.add(SleepSession(startTime = Date(), type = type, status = SleepStatus.DURMIENDO)).await()
    }

    override suspend fun finishSleep() {
        val active = sessionsCollection.whereEqualTo("status", "DURMIENDO").get().await()
        val awake = sessionsCollection.whereEqualTo("status", "DESPIERTO").get().await()
        val docs = active.documents + awake.documents
        if (docs.isNotEmpty()) {
            docs[0].reference.update(mapOf("status" to SleepStatus.FINALIZADO, "endTime" to Date())).await()
        }
    }

    override suspend fun wakeUp() {
        val active = sessionsCollection.whereEqualTo("status", "DURMIENDO").get().await()
        if (!active.isEmpty) {
            val doc = active.documents[0]
            val session = doc.toObject(SleepSession::class.java)
            val updatedList = session?.interruptions.orEmpty() + Interruption(wokeUpAt = Date())
            doc.reference.update(mapOf("status" to SleepStatus.DESPIERTO, "interruptions" to updatedList)).await()
        }
    }

    override suspend fun backToSleep() {
        val active = sessionsCollection.whereEqualTo("status", "DESPIERTO").get().await()
        if (!active.isEmpty) {
            val doc = active.documents[0]
            val session = doc.toObject(SleepSession::class.java) ?: return
            val list = session.interruptions.toMutableList()
            if (list.isNotEmpty()) list[list.lastIndex] = list.last().copy(backToSleepAt = Date())
            doc.reference.update(mapOf("status" to SleepStatus.DURMIENDO, "interruptions" to list)).await()
        }
    }

    override fun getDiaperChanges(): Flow<List<DiaperChange>> = callbackFlow {
        val query = diapersCollection.orderBy("timestamp", Query.Direction.DESCENDING)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null) trySend(snapshot.toObjects(DiaperChange::class.java))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addDiaperChange(type: DiaperType, notes: String, timestamp: Date) {
        diapersCollection.add(DiaperChange(timestamp = timestamp, type = type, notes = notes)).await()
    }

    override suspend fun deleteDiaperChange(diaperId: String) {
        diapersCollection.document(diaperId).delete().await()
    }

    override suspend fun deleteSession(sessionId: String) {
        sessionsCollection.document(sessionId).delete().await()
    }

    override suspend fun addManualSession(session: SleepSession) {
        sessionsCollection.add(session.copy(status = SleepStatus.FINALIZADO)).await()
    }

    // NUEVO: Función para guardar los cambios al editar
    override suspend fun updateSession(session: SleepSession) {
        sessionsCollection.document(session.id).set(session).await()
    }
}