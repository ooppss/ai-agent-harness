# IMPLEMENTATION_TASK_04_NAMING_LOGGING

## 1. 문서 목적

본 문서는 Storage Device PCAP Ingest Daemon의 네 번째 구현 작업 지시를 정의한다.

이번 작업의 목적은 `naming`, `logging` package의 초기 구현을 통해 `ingest-staging` 업로드용 object key를 계산할 수 있는 구조를 만들고, 각 단계의 처리 결과와 오류를 일관되게 기록할 수 있는 기반을 마련하는 것이다.

본 작업은 앞선 `app`, `config`, `device`, `scan`, `storage` 구현이 안정적으로 연결될 수 있도록 naming rule 적용과 logging 구조를 고정하는 것을 목표로 한다.

---

## 2. 작업 범위

이번 작업의 구현 범위는 다음으로 제한한다.

- `naming` package 생성
- `logging` package 생성
- object key 생성 클래스 생성
- 처리 로그 기록 클래스 생성
- 오류 기록 구조 생성
- 기존 `storage` 흐름에 naming / logging 연결 가능 구조 마련

이번 작업은 object key 최종 세부 규칙 확정, 로그 적재 시스템 연동, 외부 모니터링 시스템 연동, 운영 대시보드 구현까지 포함하지 않는다.
---

## 3. 기준 문서

구현 Agent는 작업 전에 다음 문서를 반드시 확인해야 한다.

- `PROJECT_STATE.md`
- `docs/project/PRD.md`
- `docs/project/ARCHITECTURE.md`
- `docs/project/IMPLEMENTATION_TASK_01_APP_CONFIG.md`
- `docs/project/IMPLEMENTATION_TASK_02_DEVICE_SCAN.md`
- `docs/project/IMPLEMENTATION_TASK_03_STORAGE.md`

문서 간 충돌이 있을 경우 최신 사용자 요청과 `PROJECT_STATE.md`를 우선 확인한다.

---

## 4. 이번 작업의 목표

이번 작업에서 달성해야 하는 목표는 다음과 같다.

1. `ingest-staging` 업로드 시 사용할 object key 계산 구조를 `차량유형/yyyy/mm/dd/yymmdd_v차량번호/원본파일명` 기준으로 만든다.
2. 기본 규칙은 반영하되, 세부 예외 규칙을 과도하게 하드코딩하지 않는 유연한 구조를 만든다.
3. 주요 처리 단계의 성공, 실패를 일관되게 기록할 수 있는 logging 구조를 만든다.
4. 이후 실제 운영 규칙 확정 시 naming rule을 교체하거나 확장하기 쉬운 구조를 만든다.

---

## 5. 대상 package

이번 작업의 대상 package는 다음 두 개이다.

- `naming`
- `logging`

### 5.1 package 역할

- `naming`
  - `ingest-staging` object key 계산
  - 입력 경로 구조와 운영 규칙을 반영한 naming rule 적용

- `logging`
  - 처리 단계 로그 기록
  - 오류 로그 기록
  - 후속 추적에 필요한 핵심 정보 정리
---

## 6. 생성 대상 클래스

이번 작업에서 생성할 클래스는 다음과 같다.

### 6.1 naming package

#### `ObjectKeyBuilder`
역할:
- 업로드 대상 파일과 입력 경로 정보를 바탕으로 `ingest-staging` object key 생성
- 현재 기준상 입력 경로 구조를 반영할 수 있는 기본 생성 규칙 제공
- 최종 규칙 확정 전까지 과도한 하드코딩 없이 확장 가능 구조 유지

### 6.2 logging package

#### `DaemonLogger`
역할:
- 주요 처리 흐름 로그 기록
- 장치 감지, `mount path` 확인, 탐색 시작/종료, 업로드 시작/완료/실패 등 주요 이벤트 기록
- 후속 추적을 위한 공통 로그 진입점 제공

#### `ErrorLogger`
역할:
- 오류 기록
- 지정 경로 미존재, `pcap` 미발견, object key 계산 실패, 업로드 실패 등 예외 상황 기록
- 문제 원인과 재처리 판단에 필요한 최소 정보 제공

---

## 7. 권장 파일 배치

```text
daemon/
└─ src/
   └─ main/
      └─ java/
         └─ <base-package>/
            ├─ naming/
            │  └─ ObjectKeyBuilder.java
            └─ logging/
               ├─ DaemonLogger.java
               └─ ErrorLogger.java
```

## 8. 구현 기준

### 8.1 `naming` 구현 기준
- `naming`은 계산 규칙 제공까지만 담당한다.
- 실제 업로드 수행 로직을 직접 포함하지 않는다.
- 입력 정보와 규칙을 받아 결과 문자열을 계산하는 역할에 집중한다.

### 8.2 `object key` 계산 기준
- `ObjectKeyBuilder`는 입력 경로 구조 `<차량유형>/<수집날짜>/<차량유형-차량번호>/`를 반영할 수 있어야 한다.
- object key는 `차량유형/yyyy/mm/dd/yymmdd_v차량번호/원본파일명` 규칙을 기본으로 생성해야 한다.
- `yyyy/mm/dd`와 `yymmdd`는 입력 경로의 수집날짜(YYYYMMDD)에서 추출해야 한다.
- `v차량번호`는 `<차량유형-차량번호>`에서 차량번호만 추출한 뒤 접두어 `v`를 붙여 생성해야 한다.
- 업로드 대상 `*.pcap` 파일명은 원본을 유지해야 한다.
- 세부 예외 규칙은 코드에 과도하게 고정하지 않는다.
- 향후 운영 규칙이 확정되면 교체 또는 확장 가능한 구조를 권장한다.

### 8.3 `logging` 구현 기준
- `logging`은 기록 담당까지만 맡는다.
- 장치 감지, 탐색, 업로드 로직을 직접 포함하지 않는다.
- 주요 단계의 시작 / 완료 / 실패를 일관된 방식으로 남길 수 있어야 한다.

### 8.4 기록 대상 기준

최소한 다음 항목은 기록 가능해야 한다.

- 장치 감지
- `mount path` 확인
- 지정 경로 탐색 시작 / 종료
- `pcap` 파일 목록 또는 개수
- `ingest-staging` 업로드 시작 / 완료 / 실패
- object key 계산 성공 / 실패
- 오류 원인

### 8.5 `ErrorLogger` 기준
- `ErrorLogger`는 실패를 구분 가능하게 기록해야 한다.
- 재처리 판단에 필요한 핵심 정보가 누락되지 않도록 해야 한다.

### 8.6 구조 분리 기준
- `naming`은 문자열 / 경로 계산 담당
- `logging`은 기록 담당
- 두 `package`의 책임을 섞지 않는다.

예:

- `ObjectKeyBuilder` 안에 `MinIO` 업로드 로직을 넣지 않는다.
- `DaemonLogger` 안에 `object key` 계산 로직을 넣지 않는다.
- `ErrorLogger` 안에 업로드 처리 로직을 넣지 않는다.

## 9. 이번 작업에서 하지 않는 것

다음 항목은 이번 작업 범위에 포함하지 않는다.

- `udev` 이벤트 감지 구현
- `mount path` 확인 구현
- `*.pcap` 탐색 구현
- `MinIO` 업로드 구현
- object key 최종 운영 규칙 확정
- 외부 로그 수집 시스템 연동
- 운영 대시보드 구현
- 테스트 코드의 완전한 구현
- `src-extracted` 관련 처리 구현

## 10. 완료 조건

이번 작업은 아래 조건을 만족하면 완료로 본다.

- `naming` package, `logging` package가 생성되어 있다.
- `ObjectKeyBuilder`, `DaemonLogger`, `ErrorLogger` 클래스가 생성되어 있다.
- 업로드용 `object key` 계산 구조가 존재한다.
- 주요 처리 단계 로그를 남길 수 있는 공통 구조가 존재한다.
- 오류를 구분해 기록할 수 있는 구조가 존재한다.
- 기존 `storage` 흐름에 연결 가능한 형태로 되어 있다.

## 11. 산출물

이번 작업의 산출물은 다음과 같다.

- `naming` package 초기 클래스
- `logging` package 초기 클래스
- `object key` 계산 구조
- 공통 처리 로그 구조
- 오류 기록 구조

## 12. 후속 작업 연결

이번 작업이 완료되면 다음 단계로 이어진다.

- package 간 실제 연결 보강
- 테스트 구조 보강
- 예외 처리 보강
- 첫 번째 통합 구현 사이클 점검

즉, 이번 작업은 naming rule과 logging 기반을 고정하는 4차 작업이다.

## 13. 작업 시 주의 사항
- 현재 단계에서는 구조를 단순하게 유지한다.
- 기본 object key 규칙은 반영하되, 아직 확정되지 않은 세부 예외 규칙은 코드에 과도하게 고정하지 않는다.
- 문서 기준을 벗어난 임의 확장을 하지 않는다.
- 성공 / 실패를 구분하는 기록 구조를 명확히 만든다.
- naming rule은 바뀔 수 있다는 전제를 유지한다.