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

