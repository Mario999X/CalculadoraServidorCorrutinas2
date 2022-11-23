package client

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Operacion
import model.mensajes.Request
import model.mensajes.Response
import mu.KotlinLogging

private val log = KotlinLogging.logger {}
private val json = Json

private lateinit var request: Request<Operacion>

fun main() = runBlocking {

    // Indicamos el Dispatcher para el Cliente
    val selectorManager = SelectorManager(Dispatchers.IO)

    // Generamos el objeto Operacion pregutando por teclado
    log.debug { "Por favor, introduzca su NOMBRE de USUARIO:" }
    val user = readln()
    log.debug { "Por favor, introduzca el PRIMER numero de su operacion:" }
    val num1 = readln().toInt()
    log.debug { "Por favor, introduzca el TIPO de OPERACION a realizar:" }
    val operador = readln().trim()
    log.debug { "Por favor, introduzca el SEGUNDO numero de su operacion:" }
    val num2 = readln().toInt()

    // Objeto a enviar
    val operacion = Operacion(user, num1, operador, num2)

    // Conectamos con el servidor
    val socket = aSocket(selectorManager).tcp().connect("localhost", 6969)
    log.debug { "Conectado a ${socket.remoteAddress}" }

    // Preparamos los canales de lectura-escritura
    val entrada = socket.openReadChannel()
    val salida = socket.openWriteChannel(true)

    // Esperamos recibir el historial por parte del servidor
    log.debug { "Esperando el historial..." }
    val responseHistorial = entrada.readUTF8Line()
    //println(responseHistorial)

    val historial = json.decodeFromString<List<Operacion>>(responseHistorial!!)
    log.debug { "Historial: $historial" }

    // Lanzamos la corrutina que envia el objeto operacion
    val envioOperacion = launch {
        log.debug { "Lanzada corrutina de envio de operaciones" }

        // Elegimos el tipo de request segun el operador introducido
        request = when (operador) {
            "+" -> {
                Request(operacion, Request.Type.SUMA)
            }

            "-" -> {
                Request(operacion, Request.Type.RESTA)
            }

            "*" -> {
                Request(operacion, Request.Type.MULTIPLICAR)
            }

            "/" -> {
                Request(operacion, Request.Type.DIVIDIR)
            }

            else -> {
                Request(operacion, Request.Type.DESCONOCIDO)
            }
        }

        // Lo preparamos, y lo mandamos como un json
        salida.writeStringUtf8(json.encodeToString(request) + "\n") // Añadimos el salto de línea para que se envíe
        log.debug { "$operacion enviada con exito, esperando solucion..." }

        // Esperamos a la respuesta del servidor, en este caso devuelve la solucion como un json
        val reciboRespuesta = entrada.readUTF8Line()

        val solucion = json.decodeFromString<Response<Int>>(reciboRespuesta!!)
        log.debug { "Resultado: ${solucion.content}" }
    }

    // Recogemos la corrutina
    envioOperacion.join()

    // Cerramos la salida y el propio socket
    log.debug { "Desconectando del servidor..." }
    withContext(Dispatchers.IO) {
        salida.close()
        socket.close()
    }

}