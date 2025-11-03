## **<Instruction.md (Console + JSON 기반 CampusPlanner)>**

### 1. 목적 (Purpose)

CampusPlanner는 대학생을 위한 통합 일정 관리 프로그램으로,
할 일(To-Do), 시간표, 친구 목록, 다이어리, 성적 등을 콘솔 기반(Java) 환경에서 관리할 수 있도록 개발되었다.

본 문서는 “어떻게(How)” 개발되었는지에 대해 설명하며,
사용 기술, 아키텍처 구조, 데이터 저장 방식, 주요 클래스 설계를 포함한다.

### 2. 개발 환경 (Development Environment)

| 항목                             | 내용                                                                                  |
| -------------------------------- | ------------------------------------------------------------------------------------- |
| **개발 언어**                    | Java (JDK 11 이상)                                                                    |
| **IDE**                          | IntelliJ IDEA, Eclipse, VS Code 등                                                   |
| **실행 방식**                    | 콘솔 기반: `javac CampusPlanner_FullBadMerged.java && java CampusPlanner_FullBadMerged` |
| **데이터 저장 방식**            | JSON 파일(`program_data.json`) 저장 (Gson 사용)                                       |
| **알림 기능**                    | Java `ScheduledExecutorService` 기반 콘솔 알림              |
| **UI 방식**                      | `Scanner + System.out.println()` 텍스트 기반                         |
| **아키텍처**                     | MVC + DAO + ViewModel 구조                                                           |
| **사용 라이브러리**             | Gson (JSON 직렬화/역직렬화)                                                          |
                                                

