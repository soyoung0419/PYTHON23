📘 목적

대학생을 위한 통합 일정 관리 앱 CampusPlanner의 개발 지침서이다.
본 문서는 “어떻게(How)” 개발할지를 정의하며, 사용 언어, 프레임워크, 아키텍처, 클래스 구조를 포함한다.

⸻

⚙️ 개발 환경
	•	언어: Java (Android SDK 기반)
	•	IDE: Android Studio (Gradle 빌드)
	•	Database: Firebase Firestore (클라우드 데이터 저장)
	•	Local DB: Room (오프라인 캐싱)
	•	Notification: Android AlarmManager + NotificationManager
	•	UI: XML Layout, RecyclerView, ViewModel (MVVM 구조)
	•	Architecture: MVVM + 일부 Micro Service 연동 설계
→ 일정/시간표/알림/친구 기능을 독립 모듈화

⸻
📘디렉터리 구조

CampusPlanner/
├── app/
│   ├── java/com/example/campusplanner/
│   │   ├── MainActivity.java
│   │   ├── data/
│   │   │   ├── model/
│   │   │   │   ├── Task.java
│   │   │   │   ├── Schedule.java
│   │   │   │   ├── Course.java
│   │   │   │   ├── Friend.java
│   │   │   │   └── Diary.java
│   │   │   ├── dao/
│   │   │   │   ├── TaskDao.java
│   │   │   │   ├── ScheduleDao.java
│   │   │   │   └── FriendDao.java
│   │   ├── ui/
│   │   │   ├── task/          (할 일 목록 화면)
│   │   │   ├── schedule/      (시간표 화면)
│   │   │   ├── calendar/      (캘린더/다이어리)
│   │   │   ├── friend/        (친구 관리)
│   │   │   └── notification/  (알림)
│   │   ├── viewmodel/
│   │   │   ├── TaskViewModel.java
│   │   │   ├── ScheduleViewModel.java
│   │   │   ├── CalendarViewModel.java
│   │   │   └── FriendViewModel.java
│   │   └── util/
│   │       ├── NotificationHelper.java
│   │       └── DateTimeUtils.java
│   ├── res/layout/
│   │   ├── activity_main.xml
│   │   ├── fragment_task.xml
│   │   ├── fragment_schedule.xml
│   │   ├── fragment_calendar.xml
│   │   ├── fragment_friend.xml
│   │   └── item_task.xml
└── build.gradle



⸻

📘 주요 클래스 설계 

| 클래스명                   | 주요 역할                         |
| ---------------------- | ----------------------------- |
| **Task**               | 할 일 데이터 모델 (제목, 마감일, 완료 여부 등) |
| **TaskDao**            | Room DB 접근, CRUD 함수 정의        |
| **TaskViewModel**      | UI ↔ DB 데이터 바인딩 담당            |
| **Schedule**           | 과목명, 교수명, 강의실, 요일/시간, 성적 등    |
| **Friend**             | 친구 이름, UID, 시간표 참조 ID         |
| **NotificationHelper** | 오늘 할 일 / 수업 알림 예약 및 발송        |
| **Diary**              | 특정 날짜의 메모 / 다이어리 내용 저장        |
| **MainActivity**       | 네비게이션, 프래그먼트 전환 관리            |

