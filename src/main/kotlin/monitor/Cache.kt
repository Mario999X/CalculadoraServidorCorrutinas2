package monitor

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import model.Operacion
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class Cache(
    private val limiteHistorial: Int = 3
) {
    // Lista donde se guardan las operaciones / Historial
    private val listaOperaciones = mutableListOf<Operacion>()

    // Lock + Condiciones
    private val lock = Mutex()

    // Obtenemos la lista de operaciones
    suspend fun get(): List<Operacion> {
        // Mientras haya un escritor pendiente
        lock.withLock {
            val operaciones = listaOperaciones

            log.debug { "\tSe envia el historial..." }

            return operaciones
        }
    }

    suspend fun put(item: Operacion) {
        lock.withLock {
            // Si se llega al limite, se elimina el primero de la lista
            if (listaOperaciones.size == limiteHistorial) {
                listaOperaciones.removeFirst()

                log.debug { "\t-Limite alcanzado, se elimina una operacion" }
            }
            // Agregamos la operacion
            listaOperaciones.add(item)

            log.debug { "\tOperacion introducida..." }
            //println(listaOperaciones)
        }
    }
}