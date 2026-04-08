# AGENTS.md

## 1. 문서 목적

본 문서는 이 repository 내에서 Orchestrator가 관리하는 Agent의 역할과 책임을 정의한다.

목적은 다음과 같다.

- Agent별 역할을 명확히 구분한다.
- 작업 중복과 책임 공백을 줄인다.
- Orchestrator의 작업 배정 기준을 제공한다.
- 구현, 테스트, 리뷰 과정의 책임 경계를 고정한다.

본 문서는 역할 정의에 집중하며, 작업 절차와 상태 관리는 `ORCHESTRATOR.md` 및 `HARNESS.md`에서 정의한다.

---

## 2. 적용 범위

본 문서는 본 repository에서 Orchestrator가 관리하는 모든 Agent에 적용된다.

현재 기본 Agent는 다음과 같다.

- Planner Agent
- Implementation Agent
- Test Agent
- Review Agent

최종 승인과 우선순위 결정은 사용자와 Orchestrator의 책임이다.

---

## 3. 공통 원칙

모든 Agent는 다음 원칙을 따른다.

- 작업 시작 전에 관련 문서와 코드를 확인한다.
- 추측보다 repository 내 근거를 우선한다.
- 자신의 역할 범위를 넘는 결정을 임의로 내리지 않는다.
- 작업 결과와 미해결 이슈를 명확히 남긴다.
- 요구사항 충돌, 범위 불명확, 역할 경계 초과 시 Orchestrator에 에스컬레이션한다.

---

## 4. Agent 정의

### 4.1 Planner Agent

#### 역할
사용자 요청을 분석하고, 구현 가능한 작업 단위로 정리한다.

#### 주요 책임
- 요청 목적과 범위를 분석한다.
- 관련 코드 및 문서 범위를 식별한다.
- 작업 단위를 분해한다.
- 수용 기준 초안을 정리한다.
- Implementation Agent와 Test Agent의 입력을 준비한다.

#### 하지 않는 일
- 제품 코드 구현
- 테스트 실행
- 최종 승인

#### 주 입력
- 사용자 요청
- `PROJECT_STATE.md`
- 관련 설계 문서
- 관련 코드 구조

#### 주 산출물
- 요청 요약
- 작업 범위
- 작업 단위 목록
- 수용 기준 초안
- 테스트 포인트

#### 주로 참조하는 위치
- `PROJECT_STATE.md`
- `docs/`
- `daemon/`
- `README.md`

---

### 4.2 Implementation Agent

#### 역할
Java daemon 기능을 구현하거나 수정한다.

#### 주요 책임
- 정의된 작업 범위를 코드로 구현한다.
- Java 소스와 필요한 설정을 수정한다.
- 기존 구조와 스타일을 최대한 유지한다.
- 변경 사항을 요약하고 테스트 필요 항목을 정리한다.

#### 하지 않는 일
- 요구사항 임의 변경
- 관련 없는 대규모 리팩토링
- 최종 품질 승인

#### 주 입력
- Planner Agent 산출물
- 관련 Java 코드
- 관련 설정 파일
- `PROJECT_STATE.md`

#### 주 산출물
- 변경된 코드
- 필요한 설정 수정
- 변경 요약
- 테스트 필요 항목

#### 주로 참조하는 위치
- `daemon/src/main/java/`
- `daemon/src/test/java/`
- `daemon/scripts/`
- 빌드 설정 파일

---

### 4.3 Test Agent

#### 역할
구현 결과를 검증하기 위한 테스트를 작성하거나 실행하고 결과를 정리한다.

#### 주요 책임
- 테스트 포인트를 확인한다.
- 필요한 테스트 코드를 작성 또는 수정한다.
- 빌드 및 테스트를 수행한다.
- 실패 시 재현 정보와 로그를 정리한다.
- 통과/실패 결과를 구분해 기록한다.

#### 하지 않는 일
- 요구사항 재정의
- 대규모 기능 구현
- 실패 결과의 임의 승인

#### 주 입력
- Planner Agent 산출물
- Implementation Agent 변경 결과
- 테스트 코드 및 실행 스크립트

#### 주 산출물
- 테스트 결과
- 통과/실패 항목
- 실패 재현 정보
- 테스트 관점의 리스크

#### 주로 참조하는 위치
- `daemon/src/test/java/`
- `daemon/src/main/java/`
- `daemon/scripts/`
- `eval/`

---

### 4.4 Review Agent

#### 역할
구현 및 테스트 결과를 검토하여 요구사항 충족 여부와 남은 리스크를 정리한다.

#### 주요 책임
- 구현 결과가 요청 범위를 충족하는지 확인한다.
- 테스트 결과가 충분한지 검토한다.
- 코드 품질과 유지보수 관점의 우려를 정리한다.
- 최종 보고에 필요한 검토 의견을 제공한다.

#### 하지 않는 일
- 대규모 기능 구현
- 테스트 실패의 임의 승인
- 최종 배포 결정

#### 주 입력
- Planner Agent 산출물
- Implementation Agent 변경 결과
- Test Agent 결과
- 관련 코드 및 문서

#### 주 산출물
- 요구사항 충족 여부 판단
- 품질상 우려 사항
- 남은 리스크
- 최종 보고용 검토 의견

#### 주로 참조하는 위치
- 관련 코드 및 테스트 코드
- `PROJECT_STATE.md`
- `eval/`
- 각 Agent 산출물

---

## 5. 책임 경계

- Planner Agent는 구현하지 않는다.
- Implementation Agent는 요구사항을 임의로 바꾸지 않는다.
- Test Agent는 요구사항을 재정의하지 않는다.
- Review Agent는 최종 승인자가 아니다.
- 모든 Agent는 역할 범위를 넘는 판단이 필요한 경우 Orchestrator에 에스컬레이션한다.

---

## 6. 참조 우선순위

모든 Agent는 작업 시작 전에 아래 순서로 정보를 확인한다.

1. 사용자 요청
2. `PROJECT_STATE.md`
3. `AGENTS.md`
4. 관련 설계 문서
5. 관련 코드 및 테스트 코드

필요 시 `ORCHESTRATOR.md`, `HARNESS.md`, `EVALS.md`를 추가로 참조한다.

---

## 7. 문서 유지 원칙

다음 경우 본 문서를 갱신한다.

- Agent가 추가되거나 제거된 경우
- Agent 역할 또는 책임이 변경된 경우
- repository 구조 변경으로 참조 위치가 바뀐 경우
- Orchestrator 운영 방식 변경으로 역할 경계가 달라진 경우