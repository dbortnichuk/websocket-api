package com.dbortnichuk.wapi.test

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import org.scalatest.{FunSuite, Matchers}
import com.dbortnichuk.wapi.Service

class ServerTest extends FunSuite with Matchers with ScalatestRouteTest {

  test("should create empty service") {
    new Service()
  }

  test("should be able to connect to service web socket") {
    assertWebsocket("arny") { wsClient =>
      isWebSocketUpgrade shouldEqual true
    }
  }

  test("should be able to connect and login") {
    assertWebsocket("arny") { wsClient =>
      wsClient.expectMessage("""{"$type":"connect_success"}""")
      wsClient.sendMessage("""{ "$type": "login", "userName": "arny", "password": "pass123" }""")
      wsClient.expectMessage("""{"$type":"login_successful","userName":"arny","userType":"admin"}""")
    }
  }

  test("admin user should be able to connect, login and remove table") {
    assertWebsocket("arny") { wsClient =>
      wsClient.expectMessage("""{"$type":"connect_success"}""")
      wsClient.sendMessage("""{ "$type": "login", "userName": "arny", "password": "pass123" }""")
      wsClient.expectMessage("""{"$type":"login_successful","userName":"arny","userType":"admin"}""")
      wsClient.sendMessage("""{"$type": "remove_table","idx": 1}""")
      wsClient.expectMessage("""{"$type":"table_removed","idx":1}""")
    }
  }

  test("not authorized user should not be able to remove table") {
    assertWebsocket("sly") { wsClient =>
      wsClient.expectMessage("""{"$type":"connect_success"}""")
      wsClient.sendMessage("""{ "$type": "login", "userName": "sly", "password": "pass123" }""")
      wsClient.expectMessage("""{"$type":"login_successful","userName":"sly","userType":"user"}""")
      wsClient.sendMessage("""{"$type": "remove_table","idx": 1}""")
      wsClient.expectMessage("""{"$type":"not_authorized"}""")
    }
  }

  test("subscribed user should see changes done to table") {
    val service = new Service()
    val adminClient = WSProbe()
    val userClient = WSProbe()
    WS(s"/?userName=arny", adminClient.flow) ~> service.webSocketRoute ~> check {
      adminClient.expectMessage("""{"$type":"connect_success"}""")
      adminClient.sendMessage("""{ "$type": "login", "userName": "arny", "password": "pass123" }""")
      adminClient.expectMessage("""{"$type":"login_successful","userName":"arny","userType":"admin"}""")
      adminClient.sendMessage("""{"$type": "remove_table","idx": 1}""")
      adminClient.expectMessage("""{"$type":"table_removed","idx":1}""")
    }
    WS(s"/?userName=sly", userClient.flow) ~> service.webSocketRoute ~> check {
      userClient.expectMessage("""{"$type":"connect_success"}""")
      userClient.sendMessage("""{ "$type": "login", "userName": "sly", "password": "pass123" }""")
      userClient.expectMessage("""{"$type":"login_successful","userName":"sly","userType":"user"}""")
      userClient.sendMessage("""{ "$type": "subscribe_tables" }""")
      userClient.expectMessage("""{"$type":"table_list","tables":[{"id":"13ff3e5b-7264-429b-91ba-7f23d0634396","name":"table - Terminator","participants":4},{"id":"6d325792-237e-45b7-a7fd-59b8f990e17a","name":"table - Kickboxer","participants":2}]}""")
    }

  }

  def assertWebsocket(userName: String)(assertions: (WSProbe) => Unit): Unit = {
    val service = new Service()
    val wsClient = WSProbe()
    WS(s"/?userName=$userName", wsClient.flow) ~> service.webSocketRoute ~> check(assertions(wsClient))
  }

}
