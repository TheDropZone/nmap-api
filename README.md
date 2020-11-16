# nmap-api
A scalable lightweight nmap service.

This application is design for deployment in ECS and for task ingestion through an ActiveMQ
queue. The application scales up and down depending on queue demand and is capable of 
transactionally safe, batch processing of queued tasks.
