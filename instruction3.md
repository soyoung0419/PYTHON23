## <Instruction.md (Console CampusPlanner)>


### 1. 목적 (Purpose)

CampusPlanner는 대학생을 위한 통합 일정 관리 프로그램으로,
할 일(To-Do), 시간표, 친구 목록, 다이어리, 성적 등을 콘솔 기반(Java) 환경에서 관리할 수 있도록 개발되었다.

본 문서는 “어떻게(How)” 개발되었는지에 대해 설명하며,
사용 기술, 아키텍처 구조, 데이터 저장 방식, 주요 클래스 설계를 포함한다.




### 2. 개발 환경 (Development Environment)

| 항목            | 내용                                                                 |
|-----------------|----------------------------------------------------------------------|
| **개발 언어**     | Java (JDK 11 이상)                                                   |
| **실행 방식**     | 콘솔 기반: javac CampusPlannerConsole.java && java CampusPlannerConsole |
| **UI 방식**      | GUI(X), **System.out + Scanner 입력 기반 Console UI**                |
| **데이터 저장**    | **Java Object Serialization → program_data.bin 파일 저장**           |
| **알림 기능**     | Android AlarmManager(X) → **ScheduledExecutorService 기반 콘솔 알림** |
| **아키텍처**      | **MVC + DAO + ViewModel 구조**                                       |
| **외부 라이브러리** | 없음 (**Pure Java**)                                                 |
| **파일 위치**     | 실행 경로 기준 program_data.bin 자동 생성                                |


                                                

### 3. 디렉터리 구조 

CampusPlanner/

├── CampusPlannerConsole.java

│

├── ── (코드 내부 구성 흐름) ───────────────────────────────
│
│   ├── 1) Utils (유틸리티 클래스)
│   │    ├── DateTimeUtils            # 날짜 파싱, 요일 변환, 형식화
│   │    └── NotificationHelper       # ScheduledExecutorService 기반 콘솔 알림
│   │
│   ├── 2) Model (Serializable 클래스들)
│   │    ├── Task                     # id, title, dueAt, memo, completed
│   │    ├── Schedule                 # courseName, room, 요일, 시작시간, 종료시간
│   │    ├── Friend                   # 이름, uid
│   │    ├── Diary                    # 날짜, 본문, 이미지 경로
│   │    └── GradeEntry               # 과목명, 점수, 등급 계산
│   │
│   ├── 3) Local Persistence (파일 저장/로드)
│   │    ├── LocalStore               # 모든 데이터 ArrayList 저장소 + id 시퀀스
│   │    └── LocalDatabaseManager     # program_data.bin.tmp → Atomic Move 저장
│   │
│   ├── 4) DAO (Data Access Object)
│   │    ├── Crud<T> interface        # add, get, getAll, update, delete
│   │    ├── TaskDao                  # Task 추가, 수정, 삭제, 임박 조회
│   │    ├── ScheduleDao              # 시간표 저장, 중복 검사, 요일별 가져오기
│   │    ├── FriendDao                # 친구 목록 저장, 삭제
│   │    ├── DiaryDao                 # 날짜별 다이어리 저장/조회
│   │    └── GradeDao                 # 성적 저장, byCourse(), id 기반 수정
│   │
│   ├── 5) ViewModel (비즈니스 로직 / 검증 로직 층)
│   │    ├── TaskViewModel            # Task + 알림 연결, 완료 toggle, dueSoon()
│   │    ├── ScheduleViewModel        # hasOverlap 검사 후 저장, 다음 수업 찾기
│   │    ├── FriendViewModel          # FriendDao 호출 (추후 중복검사 확장 가능)
│   │    ├── CalendarViewModel        # Diary + 할일 연결 (오늘보기)
│   │    └── GradeViewModel           # GPA, 평균 계산, 성적 요약 summary()
│   │
│   ├── 6) Console Handlers (메뉴 UI)
│   │    ├── printMenu()              # 메인 메뉴 출력
│   │    ├── handleTasks()            # 할 일 추가, 목록, 수정 등
│   │    ├── handleSchedule()         # 수업 추가, 중복 검사, 다음 수업 조회
│   │    ├── handleFriends()          # 친구 추가, 삭제, 조회
│   │    ├── handleDiary()            # 다이어리 추가, 수정, 삭제
│   │    ├── handleToday()            # 오늘 할 일 + 오늘 일기 동시에 보기
│   │    └── handleGrade()            # 성적 추가, GPA 계산, 요약
│   │
│   └── 7) Seed Data / Main 실행 흐름
│        ├── seed()                   # 첫 실행 시 테스트 데이터 자동 생성
│        └── main()                   # 전체 흐름 제어 + 저장 후 종료
│
├── program_data.bin                  # LocalStore 직렬화 파일 (자동 생성)
├── program_data.bin.tmp              # 저장 시 임시 파일
│
└── README.md / Instruction.md        # 문서 파일





### 4. 주요 클래스 설계

| 클래스                      | 역할                 | 주요 기능                                                   |
| -------------------------- | -------------------- | ----------------------------------------------------------- |
| **LocalStore**             | 메모리 기반 저장소        | Task, Schedule, Friend, Diary, Grade 저장 + ID 시퀀스        |
| **LocalDatabaseManager**   | 파일 저장/로드           | program_data.bin.tmp → program_data.bin 원자적 저장       |
| **Task**                   | 할 일 데이터 모델         | 제목, 마감일, 완료 여부, 메모 + isDueSoon() 제공           |
| **TaskDao**                | Task CRUD + 저장     | addNew(), delete(), toggleDone(), 저장 시 파일 즉시 반영    |
| **TaskViewModel**          | 할 일 비즈니스 로직       | Task 추가 시 알림 예약 / 완료 처리 시 알림 취소             |
| **Schedule**               | 주간 시간표 데이터        | 요일, 시작/종료 시각, overlaps() 겹침 검사, 다음 수업 계산  |
| **ScheduleViewModel**      | 시간표 처리 로직         | 겹침 검사 후 추가, 요일별/주간 조회, 다음 수업 보여주기       |
| **Friend**                 | 친구 정보             | 이름, UID 저장                                              |
| **FriendDao / VM**         | 친구 CRUD 관리         | 추가, 삭제, 조회 (중복 검사 로직 추가 가능)                  |
| **Diary**                  | 일기 데이터           | 날짜, 텍스트, 이미지 경로 저장                               |
| **CalendarViewModel**      | 다이어리 + 일정 연동     | 특정 날짜의 일기 + 마감할 일 조회, 오늘보기 기능 포함        |
| **GradeEntry**             | 성적 데이터           | 과목명, 점수 저장, 성적 등급(A+, B0 등) 변환                |
| **GradeViewModel**         | 성적 비즈니스 로직       | 시간표에 있는 과목만 성적 입력, 평균, GPA, 요약 계산         |
| **NotificationHelper**     | 콘솔 알림 처리          | ScheduledExecutorService 기반 알림 예약/취소 관리            |




