# IMPLEMENTATION_TASK_03_STORAGE_RELAY

## 1. 문서 목적

본 문서는 Storage Device PCAP Ingest / Relay Daemon의 세 번째 구현 작업 지시를 정의한다.

이번 작업의 목적은 `storage`, `relay` package의 초기 구현을 통해 `scan` 단계의 탐색 결과를 받아 `ingest-staging` bucket에 업로드하고, 이후 `src-extracted` 대상 경로로 copy 후 source 삭제 방식의 전달 구조를 만들 수 있도록 하는 것이다.

본 작업은 이후 `naming`, `logging` 구현이 안정적으로 이어질 수 있도록 업로드와 bucket 간 전달의 기본 흐름을 고정하는 것을 목표로 한다.

---

## 2. 작업 범위

이번 작업의 구현 범위는 다음으로 제한한다.

- `storage` package 생성
- `relay` package 생성
- MinIO 접근용 기본 클라이언트 클래스 생성
- `ingest-staging` 업로드 클래스 생성
- `src-extracted` 전달 클래스 생성
- 업로드 → copy → source 삭제까지의 흐름 연결
- 초기 버전의 relay 결과 구조 작성

이번 작업은 object key 최종 세부 규칙 확정, `src-extracted` 대상 경로의 최종 세부 규칙 확정, 상세 로그 포맷 설계까지 구현하지 않는다.

---

## 3. 기준 문서

구현 Agent는 작업 전에 다음 문서를 반드시 확인해야 한다.

- `PROJECT_STATE.md`
- `docs/project/PRD.md`
- `docs/project/ARCHITECTURE.md`
- `docs/project/IMPLEMENTATION_TASK_01_APP_CONFIG.md`
- `docs/project/IMPLEMENTATION_TASK_02_DEVICE_SCAN.md`

문서 간 충돌이 있을 경우 최신 사용자 요청과 `PROJECT_STATE.md`를 우선 확인한다.

---

## 4. 이번 작업의 목표

이번 작업에서 달성해야 하는 목표는 다음과 같다.

1. `scan` 단계의 결과를 받아 `ingest-staging` bucket에 업로드하는 구조를 만든다.
2. 업로드된 object를 `src-extracted` 대상 경로로 전달하는 구조를 만든다.
3. 전달은 논리적 move가 아니라 copy 후 source 삭제 방식으로 구현되도록 한다.
4. copy 성공 시에만 source 삭제가 수행되도록 한다.
5. copy 실패 또는 source 삭제 실패를 구분 가능한 결과 구조를 만든다.
6. 이후 `naming`, `logging`이 붙을 수 있는 업로드/전달 뼈대를 만든다.

---

## 5. 대상 package

이번 작업의 대상 package는 다음 두 개이다.

- `storage`
- `relay`

### 5.1 package 역할

- `storage`
  - MinIO 기본 접근
  - `ingest-staging` 업로드
  - bucket/object 단위 기본 작업 수행

- `relay`
  - `ingest-staging` → `src-extracted` 전달
  - copy 후 source 삭제 방식 적용
  - 전달 결과 정리
  - 부분 실패 상태 구분

---

## 6. 생성 대상 클래스

이번 작업에서 생성할 클래스는 다음과 같다.

### 6.1 storage package

#### `MinioStorageClient`
역할:
- MinIO SDK 래핑
- bucket 접근, object 업로드, object 복사, object 삭제, 존재 여부 확인 등 기본 기능 제공
- 이후 `storage`, `relay`가 공통으로 사용할 수 있는 접근 계층 제공

#### `IngestUploader`
역할:
- `ScanResult` 또는 그에 준하는 입력을 받아 `ingest-staging` 업로드 수행
- 업로드 대상 파일과 object key를 기준으로 업로드 실행
- 업로드 성공/실패를 후속 단계에 전달 가능한 형태로 반환

### 6.2 relay package

#### `RelayService`
역할:
- 업로드 완료된 object를 `src-extracted` 대상 경로로 전달
- copy 성공 시에만 source 삭제 수행
- copy 실패 시 source 삭제 금지
- source 삭제 실패 시 부분 실패 처리

#### `RelayResult`
역할:
- relay 결과를 담는 구조
- 최소한 다음 정보를 담을 수 있어야 한다.
  - source bucket / object key
  - target bucket / target path
  - copy 성공/실패 상태
  - source 삭제 성공/실패 상태
  - 전체 성공 / 부분 실패 / 실패 상태

---

## 7. 권장 파일 배치

```text
daemon/
└─ src/
   └─ main/
      └─ java/
         └─ <base-package>/
            ├─ storage/
            │  ├─ MinioStorageClient.java
            │  └─ IngestUploader.java
            └─ relay/
               ├─ RelayService.java
               └─ RelayResult.java
```

## 8. 구현 기준

### 8.1 storage 구현 기준
- `storage`는 `MinIO` 접근과 `ingest-staging` 업로드까지만 담당한다.
- `IngestUploader`는 장치 감지나 파일 탐색 로직을 직접 포함하지 않는다.
- 업로드 대상은 scan 단계에서 식별된 `*.pcap` 파일로 제한한다.
- `bucket` 이름은 설정값을 통해 받을 수 있어야 한다.
- `MinIO` 접근 상세는 `MinioStorageClient`에 최대한 모은다.

### 8.2 ingest-staging 업로드 기준
- 업로드 대상은 `*.pcap` 파일만 처리한다.
- 업로드 시 `object key`는 현재 기준상 입력 경로 구조를 반영할 수 있는 형태로 준비한다.
- 단, `object key`의 최종 세부 규칙은 아직 미정이므로 코드에 과도하게 고정하지 않는다.
- 업로드 실패 시 relay 단계로 넘어가지 않아야 한다.

### 8.3 relay 구현 기준
- `RelayService`는 `ingest-staging`에서 `src-extracted`로의 전달만 담당한다.
- 전달은 논리적으로 move이지만, 구현은 copy 후 source 삭제 방식으로 수행한다.
- copy 성공 시에만 `source object`를 삭제해야 한다.
- copy 실패 시 source 삭제를 수행해서는 안 된다.
- source 삭제 실패 시 전체 성공이 아니라 부분 실패로 처리해야 한다.

### 8.4 src-extracted 대상 경로 기준
- `src-extracted` 대상 경로는 설정값 또는 별도 규칙 계산 결과를 입력으로 받을 수 있는 구조를 권장한다.
- 현재 단계에서는 대상 경로의 최종 세부 규칙을 하드코딩하지 않는다.
- `relay`는 대상 경로를 받아 전달을 수행하는 쪽에 집중한다.

### 8.5 RelayResult 기준
- `RelayResult`는 단순 성공/실패만 담지 않는다.
- 최소한 copy 결과와 source 삭제 결과를 분리해 표현할 수 있어야 한다.
- 전체 성공, 부분 실패, 실패를 구분 가능해야 한다.
- 이후 logging 단계에서 그대로 활용할 수 있는 결과 구조를 권장한다.

### 8.6 구조 분리 기준
- `storage`는 업로드 담당
- relay는 `bucket` 간 전달 담당
- 두 `package`의 책임을 섞지 않는다.

예:

- `IngestUploader` 안에 `src-extracted` copy/delete 로직을 길게 넣지 않는다.
- `RelayService` 안에 장치 탐색이나 `*.pcap` 식별 로직을 넣지 않는다.

## 9. 이번 작업에서 하지 않는 것

다음 항목은 이번 작업 범위에 포함하지 않는다.

- `udev` 이벤트 감지 구현
- `mount path` 확인 구현
- `*.pcap` 탐색 구현
- `object key` 최종 세부 규칙 확정
- `src-extracted` 대상 경로의 최종 세부 규칙 확정
- 상세 로그 포맷 설계
- 운영 서버별 예외 케이스의 완전한 처리
- 테스트 코드의 완전한 구현

## 10. 완료 조건

이번 작업은 아래 조건을 만족하면 완료로 본다.

- `storage` package, `relay` package가 생성되어 있다.
- `MinioStorageClient`, `IngestUploader`, `RelayService`, `RelayResult` 클래스가 생성되어 있다.
- scan 단계 결과를 받아 `ingest-staging` 업로드를 수행할 수 있는 구조가 존재한다.
- 업로드 성공 이후에만 relay 단계가 수행되도록 흐름이 분리되어 있다.
- `ingest-staging` → `src-extracted` 전달이 copy 후 source 삭제 방식으로 구현되어 있다.
- copy 실패 시 source 삭제가 수행되지 않는다.
- source 삭제 실패 시 부분 실패 상태를 구분할 수 있다.
- 이후 `naming`, `logging` package가 붙을 수 있는 구조로 되어 있다.

## 11. 산출물

이번 작업의 산출물은 다음과 같다.

- `storage` package 초기 클래스
- `relay` package 초기 클래스
- `ingest-staging` 업로드 구조
- copy 후 source 삭제 기반 relay 구조
- 부분 실패를 구분 가능한 relay 결과 구조

## 12. 후속 작업 연결

이번 작업이 완료되면 다음 구현 작업으로 이어진다.

- `naming` package 구현
- `logging` package 구현

즉, 이번 작업은 업로드와 bucket 간 전달의 기반을 고정하는 3차 작업이다.

## 13. 작업 시 주의 사항
- 현재 단계에서는 구조를 단순하게 유지한다.
- 아직 확정되지 않은 `object key` 규칙과 대상 경로 규칙을 코드에 과도하게 고정하지 않는다.
- 문서 기준을 벗어난 임의 확장을 하지 않는다.
- copy 성공 여부와 source 삭제 여부를 반드시 분리해 처리한다.
- 실제 구현에서는 부분 실패 상태가 숨겨지지 않도록 결과 구조를 명확히 만든다.