# AI 활용보고서


## *질문 1

- 사용 AI: ChatGPT 5
- 문제: 캠퍼스 플래너 콘솔 프로그램

---
**프롬프트**

<br>친구 UID가 중복되는지 먼저 검사해야 하는데, 이런 검사는 DAO와 ViewModel 중에 어디서 수행해야 할까?

---

**결과**

<br>좋은 질문이야!  답을 설계 관점에서 정리해서 설명해줄게.

<br>✅ 친구 UID 중복 검사 — DAO vs ViewModel 어디서 하는 게 맞을까?
| 기준     | DAO에서 검사                                                                          | ViewModel에서 검사                                                                      |
|----------|---------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| **역할** | 데이터 저장/조회/삭제 등 “저장소와 직접 상호작용”                                      | 사용자 입력과 비즈니스 로직 처리                                                        |
| **장점** | - 데이터 중복이 저장소에 들어가는 것을 가장 근본적으로 막을 수 있음<br>- 다른 계층(ViewModel, UI)이 실수해도 안전하게 보장됨 | - UI 혹은 사용자 흐름에 따라 안내 메시지를 제공하거나 예외 처리하기 쉬움<br>- 사용자 친화적인 메시지 가능 |
| **단점** | - 비즈니스 로직까지 DAO에 넣으면 역할이 무거워짐<br>- MVC/MVVM 규칙에서 벗어나기 쉬움 | - ViewModel에서만 중복 검사를 하고 DAO를 그대로 저장하면, 다른 코드에서 DAO를 직접 호출할 경우 중복 데이터 저장 위험 존재 |
| **결론** | “저장되는 데이터 무결성”을 보장하려면 **DAO에서도 최종적으로 검증하는 것이 바람직**         | “사용자 안내 및 흐름 제어”는 **ViewModel에서 먼저 검사 후 DAO에 전달하는 구조가 이상적**      |



<br>✅ 가장 이상적인 구조는?
<br>✔ 1차 검사 → ViewModel에서
```plaintext
if (friendDao.existsByUid(uid)) {
    throw new IllegalArgumentException("이미 존재하는 UID입니다.");
}
friendDao.addNew(name, uid);
```

<br>✔ 2차 (최종) 보증 → DAO 내부에서도 확인 가능 (선택적)
```plaintext
Friend addNew(String name, String uid) {
    if (friends.stream().anyMatch(f -> f.uid.equals(uid))) {
        throw new IllegalStateException("중복 UID 저장 시도!");
    }
    // 안전하게 저장
}
```

<br>✅ 요약하면,

“입력 흐름과 사용자 메시지를 담당하는 ViewModel에서 먼저 검사하고,
데이터의 최종 무결성을 보장하는 DAO에서 한 번 더 안전장치로 확인하는 것이 가장 좋은 구조다.”

---<br><br><br>





### *질문 2
- 사용 AI: ChatGPT 5
- 문제: 캠퍼스 플래너 콘솔 프로그램

---

**프롬프트**

<br>ViewModel에서 Scanner를 직접 쓰지 않도록 했는데, 이런 구조가 테스트 코드 작성에 왜 유리한가요?

---

**결과**


<br>좋은 질문이에요! 이 질문에 대해 이해하기 쉽게 설명해드릴게요.

<br>✅ 💡 답변 핵심 요약:
| 이유                                                | 설명                                                                                                                                                                           |
| ------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **1. 입력(Scanner)과 로직(ViewModel)을 분리 → 단위 테스트 가능** | ViewModel 안에 Scanner 같은 사용자 입력 코드가 포함되어 있으면, 테스트할 때 항상 키보드 입력을 흉내 내야 합니다. 하지만 입력을 핸들러(UI 계층)에만 두고 ViewModel은 로직만 담당하게 하면, 테스트 시에 직접 메서드를 호출해서 결과만 검증하면 되기 때문에 테스트가 훨씬 쉬워집니다. |
| **2. Mocking / 자동화 테스트에 유리**                      | ViewModel에 Scanner가 있으면 테스트 환경에서 입력 흐름을 가짜(mock)로 만들기 어렵습니다. 반면, ViewModel이 입력 없이 메서드만 제공하면 JUnit 같은 테스트 코드에서 DAO를 Mocking하여 자유롭게 테스트할 수 있습니다.                               |
| **3. ViewModel이 “순수 로직(Pure Logic)” 상태가 됨**       | Scanner, System.out 같은 I/O는 외부 환경에 영향을 받기 때문에 테스트하기 어렵습니다. ViewModel에서 이를 제거하면 부작용 없는 ‘순수한 로직’만 남게 되어 테스트뿐 아니라 유지보수도 쉬워집니다.                                                  |
| **4. UI 변경에 독립적**                                 | 나중에 콘솔 UI → GUI 또는 Android UI로 바꿔도 ViewModel은 그대로 재사용 가능해집니다. 즉, ViewModel이 Scanner에 묶여 있지 않기 때문에 구조적으로 더 유연합니다.                                                             |


<br>✅ 📘 예시로 더 쉽게 설명하면:

```plaintext
// ❌ Scanner가 ViewModel 안에 있는 경우
class FriendViewModel {
    void addFriend() {
        Scanner sc = new Scanner(System.in); // UI 코드
        String name = sc.nextLine();
        dao.add(new Friend(name));
    }
}

// ✅ ViewModel은 입력 없이 로직만 처리하는 경우
class FriendViewModel {
    void addFriend(String name, String uid) {
        dao.addNew(name, uid); // 순수 로직
    }
}
```

✔ 첫 번째 경우에는 테스트할 때 키보드 입력을 흉내 내야 해서 자동화 테스트가 어렵습니다.
<br>✔ 두 번째 경우에는 단순히 메서드 호출 → 결과만 검증하면 되므로 테스트가 쉬워집니다.


---

