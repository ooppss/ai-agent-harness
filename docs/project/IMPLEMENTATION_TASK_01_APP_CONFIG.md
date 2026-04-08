# IMPLEMENTATION_TASK_01_APP_CONFIG

## 1. 문서 목적

본 문서는 Storage Device PCAP Ingest / Relay Daemon의 첫 번째 구현 작업 지시를 정의한다.

이번 작업의 목적은 daemon의 전체 기능 구현을 바로 시작하는 것이 아니라, 이후 기능 구현의 기반이 되는 `app`, `config` package의 초기 뼈대를 만드는 것이다.

본 작업은 이후 `device`, `scan`, `storage`, `relay`, `naming`, `logging` 구현이 안정적으로 이어질 수 있도록 시작점과 설정 로드 구조를 고정하는 것을 목표로 한다.

---

## 2. 작업 범위

이번 작업의 구현 범위는 다음으로 제한한다.

- `app` package 생성
- `config` package 생성
- daemon 시작점 클래스 생성
- daemon 실행 흐름 클래스 생성
- `.properties` 설정 로드 클래스 생성
- 설정값 보관 클래스 생성
- 최소 실행 가능한 초기 구조 작성

이번 작업은 실제 장치 감지, 파일 탐색, MinIO 업로드, relay 이동 처리까지 구현하지 않는다.

---

## 3. 기준 문서

구현 Agent는 작업 전에 다음 문서를 반드시 확인해야 한다.

- `PROJECT_STATE.md`
- `docs/project/PRD.md`
- `docs/project/ARCHITECTURE.md`

문서 간 충돌이 있을 경우 최신 사용자 요청과 `PROJECT_STATE.md`를 우선 확인한다.

---

## 4. 이번 작업의 목표

이번 작업에서 달성해야 하는 목표는 다음과 같다.

1. daemon의 시작점을 명확히 만든다.
2. 설정 파일을 읽어 애플리케이션 전역에서 사용할 수 있는 구조를 만든다.
3. 이후 기능 package가 연결될 수 있는 실행 흐름의 뼈대를 만든다.
4. 실제 기능 구현 전에도 프로젝트 구조가 일관되게 유지되도록 한다.

---

## 5. 대상 package

이번 작업의 대상 package는 다음 두 개이다.

- `app`
- `config`

### 5.1 package 역할

- `app`
  - daemon 시작
  - 초기화
  - 실행 흐름 연결
  - 종료 흐름의 기본 틀 제공

- `config`
  - `.properties` 파일 로드
  - 설정값 보관
  - 다른 package가 설정값을 사용할 수 있도록 제공

---

## 6. 생성 대상 클래스

이번 작업에서 생성할 클래스는 다음과 같다.

### 6.1 app package

#### `DaemonApplication`
역할:
- daemon의 시작점
- `main` 메서드 제공
- 설정 로드 및 `DaemonRunner` 실행 시작

#### `DaemonRunner`
역할:
- 애플리케이션 실행 흐름의 1차 제어
- 초기 버전에서는 실제 무한 루프보다, 이후 기능을 연결할 수 있는 실행 뼈대 역할 수행
- 현재 단계에서는 config 로드 성공 여부를 바탕으로 시작 로그를 남기고 종료 가능한 수준이면 충분

### 6.2 config package

#### `AppConfig`
역할:
- `.properties`에서 읽은 설정값 보관
- MinIO endpoint, bucket 이름, 탐색 루트, 재시도 값 등 기본 설정을 담는 구조 제공

#### `ConfigLoader`
역할:
- 설정 파일을 읽어 `AppConfig` 객체 생성
- 필수 설정 누락 또는 파일 로드 실패 시 명확한 예외 발생

---

## 7. 권장 파일 배치

```text
daemon/
└─ src/
   └─ main/
      └─ java/
         └─ <base-package>/
            ├─ app/
            │  ├─ DaemonApplication.java
            │  └─ DaemonRunner.java
            └─ config/
               ├─ AppConfig.java
               └─ ConfigLoader.java
```

## 8. 설정 파일 처리 기준

설정 파일 형식은 `.properties`를 사용한다.

이번 작업에서 최소한 고려할 설정 항목 예시는 다음과 같다.

- MinIO endpoint
- MinIO access key
- MinIO secret key
- ingest-staging bucket 이름
- src-extracted bucket 이름
- 장치 탐색 루트
- 지정 경로 규칙
- mount path 재시도 횟수
- mount path 재시도 간격
- 로그 경로
- src-extracted 대상 경로 규칙

이번 단계에서는 위 항목 전체를 완전히 활용하지 않아도 되지만, `AppConfig` 구조상 수용 가능하도록 준비하는 것을 권장한다.

## 9. 구현 기준

### 9.1 app 구현 기준
- `DaemonApplication`은 진입점만 담당한다.
- 설정 로드, runner 생성, 실행 시작 수준까지만 맡는다.
- 장치 감지, 스캔, 업로드 같은 실제 기능 로직을 직접 포함하지 않는다.

### 9.2 config 구현 기준
- `ConfigLoader`는 `.properties` 파일을 읽어 `AppConfig`를 생성한다.
- 설정 로드 실패는 조용히 무시하지 않는다.
- 필수 항목이 없으면 명확히 실패해야 한다.
- 설정값 접근 구조는 이후 package에서 재사용 가능해야 한다.

### 9.3 구조 분리 기준
- app은 실행 흐름 제어
- config는 설정 제공
- 두 package의 책임을 섞지 않는다.

예:

`DaemonApplication` 안에 설정 파싱 로직을 길게 넣지 않는다.
`ConfigLoader` 안에 실행 흐름 제어 로직을 넣지 않는다.

## 10. 이번 작업에서 하지 않는 것

다음 항목은 이번 작업 범위에 포함하지 않는다.

- `udev` 연동 구현
- `libudev` + JNA 연동
- mount path 확인 구현
- *.pcap 탐색 구현
- MinIO 연결 및 업로드 구현
- src-extracted relay 구현
- object key 계산 구현
- 대상 경로 규칙 계산 구현
- 상세 로그 포맷 설계
- 테스트 코드의 완전한 구현

## 11. 완료 조건

이번 작업은 아래 조건을 만족하면 완료로 본다.

1. app, config package가 생성되어 있다.
2. `DaemonApplication`, `DaemonRunner`, `AppConfig`, `ConfigLoader` 클래스가 생성되어 있다.
3. `.properties` 파일을 읽어 `AppConfig` 객체를 만들 수 있다.
4. daemon 시작 시 설정 로드 후 runner를 실행하는 기본 흐름이 존재한다.
5. 실제 장치 감지 기능 없이도 애플리케이션 시작 구조가 이해 가능하다.
6. 이후 device, scan, storage, relay package가 붙을 수 있는 구조로 되어 있다.


## 12. 산출물

이번 작업의 산출물은 다음과 같다.

- app package 초기 클래스
- config package 초기 클래스
- 최소 설정 파일 예시 또는 샘플 설정 키 목록
- 이후 기능 연결이 가능한 시작 구조


## 13. 후속 작업 연결

이번 작업이 완료되면 다음 구현 작업으로 이어진다.

- device package 구현
- scan package 구현
- storage package 구현
- relay package 구현
- naming package 구현
- logging package 구현

즉, 이번 작업은 전체 구현의 시작점과 설정 기반을 고정하는 1차 작업이다.

## 14. 작업 시 주의 사항
- 현재 단계에서는 구조를 단순하게 유지한다.
- 아직 확정되지 않은 운영 규칙을 코드에 과도하게 박아 넣지 않는다.
- 문서 기준을 벗어난 임의 확장을 하지 않는다.
- 실제 기능 구현보다, 이후 기능을 안정적으로 붙일 수 있는 구조를 만드는 데 집중한다.