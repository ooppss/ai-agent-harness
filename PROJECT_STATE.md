# PROJECT_STATE.md

## 1. 문서 목적

본 문서는 현재 프로젝트의 목표, 확정 사항, 제약 사항, 진행 상태, 미정 사항을 기록하는 상태 기준 문서이다.

본 문서의 목적은 다음과 같다.

- Orchestrator와 Agent가 현재 프로젝트 상태를 동일하게 이해하도록 한다.
- 확정된 사항과 미정 사항을 구분한다.
- 작업 시작 전 반드시 확인해야 할 현재 기준을 제공한다.
- 중복 논의와 잘못된 가정을 줄인다.
- 후속 작업의 우선순위와 방향을 고정한다.

본 문서는 설계 문서 전체를 대체하지 않으며, 현재 시점의 프로젝트 상태를 유지·관리하기 위한 기준 문서로 사용한다.

---

## 2. 프로젝트 개요

본 프로젝트는 AI Agent 개발 환경의 성능 평가를 목적으로 한다.

사용자는 Orchestrator에게 기능 구현을 요청한다.  
Orchestrator는 Claude Agent SDK 기반 Agent를 활용하여 요청을 분석하고, 구현, 테스트, 리뷰를 수행한 뒤 결과를 사용자에게 보고한다.

본 프로젝트에서 Agent가 개발하는 대상 소프트웨어는 Ubuntu 20.04 서버에서 동작하는 Plain Java Service 기반 daemon이다.

현재 대상 제품은 다음 기능을 수행하는 daemon이다.

1. 서버에 외부 연결 저장장치가 연결되면 대상 블록디바이스 이벤트를 감지한다.
2. 장치의 mount path를 확인한다.
3. 장치 내부의 지정 경로에서 `*.pcap` 파일을 탐색한다.
4. 탐색된 `*.pcap` 파일을 MinIO `ingest-staging` bucket에 업로드한 뒤, `src-extracted` bucket의 지정 폴더로 이동(move)한다.

본 제품은 `pcap` 파일의 분해, 센서별 추출, 데이터 해석을 수행하지 않는다.

---

## 3. 현재 목표

현재 목표는 다음과 같다.

- AI Agent 개발 환경의 전체 운영 구성을 정의한다.
- Harness, Orchestrator, Agent 역할을 문서로 고정한다.
- Java daemon 개발을 수행할 수 있는 repository 구조와 운영 문서를 준비한다.
- 실제 기능 요청을 기준으로 Orchestrator와 Agent가 일관된 방식으로 작업할 수 있는 기반을 마련한다.
- 현재 제품 대상 daemon의 요구사항과 아키텍처 기준을 문서로 확정한다.
- Storage Device PCAP Ingest / Relay Daemon의 첫 구현 사이클에 착수할 수 있는 준비 상태를 만든다.

현재 프로젝트는 **운영 문서 정리 단계를 지나, 제품 요구사항과 아키텍처 기준을 1차 확정한 상태**이다.

---

## 4. 현재 확정 사항

### 4.1 개발 환경 구성 방향

- 본 프로젝트는 AI Agent 개발 환경의 성능 평가를 목적으로 한다.
- Harness를 설정하고 적용한다.
- 사용자의 요청은 Orchestrator가 접수한다.
- Orchestrator는 Agent를 이용해 기능 개발, 테스트, 리뷰를 진행한다.
- 최종 결과는 Orchestrator가 사용자에게 보고한다.

### 4.2 사용 기술

- 주요 LLM은 Claude를 사용한다.
- Agent 생성은 Claude Agent SDK를 사용한다.
- 형상관리는 Git repository를 사용한다.
- 제품 개발 언어는 Java를 사용한다.
- 실행 대상은 Ubuntu 20.04 서버에서 동작하는 daemon이다.
- 제품 실행 형태는 Plain Java Service이다.
- 저장소는 MinIO를 사용한다.
- 장치 감지는 Linux `udev` 이벤트를 기준으로 한다.
- Java는 `libudev`를 JNA로 연동하는 방식을 사용한다.
- 설정 파일 형식은 `.properties`를 사용한다.
- 빌드 도구는 Maven을 사용한다.

### 4.3 운영 문서

현재 운영 기준 문서는 다음과 같이 구성한다.

- `AGENTS.md`
- `ORCHESTRATOR.md`
- `HARNESS.md`
- `PROJECT_STATE.md`
- `EVALS.md`
- `docs/project/PRD.md`
- `docs/project/ARCHITECTURE.md`

성능 평가 기준 문서인 `EVALS.md`는 별도로 작성한다.

### 4.4 Agent 구성

현재 기본 Agent는 다음 네 종류로 정의한다.

- Planner Agent
- Implementation Agent
- Test Agent
- Review Agent

Orchestrator는 직접 제품 코드를 구현하지 않으며, 작업 배정, 상태 추적, 결과 종합, 사용자 보고를 담당한다.

### 4.5 제품 대상

Agent가 개발하는 제품은 Ubuntu 20.04 서버에서 동작하는 Plain Java Service 기반 daemon이다.

현재 제품 수준의 확정 사항은 다음과 같다.

- 외부 연결 저장장치의 대상 블록디바이스 이벤트를 감지한다.
- mount path를 확인한다.
- 장치 내부의 지정 경로에서 `*.pcap` 파일을 탐색한다.
- 탐색된 `*.pcap` 파일을 MinIO `ingest-staging` bucket에 업로드한다.
- 업로드된 파일을 `src-extracted` bucket으로 이동(move)한다.
- `pcap` 파일명은 원본을 유지한다.
- 입력 저장 구조는 `<차량유형>/<수집날짜>/<차량유형-차량번호>/` 형태를 따른다.
- `pcap` 파일명 형식은 `<sensor_name>_YYYYMMDDhhmmss.pcap` 을 따른다.
- `pcap` 파일은 센서별로 1분 단위 분할 생성된다.
- `src-extracted` 대상 경로 규칙은 최신 운영 기준에 따라 적용한다.
- 중복 파일은 자동 덮어쓴다.
- 사용자 승인 절차는 두지 않는다.
- `pcap` 파일 분해는 수행하지 않는다.
- 장치 원본 데이터는 수정하거나 삭제하지 않는다.
- 장치 감지는 이벤트 기반으로 처리한다.
- 장치 감지는 `udev` 이벤트를 기준으로 한다.
- mount path는 이벤트 직후 확인하며, 즉시 확인되지 않으면 짧은 재시도를 수행한다.
- 초기 구현은 단일 장치 처리 방식으로 제한한다.
- `ingest-staging`에서 `src-extracted`로의 이동은 copy 후 source 삭제 방식으로 구현한다.
- 설정 파일은 `.properties` 형식을 사용한다.
- 빌드 도구는 Maven을 사용한다.
- daemon은 `dex-cli`를 실행하지 않는다.
- daemon은 `src-extracted` 내부의 추출 결과 구조를 생성하지 않는다.

---

## 5. 현재 진행 상태

### 5.1 완료된 항목

- 전체 구성 방향 정의
- `AGENTS.md` 초안 작성
- `ORCHESTRATOR.md` 초안 작성
- `HARNESS.md` 초안 작성
- `docs/project/PRD.md` 작성
- `docs/project/ARCHITECTURE.md` 작성
- Java daemon의 초기 기능 범위 1차 확정
- 현재 상태 기준 문서 1차 정리

### 5.2 진행 중인 항목

- 운영 문서 체계 정비
- repository 내 문서 배치 정리
- daemon 초기 구조 상세화 준비

### 5.3 미착수 항목

- `EVALS.md` 작성
- daemon package / module 구조 설계
- MinIO 연동 구현 상세화
- 첫 번째 실제 구현 사이클 수행
- 테스트 코드 구조 구체화
- 운영 스크립트 및 설정 파일 구조 구체화
- 장치 감지 구현 구조 상세화

---

## 6. 현재 기준 저장소 구조 초안

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
│  ├─ src/
│  │  ├─ main/java/
│  │  └─ test/java/
│  └─ scripts/
├─ eval/
└─ agent-runtime/
```

## 7. 현재 제약 사항
### 7.1 제품 언어 제약

최종 개발 대상 소프트웨어는 Java로 작성한다.

### 7.2 실행 환경 제약

제품은 Ubuntu 20.04 서버에서 장기 실행되는 daemon 형태를 전제로 한다.

### 7.3 Agent 실행 계층 제약

Claude Agent SDK를 사용하므로 Agent 실행 계층은 제품 코드와 별도 구조로 구성할 수 있다.
제품 코드는 Java daemon이며, Agent 운영 계층은 별도 런타임으로 구성될 수 있다.

### 7.4 문서 우선 제약

현재 단계에서는 운영 문서와 기준 문서를 먼저 정리한 후 실제 코드 구조를 구체화한다.

### 7.5 저장소 기준 제약

모든 Agent는 repository 내 문서와 코드를 기준으로 작업한다.
역할, 절차, 상태 판단은 운영 문서를 우선으로 참조한다.

### 7.6 제품 범위 제약

현재 daemon은 `pcap` 파일의 수집, 업로드, `src-extracted` 대상 폴더로의 이동까지만 담당한다.
`pcap` 파일의 분해, 해석, 센서별 추출, `dex-cli` 실행은 별도 기능 또는 수동 절차의 책임 범위로 본다.

## 8. 현재 미정 사항

현재 미정 사항은 다음과 같다.

- `EVALS.md`의 상세 평가 기준
- Claude Agent SDK 기반 Orchestrator의 실제 구현 언어
- repository의 상세 디렉터리 구조
- Java daemon의 package / module 상세 구조
- 테스트 실행 방식의 상세 기준
- 장치 mount 경로의 실제 운영 환경 규칙
- 대상 블록디바이스 식별 규칙
- `ingest-staging` object key의 최종 세부 규칙
- `src-extracted` 대상 경로 규칙의 최종 세부 기준
- 로그, 설정, 운영 스크립트 정책의 상세 구조

## 9. 현재 리스크 및 주의 사항

### 9.1 문서와 실제 구현의 불일치 가능성

현재 운영 문서를 먼저 정리하고 있으므로, 이후 실제 코드 구조와 문서 간 불일치가 발생할 수 있다.
문서와 실제 구조가 어긋날 경우 문서를 즉시 갱신한다.

### 9.2 Agent 역할 혼합 가능성

초기에는 Agent 역할을 분리하더라도 실제 작업 과정에서 역할 경계가 흐려질 수 있다.
특히 Implementation, Test, Review의 책임 경계를 지속적으로 점검한다.

### 9.3 Orchestrator 책임 과다 가능성

Orchestrator에 과도한 규칙과 책임이 집중되면 운영 복잡도가 증가할 수 있다.
역할 정의와 실행 규칙은 중복 없이 유지한다.

### 9.4 제품 코드와 Agent 실행 코드의 혼동 가능성

제품은 Java daemon이며, Agent 실행 계층은 별도 구조일 수 있다.
저장소 구조와 문서에서 두 계층을 명확히 분리한다.

### 9.5 장치 mount 환경 차이 가능성

실제 운영 서버에서 대상 저장장치가 mount되는 방식과 경로는 환경마다 다를 수 있다.
이 차이는 구현 방식과 테스트 방식에 직접 영향을 줄 수 있다.

### 9.6 장치 감지 방식의 구현 복잡도

장치 감지는 이벤트 기반의 `udev` 연동 방식으로 확정되었다.
다만 `libudev`와 JNA 연동, mount path 재시도 처리, 운영 환경별 mount 타이밍 차이로 인해 구현 복잡도가 존재한다.

### 9.7 MinIO move 처리 방식의 구현 차이 가능성

현재 기준에서 `ingest-staging`에서 `src-extracted`로의 이동은 copy 후 source 삭제 방식으로 구현한다.
다만 MinIO SDK 수준에서 object 복사, overwrite, source 삭제, 실패 처리 순서를 어떻게 안전하게 조합할지 구현 제약을 확인해야 한다.

### 9.8 src-extracted 경로 규칙 변경 가능성

`src-extracted` 대상 폴더명 및 경로 규칙은 최신 운영 기준에 따라 확정되어야 한다.
최종 규칙이 변경되거나 추가 세부 기준이 생길 경우 구현 구조와 테스트 기준에 영향을 줄 수 있다.

### 9.9 udev 연동 계층 의존성
장치 감지는 `libudev`와 JNA 연동을 전제로 하므로, 운영 환경에서 해당 연동 방식의 호환성과 배포 구성을 확인해야 한다.

## 10. 현재 우선 순위
1. 운영 문서 세트 최신화
2. repository 문서 배치 정리
3. daemon package / module 구조 확정
4. 장치 감지 구현 구조 상세화
5. MinIO 업로드/이동 구현 구조 상세화
6. 첫 번째 실제 구현 사이클 시작
7. EVALS.md 구체화

## 11. 작업 시작 전 확인 기준

Orchestrator와 Agent는 작업 시작 전에 최소한 다음 항목을 확인한다.

- 현재 사용자 요청
- `PROJECT_STATE.md`
- `AGENTS.md`
- `ORCHESTRATOR.md`
- `HARNESS.md`
- `docs/project/PRD.md`
- `docs/project/ARCHITECTURE.md`

위 문서와 현재 코드 구조가 충돌할 경우, 최신 사용자 요청과 본 문서의 현재 상태를 기준으로 우선 판단한다.
중요한 불일치는 명시적으로 기록한다.

## 12. 후속 작업 항목

현재 기준 후속 작업 항목은 다음과 같다.

- EVALS.md 작성
- repository 실제 폴더 구조 확정
- daemon 초기 package 구조 설계
- 장치 감지 구현 구조 상세화
- MinIO 업로드/이동 정책 구현 상세화
- 테스트 구조 정리
- 첫 번째 구현 태스크 분해 및 착수

## 13. 문서 유지 원칙

다음 경우 본 문서를 갱신한다.

- 프로젝트 목표가 변경된 경우
- 기술 스택이 변경된 경우
- Agent 운영 방식이 변경된 경우
- repository 구조가 확정 또는 변경된 경우
- 실제 구현 착수로 진행 상태가 달라진 경우
- 미정 사항이 확정된 경우
- PRD 또는 Architecture 기준이 변경된 경우

본 문서는 현재 상태를 기록하는 기준 문서이므로, 실제 상태와 다를 경우 즉시 수정한다.
