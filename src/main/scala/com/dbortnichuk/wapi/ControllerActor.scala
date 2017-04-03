package com.dbortnichuk.wapi

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import com.dbortnichuk.wapi.Protocol._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ControllerActor extends Actor {

  private[this] val userDatabase = Map(
    "arny" -> User("arny", "pass123", "admin"),
    "sly" -> User("sly", "pass123", "user"),
    "jcvd" -> User("jcvd", "pass123", "user"))

  private[this] val database = ArrayBuffer[Row]()


  private[this] var sessions = collection.mutable.LinkedHashMap[String, UserSession]()
  private[this] val databaseSubscribers = collection.mutable.LinkedHashMap[String, UserSession]()

  private implicit val executionContext: ExecutionContext = context.dispatcher
  context.system.scheduler.schedule(20.minute, 10.minute, self, CleanNotAuthenticated)


  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    database.appendAll(List(
      Row("13ff3e5b-7264-429b-91ba-7f23d0634396", "table - Terminator", 4),
      Row("d14b0322-4839-4f35-8665-e96facd49a05", "table - Rokky", 6),
      Row("6d325792-237e-45b7-a7fd-59b8f990e17a", "table - Kickboxer", 2)))
  }

  override def receive: Receive = {

    case ConnectEvent(uName, uActorRef) => {
      sessions += (uName -> UserSession(User(uName, "", ""), uActorRef, authenticated = false))
      uActorRef ! ConnectSuccessEvent(connectSuccessful)
    }

    case LoginRequestEvent(_, uName, uPass) => {
      userDatabase.get(uName) match {
        case None => {
          val unauthenticatedUserSession = sessions.get(uName)
          unauthenticatedUserSession match {
            case None => println(s"$uName session not found")
            case Some(session) => session.actor ! LoginFailureEvent(loginFailed)
          }
        }
        case Some(user) => {
          val unauthenticatedUserSession = sessions.get(uName)
          unauthenticatedUserSession match {
            case None => println(s"$uName session not found")
            case Some(session) => {
              if (user.password == uPass) {
                sessions += (uName -> UserSession(user, session.actor, authenticated = true))
                broadcastToAll(LoginSuccessEvent(loginSuccessful, user.name, user.role))
              } else {
                session.actor ! LoginFailureEvent(loginFailed)
              }
            }
          }
        }
      }
    }
    case LogoutEvent(t, uName) => {
      sessions.get(uName) match {
        case None => println(s"$uName session not found")
        case Some(session) => {
          if (session.authenticated) broadcastToAll(LogoutEvent(t, uName))
          sessions -= uName
        }
      }
    }
    case PingSubjEvent(pingEvent, uName) => {
      sessions(uName).actor ! PongEvent(pong, pingEvent.seq)
    }
    case SubscribeSubjEvent(uName) => {
      sessions.get(uName) match {
        case None => println(s"$uName session not found")
        case Some(session) => {
          if (!session.authenticated) session.actor ! UnauthorizedEvent(notAuthenticated)
          else {
            databaseSubscribers += (uName -> session)
            session.actor ! ListEvent(tableList, database.toList)
          }
        }
      }


    }
    case UnsubscribeSubjEvent(uName) => {
      sessions.get(uName) match {
        case None => println(s"$uName session not found")
        case Some(session) => {
          if (!session.authenticated) session.actor ! UnauthorizedEvent(notAuthenticated)
          else {
            databaseSubscribers -= uName
          }
        }
      }
    }
    case AddRequestSubjEvent(addRequestEvent, uName) => {
      sessions.get(uName) match {
        case None => println(s"$uName session not found")
        case Some(session) => {
          if (!session.authenticated) session.actor ! UnauthorizedEvent(notAuthenticated)
          else {
            if (isAuthorised(session.user)) {
              val afterId = addRequestEvent.afterId
              var requiredPos = afterId + 1
              //amend insertion index to fit the range 0 - length to avoid IndexOutOfBounds
              if (requiredPos < 0) requiredPos = 0
              if (requiredPos > database.length) requiredPos = database.length

              val row = Row(getUUID, addRequestEvent.table.name, addRequestEvent.table.participants)
              database.insert(requiredPos, row)
              val addSuccessEvent = AddSuccessEvent(addedTable, requiredPos, row)
              session.actor ! addSuccessEvent
              broadcastToSubscribers(addSuccessEvent, Some(uName))
            } else session.actor ! UnauthorizedEvent(unauthorized)
          }
        }
      }
    }
    case UpdateRequestSubjEvent(updateRequestEvent, uName) => {
      sessions.get(uName) match {
        case None => println(s"$uName session not found")
        case Some(session) => {
          if (!session.authenticated) session.actor ! UnauthorizedEvent(notAuthenticated)
          else {
            if (isAuthorised(session.user)) {
              val index = updateRequestEvent.idx
              if (!isValidIndex(index)) session.actor ! UpdateFailureEvent(updateFailed, index, "out of bounds")
              database.update(index, updateRequestEvent.table)
              val updateSuccessEvent = UpdateSuccessEvent(updatedTable, updateRequestEvent.table)
              session.actor ! updateSuccessEvent
              broadcastToSubscribers(updateSuccessEvent, Some(uName))
            } else session.actor ! UnauthorizedEvent(unauthorized)
          }
        }
      }
    }
    case RemoveRequestSubjEvent(removeRequestEvent, uName) => {
      sessions.get(uName) match {
        case None => println(s"$uName session not found")
        case Some(session) => {
          if (!session.authenticated) session.actor ! UnauthorizedEvent(notAuthenticated)
          else {
            if (isAuthorised(session.user)) {
              val index = removeRequestEvent.idx
              if (!isValidIndex(index)) session.actor ! RemoveFailureEvent(removalFailed, index, "out of bounds")
              database.remove(index)
              val removeSuccessEvent = RemoveSuccessEvent(removedTable, index)
              session.actor ! removeSuccessEvent
              broadcastToSubscribers(removeSuccessEvent, Some(uName))
            } else session.actor ! UnauthorizedEvent(unauthorized)
          }
        }
      }
    }


    case UnknownEvent(ue, uName) => {
      sessions(uName).actor ! UnknownEvent(ue, uName)
    }

    case CleanNotAuthenticated => sessions = sessions.filter(s => s._2.authenticated)
  }

  private[this] def broadcastToAll(event: Event): Unit = {
    broadcast(sessions, event)
  }

  private[this] def broadcastToSubscribers(event: Event, excludeUserName: Option[String] = None): Unit = {
    excludeUserName match {
      case None => broadcast(databaseSubscribers, event)
      case Some(uName) => broadcast(databaseSubscribers.filter(t => t._1 != uName), event)
    }
  }

  private[this] def broadcast(subscribers: mutable.Map[String, UserSession], event: Event): Unit = {
    subscribers.values.foreach(session => session.actor ! event)
  }

  private[this] def isAuthorised(user: User): Boolean = user.role == "admin"

  private[this] def isValidIndex(idx: Int): Boolean = if (idx >= 0 && idx < database.length) true else false

  private[this] def getUUID = UUID.randomUUID.toString


}


