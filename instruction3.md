## <Instruction.md (CampusPlannerConsole)>


### 1. 목적 

CampusPlanner는 대학생을 위한 통합 일정 관리 프로그램으로,
할 일(To-Do), 시간표, 친구 목록, 다이어리, 성적 등을 콘솔 기반(Java) 환경에서 관리할 수 있도록 개발되었다.

본 문서는 “어떻게(How)” 개발되었는지에 대해 설명하며,
사용 기술, 아키텍처 구조, 데이터 저장 방식, 주요 클래스 설계를 포함한다.




### 2. 개발 환경 

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



```plaintext
CampusPlanner/
├── CampusPlannerConsole.java      (메인 전체 통합 코드: Model + DAO + ViewModel + Main)
│
├── 1) Utils (도움 클래스)
│   ├── DateTimeUtils              
│   └── NotificationHelper       
│
├── 2) Model (Serializable 데이터 클래스)
│   ├── Task                       
│   ├── Schedule                   
│   ├── Friend                    
│   ├── Diary                     
│   └── GradeEntry                 
│
├── 3) Local Persistence (파일 저장/로드)
│   ├── LocalStore                 
│   └── LocalDatabaseManager       
│
├── 4) DAO (Data Access Object)
│   ├── TaskDao                    
│   ├── ScheduleDao               
│   ├── FriendDao                 
│   ├── DiaryDao                 
│   └── GradeDao                   
│
├── 5) ViewModel (비즈니스 로직 계층)
│   ├── TaskViewModel              
│   ├── ScheduleViewModel          
│   ├── FriendViewModel           
│   ├── CalendarViewModel          
│   └── GradeViewModel             
│
├── 6) Console Handlers (UI)
│   ├── printMenu()                
│   ├── handleTasks()              
│   ├── handleSchedule()        
│   ├── handleFriends()         
│   ├── handleDiary()             
│   ├── handleToday()             
│   └── handleGrade()             
│
├── 7) Seed Data & Main
│   ├── seed()                     
│   └── main()                    
│
├── program_data.bin               
└── program_data.bin.tmp           
```






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



### 5. 저장 구조

| 항목       | 방식                                                          |
|------------|---------------------------------------------------------------|
| **파일 이름**  | program_data.bin                                          |
| **저장 형식**  | Java ObjectOutputStream (객체 직렬화 방식)                |
| **안전 저장**  | .tmp 파일에 먼저 저장 후 → 프로그램 종료 직전에 Atomic Replace 적용 |
| **저장 위치**  | .java 파일이 실행된 현재 디렉터리(working directory)    |
| **데이터 포함**| Task, Schedule, Friend, Diary, GradeEntry, 각 ID 시퀀스 값    |                                                                               




