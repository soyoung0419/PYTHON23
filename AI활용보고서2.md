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

### ① UI와 로직 분리 → 유지보수성 극대화

UI를 바꾸더라도 로직(ViewModel) 수정은 필요 없음
예:

콘솔 UI → Android UI로 바꿔도 ViewModel 그대로 사용 가능

### ② 테스트 용이성 증가

ViewModel만 따로 테스트 가능 → UI 없이도 기능 검증 가능
“UID가 중복이면 예외 메시지 반환” 같은 테스트를 쉽게 작성.

### ③ 코드가 훨씬 더 견고해짐

DAO가 마지막 수비수라면, ViewModel은 미드필더
부정확한 데이터가 DAO로 내려가지 않도록 방어.

### ④ 대규모 프로젝트에서 책임 분배가 명확함

CampusPlanner 같은 팀 프로젝트에 매우 중요

- View 팀

- Model 팀

- ViewModel/Service 팀
이렇게 분업이 쉽다.


## **3. MVVM 구성 요소별 깊이 있는 역할**

### (1) Model

- 데이터 구조 (DTO/Entity)

- 순수 비즈니스 로직

- Repository/DAO 같은 저장소

- UI와 완전히 독립

- Model은 어떤 UI에서 쓰이는지도 모르는 상태가 이상적인 구조

### (2) View

- 사용자 입력/출력 담당

- Android에서는 Activity/Fragment

- Console 프로그램에서는 Scanner로 입력 받고 println으로 출력

View는 아래 같은 일을 하면 안 된다 (금지)
- “UID 중복인가?” 검사
- 날짜 포맷 오류 잡기
- 비즈니스 로직 호출 (DAO 직접 사용)

View는 오직 ViewModel 메서드만 호출해야 한다.


### (3) ViewModel (MVVM의 핵심 두뇌)

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



## **4. MVVM 구조를 도식화하면**
```
   [User]
      ↓ (입력)
   [View]  --- UI만 담당
      ↓
[ViewModel] --- 로직/검증/DAO호출
      ↓
   [Model] --- 데이터/저장 담당
      ↑
      └── DAO/Repository
```




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


### (2) DAO (데이터 저장/조회만 담당)
```
public class FriendDAO {

    private final Map<String, Friend> storage = new HashMap<>();

    public boolean exists(String uid) {
        return storage.containsKey(uid);
    }

    public void save(Friend f) { storage.put(f.getUid(), f); }

    public void delete(String uid) { storage.remove(uid); }

    public List<Friend> findAll() {
        return new ArrayList<>(storage.values());
    }
}
```
<br>DAO는 오직 저장소 역할만 한다.
검증, 예외처리, 메시지 출력 → 모두 ViewModel 책임이다.


### (3) ViewModel (핵심!)
```
public class FriendViewModel {

    private final FriendDAO dao;

    public FriendViewModel(FriendDAO dao) {
        this.dao = dao;
    }

    public String addFriend(String uid, String name) {

        // 1. 입력 검증
        if (uid == null || uid.isBlank())
            return "❌ UID를 입력하세요.";

        if (name == null || name.isBlank())
            return "❌ 이름을 입력하세요.";

        // 2. 중복 UID 검사 (DAO 호출)
        if (dao.exists(uid))
            return "❌ 이미 존재하는 UID입니다.";

        // 3. 정상 저장
        dao.save(new Friend(uid, name));
        return "✔ 친구 추가 완료.";
    }

    public String deleteFriend(String uid) {
        if (!dao.exists(uid))
            return "❌ 존재하지 않는 UID입니다.";

        dao.delete(uid);
        return "✔ 삭제되었습니다.";
    }

    public List<Friend> listFriends() {
        return dao.findAll();
    }
}
```

ViewModel은 DAO를 호출하지만 UI는 DAO를 직접 호출할 수 없음.
모든 보험, 필터, 보안, 검증 로직이 여기 모인다.


### (4) View (콘솔 UI)

```
public class FriendUI {

    private final FriendViewModel vm;
    private final Scanner sc = new Scanner(System.in);

    public FriendUI(FriendViewModel vm) {
        this.vm = vm;
    }

    public void start() {
        while (true) {
            System.out.println("\n1. 추가  2. 목록  3. 삭제  0. 종료");
            String sel = sc.nextLine();

            switch (sel) {
                case "1" -> addFriend();
                case "2" -> listFriends();
                case "3" -> deleteFriend();
                case "0" -> { return; }
            }
        }
    }

    private void addFriend() {
        System.out.print("UID: ");
        String uid = sc.nextLine();
        System.out.print("이름: ");
        String name = sc.nextLine();

        System.out.println(vm.addFriend(uid, name));
    }

    private void listFriends() {
        for (Friend f : vm.listFriends()) {
            System.out.println(f.getUid() + " - " + f.getName());
        }
    }

    private void deleteFriend() {
        System.out.print("삭제할 UID: ");
        System.out.println(vm.deleteFriend(sc.nextLine()));
    }
}
```

View는
- 출력만
- 입력만
하고 나머지는 전부 ViewModel이 처리한다.


## 6. Android에서의 MVVM 예시 

### (1)ViewModel
```
class LoginViewModel : ViewModel() {

    val loginResult = MutableLiveData<String>()

    fun login(id: String, pw: String) {
        if (id.isBlank() || pw.isBlank()) {
            loginResult.value = "아이디/비밀번호를 입력하세요."
            return
        }

        // Firebase Auth 호출 가능
        loginResult.value = "로그인 성공"
    }
}
```

### (2) View(Activity)

```
class LoginActivity : AppCompatActivity() {

    private val vm: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        vm.loginResult.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        binding.btnLogin.setOnClickListener {
            vm.login(
                binding.etId.text.toString(),
                binding.etPw.text.toString()
            )
        }
    }
}
```


### (3) View(Activity)
```
class LoginActivity : AppCompatActivity() {

    private val vm: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        vm.loginResult.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        binding.btnLogin.setOnClickListener {
            vm.login(
                binding.etId.text.toString(),
                binding.etPw.text.toString()
            )
        }
    }
}
```

Activity는
- DB 호출 안 함
- 검증 안 함
- 비즈니스 로직 없음
→ ViewModel이 결과를 LiveData로 넘기면 UI는 자동업데이트



## **7. MVVM의 심층 내부 원리**
### 1) ViewModel은 상태 관리자

UI가 회전해도(Activity 재생성) 데이터가 유지됨
(특히 안드로이드에서 중요)

### 2) View는 절대 Model을 직접 건드리지 않음

UI와 데이터 저장소 간에 어떤 연결도 존재하면 안 됨.
이것이 MVC 대비 MVVM의 가장 강력한 장점.

### 3) ViewModel에서는 이벤트를 상태(State)로 변환

View는 상태 변화만 관찰

### 4) Repository 패턴과 결합하면 확장성이 폭발적으로 증가

Android 실무에서는
ViewModel ↔ Repository ↔ Remote(API), Local(Room)
구조로 발전한다.



## **8. MVVM 단위 테스트 예시**

### ViewModel을 테스트하는 경우:
```
@Test
public void testAddFriend() {
    FriendDAO dao = new FriendDAO();
    FriendViewModel vm = new FriendViewModel(dao);

    String result = vm.addFriend("u1", "아무개");

    assertEquals("✔ 친구 추가 완료.", result);
    assertTrue(dao.exists("u1"));
}
```

View 없이도 모든 기능을 검증할 수 있다 → MVVM의 핵심 장점.




## **9. 실무에서 MVVM 사용할 때 팁**

- 오류/예외는 ViewModel에서 메시지 형태로 반환

UI는 메시지를 그대로 보여주면 됨

- ViewModel을 '스마트', View는 '멍청하게'

View = 바보

ViewModel = 천재
구조가 유지될수록 아키텍처 품질은 올라간다.

- DAO는 절대 검증 로직 넣지 말 것

DAO = 저장만 담당하는 계층
ViewModel = 논리적 판단 담당 계층
역할 섞이면 유지보수성 박살남

- UI 종류를 바꿀 수 있는 아키텍처 만들기

Console → Swing → Android → Web
View만 바꾸면 나머지는 재사용 가능



## **결론 요약**

ViewModel 패턴(MVVM)은 UI와 비즈니스 로직을 완전히 분리하여
테스트, 유지보수, 확장성을 극대화하는 구조이다.

### 구성 요소 역할 정리:
| 계층 | 역할 |
|------|------|
| **Model** | 데이터/비즈니스 로직 |
| **DAO/Repository** | 저장소 제어 |
| **ViewModel** | 검증 + 로직 + DAO 호출 |
| **View** | UI 처리 |

너처럼 CampusPlanner, Android 앱, 콘솔 기반 프로젝트, UI/백엔드 분리 과제를 하는 학생에게 가장 적합한 아키텍처야.



---

### 프롬프트
LocalStore가 메모리 기반 DB로 동작하는 이유와 장단점은 뭐야?

---

### 결과

아주 정확하고, “왜 LocalStore가 메모리 기반 DB처럼 동작하는지”를
CampusPlanner와 일반적인 소프트웨어 구조 관점에서 명확하게 설명해줄게.

## LocalStore가 ‘메모리 기반 DB’처럼 동작하는 이유

LocalStore는 내부적으로 이렇게 생겼지:
```
static class LocalStore implements Serializable {
    List<Task> tasks = new ArrayList<>();
    List<Schedule> schedules = new ArrayList<>();
    List<Friend> friends = new ArrayList<>();
    List<Diary> diaries = new ArrayList<>();
    List<GradeEntry> grades = new ArrayList<>();

    long taskSeq = 1;
    long scheduleSeq = 1;
    long friendSeq = 1;
    long diarySeq = 1;
    long gradeSeq = 1;
}
```

여기서 볼 수 있는 특징:

### 1) 모든 데이터(Task, Friend, Diary…)가 Java 객체로 메모리에 저장됨

- ArrayList에 보관

- DAO는 이 ArrayList를 직접 조작

- 조회도 메모리에서 Linear scan
→ 즉, 실행 중 데이터는 전부 메모리에서 작동

### 2) 디스크는 단지 “영속 저장 파일” 역할만 함

LocalDatabaseManager.save()는 애플리케이션 종료 시 또는 CRUD 시
```
ObjectOutputStream → 파일 저장
```

LocalDatabaseManager.load()는 시작 시
```
ObjectInputStream → 파일 읽어서 LocalStore 객체로 로드
```


즉,

- 실행 중: 메모리에서 동작 = 메모리 DB

- 종료/시작 사이: 파일로 저장 = 단순 파일 기반 영속성

형식적으로 보면 LocalStore는:

“실행 중에는 메모리 DB처럼 동작하고,
종료 시 파일로 스냅샷을 저장하는 방식의 in-memory database”

라고 할 수 있음.



## **LocalStore가 메모리 DB처럼 동작하는 이유(왜 이렇게 설계했나?)**

CampusPlanner 같은 로컬 콘솔 앱(또는 작은 데스크톱 앱)에서는
다음과 같은 요구사항이 있다:

### 이유 1) 속도가 매우 빠르다

메모리 기반 DB는 디스크 I/O가 없기 때문에
CRUD, 검색, 정렬이 즉각적이다.

- 친구 추가 → 리스트 추가

- 일기 검색 → 리스트 검색

-  Task 정렬 → 메모리에서 정렬

특히 Console 앱처럼 가볍고 빠르게 반응해야 하는 프로그램에서는 이상적인 구조다.

### 이유 2) 구현이 단순하다

SQL DB를 붙이거나 복잡한 ORM을 부르는 대신

List<Data>

File serialization

만으로 전체 DB 기능을 대체할 수 있다.

DAO 패턴 + 메모리 리스트 = 간단하면서도 강력한 구조.

### 이유 3) Java 객체를 그대로 저장하기에 개발 속도가 빠르다

별도로 DB 스키마를 설계할 필요 없음.

예:
```
Task → 그대로 직렬화
Friend → 그대로 직렬화
Diary → 그대로 직렬화
```

즉, 모델 객체가 DB 엔트리가 되는 구조
=> 개발 생산성이 매우 높음


### 이유 4) 작은 프로젝트·시뮬레이터·테스트 환경에 적합

CampusPlanner는:

- 복잡한 트랜잭션 필요 없음

- 서버와 동기화 필요 없음

- 단일 사용자 환경

- 단일 파일 저장 가능

따라서 메모리 기반 + 파일 저장 방식이 아주 적합하다.

### 이유 5) DB가 없어도 작동하는 “가벼운 자가 포함형 아키텍처”

외부 의존성 없음.

- 설치 불필요

- 서버 필요 없음

- SQLite 등도 필요 없음

프로그램 하나만으로 모든 기능이 동작한다.

이것은 교육용, 프로토타입, 콘솔 앱에서 특히 큰 장점이다.



## **LocalStore 메모리 DB 방식의 장점**

아주 중요하므로 항목별로 정리해줄게.

### 장점 1) 빠르다 (모든 연산이 메모리에서 수행되므로)

CRUD가 O(1)~O(n) 수준에서 즉시 수행된다.
디스크 I/O가 없으니 DB보다 10~100배 빠름.

### 장점 2) 설계/구현이 단순해진다

DB 스키마도 없고, ORM도 없고, 복잡한 SQL도 없다.
Java 객체 List를 조작하는 것이 전부다.

→ 프로토타이핑 최적
→ 학생 프로젝트에 이상적

### 장점 3) 모델 객체를 그대로 관리 가능 (No impedance mismatch)

SQL DB에서는:

- VARCHAR

- DATE

- INT
등으로 매핑해야 하지만

LocalStore에서는:

- String

- LocalDate

- LocalDateTime
그대로 사용 가능.

→ 개발 생산성이 매우 높음

### 장점 4) 오프라인 완전 독립 실행 가능

파일 하나만 있으면 데이터가 유지되며,
어떤 PC에서도 실행 가능.

DB 서버가 필요 없다는 점이 매우 큰 장점.

###  장점 5) 테스트가 쉬움

단위 테스트에서:
```
LocalStore store = new LocalStore();
TaskDao dao = new TaskDao(store, fakeDb);
```

이렇게 만들어두고 메모리에서 모든 동작을 검증할 수 있음.

## **LocalStore 메모리 DB 방식의 단점**

물론 이 방식도 한계가 있음.

### 단점 1) 데이터 크기가 커지면 성능 저하

100만 개 Task가 있으면?

- 메모리 부족 발생

- 정렬과 검색 성능 저하

- 파일 직렬화/역직렬화가 느려짐

→ 작은 앱에는 좋은데, 대규모에는 부적합

### 단점 2) 멀티스레드/다중 사용자 환경에서는 처리 어려움

LocalStore는 단일 사용자, 단일 프로세스 기준이다.

웹 서버나 여러 스레드가 동시에 접근하면
경쟁 조건(race condition)이 발생할 수 있다.

### 단점 3) 트랜잭션/롤백이 없다

DB와 달리:

- atomic update

- rollback

- concurrency control

같은 기능이 없다.

파일 저장 중 오류가 나면 문제가 될 수 있다.

### 단점 4) 데이터 일관성 보장이 약하다

직렬화 파일이 손상되면 전체 데이터가 날아갈 위험이 있음.

실제 DB는 인덱스, 저널링, 트랜잭션 로그 등으로 안전하게 보관하지만
LocalStore는 단순 파일 하나만 관리한다.

### 단점 5) 많은 데이터 처리에는 적합하지 않음

데이터 양이 커지면 List 기반 검색 O(n)은 한계가 있다.
DB 인덱스(트리 기반 검색)가 없으므로 검색에 비용이 큼.

## 결론

### LocalStore는 메모리 기반 DB처럼 동작하는 이유

- 모든 데이터를 Java 객체 리스트(List)에 저장하고

- 실행 중에는 메모리에서만 CRUD를 처리하며

- 종료할 때만 파일로 저장하는 구조이기 때문이다.

즉,

메모리 DB(In-Memory DB) + 파일 스냅샷 기반 영속성 구조다.

### 장점

- 매우 빠름

- 개발/구현 쉬움

- 외부 DB 필요 없음

- 테스트 쉬움

- 작은 프로젝트에 최적

### 단점

- 대규모 데이터 부적합

- 멀티 사용자/멀티 스레드 환경 부적합

- 트랜잭션/롤백 없음

- 파일 손상 시 전체 데이터 손실 위험


## 코드 예시 

### 1. LocalStore = “메모리 테이블” 구조

LocalStore는 진짜로 DB 테이블처럼 동작한다.
하지만 저장은 ArrayList이므로 완전한 “메모리 기반”이다.

```
static class LocalStore implements Serializable {
    List<Task> tasks = new ArrayList<>();       // 메모리 테이블 1
    List<Schedule> schedules = new ArrayList<>(); // 메모리 테이블 2
    List<Friend> friends = new ArrayList<>();   // 메모리 테이블 3
    List<Diary> diaries = new ArrayList<>();    // 메모리 테이블 4

    long taskSeq = 1;       // PK Auto Increment 시퀀스
    long scheduleSeq = 1;
    long friendSeq = 1;
    long diarySeq = 1;
}
```

이게 바로 메모리 DB의 정의와 거의 동일하다:

DB 테이블 → List<T>

Row 추가 → list.add()

PK → seq++

Row 검색 → stream().filter()

전체 조회 → getAll()


### 2. DAO가 LocalStore를 메모리 기반 DB처럼 사용하는 코드

DAO는 SQL 대신 LocalStore 리스트를 직접 조작한다.

#### TaskDao.addNew()
```
Task addNew(String title, LocalDateTime dueAt, String memo) {
    long id = store.taskSeq++;        // PK auto increment
    Task t = new Task(id, title, dueAt, false, memo);

    store.tasks.add(t);               // 메모리 DB에 insert
    db.save(store);                   // 스냅샷 저장

    return t;
}
```

SQL로 쓰면 다음과 같은 의미다:

```
INSERT INTO tasks(id, title, dueAt, completed, memo)
VALUES(seq++, title, dueAt, false, memo);
```

#### TaskDao.get()
```
public Optional<Task> get(long id) {
    return store.tasks.stream()
                       .filter(t -> t.id == id)
                       .findFirst();
}
```


SQL이면:
```
SELECT * FROM tasks WHERE id = ?
```

하지만 여기서는 메모리에서 즉시 조회됨 → 매우 빠름.



#### TaskDao.update()
```
public void update(Task t) {
    db.save(store);   // 메모리 내용이 이미 업데이트 되어 있으므로 저장만 하면 됨
}
```

SQL이면:
```
UPDATE tasks SET ... WHERE id=?
```

하지만 메모리 DB에서는 객체가 참조로 연결돼 있으므로
이미 변경된 내용이 저장됨.


### 3. 메모리 기반 CRUD 동작 예시

아래 코드는 메모리 기반 DB임을 직관적으로 이해할 수 있는 예시다.

#### (1) insert 후 즉시 다른 DAO에서 접근 가능
```
TaskDao dao = new TaskDao(store, db);
dao.addNew("네트워크 과제", LocalDateTime.now(), "Canvas 제출");

// insert 직후
System.out.println(store.tasks.size());     // 1
System.out.println(store.tasks.get(0).title); // "네트워크 과제"
```

DB 커넥션, 트랜잭션 필요 없이 바로 메모리 반영.

#### (2) update는 객체 참조를 자동으로 변경
```
Task t = dao.get(1).get();
t.completed = true;     // update

dao.update(t);

System.out.println(store.tasks.get(0).completed); 
// true → 메모리에 즉시 반영됨
```

객체가 “참조로 연결”되어 있기 때문에
update가 그냥 객체 수정으로 처리됨.

그래서 메모리 DB처럼 매우 빠르고 간단하다.

#### (3) delete도 즉시 메모리에서 제거됨
```
dao.delete(1);  // tasks.remove()

System.out.println(store.tasks.size());  // 0
```

SQL로 따지면:
```
DELETE FROM tasks WHERE id = 1;
```

하지만 리스트 remove()가 훨씬 빠르다(O(n)).

### 4. LocalStore가 “스냅샷 기기반 영속 저장”을 하는 코드

실행 중에는 메모리에 저장되지만
종료 시에는 파일로 전체 DB를 저장하는 구조이다.

즉, 메모리 DB + 스냅샷 파일 DB 하이브리드다.

#### 저장: LocalDatabaseManager.save()
```
void save(LocalStore store) {
    try (ObjectOutputStream out = new ObjectOutputStream(
            new FileOutputStream(FILE_PATH))) {

        out.writeObject(store);     // LocalStore 전체를 파일로 저장

    } catch (Exception ex) {
        ex.printStackTrace();
    }
}
```

이건 DB에서 말하는 “checkpoint” 또는 “snapshot”과 동일하다.

#### 로드: LocalDatabaseManager.load()
```
LocalStore load() {
    try (ObjectInputStream in = new ObjectInputStream(
            new FileInputStream(FILE_PATH))) {

        return (LocalStore) in.readObject();  // 파일 → 메모리 로드

    } catch (Exception ex) {
        return new LocalStore();   // 파일 없으면 새 DB 생성
    }
}
```


프로그램 시작 시:

- 파일로부터 DB 전체를 읽어서

- 메모리(LocalStore)에 적재한다

→ In-memory DB 시작!


### 5. LocalStore가 메모리 DB임을 확실히 보여주는 “전체 동작 흐름”
실행 흐름 ↓↓

1. 프로그램 시작

2. 파일에서 LocalStore 객체 하나만 읽음

3. 메모리에 store가 로드됨

4. 모든 DAO 연산(insert/update/delete/get)은 store 리스트를 직접 조작

5. 사용 종료 시 store 전체를 다시 파일로 세이브

6. 다음 실행에서 다시 load()

이 구조는 Redis RDB snapshot, H2 in-memory 모드,
SQLite + WAL off와 유사한 방식이다.


### 6. 메모리 DB의 장점 — 코드로 직접 체감

#### 초고속 조회
```
long count = store.tasks.stream().filter(t -> t.completed).count();
```

이건 DB 쿼리보다 훨씬 빠름.

####  No SQL → 모델 객체 그대로 사용
```
LocalDateTime dt = task.dueAt;   // 그대로 사용
```

DB에서는 날짜 타입 변환이 필요하지만
LocalStore는 그 자체가 저장소라서 변환이 필요 없다.

#### DAO 단위 테스트가 쉬움
```
LocalStore store = new LocalStore();
TaskDao dao = new TaskDao(store, fakeDb);

dao.addNew("test", null, "");

assertEquals(1, store.tasks.size());
```

DB connection 없이 테스트 가능.



### 7. 메모리 DB의 단점 — 코드로 직접 드러나는 부분
#### 데이터가 많아지면 느려짐

검색이 항상 메모리 리스트 O(n)
```
store.tasks.stream().filter(...).findFirst();
```

#### 트랜잭션 없음

파일 저장 중 문제 생기면 rollback 불가능
```
db.save(store);  
// 실패 시 전체 데이터 손상 가능
```

#### 동시성 제약

멀티 스레드 환경에서는 unsafe
```
store.tasks.add(t);  // Thread-safe 아님
```


### 결론
#### LocalStore가 메모리 기반 DB로 동작하는 이유

- 모든 데이터가 Java 객체(List)에 저장됨

- CRUD가 메모리에서 즉시 실행됨

- 파일은 단지 스냅샷 용도

- DAO가 store 리스트를 일종의 “테이블”처럼 사용함

따라서:

LocalStore = In-memory DB(실행 중) + Snapshot File(종료 시)
→ 작고 빠르고 간단한 로컬 프로그램에 최적화된 구조
