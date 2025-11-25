---

### 최소영

### 프롬프트
인터페이스 추상화가 DIP를 실질적으로 어떻게 구현하는지, FriendService → FriendRepository 구조를 예로 들어 설명해줘.

---

### 결과

## 1. DIP(Dependency Inversion Principle) 다시 정확히 짚고 가기

### SOLID에서 DIP(의존성 역전 원칙)는 보통 이렇게 요약된다.

1. 상위 모듈(High-level module)은 하위 모듈(Low-level module)에 의존하면 안 된다.

2. 둘 다 추상화(Abstraction)에 의존해야 한다.

3. 추상화는 구체(Concrete)에 의존하면 안 되고, 구체가 추상화에 의존해야 한다.

여기서 말하는:

- 상위 모듈: 비즈니스 로직, 정책을 결정하는 부분
→ 너의 코드에서는 FriendService가 여기에 해당

- 하위 모듈: DB, 파일, 네트워크 등 실제 기술 의존적인 부분
→ LocalStoreFriendRepository, LocalStore, LocalDatabaseManager 가 여기에 해당

### DIP의 핵심 문장은 이렇게 말할 수 있다:

“핵심 비즈니스 로직은 구체적인 기술(파일 저장, DB, 프레임워크)에 묶이면 안 되고,
이런 기술들은 추상화된 인터페이스 뒤로 숨겨져야 한다.”

## 2. Before 구조: Service가 구현체에 직접 묶여 있던 상태

### 너가 처음 올렸던 “나쁜 코드”에서는 Friend 관련 계층이 대략 이런 모양이었다:

- FriendViewModel 또는 FriendDao가 직접 LocalStore와 LocalDatabaseManager에 붙어 있음

- UID 검증, 중복 체크, 예외 처리, 저장 로직이 한 클래스 안에 섞여 있음

- 상위 계층 코드가 “이게 파일 기반 저장”이라는 사실을 그대로 알고 있음

예를 들어 비슷한 느낌의 구조는 이런 식이었지:
```
static class FriendDao implements Crud<Friend> {
    private final LocalStore store;
    private final LocalDatabaseManager db;

    FriendDao(LocalStore store, LocalDatabaseManager db){ 
        this.store = store; 
        this.db = db; 
    }

    Friend addNew(String name, String uid){ 
        if (!uid.matches("^uid_[0-9]+$")) { 
            throw new IllegalArgumentException("UID 형식 오류"); 
        }
        for(Friend f : store.friends){ 
            if(f.uid.equalsIgnoreCase(uid)) 
                throw new IllegalArgumentException("이미 존재하는 UID입니다.");
        }
        long id = store.friendSeq++; 
        Friend f = new Friend(id, name, uid); 
        store.friends.add(f); 
        db.save(store); 
        return f; 
    }
    // ...
}
```

이 구조의 문제점:

1. 상위 로직(Friend 관리)이 바로 LocalStore와 LocalDatabaseManager라는 구체 구현에 붙어 있음

2. 만약 나중에 “파일 말고 DB(MySQL)로 저장”으로 바꾸고 싶으면:

- FriendDao를 갈아엎거나

- FriendDao 안에 if(DB냐, 파일이냐) 같은 조건문을 넣어야 함 → 더 나쁜 코드

3. 테스트에서 FriendDao를 테스트하려면 실제 파일이 계속 생성됨
   
→ 단위 테스트가 느리고, 환경 의존적이 됨

5. “친구 기능”이라는 도메인 로직과 “파일 기반 저장”이라는 인프라 로직이 섞여 있음
   
→ SRP(단일 책임 원칙)도 같이 깨짐

### 즉, Before 구조에서는:

- 상위 모듈(친구 비즈니스 로직)이 하위 모듈(파일 기반 저장 세부 구현)에 직접 의존하고 있었다.
→ DIP를 어긴 상태.



## 3. After 구조: FriendRepository 인터페이스로 추상화 도입

### 좋은 코드에서는 이렇게 구조를 재설계했다.

### 3-1. FriendRepository 인터페이스 정의
```
public interface FriendRepository {

    Friend create(String name, String uid, String tag);  // 새 친구 생성 + 저장
    Friend save(Friend friend);                          // 수정 후 저장

    Optional<Friend> findById(long id);
    Optional<Friend> findByUid(String uid);

    List<Friend> findAll();
    List<Friend> findByTag(String tag);

    void deleteById(long id);
}
```

이 인터페이스는 “친구를 저장/조회하기 위해 상위 계층이 원하는 동작”만을 표현한다.

#### 중요한 점:

- 여기에는 “어디에 어떻게 저장하는지”에 대한 정보가 없다.

- DB를 쓰든, 파일을 쓰든, 메모리를 쓰든, UI는 신경 쓰지 않고 이 인터페이스만 사용하면 된다.

- 즉, 저장 기술을 도메인에서 완전히 숨기는 추상화 레이어다.


### 3-2. LocalStore 기반 구현체: LocalStoreFriendRepository
```
public class LocalStoreFriendRepository implements FriendRepository {

    private final LocalStore store;
    private final LocalDatabaseManager db;

    public LocalStoreFriendRepository(LocalStore store, LocalDatabaseManager db) {
        if (store == null) throw new IllegalArgumentException("store must not be null");
        if (db == null) throw new IllegalArgumentException("db must not be null");
        this.store = store;
        this.db = db;

        if (this.store.friends == null) {
            this.store.friends = new ArrayList<>();
        }
        if (this.store.friendSeq <= 0) {
            this.store.friendSeq = 1;
        }
    }

    @Override
    public Friend create(String name, String uid, String tag) {
        long id = store.friendSeq++;
        Friend friend = new Friend(id, name, uid, tag);
        store.friends.add(friend);
        db.save(store);
        return friend;
    }

    // 나머지 findById, findAll 등 구현...
}
```

#### 여기서 볼 수 있는 것:

- 이 클래스는 “파일 기반 LocalStore를 사용해서 Friend를 영속화”한다.

- 그런데 이 구현은 상위 계층에서 전혀 알 필요가 없다.

- 상위 계층은 오직 FriendRepository 타입으로만 다루기 때문.


## 4. FriendService는 FriendRepository(추상화)에만 의존

핵심 구조:
```
public class FriendService {

    private final FriendRepository repository;

    public FriendService(FriendRepository repository) {
        this.repository = repository;
    }

    public Friend addFriend(String name, String uid, String tag) {
        validateName(name);
        validateUidFormat(uid);
        validateUidUnique(uid, null);
        return repository.create(name.trim(), uid.trim(), tag);
    }

    public List<Friend> listFriends() {
        return repository.findAll();
    }

    public void deleteFriend(long id) {
        Friend f = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 친구가 없습니다."));
        repository.deleteById(id);
    }

    // updateFriend, updateTag, findByTag 등...
}
```

#### 여기서 중요한 점:

1. FriendService는 LocalStoreFriendRepository라는 이름을 전혀 모른다.

2. “친구를 어떻게 저장하는지”에 대한 구체 구현은 알지 못하고, 알 필요도 없다.

3. Service는 “추상 인터페이스가 어떤 기능을 제공하는가”만 신경 쓴다.

- create / save / findById / findAll / findByTag / deleteById

#### 이 상태를 DIP 관점에서 보면:

- High-level module: FriendService (도메인/비즈니스 로직)

- Low-level module: LocalStoreFriendRepository (구체 저장소 구현)

- 둘 다 FriendRepository라는 추상화에 의존하는 구조가 된다.

바로 이 지점에서 “의존성 역전”이 일어난다.

## 5. 의존성 역전이 실제로 “어떻게” 일어나는지 단계별로 보기

### 5-1. 전통적인(나쁜) 구조에서의 의존 방향

#### Before에는 의존 방향이 이렇게 흐른다:

UI/콘솔 → FriendViewModel/FriendDao → LocalStore + LocalDatabaseManager

- 상위가 아래 구현을 직접 사용

- 변경 방향: 저장 방식이 바뀌면, 위까지 줄줄이 수정 필요

### 5-2. 현재(좋은) 구조에서의 의존 방향

#### After 구조를 의존 관계로 그리면:

- FriendService → FriendRepository (인터페이스, 추상화 계층)

- LocalStoreFriendRepository → FriendRepository (인터페이스 구현)

즉:

- High-level 모듈(FriendService)도 FriendRepository에 의존

- Low-level 모듈(LocalStoreFriendRepository)도 FriendRepository에 의존

의존 방향이 “구체 → 추상”으로 향하게 된다.

그래서 이를 “Dependency Inversion(의존성 역전)”이라고 부른다.

실행 시에는 main에서 구체 구현을 주입한다:
```
LocalDatabaseManager db = new LocalDatabaseManager("campusplanner.db");
LocalStore store = db.load();

FriendRepository friendRepo = new LocalStoreFriendRepository(store, db);
FriendService friendService = new FriendService(friendRepo);
```

여기서:

- 컴파일 시 기준: FriendService는 FriendRepository 인터페이스만 알고 있음

- 런타임 기준: 실제 객체는 LocalStoreFriendRepository 인스턴스를 받음

- DI(Dependency Injection)를 통해 구체를 주입함으로써 DIP 구조가 완성된다.
  

## 6. 인터페이스 추상화가 DIP를 구현하면서 생기는 실제 효과들

### 6-1. 저장 방식 교체 시 상위 모듈 수정 없음

예를 들어 나중에 MySQL로 바꾸고 싶다고 해보자.
```
public class MySqlFriendRepository implements FriendRepository {
    // JDBC로 insert/select/update/delet 구현
}
```

이렇게 구현한 뒤 main에서:
```
FriendRepository friendRepo = new MySqlFriendRepository(/* 커넥션 정보 */);
FriendService friendService = new FriendService(friendRepo);
```

이렇게만 바꾸면 된다.

- FriendService 코드는 한 줄도 수정할 필요가 없다.

- 콘솔 UI(handleFriends)도 수정할 필요가 없다.

즉, “저장소 기술 교체”의 변화가 상위 도메인 로직에 퍼져 올라오지 않는다.

이게 바로 DIP의 목적이자, 인터페이스 추상화의 위력이다.

### 6-2. 테스트 코드가 훨씬 쉬워진다

#### 단위 테스트를 할 때 실제 파일 IO를 사용하면:

- 테스트 속도가 느려지고

- 파일이 생성/삭제되며

- 환경에 따라(권한, 경로) 깨질 가능성이 생긴다.

하지만 지금 구조에서는:
```
class FakeFriendRepository implements FriendRepository {
    // 메모리 리스트만 사용하는 가짜 구현
}
```

를 만들어서:

```
FriendRepository repo = new FakeFriendRepository();
FriendService service = new FriendService(repo);
```

#### 이렇게 하면:

- 파일이나 DB 없이도 순수 메모리 상에서 테스팅 가능

- 오류 상황, UID 중복, 형식 오류 등 순수 비즈니스 로직만 집중해서 검증 가능

이것도 “Service가 구체 구현이 아니라 인터페이스에만 의존한다”는 구조 덕분에 가능해진다.


### 6-3. 역할 분리가 명확해짐 (SRP와도 연결)

#### 지금 구조에서 파일 별 책임을 정리해 보면:

- Friend
→ 도메인 엔티티 (id 불변, changeName/changeUid/changeTag 제공)

- FriendRepository
→ “친구를 저장/조회하기 위한 추상 인터페이스”

- LocalStoreFriendRepository
→ FriendRepository를 LocalStore 기반으로 구현 (기술 의존)

- FriendService
→ “친구 도메인 비즈니스 규칙” (UID 형식 검증, 중복 검증, 예외 메시지 등)

- LocalStore, LocalDatabaseManager
→ 데이터 구조와 직렬화/파일 IO 책임

이렇게 “도메인 / 저장소 인터페이스 / 저장소 구현 / 인프라”가 각각 분리된다.


DIP를 지키는 과정에서 자연스럽게 SRP(단일 책임 원칙)도 지켜지고,

각 클래스는 자기 역할에만 집중하게 된다.


## 7. FriendService → FriendRepository 구조를 한 문단으로 정리하면


기존 코드는 Friend 관련 기능에서 LocalStore와 LocalDatabaseManager 같은 구체적인 파일 기반 저장 구현에 직접 의존하고 있어,

비즈니스 로직과 저장 기술이 강하게 결합된 형태였다. 

이를 개선하기 위해 FriendRepository라는 인터페이스를 도입하여, 친구 데이터를 생성·조회·수정·삭제하는 동작을 추상화하였다. 

상위 모듈인 FriendService는 더 이상 LocalStoreFriendRepository나 LocalStore 같은 구현 세부사항을 알지 않고, 오직 FriendRepository 인터페이스에만 의존한다. 

반대로 하위 모듈(LocalStoreFriendRepository)은 FriendRepository를 구현하면서 실제 파일 기반 저장 기능을 제공한다. 

이처럼 상위 모듈과 하위 모듈이 모두 추상화(인터페이스)에 의존하도록 의존 방향을 역전시킴으로써, 

DIP(의존성 역전 원칙)를 실질적으로 구현할 수 있었다. 


그 결과, 저장소 구현을 파일에서 DB나 다른 외부 시스템으로 교체하더라도 FriendService와 UI 코드는 수정할 필요가 없으며, 

테스트 시에는 가짜(Fake) Repository를 주입하여 순수 비즈니스 로직만 손쉽게 검증할 수 있게 되었다.

