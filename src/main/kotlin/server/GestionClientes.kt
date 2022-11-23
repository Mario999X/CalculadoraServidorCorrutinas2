package server

import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Operacion
import model.mensajes.Request
import model.mensajes.Response
import monitor.Cache
import mu.KotlinLogging

private val log = KotlinLogging.logger {}
private val json = Json

class GestionClientes(private val socket: Socket, private val cache: Cache) {

    private var result = 0

    // Canal de entrada y de salida
    private val entrada = socket.openReadChannel()
    private val salida = socket.openWriteChannel(true) // true, para que se envíe el dato al instante

    suspend fun run() = withContext(Dispatchers.IO) { // Importante pasar el contexto

        // --- HISTORIAL DE OPERACIONES AL CLIENTE ---
        log.debug { "Obteniendo el historial de operaciones..." }
        val historial = cache.get()

        // Enviamos al cliente el historial
        log.debug { "Enviando historial" }
        val enviarHistorial = json.encodeToString(historial) + "\n" // Añadimos el salto de línea para que se envíe

        salida.writeStringUtf8(enviarHistorial)

        // --- OPERACION CLIENTE ---
        val procesarOperacion = launch {
            log.debug { "Recibiendo operacion" }

            // Recibimos el dato del cliente
            val input = entrada.readUTF8Line()
            log.debug { "Se ha recibido un mensaje: $input" }

            // json recogido
            input?.let {
                // Lo decodificamos
                val request = json.decodeFromString<Request<Operacion>>(input)
                // Recogemos el contenido y lo casteamos a Operacion
                val op = request.content as Operacion

                // Segun el tipo de request, se ejecutara una parte del codigo o otra (solo 1 caso en este problema)
                result = when (request.type) {
                    Request.Type.SUMA -> op.num1 + op.num2
                    Request.Type.RESTA -> op.num1 - op.num2
                    Request.Type.MULTIPLICAR -> op.num1 + op.num2
                    Request.Type.DIVIDIR -> if (op.num2 > 0) op.num1 / op.num2 else 0
                    Request.Type.DESCONOCIDO -> 0
                }
                // Agregamos la operacion a la cache
                cache.put(op)
            }
            // Se obtiene el resultado, y es enviado en un String por el canal de salida
            log.debug { "Resultado obtenido: $result" }

            val response = Response(result, Response.Type.OK)
            val sendSolucion = json.encodeToString(response) + "\n"

            salida.writeStringUtf8(sendSolucion)
            log.debug { "Resultado enviado" }
        }

        // Terminamos la corrutina y cerramos lo necesario.
        procesarOperacion.join()

        log.debug { "Cerrando conexion" }
        withContext(Dispatchers.IO) {
            salida.close()
            socket.close()
        }
    }
}