
###Custom WebSocket server

run server
```
sbt run
```
connect
```
ws://localhost:9999/?userName=arny
```

login message
```
{ "$type": "login", "userName": "arny", "password": "pass123" }
```
userName for connect and login must match,

login is required, otherwise - not authenticated

registered users:
```
User("arny", "pass123", "admin"),
User("sly", "pass123", "user"),
User("jcvd", "pass123", "user"))
```

ping
```
{ "$type": "ping", "seq": 1 }
```

subscribe to tables
```
{ "$type": "subscribe_tables" }
```
unsubscribe from tables
```
{ "$type": "unsubscribe_tables" }
```

add table
```
{"$type": "add_table","afterId": 1,"table": {"name": "table - Foo Fighters","participants": 4}}
```

update table
```
{"$type": "update_table","idx": 1,"table": {"id":"90055284-5f13-41cd-a3fe-68b2e90ee8c3","name":"table - Foo Fighters","participants":4}}
```

remove table
```
{"$type": "remove_table","idx": 1}
```

###Comments
As far as I understand task suggests client with UI availability as part of server implementation.
Haven't implemented client due to lack of time.
Any specification for UI part ? 