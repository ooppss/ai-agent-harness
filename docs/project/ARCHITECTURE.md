# ARCHITECTURE.md
## Storage Device PCAP Ingest Daemon 아키텍처 문서

### 1. 문서 목적
본 문서는 Storage Device PCAP Ingest Daemon의 주요 구성요소, 책임 분리, 데이터 흐름, 외부 의존성, 설계 제약을 정의한다.

이 문서는 구현 Agent, 테스트 Agent, 리뷰 Agent가 공통적으로 참조하는 아키텍처 기준 문서로 사용한다.

---

## 2. 시스템 개요
본 시스템은 Ubuntu 20.04 서버에서 상주 실행되는 Plain Java Service이다.

시스템은 다음 흐름으로 동작한다.

1. 서버에 외부 연결 저장장치가 연결되면 대상 블록디바이스 이벤트를 감지한다.
2. 장치의 mount path를 확인한다.
3. 장치 내부의 지정 경로에서 `*.pcap` 파일을 탐색한다.
4. 탐색된 `*.pcap` 파일을 MinIO `ingest-staging` bucket의 지정 경로에 업로드한다.

본 시스템은 `pcap` 파일의 분해, 센서별 추출, 내용 해석, `src-extracted` 관련 처리를 수행하지 않는다.

---

## 3. 상위 아키텍처

### 3.1 구성요소
시스템은 아래 구성요소로 나눈다.

- **Daemon Main**
  - 서비스 시작점
  - 설정 로드
  - 주요 컴포넌트 초기화
  - 서비스 생명주기 관리

- **Device Detection Module**
  - Linux `udev` 이벤트를 기준으로 대상 블록디바이스 연결 감지
  - `libudev`를 JNA로 연동하여 이벤트 수신
  - 단일 장치 처리 흐름 시작

- **Device Scan Module**
  - mount path 하위 지정 경로 탐색
  - `*.pcap` 파일 식별
  - 지정 경로 존재 여부 및 처리 대상 파일 존재 여부 확인

- **Ingest Upload Module**
  - `ingest-staging` bucket 업로드
  - object key 생성
  - 업로드 결과 기록

- **MinIO Client Module**
  - MinIO SDK 래핑
  - bucket 접근
  - 업로드 수행
  - object 존재 여부 확인

- **Logging Module**
  - 이벤트/오류 로그 기록
  - 처리 단위 추적

- **Configuration Module**
  - endpoint, bucket 이름, 탐색 경로, 재시도 정책 등 외부 설정 관리

---

## 4. 데이터 흐름

### 4.1 장치 → ingest-staging
1. Device Detection Module이 `udev` 이벤트를 통해 대상 블록디바이스 연결을 감지한다.
2. Mount path 확인을 시도한다.
3. mount path가 즉시 확인되지 않으면 짧은 재시도를 수행한다.
4. Device Scan Module이 지정 경로를 탐색한다.
5. `*.pcap` 파일을 찾으면 Ingest Upload Module이 업로드 작업을 수행한다.
6. Ingest Upload Module이 업로드 대상 object key를 계산한다.
7. MinIO Client Module이 `ingest-staging` bucket에 파일을 업로드한다.
8. Logging Module이 결과를 기록한다.

---

## 5. 주요 설계 규칙

### 5.1 입력 파일 규칙
- 입력 파일은 `*.pcap`만 처리한다.
- 장치 내부에는 `pcap` 외 다른 파일은 고려하지 않는다.
- 기본 입력 경로 구조는 다음과 같다.

```text
<차량유형>/<수집날짜>/<차량유형-차량번호>/
```

예)
U100/20260327/U100-009/

### 5.2 파일명 처리 규칙
- `pcap` 파일명은 변경하지 않는다.
- 원본 파일명 그대로 `ingest-staging`에 사용한다.
- `pcap` 파일명 형식은 다음과 같다.

```text
<sensor_name>_YYYYMMDDhhmmss.pcap
```

예)
cam_a_1_20260327112330.pcap
eth_e_a_20260327112330.pcap
imu_20260327112331.pcap
can_20260327112331.pcap

timestamp는 해당 pcap segment의 시작 시각 기준이다.
pcap 파일은 센서별로 1분 단위 분할 생성된다.

### 5.3 ingest-staging 저장 경로 규칙
- 업로드 대상 경로는 `차량유형/yyyy/mm/dd/yymmdd_v차량번호/` 규칙을 따른다.
- 예: `U100/2026/03/27/260327_v009/`
- 각 `pcap` 파일은 위 경로 아래에 원본 파일명 그대로 저장한다.

### 5.4 object key 계산 규칙
- object key는 입력 경로 구조 `<차량유형>/<수집날짜>/<차량유형-차량번호>/`를 이용해 계산한다.
- 상위 경로는 `차량유형/yyyy/mm/dd/` 규칙을 따른다.
- 하위 폴더는 `yymmdd_v차량번호/` 규칙을 따른다.
- `yyyy/mm/dd`와 `yymmdd`는 입력 경로의 수집날짜(YYYYMMDD)에서 추출한다.
- `v차량번호`는 `<차량유형-차량번호>`에서 차량번호만 추출한 뒤 접두어 `v`를 붙여 생성한다.
- 파일명은 원본 `pcap` 파일명을 유지한다.
- object key의 세부 예외 규칙은 운영 기준 확정 시 추가 반영한다.

### 5.5 중복 처리 규칙
- 동일 object key가 이미 존재하면 해당 파일 업로드는 수행하지 않고 skip 처리한다.
- skip은 실패와 구분하여 기록한다.
- skip 처리된 파일이 있어도 나머지 파일 처리는 계속 진행할 수 있어야 한다.

### 5.6 외부 저장 장치 감지 및 처리 범위 규칙
- 장치 감지는 이벤트 기반으로 처리한다.
- Linux `udev` 이벤트를 기준으로 동작한다.
- mount path가 즉시 확인되지 않으면 짧은 재시도를 수행한다.
- 초기 구현은 단일 장치 처리 방식으로 제한한다.

## 6. 외부 의존성

### 6.1 운영체제
- Ubuntu 20.04
- `udev` 이벤트 감지가 가능해야 한다.
- 대상 블록디바이스의 mount path 확인이 가능해야 한다.

### 6.2 MinIO
- endpoint 접근 가능해야 한다.
- `ingest-staging` bucket 접근 권한이 필요하다.

### 6.3 Java
- Plain Java Service 형태
- Maven 기반 프로젝트 구조
- `libudev` 연동을 위한 JNA 사용
- MinIO Java SDK 사용 가능 구조 권장

## 7. 설정 항목

설정 파일 형식은 `.properties`를 사용한다.

다음 값은 외부 설정으로 관리한다.

- MinIO endpoint
- MinIO access key
- MinIO secret key
- `ingest-staging` bucket 이름
- 장치 탐색 루트
- 지정 경로 규칙
- 로그 경로
- mount path 재시도 횟수
- mount path 재시도 간격
- 중복 처리 정책 관련 설정값

## 8. 장애 및 예외 처리

### 8.1 장치 관련
- `udev` 이벤트 수신 실패
- mount path 미확인
- mount path 재시도 후에도 확인 실패
- 지정 경로 미존재
- `pcap` 파일 미발견

### 8.2 MinIO 관련
- endpoint 연결 실패
- bucket 접근 실패
- 업로드 실패
- object key 계산 실패
- 동일 object key 존재 시 skip 처리

### 8.3 부분 실패
- 일부 파일만 업로드 성공

모든 실패는 로그에 기록해야 하며, 재처리 가능하도록 충분한 정보를 남겨야 한다.
skip은 실패와 구분되는 별도 결과로 기록해야 한다.

## 9. 구현 제약
- 장치 감지는 `udev` 이벤트 기반으로 구현한다.
- Java와 `udev`의 연동은 `libudev` + JNA를 사용한다.
- 초기 구현은 단일 장치 처리 방식으로 제한한다.
- 설정 파일은 `.properties` 형식을 사용한다.
- 빌드 도구는 Maven을 사용한다.
- GUI를 두지 않는다.
- 사용자 승인 절차를 두지 않는다.
- `pcap` 파일을 분해하지 않는다.
- 장치 원본 데이터는 수정하거나 삭제하지 않는다.
- 파일명은 유지하고, 업로드 대상 경로는 `차량유형/yyyy/mm/dd/yymmdd_v차량번호/` 규칙을 따른다.
- 시스템은 단일 서버 상주 서비스 기준으로 설계한다.
- daemon은 `src-extracted` 관련 처리를 수행하지 않는다.
- daemon은 `dex-cli`를 실행하지 않는다.

## 10. 테스트 관점의 핵심 포인트
- 장치 연결 후 mount path 감지 가능 여부
- 지정 경로의 `pcap` 탐색 여부
- `ingest-staging` 업로드 성공 여부
- object key 계산 규칙 적용 여부
- 업로드 경로 규칙 적용 여부
- 로그 기록 정확성
- 입력 경로에서 날짜와 차량번호를 추출해 object key 규칙에 맞게 조합하는지
- 동일 object key 존재 시 overwrite 하지 않고 skip 처리되는지

## 11. 향후 확장 가능 지점
- 장치 감지 구현을 더 안정적인 운영 방식으로 고도화
- 다중 장치 동시 처리
- 처리 이력 영속 저장
- 관리자용 상태 조회 CLI 추가
- 후속 앱 연계 인터페이스 추가