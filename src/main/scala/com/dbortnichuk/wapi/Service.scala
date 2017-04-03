package com.dbortnichuk.wapi

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy}
import spray.json._
import com.dbortnichuk.wapi.Protocol._


class Service(implicit val actorSystem: ActorSystem, implicit val actorMaterializer: ActorMaterializer) extends Directives {


  val webSocketRoute = (get & parameters('userName)) { userName =>
    handleWebSocketMessages(flow(userName))
  }

  val controllerActor = actorSystem.actorOf(Props(classOf[ControllerActor]))
  val userActorSource = Source.actorRef[Event](5, OverflowStrategy.fail)

  def flow(userName: String): Flow[Message, Message, Any] = Flow.fromGraph(GraphDSL.create(userActorSource) {
    implicit builder =>
      userActor =>

        import GraphDSL.Implicits._

        val materialization = builder.materializedValue.map(userActorRef => ConnectEvent(userName, userActorRef))
        val merge = builder.add(Merge[Event](2))

        val messagesToEventsFlow = builder.add(Flow[Message].collect {
          case TextMessage.Strict(eventString) => {
            getEventType(eventString) match {
              case Protocol.login => eventString.parseJson.convertTo[LoginRequestEvent]
              case Protocol.ping => PingSubjEvent(eventString.parseJson.convertTo[PingEvent], userName)
              case Protocol.subscribeTables => SubscribeSubjEvent(userName)
              case Protocol.unsubscribeTables => UnsubscribeSubjEvent(userName)
              case Protocol.addTable => AddRequestSubjEvent(eventString.parseJson.convertTo[AddRequestEvent], userName)
              case Protocol.updateTable => UpdateRequestSubjEvent(eventString.parseJson.convertTo[UpdateRequestEvent], userName)
              case Protocol.removeTable => RemoveRequestSubjEvent(eventString.parseJson.convertTo[RemoveRequestEvent], userName)
              case _ => UnknownEvent(unknownEvent, userName)
            }
          }
        })

        val eventsToMessagesFlow = builder.add(Flow[Event].map {
          case connectSuccessEvent: ConnectSuccessEvent => TextMessage(connectSuccessEvent.toJson.toString())
          case loginSuccessEvent: LoginSuccessEvent => TextMessage(loginSuccessEvent.toJson.toString())
          case loginFailureEvent: LoginFailureEvent => TextMessage(loginFailureEvent.toJson.toString())
          case logoutEvent: LogoutEvent => TextMessage(logoutEvent.toJson.toString())
          case pongEvent: PongEvent => TextMessage(pongEvent.toJson.toString)
          case listEvent: ListEvent => TextMessage(listEvent.toJson.toString)
          case unauthorizedEvent: UnauthorizedEvent => TextMessage(unauthorizedEvent.toJson.toString)
          case addSuccessEvent: AddSuccessEvent => TextMessage(addSuccessEvent.toJson.toString)
          case updateSuccessEvent: UpdateSuccessEvent => TextMessage(updateSuccessEvent.toJson.toString)
          case removeSuccessEvent: RemoveSuccessEvent => TextMessage(removeSuccessEvent.toJson.toString)
          case addFailureEvent: AddFailureEvent => TextMessage(addFailureEvent.toJson.toString)
          case updateFailureEvent: UpdateFailureEvent => TextMessage(updateFailureEvent.toJson.toString)
          case removeFailureEvent: RemoveFailureEvent => TextMessage(removeFailureEvent.toJson.toString)
          case unknownEvent: UnknownEvent => TextMessage(unknownEvent.toJson.toString)
        })

        val controllerActorSink = Sink.actorRef[Event](controllerActor, LogoutEvent(logout, userName))

        materialization ~> merge.in(1)
        messagesToEventsFlow ~> merge.in(0)
        merge ~> controllerActorSink

        userActor ~> eventsToMessagesFlow

        FlowShape(messagesToEventsFlow.in, eventsToMessagesFlow.out)
  })

  def getEventType(eventString: String): String = {
    val baseEvent: BaseEvent =
      try {
        eventString.parseJson.convertTo[BaseEvent]
      } catch {
        case e: Exception => BaseEvent("unknown")
      }
    baseEvent.$type
  }
}


