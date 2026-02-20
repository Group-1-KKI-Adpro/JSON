# JSON – Jastip Online Nasional
Advanced Programming – Group Preparation

## Group Information
- Group Name: JSON – Jastip Online Nasional
- Group ID: Group-1-KKI-Adpro

### Group Members & Module PICs
- Authentication & Profile: Tara Nirmala Anwar (2406365276)
- Inventory & Catalog: Evelyne Octaviana Benedicta Aritonang (2406365282)
- Order: Sultanadika Shidqi M (2406365326)
- Wallet & Transaction: Muhammad Rifqi AlGhan (2406365396)
- Voucher & Promo: Melanton Gabriel Siregar (2406365364)

## Project Overview
JSON (Jastip Online Nasional) is a consignment and limited-goods sales platform that connects travelers (Jastipers) and buyers (Titipers).

This repository demonstrates the Group Preparation milestone, focusing on:
- System architecture planning
- Module responsibility distribution
- Backend–database integration
- CI/CD readiness

## Tech Stack
- Java 21
- Spring Boot
- H2 Database (in-memory)
- Gradle
- GitHub Actions (CI + Dummy CD)

## How to Run Locally
Run:
./gradlew bootRun

Then open:
http://localhost:8080/api/health

Expected response example:
JSON backend alive. userCount=1

This endpoint demonstrates backend and database integration using H2 and JPA.
The repository also includes a basic landing page setup for frontend integration.

## CI/CD
- Continuous Integration (CI) is implemented using GitHub Actions.
- CI runs automatically on every push and pull request.
- CI executes automated tests using: ./gradlew test
- A dummy Continuous Deployment (CD) job is implemented.
- The dummy CD job is guarded by CI and only runs if CI passes successfully.

## Deployment
Deployment URL is not provided for this submission due to AWS Education license delay, as stated in the course announcement.
Deployment preparation will be performed at the 25% milestone.

## Milestones & Work Plan

Preparation
- Requirement analysis & system scope definition — All members
- Tech stack decision & repository setup — Tara
- CI/CD setup & backend + DB integration demo — Tara
- Landing page / frontend setup — Muhammad Rifqi AlGhan

25%
- Authentication & Profile implementation — Tara
- Inventory & Catalog structure — Evelyn
- Order flow design — Sultanadika
- Wallet balance logic — Muhammad Rifqi AlGhan
- Voucher data model — Melanton

50%
- Inventory CRUD & stock validation
- Order lifecycle implementation
- Wallet payment & refund logic
- Voucher validation
- Integration testing

75%
- Concurrency handling (flash sale / war scenario)
- Rating & reputation system
- Refactoring & architectural improvements

100% (Final)
- Full system integration
- Performance optimization
- Final testing & presentation

## Repository
This repository can be run locally by the teaching assistant and already demonstrates:
- Backend
- Frontend (landing page)
- Database integration
- CI with quality checks
- Dummy CD guarded by CI
