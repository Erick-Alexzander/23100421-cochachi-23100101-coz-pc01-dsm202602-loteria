package dev.erick.pc1.data.remote

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

class FirebaseJugadaManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        private const val TAG = "FirebaseJugadaManager"
    }

    suspend fun getActiveSorteo(): Result<Pair<String, Map<String, Any>>?> {
        val currentUser = auth.currentUser
        val projectId = firestore.app.options.projectId
        Log.d(TAG, "getActiveSorteo [START] - ProjectId: $projectId, Auth User: ${currentUser?.email}, UID: ${currentUser?.uid}")

        return try {
            val snapshot = firestore.collection("sorteos")
                .limit(1)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                Log.d(TAG, "getActiveSorteo [SUCCESS] - No active sorteos found in collection 'sorteos'")
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                Log.d(TAG, "getActiveSorteo [SUCCESS] - Found sorteo ID: ${doc.id}")
                Result.success(Pair(doc.id, doc.data ?: emptyMap()))
            }
        } catch (e: Exception) {
            val code = (e as? FirebaseFirestoreException)?.code
            Log.e(TAG, "getActiveSorteo [ERROR] - Code: $code, Message: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun getJugada(sorteoId: String, uid: String): Result<List<Int>?> {
        val currentUser = auth.currentUser
        Log.d(TAG, "getJugada [START] - SorteoId: $sorteoId, Target UID: $uid, Current Auth UID: ${currentUser?.uid}")

        return try {
            val docSnapshot = firestore.collection("sorteos")
                .document(sorteoId)
                .collection("jugadas")
                .document(uid)
                .get()
                .await()

            if (docSnapshot.exists()) {
                @Suppress("UNCHECKED_CAST")
                val numeros = docSnapshot.get("numeros") as? List<Long>
                val intList = numeros?.map { it.toInt() }
                Log.d(TAG, "getJugada [SUCCESS] - Existing jugada found with numbers: $intList")
                Result.success(intList)
            } else {
                Log.d(TAG, "getJugada [SUCCESS] - No existing jugada found for document 'sorteos/$sorteoId/jugadas/$uid'")
                Result.success(null)
            }
        } catch (e: Exception) {
            val code = (e as? FirebaseFirestoreException)?.code
            Log.e(TAG, "getJugada [ERROR] - Code: $code, Message: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun saveJugada(sorteoId: String, uid: String, numeros: List<Int>): Result<Unit> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.failure(Exception("Usuario no autenticado."))
        }
        val authUid = currentUser.uid
        Log.d(TAG, "saveJugada [START] - SorteoId: $sorteoId, Param UID: $uid, Auth UID: $authUid, Numbers: $numeros")

        // 1. Validar que sean exactamente 6 números distintos entre 1 y 45
        if (numeros.size != 6 || !numeros.all { it in 1..45 } || numeros.toSet().size != 6) {
            return Result.failure(IllegalArgumentException("La jugada debe tener exactamente 6 números distintos entre 1 y 45."))
        }

        return try {
            val sorteoRef = firestore.collection("sorteos").document(sorteoId)
            val jugadaRef = sorteoRef.collection("jugadas").document(authUid)

            // 2. Ejecutar transacción atómica de lectura y escritura de forma totalmente integrada
            firestore.runTransaction { transaction ->
                // Lectura 1: Verificar existencia y estado del sorteo dentro de la transacción
                val sorteoSnapshot = transaction.get(sorteoRef)
                if (!sorteoSnapshot.exists()) {
                    throw IllegalStateException("El sorteo especificado no existe.")
                }
                val estado = sorteoSnapshot.getString("estado") ?: "abierto"
                if (estado.equals("finalizado", ignoreCase = true) || estado.equals("cerrado", ignoreCase = true)) {
                    throw IllegalStateException("El sorteo ya comenzó o finalizó. No se pueden registrar jugadas.")
                }

                // Lectura 2: Verificar si ya existe jugada previa para este usuario en este sorteo
                val jugadaSnapshot = transaction.get(jugadaRef)
                if (jugadaSnapshot.exists()) {
                    throw IllegalStateException("Ya tienes una jugada registrada para este sorteo.")
                }

                // Escritura: Crear la nueva jugada con los campos exactos requeridos
                val data = mapOf(
                    "uidUsuario" to authUid,
                    "sorteoId" to sorteoId,
                    "numeros" to numeros.sorted(),
                    "createdAt" to Timestamp.now()
                )
                transaction.set(jugadaRef, data)
            }.await()

            Log.d(TAG, "saveJugada [SUCCESS] - Jugada successfully saved at 'sorteos/$sorteoId/jugadas/$authUid'")
            Result.success(Unit)
        } catch (e: Exception) {
            val code = (e as? FirebaseFirestoreException)?.code
            Log.e(TAG, "saveJugada [ERROR] - Code: $code, Message: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }
}
