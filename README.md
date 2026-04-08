# README.md

## 1. 프로젝트 개요

본 repository는 AI Agent 개발 환경의 성능을 평가하기 위한 기준 문서, 실행 구조, 제품 코드 저장소를 포함한다.

이 프로젝트의 목적은 다음과 같다.

- 사용자가 Orchestrator에게 기능 구현을 요청한다.
- Orchestrator는 Claude Agent SDK 기반 Agent를 활용하여 요청을 분석한다.
- Agent는 Java 기반 Linux daemon을 구현, 테스트, 리뷰한다.
- Orchestrator는 결과를 종합하여 사용자에게 보고한다.
- 이 전체 개발 루프의 품질과 효율을 평가한다.

본 repository는 단순한 Java 프로젝트가 아니라, **AI Agent 기반 개발 환경과 그 평가 체계**를 함께 관리하는 저장소이다.

---

## 2. 프로젝트 목표

현재 프로젝트의 목표는 다음과 같다.

- Harness 기반의 Agent 운영 구조를 정의한다.
- Orchestrator와 Agent의 역할을 문서로 고정한다.
- Java daemon 개발을 수행할 수 있는 repository 구조를 마련한다.
- 실제 기능 요청을 기준으로 개발, 테스트, 리뷰, 보고 루프를 수행할 수 있도록 준비한다.
- 전체 환경의 성능을 평가할 수 있는 기준을 수립한다.

---

## 3. 기술 전제

현재 기준 기술 전제는 다음과 같다.

- 주요 LLM: Claude
- Agent 생성 방식: Claude Agent SDK
- 형상관리: Git
- 제품 개발 언어: Java
- 실행 대상: Ubuntu 20.04 서버에서 동작하는 daemon
- 제품 실행 형태: Plain Java Service
- 빌드 도구: Maven
- 설정 파일 형식: `.properties`
- 저장소: MinIO

---

## 4. 저장소 구조

현재 기준 저장소 구조는 다음과 같다.

```text
repo-root/
├─ AGENTS.md
├─ ORCHESTRATOR.md
├─ HARNESS.md
├─ PROJECT_STATE.md
├─ EVALS.md
├─ README.md
├─ docs/
│  └─ project/
│     ├─ PRD.md
│     └─ ARCHITECTURE.md
├─ daemon/
│  ├─ pom.xml
│  ├─ src/
│  │  ├─ main/java/
│  │  └─ test/java/
│  └─ scripts/
├─ eval/
└─ agent-runtime/
```

## 5. 주요 문서 안내

운영문서

- `AGENTS.md` : Agent의 역할과 책임을 정의한다.
- `ORCHESTRATOR.md` : 요청 분석, 작업 분해, 배정, 상태 추적, 보고 기준을 정의한다.
- `HARNESS.md` : 실제 실행 규칙과 작업 수행 원칙을 정의한다.
- `PROJECT_STATE.md` : 현재 프로젝트의 확정 사항, 제약, 진행 상태를 기록한다.
- `EVALS.md` : 성능 평가 기준과 결과 기록 방식을 정의한다.

제품 기준 문서

- `docs/project/PRD.md` : 대상 daemon의 요구사항, 범위, 수용 기준, 미정 사항을 정의한다.
- `docs/project/ARCHITECTURE.md` : 대상 daemon의 구성요소, 데이터 흐름, 설계 규칙, 제약을 정의한다.

## 6. 핵심 개념

- User : 기능 요청을 제시하고 결과를 확인하는 주체이다.
- Orchestrator : 사용자 요청을 분석하고, 작업을 분해하고, Agent를 배정하며, 결과를 종합해 보고하는 관리 주체이다.
- Agents : 분석, 구현, 테스트, 리뷰 작업을 수행하는 실행 주체이다. 현재 기본 Agent는 다음과 같다.

    a) Planner Agent
    b) Implementation Agent
    c) Test Agent
    d) Review Agent

- Harness : Orchestrator와 Agent가 어떤 절차와 규칙으로 일할지 정의하는 실행 프레임이다.
- Product : Agent가 개발하는 대상 제품은 Linux 서버에서 동작하는 Java daemon이다.

## 7. 작업 흐름 요약

기본 작업 흐름은 다음과 같다.

    a) 사용자 요청 접수
    b) 요청 분석
    c) 작업 범위 정의
    d) 작업 분해
    e) Agent 배정
    f) 구현 수행
    g) 테스트 수행
    h) 리뷰 수행
    i) 결과 종합
    j) 사용자 보고

상세 작업 기준은 `AGENTS.md`, `ORCHESTRATOR.md`, `HARNESS.md`를 따른다.

## 8. Java daemon 구조

제품 코드는 `daemon/` 디렉터리 아래에서 관리한다.

현재 기준 구조는 다음과 같다.

```text
daemon/
├─ pom.xml
├─ src/
│    ├─ main/java/
│    └─ test/java/
└─ scripts/
```
빌드 도구는 Maven을 사용한다.
daemon 내부의 package / module 구조와 scripts/ 상세 구성은 후속 작업에서 확정한다.

## 9. 시작 방법

### 9.1 문서부터 확인할 경우

다음 순서로 읽는 것을 권장한다.

    a) `README.md`
    b) `PROJECT_STATE.md`
    c) `AGENTS.md`
    d) `ORCHESTRATOR.md`
    e) `HARNESS.md`
    f) `docs/project/PRD.md`
    g) `docs/project/ARCHITECTURE.md`
    h) `EVALS.md`

### 9.2 제품 코드 기준으로 시작할 경우

다음 경로를 우선 확인한다.

- `daemon/pom.xml`
- `daemon/src/main/java/`
- `daemon/src/test/java/`
- `daemon/scripts/`

## 10. 로컬 실행 및 빌드

현재 저장소는 문서와 기준 구조를 먼저 정리하는 단계에 있다.  
실행 스크립트와 패키징 스크립트의 최종 형태는 후속 작업에서 확정한다.

Maven 기준으로는 `daemon/` 디렉터리에서 빌드와 테스트를 수행하는 구조를 전제로 한다.

배포 및 실행 방식의 상세 기준은 후속 작업에서 구체화한다.

## 11. 평가 기준

본 프로젝트는 단순 코드 생성이 아니라 AI Agent 개발 환경 전체의 운영 성능을 평가한다. 평가 항목 예시는 다음과 같다.

- 요구사항 해석 품질
- 구현 정확도
- 테스트 수행 품질
- 리뷰 및 보고 품질
- 재작업 빈도
- 차단 발생 여부
- Linux daemon 관점의 운영 적합성

상세 기준은 `EVALS.md`를 따른다.

## 12. 현재 상태

현재 프로젝트는 운영 문서와 제품 기준 문서를 우선 정리하는 단계에 있다.  
현재 완료 또는 정리된 항목은 다음과 같다.

- Agent 역할 정의
- Orchestrator 운영 기준 정의
- Harness 실행 규칙 정의
- 프로젝트 상태 문서 정리
- 대상 daemon의 요구사항 문서 정리
- 대상 daemon의 아키텍처 문서 정리

이후 단계에서는 repository 구조 보강, daemon package / module 구조 확정, Maven 기준 초기 프로젝트 뼈대 설계, 실제 기능 요청 기반 첫 개발 사이클 수행이 이어진다.

## 13. 유지 원칙

다음 경우 본 문서를 갱신한다.

- 프로젝트 목표가 변경된 경우
- 저장소 구조가 변경된 경우
- 핵심 문서 구성이 변경된 경우
- 빌드 및 실행 방식이 변경된 경우
- 제품 또는 Agent 운영 구조가 변경된 경우

README는 저장소의 입구 문서이므로, 현재 구조와 맞지 않으면 우선적으로 갱신한다.
