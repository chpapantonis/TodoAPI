
package chpapantonis.routes

import chpapantonis.API_VERSION
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters

import chpapantonis.auth.MySession
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.locations.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.post
import io.ktor.server.sessions.*
import repository.Repository

const val TODOS = "$API_VERSION/todos"

@Location(TODOS)
class TodoRoute

fun Route.todos(db: Repository) {
    authenticate("jwt") {
        post<TodoRoute> {
            val todosParameters = call.receive<Parameters>()
            val todo =
                todosParameters["todo"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing Todo")
            val done =
                todosParameters["done"] ?: "false"
            val user = call.sessions.get<MySession>()?.let { db.findUser(it.userId) }
            if (user == null) {
                call.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
                return@post
            }

            try {
                val currentTodo = db.addTodo(user.userId, todo, done.toBoolean())
                currentTodo?.id?.let {
                    call.respond(HttpStatusCode.OK, currentTodo)
                }
            } catch (e: Throwable) {
                application.log.error("Failed to add todo", e)
                call.respond(HttpStatusCode.BadRequest, "Problems Saving Todo")
            }
        }
        get<TodoRoute> {
            val user = call.sessions.get<MySession>()?.let { db.findUser(it.userId) }
            if (user == null) {
                call.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
                return@get
            }

            val todosParameters = call.request.queryParameters
            val limit = if (todosParameters.contains("limit")) todosParameters["limit"] else null
            val offset = if (todosParameters.contains("offset")) todosParameters["offset"] else null
            try {
                if (limit != null && offset != null) {
                    val todos = db.getTodos(user.userId, offset.toInt(), limit.toInt())
                    call.respond(todos)

                } else {
                    val todos = db.getTodos(user.userId)
                    call.respond(todos)
                }
            } catch (e: Throwable) {
                application.log.error("Failed to get Todos", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting Todos")
            }
        }
        delete<TodoRoute> {
            val todosParameters = call.receive<Parameters>()
            if (!todosParameters.contains("id")) {
                return@delete call.respond(HttpStatusCode.BadRequest, "Missing Todo Id")
            }
            val todoId =
                todosParameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing Todo Id")
            val user = call.sessions.get<MySession>()?.let { db.findUser(it.userId) }
            if (user == null) {
                call.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
                return@delete
            }

            try {
                db.deleteTodo(user.userId, todoId.toInt())
                call.respond(HttpStatusCode.OK)
            } catch (e: Throwable) {
                application.log.error("Failed to delete todo", e)
                call.respond(HttpStatusCode.BadRequest, "Problems Deleting Todo")
            }
        }
    }
}
