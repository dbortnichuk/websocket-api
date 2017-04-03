package com.dbortnichuk.wapi

import akka.actor.ActorRef
import spray.json.{DefaultJsonProtocol}

object Protocol {

  val connect = "connect"
  val connectSuccessful = "connect_success"
  val login = "login"
  val loginSuccessful = "login_successful"
  val loginFailed = "login_failed"
  val logout = "logout"
  val ping = "ping"
  val pong = "pong"
  val tableList = "table_list"
  val subscribeTables = "subscribe_tables"
  val unsubscribeTables = "unsubscribe_tables"
  val notAuthenticated = "not_authenticated"
  val unauthorized = "not_authorized"
  val addTable = "add_table"
  val updateTable = "update_table"
  val removeTable = "remove_table"
  val addedTable = "table_added"
  val updatedTable = "table_updated"
  val removedTable = "table_removed"
  val additionFailed = "addition_failed"
  val updateFailed = "update_failed"
  val removalFailed = "removal_failed"
  val unknownEvent = "unknown_event"

  sealed trait Event

  case class BaseEvent($type: String) extends Event
  object BaseEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(BaseEvent.apply)
  }

  case class ConnectEvent(userName: String, userActorRef: ActorRef) extends Event


  case class ConnectSuccessEvent($type: String) extends Event
  object ConnectSuccessEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(ConnectSuccessEvent.apply)
  }

  case class LoginRequestEvent($type: String, userName: String, password: String) extends Event
  object LoginRequestEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(LoginRequestEvent.apply)
  }
  case class LoginSuccessEvent($type: String, userName: String, userType: String) extends Event
  object LoginSuccessEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(LoginSuccessEvent.apply)
  }


  case class LoginFailureEvent($type: String) extends Event
  object LoginFailureEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(LoginFailureEvent.apply)
  }



  case class LogoutEvent($type: String, userName: String) extends Event
  object LogoutEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(LogoutEvent.apply)
  }


  case class PingEvent($type: String, seq: Int) extends Event
  object PingEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(PingEvent.apply)
  }

  case class PingSubjEvent(pingEvent: PingEvent, userName: String) extends Event

  case class PongEvent($type: String, seq: Int) extends Event
  object PongEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(PongEvent.apply)
  }

  case class UnknownEvent($type: String, userName: String) extends Event
  object UnknownEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(UnknownEvent.apply)
  }

  case class SubscribeEvent($type: String) extends Event
  object SubscribeEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(SubscribeEvent.apply)
  }

  case class SubscribeSubjEvent(userName: String) extends Event

  case class UnsubscribeEvent($type: String) extends Event
  object UnsubscribeEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(UnsubscribeEvent.apply)
  }

  case class UnsubscribeSubjEvent(userName: String) extends Event

  case class Row(id: String, name: String, participants: Int)
  object Row extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(Row.apply)
  }

  case class SubRow(name: String, participants: Int)
  object SubRow extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(SubRow.apply)
  }

  case class ListEvent($type: String, tables: List[Row]) extends Event
  object ListEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(ListEvent.apply)
  }

  case class AddRequestEvent($type: String, afterId: Int, table: SubRow) extends Event
  object AddRequestEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(AddRequestEvent.apply)
  }

  case class AddRequestSubjEvent(addRequestEvent: AddRequestEvent, userName: String) extends Event

  case class UpdateRequestEvent($type: String, idx: Int, table: Row) extends Event
  object UpdateRequestEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(UpdateRequestEvent.apply)
  }

  case class UpdateRequestSubjEvent(updateRequestEvent: UpdateRequestEvent, userName: String) extends Event

  case class RemoveRequestEvent($type: String, idx: Int) extends Event
  object RemoveRequestEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(RemoveRequestEvent.apply)
  }

  case class RemoveRequestSubjEvent(removeRequestEvent: RemoveRequestEvent, userName: String) extends Event

  case class AddFailureEvent($type: String, id: Int, reason: String) extends Event
  object AddFailureEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(AddFailureEvent.apply)
  }

  case class UpdateFailureEvent($type: String, idx: Int, reason: String) extends Event
  object UpdateFailureEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(UpdateFailureEvent.apply)
  }

  case class RemoveFailureEvent($type: String, idx: Int, reason: String) extends Event
  object RemoveFailureEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(RemoveFailureEvent.apply)
  }

  case class AddSuccessEvent($type: String, idx: Int, table: Row) extends Event
  object AddSuccessEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(AddSuccessEvent.apply)
  }

  case class UpdateSuccessEvent($type: String, table: Row) extends Event
  object UpdateSuccessEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(UpdateSuccessEvent.apply)
  }

  case class RemoveSuccessEvent($type: String, idx: Int) extends Event
  object RemoveSuccessEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(RemoveSuccessEvent.apply)
  }

  case class UnauthorizedEvent($type: String) extends Event
  object UnauthorizedEvent extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(UnauthorizedEvent.apply)
  }

  case class User(name: String, password: String, role: String)

  //case class UserSession(user: User, actor: ActorRef)
  case class UserSession(user: User, actor: ActorRef, authenticated: Boolean)

  case object CleanNotAuthenticated


}
