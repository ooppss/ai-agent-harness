# AGENT_EXECUTION_REQUEST_01

## 1. 목적

본 요청의 목적은 Storage Device PCAP Ingest Daemon의 1차 구현을 시작하는 것이다.

구현 범위는 이미 정의된 구현 작업 문서 기준에 따라 진행하며, 이번 실행 요청에서는 `task 1`부터 `task 4`까지의 package 뼈대와 기본 연결 구조를 구현 대상으로 한다.

이번 요청은 전체 기능 완성보다, 문서 기준에 맞는 초기 코드 구조와 책임 분리를 정확하게 반영한 1차 구현을 목표로 한다.

---

## 2. 작업 대상

구현 대상은 Ubuntu 20.04 서버에서 동작하는 Plain Java Service 기반 daemon이다.

사용 기술 및 기준은 다음을 따른다.

- Java
- Maven
- `.properties` 설정 파일
- MinIO
- Linux `udev`
- `libudev` + JNA 연동 방식
- 단일 장치 처리 기준

---

## 3. 반드시 확인할 문서

작업 시작 전 아래 문서를 반드시 확인한다.

1. `PROJECT_STATE.md`
2. `docs/project/PRD.md`
3. `docs/project/ARCHITECTURE.md`
4. `docs/project/IMPLEMENTATION_TASK_01_APP_CONFIG.md`
5. `docs/project/IMPLEMENTATION_TASK_02_DEVICE_SCAN.md`
6. `docs/project/IMPLEMENTATION_TASK_03_STORAGE.md`
7. `docs/project/IMPLEMENTATION_TASK_04_NAMING_LOGGING.md`

문서 간 충돌이 있을 경우 최신 사용자 요청과 `PROJECT_STATE.md`를 우선 기준으로 삼는다.

---

## 4. 이번 실행의 작업 범위

이번 실행에서는 다음 package의 초기 구현을 수행한다.

- `app`
- `config`
- `device`
- `scan`
- `storage`
- `naming`
- `logging`

구현은 각 task 문서의 범위를 벗어나지 않아야 한다.

즉, 이번 실행의 핵심은 다음과 같다.

- package 구조 생성
- 각 package의 대표 클래스 생성
- 문서 기준에 맞는 책임 분리 반영
- package 간 최소 연결 구조 마련
- 이후 통합 작업이 가능한 초기 코드 뼈대 확보

---

## 5. 구현 순서

다음 순서를 기본으로 작업한다.

1. `IMPLEMENTATION_TASK_01_APP_CONFIG.md`
2. `IMPLEMENTATION_TASK_02_DEVICE_SCAN.md`
3. `IMPLEMENTATION_TASK_03_STORAGE.md`
4. `IMPLEMENTATION_TASK_04_NAMING_LOGGING.md`

앞 단계 산출물을 다음 단계 입력으로 자연스럽게 연결해야 한다.

---

## 6. 구현 원칙

구현 시 반드시 다음 원칙을 지킨다.

- 문서에 정의되지 않은 기능을 임의로 확장하지 않는다.
- package 책임을 섞지 않는다.
- 초기 구현은 단순한 구조를 유지한다.
- 최종 운영 규칙이 미정인 항목은 하드코딩하지 않는다.
- 장치 전체 무차별 탐색을 하지 않는다.
- `*.pcap` 파일만 처리 대상으로 본다.
- 업로드 대상 경로는 `차종/yyyy/mm/dd/yyddmm_차번호/` 규칙을 반영할 수 있는 구조로 만든다.
- 원본 `pcap` 파일명은 유지한다.
- `src-extracted` 관련 처리는 구현하지 않는다.
- 성공 / 실패를 구분 가능한 구조를 유지한다.

---

## 7. 이번 실행에서 기대하는 산출물

최소 산출물은 다음과 같다.

### 7.1 package
- `app`
- `config`
- `device`
- `scan`
- `storage`
- `naming`
- `logging`

### 7.2 클래스
- `DaemonApplication`
- `DaemonRunner`
- `AppConfig`
- `ConfigLoader`
- `DeviceEventListener`
- `MountPathResolver`
- `PcapScanner`
- `ScanResult`
- `MinioStorageClient`
- `IngestUploader`
- `ObjectKeyBuilder`
- `DaemonLogger`
- `ErrorLogger`

### 7.3 구현 결과 성격
- Maven 기반 초기 프로젝트 구조
- `.properties` 기반 설정 로드 구조
- 장치 감지 → mount path 확인 → 지정 경로 탐색 구조
- `ingest-staging` 업로드 구조
- object key 계산 구조
- 공통 로그 / 오류 기록 구조

---

## 8. 이번 실행에서 하지 않는 것

다음 항목은 이번 실행에서 완성 목표로 두지 않는다.

- 최종 운영 규칙 확정
- 외부 모니터링 시스템 연동
- 운영 대시보드 구현
- 예외 케이스의 완전한 처리
- 테스트 코드의 완전한 작성
- 운영 배포 자동화 완성
- `dex-cli` 실행
- `pcap` 분해
- 센서별 추출
- dataset 생성
- `src-extracted` 관련 처리

---

## 9. 결과 보고 방식

작업 완료 후 최소한 다음 내용을 보고한다.

1. 생성한 package 목록
2. 생성한 클래스 목록
3. 각 클래스의 역할 요약
4. 문서 기준과 실제 구현의 차이 여부
5. 현재 남아 있는 미구현 항목
6. 다음 통합 작업에 필요한 후속 조치

---

## 10. 완료 기준

이번 실행은 아래 조건을 만족하면 완료로 본다.

- task 1~4의 현재 기준 대상 package와 클래스가 생성되어 있다.
- 문서 기준에 맞는 책임 분리가 반영되어 있다.
- package 간 최소 연결 구조가 존재한다.
- 문서 기준을 벗어난 임의 확장이 없다.
- 다음 통합 작업으로 넘어갈 수 있는 초기 코드 뼈대가 준비되어 있다.

---

## 11. 주의 사항

- 구현 속도보다 문서 기준 일치가 우선이다.
- 초기 단계에서는 과도한 추상화보다 이해 가능한 구조를 우선한다.
- 미정 규칙은 확장 가능하게 남겨둔다.
- 작업 중 중요한 불일치가 발견되면 임의 결정하지 말고 보고한다.