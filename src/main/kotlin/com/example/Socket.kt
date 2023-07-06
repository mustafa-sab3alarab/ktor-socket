package com.example

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun Routing.socket() {

    route("/chat") {
        val connections = Collections.synchronizedSet<Connection?>(mutableSetOf())

        webSocket("/connect") {

            val thisConnection = Connection(this)
            if (connections.size < 2) {
                connections += thisConnection
            } else {
                send("Chat is full. You cannot join at the moment.")
                close(CloseReason(CloseReason.Codes.NORMAL, "Chat is full."))
                return@webSocket
            }

            try {
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    val textWithUsername = "[${thisConnection.name}]: $receivedText"
                    connections.forEach {
                        it.session.send(textWithUsername)
                    }
                }
            } catch (e: Throwable) {
                throw e
            } finally {
                println("Removing $thisConnection!")
                connections -= thisConnection
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "no session."))
            }
        }
    }
}


class Connection(val session: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicInteger(0)
    }

    val name = "user${lastId.getAndIncrement()}"
}
