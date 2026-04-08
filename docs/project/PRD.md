# PRD.md
## Storage Device PCAP Ingest / Relay Daemon 제품 요구사항 문서

### 1. 문서 목적
본 문서는 Ubuntu 20.04 서버에 상주 실행되는 Plain Java Service 기반 daemon의 제품 요구사항을 정의한다.

본 daemon의 목적은 다음과 같다.

1. 서버에 외부 연결 저장장치가 연결되면 대상 블록디바이스 이벤트를 감지한다.
2. mount path를 확인한다.
3. 장치 내부의 지정 경로에서 `*.pcap` 파일을 탐색한다.
4. 탐색된 `*.pcap` 파일을 MinIO `ingest-staging` bucket에 업로드한다.
5. 업로드된 파일을 `src-extracted` bucket의 지정 폴더로 이동(move)한다.

본 daemon은 **pcap 파일의 분해, 센서별 추출, 데이터 해석**을 수행하지 않는다.  
해당 작업은 별도 앱 또는 별도 기능의 책임 범위로 본다.

---

## 2. 제품 개요

### 2.1 제품 설명
본 제품은 Ubuntu 20.04 서버에서 상주 실행되는 Java daemon이다.

daemon은 외부 연결 저장장치가 서버에 연결되었을 때 OS가 생성한 대상 블록디바이스 이벤트를 감지하고, mount path를 기준으로 장치를 스캔한다.

업로드는 2단계로 구성된다.

- **1단계**: 외부 연결 저장장치 / 대상 블록디바이스 → `ingest-staging`
- **2단계**: `ingest-staging` → `src-extracted`

본 daemon은 `pcap` 파일의 분해, 센서별 추출, 데이터 해석, `dex-cli` 실행을 수행하지 않는다.
`dex-cli` 실행은 linux에서 pcap 파일을 파싱하는 동작을 의미한다.
해당 작업은 서버에서 사람이 별도로 수행하는 후속 절차이다.

### 2.2 주요 목표
- 장치 삽입을 자동 감지한다.
- mount path를 기준으로 지정 경로를 탐색한다.
- 지정 경로 내 `*.pcap` 파일을 자동 업로드한다.
- 업로드 후 `src-extracted` bucket 내 지정 규칙의 대상 폴더를 생성하고 해당 위치로 파일을 이동한다.
- 전체 동작은 사용자 승인 없이 자동 수행한다.
- 중복 발생 시 자동 덮어쓰기한다.
- 모든 주요 동작과 오류를 로그로 남긴다.

### 2.3 비목표
다음 항목은 본 daemon의 범위에 포함하지 않는다.

- GUI / 브라우징 인터페이스 제공
- 서버 로그인 처리
- 장치 내부 데이터 수정
- `pcap` 파일 분해 또는 센서별 데이터 추출
- `pcap` 내용 분석
- 후속 데이터 처리 앱의 내부 로직 구현

---

## 3. 운영 환경

### 3.1 실행 환경
- 운영체제: Ubuntu 20.04
- 구현 언어: Java
- 실행 형태: Plain Java Service
- 실행 방식: 서버 상주 서비스 + 터미널 제어
- 사용자 인증: 서버 로그인으로 대체

### 3.2 저장소
- MinIO endpoint 사용
- 1차 bucket: `ingest-staging`
- 2차 bucket: `src-extracted`

---

## 4. 범위 정의

### 4.1 In Scope
- 외부 연결 저장장치의 블록디바이스 이벤트 감지
- mount path 확인
- 외부 연결 저장장치 / 대상 블록디바이스 내 지정 경로 탐색
- `*.pcap` 파일 식별
- `ingest-staging` bucket 업로드
- `src-extracted` bucket용 폴더 생성
- `src-extracted` bucket으로 파일 전달
- 중복 자동 덮어쓰기
- 로그 기록
- 설정 기반 동작

### 4.2 Out of Scope
- pcap 분해
- 센서별 폴더 생성
- 추출 데이터 검증
- 후속 분석/전처리
- 사용자 승인 절차
- 웹 UI/브라우저 UI
- `dex-cli` 실행
- `src-extracted` 내부의 추출 결과 구조 생성

---

## 5. 처리 흐름

### 5.1 전체 처리 흐름
1. 외부 연결 저장장치가 서버에 연결된다.
2. Ubuntu가 대상 블록디바이스 이벤트를 발생시킨다.
3. daemon이 `udev` 이벤트를 통해 장치 연결을 감지한다.
4. daemon이 mount path 확인을 시도한다.
5. mount path가 즉시 확인되지 않으면 짧은 재시도를 수행한다.
6. daemon이 장치 내부의 지정 경로를 탐색한다.
7. 지정 경로 내 `*.pcap` 파일을 식별한다.
8. 식별된 `*.pcap` 파일을 `ingest-staging` bucket에 업로드한다.
9. 업로드 완료 후 규칙에 따라 `src-extracted` bucket 내 대상 폴더를 생성한다.
10. 해당 파일을 `src-extracted` bucket의 대상 폴더로 이동한다.
11. 전체 처리 결과를 로그에 기록한다.

### 5.2 단계별 책임
#### 1단계: 외부 연결 저장장치 / 대상 블록디바이스 → ingest-staging
- 외부 연결 저장장치 / 대상 블록디바이스 인식
- mount path 확인
- 지정 경로 탐색
- pcap 업로드

#### 2단계: ingest-staging → src-extracted
- 업로드 파일 식별
- `src-extracted` 대상 폴더 생성
- 대상 폴더로 파일 이동
- 결과 기록

---

## 6. 입력 데이터 규칙

### 6.1 장치 내 저장 구조
장치 내부에서 `pcap` 파일은 기본적으로 다음 구조에 저장된다고 가정한다.

```text
<차량유형>/<수집날짜>/<차량유형-차량번호>/

예)
U100/20260327/U100-009/
```

### 6.2 입력 파일 종류
- 입력 파일은 `*.pcap`만 처리 대상으로 본다.
- 장치 내부에는 `pcap` 외 다른 파일은 고려하지 않는다.
- `pcap` 파일명 형식은 다음과 같다.

```text
<sensor_name>_YYYYMMDDhhmmss.pcap
```

예)
cam_a_1_20260327112330.pcap
eth_e_a_20260327112330.pcap
imu_20260327112331.pcap
can_20260327112331.pcap

- timestamp는 해당 pcap segment의 시작 시각 기준이다.
- pcap 파일은 센서별로 1분 단위 분할 생성된다.

### 6.3 지정 경로
daemon은 장치 전체를 무차별 탐색하지 않고, 사전에 정의된 지정 경로 기준으로 pcap 파일을 탐색해야 한다.
지정 경로의 최종 상세 규칙은 설정값 또는 운영 규칙으로 관리한다.
지정 경로의 기본 형태는 다음 구조를 따른다.

```text
<차량유형>/<수집날짜>/<차량유형-차량번호>/
```

## 7. 지정 규칙
### 7.1 ingest-staging 업로드 규칙
daemon은 장치에서 탐색된 *.pcap 파일을 ingest-staging bucket에 업로드해야 한다.
업로드 시 object key는 장치의 상대 경로 구조를 기준으로 유지하는 것을 기본 원칙으로 한다.

### 7.2 src-extracted 저장 규칙
daemon은 `ingest-staging`에 업로드된 pcap 파일을 후속 처리 입력용으로 `src-extracted` bucket의 지정 폴더에 전달해야 한다.

`src-extracted` 내 대상 폴더명 및 경로 규칙은 운영 기준에 따라 확정된 naming rule을 사용한다.
현재 daemon은 해당 규칙에 따라 대상 폴더를 생성하고 파일을 이동해야 한다.

### 7.3 src-extracted 구조 생성 책임
daemon은 src-extracted에 대상 폴더가 없으면 자동으로 생성 가능한 방식으로 저장을 수행해야 한다.

### 7.4 중복 처리
동일 object key 또는 동일 대상 경로가 존재하면 자동으로 덮어써야 한다.
별도 사용자 승인 절차는 두지 않는다.

### 7.5 ingest-staging → src-extracted 전달 방식(copy 후 source 삭제)
daemon은 `ingest-staging`에 업로드된 `pcap` 파일을 `src-extracted` bucket의 대상 폴더로 이동(move)해야 한다.

이동은 논리적으로 move이지만, 구현은 copy 후 source 삭제 방식으로 수행한다.
대상 경로 복사가 성공한 경우에만 source object를 삭제해야 한다.
복사 실패 시 source 삭제를 수행해서는 안 된다.
삭제 실패 시 부분 실패로 기록해야 한다.

## 8. 기능 요구사항
FR-01 장치 연결 감지 : 서비스는 외부 연결 저장장치에 대해 발생하는 대상 블록디바이스 이벤트를 감지해야 한다.

FR-02 mount path 확인 : 서비스는 OS가 생성한 mount path를 식별해야 한다.

FR-03 지정 경로 탐색 : 서비스는 mount path 하위의 지정 경로에서만 입력 파일을 탐색해야 한다.

FR-04 pcap 파일 식별 : 서비스는 `*.pcap` 파일만 업로드 대상으로 식별해야 한다.

FR-05 ingest-staging 자동 업로드 : 서비스는 탐색된 `pcap` 파일을 사용자 승인 없이 ingest-staging bucket에 자동 업로드해야 한다.

FR-06 src-extracted 대상 폴더 생성 : 서비스는 지정 naming rule에 따라 `src-extracted` bucket 내 대상 폴더를 생성할 수 있어야 한다.

FR-07 src-extracted 파일 전달 : 서비스는 `ingest-staging` 업로드 완료 후 해당 `pcap` 파일을 `src-extracted` bucket의 대상 폴더로 이동(move)해야 한다.
이동은 copy 후 source 삭제 방식으로 수행해야 한다.

FR-08 pcap 비분해 원칙 : 서비스는 `pcap` 파일을 분해하거나 센서별 데이터로 나누지 않아야 한다.

FR-09 중복 자동 덮어쓰기 : 서비스는 중복 경로가 존재할 경우 자동으로 덮어써야 한다.

FR-10 로그 기록 : 서비스는 다음 항목을 로그에 기록해야 한다.

- 장치 감지
- mount path 확인
- 지정 경로 탐색 시작/종료
- 탐색된 `pcap` 파일 목록 또는 개수
- ingest-staging 업로드 시작/완료/실패
- `src-extracted` 전달 시작/완료/실패
- 덮어쓰기 발생 여부
- 오류 발생 원인

## 9. 비기능 요구사항
NFR-01 상주 안정성 : 서비스는 장시간 상주 실행 중 반복적인 장치 삽입/제거 상황을 안정적으로 처리해야 한다.

NFR-02 무인 자동 실행 : 서비스는 사용자 승인 없이 자동으로 동작해야 한다.

NFR-03 원본 데이터 비변경 : 서비스는 장치 원본 데이터를 수정하거나 삭제해서는 안 된다.

NFR-04 추적 가능성 : 문제 발생 시 어떤 mount path와 어떤 `pcap` 파일이 어느 bucket과 경로로 업로드 및 이동되었는지 추적 가능해야 한다.

NFR-05 설정 가능성 : 다음 값은 외부 설정으로 관리 가능해야 한다.

- MinIO endpoint
- access key
- secret key
- bucket 이름
- 장치 탐색 루트
- 지정 경로 규칙
- 재시도 정책
- 로그 경로
- `src-extracted` 경로 규칙 적용 방식

NFR-06 장애 대응성 : 일시적 오류 발생 시 재시도 또는 실패 기록을 통해 재처리 가능해야 한다.

## 10. 오류 처리 요구사항
ER-01 mount path 미확인 : mount path를 찾지 못하면 처리 중단 후 오류를 로그에 기록해야 한다.

ER-02 지정 경로 미존재 : 지정 경로가 존재하지 않으면 업로드하지 않고 로그를 기록해야 한다.

ER-03 pcap 미발견 : `*.pcap` 파일이 없으면 업로드를 수행하지 않고 로그를 기록해야 한다.

ER-04 MinIO 연결 실패 : MinIO 연결 실패 시 업로드를 중단하고 오류를 기록해야 한다.

ER-05 부분 이동 실패 : 일부 파일만 업로드되었거나 `src-extracted`로의 이동이 일부만 완료된 경우 부분 실패 상태를 기록해야 한다.

ER-06 source 삭제 실패 : 대상 경로 복사는 성공했지만 source object 삭제에 실패한 경우 부분 실패로 기록하고 재처리 가능해야 한다.

## 11. 수용 기준
AC-01 장치 감지 : 외부 연결 저장장치에 대해 발생한 대상 블록디바이스 이벤트를 감지할 수 있어야 한다.

AC-02 지정 경로 탐색 : 외부 연결 저장장치 / 대상 블록디바이스 내부의 지정 경로에서 `*.pcap` 파일을 찾을 수 있어야 한다.

AC-03 ingest-staging 업로드 : 탐색된 `pcap` 파일이 자동으로 ingest-staging bucket에 업로드되어야 한다.

AC-04 src-extracted 전달 : 업로드된 `pcap` 파일이 지정 규칙에 따라 `src-extracted` bucket의 대상 폴더로 이동되어야 한다.

AC-05 pcap 비분해 : daemon은 `pcap` 파일을 분해하지 않고 그대로 전달해야 한다.

AC-06 중복 자동 덮어쓰기 : 중복 대상이 있으면 자동으로 덮어써야 한다.

AC-07 로그 기록 : 주요 이벤트와 오류가 로그에 남아야 한다.

## 12. Open Issues / Pending Inputs

OI-01 외부 연결 저장장치 / 대상 블록디바이스 mount 경로 규칙 : 실제 운영 서버에서 장치가 어떤 경로로 mount되는지 운영 환경 확인이 필요하다.
OI-02 대상 블록디바이스 식별 규칙 : 실제 운영 서버에서 대상 저장장치를 어떤 속성으로 식별할지 운영 환경 확인이 필요하다.
OI-03 src-extracted 경로 규칙 : 대상 폴더명 및 경로 생성 규칙을 최신 운영 기준으로 확정할 필요가 있다.

## 13. 구현 원칙
- 사용자 승인 없이 자동 실행한다.
- 장치 원본 데이터는 변경하지 않는다.
- `pcap` 파일은 분해하지 않는다.
- 파일 중복은 자동 덮어쓴다.
- `pcap` 파일명은 원본을 유지한다.
- `src-extracted` 경로 규칙은 최신 운영 기준에 따라 적용한다.
- 모든 주요 동작은 로그로 남긴다.
- daemon은 `src-extracted` bucket의 지정 폴더 생성 및 파일 이동까지만 수행한다.
- daemon은 `dex-cli`를 실행하지 않는다.
- daemon은 `pcap` 파일을 extract하지 않는다.

