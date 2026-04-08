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

본 프로젝트는 repo 기반의 AI Agent 개발 환경을 구성하고, 그 환경이 실제 개발 작업에 얼마나 유효한지 평가하기 위한 프로젝트이다.

사용자는 repository 내 운영 문서, 제품 문서, 구현 task 문서, 실행 요청 문서를 기준으로 AI Agent에게 개발 작업을 지시한다.  
Orchestrator / Agent 구조는 작업 분해, 구현, 테스트, 리뷰를 위한 운영 모델로 사용한다.

현재 실제 구현 착수 도구는 OpenAI Codex CLI이며, 사용자는 Codex CLI를 repository 루트에서 실행하여 문서 기준으로 작업을 요청하는 방식으로 첫 구현 사이클을 시작하려는 상태이다.

본 프로젝트에서 Agent가 개발하는 대상 소프트웨어는 Ubuntu 20.04 서버에서 동작하는 Plain Java Service 기반 daemon이다.

현재 대상 제품은 다음 기능을 수행하는 daemon이다.

1. 서버에 외부 연결 저장장치가 연결되면 대상 블록디바이스 이벤트를 감지한다.
2. 장치의 mount path를 확인한다.
3. 장치 내부의 지정 경로에서 `*.pcap` 파일을 탐색한다.
4. 탐색된 `*.pcap` 파일을 MinIO `ingest-staging` bucket의 지정 경로로 업로드한다.

본 제품은 `pcap` 파일의 분해, 센서별 추출, 데이터 해석, dataset 생성, `dex-cli` 실행, `src-extracted` 관련 처리, `src-extracted` 내부 추출 결과 구조 생성을 수행하지 않는다.

---

## 3. 현재 목표

현재 목표는 다음과 같다.

- repo 기반 AI Agent 개발 환경의 운영 문서 체계를 유지한다.
- Java daemon 개발을 수행할 수 있는 제품 문서와 구현 task 문서를 기준 상태로 유지한다.
- 변동사항을 반영하여 현재 문서 세트를 최신 상태로 맞춘다.
- 수정된 문서를 기준으로 Codex 실행 요청문을 재정비한다.
- Storage Device PCAP Ingest Daemon의 첫 구현 사이클에 착수한다.

현재 프로젝트는 운영 문서, 제품 문서, 구현 task 문서, 실행 요청 문서가 작성된 상태이며, 변동사항 반영 후 첫 구현 요청을 Codex CLI에 입력하기 직전 단계이다.

---

## 4. 현재 확정 사항

### 4.1 개발 환경 구성 방향

- 본 프로젝트는 AI Agent 개발 환경의 성능 평가를 목적으로 한다.
- Harness를 설정하고 적용한다.
- 사용자의 요청은 Orchestrator가 접수한다.
- Orchestrator는 Agent를 이용해 기능 개발, 테스트, 리뷰를 진행한다.
- 최종 결과는 Orchestrator가 사용자에게 보고한다.

### 4.2 사용 기술

- 형상관리는 Git repository를 사용한다.
- 원격 저장소는 GitHub를 사용한다.
- 현재 구현 착수 도구는 OpenAI Codex CLI를 사용한다.
- 제품 개발 언어는 Java를 사용한다.
- 실행 대상은 Ubuntu 20.04 서버에서 동작하는 daemon이다.
- 제품 실행 형태는 Plain Java Service이다.
- 저장소는 MinIO를 사용한다.
- 주요 bucket은 `ingest-staging` 이다.
- 장치 감지는 Linux `udev` 이벤트를 기준으로 한다.
- Java는 `libudev`를 JNA로 연동하는 방식을 사용한다.
- 설정 파일 형식은 `.properties`를 사용한다.
- 빌드 도구는 Maven을 사용한다.

### 4.3 운영 문서

현재 기준 문서 세트는 다음과 같이 구성한다.

루트 운영 문서
- `AGENTS.md`
- `ORCHESTRATOR.md`
- `HARNESS.md`
- `PROJECT_STATE.md`
- `EVALS.md`
- `README.md`

제품 문서
- `docs/project/PRD.md`
- `docs/project/ARCHITECTURE.md`

구현 task 문서
- `docs/project/IMPLEMENTATION_TASK_01_APP_CONFIG.md`
- `docs/project/IMPLEMENTATION_TASK_02_DEVICE_SCAN.md`
- `docs/project/IMPLEMENTATION_TASK_03_STORAGE.md`
- `docs/project/IMPLEMENTATION_TASK_04_NAMING_LOGGING.md`

실행 요청 문서
- `docs/project/AGENT_EXECUTION_REQUEST_01.md`

Orchestrator와 Agent는 작업 시작 전에 위 문서 세트를 현재 기준 문서로 참조한다.

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

- 외부 연결 저장장치가 블록디바이스로 인식되는 경우를 감지 대상으로 한다.
- 장치 감지는 이벤트 기반으로 처리한다.
- 장치 감지는 `udev` 이벤트를 기준으로 한다.
- Java는 `libudev`와 JNA 연동 방식으로 장치 이벤트를 처리한다.
- mount path는 이벤트 직후 확인하며, 즉시 확인되지 않으면 짧은 재시도를 수행한다.
- 초기 구현은 단일 장치 처리 방식으로 제한한다.
- 장치 내부의 지정 경로에서 `*.pcap` 파일을 탐색한다.
- 탐색된 `*.pcap` 파일을 MinIO `ingest-staging` bucket에 업로드한다.
- `ingest-staging` 내 저장 경로는 `차량유형/yyyy/mm/dd/yymmdd_v차량번호/` 규칙을 따른다.
- 저장 경로 예시는 `U100/2026/03/27/260327_v009/` 이다.
- `pcap` 파일명은 원본을 유지한다.
- 입력 저장 구조는 `<차량유형>/<수집날짜>/<차량유형-차량번호>/` 형태를 따른다.
- `pcap` 파일명 형식은 `<sensor_name>_YYYYMMDDhhmmss.pcap` 을 따른다.
- `pcap` 파일은 센서별로 1분 단위 분할 생성된다.
- 설정 파일은 `.properties` 형식을 사용한다.
- 빌드 도구는 Maven을 사용한다.
- daemon은 `pcap extract`를 수행하지 않는다.
- daemon은 센서별 분해를 수행하지 않는다.
- daemon은 dataset 생성을 수행하지 않는다.
- daemon은 `dex-cli`를 실행하지 않는다.
- daemon은 `src-extracted` 관련 처리를 수행하지 않는다.
- daemon은 `src-extracted` 내부의 추출 결과 구조를 생성하지 않는다.

---

## 5. 현재 진행 상태

### 5.1 완료된 항목

- 로컬 PC에 Git repository 생성 완료
- GitHub push 완료
- `.gitignore` 작성 및 push 완료
- OpenAI Codex CLI 설치 완료
- Codex 로그인 완료
- repository 루트 진입 및 작업 준비 상태 확인 완료
- `AGENTS.md` 작성 완료
- `ORCHESTRATOR.md` 작성 완료
- `HARNESS.md` 작성 완료
- `PROJECT_STATE.md` 작성 완료
- `EVALS.md` 작성 완료
- `README.md` 작성 완료
- `docs/project/PRD.md` 작성 완료
- `docs/project/ARCHITECTURE.md` 작성 완료
- 구현 task 문서 1~4 작성 완료
- `docs/project/AGENT_EXECUTION_REQUEST_01.md` 작성 완료
- Java daemon의 초기 기능 범위 1차 정리 완료
- package 구조 초안 정리 완료
- Maven 기반 실제 Java 프로젝트 skeleton 생성 완료
- `app`, `config`, `device`, `scan`, `storage`, `naming`, `logging` 초기 코드 구조 생성 완료
- task 2, task 3, task 4 기준 1차 구현 보강 완료
- 로컬 환경에서 `mvn compile` 및 `mvn test` 통과 확인 완료
- object key 기본 규칙 확정 완료

### 5.2 진행 중인 항목

- object key 확정 규칙의 문서 반영 마무리
- naming / storage / logging 보강 방향 정리
- 다음 구현 사이클 범위 정리

### 5.3 미착수 항목

- object key 세부 예외 규칙 처리
- 중복 object 처리 정책 확정 및 반영
- `udev` / `libudev` / JNA 실제 연동 정교화
- MinIO 통합 테스트 보강
- naming / storage / logging 단위 테스트 보강
- device → scan → storage 통합 검증 강화
- 후속 구현 사이클 수행

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
│     ├─ ARCHITECTURE.md
│     ├─ IMPLEMENTATION_TASK_01_APP_CONFIG.md
│     ├─ IMPLEMENTATION_TASK_02_DEVICE_SCAN.md
│     ├─ IMPLEMENTATION_TASK_03_STORAGE.md
│     ├─ IMPLEMENTATION_TASK_04_NAMING_LOGGING.md
│     └─ AGENT_EXECUTION_REQUEST_01.md
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

현재 구현 착수 도구는 OpenAI Codex CLI를 사용한다.
Agent 실행 계층은 제품 코드와 별도 구조로 구성할 수 있다.
제품 코드는 Java daemon이며, Agent 운영 계층은 별도 런타임으로 구성될 수 있다.

### 7.4 문서 우선 제약

현재 단계에서는 운영 문서와 기준 문서를 먼저 정리한 후 실제 코드 구조를 구체화한다.

### 7.5 저장소 기준 제약

모든 Agent는 repository 내 문서와 코드를 기준으로 작업한다.
역할, 절차, 상태 판단은 운영 문서를 우선으로 참조한다.

### 7.6 제품 범위 제약

현재 daemon은 `pcap` 파일의 수집 대상 탐색과 `ingest-staging` bucket 업로드까지만 담당한다.
`pcap` 파일의 분해, 해석, 센서별 추출, `dex-cli` 실행, `src-extracted` 관련 처리는 별도 기능 또는 수동 절차의 책임 범위로 본다.

## 8. 현재 미정 사항

현재 미정 사항은 다음과 같다.

- `ingest-staging` object key의 세부 예외 규칙
- 대상 블록디바이스 식별 규칙의 운영 상세 기준
- 장치 mount path의 실제 운영 환경 규칙
- 로그 파일 정책과 운영 로그 보존 방식의 상세 기준
- 운영 스크립트 배치 방식의 상세 기준
- 중복 object 처리 정책의 최종 기준

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

### 9.7 ingest-staging object key 규칙 적용 리스크

현재 기준에서 daemon은 `ingest-staging` bucket 내 지정 경로로 `pcap` 파일을 업로드한다.
다만 업로드 대상 object key를 `차량유형/yyyy/mm/dd/yymmdd_v차량번호/` 규칙에 맞게 안정적으로 계산하고, 운영 입력값과 파일명 규칙을 일관되게 반영할 수 있는지 구현 시 확인이 필요하다.

### 9.8 ingest-staging 경로 규칙 세부 예외 가능성

`ingest-staging` 대상 폴더명 및 경로 규칙은 현재 기준으로 `차량유형/yyyy/mm/dd/yymmdd_v차량번호/` 형태를 따른다.
다만 차량번호 표기 방식, 날짜 해석 기준, object key 조합 규칙의 세부 예외 기준이 추가될 경우 구현 구조와 테스트 기준에 영향을 줄 수 있다.

### 9.9 udev 연동 계층 의존성
장치 감지는 `libudev`와 JNA 연동을 전제로 하므로, 운영 환경에서 해당 연동 방식의 호환성과 배포 구성을 확인해야 한다.

## 10. 현재 우선 순위
1. object key 확정 규칙의 문서 반영 완료
2. naming / storage / logging 보강 방향 정리
3. 중복 object 처리 정책 확정
4. object key 세부 예외 규칙 정리
5. device → scan → storage 통합 검증 강화
6. `udev` 실제 연동 정교화
7. 다음 구현 사이클 시작

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

- object key 확정 규칙의 문서 반영 완료
- 중복 object 처리 정책 확정
- object key 세부 예외 규칙 정리
- naming / storage / logging 단위 테스트 보강
- device → scan → storage 통합 검증 강화
- `udev` 실제 연동 정교화
- 다음 구현 사이클 실행

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
