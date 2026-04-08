# PRD.md
## Storage Device PCAP Ingest Daemon 제품 요구사항 문서

### 1. 문서 목적
본 문서는 Ubuntu 20.04 서버에 상주 실행되는 Plain Java Service 기반 daemon의 제품 요구사항을 정의한다.

본 daemon의 목적은 다음과 같다.

1. 서버에 외부 연결 저장장치가 연결되면 대상 블록디바이스 이벤트를 감지한다.
2. mount path를 확인한다.
3. 장치 내부의 지정 경로에서 `*.pcap` 파일을 탐색한다.
4. 탐색된 `*.pcap` 파일을 MinIO `ingest-staging` bucket의 지정 경로에 업로드한다.

본 daemon은 `pcap` 파일의 분해, 센서별 추출, 데이터 해석, dataset 생성, `dex-cli` 실행, `src-extracted` 관련 처리를 수행하지 않는다.  
해당 작업은 별도 앱 또는 별도 기능의 책임 범위로 본다.

---

## 2. 제품 개요

### 2.1 제품 설명
본 제품은 Ubuntu 20.04 서버에서 동작하는 Plain Java Service 기반 daemon이다.

본 daemon의 목적은 외부 연결 저장장치가 블록디바이스로 인식되는 경우 이를 감지하고, 장치 내부 지정 경로에서 `*.pcap` 파일을 탐색하여 MinIO `ingest-staging` bucket의 지정 경로로 업로드하는 것이다.

본 제품의 현재 책임 범위는 `ingest-staging` 업로드까지이며, `src-extracted` 관련 처리는 현재 범위에 포함하지 않는다.

### 2.2 주요 목표
- 외부 연결 저장장치의 블록디바이스 이벤트를 감지한다.
- 장치의 mount path를 확인한다.
- 장치 내부의 지정 경로에서 `*.pcap` 파일을 탐색한다.
- 탐색된 `*.pcap` 파일을 `ingest-staging` bucket의 지정 경로로 업로드한다.
- 저장 경로 규칙과 파일명 규칙을 일관되게 적용한다.

### 2.3 비목표
다음 항목은 본 daemon의 범위에 포함하지 않는다.

- GUI / 브라우징 인터페이스 제공
- 서버 로그인 처리
- 장치 내부 데이터 수정
- `pcap` 파일 분해 또는 센서별 데이터 추출
- `pcap` 내용 분석
- 후속 데이터 처리 앱의 내부 로직 구현
- `src-extracted` bucket으로의 이동 또는 복사
- 업로드 이후 후속 처리 파이프라인 실행

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

---

## 4. 범위 정의

### 4.1 In Scope
- 블록디바이스 이벤트 감지
- mount path 확인
- 지정 경로 기준 `*.pcap` 파일 탐색
- MinIO `ingest-staging` bucket 업로드
- `ingest-staging` object key 계산
- 업로드 처리 로그 및 오류 기록

### 4.2 Out of Scope
- `pcap extract`
- 센서별 분해
- dataset 생성
- `dex-cli` 실행
- `src-extracted` bucket으로의 이동 또는 복사
- `src-extracted` 내부 추출 결과 구조 생성
- 업로드 이후 후속 처리 파이프라인 실행

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
8. 식별된 `*.pcap` 파일에 대해 업로드 대상 object key를 계산한다.
9. 계산된 경로에 따라 `ingest-staging` bucket에 업로드한다.
10. 전체 처리 결과를 로그에 기록한다.


### 5.2 단계별 책임
#### 1단계: 외부 연결 저장장치 / 대상 블록디바이스 → ingest-staging
- 외부 연결 저장장치 / 대상 블록디바이스 인식
- mount path 확인
- 지정 경로 탐색
- `pcap` 파일 식별
- ingest-staging object key 계산
- `pcap` 업로드
- 결과 기록

---

## 6. 입력 데이터 규칙

### 6.1 장치 내 저장 구조
장치 내부 입력 경로 구조는 다음 형태를 따른다.

`<차량유형>/<수집날짜>/<차량유형-차량번호>/`

예:
- `U100/20260327/U100-009/`

`pcap` 파일명 규칙은 다음 형태를 따른다.

`<sensor_name>_YYYYMMDDhhmmss.pcap`

예:
- `cam_a_1_20260327112330.pcap`
- `eth_e_a_20260327112330.pcap`
- `imu_20260327112331.pcap`
- `can_20260327112331.pcap`

timestamp는 해당 `pcap` segment의 시작 시각 기준이다.  
`pcap` 파일은 센서별로 1분 단위 분할 생성된다.  
파일명은 업로드 시에도 원본을 유지한다.

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

## 7. 저장 규칙
### 7.1 ingest-staging 저장 경로 규칙
daemon은 장치에서 탐색된 `*.pcap` 파일을 `ingest-staging` bucket에 업로드해야 한다.

업로드 대상 경로는 다음 규칙을 따른다.

`차량유형/yyyy/mm/dd/yymmdd_v차량번호/`

예:
- `U100/2026/03/27/260327_v009/`

각 `pcap` 파일은 위 경로 아래에 원본 파일명 그대로 저장한다.

### 7.2 object key 구성 원칙
daemon은 입력 경로 구조 `<차량유형>/<수집날짜>/<차량유형-차량번호>/`를 이용해 업로드 대상 object key를 계산해야 한다.

- 상위 경로는 `차량유형/yyyy/mm/dd/` 규칙을 따른다.
- 하위 폴더는 `yymmdd_v차량번호/` 규칙을 따른다.
- `yyyy/mm/dd`와 `yymmdd`는 입력 경로의 수집날짜(YYYYMMDD)에서 추출한다.
- `v차량번호`는 `<차량유형-차량번호>`에서 차량번호만 추출한 뒤 접두어 `v`를 붙여 생성한다.
- 파일명은 원본 `pcap` 파일명을 유지한다.
- object key의 세부 예외 규칙은 운영 기준 확정 시 추가 반영한다.

### 7.3 중복 처리
동일 object key가 이미 존재하면 해당 파일 업로드는 수행하지 않고 skip 처리한다.
skip은 실패와 구분하여 기록해야 하며, 나머지 파일 처리는 계속 진행할 수 있어야 한다.

## 8. 기능 요구사항
- FR-01. daemon은 외부 연결 저장장치가 블록디바이스로 인식되는 경우 이를 감지해야 한다.
- FR-02. daemon은 장치 이벤트 직후 mount path를 확인해야 하며, 즉시 확인되지 않으면 짧은 재시도를 수행해야 한다.
- FR-03. daemon은 장치 내부 지정 경로에서 `*.pcap` 파일을 탐색해야 한다.
- FR-04. daemon은 탐색된 `*.pcap` 파일을 MinIO `ingest-staging` bucket에 업로드해야 한다.
- FR-05. daemon은 업로드 대상 object key를 `차량유형/yyyy/mm/dd/yymmdd_v차량번호/` 규칙과 원본 `pcap` 파일명을 조합하여 계산해야 한다.
- FR-06. daemon은 업로드 시 원본 `pcap` 파일명을 변경하지 않아야 한다.
- FR-07. daemon은 업로드 성공 및 실패 결과를 추적 가능한 로그로 기록해야 한다.
- FR-08. 초기 구현은 단일 장치 처리 방식으로 제한해야 한다.

## 9. 비기능 요구사항
- NFR-01. 제품은 Ubuntu 20.04 서버에서 장기 실행 가능한 daemon 형태를 전제로 한다.
- NFR-02. 제품은 Java와 Maven을 기준으로 구현한다.
- NFR-03. 설정 파일 형식은 `.properties`를 사용한다.
- NFR-04. 장치 감지는 Linux `udev` 이벤트와 `libudev` + JNA 연동을 기준으로 한다.
- NFR-05. 오류 발생 시 원인 추적이 가능한 수준의 로그를 남겨야 한다.

## 10. 오류 처리 요구사항
- ER-01 mount path 미확인 : mount path를 찾지 못하면 처리 중단 후 오류를 로그에 기록해야 한다.
- ER-02 지정 경로 미존재 : 지정 경로가 존재하지 않으면 업로드를 수행하지 않고 로그를 기록해야 한다.
- ER-03 pcap 미발견 : `*.pcap` 파일이 없으면 업로드를 수행하지 않고 로그를 기록해야 한다.
- ER-04 MinIO 연결 실패 : MinIO 연결 실패 시 업로드를 중단하고 오류를 기록해야 한다.
- ER-05 object key 계산 실패 : 업로드 대상 경로를 계산할 수 없으면 해당 파일 업로드를 중단하고 오류를 기록해야 한다.
- ER-06 부분 업로드 실패 : 일부 파일만 업로드에 성공한 경우 부분 실패 상태를 기록하고 재처리 가능하도록 해야 한다.
- ER-07 duplicate object skip : 동일 object key가 이미 존재하면 해당 파일은 skip 처리하고, skip 사실을 로그에 기록해야 한다.

## 11. 수용 기준
- AC-01. 외부 연결 저장장치가 연결되면 daemon이 블록디바이스 이벤트를 감지할 수 있어야 한다.
- AC-02. daemon이 mount path를 확인하고 지정 경로에서 `*.pcap` 파일을 탐색할 수 있어야 한다.
- AC-03. 탐색된 `pcap` 파일이 `ingest-staging` bucket의 지정 경로에 업로드되어야 한다.
- AC-04. 업로드 경로가 `차량유형/yyyy/mm/dd/yymmdd_v차량번호/` 규칙과 일치해야 한다.
- AC-05. 업로드된 object 이름이 원본 `pcap` 파일명과 동일해야 한다.
- AC-06. 동일 object key가 이미 존재하면 해당 파일은 overwrite 되지 않고 skip 처리되어야 한다.
- AC-07. `src-extracted` 관련 처리 없이도 현재 요구사항을 만족하는 것으로 판정해야 한다.

## 12. Open Issues / Pending Inputs
- OI-01 외부 연결 저장장치 / 대상 블록디바이스 mount 경로 규칙 : 실제 운영 서버에서 장치가 어떤 경로로 mount되는지 운영 환경 확인이 필요하다.
- OI-02 대상 블록디바이스 식별 규칙 : 실제 운영 서버에서 대상 저장장치를 어떤 속성으로 식별할지 운영 환경 확인이 필요하다.
- OI-03 ingest-staging object key 세부 예외 규칙 : 기본 규칙은 확정되었으며, 예외 입력값 처리 방식의 운영 기준 확정이 필요하다.
- OI-04 중복 object skip 처리 세부 기준 : skip 결과 기록 방식과 배치 요약 반영 기준의 운영 상세 확정이 필요하다.

## 13. 구현 원칙
- 사용자 승인 없이 자동 실행한다.
- 장치 원본 데이터는 변경하지 않는다.
- `pcap` 파일은 분해하지 않는다.
- `pcap` 파일명은 원본을 유지한다.
- 모든 주요 동작은 로그로 남긴다.
- daemon은 `ingest-staging` bucket의 지정 경로 업로드까지만 수행한다.
- daemon은 `src-extracted` 관련 처리를 수행하지 않는다.
- daemon은 `dex-cli`를 실행하지 않는다.
- daemon은 `pcap` 파일을 extract하지 않는다.
