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
        return try {
            val snapshot = firestore.collection("sorteos")
                .whereEqualTo("estado", "abierto")
                .limit(1)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                Result.success(Pair(doc.id, doc.data ?: emptyMap()))
            }
        } catch (e: Exception) {
            val code = (e as? FirebaseFirestoreException)?.code
            Log.e(TAG, "getActiveSorteo error - Code: $code, Message: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun getJugada(sorteoId: String, uid: String): Result<List<Int>?> {
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
                Result.success(intList)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            val code = (e as? FirebaseFirestoreException)?.code
            Log.e(TAG, "getJugada error - Code: $code, Message: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun saveJugada(sorteoId: String, uid: String, numeros: List<Int>): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("Usuario no autenticado."))
            }
            if (currentUser.uid != uid) {
                return Result.failure(Exception("El UID especificado no coincide con el usuario autenticado."))
            }

            if (numeros.size != 6) {
                return Result.failure(Exception("La jugada debe contener exactamente 6 números."))
            }

            val sorteoDoc = firestore.collection("sorteos").document(sorteoId).get().await()
            if (!sorteoDoc.exists()) {
                return Result.failure(Exception("El sorteo con ID '$sorteoId' no existe en Firestore."))
            }

            val estado = sorteoDoc.getString("estado")
            if (estado != "abierto") {
                return Result.failure(Exception("El sorteo no está abierto para registrar jugadas (estado actual: '${estado ?: "nulo"}')."))
            }

            val jugadaRef = firestore.collection("sorteos")
                .document(sorteoId)
                .collection("jugadas")
                .document(uid)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(jugadaRef)
                if (snapshot.exists()) {
                    throw IllegalStateException("Ya tienes una jugada registrada para este sorteo.")
                }
                
                val data = mapOf(
                    "uidUsuario" to uid,
                    "sorteoId" to sorteoId,
                    "numeros" to numeros.sorted(),
                    "createdAt" to Timestamp.now()
                )
                transaction.set(jugadaRef, data)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            val code = (e as? FirebaseFirestoreException)?.code
            Log.e(TAG, "saveJugada error - Code: $code, Message: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }
}
