package dev.erick.pc1.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dev.erick.pc1.data.model.BolillaModel
import dev.erick.pc1.data.model.SorteoModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseSorteoManager {
    private val firestore = FirebaseFirestore.getInstance()

    fun observarSorteo(sorteoId: String): Flow<SorteoModel?> = callbackFlow {
        val listenerRegistration = firestore.collection("sorteos")
            .document(sorteoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val sorteo = snapshot.toObject(SorteoModel::class.java)?.copy(id = snapshot.id)
                    trySend(sorteo)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    fun observarBolillas(sorteoId: String): Flow<List<BolillaModel>> = callbackFlow {
        val listenerRegistration = firestore.collection("sorteos")
            .document(sorteoId)
            .collection("bolillas")
            .orderBy("orden", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val bolillas = snapshot.documents.mapNotNull {
                        it.toObject(BolillaModel::class.java)
                    }
                    trySend(bolillas)
                }
            }

        awaitClose { listenerRegistration.remove() }
    }
}