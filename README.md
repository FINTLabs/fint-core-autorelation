# fint-core-autorelation

## Overview
`fint-core-autorelation` is a backend service responsible for automatically maintaining and updating relations between entities in the FINT Core ecosystem.  
It listens to entity events published by providers, determines which relations are affected, and issues corresponding updates that other services can consume.

This service ensures that relational integrity across entities remains consistent and up to date without requiring manual intervention.

---

## How It Works

### 1. Listening to Entity Topics
The service subscribes to Kafka topics containing events for all entities provided by a FINT provider.  
Each event represents a change to an entity (for example: creation, update, or deletion).

### 2. Relation Detection
Upon receiving an event, `fint-core-autorelation` analyzes the entity data and determines which related entities may need to have their relations updated.  
This process involves:
- Understanding entity structures and relation metadata.
- Evaluating which relations are impacted by the new or changed entity.
- Preparing a standardized `RelationUpdate` event describing the necessary modifications.

### 3. Publishing Relation Updates
Once relations to update have been identified, the service produces a **`RelationUpdate`** message to Kafka.  
This message contains the required details for synchronizing relational references between entities.

### 4. Downstream Consumption
The **`fint-core-consumer`** service subscribes to the same Kafka topic and handles these relation updates.  
It applies the relation changes directly to the appropriate resources, ensuring that all data references remain correct and current.

---

## Data Flow

```text
 ┌──────────────────────┐
 │  FINT Provider       │
 │  (Entity Topics)     │
 └──────────┬───────────┘
            │
            ▼
 ┌──────────────────────┐
 │  fint-core-autorelation
 │  - Listens to entity topics
 │  - Detects affected relations
 │  - Publishes RelationUpdate
 └──────────┬───────────┘
            │
            ▼
 ┌──────────────────────┐
 │  fint-core-consumer  │
 │  - Consumes RelationUpdate
 │  - Updates related resources
 └──────────────────────┘
