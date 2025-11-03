### 최소영

### 프롬프트
MVC 구조에 대해 설명해주고 실제 코드 예시를 알려줘

---

### 결과

좋습니다! MVC(Model–View–Controller) 구조에 대해 이해하기 쉽게 설명하고, 실제 Java + Console 기반 또는 Android 기반 코드 예시까지 같이 보여드릴게요.

<br> (1) MVC 구조란?
<br> MVC는 프로그램을 세 가지 역할로 분리하여 구조화하는 디자인 패턴입니다.
| 구성요소                                  | 역할                     | 핵심 책임                             |
| ----------------------------------------- | ------------------------ | ------------------------------------- |
| **Model (모델)**                          | 데이터 및 그에 관련된 로직 | 예: Friend, Task, DB 저장 데이터 구조 |
| **View (뷰)**                             | 사용자에게 보여지는 화면(UI) | 예: 콘솔 출력, Android XML 화면       |
| **Controller (또는 MVVM의 ViewModel)**   | View와 Model 연결, 흐름 제어 | 예: 입력 처리, Model 업데이트, 중복 검사 |


<br> (2) MVC 흐름 간단 이미지 설명
```plaintext
사용자 입력 → [View] → [Controller] → [Model]
                   ↓           ↑
                 출력 ← [View] ←
```

<br> (3) 콘솔 기반 CampusPlanner의 MVC 예제 (Friend 관리)
<br>1. Model (Friend.java)
```plaintext
public class Friend implements Serializable {
    private String name;
    private String uid;

    public Friend(String name, String uid) {
        this.name = name;
        this.uid = uid;
    }

    public String getName() { return name; }
    public String getUid() { return uid; }

    @Override
    public String toString() {
        return name + " (" + uid + ")";
    }
}
```

<br>2. DAO (FriendDao.java) → Model 저장/조회만 담당
```plaintext
   public class FriendDao {
    private final List<Friend> friends = new ArrayList<>();

    public void add(Friend friend) {
        friends.add(friend);
    }

    public boolean existsByUid(String uid) {
        return friends.stream().anyMatch(f -> f.getUid().equals(uid));
    }

    public List<Friend> getAll() {
        return friends;
    }
}
```



<br>3. Controller/ViewModel (FriendController.java) → 비즈니스 로직 / 중복 UID 검사 담당
```plaintext
public class FriendController {
    private FriendDao dao;

    public FriendController(FriendDao dao) {
        this.dao = dao;
    }

    // 중복 UID 검사 + 예외 처리
    public String addFriend(String name, String uid) {
        if (dao.existsByUid(uid)) {
            return "⚠ UID가 이미 존재합니다. 추가되지 않았습니다.";
        }
        dao.add(new Friend(name, uid));
        return "✅ 친구가 추가되었습니다.";
    }

    public List<Friend> listFriends() {
        return dao.getAll();
    }
}
```



<br>4. View (Console - CampusPlannerConsole.java 일부)
```plaintext
   public class CampusPlannerConsole {

    static void handleFriends(Scanner sc, FriendController controller) {
        while (true) {
            System.out.println("\n1. 친구 추가 | 2. 전체 조회 | 0. 뒤로");
            System.out.print("선택: ");
            int cmd = Integer.parseInt(sc.nextLine());

            if (cmd == 1) {
                System.out.print("이름 입력: ");
                String name = sc.nextLine();
                System.out.print("UID 입력: ");
                String uid = sc.nextLine();

                String result = controller.addFriend(name, uid);
                System.out.println(result);

            } else if (cmd == 2) {
                controller.listFriends().forEach(System.out::println);
            } else if (cmd == 0) {
                break;
            }
        }
    }
}
```




<br> (4) 이해하기 쉽게 정리하면
| 구분                          | 실제 역할           | 코드 파일                     |
| ----------------------------- | -------------------- | ----------------------------- |
| **Model**                     | 데이터 정의          | `Friend.java`                |
| **DAO**                       | 데이터 저장/조회     | `FriendDao.java`             |
| **Controller (Business Logic)** | 중복 검사, 실행 흐름 제어 | `FriendController.java`      |
| **View**                      | UI 콘솔 입출력       | `CampusPlannerConsole.java`  |



<br> (5) Android 버전 MVC(MVVM)으로 확장할 수도 있음!
<br>추가 요청하시면 Android Activity + ViewModel + LiveData + Room 형태로도 변환해줄게요.




<br> (6) 더 발전시키고 싶다면?

- 친구 삭제 기능 추가
-  파일 저장 (Serialization or RoomDB)
-  iewModel → MVVM 구조로 변경
-  ndroid XML UI 연동도 가능
