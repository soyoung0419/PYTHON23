--- 

### 최소영

### 프롬프트
ViewModel 패턴(MVVM)에 대한 설명과 역할, 실제 코드예시를 알려줘

---

### 결과

아래에서 **MVVM(ViewModel 패턴)**을 “왜 쓰는지 → 어떻게 구성되는지 → 실제 코드에서는 어떻게 구현하는지” 순서로 완전 정리해줄게.

## **1. MVVM(ViewModel 패턴)이란?**

MVVM(Model–View–ViewModel)은 **UI(View)**와 **비즈니스 로직(Model)**을 확실하게 분리하기 위해 **중간층(ViewModel)**을 둔 아키텍처 패턴이다.

 즉,

- View는 오직 UI 처리

- Model은 오직 데이터/저장 처리

- ViewModel이 이 둘을 연결하며 모든 로직을 담당



## **2. 왜 MVVM이 필요한가? (근본적인 목적)**

① UI와 로직 분리 → 유지보수성 극대화

UI를 바꾸더라도 로직(ViewModel) 수정은 필요 없음
예:

콘솔 UI → Android UI로 바꿔도 ViewModel 그대로 사용 가능

② 테스트 용이성 증가

ViewModel만 따로 테스트 가능 → UI 없이도 기능 검증 가능
“UID가 중복이면 예외 메시지 반환” 같은 테스트를 쉽게 작성.

③ 코드가 훨씬 더 견고해짐

DAO가 마지막 수비수라면, ViewModel은 미드필더
부정확한 데이터가 DAO로 내려가지 않도록 방어.

④ 대규모 프로젝트에서 책임 분배가 명확함

CampusPlanner 같은 팀 프로젝트에 매우 중요

- View 팀

- Model 팀

- ViewModel/Service 팀
이렇게 분업이 쉽다.


## **3. MVVM 구성 요소별 깊이 있는 역할**

<br> (1) Model

- 데이터 구조 (DTO/Entity)

- 순수 비즈니스 로직

- Repository/DAO 같은 저장소

- UI와 완전히 독립

- Model은 어떤 UI에서 쓰이는지도 모르는 상태가 이상적인 구조

<br> (2) View

- 사용자 입력/출력 담당

- Android에서는 Activity/Fragment

- Console 프로그램에서는 Scanner로 입력 받고 println으로 출력

View는 아래 같은 일을 하면 안 된다 (금지)
- “UID 중복인가?” 검사
- 날짜 포맷 오류 잡기
- 비즈니스 로직 호출 (DAO 직접 사용)

View는 오직 ViewModel 메서드만 호출해야 한다.


<br>(3) ViewModel (MVVM의 핵심 두뇌)

역할을 더 깊게 보면:

✔ A. UI에서 넘어온 입력을 검증

- 값이 비었는지

- 포맷이 맞는지

- 중복인지

- 논리적으로 말이 되는지 (예: 종료 시간이 시작 시간보다 빠름)

✔ B. Model/DAO 호출

- 저장/조회/삭제 등의 핵심 행위 수행

- UI는 절대 DB를 직접 건드리지 않음

✔ C. 예외/오류 메시지 가공 후 UI로 전달

- 오류 발생 시 사용자 친화적인 메시지 반환

- View는 그대로 출력만 하면 됨

✔ D. 상태 저장 (Android에서는 LiveData, StateFlow 사용)

- UI에서 자동 반응 → ViewModel은 UI 코드를 전혀 몰라도 됨



## **5. 실전 예제 (CampusPlannerConsole 기반)**

### (1) Model
```
public class Friend {
    private final String uid;
    private final String name;

    public Friend(String uid, String name) {
        this.uid = uid;
        this.name = name;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
}
```
