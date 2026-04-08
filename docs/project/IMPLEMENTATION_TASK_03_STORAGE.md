# IMPLEMENTATION_TASK_03_STORAGE

## 1. 문서 목적

본 문서는 Storage Device PCAP Ingest Daemon의 세 번째 구현 작업 지시를 정의한다.

이번 작업의 목적은 `storage` package의 초기 구현을 통해 `scan` 단계의 탐색 결과를 받아 `ingest-staging` bucket에 업로드할 수 있는 구조를 만드는 것이다.

본 작업은 이후 `naming`, `logging` 구현이 안정적으로 이어질 수 있도록 MinIO 접근과 업로드의 기본 흐름을 고정하는 것을 목표로 한다.

---

## 2. 작업 범위

이번 작업의 구현 범위는 다음으로 제한한다.

- `storage` package 생성
- MinIO 접근용 기본 클라이언트 클래스 생성
- `ingest-staging` 업로드 클래스 생성
- scan 결과를 받아 업로드를 수행하는 흐름 연결
- 업로드 결과를 후속 단계에서 활용할 수 있는 기본 구조 작성

이번 작업은 object key 최종 세부 규칙 확정, 상세 로그 포맷 설계, 중복 처리 정책의 최종 확정까지 구현하지 않는다.

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
2. MinIO 접근과 업로드 로직을 `storage` package 안에 분리한다.
3. 이후 `naming`, `logging`이 붙을 수 있는 업로드 뼈대를 만든다.
4. 업로드 성공/실패를 후속 단계에서 사용할 수 있는 형태로 정리한다.

---

## 5. 대상 package

이번 작업의 대상 package는 다음 하나이다.

- `storage`

### 5.1 package 역할

- `storage`
  - MinIO 기본 접근
  - `ingest-staging` 업로드
  - bucket/object 단위 기본 작업 수행
  - 업로드 결과 정리

---

## 6. 생성 대상 클래스

이번 작업에서 생성할 클래스는 다음과 같다.

### 6.1 storage package

#### `MinioStorageClient`
역할:
- MinIO SDK 래핑
- bucket 접근, object 업로드, object 존재 여부 확인 등 기본 기능 제공
- 이후 `storage`가 공통으로 사용할 수 있는 접근 계층 제공

#### `IngestUploader`
역할:
- `ScanResult` 또는 그에 준하는 입력을 받아 `ingest-staging` 업로드 수행
- 업로드 대상 파일과 object key를 기준으로 업로드 실행
- 업로드 성공/실패를 후속 단계에 전달 가능한 형태로 반환

---

## 7. 권장 파일 배치

```text
daemon/
└─ src/
   └─ main/
      └─ java/
         └─ <base-package>/
            └─ storage/
               ├─ MinioStorageClient.java
               └─ IngestUploader.java
```

## 8. 구현 기준

### 8.1 storage 구현 기준
- `storage`는 `MinIO` 접근과 `ingest-staging` 업로드까지만 담당한다.
- `IngestUploader`는 장치 감지나 파일 탐색 로직을 직접 포함하지 않는다.
- 업로드 대상은 scan 단계에서 식별된 `*.pcap` 파일로 제한한다.
- bucket 이름은 설정값을 통해 받을 수 있어야 한다.
- `MinIO` 접근 상세는 `MinioStorageClient`에 최대한 모은다.

### 8.2 ingest-staging 업로드 기준
- 업로드 대상은 `*.pcap` 파일만 처리한다.
- 업로드 대상 경로는 `차종/yyyy/mm/dd/yyddmm_차번호/` 규칙을 반영할 수 있는 구조로 준비한다.
- 단, object key의 최종 세부 규칙은 아직 미정이므로 코드에 과도하게 고정하지 않는다.
- 파일명은 원본 `pcap` 파일명을 유지한다.

### 8.3 결과 구조 기준
- 업로드 결과는 후속 단계에서 활용할 수 있는 형태로 반환할 수 있어야 한다.
- 최소한 업로드 대상 bucket, object key, 성공/실패 여부를 구분할 수 있는 구조를 권장한다.
- 이후 `logging` 단계에서 그대로 활용할 수 있는 결과 구조를 권장한다.

### 8.4 구조 분리 기준
- `storage`는 업로드 담당이다.
- 장치 탐색과 `*.pcap` 식별 로직은 `scan` 단계 책임으로 유지한다.
- `storage` 안에 `src-extracted` 관련 처리나 후속 파이프라인 로직을 넣지 않는다.

## 9. 이번 작업에서 하지 않는 것

다음 항목은 이번 작업 범위에 포함하지 않는다.

- `udev` 이벤트 감지 구현
- `mount path` 확인 구현
- `*.pcap` 탐색 구현
- object key 최종 세부 규칙 확정
- 중복 처리 정책의 최종 확정
- 상세 로그 포맷 설계
- 운영 서버별 예외 케이스의 완전한 처리
- 테스트 코드의 완전한 구현
- `src-extracted` 관련 처리 구현

## 10. 완료 조건

이번 작업은 아래 조건을 만족하면 완료로 본다.

- `storage` package가 생성되어 있다.
- `MinioStorageClient`, `IngestUploader` 클래스가 생성되어 있다.
- scan 단계 결과를 받아 `ingest-staging` 업로드를 수행할 수 있는 구조가 존재한다.
- 업로드 대상 bucket과 object key를 기준으로 업로드 호출이 가능하다.
- 이후 `naming`, `logging` package가 붙을 수 있는 구조로 되어 있다

## 11. 산출물

이번 작업의 산출물은 다음과 같다.

- `storage` package 초기 클래스
- `ingest-staging` 업로드 구조
- MinIO 기본 접근 구조
- 후속 단계에서 활용 가능한 업로드 결과 구조

## 12. 후속 작업 연결

이번 작업이 완료되면 다음 구현 작업으로 이어진다.

- `naming` package 구현
- `logging` package 구현

즉, 이번 작업은 `ingest-staging` 업로드 기반을 고정하는 3차 작업이다.

## 13. 작업 시 주의 사항
- 현재 단계에서는 구조를 단순하게 유지한다.
- 아직 확정되지 않은 object key 세부 규칙을 코드에 과도하게 고정하지 않는다.
- 문서 기준을 벗어난 임의 확장을 하지 않는다.
- 업로드 대상 경로 규칙과 원본 파일명 유지 원칙을 분리해서 다룰 수 있도록 구조를 잡는다.
- 실제 구현에서는 업로드 실패 상태가 숨겨지지 않도록 결과 구조를 명확히 만든다.