package com.elyefris.khalessleeptracker.data.repository

import android.util.Log
import com.elyefris.khalessleeptracker.data.model.Interruption
import com.elyefris.khalessleeptracker.data.model.SleepSession
import com.elyefris.khalessleeptracker.data.model.SleepStatus
import com.elyefris.khalessleeptracker.data.model.SleepType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseSleepRepository : SleepRepository {

    private val db = FirebaseFirestore.getInstance()
    // Asegúrate de usar el mismo nombre de colección siempre
    private val sessionsCollection = db.collection("sleep_sessions")

    // 1. Escuchar la sesión activa (la más reciente)
    override fun getLastSession(): Flow<SleepSession?> = callbackFlow {
        val query = sessionsCollection
            .orderBy("startTime", Query.Direction.DESCENDING)
            .limit(1)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirebaseRepo", "Error escuchando sesión actual", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val session = snapshot.documents[0].toObject(SleepSession::class.java)
                trySend(session)
            } else {
                trySend(null)
            }
        }

        awaitClose { registration.remove() }
    }

    // 2. Traer el historial (últimas 20 sesiones)
    override fun getHistory(): Flow<List<SleepSession>> = callbackFlow {
        val query = sessionsCollection
            .orderBy("startTime", Query.Direction.DESCENDING)
            .limit(20)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirebaseRepo", "Error escuchando historial", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val sessions = snapshot.toObjects(SleepSession::class.java)
                trySend(sessions)
            }
        }
        awaitClose { registration.remove() }
    }

    // 3. Crear nueva sesión
    override suspend fun startSleep(typeString: String) {
        val type = if (typeString == "SIESTA") SleepType.SIESTA else SleepType.NOCHE

        val newSession = SleepSession(
            startTime = Date(),
            type = type,
            status = SleepStatus.DURMIENDO
        )
        sessionsCollection.add(newSession).await()
    }

    // 4. Finalizar sesión
    override suspend fun finishSleep() {
        val activeSnapshot = sessionsCollection
            .whereEqualTo("status", "DURMIENDO")
            .get()
            .await()

        // Revisamos si también hay alguna marcada como "DESPIERTO" (interrupción activa)
        val awakeSnapshot = sessionsCollection
            .whereEqualTo("status", "DESPIERTO")
            .get()
            .await()

        val docs = activeSnapshot.documents + awakeSnapshot.documents

        if (docs.isNotEmpty()) {
            val doc = docs[0] // Tomamos la primera que encontremos activa
            doc.reference.update(
                mapOf(
                    "status" to SleepStatus.FINALIZADO,
                    "endTime" to Date()
                )
            ).await()
        }
    }

    // 5. Registrar despertar temporal
    override suspend fun wakeUp() {
        val activeSnapshot = sessionsCollection
            .whereEqualTo("status", "DURMIENDO")
            .get()
            .await()

        if (!activeSnapshot.isEmpty) {
            val doc = activeSnapshot.documents[0]
            val session = doc.toObject(SleepSession::class.java)

            val newInterruption = Interruption(wokeUpAt = Date())
            val updatedList = session?.interruptions.orEmpty() + newInterruption

            doc.reference.update(
                mapOf(
                    "status" to SleepStatus.DESPIERTO,
                    "interruptions" to updatedList
                )
            ).await()
        }
    }

    // 6. Volver a dormir tras interrupción
    override suspend fun backToSleep() {
        val activeSnapshot = sessionsCollection
            .whereEqualTo("status", "DESPIERTO")
            .get()
            .await()

        if (!activeSnapshot.isEmpty) {
            val doc = activeSnapshot.documents[0]
            val session = doc.toObject(SleepSession::class.java) ?: return

            val list = session.interruptions.toMutableList()
            if (list.isNotEmpty()) {
                val lastInterruption = list.last()
                list[list.lastIndex] = lastInterruption.copy(backToSleepAt = Date())
            }

            doc.reference.update(
                mapOf(
                    "status" to SleepStatus.DURMIENDO,
                    "interruptions" to list
                )
            ).await()
        }
    }
}